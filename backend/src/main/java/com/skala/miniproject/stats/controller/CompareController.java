package com.skala.miniproject.stats.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.stats.dto.ComparisonResponse;
import com.skala.miniproject.stats.service.CompareQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recordings")
@RequiredArgsConstructor
public class CompareController {

    private final CompareQueryService compareQueryService;

    @GetMapping("/{recordingId}/compare")
    public ResponseEntity<ApiResponse<ComparisonResponse>> compare(
            @PathVariable Long recordingId,
            @RequestParam(required = false) Long targetRecordingId
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(
                compareQueryService.compare(userId, recordingId, targetRecordingId)
        ));
    }
}
