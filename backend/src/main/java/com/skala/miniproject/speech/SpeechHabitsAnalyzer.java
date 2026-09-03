package com.skala.miniproject.speech;

import com.skala.miniproject.speech.model.Basic;
import com.skala.miniproject.speech.model.Coaching;
import com.skala.miniproject.speech.model.FillerBreakdown;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.SpeechMetrics;
import com.skala.miniproject.speech.model.SpeechRate;
import com.skala.miniproject.speech.model.TimedToken;
import com.skala.miniproject.speech.model.WaveformPoint;
import com.skala.miniproject.speech.rule.CoachingComposer;
import com.skala.miniproject.speech.rule.FillerCounter;
import com.skala.miniproject.speech.rule.IntervalNormalizer;
import com.skala.miniproject.speech.rule.RepetitionCounter;
import com.skala.miniproject.speech.rule.SilenceCalculator;
import com.skala.miniproject.speech.rule.SilenceMetrics;
import com.skala.miniproject.speech.rule.SpeechRateCalculator;
import com.skala.miniproject.speech.rule.TokenNormalizer;
import com.skala.miniproject.speech.rule.WaveformBuilder;

import java.util.List;

/**
 * speech-habits-v1 규칙 12개를 순서대로 적용하는 엔트리포인트.
 * Spring·JPA·엔티티·DB 를 참조하지 않는 순수 함수다 — Phase 1 Mock 과 Phase 2 실제 분석이
 * 공유하는 데이터 의미가 여기서 정의된다 (docs/API명세서.md 「분석 규칙 — speech-habits-v1」).
 */
public final class SpeechHabitsAnalyzer {

    private SpeechHabitsAnalyzer() {
    }

    public static SpeechMetrics analyze(int durationMs,
                                         List<SpeechInterval> candidateIntervals,
                                         List<TimedToken> rawTokens,
                                         double[] amplitudes) {
        return analyze(durationMs, candidateIntervals, rawTokens, amplitudes, false);
    }

    /**
     * skipOverlapCheck=true 는 발화 구간이 없는 MOCK_002(전사 Mock) 경로 전용이다 (규칙 4 예외).
     * 이 경로에서는 candidateIntervals 를 빈 목록으로 넘긴다.
     */
    public static SpeechMetrics analyze(int durationMs,
                                         List<SpeechInterval> candidateIntervals,
                                         List<TimedToken> rawTokens,
                                         double[] amplitudes,
                                         boolean skipOverlapCheck) {
        List<SpeechInterval> speechIntervals = IntervalNormalizer.normalize(candidateIntervals, durationMs);
        SilenceMetrics silence = SilenceCalculator.compute(speechIntervals, durationMs);
        List<TimedToken> validTokens = TokenNormalizer.normalize(rawTokens, durationMs, speechIntervals, skipOverlapCheck);

        List<FillerBreakdown> breakdown = FillerCounter.breakdown(validTokens);
        List<FillerEvent> timeline = FillerCounter.timeline(validTokens);
        List<SegmentCount> segments = FillerCounter.segments(validTokens, durationMs);
        int fillerTotalCount = FillerCounter.totalCount(breakdown);
        verifyFillerInvariant(fillerTotalCount, breakdown, timeline, segments);

        SpeechRate speechRate = SpeechRateCalculator.compute(validTokens, silence.speechDurationMs());
        int repeatedExpressionCount = RepetitionCounter.count(validTokens);
        List<WaveformPoint> waveform = WaveformBuilder.build(amplitudes, speechIntervals, durationMs);

        Coaching coaching = CoachingComposer.compose(silence.speechDurationMs(), fillerTotalCount, segments,
                repeatedExpressionCount, silence.longSilenceCount());
        verifyCoaching(coaching);

        Basic basic = new Basic(fillerTotalCount, breakdown);
        return new SpeechMetrics(durationMs, silence.speechDurationMs(), silence.silenceDurationMs(),
                silence.longSilenceCount(), repeatedExpressionCount, basic,
                speechIntervals, waveform, timeline, speechRate, segments, coaching);
    }

    /** 규칙 11 — 저장 전에 반드시 검증한다. 패키지 가시성: 단위 테스트에서 직접 호출한다. */
    static void verifyFillerInvariant(int fillerTotalCount, List<FillerBreakdown> breakdown,
                                       List<FillerEvent> timeline, List<SegmentCount> segments) {
        int breakdownSum = breakdown.stream().mapToInt(FillerBreakdown::count).sum();
        int segmentSum = segments.stream().mapToInt(SegmentCount::fillerCount).sum();
        if (fillerTotalCount != breakdownSum || fillerTotalCount != timeline.size() || fillerTotalCount != segmentSum) {
            throw new SpeechAnalysisException(
                    "추임새 합계 불일치: total=%d breakdown=%d timeline=%d segment=%d"
                            .formatted(fillerTotalCount, breakdownSum, timeline.size(), segmentSum));
        }
    }

    /** 규칙 12 — 코칭 생성 실패는 전체 분석 실패다. */
    static void verifyCoaching(Coaching coaching) {
        if (coaching.summary().isBlank() || coaching.practiceRecommendation().isBlank()
                || coaching.actionItems().isEmpty()) {
            throw new SpeechAnalysisException("코칭 생성에 실패했습니다.");
        }
    }
}
