package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.Coaching;
import com.skala.miniproject.speech.model.SegmentCount;
import com.skala.miniproject.speech.model.SegmentType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 규칙 12 — 코칭. 지표(추임새 위치·반복·긴 침묵)를 근거로 결정적으로(무작위성 없이) 생성한다.
 * 전체 전사문이나 긴 발화 인용은 저장하지 않는다. 무음 빌지도 유효한 코칭을 만든다.
 */
public final class CoachingComposer {

    private static final Map<SegmentType, String> LOCATION = new EnumMap<>(Map.of(
            SegmentType.INITIAL, "초반",
            SegmentType.MIDDLE, "중반",
            SegmentType.FINAL, "후반"
    ));

    private CoachingComposer() {
    }

    public static Coaching compose(int speechDurationMs, int fillerTotalCount, List<SegmentCount> segments,
                                    int repeatedExpressionCount, int longSilenceCount) {
        if (speechDurationMs == 0) {
            return new Coaching(
                    "발화가 감지되지 않아 무음으로 채점되었습니다. 마이크 상태와 발화 여부를 확인해 주세요.",
                    List.of("조용한 곳에서 마이크에 가까이 대고 다시 녹음해 보세요."),
                    "짧은 문장을 하나 소리 내어 읽으며 마이크 입력이 정상인지 먼저 확인하세요."
            );
        }

        List<String> fillerLocations = segments.stream()
                .filter(s -> s.fillerCount() > 0)
                .map(s -> LOCATION.get(s.segment()))
                .toList();

        StringBuilder summary = new StringBuilder();
        List<String> actionItems = new ArrayList<>();

        if (fillerTotalCount == 0) {
            summary.append("추임새 없이 안정적으로 말했습니다.");
            actionItems.add("지금의 속도와 발음을 유지하며 연습해 보세요.");
        } else if (fillerLocations.size() == 3) {
            summary.append("전반적으로 추임새가 고르게 관측되었습니다.");
            actionItems.add("문장을 시작하기 전에 한 박자 쉬고 말해 보세요.");
        } else {
            summary.append(String.join("과 ", fillerLocations)).append("에 추임새가 관측되었습니다.");
            actionItems.add(fillerLocations.get(0) + " 구간에서 말을 잠시 멈추는 연습을 해 보세요.");
        }

        if (repeatedExpressionCount > 0) {
            summary.append(" 같은 표현을 반복한 부분도 ").append(repeatedExpressionCount).append("회 있었습니다.");
            actionItems.add("반복되는 표현 대신 다음 문장으로 바로 넘어가는 연습을 해 보세요.");
        }
        if (longSilenceCount > 0) {
            summary.append(" 긴 침묵 구간도 ").append(longSilenceCount).append("회 있었습니다.");
            actionItems.add("긴 침묵 대신 짧게 숨을 고르는 정도로 쉬는 연습을 해 보세요.");
        }
        if (actionItems.size() > 5) {
            actionItems = actionItems.subList(0, 5);
        }

        String recommendation = "같은 내용을 다시 녹음하고 추임새·반복 표현의 변화를 확인하세요.";
        return new Coaching(summary.toString(), List.copyOf(actionItems), recommendation);
    }
}
