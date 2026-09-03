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

/** API 08 {@code GET /analyses/{analysisId}/status}. */
@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
public class AnalysisStatusController {

    private final AnalysisStatusQueryService analysisStatusQueryService;

    @GetMapping("/{analysisId}/status")
    public ResponseEntity<ApiResponse<AnalysisStatusResponse>> getStatus(@PathVariable Long analysisId) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(analysisStatusQueryService.getByAnalysisId(userId, analysisId)));
    }
}
