package com.skala.miniproject.mockapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** API 21 — POST /mock/transcript-analysis 요청 본문. tokens 최대 1000개. */
public record MockTranscriptRequest(
        @NotNull @Min(1000) @Max(60000) Integer durationMs,
        @NotNull @Size(max = 1000) @Valid List<TimedTokenDto> tokens
) {
}
