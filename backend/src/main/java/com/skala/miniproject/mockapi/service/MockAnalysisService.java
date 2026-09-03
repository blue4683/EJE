package com.skala.miniproject.mockapi.service;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.mockapi.dto.AmplitudePointDto;
import com.skala.miniproject.mockapi.dto.MockTranscriptRequest;
import com.skala.miniproject.mockapi.dto.MockTranscriptResponse;
import com.skala.miniproject.mockapi.dto.MockWaveformRequest;
import com.skala.miniproject.mockapi.dto.MockWaveformResponse;
import com.skala.miniproject.speech.SpeechAnalysisException;
import com.skala.miniproject.speech.model.Basic;
import com.skala.miniproject.speech.model.FillerBreakdown;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.TimedToken;
import com.skala.miniproject.speech.model.WaveformPoint;
import com.skala.miniproject.speech.rule.FillerCounter;
import com.skala.miniproject.speech.rule.IntervalNormalizer;
import com.skala.miniproject.speech.rule.RepetitionCounter;
import com.skala.miniproject.speech.rule.SilenceCalculator;
import com.skala.miniproject.speech.rule.SilenceMetrics;
import com.skala.miniproject.speech.rule.SpeechRateCalculator;
import com.skala.miniproject.speech.rule.TokenNormalizer;
import com.skala.miniproject.speech.rule.WaveformBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * B1 규칙 엔진을 HTTP 로 즉시 검증하는 개발·검증용 서비스. B1 의 개별 규칙 클래스만 조합해서 쓴다 —
 * {@code SpeechHabitsAnalyzer.analyze()} 통합 진입점은 12개 규칙을 전부 실행하므로, API 20 에서
 * 안 쓰는 코칭 텍스트를 매번 생성하거나 API 21 에서 안 쓰는 빈 파형을 매번 만드는 낭비가 생긴다.
 * 이 서비스는 DB 에 아무것도 쓰지 않는다.
 */
@Service
public class MockAnalysisService {

    /** API명세서.md API 20 처리 규칙: 진폭 0.1 이상인 100ms 구간을 원시 발화로 삼는다. */
    private static final double SPEECH_AMPLITUDE_THRESHOLD = 0.1;
    private static final int BUCKET_MS = 100;

    public MockWaveformResponse analyzeWaveform(MockWaveformRequest request) {
        int durationMs = request.durationMs();

        List<SpeechInterval> candidates = new ArrayList<>();
        for (AmplitudePointDto point : request.waveform()) {
            if (point.amplitude() >= SPEECH_AMPLITUDE_THRESHOLD) {
                candidates.add(new SpeechInterval(point.timeMs(), point.timeMs() + BUCKET_MS));
            }
        }
        List<SpeechInterval> speechIntervals = IntervalNormalizer.normalize(candidates, durationMs);
        SilenceMetrics silence = SilenceCalculator.compute(speechIntervals, durationMs);

        double[] amplitudes = request.waveform().stream().mapToDouble(AmplitudePointDto::amplitude).toArray();
        List<WaveformPoint> waveform = WaveformBuilder.build(amplitudes, speechIntervals, durationMs);

        return new MockWaveformResponse(durationMs, silence.speechDurationMs(), silence.silenceDurationMs(),
                silence.longSilenceCount(), speechIntervals, waveform);
    }

    public MockTranscriptResponse analyzeTranscript(MockTranscriptRequest request) {
        int durationMs = request.durationMs();
        List<TimedToken> rawTokens = request.tokens().stream()
                .map(t -> new TimedToken(t.text(), t.startMs(), t.endMs()))
                .toList();

        List<TimedToken> validTokens = normalizeWithoutOverlapCheck(rawTokens, durationMs);

        List<FillerBreakdown> breakdown = FillerCounter.breakdown(validTokens);
        int fillerTotalCount = FillerCounter.totalCount(breakdown);
        List<FillerEvent> timeline = FillerCounter.timeline(validTokens);
        List<SegmentCount> segments = FillerCounter.segments(validTokens, durationMs);
        int repeatedExpressionCount = RepetitionCounter.count(validTokens);
        // speechDurationMs 가 없는 경로라 wordsPerMinute 는 버리고 totalWordCount 만 쓴다.
        int totalWordCount = SpeechRateCalculator.compute(validTokens, 0).totalWordCount();

        Basic basic = new Basic(fillerTotalCount, breakdown);
        return new MockTranscriptResponse(basic, totalWordCount, repeatedExpressionCount, timeline, segments);
    }

    /**
     * 규칙 4 ⑤(startMs 비감소 검사)를 어긴 입력은 TokenNormalizer 가 SpeechAnalysisException 을 던진다.
     * B5 파이프라인에서는 이 예외가 INVALID_ANALYSIS_RESULT(서버측 결과 오류)로 매핑되지만, 여기서는
     * 사용자가 보낸 요청 자체가 형식을 어긴 것이므로 VALIDATION_ERROR(422)로 바꿔서 던진다.
     */
    private List<TimedToken> normalizeWithoutOverlapCheck(List<TimedToken> rawTokens, int durationMs) {
        try {
            return TokenNormalizer.normalize(rawTokens, durationMs, List.of(), true);
        } catch (SpeechAnalysisException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
