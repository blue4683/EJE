package com.skala.miniproject.history.repository;

import com.skala.miniproject.domain.analysis.Analysis;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalysisQueryRepository extends Repository<Analysis, Long> {

    @Query(value = """
            select expression as "expression", occurrence_count as "count"
            from filler_breakdowns
            where analysis_id = :analysisId
            order by occurrence_count desc, expression asc
            """, nativeQuery = true)
    List<FillerBreakdownView> findFillerBreakdown(@Param("analysisId") Long analysisId);

    interface FillerBreakdownView {
        String getExpression();

        Integer getCount();
    }
}
