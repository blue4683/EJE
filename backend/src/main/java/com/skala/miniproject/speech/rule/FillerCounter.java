package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.FillerBreakdown;
import com.skala.miniproject.speech.model.FillerEvent;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SegmentType;
import com.skala.miniproject.speech.model.TimedToken;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 규칙 5(추임새) · 8(습관어) · 9(구간 분류)를 담당한다. */
public final class FillerCounter {

    private static final Set<String> FILLERS = Set.of("음", "어");
    private static final Set<String> HABIT_WORDS = Set.of("그러니까", "약간", "사실");

    private FillerCounter() {
    }

    public static boolean isFiller(TimedToken token) {
        return FILLERS.contains(token.text());
    }

    public static boolean isHabitWord(TimedToken token) {
        return HABIT_WORDS.contains(token.text());
    }

    /** count 내림차순, 동률이면 음 → 어 순 (API명세서.md 규칙 5). count>0 인 항목만 반환한다. */
    public static List<FillerBreakdown> breakdown(List<TimedToken> validTokens) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TimedToken token : validTokens) {
            if (isFiller(token)) {
                counts.merge(token.text(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new FillerBreakdown(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(FillerBreakdown::count).reversed()
                        .thenComparingInt(fb -> fb.expression().equals("음") ? 0 : 1))
                .toList();
    }

    public static int totalCount(List<FillerBreakdown> breakdown) {
        return breakdown.stream().mapToInt(FillerBreakdown::count).sum();
    }

    /** eventIndex 는 시간순(=유효 토큰 순서) 0부터 연속 부여한다. */
    public static List<FillerEvent> timeline(List<TimedToken> validTokens) {
        List<FillerEvent> events = new ArrayList<>();
        int index = 0;
        for (TimedToken token : validTokens) {
            if (isFiller(token)) {
                events.add(new FillerEvent(index++, token.startMs(), token.text()));
            }
        }
        return List.copyOf(events);
    }

    /** 3개 구간(INITIAL/MIDDLE/FINAL)을 횟수가 0이어도 항상 반환한다. */
    public static List<SegmentCount> segments(List<TimedToken> validTokens, int durationMs) {
        Map<SegmentType, int[]> tally = new EnumMap<>(SegmentType.class);
        for (SegmentType type : SegmentType.values()) {
            tally.put(type, new int[2]); // [0]=fillerCount, [1]=habitWordCount
        }

        for (TimedToken token : validTokens) {
            if (isFiller(token)) {
                tally.get(classify(token.startMs(), durationMs))[0]++;
            } else if (isHabitWord(token)) {
                tally.get(classify(token.startMs(), durationMs))[1]++;
            }
        }

        List<SegmentCount> result = new ArrayList<>(3);
        for (SegmentType type : List.of(SegmentType.INITIAL, SegmentType.MIDDLE, SegmentType.FINAL)) {
            int[] counts = tally.get(type);
            result.add(new SegmentCount(type, counts[0], counts[1]));
        }
        return List.copyOf(result);
    }

    private static SegmentType classify(int timeMs, int durationMs) {
        int index = (int) Math.floor(3.0 * timeMs / durationMs);
        index = Math.max(0, Math.min(2, index));
        return switch (index) {
            case 0 -> SegmentType.INITIAL;
            case 1 -> SegmentType.MIDDLE;
            default -> SegmentType.FINAL;
        };
    }
}
