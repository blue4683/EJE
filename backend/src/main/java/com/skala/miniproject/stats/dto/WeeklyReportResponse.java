package com.skala.miniproject.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String timezone,
        String algorithmVersion,
        boolean isPartial,
        int practiceCount,
        BigDecimal averageFillerCount,
        int previousWeekPracticeCount,
        BigDecimal previousWeekAverageFillerCount,
        BigDecimal improvementRatePercent,
        List<DailyPointDto> trend
) {
}
