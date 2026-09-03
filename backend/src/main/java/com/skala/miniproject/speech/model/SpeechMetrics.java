package com.skala.miniproject.speech.model;

import java.util.List;

/** API명세서.md DTO Metrics 와 동일한 모양의 값 객체. */
public record SpeechMetrics(
        int durationMs,
        int speechDurationMs,
        int silenceDurationMs,
        int longSilenceCount,
        int repeatedExpressionCount,
        Basic basic,
        List<SpeechInterval> speechIntervals,
        List<WaveformPoint> waveform,
        List<FillerEvent> fillerTimeline,
        SpeechRate speechRate,
        List<SegmentCount> segmentAnalysis,
        Coaching coaching
) {}
