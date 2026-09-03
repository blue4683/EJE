package com.skala.miniproject.analysis.pipeline;

import com.skala.miniproject.analysis.AnalysisSlotGuard;
import com.skala.miniproject.analysis.client.SpeechAnalysisClient;
import com.skala.miniproject.analysis.client.SpeechAnalysisClientException;
import com.skala.miniproject.analysis.client.SpeechAnalysisRequest;
import com.skala.miniproject.analysis.client.SpeechAnalysisResult;
import com.skala.miniproject.config.AnalysisProperties;
import com.skala.miniproject.domain.analysis.FailureCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 비동기 진입점(C1). {@code CurrentUser.id()} 를 부르지 않고 필요한 값을 전부 인자로 받는다.
 * {@code execute()} 는 즉시 반환하고, 실제 실행(상태 전이·재시도·결과 저장)은 별도 가상 스레드에서
 * PENDING → PROCESSING → COMPLETED/FAILED 로 끝날 때까지 돈다.
 *
 * 분석 슬롯(B4 가 접수 시 확보) 의 소유권은 이 지점에서 이 실행으로 넘어온다 — 슬롯은 여기서
 * 실행이 끝날 때(성공·실패 모두) 딱 한 번 해제된다. 접수 단계에서 바로 해제하면 "인스턴스당 동시
 * 분석 2개"라는 용량 제한이 실제 처리 시간 동안은 지켜지지 않게 된다.
 */
@Slf4j
@Component
public class AnalysisExecutor {

    private final SpeechAnalysisClient speechAnalysisClient; // ✅ 인터페이스만 참조한다
    private final AnalysisStateMachine stateMachine;
    private final AnalysisResultWriter resultWriter;
    private final AnalysisSlotGuard analysisSlotGuard;
    private final AnalysisProperties properties;
    private final ExecutorService executorService;

    public AnalysisExecutor(
            SpeechAnalysisClient speechAnalysisClient,
            AnalysisStateMachine stateMachine,
            AnalysisResultWriter resultWriter,
            AnalysisSlotGuard analysisSlotGuard,
            AnalysisProperties properties,
            ExecutorService analysisExecutorService
    ) {
        this.speechAnalysisClient = speechAnalysisClient;
        this.stateMachine = stateMachine;
        this.resultWriter = resultWriter;
        this.analysisSlotGuard = analysisSlotGuard;
        this.properties = properties;
        this.executorService = analysisExecutorService;
    }

    /** 커밋 직후 호출한다. 분석 슬롯은 이미 호출자가 확보해 뒀다 — 여기서부터 이 실행이 그 슬롯을 쓴다. */
    public void execute(long userId, long recordingId, long analysisId, UUID workerId, int attemptNo,
                         byte[] audioBytes, int durationMs, Instant executionDeadlineAt) {
        executorService.execute(() -> run(userId, recordingId, analysisId, workerId, attemptNo,
                audioBytes, durationMs, executionDeadlineAt));
    }

    private void run(long userId, long recordingId, long analysisId, UUID workerId, int attemptNo,
                      byte[] audioBytes, int durationMs, Instant executionDeadlineAt) {
        try {
            runUntilTerminal(recordingId, analysisId, workerId, attemptNo, audioBytes, durationMs, executionDeadlineAt);
        } finally {
            analysisSlotGuard.release();
        }
    }

    private void runUntilTerminal(long recordingId, long analysisId, UUID workerId, int attemptNo,
                                   byte[] audioBytes, int durationMs, Instant executionDeadlineAt) {
        if (!stateMachine.beginProcessing(analysisId, workerId, Instant.now())) {
            log.warn("분석 {} 을 PROCESSING 으로 전이하지 못해 실행을 중단합니다.", analysisId);
            return;
        }

        int autoRetryCount = 0;
        while (true) {
            Duration remaining = Duration.between(Instant.now(), executionDeadlineAt);
            if (remaining.isNegative() || remaining.isZero()) {
                stateMachine.fail(analysisId, workerId, FailureCode.ANALYSIS_TIMEOUT, Instant.now());
                return;
            }
            long callTimeoutSeconds = Math.min(properties.callTimeoutSeconds(), remaining.toSeconds());

            SpeechAnalysisRequest request = new SpeechAnalysisRequest(recordingId, analysisId, attemptNo,
                    autoRetryCount, durationMs, audioBytes, properties.algorithmVersion(), null, executionDeadlineAt);

            try (HeartbeatRunner heartbeat = startHeartbeat(analysisId, workerId)) {
                SpeechAnalysisResult result = callWithTimeout(request, callTimeoutSeconds);
                resultWriter.save(analysisId, attemptNo, autoRetryCount, workerId, result.metrics(), Instant.now());
                return;
            } catch (TimeoutException e) {
                if (!retryOrGiveUp(analysisId, workerId, FailureCode.STT_TIMEOUT, null, autoRetryCount + 1)) {
                    return;
                }
                autoRetryCount++;
            } catch (SpeechAnalysisClientException e) {
                if (!retryOrGiveUp(analysisId, workerId, e.failureCode(), e.retryAfterSeconds(), autoRetryCount + 1)) {
                    return;
                }
                autoRetryCount++;
            } catch (RuntimeException e) {
                // 내부 오류 원문을 공개 응답·로그에 그대로 남기지 않는다.
                log.error("분석 {} 실행 중 분류되지 않은 오류", analysisId, e);
                stateMachine.fail(analysisId, workerId, FailureCode.INTERNAL_ERROR, Instant.now());
                return;
            }
        }
    }

    private HeartbeatRunner startHeartbeat(long analysisId, UUID workerId) {
        return new HeartbeatRunner(
                () -> stateMachine.renewLease(analysisId, workerId,
                        Instant.now().plusSeconds(properties.leaseSeconds()), Instant.now()),
                Duration.ofSeconds(properties.heartbeatSeconds()));
    }

    /** @return true 면 재시도를 등록했으니 계속 반복한다. false 면 실패로 정리했으니 반환한다. */
    private boolean retryOrGiveUp(long analysisId, UUID workerId, FailureCode failureCode,
                                   Integer retryAfterSeconds, int nextAutoRetryCount) {
        if (AutoRetryPolicy.isRetryable(failureCode) && nextAutoRetryCount <= AutoRetryPolicy.MAX_AUTO_RETRIES) {
            sleepQuietly(AutoRetryPolicy.effectiveDelay(nextAutoRetryCount, retryAfterSeconds));
            stateMachine.registerAutoRetry(analysisId, workerId, Instant.now());
            return true;
        }
        stateMachine.fail(analysisId, workerId, failureCode, Instant.now());
        return false;
    }

    private SpeechAnalysisResult callWithTimeout(SpeechAnalysisRequest request, long timeoutSeconds)
            throws TimeoutException {
        CompletableFuture<SpeechAnalysisResult> future =
                CompletableFuture.supplyAsync(() -> speechAnalysisClient.analyze(request), executorService);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            switch (e.getCause()) {
                case SpeechAnalysisClientException clientException -> throw clientException;
                case RuntimeException runtimeException -> throw runtimeException;
                default -> throw new IllegalStateException(e.getCause());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("분석 호출이 중단되었습니다.", e);
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
