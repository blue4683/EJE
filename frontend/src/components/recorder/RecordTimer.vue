<script setup>
import { computed } from 'vue'
import { MAX_DURATION_MS } from '@/constants/audio'

const props = defineProps({
  elapsedMs: { type: Number, default: 0 },
})

const formatTime = (milliseconds) => {
  const totalSeconds = Math.floor(milliseconds / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

const current = computed(() => formatTime(props.elapsedMs))
const limit = formatTime(MAX_DURATION_MS)
const isNearLimit = computed(() => props.elapsedMs >= MAX_DURATION_MS - 10000)
</script>

<template>
  <p class="timer" :class="{ 'timer--warning': isNearLimit }" aria-live="off">
    <span class="timer__current">{{ current }}</span>
    <span aria-hidden="true"> / </span>
    <span>{{ limit }}</span>
  </p>
</template>

<style scoped>
.timer { margin: 0; color: var(--color-text-muted, var(--text)); font-variant-numeric: tabular-nums; }
.timer__current { color: var(--color-text, var(--text-h)); font-size: 1.5rem; font-weight: 700; }
.timer--warning,
.timer--warning .timer__current { color: var(--color-danger, #d64545); }
</style>
