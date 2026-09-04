package com.skala.miniproject.speech.model;

/** 입력 STT 시각 토큰. text는 원문(정규화 전)일 수도, 이미 정규화된 값일 수도 있다 — 정규화는 TokenNormalizer 의 몫이다. */
public record TimedToken(String text, int startMs, int endMs) {}
