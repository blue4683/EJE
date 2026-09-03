package com.skala.miniproject.history.dto;

import com.skala.miniproject.domain.analysis.EngineType;

import java.time.Instant;

public record RecordingDetailResponse(
        String recordingId,
        Instant submittedAt,
        Integer durationMs,
        String mimeType,
        Long fileSizeBytes,
        AnalysisStatusDto analysis,
        BasicDto basic,
        ProAccessDto pro,
        String algorithmVersion,
        EngineType engineType,
        String engineVersion
) {
}
