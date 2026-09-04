package com.skala.miniproject.pro.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.pro.dto.ProResultResponse;
import com.skala.miniproject.pro.service.ProAnalysisQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recordings")
@RequiredArgsConstructor
public class ProAnalysisController {

    private final ProAnalysisQueryService proAnalysisQueryService;

    @GetMapping("/{recordingId}/pro-analysis")
    public ResponseEntity<ApiResponse<ProResultResponse>> getProAnalysis(
            @PathVariable Long recordingId
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(proAnalysisQueryService.getProAnalysis(userId, recordingId)));
    }
}
