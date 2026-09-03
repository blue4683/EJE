<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchTrends } from '@/api/stats'
import { formatCount, formatDayLabel, formatNumber } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import PeriodPicker from '@/components/stats/PeriodPicker.vue'
import LineChart from '@/components/chart/LineChart.vue'

const data = ref(null)
const state = ref('loading')
const error = ref(null)
const params = ref({ period: 'WEEK' })

const chartPoints = computed(() => (data.value?.dailyPoints ?? []).map((point) => ({
  date: point.date,
  label: formatDayLabel(point.date),
  value: point.averageFillerCount,
})))

const practiceTotal = computed(() =>
  (data.value?.dailyPoints ?? []).reduce((sum, point) => sum + point.practiceCount, 0),
)

async function load(next = params.value) {
  params.value = next
  state.value = 'loading'
  error.value = null
  try {
    data.value = await fetchTrends(next)
    state.value = data.value.dailyPoints.length ? 'ready' : 'empty'
  } catch (caught) {
    error.value = caught
    state.value = 'error'
  }
}

onMounted(() => load())
</script>

<template>
  <PageHeader title="추임새 추이" description="연습하지 않은 날은 0회가 아니라 빈 구간으로 표시합니다." />
  <PeriodPicker :period="params.period" @search="load" />
  <StateBlock :state="state" :message="error?.message" @retry="load()">
    <template v-if="data">
      <section class="summary-grid">
        <div><span>기간</span><strong>{{ data.startDate }} ~ {{ data.endDate }}</strong></div>
        <div><span>완료한 연습</span><strong>{{ formatNumber(practiceTotal) }}건</strong></div>
        <div><span>시간대</span><strong>한국 시간 기준</strong></div>
      </section>
      <section class="chart-card">
        <div class="chart-card__heading"><div><p>일별 평균 추임새</p><h2>{{ params.period === 'WEEK' ? '최근 7일' : params.period === 'MONTH' ? '최근 30일' : '선택한 기간' }}</h2></div><span>{{ data.algorithmVersion }}</span></div>
        <LineChart :points="chartPoints" aria-label="일별 평균 추임새 추이" />
        <p class="chart-card__note">점이 없는 날은 완료된 연습이 없는 날입니다.</p>
      </section>
      <section class="daily-list">
        <div v-for="point in data.dailyPoints" :key="point.date"><span>{{ point.date }}</span><span>{{ point.practiceCount }}건</span><strong>{{ formatCount(point.averageFillerCount) }}</strong></div>
      </section>
      <p class="version-note">분석 기준 {{ data.algorithmVersion }} · 이전 버전 기록은 집계에서 제외됩니다.</p>
    </template>
  </StateBlock>
</template>

<style scoped>
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-3); margin-bottom: var(--space-5); }
.summary-grid div { display: grid; gap: var(--space-1); padding: var(--space-4); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.summary-grid span { color: var(--color-text-muted); font-size: 0.8rem; }
.chart-card { padding: var(--space-6); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.chart-card__heading { display: flex; align-items: start; justify-content: space-between; gap: var(--space-4); }
.chart-card__heading p, .chart-card__heading h2 { margin: 0; }
.chart-card__heading p, .chart-card__heading > span, .chart-card__note, .version-note { color: var(--color-text-muted); font-size: 0.82rem; }
.daily-list { display: grid; max-height: 300px; overflow: auto; margin-top: var(--space-5); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.daily-list div { display: grid; grid-template-columns: 1fr 80px 100px; gap: var(--space-3); padding: var(--space-3) var(--space-4); border-bottom: 1px solid var(--color-border); }
.daily-list strong { text-align: right; }
@media (max-width: 650px) { .summary-grid { grid-template-columns: 1fr; } }
</style>
