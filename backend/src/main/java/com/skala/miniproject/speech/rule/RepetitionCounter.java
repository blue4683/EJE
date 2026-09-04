package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.TimedToken;

import java.util.List;

/**
 * 규칙 7 — 반복 표현.
 * 유효 토큰의 원래 순서에서 인접한 동일 비추임새 토큰 쌍을 센다. 중간에 추임새가 끼면
 * 연속성이 끊긴다 — 그 자리에서 이어지던 동일 토큰 판정을 리셋한다(단순히 건너뛰지 않는다).
 */
public final class RepetitionCounter {

    private static final int MAX_GAP_MS = 1000;

    private RepetitionCounter() {
    }

    public static int count(List<TimedToken> validTokens) {
        int repeats = 0;
        TimedToken previousInRun = null;

        for (TimedToken token : validTokens) {
            if (FillerCounter.isFiller(token)) {
                previousInRun = null; // 추임새는 연속성을 끊는다
                continue;
            }
            if (previousInRun != null
                    && previousInRun.text().equals(token.text())
                    && Math.max(0, token.startMs() - previousInRun.endMs()) <= MAX_GAP_MS) {
                repeats++;
            }
            previousInRun = token;
        }
        return repeats;
    }
}
