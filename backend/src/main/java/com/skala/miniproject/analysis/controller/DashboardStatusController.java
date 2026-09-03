package com.skala.miniproject.analysis.controller;

import com.skala.miniproject.analysis.dto.AnalysisStatusResponse;
import com.skala.miniproject.analysis.service.AnalysisStatusQueryService;
import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 13 {@code GET /dashboard/recordings/{recordingId}/status}. API 08(AnalysisStatusController)과
 * 같은 서비스·같은 DTO 를 쓴다. A5 의 {@code DashboardController} 와는 별개 클래스다(§4).
 */
@RestController
@RequestMapping("/dashboard/recordings")
@RequiredArgsConstructor
public class DashboardStatusController {

    private final AnalysisStatusQueryService analysisStatusQueryService;

    @GetMapping("/{recordingId}/status")
    public ResponseEntity<ApiResponse<AnalysisStatusResponse>> getStatus(@PathVariable Long recordingId) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(analysisStatusQueryService.getByRecordingId(userId, recordingId)));
    }
}
