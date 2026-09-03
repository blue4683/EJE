package com.skala.miniproject.analysis.client;

/**
 * 분석 호출의 유일한 확장 지점 (루트 AGENTS.md §6). 파라미터·반환값은 표준 Java 타입과 자체
 * record 뿐이다 — 외부 라이브러리 타입을 노출하지 않는다. 지금은 MockSpeechAnalysisClient
 * 하나만 구현체로 등록돼 있다. 실제 연동은 이 인터페이스를 구현한 클래스 하나만 추가하면 된다.
 */
public interface SpeechAnalysisClient {

    /** 실패하면 SpeechAnalysisClientException 을 던진다. */
    SpeechAnalysisResult analyze(SpeechAnalysisRequest request);
}
