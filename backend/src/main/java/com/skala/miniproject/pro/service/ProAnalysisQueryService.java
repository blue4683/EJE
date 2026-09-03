package com.skala.miniproject.pro.service;

import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.EngineType;
import com.skala.miniproject.domain.analysis.SegmentType;
import com.skala.miniproject.domain.user.Plan;
import com.skala.miniproject.history.dto.BasicDto;
import com.skala.miniproject.history.dto.FillerBreakdownDto;
import com.skala.miniproject.pro.dto.CoachingDto;
import com.skala.miniproject.pro.dto.FillerEventDto;
import com.skala.miniproject.pro.dto.MetricsDto;
import com.skala.miniproject.pro.dto.ProResultResponse;
import com.skala.miniproject.pro.dto.SegmentDto;
import com.skala.miniproject.pro.dto.SpeechIntervalDto;
import com.skala.miniproject.pro.dto.SpeechRateDto;
import com.skala.miniproject.pro.dto.WaveformPointDto;
import com.skala.miniproject.pro.repository.ProResultQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ProAnalysisQueryService {

    private final ProResultQueryRepository proResultQueryRepository;
    private final JsonMapper jsonMapper;

    public ProResultResponse getProAnalysis(Long userId, Long recordingId) {
        var row = proResultQueryRepository.findOwnedResult(userId, recordingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (Plan.valueOf(row.getPlan()) != Plan.PRO) {
            throw new BusinessException(ErrorCode.PRO_REQUIRED);
        }

        AnalysisStatus status = AnalysisStatus.valueOf(row.getStatus());
        if (status != AnalysisStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        if (row.getProResultId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        BasicDto basic = new BasicDto(
                row.getFillerTotalCount(),
                proResultQueryRepository.findFillerBreakdown(row.getAnalysisId()).stream()
                        .map(item -> new FillerBreakdownDto(item.getExpression(), item.getCount()))
                        .toList()
        );
        List<FillerEventDto> fillerTimeline = proResultQueryRepository.findFillerTimeline(row.getAnalysisId()).stream()
                .map(item -> new FillerEventDto(item.getEventIndex(), item.getTimeMs(), item.getExpression()))
                .toList();
        List<SegmentDto> segments = proResultQueryRepository.findSegments(row.getAnalysisId()).stream()
                .map(item -> new SegmentDto(
                        SegmentType.valueOf(item.getSegment()),
                        item.getFillerCount(),
                        item.getHabitWordCount()))
                .toList();
        List<String> actionItems = proResultQueryRepository.findCoachingActionItems(row.getProResultId());

        MetricsDto metrics = new MetricsDto(
                row.getDurationMs(),
                row.getSpeechDurationMs(),
                row.getSilenceDurationMs(),
                row.getLongSilenceCount(),
                row.getRepeatedExpressionCount(),
                basic,
                parseSpeechIntervals(row.getSpeechIntervals()),
                parseWaveform(row.getWaveform()),
                fillerTimeline,
                new SpeechRateDto(row.getWordsPerMinute(), row.getTotalWordCount()),
                segments,
                new CoachingDto(
                        row.getCoachingSummary(),
                        actionItems,
                        row.getCoachingPracticeRecommendation())
        );

        return new ProResultResponse(
                String.valueOf(row.getRecordingId()),
                String.valueOf(row.getAnalysisId()),
                status,
                row.getAlgorithmVersion(),
                EngineType.valueOf(row.getEngineType()),
                row.getEngineVersion(),
                metrics
        );
    }

    private List<SpeechIntervalDto> parseSpeechIntervals(String json) {
        return List.copyOf(Arrays.asList(jsonMapper.readValue(json, SpeechIntervalDto[].class)));
    }

    private List<WaveformPointDto> parseWaveform(String json) {
        return List.copyOf(Arrays.asList(jsonMapper.readValue(json, WaveformPointDto[].class)));
    }
}
