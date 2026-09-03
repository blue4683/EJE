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

/** 관측된 추임새만 한 표현당 1행. 0회 표현은 행을 만들지 않는다. B5 가 COMPLETED 저장 시 함께 커밋한다. */
@Getter
@Entity
@Table(name = "filler_breakdowns")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FillerBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(nullable = false, length = 50)
    private String expression;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    public static FillerBreakdown of(Long analysisId, String expression, Integer occurrenceCount) {
        FillerBreakdown f = new FillerBreakdown();
        f.analysisId = analysisId;
        f.expression = expression;
        f.occurrenceCount = occurrenceCount;
        return f;
    }
}
