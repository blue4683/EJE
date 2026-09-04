package com.skala.miniproject.mockapi;

import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.config.MockApiSecurityChain;
import com.skala.miniproject.mockapi.controller.MockWaveformController;
import com.skala.miniproject.mockapi.dto.AmplitudePointDto;
import com.skala.miniproject.mockapi.dto.MockTranscriptRequest;
import com.skala.miniproject.mockapi.dto.MockWaveformRequest;
import com.skala.miniproject.mockapi.service.MockAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B1 규칙 엔진을 재구현하지 않고, API명세서.md 의 API 20·21 요청/응답 예제 값을 그대로 재현하는지 검증한다.
 */
class MockApiTest {

    private final MockAnalysisService service = new MockAnalysisService();

    @Test
    void API20_명세_예제를_그대로_재현한다() {
        List<AmplitudePointDto> waveform = new ArrayList<>();
        for (int t = 0; t < 3000; t += 100) {
            double amplitude = (t >= 500 && t < 2500) ? 0.65 : 0.02;
            waveform.add(new AmplitudePointDto(t, amplitude));
        }

        var response = service.analyzeWaveform(new MockWaveformRequest(3000, waveform));

        assertThat(response.speechDurationMs()).isEqualTo(2000);
        assertThat(response.silenceDurationMs()).isEqualTo(1000);
        assertThat(response.longSilenceCount()).isEqualTo(0);
        assertThat(response.speechIntervals()).hasSize(1);
        assertThat(response.speechIntervals().getFirst().startMs()).isEqualTo(500);
        assertThat(response.speechIntervals().getFirst().endMs()).isEqualTo(2500);
        assertThat(response.speechDurationMs() + response.silenceDurationMs()).isEqualTo(3000);
        assertThat(response.waveform()).hasSize(30);
    }

    @Test
    void API21_명세_예제를_그대로_재현한다() {
        List<com.skala.miniproject.mockapi.dto.TimedTokenDto> tokens = List.of(
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("음", 700, 800),
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("저는", 900, 1100),
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("저는", 1200, 1400),
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("사실", 1500, 1700),
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("어", 2100, 2200),
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("홍길동입니다", 2300, 2500)
        );

        var response = service.analyzeTranscript(new MockTranscriptRequest(3000, tokens));

        assertThat(response.basic().fillerTotalCount()).isEqualTo(2);
        assertThat(response.totalWordCount()).isEqualTo(4);
        assertThat(response.repeatedExpressionCount()).isEqualTo(1);
        assertThat(response.fillerTimeline()).hasSize(2);
        assertThat(response.segmentAnalysis()).extracting("fillerCount")
                .containsExactly(1, 0, 1);
    }

    @Test
    void API21은_발화_구간이_없어도_토큰_전체를_유효로_본다() {
        // 규칙4 ⑥ 겹침 검사를 건너뛰는지 확인 — 정상적으로는 발화 구간이 없으면 모든 토큰이 무효 처리된다.
        List<com.skala.miniproject.mockapi.dto.TimedTokenDto> tokens = List.of(
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("안녕하세요", 0, 500)
        );

        var response = service.analyzeTranscript(new MockTranscriptRequest(1000, tokens));

        assertThat(response.totalWordCount()).isEqualTo(1);
    }

    @Test
    void API21은_startMs가_비감소가_아니면_VALIDATION_ERROR다() {
        List<com.skala.miniproject.mockapi.dto.TimedTokenDto> tokens = List.of(
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("어", 500, 600),
                new com.skala.miniproject.mockapi.dto.TimedTokenDto("음", 100, 200)
        );

        assertThatThrownBy(() -> service.analyzeTranscript(new MockTranscriptRequest(1000, tokens)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

}

/** Access JWT 없이 호출하면 401, 있으면 명세 예제와 같은 200 응답을 반환하는지 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MockApiHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final Long SEED_USER_ID = 1L;

    @Test
    void 토큰_없이_호출하면_401이다() throws Exception {
        mockMvc.perform(post("/mock/waveform-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMs\":3000,\"waveform\":[{\"timeMs\":0,\"amplitude\":0.02}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 범위를_벗어난_durationMs는_422다() throws Exception {
        String token = tokenProvider.issueAccessToken(SEED_USER_ID);

        mockMvc.perform(post("/mock/waveform-analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMs\":999,\"waveform\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 유효한_토큰이면_200과_명세_형태의_data를_반환한다() throws Exception {
        String token = tokenProvider.issueAccessToken(SEED_USER_ID);

        mockMvc.perform(post("/mock/transcript-analysis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationMs":1000,"tokens":[{"text":"어","startMs":0,"endMs":100}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.basic.fillerTotalCount").value(1));
    }
}

/**
 * prod 에서는 MockApiSecurityChain(@Order(0))이 인증 필터보다 먼저 매칭돼 컨트롤러가 없다는 이유로
 * 404 가 나야 한다(401 이 아니다). @WebMvcTest 는 ApplicationRunner 를 실행하지 않으므로
 * AnalysisEngineStartupGuard(prod+engine=mock 기동 실패)와 부딪히지 않고 라우팅만 검증할 수 있다 —
 * 이 두 요구사항(prod 기동 실패 vs prod 라우팅 확인)을 하나의 @SpringBootTest 로 동시에 만족시킬 수
 * 없어서 검증 전략을 분리했다.
 */
@WebMvcTest(controllers = MockWaveformController.class)
@Import({MockApiProdRoutingTest.SecurityInfra.class, MockApiSecurityChain.class})
@ActiveProfiles("prod")
class MockApiProdRoutingTest {

    /** @WebMvcTest 슬라이스에는 HttpSecurity 빈을 만드는 @EnableWebSecurity 인프라가 없다.
     * 실제 앱에서는 SecurityConfig 의 @EnableWebSecurity 가 이 역할을 하므로, 여기서는
     * 그 무거운 의존성(UserRepository 등) 없이 최소한으로만 재현한다. */
    @EnableWebSecurity
    static class SecurityInfra {
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prod에서는_mock_엔드포인트가_404다() throws Exception {
        mockMvc.perform(post("/mock/waveform-analysis"))
                .andExpect(status().isNotFound());
    }
}
