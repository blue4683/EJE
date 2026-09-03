package com.skala.miniproject.audio;

import com.skala.miniproject.config.AnalysisProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegAudioDecoderTest {

    private final FfmpegAudioDecoder decoder = new FfmpegAudioDecoder(testProperties());

    @Test
    void 정상_webm_3초는_durationMs가_3000_근처다() throws IOException {
        DecodedAudio result = decoder.decode(fixture("sample-3s.webm"), "audio/webm");

        assertTrue(Math.abs(result.durationMs() - 3000) <= 50, "durationMs=" + result.durationMs());
    }

    @Test
    void 정상_파일의_진폭_배열은_30개_근처다() throws IOException {
        // opus 인코딩·리샘플링 왕복 오차로 샘플 수가 정확히 48000이 아닐 수 있어 근접 범위로 검증한다.
        DecodedAudio result = decoder.decode(fixture("sample-3s.webm"), "audio/webm");

        assertTrue(result.amplitudes().length >= 29 && result.amplitudes().length <= 31,
                "points=" + result.amplitudes().length);
    }

    @Test
    void _500ms_파일은_AUDIO_DURATION_OUT_OF_RANGE다() throws IOException {
        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> decoder.decode(fixture("too-short-500ms.wav"), "audio/wav"));

        assertEquals(AudioDecodeException.Reason.DURATION_OUT_OF_RANGE, ex.reason());
    }

    @Test
    void 손상된_파일은_INVALID_AUDIO다() throws IOException {
        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> decoder.decode(fixture("broken.webm"), "audio/webm"));

        assertEquals(AudioDecodeException.Reason.INVALID_AUDIO, ex.reason());
    }

    @Test
    void 빈파일은_INVALID_AUDIO다() {
        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> decoder.decode(new byte[0], "audio/webm"));

        assertEquals(AudioDecodeException.Reason.INVALID_AUDIO, ex.reason());
    }

    @Test
    void 허용목록에_없는_MIME은_UNSUPPORTED_MEDIA_TYPE이다() throws IOException {
        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> decoder.decode(fixture("sample-3s.webm"), "audio/flac"));

        assertEquals(AudioDecodeException.Reason.UNSUPPORTED_MEDIA_TYPE, ex.reason());
    }

    @Test
    void 선언한_형식과_실제_컨테이너가_다르면_UNSUPPORTED_MEDIA_TYPE이다() throws IOException {
        // 실제로는 wav(RIFF/WAVE) 파일인데 webm 이라고 선언한 경우
        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> decoder.decode(fixture("too-short-500ms.wav"), "audio/webm"));

        assertEquals(AudioDecodeException.Reason.UNSUPPORTED_MEDIA_TYPE, ex.reason());
    }

    @Test
    void audio_webm_codecs_opus는_파라미터를_떼고_통과한다() throws IOException {
        DecodedAudio result = decoder.decode(fixture("sample-3s.webm"), "audio/webm;codecs=opus");

        assertTrue(Math.abs(result.durationMs() - 3000) <= 50);
    }

    @Test
    void _60초를_넘는_파일은_잘라내지_않고_거절한다() {
        byte[] longSilentWav = buildSilentWav(16000, 1, 16, 61);

        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> decoder.decode(longSilentWav, "audio/wav"));

        assertEquals(AudioDecodeException.Reason.DURATION_OUT_OF_RANGE, ex.reason());
    }

    @Test
    void 타임아웃이_지나면_TIMEOUT_예외를_던지고_프로세스를_강제종료한다() throws IOException {
        // decodeTimeoutSeconds=0 으로 실제 10초를 기다리지 않고 타임아웃 경로를 결정적으로 검증한다.
        AnalysisProperties zeroTimeout = new AnalysisProperties(
                "mock", "speech-habits-v1", "mock-pipeline-v1",
                2, 4, 30, 5, 600, 120, 3,
                "ffmpeg", 0, 30
        );
        FfmpegAudioDecoder impatientDecoder = new FfmpegAudioDecoder(zeroTimeout);
        byte[] audio = fixture("sample-3s.webm");

        AudioDecodeException ex = assertThrows(AudioDecodeException.class,
                () -> impatientDecoder.decode(audio, "audio/webm"));

        assertEquals(AudioDecodeException.Reason.TIMEOUT, ex.reason());
    }

    @Test
    void 큰_입력에서도_교착하지_않는다() {
        // 192kHz 스테레오 30초 무음 WAV ≈ 22MB 입력. stdin 쓰기와 stdout 읽기를 동시에 하지 않으면 여기서 멈춘다.
        byte[] bigWav = buildSilentWav(192000, 2, 16, 30);
        assertTrue(bigWav.length > 10 * 1024 * 1024, "fixture size=" + bigWav.length);

        DecodedAudio result = decoder.decode(bigWav, "audio/wav");

        assertTrue(Math.abs(result.durationMs() - 30000) <= 50);
    }

    private static AnalysisProperties testProperties() {
        return new AnalysisProperties(
                "mock", "speech-habits-v1", "mock-pipeline-v1",
                2, 4, 30, 5, 600, 120, 3,
                "ffmpeg", 10, 30
        );
    }

    private static byte[] fixture(String name) throws IOException {
        try (InputStream in = FfmpegAudioDecoderTest.class.getResourceAsStream("/fixtures/" + name)) {
            if (in == null) {
                throw new IOException("fixture not found: " + name);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    /** 무음 PCM 데이터를 WAV 컨테이너로 감싼다. 순수 Java 로 합성해 무거운 바이너리를 커밋하지 않는다. */
    private static byte[] buildSilentWav(int sampleRate, int channels, int bitsPerSample, int durationSeconds) {
        int blockAlign = channels * bitsPerSample / 8;
        int byteRate = sampleRate * blockAlign;
        int dataSize = byteRate * durationSeconds;
        byte[] wav = new byte[44 + dataSize]; // 나머지는 0(무음)으로 채워진다

        writeAscii(wav, 0, "RIFF");
        writeIntLe(wav, 4, 36 + dataSize);
        writeAscii(wav, 8, "WAVE");
        writeAscii(wav, 12, "fmt ");
        writeIntLe(wav, 16, 16);
        writeShortLe(wav, 20, (short) 1); // PCM
        writeShortLe(wav, 22, (short) channels);
        writeIntLe(wav, 24, sampleRate);
        writeIntLe(wav, 28, byteRate);
        writeShortLe(wav, 32, (short) blockAlign);
        writeShortLe(wav, 34, (short) bitsPerSample);
        writeAscii(wav, 36, "data");
        writeIntLe(wav, 40, dataSize);
        return wav;
    }

    private static void writeAscii(byte[] target, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, target, offset, bytes.length);
    }

    private static void writeIntLe(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
        target[offset + 2] = (byte) (value >> 16);
        target[offset + 3] = (byte) (value >> 24);
    }

    private static void writeShortLe(byte[] target, int offset, short value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
    }
}
