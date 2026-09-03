package com.skala.miniproject.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 명세: "prod에서 analysis.engine=mock이면 애플리케이션 시작을 실패시킨다."
 * mock 은 시연용 엔진이므로 운영 트래픽을 mock 결과로 응답하면 안 된다. local/test 에서는
 * 이 검사를 하지 않는다 — Mock API(B3)와 개발용 파이프라인이 정상적으로 mock 을 쓰기 때문이다.
 */
@Configuration
@Profile("prod")
public class AnalysisEngineStartupGuard {

    private static final String MOCK_ENGINE = "mock";

    @Bean
    ApplicationRunner rejectMockEngineInProd(AnalysisProperties properties) {
        return args -> {
            if (MOCK_ENGINE.equals(properties.engine())) {
                throw new IllegalStateException(
                        "prod 프로파일에서는 analysis.engine=mock 으로 기동할 수 없습니다.");
            }
        };
    }
}
