package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.TimedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepetitionCounterTest {

    @Test
    void 동일_토큰_3연속은_2회다() {
        List<TimedToken> tokens = List.of(
                new TimedToken("가", 0, 100),
                new TimedToken("가", 200, 300),
                new TimedToken("가", 400, 500)
        );

        assertEquals(2, RepetitionCounter.count(tokens));
    }

    @Test
    void 중간에_추임새가_끼면_연속성이_끊긴다() {
        List<TimedToken> tokens = List.of(
                new TimedToken("가", 0, 100),
                new TimedToken("어", 200, 300),
                new TimedToken("가", 400, 500)
        );

        assertEquals(0, RepetitionCounter.count(tokens));
    }

    @Test
    void 간격이_1001ms면_반복이_아니다() {
        List<TimedToken> tokens = List.of(
                new TimedToken("가", 0, 100),
                new TimedToken("가", 1101, 1200) // 간격 = 1101-100 = 1001
        );

        assertEquals(0, RepetitionCounter.count(tokens));
    }
}
