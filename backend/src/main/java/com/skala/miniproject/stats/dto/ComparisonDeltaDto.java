package com.skala.miniproject.stats.dto;

public record ComparisonDeltaDto(
        int fillerCountChange,
        int silenceDurationMsChange,
        Integer wordsPerMinuteChange
) {
}
