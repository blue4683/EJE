package com.skala.miniproject.history.service;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.EngineType;
import com.skala.miniproject.domain.analysis.FailureCode;
import com.skala.miniproject.domain.user.Plan;
import com.skala.miniproject.domain.user.User;
import com.skala.miniproject.domain.user.UserRepository;
import com.skala.miniproject.history.dto.AnalysisStatusDto;
import com.skala.miniproject.history.dto.BasicDto;
import com.skala.miniproject.history.dto.BasicResultResponse;
import com.skala.miniproject.history.dto.FillerBreakdownDto;
import com.skala.miniproject.history.dto.ProAccessDto;
import com.skala.miniproject.history.dto.RecentAnalysesResponse;
import com.skala.miniproject.history.dto.RecordingDetailResponse;
import com.skala.miniproject.history.dto.RecordingPageResponse;
import com.skala.miniproject.history.dto.RecordingSummaryDto;
import com.skala.miniproject.history.repository.AnalysisQueryRepository;
import com.skala.miniproject.history.repository.RecordingQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordingQueryService {

    private static final List<String> LOCKED_FEATURES = List.of(
            "waveform", "silence", "speed", "timeline", "segment",
            "repetition", "comparison", "coaching", "weeklyReport"
    );

    private final RecordingQueryRepository recordingQueryRepository;
    private final AnalysisQueryRepository analysisQueryRepository;
    private final UserRepository userRepository;

    public RecordingDetailResponse getRecording(Long userId, Long recordingId) {
        Plan plan = getPlan(userId);
        var row = findOwned(userId, recordingId);
        AnalysisStatus status = statusOf(row);

        return new RecordingDetailResponse(
                id(row.getRecordingId()),
                row.getSubmittedAt(),
                row.getDurationMs(),
                row.getMimeType(),
                row.getFileSizeBytes(),
                analysisStatus(row, status),
                status == AnalysisStatus.COMPLETED ? basic(row) : null,
                proAccess(plan, status, row.getRecordingId()),
                row.getAlgorithmVersion(),
                EngineType.valueOf(row.getEngineType()),
                row.getEngineVersion()
        );
    }

    public RecordingPageResponse getRecordingPage(Long userId, int page, int size) {
        validatePage(page, size);
        getPlan(userId);

        long totalElements = recordingQueryRepository.countOwned(userId);
        List<RecordingSummaryDto> content = recordingQueryRepository
                .findOwnedPage(userId, PageRequest.of(page, size))
                .stream()
                .map(this::summary)
                .toList();
        int totalPages = totalElements == 0 ? 0 : Math.toIntExact((totalElements + size - 1) / size);

        return new RecordingPageResponse(content, page, size, totalElements, totalPages);
    }

    public BasicResultResponse getBasicResult(Long userId, Long recordingId) {
        Plan plan = getPlan(userId);
        var row = findOwned(userId, recordingId);
        AnalysisStatus status = statusOf(row);
        if (status != AnalysisStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }

        return new BasicResultResponse(
                id(row.getRecordingId()),
                id(row.getAnalysisId()),
                status,
                basic(row),
                proAccess(plan, status, row.getRecordingId())
        );
    }

    public RecentAnalysesResponse getRecentAnalyses(Long userId) {
        getPlan(userId);
        List<RecordingSummaryDto> items = recordingQueryRepository.findRecentCompleted(userId)
                .stream()
                .map(this::summary)
                .toList();
        return new RecentAnalysesResponse(items);
    }

    private RecordingQueryRepository.RecordingAnalysisView findOwned(Long userId, Long recordingId) {
        return recordingQueryRepository.findOwnedDetail(userId, recordingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Plan getPlan(Long userId) {
        return userRepository.findById(userId)
                .map(User::getPlan)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private BasicDto basic(RecordingQueryRepository.RecordingAnalysisView row) {
        List<FillerBreakdownDto> breakdown = analysisQueryRepository.findFillerBreakdown(row.getAnalysisId())
                .stream()
                .map(item -> new FillerBreakdownDto(item.getExpression(), item.getCount()))
                .toList();
        return new BasicDto(row.getFillerTotalCount(), breakdown);
    }

    private AnalysisStatusDto analysisStatus(
            RecordingQueryRepository.RecordingAnalysisView row,
            AnalysisStatus status
    ) {
        boolean retryable = status == AnalysisStatus.FAILED && row.getAttemptNo() < 4;
        FailureCode failureCode = row.getFailureCode() == null
                ? null
                : FailureCode.valueOf(row.getFailureCode());
        return new AnalysisStatusDto(
                id(row.getAnalysisId()),
                id(row.getRecordingId()),
                status,
                row.getAttemptNo(),
                row.getAutoRetryCount(),
                failureCode,
                retryable,
                retryable,
                row.getStartedAt(),
                row.getFinishedAt()
        );
    }

    private RecordingSummaryDto summary(RecordingQueryRepository.RecordingAnalysisView row) {
        AnalysisStatus status = statusOf(row);
        return new RecordingSummaryDto(
                id(row.getRecordingId()),
                row.getSubmittedAt(),
                row.getDurationMs(),
                status,
                status == AnalysisStatus.COMPLETED ? row.getFillerTotalCount() : null
        );
    }

    private ProAccessDto proAccess(Plan plan, AnalysisStatus status, Long recordingId) {
        if (plan == Plan.FREE) {
            return new ProAccessDto(true, false, null, "/upgrade", LOCKED_FEATURES);
        }
        boolean available = status == AnalysisStatus.COMPLETED;
        String detailUrl = available ? "/api/v1/recordings/" + recordingId + "/pro-analysis" : null;
        return new ProAccessDto(false, available, detailUrl, null, List.of());
    }

    private AnalysisStatus statusOf(RecordingQueryRepository.RecordingAnalysisView row) {
        return AnalysisStatus.valueOf(row.getStatus());
    }

    private String id(Long value) {
        return String.valueOf(value);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
