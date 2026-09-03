package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.SpeechAnalysisException;
import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.TimedToken;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * 규칙 4 — STT 시각 토큰 정규화.
 * NFKC 정규화 → 양끝 문장부호 제거 → 빈 토큰 제거 → 범위 검사 → 발화 구간과 50% 이상 겹침 검사.
 * skipOverlapCheck=true 는 발화 구간이 없는 MOCK_002 전용 예외 분기다 (마지막 검사를 건너뛴다).
 */
public final class TokenNormalizer {

    private TokenNormalizer() {
    }

    public static List<TimedToken> normalize(List<TimedToken> raw, int durationMs,
                                              List<SpeechInterval> normalizedSpeechIntervals,
                                              boolean skipOverlapCheck) {
        List<TimedToken> result = new ArrayList<>();
        int lastStartMs = Integer.MIN_VALUE;

        for (TimedToken token : raw) {
            if (token.startMs() < lastStartMs) {
                throw new SpeechAnalysisException("토큰 startMs 가 입력 순서에서 비감소가 아닙니다: " + token);
            }
            lastStartMs = token.startMs();

            String text = normalizeText(token.text());
            if (text.isEmpty()) {
                continue;
            }
            if (!(token.startMs() >= 0 && token.startMs() < token.endMs() && token.endMs() <= durationMs)) {
                continue;
            }
            if (!skipOverlapCheck && overlapRatio(token.startMs(), token.endMs(), normalizedSpeechIntervals) < 0.5) {
                continue;
            }
            result.add(new TimedToken(text, token.startMs(), token.endMs()));
        }
        return List.copyOf(result);
    }

    private static String normalizeText(String raw) {
        String nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        int start = 0;
        int end = nfkc.length();
        while (start < end) {
            int cp = nfkc.codePointAt(start);
            if (!isPunctuation(cp)) {
                break;
            }
            start += Character.charCount(cp);
        }
        while (end > start) {
            int cp = nfkc.codePointBefore(end);
            if (!isPunctuation(cp)) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return nfkc.substring(start, end);
    }

    private static boolean isPunctuation(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION;
    }

    private static double overlapRatio(int startMs, int endMs, List<SpeechInterval> speechIntervals) {
        int tokenLen = endMs - startMs;
        if (tokenLen <= 0) {
            return 0;
        }
        int overlap = 0;
        for (SpeechInterval iv : speechIntervals) {
            int os = Math.max(startMs, iv.startMs());
            int oe = Math.min(endMs, iv.endMs());
            if (os < oe) {
                overlap += oe - os;
            }
        }
        return (double) overlap / tokenLen;
    }
}
