package com.skala.miniproject.auth.jwt;

import com.skala.miniproject.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    private static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final JwtProperties properties;

    public ResponseCookie create(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(Duration.ofSeconds(properties.refreshTtlSeconds()))
                .build();
    }

    public ResponseCookie expire() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }
}
