<script setup>
import { computed, reactive, watch } from 'vue'
import { daysBetween, seoulToday } from '@/composables/useSeoulDate'

const props = defineProps({ period: { type: String, default: 'WEEK' } })
const emit = defineEmits(['search'])
const today = seoulToday()
const form = reactive({ period: props.period, startDate: '', endDate: '' })

watch(() => props.period, (period) => { form.period = period })

const validation = computed(() => {
  if (form.period !== 'CUSTOM') return null
  if (!form.startDate || !form.endDate) return '시작일과 종료일을 모두 선택해 주세요.'
  if (form.startDate > form.endDate) return '시작일은 종료일보다 늦을 수 없습니다.'
  if (form.endDate > today) return '오늘 이후 날짜는 선택할 수 없습니다.'
  if (daysBetween(form.startDate, form.endDate) > 366) return '조회 기간은 최대 366일입니다.'
  return null
})

function search() {
  if (validation.value) return
  emit('search', form.period === 'CUSTOM'
    ? { period: 'CUSTOM', startDate: form.startDate, endDate: form.endDate }
    : { period: form.period })
}
</script>

<template>
  <form class="picker" @submit.prevent="search">
    <div class="picker__periods" role="group" aria-label="조회 기간">
      <button v-for="option in [{ value: 'WEEK', label: '최근 7일' }, { value: 'MONTH', label: '최근 30일' }, { value: 'CUSTOM', label: '직접 선택' }]" :key="option.value" type="button" :class="{ active: form.period === option.value }" @click="form.period = option.value">
        {{ option.label }}
      </button>
    </div>
    <div v-if="form.period === 'CUSTOM'" class="picker__dates">
      <label><span>시작일</span><input v-model="form.startDate" type="date" :max="today" /></label>
      <span aria-hidden="true">–</span>
      <label><span>종료일</span><input v-model="form.endDate" type="date" :max="today" /></label>
    </div>
    <p v-if="validation" class="picker__error" role="alert">{{ validation }}</p>
    <button type="submit" class="picker__submit" :disabled="Boolean(validation)">조회</button>
  </form>
</template>

<style scoped>
.picker { display: flex; flex-wrap: wrap; align-items: end; gap: var(--space-4); margin-bottom: var(--space-6); padding: var(--space-4); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.picker__periods { display: flex; gap: var(--space-1); padding: 3px; background: var(--color-surface-soft); border-radius: var(--radius-1); }
.picker__periods button { padding: 8px 11px; color: var(--color-text-muted); background: transparent; border: 0; border-radius: 6px; }
.picker__periods button.active { color: var(--color-primary-strong); font-weight: 800; background: var(--color-surface); box-shadow: 0 1px 5px rgba(28, 42, 72, 0.12); }
.picker__dates { display: flex; align-items: end; gap: var(--space-2); }
.picker__dates label { display: grid; gap: var(--space-1); color: var(--color-text-muted); font-size: 0.78rem; }
.picker__dates input { padding: 7px 9px; color: var(--color-text); background: white; border: 1px solid var(--color-border); border-radius: var(--radius-1); }
.picker__error { flex-basis: 100%; margin: 0; color: var(--color-danger); }
.picker__submit { margin-left: auto; padding: 9px 14px; color: white; font-weight: 800; background: var(--color-primary); border: 0; border-radius: var(--radius-1); }
@media (max-width: 640px) { .picker, .picker__dates { align-items: stretch; flex-direction: column; } .picker__dates > span { display: none; } .picker__submit { width: 100%; margin: 0; } }
</style>
