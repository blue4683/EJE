package com.skala.miniproject.analysis.client;

import java.time.Instant;

/**
 * SpeechAnalysisClient 의 입력. Phase 2 내부 API(API명세서.md 「Phase 1 Mock 및 Phase 2 내부 분석
 * API」)의 metadata 필드를 그대로 옮긴 값 객체다 — 실제 구현체로 바뀌어도 이 시그니처는 유지된다.
 */
public record SpeechAnalysisRequest(
        Long recordingId,
        Long analysisId,
        int attemptNo,
        int autoRetryCount,
        int durationMs,
        byte[] audioBytes,
        String algorithmVersion,
        String expectedEngineVersion,
        Instant deadlineAt
) {
}
