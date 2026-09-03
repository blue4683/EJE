package com.skala.miniproject.analysis.pipeline;

import com.skala.miniproject.analysis.repository.AnalysisResultWriteRepository;
import com.skala.miniproject.domain.analysis.Analysis;
import com.skala.miniproject.domain.analysis.AnalysisProResult;
import com.skala.miniproject.domain.analysis.AnalysisProResultRepository;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.CoachingActionItem;
import com.skala.miniproject.domain.analysis.CoachingActionItemRepository;
import com.skala.miniproject.domain.analysis.FillerBreakdown;
import com.skala.miniproject.domain.analysis.FillerBreakdownRepository;
import com.skala.miniproject.domain.analysis.FillerTimelineEvent;
import com.skala.miniproject.domain.analysis.FillerTimelineEventRepository;
import com.skala.miniproject.domain.analysis.SegmentAnalysis;
import com.skala.miniproject.domain.analysis.SegmentAnalysisRepository;
import com.skala.miniproject.domain.analysis.SegmentType;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SpeechMetrics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PROCESSING → COMPLETED. B1 이 이미 규칙11 불변식을 검증했으므로(SpeechHabitsAnalyzer.analyze
 * 내부), 여기서는 "이 인스턴스가 지금도 이 실행의 주인인가"만 다시 확인하고 결과 5테이블 + analyses
 * 측정값 + COMPLETED 를 한 트랜잭션에 커밋한다 (§5-7).
 */
@Component
public class AnalysisResultWriter {

    private final AnalysisResultWriteRepository analysisRepository;
    private final AnalysisProResultRepository proResultRepository;
    private final SegmentAnalysisRepository segmentAnalysisRepository;
    private final FillerTimelineEventRepository timelineRepository;
    private final FillerBreakdownRepository fillerBreakdownRepository;
    private final CoachingActionItemRepository actionItemRepository;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;

    public AnalysisResultWriter(
            AnalysisResultWriteRepository analysisRepository,
            AnalysisProResultRepository proResultRepository,
            SegmentAnalysisRepository segmentAnalysisRepository,
            FillerTimelineEventRepository timelineRepository,
            FillerBreakdownRepository fillerBreakdownRepository,
            CoachingActionItemRepository actionItemRepository,
            JsonMapper jsonMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.analysisRepository = analysisRepository;
        this.proResultRepository = proResultRepository;
        this.segmentAnalysisRepository = segmentAnalysisRepository;
        this.timelineRepository = timelineRepository;
        this.fillerBreakdownRepository = fillerBreakdownRepository;
        this.actionItemRepository = actionItemRepository;
        this.jsonMapper = jsonMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * @return true 면 저장에 성공했다. false 면 이 인스턴스가 더 이상 이 실행의 주인이 아니라서
     * (이전 차수의 늦은 응답, 복구기와의 경합 등) 결과를 버렸다는 뜻이다 — 호출자는 예외 없이 무시한다.
     */
    public boolean save(Long analysisId, int expectedAttemptNo, int expectedAutoRetryCount,
                         UUID expectedWorkerId, SpeechMetrics metrics, Instant now) {
        Boolean saved = transactionTemplate.execute(status ->
                trySave(analysisId, expectedAttemptNo, expectedAutoRetryCount, expectedWorkerId, metrics, now));
        return Boolean.TRUE.equals(saved);
    }

    private boolean trySave(Long analysisId, int expectedAttemptNo, int expectedAutoRetryCount,
                             UUID expectedWorkerId, SpeechMetrics metrics, Instant now) {
        Analysis analysis = analysisRepository.lockById(analysisId).orElse(null);
        if (analysis == null || !stillOwned(analysis, expectedAttemptNo, expectedAutoRetryCount, expectedWorkerId, now)) {
            return false;
        }

        AnalysisProResult proResult = proResultRepository.save(AnalysisProResult.of(
                analysisId,
                metrics.speechRate().wordsPerMinute(),
                metrics.speechRate().totalWordCount(),
                toJson(metrics.speechIntervals()),
                toJson(metrics.waveform()),
                metrics.coaching().summary(),
                metrics.coaching().practiceRecommendation(),
                now));

        List<CoachingActionItem> actionItems = new ArrayList<>();
        List<String> items = metrics.coaching().actionItems();
        for (int i = 0; i < items.size(); i++) {
            actionItems.add(CoachingActionItem.of(proResult.getId(), i, items.get(i)));
        }
        actionItemRepository.saveAll(actionItems);

        segmentAnalysisRepository.saveAll(metrics.segmentAnalysis().stream()
                .map(segment -> toSegmentAnalysis(analysisId, segment))
                .toList());

        timelineRepository.saveAll(metrics.fillerTimeline().stream()
                .map(event -> toTimelineEvent(analysisId, event))
                .toList());

        fillerBreakdownRepository.saveAll(metrics.basic().fillerBreakdown().stream()
                .map(fb -> FillerBreakdown.of(analysisId, fb.expression(), fb.count()))
                .toList());

        analysis.completeWith(metrics.speechDurationMs(), metrics.silenceDurationMs(), metrics.basic().fillerTotalCount(),
                metrics.longSilenceCount(), metrics.repeatedExpressionCount(), now);
        return true;
    }

    private boolean stillOwned(Analysis analysis, int expectedAttemptNo, int expectedAutoRetryCount,
                                UUID expectedWorkerId, Instant now) {
        return analysis.getStatus() == AnalysisStatus.PROCESSING
                && analysis.getAttemptNo() == expectedAttemptNo
                && analysis.getAutoRetryCount() == expectedAutoRetryCount
                && expectedWorkerId.equals(analysis.getWorkerId())
                && analysis.getLeaseExpiresAt() != null && analysis.getLeaseExpiresAt().isAfter(now)
                && analysis.getExecutionDeadlineAt().isAfter(now);
    }

    private SegmentAnalysis toSegmentAnalysis(Long analysisId, SegmentCount segment) {
        SegmentType domainSegmentType = SegmentType.valueOf(segment.segment().name());
        return SegmentAnalysis.of(analysisId, domainSegmentType, segment.fillerCount(), segment.habitWordCount());
    }

    private FillerTimelineEvent toTimelineEvent(Long analysisId, FillerEvent event) {
        return FillerTimelineEvent.of(analysisId, event.eventIndex(), event.timeMs(), event.expression());
    }

    private String toJson(Object value) {
        return jsonMapper.writeValueAsString(value);
    }
}
