package com.skala.miniproject.pro;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProAnalysisTest {

    private static final Long RECORDING_OWNER_ID = 1L;
    private static final Long OTHER_PRO_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void restoreSeedData() {
        jdbcTemplate.update("UPDATE users SET plan = 'FREE' WHERE id = 1");
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 2");
        jdbcTemplate.update(
                "UPDATE analysis_pro_results SET words_per_minute = 120, total_word_count = 4 WHERE analysis_id = 5001"
        );
    }

    @Test
    void PRO_사용자는_완료된_기록의_정밀_분석_전체를_조회한다() throws Exception {
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");

        mockMvc.perform(get("/recordings/101/pro-analysis")
                        .servletPath("/recordings/101/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(RECORDING_OWNER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recordingId").value("101"))
                .andExpect(jsonPath("$.data.analysisId").value("5001"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.algorithmVersion").value("speech-habits-v1"))
                .andExpect(jsonPath("$.data.engineType").value("MOCK"))
                .andExpect(jsonPath("$.data.engineVersion").value("mock-pipeline-v1"))
                .andExpect(jsonPath("$.data.metrics.durationMs").value(3000))
                .andExpect(jsonPath("$.data.metrics.speechDurationMs").value(2000))
                .andExpect(jsonPath("$.data.metrics.silenceDurationMs").value(1000))
                .andExpect(jsonPath("$.data.metrics.longSilenceCount").value(0))
                .andExpect(jsonPath("$.data.metrics.repeatedExpressionCount").value(1))
                .andExpect(jsonPath("$.data.metrics.basic.fillerTotalCount").value(2))
                .andExpect(jsonPath("$.data.metrics.basic.fillerBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.data.metrics.basic.fillerBreakdown[?(@.expression == '음')].count").value(1))
                .andExpect(jsonPath("$.data.metrics.basic.fillerBreakdown[?(@.expression == '어')].count").value(1))
                .andExpect(jsonPath("$.data.metrics.speechIntervals", hasSize(1)))
                .andExpect(jsonPath("$.data.metrics.speechIntervals[0].startMs").value(500))
                .andExpect(jsonPath("$.data.metrics.speechIntervals[0].endMs").value(2500))
                .andExpect(jsonPath("$.data.metrics.waveform", hasSize(30)))
                .andExpect(jsonPath("$.data.metrics.waveform[0].timeMs").value(0))
                .andExpect(jsonPath("$.data.metrics.waveform[0].type").value("SILENCE"))
                .andExpect(jsonPath("$.data.metrics.fillerTimeline", hasSize(2)))
                .andExpect(jsonPath("$.data.metrics.fillerTimeline[*].eventIndex", contains(0, 1)))
                .andExpect(jsonPath("$.data.metrics.fillerTimeline[*].expression", contains("음", "어")))
                .andExpect(jsonPath("$.data.metrics.speechRate.wordsPerMinute").value(120))
                .andExpect(jsonPath("$.data.metrics.speechRate.totalWordCount").value(4))
                .andExpect(jsonPath("$.data.metrics.segmentAnalysis", hasSize(3)))
                .andExpect(jsonPath("$.data.metrics.segmentAnalysis[*].segment",
                        contains("INITIAL", "MIDDLE", "FINAL")))
                .andExpect(jsonPath("$.data.metrics.segmentAnalysis[*].fillerCount", contains(1, 0, 1)))
                .andExpect(jsonPath("$.data.metrics.coaching.summary")
                        .value("초반과 후반에 추임새가 관측되었습니다."))
                .andExpect(jsonPath("$.data.metrics.coaching.actionItems", hasSize(1)))
                .andExpect(jsonPath("$.data.metrics.coaching.actionItems[0]")
                        .value("문장 시작 전 짧게 멈춘 뒤 말해 보세요."))
                .andExpect(jsonPath("$.data.metrics.coaching.practiceRecommendation")
                        .value("같은 자기소개를 다시 녹음하고 추임새 변화를 확인하세요."))
                .andExpect(jsonPath("$.data.audioUrl").doesNotExist())
                .andExpect(jsonPath("$.data.metrics.audioUrl").doesNotExist());
    }

    @Test
    void 등급은_JWT가_아니라_매_요청_현재_DB값으로_검사한다() throws Exception {
        String authorization = bearerToken(RECORDING_OWNER_ID);

        mockMvc.perform(get("/recordings/101/pro-analysis")
                        .servletPath("/recordings/101/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PRO_REQUIRED"));

        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");

        mockMvc.perform(get("/recordings/101/pro-analysis")
                        .servletPath("/recordings/101/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk());
    }

    @Test
    void 남의_기록은_PRO_사용자에게도_404를_반환한다() throws Exception {
        mockMvc.perform(get("/recordings/101/pro-analysis")
                        .servletPath("/recordings/101/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OTHER_PRO_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void 남의_기록은_FREE_사용자에게도_등급_검사보다_먼저_404를_반환한다() throws Exception {
        jdbcTemplate.update("UPDATE users SET plan = 'FREE' WHERE id = 2");

        mockMvc.perform(get("/recordings/101/pro-analysis")
                        .servletPath("/recordings/101/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(OTHER_PRO_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void PRO_사용자의_미완료_기록은_409를_반환한다() throws Exception {
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");

        mockMvc.perform(get("/recordings/102/pro-analysis")
                        .servletPath("/recordings/102/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(RECORDING_OWNER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ANALYSIS_NOT_COMPLETED"));
    }

    @Test
    void 발화_속도를_계산할_수_없으면_wordsPerMinute는_null이다() throws Exception {
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");
        jdbcTemplate.update(
                "UPDATE analysis_pro_results SET words_per_minute = NULL, total_word_count = 0 WHERE analysis_id = 5001"
        );

        mockMvc.perform(get("/recordings/101/pro-analysis")
                        .servletPath("/recordings/101/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(RECORDING_OWNER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.speechRate.wordsPerMinute").value(nullValue()))
                .andExpect(jsonPath("$.data.metrics.speechRate.totalWordCount").value(0));
    }

    @Test
    void 코칭_항목은_sortOrder_순서대로_반환한다() throws Exception {
        jdbcTemplate.update("UPDATE users SET plan = 'PRO' WHERE id = 1");

        mockMvc.perform(get("/recordings/88/pro-analysis")
                        .servletPath("/recordings/88/pro-analysis")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(RECORDING_OWNER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.coaching.actionItems", contains(
                        "말하기 전 숨을 한 번 고르고 시작해 보세요.",
                        "문장 끝을 흐리지 않고 또렷하게 마무리해 보세요."
                )));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + tokenProvider.issueAccessToken(userId);
    }
}
