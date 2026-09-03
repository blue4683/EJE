package com.skala.miniproject.analysis.pipeline;

import com.skala.miniproject.analysis.repository.AnalysisResultWriteRepository;
import com.skala.miniproject.domain.analysis.Analysis;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.FailureCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * PENDING/PROCESSING 사이의 상태 전이. 매 전이 전에 analyses 행을 잠그고, 이 인스턴스가 지금도
 * 그 실행의 주인인지(workerId 일치) 다시 확인한다 — 복구기(B8)나 다른 인스턴스와 경합할 수 있어서다.
 * COMPLETED 전이(결과 5테이블 동반)는 AnalysisResultWriter 가 별도로 맡는다.
 */
@Component
public class AnalysisStateMachine {

    private final AnalysisResultWriteRepository repository;
    private final TransactionTemplate transactionTemplate;

    public AnalysisStateMachine(AnalysisResultWriteRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** PENDING → PROCESSING. @return 이 인스턴스가 실제로 소유권을 확보했는가. */
    public boolean beginProcessing(Long analysisId, UUID workerId, Instant now) {
        Boolean began = transactionTemplate.execute(status -> {
            Analysis analysis = repository.lockById(analysisId).orElse(null);
            if (analysis == null || analysis.getStatus() != AnalysisStatus.PENDING
                    || !workerId.equals(analysis.getWorkerId())) {
                return false;
            }
            analysis.startProcessing(now);
            return true;
        });
        return Boolean.TRUE.equals(began);
    }

    /** heartbeat 갱신. 등록된 이 작업이 여전히 이 인스턴스 소유의 PROCESSING 일 때만 갱신한다. */
    public void renewLease(Long analysisId, UUID workerId, Instant leaseExpiresAt, Instant now) {
        transactionTemplate.executeWithoutResult(status -> repository.lockById(analysisId).ifPresent(analysis -> {
            if (analysis.getStatus() == AnalysisStatus.PROCESSING && workerId.equals(analysis.getWorkerId())) {
                analysis.renewLease(leaseExpiresAt, now);
            }
        }));
    }

    /** 자동 재시도 호출 직전에 호출한다. */
    public void registerAutoRetry(Long analysisId, UUID workerId, Instant now) {
        transactionTemplate.executeWithoutResult(status -> repository.lockById(analysisId).ifPresent(analysis -> {
            if (analysis.getStatus() == AnalysisStatus.PROCESSING && workerId.equals(analysis.getWorkerId())) {
                analysis.registerAutoRetry(now);
            }
        }));
    }

    /** 최종 실패로 정리한다. 이미 다른 인스턴스가 처리해 소유권이 없으면 아무것도 하지 않는다. */
    public void fail(Long analysisId, UUID workerId, FailureCode failureCode, Instant now) {
        transactionTemplate.executeWithoutResult(status -> repository.lockById(analysisId).ifPresent(analysis -> {
            boolean active = analysis.getStatus() == AnalysisStatus.PENDING
                    || analysis.getStatus() == AnalysisStatus.PROCESSING;
            if (active && workerId.equals(analysis.getWorkerId())) {
                analysis.fail(failureCode, now);
            }
        }));
    }
}
