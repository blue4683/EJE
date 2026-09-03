package com.skala.miniproject.domain.analysis;

/** ERD ck_analyses_failure_code · API명세서.md 「비동기 failureCode」 표와 정확히 일치해야 한다. */
public enum FailureCode {
    STT_TIMEOUT,
    UPSTREAM_RATE_LIMIT,
    UPSTREAM_UNAVAILABLE,
    COACHING_FAILED,
    INVALID_ANALYSIS_RESULT,
    WORKER_LOST,
    ANALYSIS_TIMEOUT,
    INTERNAL_ERROR
}
