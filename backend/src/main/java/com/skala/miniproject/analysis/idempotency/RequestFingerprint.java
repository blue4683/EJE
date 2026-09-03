package com.skala.miniproject.analysis.idempotency;

import com.skala.miniproject.domain.idempotency.IdempotencyOperation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 멱등 접수 계약의 지문 공식 (API명세서.md 「멱등 접수 계약」).
 * requestFingerprint = SHA256(UTF8(operation + LF + targetAnalysisId + LF + audioSha256)).
 * 파일명·MIME 문자열·multipart boundary 는 포함하지 않는다.
 */
public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public static String build(IdempotencyOperation operation, String targetAnalysisId, String audioSha256) {
        String raw = operation.name() + "\n" + targetAnalysisId + "\n" + audioSha256;
        return sha256Hex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
