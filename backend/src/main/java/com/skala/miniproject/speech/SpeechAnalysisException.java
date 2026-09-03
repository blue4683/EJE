package com.skala.miniproject.speech;

/**
 * speech-habits-v1 규칙 위반(불변식 깨짐, 손상된 입력 등) 시 던진다.
 * 이 패키지는 Spring을 모르므로 BusinessException 을 직접 던지지 않는다 —
 * B5 의 파이프라인이 이 예외를 INVALID_ANALYSIS_RESULT 로 매핑한다.
 */
public class SpeechAnalysisException extends RuntimeException {

    public SpeechAnalysisException(String message) {
        super(message);
    }
}
