package com.skala.miniproject.pro.dto;

import com.skala.miniproject.domain.analysis.WaveType;

public record WaveformPointDto(int timeMs, double amplitude, WaveType type) {
}
