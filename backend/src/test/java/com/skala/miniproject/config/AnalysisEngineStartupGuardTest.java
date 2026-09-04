package com.skala.miniproject.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisEngineStartupGuardTest {

    private final AnalysisEngineStartupGuard guard = new AnalysisEngineStartupGuard();

    @Test
    void engine이_mock이면_예외를_던진다() {
        ApplicationRunner runner = guard.rejectMockEngineInProd(properties("mock"));

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void engine이_mock이_아니면_통과한다() throws Exception {
        ApplicationRunner runner = guard.rejectMockEngineInProd(properties("whisper"));

        runner.run(null);
    }

    private AnalysisProperties properties(String engine) {
        return new AnalysisProperties(engine, "speech-habits-v1", "mock-pipeline-v1",
                2, 4, 30, 5, 600, 120, 3, "ffmpeg", 10, 30);
    }
}
