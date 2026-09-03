package com.skala.miniproject.domain.idempotency;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 성공 접수만 24시간 저장한다. response_status 는 항상 202, expires_at 은 항상
 * created_at + 24h 여야 한다 (ck_idempotency_status·ck_idempotency_expiry) — 이 두 값을
 * 서비스마다 따로 계산하지 않도록 정적 팩터리에서 함께 고정한다.
 */
@Getter
@Entity
@Table(name = "api_idempotency_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiIdempotencyKey {

    private static final int RESPONSE_STATUS = 202;
    private static final Duration TTL = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyOperation operation;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "recording_id")
    private Long recordingId;

    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    /** 최초 202 응답 전체 envelope 의 JSON 문자열. 원본 음성·전사문은 담지 않는다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static ApiIdempotencyKey accept(Long userId, UUID idempotencyKey, IdempotencyOperation operation,
                                            String requestFingerprint, Long recordingId, Long analysisId,
                                            String responseBodyJson, Instant now) {
        ApiIdempotencyKey k = new ApiIdempotencyKey();
        k.userId = userId;
        k.idempotencyKey = idempotencyKey;
        k.operation = operation;
        k.requestFingerprint = requestFingerprint;
        k.recordingId = recordingId;
        k.analysisId = analysisId;
        k.responseStatus = RESPONSE_STATUS;
        k.responseBody = responseBodyJson;
        k.createdAt = now;
        k.expiresAt = now.plus(TTL);
        return k;
    }
}
