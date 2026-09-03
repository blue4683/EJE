package com.skala.miniproject.analysis.client;

import com.skala.miniproject.domain.analysis.FailureCode;

/**
 * SpeechAnalysisClient 구현체가 분석에 실패했을 때 던진다. retryAfterSeconds 는 429 응답의
 * Retry-After 를 옮긴 값이며, 없으면 null 이다 — AutoRetryPolicy 가 0~30초로 제한해서 쓴다.
 */
public class SpeechAnalysisClientException extends RuntimeException {

    private final FailureCode failureCode;
    private final Integer retryAfterSeconds;

    public SpeechAnalysisClientException(FailureCode failureCode, String message) {
        this(failureCode, null, message);
    }

    public SpeechAnalysisClientException(FailureCode failureCode, Integer retryAfterSeconds, String message) {
        super(message);
        this.failureCode = failureCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public FailureCode failureCode() {
        return failureCode;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
