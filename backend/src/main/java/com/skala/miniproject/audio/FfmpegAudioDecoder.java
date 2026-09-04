package com.skala.miniproject.audio;

import com.skala.miniproject.config.AnalysisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 유일한 {@link AudioDecoder} 구현체. ffmpeg 를 파이프로만 호출한다 — 원본을 임시 파일로 만들지 않는다.
 * 클라이언트가 보낸 duration 은 신뢰하지 않고, 서버가 16kHz mono PCM 으로 디코딩해 길이를 확정한다.
 */
@Component
@EnableConfigurationProperties(AnalysisProperties.class)
public class FfmpegAudioDecoder implements AudioDecoder {

    private static final int SAMPLE_RATE = 16000;
    private static final int BYTES_PER_SAMPLE = 2; // s16le
    private static final int MIN_DURATION_MS = 1000;
    private static final int MAX_DURATION_MS = 60000;
    private static final int BUCKET_MS = 100;
    private static final int SAMPLES_PER_BUCKET = SAMPLE_RATE * BUCKET_MS / 1000; // 1600
    private static final int MAX_POINTS = 600;

    private static final byte[] EBML_HEADER = {(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3};
    private static final byte[] OGG_HEADER = "OggS".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF_HEADER = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WAVE_HEADER = "WAVE".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FTYP_MARKER = "ftyp".getBytes(StandardCharsets.US_ASCII);

    private enum Container { WEBM, OGG, WAV, MP4, UNKNOWN }

    private final AnalysisProperties properties;

    public FfmpegAudioDecoder(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public DecodedAudio decode(byte[] audioBytes, String declaredMimeType) {
        String normalizedMime = MimeTypeNormalizer.normalize(declaredMimeType);
        if (!MimeTypeNormalizer.isAllowed(normalizedMime)) {
            throw new AudioDecodeException(AudioDecodeException.Reason.UNSUPPORTED_MEDIA_TYPE,
                    "지원하지 않는 음성 형식입니다: " + normalizedMime);
        }
        if (audioBytes.length == 0) {
            throw new AudioDecodeException(AudioDecodeException.Reason.INVALID_AUDIO, "빈 파일입니다.");
        }

        Container detected = sniffContainer(audioBytes);
        if (detected != Container.UNKNOWN && !containerMatches(normalizedMime, detected)) {
            throw new AudioDecodeException(AudioDecodeException.Reason.UNSUPPORTED_MEDIA_TYPE,
                    "선언한 형식(" + normalizedMime + ")이 실제 파일 형식과 다릅니다.");
        }

        byte[] pcm = runFfmpeg(audioBytes);

        long sampleCount = pcm.length / (long) BYTES_PER_SAMPLE;
        int durationMs = (int) Math.round(sampleCount * 1000.0 / SAMPLE_RATE);
        if (durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS) {
            throw new AudioDecodeException(AudioDecodeException.Reason.DURATION_OUT_OF_RANGE,
                    "음성 길이가 허용 범위를 벗어났습니다: " + durationMs + "ms");
        }

        double[] amplitudes = computeAmplitudes(pcm, sampleCount);
        return new DecodedAudio(durationMs, amplitudes, sampleCount);
    }

    private byte[] runFfmpeg(byte[] input) {
        ProcessBuilder builder = new ProcessBuilder(
                properties.ffmpegPath(), "-v", "error", "-hide_banner", "-nostdin",
                "-i", "pipe:0", "-f", "s16le", "-acodec", "pcm_s16le", "-ac", "1", "-ar", String.valueOf(SAMPLE_RATE),
                "pipe:1"
        );

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new AudioDecodeException(AudioDecodeException.Reason.INVALID_AUDIO, "ffmpeg 를 실행할 수 없습니다.", e);
        }

        // stdin 쓰기와 stdout·stderr 읽기를 동시에 해야 한다. 순차로 하면 파이프 버퍼가 차서 교착한다.
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        Thread stdinWriter = Thread.ofVirtual().start(() -> {
            try (OutputStream out = process.getOutputStream()) {
                out.write(input);
            } catch (IOException ignored) {
                // 프로세스가 먼저 종료되면 파이프가 닫혀 IOException 이 날 수 있다 — 정상 경로다.
            }
        });
        Thread stdoutReader = Thread.ofVirtual().start(() -> {
            try (InputStream in = process.getInputStream()) {
                in.transferTo(stdout);
            } catch (IOException ignored) {
            }
        });
        Thread stderrDrainer = Thread.ofVirtual().start(() -> {
            try (InputStream in = process.getErrorStream()) {
                in.transferTo(OutputStream.nullOutputStream());
            } catch (IOException ignored) {
            }
        });

        boolean finished;
        try {
            finished = process.waitFor(properties.decodeTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            joinQuietly(stdinWriter, stdoutReader, stderrDrainer);
            throw new AudioDecodeException(AudioDecodeException.Reason.TIMEOUT, "디코딩이 중단되었습니다.", e);
        }

        if (!finished) {
            process.destroyForcibly();
            joinQuietly(stdinWriter, stdoutReader, stderrDrainer);
            throw new AudioDecodeException(AudioDecodeException.Reason.TIMEOUT, "디코딩 시간이 초과되었습니다.");
        }

        joinQuietly(stdinWriter, stdoutReader, stderrDrainer);

        // stderr 내용(경로·원본 정보 노출 위험)은 사용자 응답에 넣지 않는다.
        if (process.exitValue() != 0) {
            throw new AudioDecodeException(AudioDecodeException.Reason.INVALID_AUDIO, "음성 파일을 읽을 수 없습니다.");
        }
        return stdout.toByteArray();
    }

    private static void joinQuietly(Thread... threads) {
        for (Thread thread : threads) {
            try {
                thread.join(java.time.Duration.ofSeconds(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static double[] computeAmplitudes(byte[] pcm, long sampleCount) {
        int bucketCount = (int) Math.min(MAX_POINTS, Math.ceil((double) sampleCount / SAMPLES_PER_BUCKET));
        double[] amplitudes = new double[bucketCount];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            long startSample = (long) bucket * SAMPLES_PER_BUCKET;
            long endSample = Math.min(startSample + SAMPLES_PER_BUCKET, sampleCount);
            long sumSquares = 0;
            for (long s = startSample; s < endSample; s++) {
                int idx = (int) (s * BYTES_PER_SAMPLE);
                short sample = (short) ((pcm[idx] & 0xFF) | (pcm[idx + 1] << 8)); // little-endian s16le
                sumSquares += (long) sample * sample;
            }
            long count = endSample - startSample;
            double rms = count > 0 ? Math.sqrt((double) sumSquares / count) : 0.0;
            amplitudes[bucket] = rms / 32768.0;
        }
        return amplitudes;
    }

    private static Container sniffContainer(byte[] bytes) {
        if (matches(bytes, 0, EBML_HEADER)) {
            return Container.WEBM;
        }
        if (matches(bytes, 0, OGG_HEADER)) {
            return Container.OGG;
        }
        if (matches(bytes, 0, RIFF_HEADER) && matches(bytes, 8, WAVE_HEADER)) {
            return Container.WAV;
        }
        if (matches(bytes, 4, FTYP_MARKER)) {
            return Container.MP4;
        }
        return Container.UNKNOWN;
    }

    /** MP3 는 보편적인 매직 넘버가 없어 바이트 서명으로 판별하지 않는다 — ffmpeg 디코딩 성공 여부로만 판정한다. */
    private static boolean containerMatches(String normalizedMime, Container detected) {
        return switch (detected) {
            case WEBM -> normalizedMime.equals("audio/webm");
            case OGG -> normalizedMime.equals("audio/ogg");
            case WAV -> normalizedMime.equals("audio/wav");
            case MP4 -> normalizedMime.equals("audio/mp4");
            case UNKNOWN -> true;
        };
    }

    private static boolean matches(byte[] data, int offset, byte[] pattern) {
        if (data.length < offset + pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}
