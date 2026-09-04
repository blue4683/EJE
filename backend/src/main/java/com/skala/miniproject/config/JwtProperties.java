package com.skala.miniproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String accessAudience,
        String refreshAudience,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        String accessSecret,
        String refreshSecret
) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public JwtProperties {
        validateSecretLength(accessSecret, "Access");
        validateSecretLength(refreshSecret, "Refresh");
        if (accessSecret.equals(refreshSecret)) {
            throw new IllegalArgumentException("Access와 Refresh 비밀키는 서로 달라야 합니다.");
        }
    }

    private static void validateSecretLength(String secret, String tokenName) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(tokenName + " 비밀키는 32바이트 이상이어야 합니다.");
        }
    }
}
