package com.skala.miniproject.stats.service;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.user.Plan;
import com.skala.miniproject.stats.dto.ComparisonDeltaDto;
import com.skala.miniproject.stats.dto.ComparisonItemDto;
import com.skala.miniproject.stats.dto.ComparisonResponse;
import com.skala.miniproject.stats.repository.CompareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CompareQueryService {

    private final CompareRepository compareRepository;

    public ComparisonResponse compare(Long userId, Long recordingId, Long targetRecordingId) {
        var current = compareRepository.findOwnedCurrent(userId, recordingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (Plan.valueOf(current.getPlan()) != Plan.PRO) {
            throw new BusinessException(ErrorCode.PRO_REQUIRED);
        }
        if (statusOf(current) != AnalysisStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }

        var target = targetRecordingId == null
                ? findAutomaticTarget(userId, current)
                : findExplicitTarget(userId, recordingId, targetRecordingId, current);

        ComparisonItemDto currentItem = toItem(current);
        ComparisonItemDto targetItem = toItem(target);
        return new ComparisonResponse(
                currentItem,
                targetItem,
                delta(currentItem, targetItem),
                current.getAlgorithmVersion()
        );
    }

    private CompareRepository.ComparisonView findAutomaticTarget(
            Long userId,
            CompareRepository.ComparisonView current
    ) {
        return compareRepository.findPreviousCompleted(
                        userId,
                        current.getRecordingId(),
                        current.getSubmittedAt(),
                        current.getAlgorithmVersion()
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_TARGET_NOT_FOUND));
    }

    private CompareRepository.ComparisonView findExplicitTarget(
            Long userId,
            Long recordingId,
            Long targetRecordingId,
            CompareRepository.ComparisonView current
    ) {
        if (recordingId.equals(targetRecordingId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        var target = compareRepository.findOwnedTarget(userId, targetRecordingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_TARGET_NOT_FOUND));
        if (!isBefore(target, current)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (statusOf(target) != AnalysisStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.COMPARISON_TARGET_NOT_FOUND);
        }
        if (!current.getAlgorithmVersion().equals(target.getAlgorithmVersion())) {
            throw new BusinessException(ErrorCode.ANALYSIS_VERSION_MISMATCH);
        }
        return target;
    }

    private boolean isBefore(
            CompareRepository.ComparisonView target,
            CompareRepository.ComparisonView current
    ) {
        int timeComparison = target.getSubmittedAt().compareTo(current.getSubmittedAt());
        return timeComparison < 0
                || (timeComparison == 0 && target.getRecordingId() < current.getRecordingId());
    }

    private AnalysisStatus statusOf(CompareRepository.ComparisonView row) {
        return AnalysisStatus.valueOf(row.getStatus());
    }

    private ComparisonItemDto toItem(CompareRepository.ComparisonView row) {
        return new ComparisonItemDto(
                String.valueOf(row.getRecordingId()),
                row.getDurationMs(),
                row.getFillerTotalCount(),
                row.getSilenceDurationMs(),
                row.getWordsPerMinute()
        );
    }

    private ComparisonDeltaDto delta(ComparisonItemDto current, ComparisonItemDto target) {
        Integer wordsPerMinuteChange = current.wordsPerMinute() == null || target.wordsPerMinute() == null
                ? null
                : current.wordsPerMinute() - target.wordsPerMinute();
        return new ComparisonDeltaDto(
                current.fillerTotalCount() - target.fillerTotalCount(),
                current.silenceDurationMs() - target.silenceDurationMs(),
                wordsPerMinuteChange
        );
    }
}
