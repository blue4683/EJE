package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.WaveType;
import com.skala.miniproject.speech.model.WaveformPoint;

import java.util.ArrayList;
import java.util.List;

/** 규칙 10 — 100ms 파형. type 은 정규화된 발화 구간과 50% 이상 겹치는지로 정한다. 최대 600점. */
public final class WaveformBuilder {

    private static final int BUCKET_MS = 100;
    private static final int MAX_POINTS = 600;

    private WaveformBuilder() {
    }

    /** amplitudes 는 B2 가 만든 100ms 구간별 RMS(0~1) 배열이다. */
    public static List<WaveformPoint> build(double[] amplitudes, List<SpeechInterval> normalizedSpeechIntervals, int durationMs) {
        int count = Math.min(amplitudes.length, MAX_POINTS);
        List<WaveformPoint> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int bucketStart = i * BUCKET_MS;
            int bucketEnd = Math.min(bucketStart + BUCKET_MS, durationMs);
            WaveType type = overlapsSpeech(bucketStart, bucketEnd, normalizedSpeechIntervals) ? WaveType.SPEECH : WaveType.SILENCE;
            points.add(new WaveformPoint(bucketStart, amplitudes[i], type));
        }
        return List.copyOf(points);
    }

    private static boolean overlapsSpeech(int bucketStart, int bucketEnd, List<SpeechInterval> intervals) {
        int bucketLen = bucketEnd - bucketStart;
        if (bucketLen <= 0) {
            return false;
        }
        int overlap = 0;
        for (SpeechInterval iv : intervals) {
            int os = Math.max(bucketStart, iv.startMs());
            int oe = Math.min(bucketEnd, iv.endMs());
            if (os < oe) {
                overlap += oe - os;
            }
        }
        return overlap * 2 >= bucketLen; // overlap / bucketLen >= 0.5, 정수 연산으로 반올림 오차를 피한다
    }
}
