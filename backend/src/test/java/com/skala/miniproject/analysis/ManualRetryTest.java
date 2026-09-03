package com.skala.miniproject.analysis;

import com.skala.miniproject.analysis.idempotency.RequestFingerprint;
import com.skala.miniproject.auth.jwt.JwtTokenProvider;
import com.skala.miniproject.domain.idempotency.IdempotencyOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 09 {@code POST /analyses/{analysisId}/retry} 통합 테스트. db/seed-dev.sql 의 analyses.id=5003
 * (recording 103, FAILED, STT_TIMEOUT, attemptNo=1, userId=1 소유)을 그대로 쓴다.
 *
 * seed 의 recordings.audio_sha256 값은 API명세서.md 예제 대조용 임의 문자열이라 실제 픽스처 파일의
 * 해시와 다르다. 정상 재시도를 검증하려면 먼저 그 값을 실제 픽스처의 해시로 맞춰야 한다 —
 * 그렇지 않으면 명세상 정상 케이스인데도 AUDIO_MISMATCH 로 튕긴다.
 *
 * userId=1 은 seed 상 analyses.id=5002(recording 102, PENDING) 도 함께 소유한다
 * (RecordingSubmitTest 가 ANALYSIS_ALREADY_ACTIVE 검증에 쓰는 바로 그 행). 이 행을 그대로 두면
 * "사용자 활성 작업 부재"(⑥) 검사에서 정상 재시도조차 409 로 막히므로, 재시도가 성공해야 하는
 * 테스트를 위해 이 행도 일시적으로 비활성 상태로 돌려 둔다.
 * @BeforeEach/@AfterEach 로 이 두 행을 매 테스트 전후로 seed 원본과 맞춘다(UserControllerTest 와 동일한 패턴).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManualRetryTest {

    private static final Long OWNER = 1L;
    private static final Long FAILED_ANALYSIS_ID = 5003L;
    private static final Long FAILED_RECORDING_ID = 103L;
    private static final Long OTHER_ACTIVE_ANALYSIS_ID = 5002L; // seed: userId=1 소유, PENDING
    private static final String SEED_AUDIO_SHA256 =
            "df175811d1a0ad1bd890c5009af63f00528948f8cfe5b442d8260c77caa40a02";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private byte[] originalAudioBytes;
    private String originalAudioSha256;

    @BeforeEach
    void alignRecordingAudioWithFixture() throws Exception {
        originalAudioBytes = Files.readAllBytes(new ClassPathResource("fixtures/sample-3s.webm").getFile().toPath());
        originalAudioSha256 = sha256Hex(originalAudioBytes);
        jdbcTemplate.update("UPDATE recordings SET audio_sha256 = ? WHERE id = ?",
                originalAudioSha256, FAILED_RECORDING_ID);
        // ⑥ 사용자 활성 작업 부재 검사에 걸리지 않도록, userId=1 의 다른 PENDING 행을 일시적으로 치운다.
        jdbcTemplate.update("""
                UPDATE analyses SET status = 'FAILED', worker_id = NULL, lease_expires_at = NULL,
                    finished_at = now(), failure_code = 'WORKER_LOST'
                WHERE id = ?
                """, OTHER_ACTIVE_ANALYSIS_ID);
    }

    @AfterEach
    void restoreSeedState() {
        // 성공한 재시도는 실제 비동기 파이프라인(B5)을 등록한다 — 드물게 이 복원보다 먼저
        // COMPLETED 로 끝나 측정값 5개가 채워질 수 있으므로, 그것들도 함께 NULL 로 되돌려야
        // ck_analyses_result_presence(비 COMPLETED 상태는 측정값이 전부 NULL) 를 어기지 않는다.
        jdbcTemplate.update("""
                UPDATE analyses SET
                    status = 'FAILED', attempt_no = 1, auto_retry_count = 3, failure_code = 'STT_TIMEOUT',
                    worker_id = NULL, lease_expires_at = NULL,
                    execution_deadline_at = '2026-09-03 03:16:00+00',
                    started_at = '2026-09-03 03:06:01+00', finished_at = '2026-09-03 03:07:30+00',
                    speech_duration_ms = NULL, silence_duration_ms = NULL, filler_total_count = NULL,
                    long_silence_count = NULL, repeated_expression_count = NULL
                WHERE id = ?
                """, FAILED_ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM filler_breakdowns WHERE analysis_id = ?", FAILED_ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM analysis_pro_results WHERE analysis_id = ?", FAILED_ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM segment_analyses WHERE analysis_id = ?", FAILED_ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM filler_timeline_events WHERE analysis_id = ?", FAILED_ANALYSIS_ID);
        jdbcTemplate.update("UPDATE recordings SET audio_sha256 = ? WHERE id = ?",
                SEED_AUDIO_SHA256, FAILED_RECORDING_ID);
        jdbcTemplate.update("""
                UPDATE analyses SET
                    status = 'PENDING', attempt_no = 1, auto_retry_count = 0, failure_code = NULL,
                    worker_id = gen_random_uuid(), lease_expires_at = '2026-09-03 03:05:30+00',
                    execution_deadline_at = '2026-09-03 03:15:00+00', started_at = NULL, finished_at = NULL
                WHERE id = ?
                """, OTHER_ACTIVE_ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM api_idempotency_keys WHERE analysis_id in (?, ?)",
                FAILED_ANALYSIS_ID, OTHER_ACTIVE_ANALYSIS_ID);
    }

    @Test
    void 멱등_재전송_판정이_상태_검사보다_먼저다() throws Exception {
        UUID key = UUID.randomUUID();

        MvcResult first = retry(FAILED_ANALYSIS_ID, key, originalAudioBytes, OWNER)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.attemptNo").value(2))
                .andReturn();

        // 이 시점에 analyses.status 는 이미 FAILED 가 아니다(PENDING/PROCESSING) — 그런데도
        // 같은 키로 재전송하면 상태 검사(③)로 가지 않고 최초 202 본문을 그대로 반환해야 한다.
        MvcResult second = retry(FAILED_ANALYSIS_ID, key, originalAudioBytes, OWNER)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();

        // response_body 는 jsonb 컬럼이라 DB 왕복 후 키 순서·공백이 원문과 달라질 수 있다(Postgres의
        // jsonb 정규화) — 바이트 동일이 아니라 JSON 트리 동일(같은 202 스냅샷)로 비교한다.
        assertThat(jsonMapper.readTree(second.getResponse().getContentAsString()))
                .isEqualTo(jsonMapper.readTree(first.getResponse().getContentAsString()));
    }

    @Test
    void 다른_파일이면_AUDIO_MISMATCH다() throws Exception {
        byte[] differentAudio = "이 바이트는 recording 103 의 원본과 다르다".getBytes(StandardCharsets.UTF_8);

        retry(FAILED_ANALYSIS_ID, UUID.randomUUID(), differentAudio, OWNER)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("AUDIO_MISMATCH"));
    }

    @Test
    void attemptNo가_4면_MANUAL_RETRY_LIMIT_EXCEEDED다() throws Exception {
        jdbcTemplate.update("UPDATE analyses SET attempt_no = 4 WHERE id = ?", FAILED_ANALYSIS_ID);

        retry(FAILED_ANALYSIS_ID, UUID.randomUUID(), originalAudioBytes, OWNER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MANUAL_RETRY_LIMIT_EXCEEDED"));
    }

    @Test
    void FAILED가_아니면_INVALID_ANALYSIS_STATE다() throws Exception {
        // 5001 은 seed 상 COMPLETED(recording 101 소유, userId=1).
        retry(5001L, UUID.randomUUID(), originalAudioBytes, OWNER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_ANALYSIS_STATE"));
    }

    @Test
    void 재시도해도_submitted_at은_바뀌지_않는다() throws Exception {
        Object before = jdbcTemplate.queryForObject(
                "select submitted_at from recordings where id = ?", Object.class, FAILED_RECORDING_ID);

        retry(FAILED_ANALYSIS_ID, UUID.randomUUID(), originalAudioBytes, OWNER)
                .andExpect(status().isAccepted());

        Object after = jdbcTemplate.queryForObject(
                "select submitted_at from recordings where id = ?", Object.class, FAILED_RECORDING_ID);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void 재시도하면_auto_retry_count가_0으로_초기화된다() throws Exception {
        // seed 의 auto_retry_count=3(자동 재시도 소진 후 FAILED) — 재시도 응답은 차수마다 0으로 되돌아가야 한다.
        retry(FAILED_ANALYSIS_ID, UUID.randomUUID(), originalAudioBytes, OWNER)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.autoRetryCount").value(0));
    }

    @Test
    void 재시도해도_analysisId는_같다() throws Exception {
        retry(FAILED_ANALYSIS_ID, UUID.randomUUID(), originalAudioBytes, OWNER)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.analysisId").value(String.valueOf(FAILED_ANALYSIS_ID)))
                .andExpect(jsonPath("$.data.recordingId").value(String.valueOf(FAILED_RECORDING_ID)));
    }

    @Test
    void 재시도_지문은_RETRY_ANALYSIS와_대상ID를_포함한다() throws Exception {
        retry(FAILED_ANALYSIS_ID, UUID.randomUUID(), originalAudioBytes, OWNER)
                .andExpect(status().isAccepted());

        String expectedFingerprint = RequestFingerprint.build(
                IdempotencyOperation.RETRY_ANALYSIS, String.valueOf(FAILED_ANALYSIS_ID), originalAudioSha256);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select operation, request_fingerprint, analysis_id, recording_id "
                        + "from api_idempotency_keys where analysis_id = ?", FAILED_ANALYSIS_ID);

        assertThat(row.get("operation")).isEqualTo("RETRY_ANALYSIS");
        assertThat(row.get("request_fingerprint")).isEqualTo(expectedFingerprint);
        assertThat(row.get("analysis_id")).isEqualTo(FAILED_ANALYSIS_ID);
        assertThat(row.get("recording_id")).isEqualTo(FAILED_RECORDING_ID);
    }

    private org.springframework.test.web.servlet.ResultActions retry(
            Long analysisId, UUID idempotencyKey, byte[] audioBytes, Long userId) throws Exception {
        MockMultipartFile audioPart = new MockMultipartFile("audio", "retry.webm", "audio/webm", audioBytes);
        String path = "/analyses/" + analysisId + "/retry";
        return mockMvc.perform(multipart(path)
                .file(audioPart)
                .servletPath(path)
                .header(HttpHeaders.AUTHORIZATION, bearerToken(userId))
                .header("Idempotency-Key", idempotencyKey.toString()));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + tokenProvider.issueAccessToken(userId);
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
