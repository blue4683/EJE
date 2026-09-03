package com.skala.miniproject.stats.repository;

import com.skala.miniproject.domain.recording.Recording;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DailyStatsRepository extends Repository<Recording, Long> {

    @Query(value = """
            select (r.submitted_at at time zone 'Asia/Seoul')::date as "date",
                   count(*) as "practiceCount",
                   sum(a.filler_total_count) as "fillerTotalCount"
            from recordings r
            join analyses a on a.recording_id = r.id
            where r.user_id = :userId
              and r.submitted_at >= :fromUtc
              and r.submitted_at < :toUtc
              and a.status = 'COMPLETED'
              and a.algorithm_version = :algorithmVersion
            group by (r.submitted_at at time zone 'Asia/Seoul')::date
            order by (r.submitted_at at time zone 'Asia/Seoul')::date
            """, nativeQuery = true)
    List<DailyStatsView> findDailyStats(
            @Param("userId") Long userId,
            @Param("algorithmVersion") String algorithmVersion,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc
    );

    interface DailyStatsView {
        LocalDate getDate();

        Long getPracticeCount();

        Long getFillerTotalCount();
    }
}
