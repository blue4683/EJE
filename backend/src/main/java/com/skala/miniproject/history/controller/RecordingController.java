package com.skala.miniproject.history.controller;

import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.security.CurrentUser;
import com.skala.miniproject.history.dto.RecordingDetailResponse;
import com.skala.miniproject.history.service.RecordingDeleteService;
import com.skala.miniproject.history.service.RecordingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingQueryService recordingQueryService;
    private final RecordingDeleteService recordingDeleteService;

    @GetMapping("/{recordingId}")
    public ResponseEntity<ApiResponse<RecordingDetailResponse>> getRecording(
            @PathVariable Long recordingId
    ) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(ApiResponse.ok(recordingQueryService.getRecording(userId, recordingId)));
    }

    @DeleteMapping("/{recordingId}")
    public ResponseEntity<Void> deleteRecording(@PathVariable Long recordingId) {
        Long userId = CurrentUser.id();
        recordingDeleteService.deleteRecording(userId, recordingId);
        return ResponseEntity.noContent().build();
    }
}
