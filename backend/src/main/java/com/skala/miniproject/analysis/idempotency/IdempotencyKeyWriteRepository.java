package com.skala.miniproject.analysis.idempotency;

import com.skala.miniproject.domain.idempotency.ApiIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * ApiIdempotencyKeyRepository(A 소유, §9-5 메서드 0개 동결)와 별개로
 * B(B4·B7)가 쓰는 조회·저장·삭제 전용 인터페이스. 같은 엔티티에 여러 Repository 를 두는 것은
 * Spring Data 에서 허용된다 (00-공통기반.md §5-13).
 */
public interface IdempotencyKeyWriteRepository extends JpaRepository<ApiIdempotencyKey, Long> {

    Optional<ApiIdempotencyKey> findByUserIdAndIdempotencyKey(Long userId, UUID idempotencyKey);
}
