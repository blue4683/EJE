package com.skala.miniproject.pro.dto;

import com.skala.miniproject.domain.analysis.SegmentType;

public record SegmentDto(SegmentType segment, int fillerCount, int habitWordCount) {
}
