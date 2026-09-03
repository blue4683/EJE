package com.skala.miniproject.mockapi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** API 20 요청의 파형 점. amplitude 0~1, timeMs 는 100ms 간격이라고 가정하고 입력 순서를 그대로 쓴다. */
public record AmplitudePointDto(
        @NotNull @Min(0) Integer timeMs,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double amplitude
) {
}
