package com.skala.miniproject.audio;

/**
 * 오디오 디코딩 실패 원인을 담아 던진다. 이 패키지는 common.exception.ErrorCode 를 직접 참조하지
 * 않는다 — 호출자(B4)가 reason() 을 보고 UNSUPPORTED_MEDIA_TYPE·INVALID_AUDIO·
 * AUDIO_DURATION_OUT_OF_RANGE·REQUEST_TIMEOUT 으로 매핑한다.
 */
public class AudioDecodeException extends RuntimeException {

    public enum Reason { UNSUPPORTED_MEDIA_TYPE, INVALID_AUDIO, DURATION_OUT_OF_RANGE, TIMEOUT }

    private final Reason reason;

    public AudioDecodeException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AudioDecodeException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
