<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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
const previewMode = import.meta.env.DEV && import.meta.env.VITE_WIREFRAME_PREVIEW !== 'false'
// 백엔드 연결 전 레이아웃 검수용 값입니다. 실제 응답이 오면 이 값은 사용하지 않습니다.
const previewMetrics = {
  durationMs: 0,
  speechDurationMs: 0,
  silenceDurationMs: 0,
  longSilenceCount: 0,
  repeatedExpressionCount: 0,
  basic: { fillerTotalCount: 0, fillerBreakdown: [{ expression: '음', count: 0 }, { expression: '어', count: 0 }] },
  speechIntervals: [],
  waveform: [],
  speechRate: { wordsPerMinute: null, totalWordCount: 0 },
  fillerTimeline: [],
  segmentAnalysis: [{ segment: 'INITIAL', fillerCount: 0 }, { segment: 'MIDDLE', fillerCount: 0 }, { segment: 'FINAL', fillerCount: 0 }],
  coaching: { summary: '분석 결과가 들어오면 코칭 내용이 표시됩니다.', practiceRecommendation: '분석 후 맞춤 연습 방법을 확인할 수 있습니다.', actionItems: [] },
}
const displayMetrics = computed(() => result.value?.metrics ?? previewMetrics)

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

    <StateBlock v-if="state === 'loading' && !previewMode" state="loading" />
    <StateBlock v-else-if="state === 'empty' && !previewMode" state="empty" />
    <StateBlock
      v-else-if="state === 'error' && !previewMode"
      state="error"
      :message="error?.message"
      :can-retry="!['RESOURCE_NOT_FOUND', 'PRO_REQUIRED', 'ANALYSIS_NOT_COMPLETED'].includes(error?.code)"
      @retry="load"
    />

    <div v-if="state === 'error' && !previewMode" class="error-action">
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

    <template v-if="(state === 'ready' && result) || previewMode">
      <p v-if="previewMode && !result" class="preview-note">WIREFRAME PREVIEW · 내부 파라미터를 연결하면 실제 값으로 교체됩니다.</p>
      <MetricGrid :metrics="displayMetrics" />

      <section class="panel">
        <h2>발화와 침묵</h2>
        <WaveformCanvas :points="displayMetrics.waveform" />
        <SpeechIntervalBar
          :intervals="displayMetrics.speechIntervals"
          :duration-ms="displayMetrics.durationMs"
        />
      </section>

      <section class="panel">
        <h2>추임새 타임라인</h2>
        <FillerTimeline
          :events="displayMetrics.fillerTimeline"
          :duration-ms="displayMetrics.durationMs"
        />
      </section>

      <section class="panel">
        <h2>구간별 추임새</h2>
        <SegmentBars :segments="displayMetrics.segmentAnalysis" />
      </section>

      <section class="panel">
        <h2>추임새 분해</h2>
        <ul class="breakdown">
          <li v-for="item in displayMetrics.basic.fillerBreakdown" :key="item.expression">
            <span>{{ item.expression }}</span>
            <strong>{{ item.count }}회</strong>
          </li>
        </ul>
      </section>

      <CoachingCard :coaching="displayMetrics.coaching" />
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
.preview-note { margin: 0; padding: var(--space-3); color: var(--color-text-muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .78rem; border: 1px dashed var(--color-border); border-radius: 7px; background: var(--color-surface); }
</style>
