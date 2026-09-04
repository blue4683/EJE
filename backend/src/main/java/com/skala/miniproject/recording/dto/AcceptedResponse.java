package com.skala.miniproject.recording.dto;

/** API 07·09 공통 응답 data 타입 — API명세서.md 「응답 필드 — data」와 정확히 일치한다. */
public record AcceptedResponse(
        String recordingId,
        String analysisId,
        String status,
        int attemptNo,
        int autoRetryCount
) {
}
