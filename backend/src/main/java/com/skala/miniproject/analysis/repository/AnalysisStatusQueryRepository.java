package com.skala.miniproject.analysis.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * API 08·13(B6)이 공유하는 조회 전용 리포지토리. {@code analyses} 에는 {@code user_id} 가 없어
 * {@code recordings} 를 조인해 소유권을 판정한다. 폴링 대상이므로 쿼리는 1회로 유지한다.
 */
public interface AnalysisStatusQueryRepository extends JpaRepository<Analysis, Long> {

    @Query(value = """
            select a.* from analyses a
            join recordings r on r.id = a.recording_id
            where a.id = :analysisId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<Analysis> findByIdAndUserId(@Param("analysisId") Long analysisId, @Param("userId") Long userId);

    @Query(value = """
            select a.* from analyses a
            join recordings r on r.id = a.recording_id
            where a.recording_id = :recordingId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<Analysis> findByRecordingIdAndUserId(@Param("recordingId") Long recordingId, @Param("userId") Long userId);
}
