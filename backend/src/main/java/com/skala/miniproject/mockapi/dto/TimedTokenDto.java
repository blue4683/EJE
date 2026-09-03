package com.skala.miniproject.mockapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** API 21 요청의 STT 시각 토큰. */
public record TimedTokenDto(
        @NotBlank @Size(min = 1, max = 100) String text,
        @NotNull @Min(0) Integer startMs,
        @NotNull @Min(0) Integer endMs
) {
}
