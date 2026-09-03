package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechRate;
import com.skala.miniproject.speech.model.TimedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpeechRateCalculatorTest {

    @Test
    void 발화시간_0이면_WPM은_null이다() {
        List<TimedToken> tokens = List.of(new TimedToken("안녕", 0, 100));

        SpeechRate rate = SpeechRateCalculator.compute(tokens, 0);

        assertNull(rate.wordsPerMinute());
        assertEquals(1, rate.totalWordCount());
    }

    @Test
    void WPM은_speechDurationMs로_나눈다() {
        // 추임새 제외 4단어, 발화 2000ms → round(4*60000/2000) = 120 (API명세서.md API17 예제와 동일)
        List<TimedToken> tokens = List.of(
                new TimedToken("음", 700, 800),
                new TimedToken("저는", 900, 1100),
                new TimedToken("저는", 1200, 1400),
                new TimedToken("사실", 1500, 1700),
                new TimedToken("어", 2100, 2200),
                new TimedToken("홍길동입니다", 2300, 2500)
        );

        SpeechRate rate = SpeechRateCalculator.compute(tokens, 2000);

        assertEquals(4, rate.totalWordCount());
        assertEquals(120, rate.wordsPerMinute());
    }
}
