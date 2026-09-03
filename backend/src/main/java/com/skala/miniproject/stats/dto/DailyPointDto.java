package com.skala.miniproject.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPointDto(LocalDate date, int practiceCount, BigDecimal averageFillerCount) {
}
