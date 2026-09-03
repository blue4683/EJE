<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: null },
  lowerIsBetter: { type: Boolean, default: true },
  neutral: { type: Boolean, default: false },
  unit: { type: String, default: '' },
})

const tone = computed(() => {
  if (props.neutral || props.value == null || props.value === 0) return 'flat'
  const improved = props.lowerIsBetter ? props.value < 0 : props.value > 0
  return improved ? 'good' : 'bad'
})
</script>

<template>
  <span :class="['delta', `delta--${tone}`]">
    {{ value == null ? '—' : `${value > 0 ? '+' : ''}${value}${unit}` }}
  </span>
</template>

<style scoped>
.delta { display: inline-flex; padding: 4px 8px; color: var(--color-text-muted); font-weight: 900; background: var(--color-surface-soft); border-radius: 999px; }
.delta--good { color: var(--color-success); background: var(--color-success-weak); }
.delta--bad { color: var(--color-danger); background: var(--color-danger-weak); }
</style>
