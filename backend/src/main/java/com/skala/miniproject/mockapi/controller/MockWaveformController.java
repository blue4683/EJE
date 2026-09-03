package com.skala.miniproject.mockapi.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.mockapi.dto.MockWaveformRequest;
import com.skala.miniproject.mockapi.dto.MockWaveformResponse;
import com.skala.miniproject.mockapi.service.MockAnalysisService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** API 20 — MOCK_001. local/test 에서만 등록된다. prod 라우팅은 MockApiSecurityChain 이 담당한다. */
@RestController
@Profile({"local", "test"})
public class MockWaveformController {

    private final MockAnalysisService mockAnalysisService;

    public MockWaveformController(MockAnalysisService mockAnalysisService) {
        this.mockAnalysisService = mockAnalysisService;
    }

    @PostMapping("/mock/waveform-analysis")
    public ApiResponse<MockWaveformResponse> analyze(@Valid @RequestBody MockWaveformRequest request) {
        return ApiResponse.ok(mockAnalysisService.analyzeWaveform(request));
    }
}
