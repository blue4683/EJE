package com.skala.miniproject.speech;

import com.skala.miniproject.speech.model.FillerBreakdown;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SegmentType;
import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.SpeechMetrics;
import com.skala.miniproject.speech.model.TimedToken;
import com.skala.miniproject.speech.model.WaveType;
import com.skala.miniproject.speech.model.WaveformPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeechHabitsAnalyzerTest {

    @Test
    void 네_합계가_모두_일치하지_않으면_예외를_던진다() {
        List<FillerBreakdown> breakdown = List.of(new FillerBreakdown("음", 2)); // 합계 2
        List<FillerEvent> timeline = List.of(new FillerEvent(0, 100, "음")); // 길이 1
        List<SegmentCount> segments = List.of(
                new SegmentCount(SegmentType.INITIAL, 1, 0),
                new SegmentCount(SegmentType.MIDDLE, 0, 0),
                new SegmentCount(SegmentType.FINAL, 0, 0)
        );

        assertThrows(SpeechAnalysisException.class,
                () -> SpeechHabitsAnalyzer.verifyFillerInvariant(2, breakdown, timeline, segments));
    }

    @Test
    void 무음_빌지도_완료_결과이며_코칭이_비어있지_않다() {
        double[] amplitudes = new double[30]; // 3000ms / 100ms, 전부 무음

        SpeechMetrics metrics = SpeechHabitsAnalyzer.analyze(3000, List.of(), List.of(), amplitudes);

        assertEquals(0, metrics.speechDurationMs());
        assertEquals(3000, metrics.silenceDurationMs());
        assertEquals(0, metrics.basic().fillerTotalCount());
        assertNull(metrics.speechRate().wordsPerMinute());
        assertEquals(0, metrics.speechRate().totalWordCount());
        assertFalse(metrics.coaching().summary().isBlank());
        assertFalse(metrics.coaching().actionItems().isEmpty());
        assertFalse(metrics.coaching().practiceRecommendation().isBlank());
    }

    /** API명세서.md API 17 성공 응답 예제(3000ms, 추임새 2회, WPM 120)를 그대로 재현하는 골든 테스트. */
    @Test
    void API17_예제와_동일한_지표를_재현한다() {
        int durationMs = 3000;
        List<SpeechInterval> candidateIntervals = List.of(new SpeechInterval(500, 2500));
        List<TimedToken> tokens = List.of(
                new TimedToken("음", 700, 800),
                new TimedToken("저는", 900, 1100),
                new TimedToken("저는", 1200, 1400),
                new TimedToken("사실", 1500, 1700),
                new TimedToken("어", 2100, 2200),
                new TimedToken("홍길동입니다", 2300, 2500)
        );
        double[] amplitudes = buildAmplitudes();

        SpeechMetrics metrics = SpeechHabitsAnalyzer.analyze(durationMs, candidateIntervals, tokens, amplitudes);

        assertEquals(3000, metrics.durationMs());
        assertEquals(2000, metrics.speechDurationMs());
        assertEquals(1000, metrics.silenceDurationMs());
        assertEquals(0, metrics.longSilenceCount());
        assertEquals(1, metrics.repeatedExpressionCount());
        assertEquals(List.of(new SpeechInterval(500, 2500)), metrics.speechIntervals());

        assertEquals(2, metrics.basic().fillerTotalCount());
        assertEquals(
                List.of(new FillerBreakdown("음", 1), new FillerBreakdown("어", 1)),
                metrics.basic().fillerBreakdown()
        );
        assertEquals(
                List.of(new FillerEvent(0, 700, "음"), new FillerEvent(1, 2100, "어")),
                metrics.fillerTimeline()
        );

        assertEquals(4, metrics.speechRate().totalWordCount());
        assertEquals(120, metrics.speechRate().wordsPerMinute());

        assertEquals(
                List.of(
                        new SegmentCount(SegmentType.INITIAL, 1, 0),
                        new SegmentCount(SegmentType.MIDDLE, 0, 1),
                        new SegmentCount(SegmentType.FINAL, 1, 0)
                ),
                metrics.segmentAnalysis()
        );

        assertEquals(30, metrics.waveform().size());
        for (WaveformPoint point : metrics.waveform()) {
            WaveType expected = (point.timeMs() >= 500 && point.timeMs() < 2500) ? WaveType.SPEECH : WaveType.SILENCE;
            assertEquals(expected, point.type(), "timeMs=" + point.timeMs());
        }

        assertTrue(metrics.coaching().summary().length() >= 1 && metrics.coaching().summary().length() <= 1000);
        assertTrue(metrics.coaching().actionItems().size() >= 1 && metrics.coaching().actionItems().size() <= 5);
    }

    private static double[] buildAmplitudes() {
        double[] amplitudes = new double[30];
        for (int i = 0; i < 30; i++) {
            int timeMs = i * 100;
            amplitudes[i] = (timeMs >= 500 && timeMs < 2500) ? 0.65 : 0.02;
        }
        return amplitudes;
    }
}
