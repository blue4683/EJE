<script setup>
import CoachingCard from '@/components/practice/CoachingCard.vue'
import MetricGrid from '@/components/practice/MetricGrid.vue'
import SegmentBars from '@/components/practice/SegmentBars.vue'
import FillerTimeline from '@/components/waveform/FillerTimeline.vue'
import SpeechIntervalBar from '@/components/waveform/SpeechIntervalBar.vue'

defineProps({
  metrics: { type: Object, default: null },
  locked: { type: Boolean, default: false },
})
</script>

<template>
  <div class="pro-summary">
    <MetricGrid :metrics="metrics" :locked="locked" />

    <section class="panel" :class="{ 'panel--locked': locked }">
      <h3>발화와 침묵</h3>
      <SpeechIntervalBar v-if="!locked" :intervals="metrics.speechIntervals" :duration-ms="metrics.durationMs" />
      <div v-else class="locked-placeholder" aria-label="PRO 전용 발화와 침묵 분석">🔒</div>
    </section>

    <section class="panel" :class="{ 'panel--locked': locked }">
      <h3>추임새 타임라인</h3>
      <FillerTimeline v-if="!locked" :events="metrics.fillerTimeline" :duration-ms="metrics.durationMs" />
      <div v-else class="locked-placeholder" aria-label="PRO 전용 추임새 타임라인">🔒</div>
    </section>

    <section class="panel" :class="{ 'panel--locked': locked }">
      <h3>구간별 추임새</h3>
      <SegmentBars v-if="!locked" :segments="metrics.segmentAnalysis" />
      <div v-else class="locked-placeholder" aria-label="PRO 전용 구간별 분석">🔒</div>
    </section>

    <section class="panel" :class="{ 'panel--locked': locked }">
      <h3>추임새 분해</h3>
      <ul v-if="!locked" class="breakdown">
        <li v-for="item in metrics.basic.fillerBreakdown" :key="item.expression">
          <span>{{ item.expression }}</span>
          <strong>{{ item.count }}회</strong>
        </li>
      </ul>
      <div v-else class="locked-placeholder" aria-label="PRO 전용 추임새 분해">🔒</div>
    </section>

    <CoachingCard v-if="!locked" :coaching="metrics.coaching" />
    <section v-else class="panel panel--locked">
      <h3>맞춤 코칭</h3>
      <div class="locked-placeholder" aria-label="PRO 전용 맞춤 코칭">🔒</div>
    </section>
  </div>
</template>

<style scoped>
.pro-summary { display: grid; gap: var(--space-4); }
.panel { display: grid; gap: var(--space-3); padding: var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.panel h3 { margin: 0; font-size: 1.05rem; }
.panel--locked { background: var(--color-surface); }
.locked-placeholder { display: grid; min-height: 72px; place-items: center; color: var(--color-text-muted); border: 1px dashed var(--color-border); border-radius: var(--radius-1); background: repeating-linear-gradient(135deg, #fff, #fff 10px, #fafafa 10px, #fafafa 20px); }
.breakdown { display: grid; gap: var(--space-2); margin: 0; padding: 0; list-style: none; }
.breakdown li { display: flex; justify-content: space-between; padding: var(--space-2); background: var(--color-surface); border-radius: var(--radius-1); }
</style>
