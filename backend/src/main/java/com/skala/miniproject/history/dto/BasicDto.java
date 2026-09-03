package com.skala.miniproject.history.dto;

import java.util.List;

public record BasicDto(Integer fillerTotalCount, List<FillerBreakdownDto> fillerBreakdown) {
}
