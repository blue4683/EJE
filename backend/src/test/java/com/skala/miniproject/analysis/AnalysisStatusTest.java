package com.skala.miniproject.analysis;

import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 08({@code /analyses/{analysisId}/status})·API 13({@code /dashboard/recordings/{recordingId}/status})
 * 통합 테스트. db/seed-dev.sql 의 고정 행을 그대로 쓴다 (전부 userId=1 소유):
 * 5000(88, COMPLETED)·5001(101, COMPLETED)·5002(102, PENDING)·5003(103, FAILED, STT_TIMEOUT, attemptNo=1).
 * userId=2 는 seed 상 recordings 가 없어 "남의 리소스" 검증에 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisStatusTest {

    private static final Long OWNER = 1L;
    private static final Long OTHER_USER_WITHOUT_RECORDINGS = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @AfterEach
    void restoreFailedAnalysis() {
        jdbcTemplate.update(
                "UPDATE analyses SET attempt_no = 1, failure_code = 'STT_TIMEOUT' WHERE id = 5003");
    }

    @Test
    void FAILED도_HTTP_200이다() throws Exception {
        mockMvc.perform(get("/analyses/5003/status")
                        .servletPath("/analyses/5003/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureCode").value("STT_TIMEOUT"));
    }

    @Test
    void retryable은_FAILED이고_attemptNo가_4미만일_때만_true다() throws Exception {
        // FAILED, attemptNo=1<4 → true
        mockMvc.perform(get("/analyses/5003/status")
                        .servletPath("/analyses/5003/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.retryable").value(true));

        // COMPLETED → false (FAILED 가 아니므로)
        mockMvc.perform(get("/analyses/5001/status")
                        .servletPath("/analyses/5001/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.retryable").value(false));

        // PENDING → false (FAILED 가 아니므로)
        mockMvc.perform(get("/analyses/5002/status")
                        .servletPath("/analyses/5002/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.retryable").value(false));

        // FAILED, attemptNo=4 → false (한도 소진)
        jdbcTemplate.update("UPDATE analyses SET attempt_no = 4 WHERE id = 5003");
        mockMvc.perform(get("/analyses/5003/status")
                        .servletPath("/analyses/5003/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.retryable").value(false));
    }

    @Test
    void retryRequiresAudio는_retryable과_항상_같다() throws Exception {
        mockMvc.perform(get("/analyses/5003/status")
                        .servletPath("/analyses/5003/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.retryable").value(true))
                .andExpect(jsonPath("$.data.retryRequiresAudio").value(true));

        mockMvc.perform(get("/analyses/5001/status")
                        .servletPath("/analyses/5001/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.retryable").value(false))
                .andExpect(jsonPath("$.data.retryRequiresAudio").value(false));
    }

    @Test
    void COACHING_FAILED도_attemptNo가_4미만이면_retryable이_true다() throws Exception {
        jdbcTemplate.update("UPDATE analyses SET failure_code = 'COACHING_FAILED' WHERE id = 5003");

        mockMvc.perform(get("/analyses/5003/status")
                        .servletPath("/analyses/5003/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(jsonPath("$.data.failureCode").value("COACHING_FAILED"))
                .andExpect(jsonPath("$.data.retryable").value(true))
                .andExpect(jsonPath("$.data.retryRequiresAudio").value(true));
    }

    @Test
    void 남의_분석은_404다() throws Exception {
        mockMvc.perform(get("/analyses/5003/status")
                        .servletPath("/analyses/5003/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OTHER_USER_WITHOUT_RECORDINGS)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/dashboard/recordings/103/status")
                        .servletPath("/dashboard/recordings/103/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OTHER_USER_WITHOUT_RECORDINGS)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        // 존재하지 않는 리소스도 동일하게 404
        mockMvc.perform(get("/analyses/99999/status")
                        .servletPath("/analyses/99999/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void API_08과_13이_같은_응답을_반환한다() throws Exception {
        MvcResult byAnalysisId = mockMvc.perform(get("/analyses/5001/status")
                        .servletPath("/analyses/5001/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult byRecordingId = mockMvc.perform(get("/dashboard/recordings/101/status")
                        .servletPath("/dashboard/recordings/101/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OWNER)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode dataFromAnalysisEndpoint = dataOf(byAnalysisId);
        JsonNode dataFromDashboardEndpoint = dataOf(byRecordingId);

        assertThat(dataFromAnalysisEndpoint).isEqualTo(dataFromDashboardEndpoint);
    }

    private JsonNode dataOf(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String bearerToken(Long userId) {
        return "Bearer " + tokenProvider.issueAccessToken(userId);
    }
}
