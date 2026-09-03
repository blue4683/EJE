package com.skala.miniproject.stats.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.stats.dto.WeeklyReportResponse;
import com.skala.miniproject.stats.service.WeeklyReportQueryService;
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
public class WeeklyReportController {

    private final WeeklyReportQueryService weeklyReportQueryService;

    @GetMapping("/weekly-report")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getWeeklyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(
                weeklyReportQueryService.getWeeklyReport(userId, weekStartDate)
        ));
    }
}
