package com.skala.miniproject.user.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ActiveAnalysisCountRepository extends Repository<Analysis, Long> {

    @Query(value = """
            select count(*) from analyses a
            join recordings r on r.id = a.recording_id
            where r.user_id = :userId and a.status in ('PENDING', 'PROCESSING')
            """, nativeQuery = true)
    long countActiveByUserId(@Param("userId") Long userId);
}
