package com.skala.miniproject.domain.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** FREE/PRO 모두 완료 시 생성. 공개 조회는 현재 PRO 사용자에게만 허용한다. */
@Getter
@Entity
@Table(name = "analysis_pro_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisProResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "words_per_minute")
    private Integer wordsPerMinute;

    @Column(name = "total_word_count", nullable = false)
    private Integer totalWordCount;

    /** startMs/endMs 객체 배열의 JSON 문자열. 오름차순·반열린 구간·비중첩. 무음이면 빈 배열. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "speech_intervals", nullable = false)
    private String speechIntervals;

    /** 100ms 간격 RMS 점 배열(timeMs, amplitude, type)의 JSON 문자열. 최대 600점. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String waveform;

    @Column(name = "coaching_summary", nullable = false)
    private String coachingSummary;

    @Column(name = "coaching_practice_recommendation", nullable = false)
    private String coachingPracticeRecommendation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AnalysisProResult of(Long analysisId, Integer wordsPerMinute, Integer totalWordCount,
                                        String speechIntervalsJson, String waveformJson,
                                        String coachingSummary, String coachingPracticeRecommendation,
                                        Instant now) {
        AnalysisProResult r = new AnalysisProResult();
        r.analysisId = analysisId;
        r.wordsPerMinute = wordsPerMinute;
        r.totalWordCount = totalWordCount;
        r.speechIntervals = speechIntervalsJson;
        r.waveform = waveformJson;
        r.coachingSummary = coachingSummary;
        r.coachingPracticeRecommendation = coachingPracticeRecommendation;
        r.createdAt = now;
        return r;
    }
}
