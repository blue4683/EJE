package com.skala.miniproject.speech.rule;

import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.TimedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenNormalizerTest {

    @Test
    void NFKC_정규화와_양끝_문장부호_제거() {
        List<TimedToken> raw = List.of(
                new TimedToken("\"hello!\"", 0, 100),   // 양끝 문장부호 제거
                new TimedToken("Ａ", 200, 300)       // 전각 A → NFKC 로 반각 A
        );

        List<TimedToken> result = TokenNormalizer.normalize(raw, 1000, List.of(), true);

        assertEquals("hello", result.get(0).text());
        assertEquals("A", result.get(1).text());
    }

    @Test
    void 발화구간과_49퍼센트_겹치는_토큰은_버린다() {
        List<TimedToken> raw = List.of(new TimedToken("가나다", 0, 100));
        List<SpeechInterval> speech = List.of(new SpeechInterval(0, 49)); // 49% 겹침

        List<TimedToken> result = TokenNormalizer.normalize(raw, 1000, speech, false);

        assertTrue(result.isEmpty());
    }
}
