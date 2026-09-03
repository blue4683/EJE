package com.skala.miniproject.analysis.controller;

import com.skala.miniproject.analysis.service.AnalysisRetryService;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.common.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** API 09 {@code POST /analyses/{analysisId}/retry}. 비즈니스 로직은 두지 않고 AnalysisRetryService 에 위임한다. */
@RestController
public class AnalysisRetryController {

    private final AnalysisRetryService analysisRetryService;

    public AnalysisRetryController(AnalysisRetryService analysisRetryService) {
        this.analysisRetryService = analysisRetryService;
    }

    /**
     * Idempotency-Key·audio 를 required=false 로 받는다. required=true 로 두면 Spring 이
     * MissingRequestHeaderException·MissingServletRequestPartException 을 던지는데, 이 두 예외는
     * GlobalExceptionHandler(공용 파일, A 소유)가 처리하지 않아 500 으로 새 나간다.
     * 이 컨트롤러 안에서 직접 null 검사를 해서 422 VALIDATION_ERROR 로 명세를 맞춘다
     * (RecordingUploadController, API 07 과 동일한 처리).
     */
    @PostMapping(value = "/analyses/{analysisId}/retry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> retry(
            @PathVariable Long analysisId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestParam(value = "audio", required = false) MultipartFile audio
    ) {
        Long userId = CurrentUser.id();
        UUID idempotencyKey = parseIdempotencyKey(idempotencyKeyHeader);
        if (audio == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        String responseBody = analysisRetryService.retry(userId, analysisId, idempotencyKey, audio);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private UUID parseIdempotencyKey(String header) {
        if (header == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
