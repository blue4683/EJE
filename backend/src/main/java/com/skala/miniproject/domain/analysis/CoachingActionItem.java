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

/** 완료 시 1~5행. sort_order 는 0부터 연속. 전체 전사문·긴 원문 인용을 포함하지 않는다. */
@Getter
@Entity
@Table(name = "coaching_action_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pro_result_id", nullable = false)
    private Long proResultId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private String content;

    public static CoachingActionItem of(Long proResultId, Integer sortOrder, String content) {
        CoachingActionItem c = new CoachingActionItem();
        c.proResultId = proResultId;
        c.sortOrder = sortOrder;
        c.content = content;
        return c;
    }
}
