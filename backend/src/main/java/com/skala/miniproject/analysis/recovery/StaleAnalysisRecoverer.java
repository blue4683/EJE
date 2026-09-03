package com.skala.miniproject.analysis.recovery;

import com.skala.miniproject.analysis.repository.AnalysisRecoveryRepository;
import com.skala.miniproject.domain.analysis.FailureCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StaleAnalysisRecoverer {

    private static final long RECOVERY_DELAY_MILLIS = 5_000L;

    private final AnalysisRecoveryRepository recoveryRepository;
    private final TransactionTemplate transactionTemplate;

    public StaleAnalysisRecoverer(
            AnalysisRecoveryRepository recoveryRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.recoveryRepository = recoveryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(
            fixedDelay = RECOVERY_DELAY_MILLIS,
            initialDelay = RECOVERY_DELAY_MILLIS
    )
    public void recoverStaleAnalyses() {
        try {
            List<Long> recoveredIds = transactionTemplate.execute(status -> recover(Instant.now()));
            if (recoveredIds != null && !recoveredIds.isEmpty()) {
                log.info("만료 분석 복구 완료: count={}, analysisIds={}", recoveredIds.size(), recoveredIds);
            }
        } catch (Exception e) {
            log.error("만료 분석 복구 주기 실행 실패", e);
        }
    }

    private List<Long> recover(Instant now) {
        var staleAnalyses = recoveryRepository.lockStaleAnalyses(now);
        List<Long> recoveredIds = new ArrayList<>(staleAnalyses.size());

        for (var analysis : staleAnalyses) {
            FailureCode failureCode = analysis.getExecutionDeadlineAt().isBefore(now)
                    ? FailureCode.ANALYSIS_TIMEOUT
                    : FailureCode.WORKER_LOST;
            analysis.fail(failureCode, now);
            recoveredIds.add(analysis.getId());
        }
        return List.copyOf(recoveredIds);
    }
}
