<script setup>
import { computed } from 'vue'
import FillerBreakdownList from './FillerBreakdownList.vue'
import { formatCount } from '@/utils/format'

const props = defineProps({ basic: { type: Object, required: true } })
const freeBreakdown = computed(() => (
  props.basic.fillerBreakdown?.filter((item) => ['어', '음'].includes(item.expression)) ?? []
))
</script>

<template>
  <section class="result-card">
    <div class="result-card__summary">
      <span>이번 연습에서 찾은 추임새</span>
      <strong>{{ formatCount(basic.fillerTotalCount) }}</strong>
    </div>
    <div>
      <h2>어·음 빈도</h2>
      <FillerBreakdownList :items="freeBreakdown" />
    </div>
  </section>
</template>

<style scoped>
.result-card { display: grid; grid-template-columns: minmax(220px, 0.7fr) 1.3fr; gap: var(--space-6); padding: var(--space-6); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.result-card__summary { display: flex; justify-content: center; flex-direction: column; padding: var(--space-5); color: var(--color-text); background: #f5f5f5; border-radius: 7px; }
.result-card__summary span { font-size: 0.9rem; font-weight: 700; }
.result-card__summary strong { font-size: 2.5rem; }
.result-card h2 { margin-top: 0; font-size: 1.05rem; }
@media (max-width: 650px) { .result-card { grid-template-columns: 1fr; } }
</style>
