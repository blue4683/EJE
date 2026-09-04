package com.skala.miniproject.history.dto;

import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.FailureCode;

import java.time.Instant;

public record AnalysisStatusDto(
        String analysisId,
        String recordingId,
        AnalysisStatus status,
        Integer attemptNo,
        Integer autoRetryCount,
        FailureCode failureCode,
        boolean retryable,
        boolean retryRequiresAudio,
        Instant startedAt,
        Instant finishedAt
) {
}
