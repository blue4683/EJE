package com.skala.miniproject.history.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.history.dto.BasicResultResponse;
import com.skala.miniproject.history.dto.RecentAnalysesResponse;
import com.skala.miniproject.history.dto.RecordingPageResponse;
import com.skala.miniproject.history.service.RecordingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RecordingQueryService recordingQueryService;

    @GetMapping("/recordings")
    public ResponseEntity<ApiResponse<RecordingPageResponse>> getRecordings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(recordingQueryService.getRecordingPage(userId, page, size)));
    }

    @GetMapping("/recordings/{recordingId}/result")
    public ResponseEntity<ApiResponse<BasicResultResponse>> getBasicResult(
            @PathVariable Long recordingId
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(recordingQueryService.getBasicResult(userId, recordingId)));
    }

    @GetMapping("/recent-analyses")
    public ResponseEntity<ApiResponse<RecentAnalysesResponse>> getRecentAnalyses() {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(recordingQueryService.getRecentAnalyses(userId)));
    }
}
