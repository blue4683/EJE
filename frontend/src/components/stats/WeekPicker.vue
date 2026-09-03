<script setup>
import { computed } from 'vue'
import { isMonday, moveYmd, seoulToday, seoulWeekStart } from '@/composables/useSeoulDate'

const props = defineProps({ modelValue: { type: String, required: true } })
const emit = defineEmits(['update:modelValue', 'change'])
const thisWeek = seoulWeekStart()
const canNext = computed(() => moveYmd(props.modelValue, 7) <= thisWeek)

function move(days) {
  const next = moveYmd(props.modelValue, days)
  if (next > thisWeek) return
  emit('update:modelValue', next)
  emit('change', next)
}

function select(event) {
  const value = event.target.value
  if (!isMonday(value) || value > seoulToday()) {
    event.target.value = props.modelValue
    return
  }
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<template>
  <div class="week-picker">
    <button type="button" @click="move(-7)">이전 주</button>
    <label><span>주 시작일(월요일)</span><input type="date" min="2000-01-03" step="7" :value="modelValue" :max="thisWeek" @change="select" /></label>
    <button type="button" :disabled="!canNext" @click="move(7)">다음 주</button>
  </div>
</template>

<style scoped>
.week-picker { display: flex; align-items: end; gap: var(--space-3); margin-bottom: var(--space-6); }
.week-picker label { display: grid; gap: var(--space-1); color: var(--color-text-muted); font-size: 0.78rem; }
.week-picker input, .week-picker button { padding: 9px 12px; color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-1); }
@media (max-width: 500px) { .week-picker { align-items: stretch; flex-direction: column; } }
</style>
