package com.skala.miniproject.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** API명세서.md 「공통 오류 코드」·「비동기 failureCode」 표와 정확히 일치해야 한다 (27개). */
@Getter
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_CONTENT, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    ORIGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "허용되지 않은 요청입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다."),
    RESOURCE_GONE(HttpStatus.GONE, "이미 삭제된 기록입니다."),
    PRO_REQUIRED(HttpStatus.FORBIDDEN, "PRO 등급에서 이용할 수 있습니다."),
    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "분석이 아직 완료되지 않았습니다."),
    CANNOT_DELETE_WHILE_PROCESSING(HttpStatus.CONFLICT, "분석이 진행 중이라 삭제할 수 없습니다."),
    ANALYSIS_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 진행 중인 분석이 있습니다. 완료된 뒤 다시 시도해 주세요."),
    INVALID_ANALYSIS_STATE(HttpStatus.CONFLICT, "실패한 분석만 다시 시도할 수 있습니다."),
    MANUAL_RETRY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "재시도 횟수를 모두 사용했습니다. 새로 녹음해 주세요."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "같은 요청 키로 다른 내용이 전송되었습니다."),
    AUDIO_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "처음 제출한 음성과 다른 파일입니다."),
    AUDIO_DURATION_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_CONTENT, "1초 이상 60초 이하로 녹음해 주세요."),
    INVALID_AUDIO(HttpStatus.UNPROCESSABLE_CONTENT, "음성 파일을 읽을 수 없습니다. 다시 녹음해 주세요."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 음성 형식입니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일이 너무 큽니다. 16MB 이하로 올려 주세요."),
    REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "요청 시간이 초과되었습니다. 다시 시도해 주세요."),
    ANALYSIS_CAPACITY_EXCEEDED(HttpStatus.SERVICE_UNAVAILABLE, "지금은 분석 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
    COMPARISON_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "비교할 이전 기록이 없습니다."),
    ANALYSIS_VERSION_MISMATCH(HttpStatus.CONFLICT, "분석 기준이 달라 비교할 수 없습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적인 오류입니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
