package com.skala.miniproject.audio;

/** amplitudes 는 100ms 구간별 RMS(0~1), 최대 600개. type(SPEECH/SILENCE) 판정은 speech 패키지의 몫이다. */
public record DecodedAudio(int durationMs, double[] amplitudes, long sampleCount) {}
