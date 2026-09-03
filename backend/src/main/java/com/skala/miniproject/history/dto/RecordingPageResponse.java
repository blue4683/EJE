package com.skala.miniproject.history.dto;

import java.util.List;

public record RecordingPageResponse(
        List<RecordingSummaryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
