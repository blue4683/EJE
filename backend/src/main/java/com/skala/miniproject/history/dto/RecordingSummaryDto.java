package com.skala.miniproject.history.dto;

import com.skala.miniproject.domain.analysis.AnalysisStatus;

import java.time.Instant;

public record RecordingSummaryDto(
        String recordingId,
        Instant submittedAt,
        Integer durationMs,
        AnalysisStatus status,
        Integer fillerTotalCount
) {
}
