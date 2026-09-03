package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;

import java.util.ArrayList;
import java.util.List;

/** 규칙 3 — 정규화된 발화 구간의 여집합으로 침묵을 계산한다. 처음·끝 침묵도 포함한다. */
public final class SilenceCalculator {

    private static final int LONG_SILENCE_MS = 2000;

    private SilenceCalculator() {
    }

    public static SilenceMetrics compute(List<SpeechInterval> normalizedSpeechIntervals, int durationMs) {
        int speechDurationMs = 0;
        for (SpeechInterval iv : normalizedSpeechIntervals) {
            speechDurationMs += iv.endMs() - iv.startMs();
        }
        int silenceDurationMs = durationMs - speechDurationMs;

        List<SpeechInterval> silenceIntervals = new ArrayList<>();
        int cursor = 0;
        for (SpeechInterval iv : normalizedSpeechIntervals) {
            if (iv.startMs() > cursor) {
                silenceIntervals.add(new SpeechInterval(cursor, iv.startMs()));
            }
            cursor = iv.endMs();
        }
        if (cursor < durationMs) {
            silenceIntervals.add(new SpeechInterval(cursor, durationMs));
        }

        int longSilenceCount = 0;
        for (SpeechInterval iv : silenceIntervals) {
            if (iv.endMs() - iv.startMs() >= LONG_SILENCE_MS) {
                longSilenceCount++;
            }
        }

        return new SilenceMetrics(speechDurationMs, silenceDurationMs, longSilenceCount);
    }
}
