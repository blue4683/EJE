package com.skala.miniproject.pro.dto;

import com.skala.miniproject.history.dto.BasicDto;

import java.util.List;

public record MetricsDto(
        int durationMs,
        int speechDurationMs,
        int silenceDurationMs,
        int longSilenceCount,
        int repeatedExpressionCount,
        BasicDto basic,
        List<SpeechIntervalDto> speechIntervals,
        List<WaveformPointDto> waveform,
        List<FillerEventDto> fillerTimeline,
        SpeechRateDto speechRate,
        List<SegmentDto> segmentAnalysis,
        CoachingDto coaching
) {
}
