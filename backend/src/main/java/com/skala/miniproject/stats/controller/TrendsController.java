package com.skala.miniproject.stats.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.stats.dto.TrendsResponse;
import com.skala.miniproject.stats.service.TrendsQueryService;
import com.skala.miniproject.stats.support.SeoulDateRange.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class TrendsController {

    private final TrendsQueryService trendsQueryService;

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<TrendsResponse>> getTrends(
            @RequestParam(defaultValue = "WEEK") Period period,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(
                trendsQueryService.getTrends(userId, period, startDate, endDate)
        ));
    }
}
