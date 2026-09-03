package com.skala.miniproject.analysis.pipeline;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 분석 하나가 살아 있는 동안만 존재하는 heartbeat. AnalysisExecutor 가 작업을 시작할 때 만들고
 * try-with-resources 로 감싸서, 그 작업이 끝나는 순간(성공·실패 모두) 자동으로 멈춘다. 이렇게
 * 작업 단위로 스코프를 좁혀서 "등록되지 않은 작업을 인스턴스 heartbeat 로 살려두지 않는다"는
 * 요구를 별도의 전역 레지스트리 없이 만족한다.
 */
public final class HeartbeatRunner implements AutoCloseable {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    public HeartbeatRunner(Runnable renewLease, Duration interval) {
        long millis = interval.toMillis();
        scheduler.scheduleAtFixedRate(renewLease, millis, millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
