package com.skala.miniproject.mockapi.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.mockapi.dto.MockTranscriptRequest;
import com.skala.miniproject.mockapi.dto.MockTranscriptResponse;
import com.skala.miniproject.mockapi.service.MockAnalysisService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** API 21 — MOCK_002. local/test 에서만 등록된다. prod 라우팅은 MockApiSecurityChain 이 담당한다. */
@RestController
@Profile({"local", "test"})
public class MockTranscriptController {

    private final MockAnalysisService mockAnalysisService;

    public MockTranscriptController(MockAnalysisService mockAnalysisService) {
        this.mockAnalysisService = mockAnalysisService;
    }

    @PostMapping("/mock/transcript-analysis")
    public ApiResponse<MockTranscriptResponse> analyze(@Valid @RequestBody MockTranscriptRequest request) {
        return ApiResponse.ok(mockAnalysisService.analyzeTranscript(request));
    }
}
