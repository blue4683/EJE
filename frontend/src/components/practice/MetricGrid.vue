<script setup>
import { computed } from 'vue'
import { formatCount, formatMs, formatNumber } from '@/utils/format'

const props = defineProps({
  metrics: { type: Object, default: null },
  locked: { type: Boolean, default: false },
})

const labels = ['전체 길이', '발화 시간', '침묵 시간', '긴 침묵', '반복 표현', '말하기 속도', '전체 단어']
const items = computed(() => {
  if (props.locked || !props.metrics) return labels.map((label) => [label, null])
  return [
    ['전체 길이', formatMs(props.metrics.durationMs)],
    ['발화 시간', formatMs(props.metrics.speechDurationMs)],
    ['침묵 시간', formatMs(props.metrics.silenceDurationMs)],
    ['긴 침묵', formatCount(props.metrics.longSilenceCount)],
    ['반복 표현', formatCount(props.metrics.repeatedExpressionCount)],
    ['말하기 속도', props.metrics.speechRate?.wordsPerMinute == null
      ? '—'
      : `${formatNumber(props.metrics.speechRate.wordsPerMinute)} WPM`],
    ['전체 단어', formatNumber(props.metrics.speechRate?.totalWordCount)],
  ]
})
</script>

<template>
  <dl class="metrics">
    <div v-for="([label, value]) in items" :key="label" :class="{ 'metric--locked': locked }">
      <dt>{{ label }}</dt>
      <dd>{{ value ?? '—' }}</dd>
      <span v-if="locked" class="metric__lock" aria-label="PRO 전용">🔒</span>
    </div>
  </dl>
</template>

<style scoped>
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: var(--space-3); }
.metrics div { position: relative; padding: var(--space-3); background: var(--color-surface); border-radius: var(--radius-1); }
.metrics dt { color: var(--color-text-muted); font-size: 0.875rem; }
.metrics dd { margin: var(--space-1) 0 0; font-weight: 700; }
.metric--locked dt, .metric--locked dd { filter: blur(4px); opacity: .45; user-select: none; }
.metric__lock { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); }
</style>
