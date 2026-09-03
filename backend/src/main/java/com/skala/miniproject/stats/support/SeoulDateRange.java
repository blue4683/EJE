package com.skala.miniproject.stats.support;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Stream;

public record SeoulDateRange(LocalDate startDate, LocalDate endDate) {

    public static final String TIMEZONE = "Asia/Seoul";
    public static final ZoneId SEOUL = ZoneId.of(TIMEZONE);

    private static final int WEEK_DAYS = 7;
    private static final int MONTH_DAYS = 30;
    private static final int MAX_CUSTOM_DAYS = 366;

    public SeoulDateRange {
        Objects.requireNonNull(startDate);
        Objects.requireNonNull(endDate);
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일 이후일 수 없습니다.");
        }
    }

    public static SeoulDateRange resolve(
            Period period,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today
    ) {
        if (period == null || today == null) {
            throw validationError();
        }

        return switch (period) {
            case WEEK -> recentDays(WEEK_DAYS, startDate, endDate, today);
            case MONTH -> recentDays(MONTH_DAYS, startDate, endDate, today);
            case CUSTOM -> custom(startDate, endDate, today);
        };
    }

    public Instant fromUtc() {
        return startDate.atStartOfDay(SEOUL).toInstant();
    }

    public Instant toUtc() {
        return endDate.plusDays(1).atStartOfDay(SEOUL).toInstant();
    }

    public Stream<LocalDate> dates() {
        return startDate.datesUntil(endDate.plusDays(1));
    }

    private static SeoulDateRange recentDays(
            int days,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate today
    ) {
        if (startDate != null || endDate != null) {
            throw validationError();
        }
        return new SeoulDateRange(today.minusDays(days - 1L), today);
    }

    private static SeoulDateRange custom(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate == null
                || endDate == null
                || startDate.isAfter(endDate)
                || endDate.isAfter(today)
                || ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_CUSTOM_DAYS) {
            throw validationError();
        }
        return new SeoulDateRange(startDate, endDate);
    }

    private static BusinessException validationError() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    public enum Period {
        WEEK,
        MONTH,
        CUSTOM
    }
}
