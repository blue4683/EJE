package com.skala.miniproject.analysis.dto;

import com.skala.miniproject.domain.analysis.Analysis;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.FailureCode;

import java.time.Instant;

/**
 * API 08·13 응답 DTO(명세 DTO명 {@code AnalysisStatus}). 도메인 상태 enum
 * {@link AnalysisStatus} 와 이름이 겹쳐 {@code Response} 접미사로 구분한다.
 */
public record AnalysisStatusResponse(
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

    public static AnalysisStatusResponse from(Analysis analysis) {
        boolean retryable = analysis.getStatus() == AnalysisStatus.FAILED && analysis.getAttemptNo() < 4;
        return new AnalysisStatusResponse(
                String.valueOf(analysis.getId()),
                String.valueOf(analysis.getRecordingId()),
                analysis.getStatus(),
                analysis.getAttemptNo(),
                analysis.getAutoRetryCount(),
                analysis.getFailureCode(),
                retryable,
                retryable,
                analysis.getStartedAt(),
                analysis.getFinishedAt()
        );
    }
}
