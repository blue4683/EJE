package com.skala.miniproject.analysis.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * AnalysisRepository(§9-5 메서드 0개 동결)와 별개로 B(B4·B6·B7)가 공유하는 쓰기·조회 전용 인터페이스.
 */
public interface AnalysisWriteRepository extends JpaRepository<Analysis, Long> {

    /** analyses 에는 user_id 가 없어 recordings 를 조인해 사용자당 활성 분석 수를 센다 (§C4). */
    @Query(value = """
            select count(*) from analyses a
            join recordings r on r.id = a.recording_id
            where r.user_id = :userId and a.status in ('PENDING', 'PROCESSING')
            """, nativeQuery = true)
    long countActiveByUserId(@Param("userId") Long userId);

    /** B7 — 재시도 대상의 소유권 사전 검사. analyses 에는 user_id 가 없어 recordings 를 조인한다. */
    @Query(value = """
            select a.* from analyses a
            join recordings r on r.id = a.recording_id
            where a.id = :analysisId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<Analysis> findByIdAndUserId(@Param("analysisId") Long analysisId, @Param("userId") Long userId);

    /**
     * B7 — §C4 락 순서(users → analyses)의 두 번째 잠금. users 를 잠근 뒤 이 잠금으로 갱신 직전
     * 상태·attemptNo 를 다시 확인해, 사전 검사와 커밋 사이에 경합으로 상태가 바뀐 요청이
     * 그대로 덮어쓰지 못하게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Analysis a where a.id = :id")
    Optional<Analysis> lockById(@Param("id") Long id);
}
