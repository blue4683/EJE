package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.FillerBreakdown;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SegmentType;
import com.skala.miniproject.speech.model.TimedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FillerCounterTest {

    @Test
    void 어제와_음악은_추임새가_아니다() {
        List<TimedToken> tokens = List.of(
                new TimedToken("어제", 0, 100),
                new TimedToken("음악", 200, 300)
        );

        assertTrue(FillerCounter.breakdown(tokens).isEmpty());
    }

    @Test
    void breakdown은_count_내림차순_동률이면_음_먼저() {
        // 동률: 어 1, 음 1 → API명세서.md 규칙5 원문(음, 어 순)에 따라 음이 먼저다.
        List<TimedToken> tie = List.of(new TimedToken("어", 0, 100), new TimedToken("음", 200, 300));
        assertEquals(
                List.of(new FillerBreakdown("음", 1), new FillerBreakdown("어", 1)),
                FillerCounter.breakdown(tie)
        );

        // count 내림차순: 어 2, 음 1
        List<TimedToken> desc = List.of(
                new TimedToken("어", 0, 100), new TimedToken("어", 200, 300), new TimedToken("음", 400, 500)
        );
        assertEquals(
                List.of(new FillerBreakdown("어", 2), new FillerBreakdown("음", 1)),
                FillerCounter.breakdown(desc)
        );
    }

    @Test
    void 그러니까_약간_사실은_습관어로_따로_센다() {
        List<TimedToken> tokens = List.of(
                new TimedToken("그러니까", 0, 100),
                new TimedToken("약간", 200, 300),
                new TimedToken("사실", 400, 500)
        );

        assertTrue(FillerCounter.breakdown(tokens).isEmpty());
        List<SegmentCount> segments = FillerCounter.segments(tokens, 1500);
        int totalHabit = segments.stream().mapToInt(SegmentCount::habitWordCount).sum();
        int totalFiller = segments.stream().mapToInt(SegmentCount::fillerCount).sum();
        assertEquals(3, totalHabit);
        assertEquals(0, totalFiller);
    }

    @Test
    void 구간은_횟수가_0이어도_항상_3개다() {
        List<SegmentCount> segments = FillerCounter.segments(List.of(), 3000);

        assertEquals(3, segments.size());
        assertEquals(SegmentType.INITIAL, segments.get(0).segment());
        assertEquals(SegmentType.MIDDLE, segments.get(1).segment());
        assertEquals(SegmentType.FINAL, segments.get(2).segment());
    }

    @Test
    void eventIndex는_시간순_0부터_연속이다() {
        List<TimedToken> tokens = List.of(
                new TimedToken("음", 700, 800),
                new TimedToken("안녕", 900, 1000),
                new TimedToken("어", 2100, 2200)
        );

        List<FillerEvent> timeline = FillerCounter.timeline(tokens);

        assertEquals(2, timeline.size());
        assertEquals(new FillerEvent(0, 700, "음"), timeline.get(0));
        assertEquals(new FillerEvent(1, 2100, "어"), timeline.get(1));
    }
}
