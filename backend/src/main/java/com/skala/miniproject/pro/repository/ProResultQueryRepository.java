package com.skala.miniproject.pro.repository;

import com.skala.miniproject.domain.analysis.AnalysisProResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProResultQueryRepository extends Repository<AnalysisProResult, Long> {

    @Query(value = """
            select r.id as "recordingId",
                   r.duration_ms as "durationMs",
                   u.plan as "plan",
                   a.id as "analysisId",
                   a.status as "status",
                   a.speech_duration_ms as "speechDurationMs",
                   a.silence_duration_ms as "silenceDurationMs",
                   a.filler_total_count as "fillerTotalCount",
                   a.long_silence_count as "longSilenceCount",
                   a.repeated_expression_count as "repeatedExpressionCount",
                   a.algorithm_version as "algorithmVersion",
                   a.engine_type as "engineType",
                   a.engine_version as "engineVersion",
                   p.id as "proResultId",
                   p.words_per_minute as "wordsPerMinute",
                   p.total_word_count as "totalWordCount",
                   p.speech_intervals as "speechIntervals",
                   p.waveform as "waveform",
                   p.coaching_summary as "coachingSummary",
                   p.coaching_practice_recommendation as "coachingPracticeRecommendation"
            from recordings r
            join users u on u.id = r.user_id
            join analyses a on a.recording_id = r.id
            left join analysis_pro_results p on p.analysis_id = a.id
            where r.id = :recordingId and r.user_id = :userId
            """, nativeQuery = true)
    Optional<ProResultView> findOwnedResult(
            @Param("userId") Long userId,
            @Param("recordingId") Long recordingId
    );

    @Query(value = """
            select expression as "expression", occurrence_count as "count"
            from filler_breakdowns
            where analysis_id = :analysisId
            order by occurrence_count desc, expression asc
            """, nativeQuery = true)
    List<FillerBreakdownView> findFillerBreakdown(@Param("analysisId") Long analysisId);

    @Query(value = """
            select event_index as "eventIndex", time_ms as "timeMs", expression as "expression"
            from filler_timeline_events
            where analysis_id = :analysisId
            order by event_index asc
            """, nativeQuery = true)
    List<FillerEventView> findFillerTimeline(@Param("analysisId") Long analysisId);

    @Query(value = """
            select segment as "segment", filler_count as "fillerCount", habit_word_count as "habitWordCount"
            from segment_analyses
            where analysis_id = :analysisId
            order by case segment when 'INITIAL' then 0 when 'MIDDLE' then 1 else 2 end
            """, nativeQuery = true)
    List<SegmentView> findSegments(@Param("analysisId") Long analysisId);

    @Query(value = """
            select content
            from coaching_action_items
            where pro_result_id = :proResultId
            order by sort_order asc
            """, nativeQuery = true)
    List<String> findCoachingActionItems(@Param("proResultId") Long proResultId);

    interface ProResultView {
        Long getRecordingId();

        Integer getDurationMs();

        String getPlan();

        Long getAnalysisId();

        String getStatus();

        Integer getSpeechDurationMs();

        Integer getSilenceDurationMs();

        Integer getFillerTotalCount();

        Integer getLongSilenceCount();

        Integer getRepeatedExpressionCount();

        String getAlgorithmVersion();

        String getEngineType();

        String getEngineVersion();

        Long getProResultId();

        Integer getWordsPerMinute();

        Integer getTotalWordCount();

        String getSpeechIntervals();

        String getWaveform();

        String getCoachingSummary();

        String getCoachingPracticeRecommendation();
    }

    interface FillerBreakdownView {
        String getExpression();

        Integer getCount();
    }

    interface FillerEventView {
        Integer getEventIndex();

        Integer getTimeMs();

        String getExpression();
    }

    interface SegmentView {
        String getSegment();

        Integer getFillerCount();

        Integer getHabitWordCount();
    }
}
