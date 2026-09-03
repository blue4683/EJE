package com.skala.miniproject.history.dto;

import java.util.List;

public record RecentAnalysesResponse(List<RecordingSummaryDto> items) {
}
