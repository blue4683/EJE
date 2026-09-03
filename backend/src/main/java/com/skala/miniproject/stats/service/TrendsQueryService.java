package com.skala.miniproject.stats.service;

import com.skala.miniproject.config.AnalysisProperties;
import com.skala.miniproject.stats.dto.DailyPointDto;
import com.skala.miniproject.stats.dto.TrendsResponse;
import com.skala.miniproject.stats.repository.DailyStatsRepository;
import com.skala.miniproject.stats.support.SeoulDateRange;
import com.skala.miniproject.stats.support.SeoulDateRange.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendsQueryService {

    private final DailyStatsRepository dailyStatsRepository;
    private final AnalysisProperties analysisProperties;

    public TrendsResponse getTrends(
            Long userId,
            Period period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate today = LocalDate.now(SeoulDateRange.SEOUL);
        SeoulDateRange range = SeoulDateRange.resolve(period, startDate, endDate, today);
        String algorithmVersion = analysisProperties.algorithmVersion();

        Map<LocalDate, DailyStatsRepository.DailyStatsView> statsByDate = dailyStatsRepository
                .findDailyStats(userId, algorithmVersion, range.fromUtc(), range.toUtc())
                .stream()
                .collect(Collectors.toMap(DailyStatsRepository.DailyStatsView::getDate, Function.identity()));

        var points = range.dates()
                .map(date -> toPoint(date, statsByDate.get(date)))
                .toList();

        return new TrendsResponse(
                period,
                range.startDate(),
                range.endDate(),
                SeoulDateRange.TIMEZONE,
                algorithmVersion,
                points
        );
    }

    private DailyPointDto toPoint(LocalDate date, DailyStatsRepository.DailyStatsView stats) {
        if (stats == null) {
            return new DailyPointDto(date, 0, null);
        }

        int practiceCount = Math.toIntExact(stats.getPracticeCount());
        BigDecimal averageFillerCount = BigDecimal.valueOf(stats.getFillerTotalCount())
                .divide(BigDecimal.valueOf(stats.getPracticeCount()), 2, RoundingMode.HALF_UP);
        return new DailyPointDto(date, practiceCount, averageFillerCount);
    }
}
