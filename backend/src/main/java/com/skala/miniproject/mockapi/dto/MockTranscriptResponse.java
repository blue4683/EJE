package com.skala.miniproject.mockapi.dto;

import com.skala.miniproject.speech.model.Basic;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;

import java.util.List;

/**
 * API 21 응답 data. WPM·침묵·파형·코칭은 발화 구간이 없어 계산할 수 없으므로 포함하지 않는다.
 */
public record MockTranscriptResponse(
        Basic basic,
        int totalWordCount,
        int repeatedExpressionCount,
        List<FillerEvent> fillerTimeline,
        List<SegmentCount> segmentAnalysis
) {
}
