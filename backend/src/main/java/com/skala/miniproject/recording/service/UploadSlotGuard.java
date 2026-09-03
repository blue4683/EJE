package com.skala.miniproject.recording.service;

import com.skala.miniproject.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/** 인스턴스당 동시 업로드를 analysis.upload-slots 개로 제한한다. 무제한 대기 큐를 두지 않는다. */
@Component
public class UploadSlotGuard {

    private final Semaphore semaphore;

    public UploadSlotGuard(AnalysisProperties properties) {
        this.semaphore = new Semaphore(properties.uploadSlots());
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }
}
