package com.skala.miniproject.recording;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.recording.service.RecordingSubmitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * API 07 POST /recordings 통합 테스트. db/seed-dev.sql 이 적용된 실제 PostgreSQL(00-공통기반.md §8)에
 * 대해 RecordingSubmitService 를 직접 호출한다 — 테스트 트랜잭션이 끝나면 전부 롤백된다.
 *
 * userId=2(pro@example.com, seed 상 활성 분석 없음)를 기본 사용자로 쓰고, userId=1(user@example.com)은
 * seed 의 PENDING(5002) 행을 그대로 이용해 ANALYSIS_ALREADY_ACTIVE 를 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordingSubmitTest {

    private static final Long FREE_USER_WITH_ACTIVE_ANALYSIS = 1L; // seed: analyses.id=5002 PENDING
    private static final Long PRO_USER_WITHOUT_RECORDINGS = 2L;    // seed: recordings 없음

    @Autowired
    private RecordingSubmitService recordingSubmitService;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void 정상_업로드는_202_PENDING_attemptNo_1_autoRetryCount_0을_반환한다() throws IOException {
        String body = recordingSubmitService.submit(
                PRO_USER_WITHOUT_RECORDINGS, UUID.randomUUID(), validAudio());

        JsonNode data = parseData(body);
        assertThat(data.get("status").asString()).isEqualTo("PENDING");
        assertThat(data.get("attemptNo").asInt()).isEqualTo(1);
        assertThat(data.get("autoRetryCount").asInt()).isEqualTo(0);
        assertThat(data.get("recordingId").asString()).isNotBlank();
        assertThat(data.get("analysisId").asString()).isNotBlank();
    }

    @Test
    void 같은_키와_같은_파일을_재전송하면_최초_응답을_그대로_반환한다() throws IOException {
        UUID key = UUID.randomUUID();

        String first = recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, key, validAudio());
        String second = recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, key, validAudio());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void 같은_키에_다른_내용의_파일을_보내면_IDEMPOTENCY_KEY_CONFLICT다() throws IOException {
        UUID key = UUID.randomUUID();
        recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, key, validAudio());

        MultipartFile differentContent = new MockMultipartFile(
                "audio", "different.webm", "audio/webm", "totally-different-bytes".getBytes());

        assertThatThrownBy(() ->
                recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, key, differentContent))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT));
    }

    /**
     * 회귀 테스트 — 명세 「멱등 접수 계약」: "비어 있거나 16MiB를 초과한 파일은 지문 조회 전에 거절한다."
     * 이 순서가 깨지면 같은 키로 빈 파일을 보냈을 때 위 테스트처럼 CONFLICT 가 먼저 나가 버린다.
     * 빈 파일은 그 자체로 잘못된 요청이므로, 이미 다른 내용으로 키가 쓰인 상태여도 INVALID_AUDIO 여야 한다.
     */
    @Test
    void 이미_사용된_키라도_빈_파일이면_충돌이_아니라_INVALID_AUDIO다() throws IOException {
        UUID key = UUID.randomUUID();
        recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, key, validAudio());

        MultipartFile emptyAudio = new MockMultipartFile("audio", "empty.webm", "audio/webm", new byte[0]);

        assertThatThrownBy(() ->
                recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, key, emptyAudio))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AUDIO));
    }

    @Test
    void 사용자에게_이미_활성_분석이_있으면_ANALYSIS_ALREADY_ACTIVE다() throws IOException {
        assertThatThrownBy(() ->
                recordingSubmitService.submit(FREE_USER_WITH_ACTIVE_ANALYSIS, UUID.randomUUID(), validAudio()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ANALYSIS_ALREADY_ACTIVE));
    }

    @Test
    void 손상된_파일은_INVALID_AUDIO다() throws IOException {
        byte[] garbage = Files.readAllBytes(
                new ClassPathResource("fixtures/broken.webm").getFile().toPath());
        MultipartFile broken = new MockMultipartFile("audio", "broken.webm", "audio/webm", garbage);

        assertThatThrownBy(() ->
                recordingSubmitService.submit(PRO_USER_WITHOUT_RECORDINGS, UUID.randomUUID(), broken))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AUDIO));
    }

    private MultipartFile validAudio() throws IOException {
        byte[] bytes = Files.readAllBytes(new ClassPathResource("fixtures/sample-3s.webm").getFile().toPath());
        return new MockMultipartFile("audio", "sample-3s.webm", "audio/webm", bytes);
    }

    private JsonNode parseData(String responseBody) {
        JsonNode root = jsonMapper.readTree(responseBody);
        assertThat(root.get("success").asBoolean()).isTrue();
        return root.get("data");
    }
}
