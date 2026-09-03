<script setup>
import { computed } from 'vue'
import { formatCount, formatMs, formatNumber } from '@/utils/format'

const props = defineProps({
  metrics: { type: Object, required: true },
})

const items = computed(() => [
  ['전체 길이', formatMs(props.metrics.durationMs)],
  ['발화 시간', formatMs(props.metrics.speechDurationMs)],
  ['침묵 시간', formatMs(props.metrics.silenceDurationMs)],
  ['긴 침묵', formatCount(props.metrics.longSilenceCount)],
  ['반복 표현', formatCount(props.metrics.repeatedExpressionCount)],
  ['말하기 속도', props.metrics.speechRate?.wordsPerMinute == null
    ? '—'
    : `${formatNumber(props.metrics.speechRate.wordsPerMinute)} WPM`],
  ['전체 단어', formatNumber(props.metrics.speechRate?.totalWordCount)],
])
</script>

<template>
  <dl class="metrics">
    <div v-for="([label, value]) in items" :key="label">
      <dt>{{ label }}</dt>
      <dd>{{ value }}</dd>
    </div>
  </dl>
</template>

<style scoped>
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: var(--space-3); }
.metrics div { padding: var(--space-3); background: var(--color-surface); border-radius: var(--radius-1); }
.metrics dt { color: var(--color-text-muted); font-size: 0.875rem; }
.metrics dd { margin: var(--space-1) 0 0; font-weight: 700; }
</style>
