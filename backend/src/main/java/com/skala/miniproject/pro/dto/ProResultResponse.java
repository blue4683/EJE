package com.skala.miniproject.pro.dto;

import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.EngineType;

public record ProResultResponse(
        String recordingId,
        String analysisId,
        AnalysisStatus status,
        String algorithmVersion,
        EngineType engineType,
        String engineVersion,
        MetricsDto metrics
) {
}
