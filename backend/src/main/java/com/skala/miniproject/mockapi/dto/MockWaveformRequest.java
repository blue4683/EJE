package com.skala.miniproject.mockapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** API 20 — POST /mock/waveform-analysis 요청 본문. */
public record MockWaveformRequest(
        @NotNull @Min(1000) @Max(60000) Integer durationMs,
        @NotEmpty @Valid List<AmplitudePointDto> waveform
) {
}
