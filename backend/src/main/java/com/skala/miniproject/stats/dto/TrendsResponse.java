package com.skala.miniproject.stats.dto;

import com.skala.miniproject.stats.support.SeoulDateRange.Period;

import java.time.LocalDate;
import java.util.List;

public record TrendsResponse(
        Period period,
        LocalDate startDate,
        LocalDate endDate,
        String timezone,
        String algorithmVersion,
        List<DailyPointDto> points
) {
}
