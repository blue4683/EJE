package com.skala.miniproject.audio;

public interface AudioDecoder {

    /** declaredMimeType 은 업로드 파트의 Content-Type 원문(파라미터 포함 가능)이다. */
    DecodedAudio decode(byte[] audioBytes, String declaredMimeType);
}
