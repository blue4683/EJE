package com.skala.miniproject.analysis.pipeline;

import com.skala.miniproject.domain.analysis.FailureCode;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 규칙: STT_TIMEOUT·UPSTREAM_RATE_LIMIT·UPSTREAM_UNAVAILABLE 만 자동 재시도한다. 추가 호출
 * 최대 3회, 대기 1·2·4초 + 0~0.5초 jitter. 429 Retry-After 는 0~30초로 제한해 백오프와 큰 값을 쓴다.
 */
public final class AutoRetryPolicy {

    public static final int MAX_AUTO_RETRIES = 3;

    private static final Set<FailureCode> RETRYABLE = EnumSet.of(
            FailureCode.STT_TIMEOUT, FailureCode.UPSTREAM_RATE_LIMIT, FailureCode.UPSTREAM_UNAVAILABLE);
    private static final long[] BASE_BACKOFF_MILLIS = {1000L, 2000L, 4000L};
    private static final long MAX_JITTER_MILLIS = 500L;
    private static final long MAX_RETRY_AFTER_SECONDS = 30L;

    private AutoRetryPolicy() {
    }

    public static boolean isRetryable(FailureCode failureCode) {
        return RETRYABLE.contains(failureCode);
    }

    /** nextAutoRetryCount 는 이번에 실행할 재시도 차수(1,2,3)다 — 1회차 1초, 2회차 2초, 3회차 4초. */
    public static Duration backoff(int nextAutoRetryCount) {
        long base = BASE_BACKOFF_MILLIS[Math.clamp(nextAutoRetryCount, 1, BASE_BACKOFF_MILLIS.length) - 1];
        long jitter = ThreadLocalRandom.current().nextLong(0, MAX_JITTER_MILLIS + 1);
        return Duration.ofMillis(base + jitter);
    }

    /** retryAfterSeconds 가 있으면 0~30초로 제한한 값과 backoff 중 더 큰 쪽을 쓴다. */
    public static Duration effectiveDelay(int nextAutoRetryCount, Integer retryAfterSeconds) {
        Duration backoff = backoff(nextAutoRetryCount);
        if (retryAfterSeconds == null) {
            return backoff;
        }
        long cappedSeconds = Math.max(0, Math.min(retryAfterSeconds, MAX_RETRY_AFTER_SECONDS));
        Duration retryAfter = Duration.ofSeconds(cappedSeconds);
        return backoff.compareTo(retryAfter) >= 0 ? backoff : retryAfter;
    }
}
