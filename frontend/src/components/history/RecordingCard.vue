<script setup>
import StatusChip from './StatusChip.vue'
import { formatCount, formatDateTime, formatMs } from '@/utils/format'

defineProps({ recording: { type: Object, required: true } })
</script>

<template>
  <router-link
    :to="{ name: 'recordingDetail', params: { recordingId: recording.recordingId } }"
    class="card"
  >
    <div class="card__top">
      <span class="card__date">{{ formatDateTime(recording.submittedAt) }}</span>
      <StatusChip :status="recording.status" />
    </div>
    <div class="card__metrics">
      <span><small>연습 길이</small><strong>{{ formatMs(recording.durationMs) }}</strong></span>
      <span><small>추임새</small><strong>{{ formatCount(recording.fillerTotalCount) }}</strong></span>
    </div>
    <span class="card__link">기록 보기 <span aria-hidden="true">→</span></span>
  </router-link>
</template>

<style scoped>
.card { display: grid; gap: var(--space-4); padding: var(--space-5); color: var(--color-text); text-decoration: none; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease; }
.card:hover { border-color: #b8c8f5; box-shadow: var(--shadow-card); transform: translateY(-2px); }
.card__top { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.card__date { color: var(--color-text-muted); font-size: 0.9rem; }
.card__metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-3); }
.card__metrics span { display: grid; gap: var(--space-1); }
.card__metrics small { color: var(--color-text-muted); }
.card__metrics strong { font-size: 1.15rem; }
.card__link { color: var(--color-primary); font-size: 0.9rem; font-weight: 800; }
</style>
