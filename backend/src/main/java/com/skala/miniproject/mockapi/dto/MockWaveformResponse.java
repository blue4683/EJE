package com.skala.miniproject.mockapi.dto;

import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.WaveformPoint;

import java.util.List;

/**
 * API 20 응답 data. speech.model 의 record 를 그대로 필드로 쓴다 — 필드명이 JSON 계약과
 * 이미 같아서 별도 변환 DTO를 두지 않는다.
 */
public record MockWaveformResponse(
        int durationMs,
        int speechDurationMs,
        int silenceDurationMs,
        int longSilenceCount,
        List<SpeechInterval> speechIntervals,
        List<WaveformPoint> waveform
) {
}
