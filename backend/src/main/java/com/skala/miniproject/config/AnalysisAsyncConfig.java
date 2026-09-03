package com.skala.miniproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 분석 파이프라인 전용 가상 스레드 Executor. spring.threads.virtual.enabled 는 서블릿 요청
 * 스레드용이고, 비동기 진입점(AnalysisExecutor)이 스레드를 직접 관리하려면 이 빈이 필요하다.
 */
@Configuration
public class AnalysisAsyncConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService analysisExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
