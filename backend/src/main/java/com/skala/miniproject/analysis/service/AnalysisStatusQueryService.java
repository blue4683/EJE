package com.skala.miniproject.analysis.service;

import com.skala.miniproject.analysis.dto.AnalysisStatusResponse;
import com.skala.miniproject.analysis.repository.AnalysisStatusQueryRepository;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.analysis.Analysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API 08({@code analysisId} 조회)·API 13({@code recordingId} 조회)이 공유하는 조회 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisStatusQueryService {

    private final AnalysisStatusQueryRepository analysisStatusQueryRepository;

    public AnalysisStatusResponse getByAnalysisId(Long userId, Long analysisId) {
        Analysis analysis = analysisStatusQueryRepository.findByIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return AnalysisStatusResponse.from(analysis);
    }

    public AnalysisStatusResponse getByRecordingId(Long userId, Long recordingId) {
        Analysis analysis = analysisStatusQueryRepository.findByRecordingIdAndUserId(recordingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return AnalysisStatusResponse.from(analysis);
    }
}
