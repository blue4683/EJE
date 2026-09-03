<script setup>
import { onMounted, ref, watch } from 'vue'
import { fetchProAnalysis } from '@/api/proAnalysis'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import CoachingCard from '@/components/practice/CoachingCard.vue'
import MetricGrid from '@/components/practice/MetricGrid.vue'
import SegmentBars from '@/components/practice/SegmentBars.vue'
import FillerTimeline from '@/components/waveform/FillerTimeline.vue'
import SpeechIntervalBar from '@/components/waveform/SpeechIntervalBar.vue'
import WaveformCanvas from '@/components/waveform/WaveformCanvas.vue'

const props = defineProps({
  recordingId: { type: String, required: true },
})

const state = ref('loading')
const result = ref(null)
const error = ref(null)

function verifyInvariants(data) {
  if (!import.meta.env.DEV) return
  const metrics = data.metrics
  const total = metrics.basic.fillerTotalCount
  const breakdownTotal = metrics.basic.fillerBreakdown.reduce((sum, item) => sum + item.count, 0)
  const segmentTotal = metrics.segmentAnalysis.reduce((sum, item) => sum + item.fillerCount, 0)

  console.assert(total === breakdownTotal, '추임새 분해 합계 불변식이 깨졌습니다.', metrics)
  console.assert(total === metrics.fillerTimeline.length, '추임새 타임라인 불변식이 깨졌습니다.', metrics)
  console.assert(total === segmentTotal, '구간별 추임새 합계 불변식이 깨졌습니다.', metrics)
  console.assert(
    metrics.speechDurationMs + metrics.silenceDurationMs === metrics.durationMs,
    '발화·침묵 시간 합계 불변식이 깨졌습니다.',
    metrics,
  )
  console.assert(metrics.segmentAnalysis.length === 3, '구간 분석은 항상 3개여야 합니다.', metrics)
  console.assert(metrics.waveform.length <= 600, '파형은 최대 600점이어야 합니다.', metrics)
  console.assert(
    metrics.coaching.actionItems.length >= 1 && metrics.coaching.actionItems.length <= 5,
    '코칭 실천 항목은 1~5개여야 합니다.',
    metrics,
  )
}

async function load() {
  state.value = 'loading'
  error.value = null
  result.value = null
  try {
    result.value = await fetchProAnalysis(props.recordingId)
    verifyInvariants(result.value)
    state.value = result.value ? 'ready' : 'empty'
  } catch (caught) {
    error.value = caught
    state.value = 'error'
  }
}

onMounted(load)
watch(() => props.recordingId, load)
</script>

<template>
  <section class="pro-page">
    <PageHeader title="PRO 상세 분석" description="말하기 습관을 시간축과 구간별로 확인합니다." />

    <StateBlock v-if="state === 'loading'" state="loading" />
    <StateBlock v-else-if="state === 'empty'" state="empty" />
    <StateBlock
      v-else-if="state === 'error'"
      state="error"
      :message="error?.message"
      :can-retry="!['RESOURCE_NOT_FOUND', 'PRO_REQUIRED', 'ANALYSIS_NOT_COMPLETED'].includes(error?.code)"
      @retry="load"
    />

    <div v-if="state === 'error'" class="error-action">
      <router-link v-if="error?.code === 'RESOURCE_NOT_FOUND'" :to="{ name: 'recordingList' }">
        기록 목록으로
      </router-link>
      <router-link v-else-if="error?.code === 'PRO_REQUIRED'" :to="{ name: 'upgrade' }">
        PRO로 업그레이드
      </router-link>
      <router-link
        v-else-if="error?.code === 'ANALYSIS_NOT_COMPLETED'"
        :to="{
          name: 'analysisProgress',
          params: { analysisId: 'recording-status' },
          query: { recordingId },
        }"
      >
        분석 진행 상황 보기
      </router-link>
    </div>

    <template v-if="state === 'ready' && result">
      <MetricGrid :metrics="result.metrics" />

      <section class="panel">
        <h2>발화와 침묵</h2>
        <WaveformCanvas :points="result.metrics.waveform" />
        <SpeechIntervalBar
          :intervals="result.metrics.speechIntervals"
          :duration-ms="result.metrics.durationMs"
        />
      </section>

      <section class="panel">
        <h2>추임새 타임라인</h2>
        <FillerTimeline
          :events="result.metrics.fillerTimeline"
          :duration-ms="result.metrics.durationMs"
        />
      </section>

      <section class="panel">
        <h2>구간별 추임새</h2>
        <SegmentBars :segments="result.metrics.segmentAnalysis" />
      </section>

      <section class="panel">
        <h2>추임새 분해</h2>
        <ul class="breakdown">
          <li v-for="item in result.metrics.basic.fillerBreakdown" :key="item.word">
            <span>{{ item.word }}</span>
            <strong>{{ item.count }}회</strong>
          </li>
        </ul>
      </section>

      <CoachingCard :coaching="result.metrics.coaching" />
    </template>
  </section>
</template>

<style scoped>
.pro-page { display: grid; gap: var(--space-6); }
.panel { display: grid; gap: var(--space-3); padding: var(--space-4);
  border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.panel h2 { margin: 0; font-size: 1.125rem; }
.breakdown { display: grid; gap: var(--space-2); margin: 0; padding: 0; list-style: none; }
.breakdown li { display: flex; justify-content: space-between; padding: var(--space-2);
  background: var(--color-surface); border-radius: var(--radius-1); }
.error-action { display: flex; justify-content: center; }
</style>
