package com.skala.miniproject.analysis.client;

import com.skala.miniproject.speech.model.SpeechMetrics;

/**
 * SpeechAnalysisClient 의 성공 출력. metrics 는 B1 의 SpeechMetrics 를 그대로 재사용한다 —
 * Phase 1 Mock 과 Phase 2 실제 분석이 공유하는 데이터 의미이기 때문이다(speech 패키지 주석 참조).
 * engineVersion 은 실제로 응답을 만든 엔진의 버전이다(Phase 2 에서는 요청한 expectedEngineVersion과
 * 일치해야 한다).
 */
public record SpeechAnalysisResult(String engineVersion, SpeechMetrics metrics) {
}
