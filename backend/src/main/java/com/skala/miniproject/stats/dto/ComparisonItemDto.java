package com.skala.miniproject.stats.dto;

public record ComparisonItemDto(
        String recordingId,
        int durationMs,
        int fillerTotalCount,
        int silenceDurationMs,
        Integer wordsPerMinute
) {
}
