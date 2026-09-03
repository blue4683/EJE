<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWeeklyReport } from '@/api/stats'
import { seoulToday, seoulWeekStart } from '@/composables/useSeoulDate'
import { formatCount, formatDayLabel, formatPercent } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import WeekPicker from '@/components/stats/WeekPicker.vue'
import LineChart from '@/components/chart/LineChart.vue'

const router = useRouter()
const weekStartDate = ref(seoulWeekStart())
const data = ref(null)
const state = ref('loading')
const error = ref(null)

const chartPoints = computed(() => (data.value?.trend ?? []).map((point) => ({
  date: point.date,
  label: formatDayLabel(point.date),
  value: point.averageFillerCount,
  future: point.date > seoulToday(),
})))

const improvementMessage = computed(() => {
  if (!data.value || data.value.improvementRatePercent != null) return null
  if (data.value.practiceCount === 0) return '이번 주 연습 기록이 없습니다.'
  if (data.value.previousWeekPracticeCount === 0) return '지난주 기록이 없어 비교할 수 없습니다.'
  if (data.value.previousWeekAverageFillerCount === 0) return '지난주 추임새가 0회라 개선율을 계산할 수 없습니다.'
  return '개선율을 계산할 수 없습니다.'
})

async function load(next = weekStartDate.value) {
  weekStartDate.value = next
  state.value = 'loading'
  error.value = null
  try {
    data.value = await fetchWeeklyReport(next)
    state.value = 'ready'
  } catch (caught) {
    error.value = caught
    if (caught.code === 'PRO_REQUIRED') await router.replace({ name: 'upgrade' })
    else state.value = 'error'
  }
}

onMounted(() => load())
</script>

<template>
  <PageHeader title="주간 리포트" description="월요일부터 일요일까지의 연습 흐름입니다." />
  <WeekPicker v-model="weekStartDate" @change="load" />
  <StateBlock :state="state" :message="error?.message" @retry="load()">
    <template v-if="data">
      <div class="report-heading"><div><p>{{ data.weekStartDate }} ~ {{ data.weekEndDate }}</p><h2>이번 주 말하기 습관</h2></div><span v-if="data.isPartial">진행 중인 주</span></div>
      <section class="summary-grid">
        <article><span>완료한 연습</span><strong>{{ data.practiceCount }}건</strong></article>
        <article><span>평균 추임새</span><strong>{{ formatCount(data.averageFillerCount) }}</strong></article>
        <article><span>지난주 대비 개선율</span><strong :class="{ positive: data.improvementRatePercent > 0, negative: data.improvementRatePercent < 0 }">{{ formatPercent(data.improvementRatePercent) }}</strong><small v-if="improvementMessage">{{ improvementMessage }}</small></article>
      </section>
      <section class="chart-card"><h2>월~일 추이</h2><LineChart :points="chartPoints" aria-label="주간 평균 추임새 추이" /><p>미래 날짜와 연습하지 않은 날은 점을 표시하지 않습니다.</p></section>
    </template>
  </StateBlock>
</template>

<style scoped>
.report-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-5); }
.report-heading p, .report-heading h2 { margin: 0; }
.report-heading p { color: var(--color-text-muted); }
.report-heading > span { padding: 5px 9px; color: var(--color-warning); font-size: 0.78rem; font-weight: 900; background: var(--color-warning-weak); border-radius: 999px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); }
.summary-grid article { display: grid; gap: var(--space-2); padding: var(--space-5); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.summary-grid span, .summary-grid small, .chart-card p { color: var(--color-text-muted); }
.summary-grid strong { font-size: 1.5rem; }
.positive { color: var(--color-success); }
.negative { color: var(--color-danger); }
.chart-card { margin-top: var(--space-5); padding: var(--space-6); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.chart-card h2 { margin-top: 0; }
@media (max-width: 680px) { .summary-grid { grid-template-columns: 1fr; } }
</style>
