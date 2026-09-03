<script setup>
defineProps({
  pro: { type: Object, required: true },
  recordingId: { type: String, required: true },
})

const FEATURE_LABEL = {
  waveform: '음성 파형',
  silence: '침묵 구간',
  speed: '말하기 속도',
  timeline: '추임새 타임라인',
  segment: '구간별 분석',
  repetition: '반복 표현',
  comparison: '이전 기록 비교',
  coaching: '맞춤 코칭',
  weeklyReport: '주간 리포트',
}
</script>

<template>
  <section class="pro-card">
    <template v-if="pro.available">
      <div><p class="eyebrow">PRO ANALYSIS</p><h2>말하기 흐름을 더 자세히 확인하세요</h2></div>
      <router-link :to="{ name: 'proAnalysis', params: { recordingId } }">상세 분석 보기</router-link>
    </template>
    <p v-else-if="!pro.locked">분석이 끝나면 PRO 상세 분석을 볼 수 있습니다.</p>
    <template v-else>
      <div><p class="eyebrow">PRO ONLY</p><h2>잠긴 상세 분석</h2></div>
      <ul><li v-for="feature in pro.lockedFeatures" :key="feature">{{ FEATURE_LABEL[feature] ?? feature }}</li></ul>
      <router-link :to="{ name: 'upgrade' }">PRO 기능 알아보기</router-link>
    </template>
  </section>
</template>

<style scoped>
.pro-card { display: flex; align-items: center; justify-content: space-between; gap: var(--space-5); margin-top: var(--space-6); padding: var(--space-6); background: linear-gradient(135deg, #f1f5ff, #fff); border: 1px solid #cbd8ff; border-radius: var(--radius-2); }
.pro-card h2, .pro-card p { margin: 0; }
.eyebrow { color: var(--color-primary); font-size: 0.72rem; font-weight: 900; letter-spacing: 0.12em; }
.pro-card ul { display: flex; flex: 1; flex-wrap: wrap; gap: var(--space-2); margin: 0; padding: 0; list-style: none; }
.pro-card li { padding: 5px 8px; color: var(--color-text-muted); font-size: 0.78rem; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 999px; }
.pro-card a { flex: none; padding: 10px 14px; color: white; font-weight: 800; text-decoration: none; background: var(--color-primary); border-radius: var(--radius-1); }
@media (max-width: 720px) { .pro-card { align-items: stretch; flex-direction: column; } .pro-card a { align-self: start; } }
</style>
