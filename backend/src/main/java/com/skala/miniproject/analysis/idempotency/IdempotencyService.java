package com.skala.miniproject.analysis.idempotency;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.idempotency.ApiIdempotencyKey;
import com.skala.miniproject.domain.idempotency.IdempotencyOperation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 멱등 접수 계약(API명세서.md 「멱등 접수 계약 — PostgreSQL」)의 공용 진입점.
 * CREATE_RECORDING(B4)·RETRY_ANALYSIS(B7) 이 이 클래스 하나를 통해서만 지문을 계산하고
 * 기존 키와 비교한다 — 두 오퍼레이션이 같은 규칙을 다르게 구현해 어긋나는 것을 막는다.
 */
@Component
public class IdempotencyService {

    private final IdempotencyKeyWriteRepository repository;

    public IdempotencyService(IdempotencyKeyWriteRepository repository) {
        this.repository = repository;
    }

    /**
     * "비어 있거나 16MiB를 초과한 파일은 지문 조회 전에 거절한다"는 규칙을 강제하는 유일한 통로다.
     * audioSha256 은 반드시 이 메서드를 거쳐야 계산할 수 있게 해서, 호출자가 실수로
     * 지문 계산·멱등 키 조회를 먼저 하고 빈 파일 검사를 나중에(B2 디코딩 시점) 하는 순서 오류를 막는다.
     * 16MiB 상한은 Spring MaxUploadSizeExceededException 이 컨트롤러 진입 전에 이미 걸러내므로
     * 여기서는 하한(빈 파일)만 검사한다.
     */
    public String requireAudioSha256(byte[] audioBytes) {
        if (audioBytes.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_AUDIO);
        }
        return RequestFingerprint.sha256Hex(audioBytes);
    }

    /** 아직 만료되지 않은 사용자·키 조합의 기존 접수를 찾는다. */
    public Optional<ApiIdempotencyKey> findValid(Long userId, UUID idempotencyKey, Instant now) {
        return repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .filter(key -> key.getExpiresAt().isAfter(now));
    }

    /**
     * 짧은 트랜잭션의 users 잠금 안에서만 호출한다 (규칙 3).
     * 만료된 동일 키 행을 삭제해 새 요청으로 취급할 수 있게 한다.
     */
    public void deleteIfExpired(Long userId, UUID idempotencyKey, Instant now) {
        repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .filter(key -> !key.getExpiresAt().isAfter(now))
                .ifPresent(repository::delete);
    }

    /**
     * 규칙 1: 같은 지문이면 최초 응답 전체를 그대로 반환하고, 다르면 409, 참조가 삭제돼 사라졌으면
     * 410 이다. 상태가 그 사이 바뀌었어도 반환값은 항상 최초 202 스냅샷이다.
     */
    public String reproduceOrFail(ApiIdempotencyKey existing, String requestFingerprint) {
        if (!existing.getRequestFingerprint().equals(requestFingerprint)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }
        if (existing.getRecordingId() == null || existing.getAnalysisId() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_GONE);
        }
        return existing.getResponseBody();
    }

    /** 신규 접수를 저장한다. responseBodyJson 은 반환한 202 envelope 문자열 그대로여야 한다. */
    public void saveAccepted(Long userId, UUID idempotencyKey, IdempotencyOperation operation,
                              String requestFingerprint, Long recordingId, Long analysisId,
                              String responseBodyJson, Instant now) {
        repository.save(ApiIdempotencyKey.accept(userId, idempotencyKey, operation,
                requestFingerprint, recordingId, analysisId, responseBodyJson, now));
    }
}
