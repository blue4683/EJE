package com.skala.miniproject.domain.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 녹음당 현재 분석 1개. 연관관계 매핑 없이 recordingId 를 원시 컬럼으로 갖는다 (§9-1).
 * 상태 전이(PENDING→PROCESSING→COMPLETED/FAILED, 수동 재시도)는 B4~B8 의 서비스 계층이
 * 이 필드들을 갱신하는 방식으로 구현한다. 여기서는 최초 접수(PENDING) 생성만 제공한다.
 */
@Getter
@Entity
@Table(name = "analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recording_id", nullable = false)
    private Long recordingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "auto_retry_count", nullable = false)
    private Integer autoRetryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 50)
    private FailureCode failureCode;

    @Column(name = "worker_id")
    private UUID workerId;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "execution_deadline_at", nullable = false)
    private Instant executionDeadlineAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "speech_duration_ms")
    private Integer speechDurationMs;

    @Column(name = "silence_duration_ms")
    private Integer silenceDurationMs;

    @Column(name = "filler_total_count")
    private Integer fillerTotalCount;

    @Column(name = "long_silence_count")
    private Integer longSilenceCount;

    @Column(name = "repeated_expression_count")
    private Integer repeatedExpressionCount;

    @Column(name = "algorithm_version", nullable = false, length = 32)
    private String algorithmVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false, length = 10)
    private EngineType engineType;

    @Column(name = "engine_version", nullable = false, length = 100)
    private String engineVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** B4 가 녹음 접수와 같은 트랜잭션에서 호출한다. PENDING 은 worker_id·lease_expires_at 이 NOT NULL 이어야 한다 (ck_analyses_state_fields). */
    public static Analysis pending(Long recordingId, UUID workerId, Instant now,
                                    Instant leaseExpiresAt, Instant executionDeadlineAt,
                                    String algorithmVersion, EngineType engineType, String engineVersion) {
        Analysis a = new Analysis();
        a.recordingId = recordingId;
        a.status = AnalysisStatus.PENDING;
        a.attemptNo = 1;
        a.autoRetryCount = 0;
        a.failureCode = null;
        a.workerId = workerId;
        a.leaseExpiresAt = leaseExpiresAt;
        a.executionDeadlineAt = executionDeadlineAt;
        a.startedAt = null;
        a.finishedAt = null;
        a.algorithmVersion = algorithmVersion;
        a.engineType = engineType;
        a.engineVersion = engineVersion;
        a.createdAt = now;
        a.updatedAt = now;
        return a;
    }
}
