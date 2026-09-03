package com.skala.miniproject.speech.rule;

public record SilenceMetrics(int speechDurationMs, int silenceDurationMs, int longSilenceCount) {}
