package com.skala.miniproject.audio;

import java.util.Locale;
import java.util.Set;

/** ERD ck_recordings_mime 과 정확히 일치하는 허용 목록. MIME 파라미터(;codecs=opus 등)는 분리한다. */
public final class MimeTypeNormalizer {

    private static final Set<String> ALLOWED = Set.of(
            "audio/webm", "audio/mp4", "audio/ogg", "audio/wav", "audio/mpeg"
    );

    private MimeTypeNormalizer() {
    }

    public static String normalize(String rawContentType) {
        if (rawContentType == null) {
            return "";
        }
        return rawContentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowed(String normalizedMimeType) {
        return ALLOWED.contains(normalizedMimeType);
    }
}
