package com.skala.miniproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * prod 에서 /mock/** 을 404 로 만들기 위한 전용 체인. MockWaveformController·MockTranscriptController
 * 가 prod 에는 등록되지 않으므로(@Profile(local,test)), SecurityConfig 의 기본 체인(§9-1 A1,
 * @Order(100))만 있으면 인증 필터가 먼저 401 을 돌려준다. 명세는 404 를 요구하므로, /mock/** 만
 * permitAll 하는 이 체인을 @Order(0) 으로 먼저 매칭시켜 인증 필터보다 앞서 통과시킨 뒤,
 * 핸들러가 없어 GlobalExceptionHandler 의 NoResourceFoundException 처리(RESOURCE_NOT_FOUND)로
 * 떨어지게 한다. SecurityConfig 는 수정하지 않는다 (§C5).
 */
@Configuration
@Profile("prod")
public class MockApiSecurityChain {

    @Bean
    @Order(0)
    SecurityFilterChain mockApiChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/mock/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
