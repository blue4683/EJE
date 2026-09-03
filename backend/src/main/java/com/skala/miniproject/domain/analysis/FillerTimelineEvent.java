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

/** 검출된 추임새 전부 저장. event_index 는 0부터 연속(같은 시각도 허용). */
@Getter
@Entity
@Table(name = "filler_timeline_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FillerTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "event_index", nullable = false)
    private Integer eventIndex;

    @Column(name = "time_ms", nullable = false)
    private Integer timeMs;

    @Column(nullable = false, length = 50)
    private String expression;

    public static FillerTimelineEvent of(Long analysisId, Integer eventIndex, Integer timeMs, String expression) {
        FillerTimelineEvent e = new FillerTimelineEvent();
        e.analysisId = analysisId;
        e.eventIndex = eventIndex;
        e.timeMs = timeMs;
        e.expression = expression;
        return e;
    }
}
