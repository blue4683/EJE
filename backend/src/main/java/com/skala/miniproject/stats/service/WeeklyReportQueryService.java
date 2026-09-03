package com.skala.miniproject.stats.service;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.config.AnalysisProperties;
import com.skala.miniproject.domain.user.Plan;
import com.skala.miniproject.domain.user.User;
import com.skala.miniproject.domain.user.UserRepository;
import com.skala.miniproject.stats.dto.DailyPointDto;
import com.skala.miniproject.stats.dto.WeeklyReportResponse;
import com.skala.miniproject.stats.repository.WeeklyStatsRepository;
import com.skala.miniproject.stats.support.SeoulDateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportQueryService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final WeeklyStatsRepository weeklyStatsRepository;
    private final UserRepository userRepository;
    private final AnalysisProperties analysisProperties;

    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate requestedWeekStart) {
        requirePro(userId);

        LocalDate today = LocalDate.now(SeoulDateRange.SEOUL);
        WeeklyRanges ranges = resolveRanges(requestedWeekStart, today);
        String algorithmVersion = analysisProperties.algorithmVersion();

        Map<LocalDate, WeeklyStatsRepository.WeeklyDailyStatsView> statsByDate = weeklyStatsRepository
                .findDailyStats(
                        userId,
                        algorithmVersion,
                        ranges.all().fromUtc(),
                        ranges.all().toUtc()
                )
                .stream()
                .collect(Collectors.toMap(
                        WeeklyStatsRepository.WeeklyDailyStatsView::getDate,
                        Function.identity()
                ));

        Totals current = totals(ranges.current(), statsByDate);
        Totals previous = totals(ranges.previous(), statsByDate);
        var trend = ranges.current().dates()
                .map(date -> toPoint(date, statsByDate.get(date)))
                .toList();

        return new WeeklyReportResponse(
                ranges.current().startDate(),
                ranges.current().endDate(),
                SeoulDateRange.TIMEZONE,
                algorithmVersion,
                ranges.current().endDate().isAfter(today),
                Math.toIntExact(current.practiceCount()),
                roundedAverage(current),
                Math.toIntExact(previous.practiceCount()),
                roundedAverage(previous),
                improvementRate(current, previous),
                trend
        );
    }

    private void requirePro(Long userId) {
        Plan plan = userRepository.findById(userId)
                .map(User::getPlan)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (plan != Plan.PRO) {
            throw new BusinessException(ErrorCode.PRO_REQUIRED);
        }
    }

    private WeeklyRanges resolveRanges(LocalDate requestedWeekStart, LocalDate today) {
        LocalDate weekStart = requestedWeekStart == null
                ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : requestedWeekStart;
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY || weekStart.isAfter(today)) {
            throw validationError();
        }

        try {
            SeoulDateRange current = new SeoulDateRange(weekStart, weekStart.plusDays(6));
            SeoulDateRange previous = new SeoulDateRange(weekStart.minusWeeks(1), weekStart.minusDays(1));
            LocalDate dataEndDate = current.endDate().isAfter(today) ? today : current.endDate();
            SeoulDateRange all = new SeoulDateRange(previous.startDate(), dataEndDate);
            return new WeeklyRanges(current, previous, all);
        } catch (DateTimeException e) {
            throw validationError();
        }
    }

    private Totals totals(
            SeoulDateRange range,
            Map<LocalDate, WeeklyStatsRepository.WeeklyDailyStatsView> statsByDate
    ) {
        long practiceCount = 0;
        long fillerTotalCount = 0;
        for (LocalDate date : range.dates().toList()) {
            var stats = statsByDate.get(date);
            if (stats != null) {
                practiceCount += stats.getPracticeCount();
                fillerTotalCount += stats.getFillerTotalCount();
            }
        }
        return new Totals(practiceCount, fillerTotalCount);
    }

    private DailyPointDto toPoint(
            LocalDate date,
            WeeklyStatsRepository.WeeklyDailyStatsView stats
    ) {
        if (stats == null) {
            return new DailyPointDto(date, 0, null);
        }
        Totals totals = new Totals(stats.getPracticeCount(), stats.getFillerTotalCount());
        return new DailyPointDto(
                date,
                Math.toIntExact(stats.getPracticeCount()),
                roundedAverage(totals)
        );
    }

    private BigDecimal roundedAverage(Totals totals) {
        if (totals.practiceCount() == 0) {
            return null;
        }
        return BigDecimal.valueOf(totals.fillerTotalCount())
                .divide(BigDecimal.valueOf(totals.practiceCount()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal improvementRate(Totals current, Totals previous) {
        if (current.practiceCount() == 0
                || previous.practiceCount() == 0
                || previous.fillerTotalCount() == 0) {
            return null;
        }

        BigDecimal numerator = BigDecimal.valueOf(previous.fillerTotalCount())
                .multiply(BigDecimal.valueOf(current.practiceCount()))
                .subtract(BigDecimal.valueOf(current.fillerTotalCount())
                        .multiply(BigDecimal.valueOf(previous.practiceCount())))
                .multiply(ONE_HUNDRED);
        BigDecimal denominator = BigDecimal.valueOf(previous.fillerTotalCount())
                .multiply(BigDecimal.valueOf(current.practiceCount()));
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BusinessException validationError() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    private record Totals(long practiceCount, long fillerTotalCount) {
    }

    private record WeeklyRanges(
            SeoulDateRange current,
            SeoulDateRange previous,
            SeoulDateRange all
    ) {
    }
}
