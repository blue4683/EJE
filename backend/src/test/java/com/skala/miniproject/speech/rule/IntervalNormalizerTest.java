package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntervalNormalizerTest {

    @Test
    void 겹치는_구간을_병합한다() {
        List<SpeechInterval> raw = List.of(
                new SpeechInterval(100, 300),
                new SpeechInterval(200, 400)
        );

        List<SpeechInterval> result = IntervalNormalizer.normalize(raw, 1000);

        assertEquals(List.of(new SpeechInterval(100, 400)), result);
    }

    @Test
    void 길이_99ms_구간은_제거하고_200ms_간격은_병합한다() {
        // B(49ms)는 100ms 미만이라 제거된다. 제거 "후" 남은 A-C 간격(300ms)은 200ms를 넘으므로 병합되지 않는다.
        // 만약 병합을 먼저 하고 제거를 나중에 했다면 A-B(간격 1ms)가 먼저 합쳐져 결과가 달라진다.
        List<SpeechInterval> raw = List.of(
                new SpeechInterval(0, 300),     // A
                new SpeechInterval(301, 350),   // B, 49ms
                new SpeechInterval(600, 900)    // C
        );

        List<SpeechInterval> result = IntervalNormalizer.normalize(raw, 1000);

        assertEquals(List.of(new SpeechInterval(0, 300), new SpeechInterval(600, 900)), result);
    }

    @Test
    void 빌지_범위를_넘는_구간은_잘라낸다() {
        List<SpeechInterval> raw = List.of(new SpeechInterval(-50, 1200));

        List<SpeechInterval> result = IntervalNormalizer.normalize(raw, 1000);

        assertEquals(List.of(new SpeechInterval(0, 1000)), result);
    }
}
