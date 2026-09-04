<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchStatusByAnalysis, fetchStatusByRecording } from '@/api/analysisStatus'
import { retryAnalysis } from '@/api/submission'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import FailureNotice from '@/components/practice/FailureNotice.vue'
import RetryPanel from '@/components/practice/RetryPanel.vue'
import StatusTimeline from '@/components/practice/StatusTimeline.vue'
import { useAnalysisStatus } from '@/composables/useAnalysisStatus'
import { useSubmissionStore } from '@/stores/submission'
import { newIdempotencyKey } from '@/utils/idempotency'

const props = defineProps({
  analysisId: { type: String, default: '' },
})

const route = useRoute()
const submission = useSubmissionStore()
const fetcher = () => (
  route.query.recordingId
    ? fetchStatusByRecording(String(route.query.recordingId))
    : fetchStatusByAnalysis(props.analysisId || String(route.params.analysisId))
)
const polling = useAnalysisStatus(fetcher)

const retrying = ref(false)
const retryProgress = ref(0)
const retryError = ref(null)
const pendingElapsedMs = ref(0)
let retryKey = null
let retryController = null
let pendingTimer = null
let pendingStartedAt = null

const status = computed(() => polling.data.value)
const isPending = computed(() => ['PENDING', 'PROCESSING'].includes(status.value?.status))
const hasAudio = computed(() => (
  status.value ? submission.hasAudioFor(status.value.analysisId) : false
))
const retryExhausted = computed(() => (
  status.value?.status === 'FAILED'
    && !status.value.retryable
    && status.value.attemptNo === 4
))
const pendingMessage = computed(() => {
  if (pendingElapsedMs.value < 10000) return '분석을 진행하고 있습니다…'
  if (pendingElapsedMs.value < 60000) return '분석 중입니다. 잠시만 기다려 주세요.'
  return '시간이 조금 더 걸리고 있습니다. 이 화면을 닫아도 분석은 계속됩니다.'
})

function updatePendingClock() {
  pendingElapsedMs.value = pendingStartedAt ? Date.now() - pendingStartedAt : 0
}

watch(isPending, (pending) => {
  clearInterval(pendingTimer)
  pendingTimer = null
  if (!pending) {
    pendingStartedAt = null
    pendingElapsedMs.value = 0
    return
  }
  pendingStartedAt ??= Date.now()
  updatePendingClock()
  pendingTimer = setInterval(updatePendingClock, 1000)
}, { immediate: true })

async function retry() {
  if (!status.value?.retryable || !hasAudio.value || retrying.value) return

  retryKey ??= newIdempotencyKey()
  retrying.value = true
  retryProgress.value = 0
  retryError.value = null
  const requestController = new AbortController()
  retryController = requestController

  try {
    await retryAnalysis(
      status.value.analysisId,
      submission.blob,
      retryKey,
      (event) => {
        retryProgress.value = event.total
          ? Math.round((event.loaded / event.total) * 100)
          : 0
      },
      requestController.signal,
    )
    retryKey = null
    polling.start()
  } catch (caught) {
    if (requestController.signal.aborted || caught.code === 'ERR_CANCELED') return
    retryError.value = caught
    if (caught.code === 'INVALID_ANALYSIS_STATE') polling.start()
    if (!['ANALYSIS_CAPACITY_EXCEEDED', 'REQUEST_TIMEOUT', 'NETWORK_ERROR'].includes(caught.code)) {
      retryKey = null
    }
  } finally {
    retrying.value = false
    if (retryController === requestController) retryController = null
  }
}

onMounted(polling.start)
onUnmounted(() => {
  clearInterval(pendingTimer)
  retryController?.abort()
})
</script>

<template>
  <section class="progress-page">
    <PageHeader title="분석 진행 상황" description="화면을 닫아도 서버의 분석은 계속됩니다." />

    <StateBlock v-if="polling.state.value === 'loading'" state="loading" />
    <StateBlock
      v-else-if="polling.state.value === 'error' && !status"
      state="error"
      :message="polling.error.value?.message || '분석 상태를 불러오지 못했습니다.'"
      :can-retry="polling.error.value?.status !== 404"
      @retry="polling.start"
    />
    <StateBlock v-else-if="!status" state="empty" message="분석 정보가 없습니다." />

    <template v-else>
      <StatusTimeline :status="status.status" />

      <p v-if="status.attemptNo > 1 || status.autoRetryCount > 0" class="attempt">
        {{ status.attemptNo }}차 시도 · 자동 재시도 {{ status.autoRetryCount }}회
      </p>

      <StateBlock v-if="isPending" state="pending" :message="pendingMessage" />

      <div v-else-if="status.status === 'COMPLETED'" class="complete">
        <strong>분석이 완료되었습니다.</strong>
        <router-link :to="{ name: 'recordingDetail', params: { recordingId: status.recordingId } }">
          결과 보기
        </router-link>
      </div>

      <template v-else-if="status.status === 'FAILED'">
        <FailureNotice :failure-code="status.failureCode" />
        <RetryPanel
          :retryable="status.retryable"
          :has-audio="hasAudio"
          :exhausted="retryExhausted"
          :busy="retrying"
          :progress="retryProgress"
          :error="retryError"
          @retry="retry"
        />
      </template>
    </template>
  </section>
</template>

<style scoped>
.progress-page { display: grid; gap: var(--space-4); }
.attempt { color: var(--color-text-muted); text-align: center; }
.complete { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4);
  padding: var(--space-6); background: var(--color-primary-weak); border-radius: var(--radius-2); }
</style>
