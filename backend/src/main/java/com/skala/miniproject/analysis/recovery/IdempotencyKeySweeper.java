package com.skala.miniproject.analysis.recovery;

import com.skala.miniproject.analysis.repository.AnalysisRecoveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Slf4j
@Component
public class IdempotencyKeySweeper {

    private static final long SWEEP_DELAY_MILLIS = 60_000L;

    private final AnalysisRecoveryRepository recoveryRepository;
    private final TransactionTemplate transactionTemplate;

    public IdempotencyKeySweeper(
            AnalysisRecoveryRepository recoveryRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.recoveryRepository = recoveryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(
            fixedDelay = SWEEP_DELAY_MILLIS,
            initialDelay = SWEEP_DELAY_MILLIS
    )
    public void sweepExpiredKeys() {
        try {
            Integer deletedCount = transactionTemplate.execute(
                    status -> recoveryRepository.deleteExpiredIdempotencyKeys(Instant.now())
            );
            if (deletedCount != null && deletedCount > 0) {
                log.info("만료 멱등 키 청소 완료: count={}", deletedCount);
            }
        } catch (Exception e) {
            log.error("만료 멱등 키 청소 주기 실행 실패", e);
        }
    }
}
