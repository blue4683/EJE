package com.skala.miniproject.domain.analysis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 완료 시 INITIAL/MIDDLE/FINAL 각 1행, 항상 3행. */
@Getter
@Entity
@Table(name = "segment_analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SegmentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SegmentType segment;

    @Column(name = "filler_count", nullable = false)
    private Integer fillerCount;

    @Column(name = "habit_word_count", nullable = false)
    private Integer habitWordCount;

    public static SegmentAnalysis of(Long analysisId, SegmentType segment, Integer fillerCount, Integer habitWordCount) {
        SegmentAnalysis s = new SegmentAnalysis();
        s.analysisId = analysisId;
        s.segment = segment;
        s.fillerCount = fillerCount;
        s.habitWordCount = habitWordCount;
        return s;
    }
}
