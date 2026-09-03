package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SilenceCalculatorTest {

    @Test
    void 앞뒤_침묵을_포함해_발화와_침묵의_합이_길이와_같다() {
        List<SpeechInterval> speech = List.of(new SpeechInterval(500, 2500));

        SilenceMetrics result = SilenceCalculator.compute(speech, 3000);

        assertEquals(2000, result.speechDurationMs());
        assertEquals(1000, result.silenceDurationMs());
        assertEquals(3000, result.speechDurationMs() + result.silenceDurationMs());
    }

    @Test
    void 긴_침묵만_longSilenceCount에_센다() {
        // 앞 침묵 [0,2000) = 2000ms(카운트), 뒤 침묵 [2100,5000) = 2900ms(카운트) → 2건
        List<SpeechInterval> speech = List.of(new SpeechInterval(2000, 2100));
        SilenceMetrics result = SilenceCalculator.compute(speech, 5000);
        assertEquals(2, result.longSilenceCount());

        // 짧은 침묵(1100ms)은 세지 않는다 — speech [0,100)과 [1200,5000) 사이 [100,1200) 만 침묵
        List<SpeechInterval> speechWithShortGap = List.of(new SpeechInterval(0, 100), new SpeechInterval(1200, 5000));
        SilenceMetrics result2 = SilenceCalculator.compute(speechWithShortGap, 5000);
        assertEquals(0, result2.longSilenceCount());
    }
}
