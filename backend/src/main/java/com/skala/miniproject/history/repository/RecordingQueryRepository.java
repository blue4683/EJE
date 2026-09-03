package com.skala.miniproject.history.repository;

import com.skala.miniproject.domain.recording.Recording;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RecordingQueryRepository extends Repository<Recording, Long> {

    @Query(value = """
            select r.id as "recordingId",
                   r.submitted_at as "submittedAt",
                   r.duration_ms as "durationMs",
                   r.mime_type as "mimeType",
                   r.file_size_bytes as "fileSizeBytes",
                   a.id as "analysisId",
                   a.status as "status",
                   a.attempt_no as "attemptNo",
                   a.auto_retry_count as "autoRetryCount",
                   a.failure_code as "failureCode",
                   a.started_at as "startedAt",
                   a.finished_at as "finishedAt",
                   a.filler_total_count as "fillerTotalCount",
                   a.algorithm_version as "algorithmVersion",
                   a.engine_type as "engineType",
                   a.engine_version as "engineVersion"
            from recordings r
            join analyses a on a.recording_id = r.id
            where r.id = :recordingId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<RecordingAnalysisView> findOwnedDetail(
            @Param("userId") Long userId,
            @Param("recordingId") Long recordingId
    );

    @Query(value = """
            select r.id as "recordingId",
                   r.submitted_at as "submittedAt",
                   r.duration_ms as "durationMs",
                   r.mime_type as "mimeType",
                   r.file_size_bytes as "fileSizeBytes",
                   a.id as "analysisId",
                   a.status as "status",
                   a.attempt_no as "attemptNo",
                   a.auto_retry_count as "autoRetryCount",
                   a.failure_code as "failureCode",
                   a.started_at as "startedAt",
                   a.finished_at as "finishedAt",
                   a.filler_total_count as "fillerTotalCount",
                   a.algorithm_version as "algorithmVersion",
                   a.engine_type as "engineType",
                   a.engine_version as "engineVersion"
            from recordings r
            join analyses a on a.recording_id = r.id
            where r.user_id = :userId
            order by r.submitted_at desc, r.id desc
            """, nativeQuery = true)
    List<RecordingAnalysisView> findOwnedPage(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query(value = "select count(*) from recordings where user_id = :userId", nativeQuery = true)
    long countOwned(@Param("userId") Long userId);

    @Query(value = """
            select r.id as "recordingId",
                   r.submitted_at as "submittedAt",
                   r.duration_ms as "durationMs",
                   r.mime_type as "mimeType",
                   r.file_size_bytes as "fileSizeBytes",
                   a.id as "analysisId",
                   a.status as "status",
                   a.attempt_no as "attemptNo",
                   a.auto_retry_count as "autoRetryCount",
                   a.failure_code as "failureCode",
                   a.started_at as "startedAt",
                   a.finished_at as "finishedAt",
                   a.filler_total_count as "fillerTotalCount",
                   a.algorithm_version as "algorithmVersion",
                   a.engine_type as "engineType",
                   a.engine_version as "engineVersion"
            from recordings r
            join analyses a on a.recording_id = r.id
            where r.user_id = :userId and a.status = 'COMPLETED'
            order by r.submitted_at desc, r.id desc
            limit 3
            """, nativeQuery = true)
    List<RecordingAnalysisView> findRecentCompleted(@Param("userId") Long userId);

    interface RecordingAnalysisView {
        Long getRecordingId();

        Instant getSubmittedAt();

        Integer getDurationMs();

        String getMimeType();

        Long getFileSizeBytes();

        Long getAnalysisId();

        String getStatus();

        Integer getAttemptNo();

        Integer getAutoRetryCount();

        String getFailureCode();

        Instant getStartedAt();

        Instant getFinishedAt();

        Integer getFillerTotalCount();

        String getAlgorithmVersion();

        String getEngineType();

        String getEngineVersion();
    }
}
