package com.skala.miniproject.history;

import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecordingDeleteTest {

    private static final Long FREE_USER_ID = 1L;
    private static final Long PRO_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 다른_녹음이_분석_중이어도_완료된_내_녹음은_204로_삭제한다() throws Exception {
        Long proResultId = jdbcTemplate.queryForObject(
                "select id from analysis_pro_results where analysis_id = 5000", Long.class);
        jdbcTemplate.update("""
                insert into api_idempotency_keys
                    (user_id, idempotency_key, operation, request_fingerprint,
                     recording_id, analysis_id, response_body, created_at, expires_at)
                values (1, '00000000-0000-0000-0000-000000000006', 'CREATE_RECORDING', repeat('a', 64),
                        88, 5000, '{}'::jsonb, '2026-09-03T04:00:00Z', '2026-09-04T04:00:00Z')
                """);

        mockMvc.perform(delete("/recordings/88")
                        .servletPath("/recordings/88")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        entityManager.flush();

        assertThat(count("select count(*) from recordings where id = 88")).isZero();
        assertThat(count("select count(*) from analyses where recording_id = 88")).isZero();
        assertThat(count("select count(*) from filler_breakdowns where analysis_id = 5000")).isZero();
        assertThat(count("select count(*) from analysis_pro_results where analysis_id = 5000")).isZero();
        assertThat(count("select count(*) from segment_analyses where analysis_id = 5000")).isZero();
        assertThat(count("select count(*) from filler_timeline_events where analysis_id = 5000")).isZero();
        assertThat(count("select count(*) from coaching_action_items where pro_result_id = " + proResultId)).isZero();

        var tombstone = jdbcTemplate.queryForMap("""
                select recording_id, analysis_id
                from api_idempotency_keys
                where idempotency_key = '00000000-0000-0000-0000-000000000006'
                """);
        assertThat(tombstone).containsEntry("recording_id", null).containsEntry("analysis_id", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PROCESSING"})
    void 대상_녹음의_분석이_진행_중이면_409이고_기록을_유지한다(
            String statusValue
    ) throws Exception {
        if (statusValue.equals("PROCESSING")) {
            jdbcTemplate.update("""
                    update analyses
                    set status = 'PROCESSING', started_at = now()
                    where id = 5002
                    """);
        }

        mockMvc.perform(delete("/recordings/102")
                        .servletPath("/recordings/102")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(FREE_USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CANNOT_DELETE_WHILE_PROCESSING"));

        assertThat(count("select count(*) from recordings where id = 102")).isOne();
        assertThat(count("select count(*) from analyses where recording_id = 102")).isOne();
    }

    @Test
    void 삭제한_녹음을_다시_삭제하면_410이_아닌_404다() throws Exception {
        String authorization = bearerToken(FREE_USER_ID);

        mockMvc.perform(delete("/recordings/88")
                        .servletPath("/recordings/88")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());

        entityManager.flush();

        mockMvc.perform(delete("/recordings/88")
                        .servletPath("/recordings/88")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void 남의_녹음은_존재를_숨기고_404를_반환한다() throws Exception {
        mockMvc.perform(delete("/recordings/101")
                        .servletPath("/recordings/101")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(PRO_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        assertThat(count("select count(*) from recordings where id = 101")).isOne();
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0 : result;
    }

    private String bearerToken(Long userId) {
        return "Bearer " + tokenProvider.issueAccessToken(userId);
    }
}
