package com.skala.miniproject.history;

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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryReadTest {

    private static final Long FREE_USER_ID = 1L;
    private static final Long PRO_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreSeedUserPlans() {
        jdbcTemplate.update("UPDATE users SET plan = 'FREE' WHERE id = 1");
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 2");
    }

    @Test
    void 완료된_내_기록_상세는_추임새와_FREE_PRO_잠금을_반환한다() throws Exception {
        mockMvc.perform(get("/recordings/101")
                        .servletPath("/recordings/101")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.recordingId").value("101"))
                .andExpect(jsonPath("$.data.submittedAt").value("2026-09-03T03:00:00Z"))
                .andExpect(jsonPath("$.data.durationMs").value(3000))
                .andExpect(jsonPath("$.data.mimeType").value("audio/webm"))
                .andExpect(jsonPath("$.data.fileSizeBytes").value(24000))
                .andExpect(jsonPath("$.data.analysis.analysisId").value("5001"))
                .andExpect(jsonPath("$.data.analysis.recordingId").value("101"))
                .andExpect(jsonPath("$.data.analysis.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.analysis.retryable").value(false))
                .andExpect(jsonPath("$.data.analysis.retryRequiresAudio").value(false))
                .andExpect(jsonPath("$.data.basic.fillerTotalCount").value(2))
                .andExpect(jsonPath("$.data.basic.fillerBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.data.basic.fillerBreakdown[?(@.expression == '음')].count").value(1))
                .andExpect(jsonPath("$.data.basic.fillerBreakdown[?(@.expression == '어')].count").value(1))
                .andExpect(jsonPath("$.data.pro.locked").value(true))
                .andExpect(jsonPath("$.data.pro.available").value(false))
                .andExpect(jsonPath("$.data.pro.detailUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.pro.upgradePath").value("/upgrade"))
                .andExpect(jsonPath("$.data.pro.lockedFeatures", contains(
                        "waveform", "silence", "speed", "timeline", "segment",
                        "repetition", "comparison", "coaching", "weeklyReport")))
                .andExpect(jsonPath("$.data.algorithmVersion").value("speech-habits-v1"))
                .andExpect(jsonPath("$.data.engineType").value("MOCK"))
                .andExpect(jsonPath("$.data.engineVersion").value("mock-pipeline-v1"));
    }

    @Test
    void 미완료_기록의_basic과_fillerTotalCount는_null이다() throws Exception {
        mockMvc.perform(get("/recordings/102")
                        .servletPath("/recordings/102")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis.status").value("PENDING"))
                .andExpect(jsonPath("$.data.analysis.startedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.analysis.finishedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.analysis.retryable").value(false))
                .andExpect(jsonPath("$.data.basic").value(nullValue()))
                .andExpect(jsonPath("$.data.pro.available").value(false));

        mockMvc.perform(get("/dashboard/recordings")
                        .servletPath("/dashboard/recordings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[1].recordingId").value("102"))
                .andExpect(jsonPath("$.data.content[1].fillerTotalCount").value(nullValue()));
    }

    @Test
    void 실패_기록은_attemptNo가_4미만이면_수동_재시도가_가능하다() throws Exception {
        mockMvc.perform(get("/recordings/103")
                        .servletPath("/recordings/103")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis.status").value("FAILED"))
                .andExpect(jsonPath("$.data.analysis.attemptNo").value(1))
                .andExpect(jsonPath("$.data.analysis.failureCode").value("STT_TIMEOUT"))
                .andExpect(jsonPath("$.data.analysis.retryable").value(true))
                .andExpect(jsonPath("$.data.analysis.retryRequiresAudio").value(true));
    }

    @Test
    void 기록_목록은_제출시각과_ID_내림차순이며_페이지_메타데이터를_반환한다() throws Exception {
        mockMvc.perform(get("/dashboard/recordings")
                        .servletPath("/dashboard/recordings")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)))
                .andExpect(jsonPath("$.data.content[*].recordingId", contains("103", "102", "101", "88")))
                .andExpect(jsonPath("$.data.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.content[0].fillerTotalCount").value(nullValue()))
                .andExpect(jsonPath("$.data.content[2].fillerTotalCount").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void 기록이_없으면_빈_목록과_totalPages_0을_반환한다() throws Exception {
        mockMvc.perform(get("/dashboard/recordings")
                        .servletPath("/dashboard/recordings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(PRO_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void 잘못된_페이지_범위는_422다() throws Exception {
        String authorization = bearerToken(FREE_USER_ID);

        mockMvc.perform(get("/dashboard/recordings")
                        .servletPath("/dashboard/recordings")
                        .param("page", "-1")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/dashboard/recordings")
                        .servletPath("/dashboard/recordings")
                        .param("size", "0")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/dashboard/recordings")
                        .servletPath("/dashboard/recordings")
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 완료된_기본_결과는_추임새와_PRO_접근_정보를_반환한다() throws Exception {
        mockMvc.perform(get("/dashboard/recordings/101/result")
                        .servletPath("/dashboard/recordings/101/result")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingId").value("101"))
                .andExpect(jsonPath("$.data.analysisId").value("5001"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.basic.fillerTotalCount").value(2))
                .andExpect(jsonPath("$.data.pro.locked").value(true));
    }

    @Test
    void 미완료_기본_결과는_409다() throws Exception {
        mockMvc.perform(get("/dashboard/recordings/102/result")
                        .servletPath("/dashboard/recordings/102/result")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ANALYSIS_NOT_COMPLETED"));
    }

    @Test
    void 없는_기록과_남의_기록은_모두_404다() throws Exception {
        mockMvc.perform(get("/recordings/99999")
                        .servletPath("/recordings/99999")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/recordings/101")
                        .servletPath("/recordings/101")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(PRO_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void 최근_분석은_완료된_기록만_제출순으로_최대_3개를_반환한다() throws Exception {
        mockMvc.perform(get("/dashboard/recent-analyses")
                        .servletPath("/dashboard/recent-analyses")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[*].recordingId", contains("101", "88")))
                .andExpect(jsonPath("$.data.items[*].status", contains("COMPLETED", "COMPLETED")));
    }

    @Test
    void PRO이면서_완료된_기록은_정밀_분석_경로를_반환한다() throws Exception {
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");

        mockMvc.perform(get("/recordings/101")
                        .servletPath("/recordings/101")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pro.locked").value(false))
                .andExpect(jsonPath("$.data.pro.available").value(true))
                .andExpect(jsonPath("$.data.pro.detailUrl")
                        .value("/api/v1/recordings/101/pro-analysis"))
                .andExpect(jsonPath("$.data.pro.upgradePath").value(nullValue()))
                .andExpect(jsonPath("$.data.pro.lockedFeatures", hasSize(0)));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + tokenProvider.issueAccessToken(userId);
    }
}
