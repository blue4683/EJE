package com.skala.miniproject.analysis.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * B5 파이프라인(상태 전이·결과 저장)이 쓰는 잠금 전용 인터페이스.
 * AnalysisWriteRepository(B4, 생성·활성 개수 조회)와 책임을 나눈다 — 같은 엔티티에 여러
 * Repository 를 두는 것은 Spring Data 에서 허용된다.
 *
 * 복구기(B8)·다른 인스턴스와 경합할 수 있으므로 모든 상태 전이 전에 이 잠금을 먼저 건다.
 */
public interface AnalysisResultWriteRepository extends JpaRepository<Analysis, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Analysis a where a.id = :id")
    Optional<Analysis> lockById(@Param("id") Long id);
}
