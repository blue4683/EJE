package com.skala.miniproject.stats.dto;

public record ComparisonResponse(
        ComparisonItemDto current,
        ComparisonItemDto target,
        ComparisonDeltaDto delta,
        String algorithmVersion
) {
}
