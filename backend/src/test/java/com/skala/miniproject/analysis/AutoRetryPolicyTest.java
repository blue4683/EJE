package com.skala.miniproject.analysis;

import com.skala.miniproject.analysis.pipeline.AutoRetryPolicy;
import com.skala.miniproject.domain.analysis.FailureCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AutoRetryPolicyTest {

    @ParameterizedTest
    @EnumSource(value = FailureCode.class, names = {"STT_TIMEOUT", "UPSTREAM_RATE_LIMIT", "UPSTREAM_UNAVAILABLE"})
    void 일시오류_3종만_재시도_대상이다(FailureCode failureCode) {
        assertThat(AutoRetryPolicy.isRetryable(failureCode)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FailureCode.class, names = {"COACHING_FAILED", "INVALID_ANALYSIS_RESULT", "WORKER_LOST",
            "ANALYSIS_TIMEOUT", "INTERNAL_ERROR"})
    void COACHING_FAILED_등은_자동_재시도하지_않는다(FailureCode failureCode) {
        assertThat(AutoRetryPolicy.isRetryable(failureCode)).isFalse();
    }

    @Test
    void 백오프가_1초_2초_4초_순이고_jitter는_0에서_500ms다() {
        assertThat(AutoRetryPolicy.backoff(1)).isBetween(Duration.ofMillis(1000), Duration.ofMillis(1500));
        assertThat(AutoRetryPolicy.backoff(2)).isBetween(Duration.ofMillis(2000), Duration.ofMillis(2500));
        assertThat(AutoRetryPolicy.backoff(3)).isBetween(Duration.ofMillis(4000), Duration.ofMillis(4500));
    }

    @Test
    void RetryAfter가_없으면_backoff만_쓴다() {
        Duration delay = AutoRetryPolicy.effectiveDelay(1, null);

        assertThat(delay).isBetween(Duration.ofMillis(1000), Duration.ofMillis(1500));
    }

    @Test
    void RetryAfter가_60초여도_30초로_제한된다() {
        Duration delay = AutoRetryPolicy.effectiveDelay(1, 60);

        assertThat(delay).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void RetryAfter가_backoff보다_작으면_backoff를_쓴다() {
        Duration delay = AutoRetryPolicy.effectiveDelay(3, 1);

        assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(4000));
    }

    @Test
    void 최대_자동_재시도는_3회다() {
        assertThat(AutoRetryPolicy.MAX_AUTO_RETRIES).isEqualTo(3);
    }
}
