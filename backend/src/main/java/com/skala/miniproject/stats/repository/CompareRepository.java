package com.skala.miniproject.stats.repository;

import com.skala.miniproject.domain.recording.Recording;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CompareRepository extends Repository<Recording, Long> {

    @Query(value = """
            select r.id as "recordingId",
                   r.submitted_at as "submittedAt",
                   r.duration_ms as "durationMs",
                   u.plan as "plan",
                   a.status as "status",
                   a.filler_total_count as "fillerTotalCount",
                   a.silence_duration_ms as "silenceDurationMs",
                   a.algorithm_version as "algorithmVersion",
                   p.words_per_minute as "wordsPerMinute"
            from recordings r
            join users u on u.id = r.user_id
            join analyses a on a.recording_id = r.id
            left join analysis_pro_results p on p.analysis_id = a.id
            where r.id = :recordingId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<ComparisonView> findOwnedCurrent(
            @Param("userId") Long userId,
            @Param("recordingId") Long recordingId
    );

    @Query(value = """
            select r.id as "recordingId",
                   r.submitted_at as "submittedAt",
                   r.duration_ms as "durationMs",
                   u.plan as "plan",
                   a.status as "status",
                   a.filler_total_count as "fillerTotalCount",
                   a.silence_duration_ms as "silenceDurationMs",
                   a.algorithm_version as "algorithmVersion",
                   p.words_per_minute as "wordsPerMinute"
            from recordings r
            join users u on u.id = r.user_id
            join analyses a on a.recording_id = r.id
            left join analysis_pro_results p on p.analysis_id = a.id
            where r.id = :targetRecordingId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<ComparisonView> findOwnedTarget(
            @Param("userId") Long userId,
            @Param("targetRecordingId") Long targetRecordingId
    );

    @Query(value = """
            select r.id as "recordingId",
                   r.submitted_at as "submittedAt",
                   r.duration_ms as "durationMs",
                   u.plan as "plan",
                   a.status as "status",
                   a.filler_total_count as "fillerTotalCount",
                   a.silence_duration_ms as "silenceDurationMs",
                   a.algorithm_version as "algorithmVersion",
                   p.words_per_minute as "wordsPerMinute"
            from recordings r
            join users u on u.id = r.user_id
            join analyses a on a.recording_id = r.id
            left join analysis_pro_results p on p.analysis_id = a.id
            where r.user_id = :userId
              and a.status = 'COMPLETED'
              and a.algorithm_version = :algorithmVersion
              and (r.submitted_at < :submittedAt
                   or (r.submitted_at = :submittedAt and r.id < :recordingId))
            order by r.submitted_at desc, r.id desc
            limit 1
            """, nativeQuery = true)
    Optional<ComparisonView> findPreviousCompleted(
            @Param("userId") Long userId,
            @Param("recordingId") Long recordingId,
            @Param("submittedAt") Instant submittedAt,
            @Param("algorithmVersion") String algorithmVersion
    );

    interface ComparisonView {
        Long getRecordingId();

        Instant getSubmittedAt();

        Integer getDurationMs();

        String getPlan();

        String getStatus();

        Integer getFillerTotalCount();

        Integer getSilenceDurationMs();

        String getAlgorithmVersion();

        Integer getWordsPerMinute();
    }
}
