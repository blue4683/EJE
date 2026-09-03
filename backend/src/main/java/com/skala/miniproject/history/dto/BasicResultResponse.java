package com.skala.miniproject.history.dto;

import com.skala.miniproject.domain.analysis.AnalysisStatus;

public record BasicResultResponse(
        String recordingId,
        String analysisId,
        AnalysisStatus status,
        BasicDto basic,
        ProAccessDto pro
) {
}
