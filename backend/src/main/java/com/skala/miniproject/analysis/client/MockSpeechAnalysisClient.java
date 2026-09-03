package com.skala.miniproject.analysis.client;

import com.skala.miniproject.config.AnalysisProperties;
import com.skala.miniproject.speech.SpeechAnalysisException;
import com.skala.miniproject.speech.SpeechHabitsAnalyzer;
import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.SpeechMetrics;
import com.skala.miniproject.speech.model.TimedToken;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.skala.miniproject.domain.analysis.FailureCode.INVALID_ANALYSIS_RESULT;

/**
 * 유일한 SpeechAnalysisClient 구현체 (Phase 1). 실제 음성 내용과 무관한 시연용 결과를 만든다 —
 * API명세서.md 「Phase 1 Mock 및 Phase 2 내부 분석 API」 1문단의 생성 규칙을 그대로 따른다.
 * 지연을 두는 이유: 즉시 끝나면 PENDING 상태를 화면에서 관측할 수 없어 비동기 설계를 증명하지 못한다.
 */
@Component
public class MockSpeechAnalysisClient implements SpeechAnalysisClient {

    private static final int SIMULATED_DELAY_MS = 2000;
    private static final int BUCKET_MS = 100;

    /** API 17 예시(durationMs=3000)의 시각 토큰. D/3000 비율로 축소·확대해서 쓴다. */
    private static final int CANONICAL_DURATION_MS = 3000;
    private static final List<TimedToken> CANONICAL_TOKENS = List.of(
            new TimedToken("음", 700, 800),
            new TimedToken("저는", 900, 1100),
            new TimedToken("저는", 1200, 1400),
            new TimedToken("사실", 1500, 1700),
            new TimedToken("어", 2100, 2200),
            new TimedToken("홍길동입니다", 2300, 2500)
    );

    private static final double SPEECH_AMPLITUDE = 0.65;
    private static final double SILENCE_AMPLITUDE = 0.02;

    private final AnalysisProperties properties;

    public MockSpeechAnalysisClient(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public SpeechAnalysisResult analyze(SpeechAnalysisRequest request) {
        sleepQuietly(SIMULATED_DELAY_MS);

        int durationMs = request.durationMs();
        int speechStart = durationMs / 6;
        int speechEnd = (5 * durationMs) / 6;
        List<SpeechInterval> candidateIntervals = List.of(new SpeechInterval(speechStart, speechEnd));

        double scale = durationMs / (double) CANONICAL_DURATION_MS;
        List<TimedToken> scaledTokens = CANONICAL_TOKENS.stream()
                .map(token -> scaleToken(token, scale, durationMs))
                .toList();

        double[] amplitudes = buildAmplitudes(durationMs, speechStart, speechEnd);

        try {
            SpeechMetrics metrics = SpeechHabitsAnalyzer.analyze(durationMs, candidateIntervals, scaledTokens, amplitudes);
            return new SpeechAnalysisResult(properties.engineVersion(), metrics);
        } catch (SpeechAnalysisException e) {
            // speech 패키지는 Spring 을 모르므로 BusinessException 을 직접 던지지 않는다(주석 참조).
            // 여기서 클라이언트 계약의 실패로 바꿔 파이프라인이 INVALID_ANALYSIS_RESULT 로 다루게 한다.
            throw new SpeechAnalysisClientException(INVALID_ANALYSIS_RESULT, e.getMessage());
        }
    }

    private static TimedToken scaleToken(TimedToken token, double scale, int durationMs) {
        int start = clamp(Math.round(token.startMs() * scale), durationMs);
        int end = clamp(Math.round(token.endMs() * scale), durationMs);
        return new TimedToken(token.text(), start, end);
    }

    private static int clamp(long value, int durationMs) {
        return (int) Math.max(0, Math.min(value, durationMs));
    }

    private static double[] buildAmplitudes(int durationMs, int speechStart, int speechEnd) {
        int bucketCount = (int) Math.ceil(durationMs / (double) BUCKET_MS);
        double[] amplitudes = new double[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            int bucketStart = i * BUCKET_MS;
            amplitudes[i] = (bucketStart >= speechStart && bucketStart < speechEnd) ? SPEECH_AMPLITUDE : SILENCE_AMPLITUDE;
        }
        return amplitudes;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
