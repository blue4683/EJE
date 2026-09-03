package com.skala.miniproject.analysis.idempotency;

import com.skala.miniproject.domain.idempotency.IdempotencyOperation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestFingerprintTest {

    @Test
    void 빈_바이트_배열의_SHA256은_잘_알려진_값이다() {
        assertThat(RequestFingerprint.sha256Hex(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void 소문자_64자_16진수를_반환한다() {
        String hex = RequestFingerprint.sha256Hex("hello".getBytes(StandardCharsets.UTF_8));

        assertThat(hex).matches("^[0-9a-f]{64}$");
    }

    @Test
    void 지문은_operation_LF_targetAnalysisId_LF_audioSha256_순서다() {
        String audioSha256 = RequestFingerprint.sha256Hex("audio-bytes".getBytes(StandardCharsets.UTF_8));
        String expectedRaw = "CREATE_RECORDING" + "\n" + "-" + "\n" + audioSha256;

        String actual = RequestFingerprint.build(IdempotencyOperation.CREATE_RECORDING, "-", audioSha256);

        assertThat(actual).isEqualTo(RequestFingerprint.sha256Hex(expectedRaw.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void CRLF가_아니라_LF_하나만_구분자로_쓴다() {
        String audioSha256 = RequestFingerprint.sha256Hex("audio-bytes".getBytes(StandardCharsets.UTF_8));
        String withCrLf = "CREATE_RECORDING" + "\r\n" + "-" + "\r\n" + audioSha256;

        String actual = RequestFingerprint.build(IdempotencyOperation.CREATE_RECORDING, "-", audioSha256);

        assertThat(actual).isNotEqualTo(RequestFingerprint.sha256Hex(withCrLf.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void operation이_다르면_같은_대상과_음성이어도_지문이_다르다() {
        String audioSha256 = RequestFingerprint.sha256Hex("audio-bytes".getBytes(StandardCharsets.UTF_8));

        String createFingerprint = RequestFingerprint.build(IdempotencyOperation.CREATE_RECORDING, "-", audioSha256);
        String retryFingerprint = RequestFingerprint.build(IdempotencyOperation.RETRY_ANALYSIS, "5001", audioSha256);

        assertThat(createFingerprint).isNotEqualTo(retryFingerprint);
    }
}
