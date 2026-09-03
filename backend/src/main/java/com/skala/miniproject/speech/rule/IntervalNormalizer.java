package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 규칙 2 — 발화 구간 정규화.
 * 순서를 바꾸면 결과가 달라진다: 범위 클리핑 → 빈 구간 제거 → 정렬 → 겹침 병합
 * → 100ms 미만 제거 → 200ms 이하 간격 병합.
 */
public final class IntervalNormalizer {

    private static final int MIN_LENGTH_MS = 100;
    private static final int MERGE_GAP_MS = 200;

    private IntervalNormalizer() {
    }

    public static List<SpeechInterval> normalize(List<SpeechInterval> raw, int durationMs) {
        List<SpeechInterval> clipped = new ArrayList<>();
        for (SpeechInterval iv : raw) {
            int start = Math.max(0, iv.startMs());
            int end = Math.min(durationMs, iv.endMs());
            if (start < end) {
                clipped.add(new SpeechInterval(start, end));
            }
        }
        clipped.sort(Comparator.comparingInt(SpeechInterval::startMs));

        List<SpeechInterval> merged = mergeOverlapping(clipped);
        List<SpeechInterval> longEnough = removeShort(merged);
        return mergeNearby(longEnough);
    }

    private static List<SpeechInterval> mergeOverlapping(List<SpeechInterval> sorted) {
        List<SpeechInterval> merged = new ArrayList<>();
        for (SpeechInterval iv : sorted) {
            if (!merged.isEmpty() && iv.startMs() <= merged.getLast().endMs()) {
                SpeechInterval last = merged.removeLast();
                merged.add(new SpeechInterval(last.startMs(), Math.max(last.endMs(), iv.endMs())));
            } else {
                merged.add(iv);
            }
        }
        return merged;
    }

    private static List<SpeechInterval> removeShort(List<SpeechInterval> intervals) {
        List<SpeechInterval> result = new ArrayList<>();
        for (SpeechInterval iv : intervals) {
            if (iv.endMs() - iv.startMs() >= MIN_LENGTH_MS) {
                result.add(iv);
            }
        }
        return result;
    }

    private static List<SpeechInterval> mergeNearby(List<SpeechInterval> intervals) {
        List<SpeechInterval> merged = new ArrayList<>();
        for (SpeechInterval iv : intervals) {
            if (!merged.isEmpty() && iv.startMs() - merged.getLast().endMs() <= MERGE_GAP_MS) {
                SpeechInterval last = merged.removeLast();
                merged.add(new SpeechInterval(last.startMs(), iv.endMs()));
            } else {
                merged.add(iv);
            }
        }
        return List.copyOf(merged);
    }
}
