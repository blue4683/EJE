package com.skala.miniproject.history.repository;

import com.skala.miniproject.domain.recording.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecordingDeleteRepository extends JpaRepository<Recording, Long> {

    Optional<Recording> findByIdAndUserId(Long id, Long userId);

    @Query(value = """
            select count(*)
            from analyses
            where recording_id = :recordingId
              and status in ('PENDING', 'PROCESSING')
            """, nativeQuery = true)
    long countActiveByRecordingId(@Param("recordingId") Long recordingId);
}
