package com.skala.miniproject.analysis.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
