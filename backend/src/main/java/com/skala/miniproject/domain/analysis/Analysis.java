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

    /**
     * B4 가 녹음 접수와 같은 트랜잭션에서 호출한다. PENDING 은 worker_id·lease_expires_at 이
     * NOT NULL 이어야 한다 (ck_analyses_state_fields).
     */
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

    /** PENDING → PROCESSING. started_at 을 설정한다 (§상태·작업 수명·재시도 계약). */
    public void startProcessing(Instant now) {
        this.status = AnalysisStatus.PROCESSING;
        this.startedAt = now;
        this.updatedAt = now;
    }

    /** heartbeat 가 5초마다 호출한다. lease 만 갱신하고 다른 필드는 건드리지 않는다. */
    public void renewLease(Instant leaseExpiresAt, Instant now) {
        this.leaseExpiresAt = leaseExpiresAt;
        this.updatedAt = now;
    }

    /** PROCESSING → PROCESSING. 자동 재시도 호출 직전에 호출한다. started_at·기한은 유지한다. */
    public void registerAutoRetry(Instant now) {
        this.autoRetryCount = this.autoRetryCount + 1;
        this.updatedAt = now;
    }

    /**
     * PROCESSING → COMPLETED. ck_analyses_result_presence·ck_analyses_state_fields 를 함께 맞춘다 —
     * 측정값 5개를 전부 채우고 worker_id·lease 를 해제한다.
     */
    public void completeWith(int speechDurationMs, int silenceDurationMs, int fillerTotalCount,
                              int longSilenceCount, int repeatedExpressionCount, Instant now) {
        this.status = AnalysisStatus.COMPLETED;
        this.finishedAt = now;
        this.workerId = null;
        this.leaseExpiresAt = null;
        this.speechDurationMs = speechDurationMs;
        this.silenceDurationMs = silenceDurationMs;
        this.fillerTotalCount = fillerTotalCount;
        this.longSilenceCount = longSilenceCount;
        this.repeatedExpressionCount = repeatedExpressionCount;
        this.updatedAt = now;
    }

    /**
     * PENDING/PROCESSING → FAILED. ck_analyses_state_fields 의 FAILED 조건대로 worker_id·lease 를
     * NULL 로 되돌린다. 측정값은 원래 NULL 이므로 건드리지 않는다.
     */
    public void fail(FailureCode failureCode, Instant now) {
        this.status = AnalysisStatus.FAILED;
        this.failureCode = failureCode;
        this.finishedAt = now;
        this.workerId = null;
        this.leaseExpiresAt = null;
        this.updatedAt = now;
    }
}
