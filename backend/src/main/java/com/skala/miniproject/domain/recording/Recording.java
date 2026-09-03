package com.skala.miniproject.domain.recording;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 제출 당시 불변 메타데이터. 생성 후 변경 메서드를 두지 않는다 (연관관계 매핑 없음, §9-1). */
@Getter
@Entity
@Table(name = "recordings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "audio_sha256", nullable = false, length = 64)
    private String audioSha256;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public static Recording submit(Long userId, Integer durationMs, String mimeType,
                                    Long fileSizeBytes, String audioSha256, Instant now) {
        Recording r = new Recording();
        r.userId = userId;
        r.durationMs = durationMs;
        r.mimeType = mimeType;
        r.fileSizeBytes = fileSizeBytes;
        r.audioSha256 = audioSha256;
        r.submittedAt = now;
        return r;
    }
}
