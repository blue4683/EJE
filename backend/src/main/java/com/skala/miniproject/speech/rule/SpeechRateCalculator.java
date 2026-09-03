package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechRate;
import com.skala.miniproject.speech.model.TimedToken;

import java.util.List;

/** 규칙 6 — 말하기 속도. words 는 한국어 어절(추임새 제외 유효 토큰)이다. */
public final class SpeechRateCalculator {

    private SpeechRateCalculator() {
    }

    public static SpeechRate compute(List<TimedToken> validTokens, int speechDurationMs) {
        int totalWordCount = (int) validTokens.stream().filter(t -> !FillerCounter.isFiller(t)).count();
        if (speechDurationMs == 0) {
            return new SpeechRate(null, totalWordCount);
        }
        int wordsPerMinute = (int) Math.round((double) totalWordCount * 60000 / speechDurationMs);
        return new SpeechRate(wordsPerMinute, totalWordCount);
    }
}
