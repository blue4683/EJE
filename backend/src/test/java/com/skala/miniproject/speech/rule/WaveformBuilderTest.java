package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.WaveType;
import com.skala.miniproject.speech.model.WaveformPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaveformBuilderTest {

    @Test
    void 파형은_최대_600점이고_마지막만_짧을_수_있다() {
        double[] amplitudes = new double[650]; // 600 을 넘겨서 넣어도 잘린다

        List<WaveformPoint> points = WaveformBuilder.build(amplitudes, List.of(), 65000);

        assertEquals(600, points.size());
        assertEquals(59900, points.get(599).timeMs());
    }

    @Test
    void 구간의_50퍼센트_이상_겹치면_SPEECH다() {
        double[] amplitudes = {0.1, 0.1, 0.1};
        List<SpeechInterval> speech = List.of(new SpeechInterval(150, 300));

        List<WaveformPoint> points = WaveformBuilder.build(amplitudes, speech, 300);

        assertEquals(WaveType.SILENCE, points.get(0).type()); // [0,100) 겹침 0%
        assertEquals(WaveType.SPEECH, points.get(1).type());  // [100,200) 겹침 50%(150~200)
        assertEquals(WaveType.SPEECH, points.get(2).type());  // [200,300) 겹침 100%
    }
}
