package com.skala.miniproject.analysis.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AnalysisRecoveryRepository extends Repository<Analysis, Long> {

    @Query(value = """
            select a.*
            from analyses a
            where a.status in ('PENDING', 'PROCESSING')
              and (a.lease_expires_at < :now or a.execution_deadline_at < :now)
            for update skip locked
            limit 50
            """, nativeQuery = true)
    List<Analysis> lockStaleAnalyses(@Param("now") Instant now);

    @Modifying
    @Query(value = """
            with expired as (
                select id
                from api_idempotency_keys
                where expires_at < :now
                order by expires_at
                for update skip locked
                limit 1000
            )
            delete from api_idempotency_keys keys
            using expired
            where keys.id = expired.id
            """, nativeQuery = true)
    int deleteExpiredIdempotencyKeys(@Param("now") Instant now);
}
