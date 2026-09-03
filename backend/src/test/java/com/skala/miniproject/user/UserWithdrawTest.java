package com.skala.miniproject.user;

import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserWithdrawTest {

    private static final long USER_ID = 9_900_004L;
    private static final long RECORDING_ID = 9_900_104L;
    private static final long ANALYSIS_ID = 9_990_104L;
    private static final String PASSWORD = "P@ssw0rd123";
    private static final String PASSWORD_HASH =
            "$2a$12$ZgH8t9.skqbuAYP1/ADm6.a3A3gUOdCWDkk9Zq2xt/Ju6KJOc6YHa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertTestUser() {
        deleteTestUser();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, name, plan)
                VALUES (?, ?, ?, ?, 'FREE')
                """, USER_ID, "withdraw@example.com", PASSWORD_HASH, "탈퇴 테스트");
    }

    @AfterEach
    void deleteTestUser() {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    @Test
    void 회원_탈퇴는_계정과_하위_데이터를_삭제하고_쿠키를_만료한다() throws Exception {
        insertCompletedAnalysis();
        String accessToken = bearerToken();

        mockMvc.perform(delete("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"P@ssw0rd123"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(content().string(""));

        assertThat(count("users", "id", USER_ID)).isZero();
        assertThat(count("recordings", "id", RECORDING_ID)).isZero();
        assertThat(count("analyses", "id", ANALYSIS_ID)).isZero();

        mockMvc.perform(get("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호가_일치하지_않으면_INVALID_PASSWORD를_반환하고_계정을_유지한다() throws Exception {
        mockMvc.perform(delete("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"wrongpass1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"));

        assertThat(count("users", "id", USER_ID)).isOne();
    }

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void 형식에_맞지_않는_비밀번호는_VALIDATION_ERROR를_반환하고_계정을_유지한다(String password)
            throws Exception {
        String passwordJson = password == null ? "null" : "\"" + password + "\"";

        mockMvc.perform(delete("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":" + passwordJson + "}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(count("users", "id", USER_ID)).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PROCESSING"})
    void 활성_분석이_있으면_CANNOT_DELETE_WHILE_PROCESSING을_반환하고_계정을_유지한다(String status)
            throws Exception {
        insertActiveAnalysis(status);

        mockMvc.perform(delete("/users/me")
                        .servletPath("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CANNOT_DELETE_WHILE_PROCESSING"));

        assertThat(count("users", "id", USER_ID)).isOne();
        assertThat(count("recordings", "id", RECORDING_ID)).isOne();
        assertThat(count("analyses", "id", ANALYSIS_ID)).isOne();
    }

    private void insertCompletedAnalysis() {
        insertRecording();
        jdbcTemplate.update("""
                INSERT INTO analyses (
                    id, recording_id, status, attempt_no, auto_retry_count,
                    execution_deadline_at, started_at, finished_at,
                    speech_duration_ms, silence_duration_ms, filler_total_count,
                    long_silence_count, repeated_expression_count,
                    algorithm_version, engine_type, engine_version
                ) VALUES (
                    ?, ?, 'COMPLETED', 1, 0,
                    clock_timestamp(), clock_timestamp(), clock_timestamp(),
                    2000, 1000, 0, 0, 0,
                    'speech-habits-v1', 'MOCK', 'mock-pipeline-v1'
                )
                """, ANALYSIS_ID, RECORDING_ID);
    }

    private void insertActiveAnalysis(String status) {
        insertRecording();
        jdbcTemplate.update("""
                INSERT INTO analyses (
                    id, recording_id, status, attempt_no, auto_retry_count,
                    worker_id, lease_expires_at, execution_deadline_at, started_at,
                    algorithm_version, engine_type, engine_version
                ) VALUES (
                    ?, ?, ?, 1, 0,
                    gen_random_uuid(), clock_timestamp() + interval '30 seconds',
                    clock_timestamp() + interval '10 minutes',
                    CASE WHEN ? = 'PROCESSING' THEN clock_timestamp() ELSE NULL END,
                    'speech-habits-v1', 'MOCK', 'mock-pipeline-v1'
                )
                """, ANALYSIS_ID, RECORDING_ID, status, status);
    }

    private void insertRecording() {
        jdbcTemplate.update("""
                INSERT INTO recordings (
                    id, user_id, duration_ms, mime_type, file_size_bytes, audio_sha256
                ) VALUES (?, ?, 3000, 'audio/webm', 24000, ?)
                """, RECORDING_ID, USER_ID,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    private long count(String table, String column, long id) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                id
        );
        return result == null ? 0 : result;
    }

    private String bearerToken() {
        return "Bearer " + tokenProvider.issueAccessToken(USER_ID);
    }

    private static Stream<String> invalidPasswords() {
        return Stream.of(new String[]{
                null,
                "short7",
                "a".repeat(65),
                "가".repeat(25)
        });
    }
}
