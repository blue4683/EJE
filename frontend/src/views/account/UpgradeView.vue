<script setup>
import PageHeader from '@/components/common/PageHeader.vue'
</script>

<template>
  <PageHeader title="PRO 안내" description="더 깊은 말하기 분석으로 연습의 변화를 확인하세요." />
  <section class="hero">
    <p class="hero__eyebrow">SPEECH HABITS PRO</p>
    <h2>숫자를 넘어, 말하는 흐름까지</h2>
    <p>상세 파형, 침묵 구간, 말하기 속도, 구간별 추임새와 주간 리포트를 제공합니다.</p>
  </section>
  <section class="features" aria-label="PRO 제공 기능">
    <article><strong>상세 분석</strong><span>파형과 타임라인으로 발화 흐름 확인</span></article>
    <article><strong>기록 비교</strong><span>이전 연습과의 변화량 비교</span></article>
    <article><strong>주간 리포트</strong><span>월요일부터 일요일까지 습관 요약</span></article>
  </section>
  <p class="notice">현재는 PRO 기능 안내만 제공합니다. 결제 기능은 준비 중입니다.</p>
</template>

<style scoped>
.hero { padding: clamp(28px, 6vw, 64px); color: white; background: linear-gradient(135deg, #1f3f9a, #5079ea); border-radius: var(--radius-3); box-shadow: var(--shadow-card); }
.hero__eyebrow { margin: 0; font-size: 0.78rem; font-weight: 900; letter-spacing: 0.12em; opacity: 0.8; }
.hero h2 { margin: var(--space-3) 0; font-size: clamp(2rem, 5vw, 3.5rem); line-height: 1.1; letter-spacing: -0.05em; }
.hero p:last-child { max-width: 620px; margin-bottom: 0; opacity: 0.88; }
.features { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); margin-top: var(--space-6); }
.features article { display: grid; gap: var(--space-2); padding: var(--space-5); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.features span, .notice { color: var(--color-text-muted); }
.notice { text-align: center; }
@media (max-width: 700px) { .features { grid-template-columns: 1fr; } }
</style>
