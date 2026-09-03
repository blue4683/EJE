package com.skala.miniproject.recording.repository;

import com.skala.miniproject.domain.recording.Recording;
import org.springframework.data.jpa.repository.JpaRepository;

/** RecordingRepository(A 소유, §9-5 메서드 0개 동결)와 별개로 B 가 쓰는 저장 전용 인터페이스. */
public interface RecordingWriteRepository extends JpaRepository<Recording, Long> {
}
