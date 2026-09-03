package com.skala.miniproject.analysis;

import com.skala.miniproject.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/** 인스턴스당 동시 분석을 analysis.slots 개로 제한한다. 접수 전에 선점적으로 확보한다. */
@Component
public class AnalysisSlotGuard {

    private final Semaphore semaphore;

    public AnalysisSlotGuard(AnalysisProperties properties) {
        this.semaphore = new Semaphore(properties.slots());
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }
}
