package com.skala.miniproject.auth.dto;

import java.time.Instant;

public record LoginData(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant refreshExpiresAt,
        UserDto user
) {
}
