<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { submitRecording } from '@/api/submission'
import PageHeader from '@/components/common/PageHeader.vue'
import UploadController from '@/components/practice/UploadController.vue'
import LevelMeter from '@/components/recorder/LevelMeter.vue'
import RecordButton from '@/components/recorder/RecordButton.vue'
import RecordTimer from '@/components/recorder/RecordTimer.vue'
import UploadProgress from '@/components/recorder/UploadProgress.vue'
import { useAudioFile } from '@/composables/useAudioFile'
import { useOnlineStatus } from '@/composables/useOnlineStatus'
import { useRecorder } from '@/composables/useRecorder'
import { MIN_DURATION_MS } from '@/constants/audio'
import { useSubmissionStore } from '@/stores/submission'
import { validateAudio } from '@/utils/audio'
import { formatMs } from '@/utils/format'
import { newIdempotencyKey } from '@/utils/idempotency'

const router = useRouter()
const recorder = useRecorder()
const audioFile = useAudioFile()
const { online } = useOnlineStatus()
const submission = useSubmissionStore()

const submitting = ref(false)
const progress = ref(0)
const uploadError = ref(null)
const autoRetryScheduled = ref(false)
let idempotencyKey = null
let controller = null
let autoRetryTimer = null
let automaticCapacityRetryUsed = false

const selectedAudio = computed(() => audioFile.file.value || recorder.blob.value)
const validationError = computed(() => audioFile.error.value || (
  recorder.state.value === 'ready' && recorder.elapsedMs.value < MIN_DURATION_MS
    ? { code: 'AUDIO_DURATION_OUT_OF_RANGE', message: '1초 이상 녹음해 주세요.' }
    : null
))
const canSubmit = computed(() => (
  Boolean(selectedAudio.value)
    && !validationError.value
    && !submitting.value
    && online.value
))

function clearAutoRetry() {
  clearTimeout(autoRetryTimer)
  autoRetryTimer = null
  autoRetryScheduled.value = false
}

function resetUploadAttempt() {
  clearAutoRetry()
  controller?.abort()
  controller = null
  idempotencyKey = null
  automaticCapacityRetryUsed = false
  submitting.value = false
  progress.value = 0
  uploadError.value = null
}

async function handleFileChange(event) {
  resetUploadAttempt()
  recorder.reset()
  await audioFile.select(event.target.files?.[0] ?? null)
}

async function startRecording() {
  resetUploadAttempt()
  audioFile.reset()
  await recorder.start()
}

function resetRecording() {
  resetUploadAttempt()
  recorder.reset()
}

function scheduleCapacityRetry() {
  if (automaticCapacityRetryUsed || autoRetryTimer) return
  automaticCapacityRetryUsed = true
  autoRetryScheduled.value = true
  autoRetryTimer = setTimeout(() => {
    autoRetryTimer = null
    autoRetryScheduled.value = false
    if (online.value) submit()
  }, 10000)
}

async function submit() {
  const blob = selectedAudio.value
  if (!blob || submitting.value || !online.value) return

  clearAutoRetry()
  uploadError.value = await validateAudio(blob)
  if (uploadError.value) return

  idempotencyKey ??= newIdempotencyKey()
  const requestController = new AbortController()
  controller = requestController
  submitting.value = true
  progress.value = 0

  try {
    const data = await submitRecording(
      blob,
      idempotencyKey,
      (event) => {
        progress.value = event.total
          ? Math.round((event.loaded / event.total) * 100)
          : 0
      },
      requestController.signal,
    )
    submission.keep({
      blob,
      analysisId: data.analysisId,
      recordingId: data.recordingId,
    })
    idempotencyKey = null
    submitting.value = false
    controller = null
    await router.push({
      name: 'analysisProgress',
      params: { analysisId: data.analysisId },
    })
  } catch (caught) {
    if (requestController.signal.aborted || caught.code === 'ERR_CANCELED') {
      uploadError.value = null
      return
    }
    uploadError.value = caught
    if (caught.code === 'ANALYSIS_CAPACITY_EXCEEDED') scheduleCapacityRetry()
    if (['IDEMPOTENCY_KEY_CONFLICT', 'RESOURCE_GONE'].includes(caught.code)) {
      idempotencyKey = null
    }
  } finally {
    submitting.value = false
    if (controller === requestController) controller = null
  }
}

function cancelUpload() {
  controller?.abort()
}

function confirmLeave() {
  if (recorder.state.value !== 'recording' && !submitting.value) return true
  return window.confirm('진행 중인 녹음 또는 업로드가 있습니다. 화면을 나가시겠습니까?')
}

onBeforeRouteLeave(() => {
  const canLeave = confirmLeave()
  if (canLeave) {
    cancelUpload()
    recorder.reset()
  }
  return canLeave
})

function handleBeforeUnload(event) {
  if (recorder.state.value !== 'recording' && !submitting.value) return
  event.preventDefault()
  event.returnValue = ''
}

onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))
onUnmounted(() => {
  clearAutoRetry()
  controller?.abort()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <section class="record-page">
    <PageHeader
      title="말하기 연습"
      description="1초 이상 60초 이하로 녹음하거나 음성 파일을 선택해 주세요."
    />

    <div class="record-card">
      <RecordButton
        :state="recorder.state.value"
        :disabled="submitting"
        @start="startRecording"
        @stop="recorder.stop"
        @reset="resetRecording"
      />
      <RecordTimer :elapsed-ms="recorder.elapsedMs.value" />
      <LevelMeter
        :level="recorder.level.value"
        :active="recorder.state.value === 'recording'"
      />

      <p v-if="recorder.error.value" class="error" role="alert">
        {{ recorder.error.value }}
      </p>
      <p v-if="recorder.state.value === 'ready'">
        녹음 길이 {{ formatMs(recorder.elapsedMs.value) }} · {{ recorder.blob.value?.size.toLocaleString('ko-KR') }} bytes
      </p>
    </div>

    <div class="divider"><span>또는</span></div>

    <label class="file-picker">
      음성 파일 선택
      <input
        type="file"
        accept="audio/webm,audio/mp4,audio/ogg,audio/wav,audio/mpeg"
        :disabled="submitting"
        @change="handleFileChange"
      >
    </label>
    <p v-if="audioFile.validating.value">파일을 확인하고 있습니다…</p>
    <p v-if="audioFile.file.value">{{ audioFile.file.value.name }}</p>
    <p v-if="validationError" class="error" role="alert">{{ validationError.message }}</p>

    <button class="submit" type="button" :disabled="!canSubmit" @click="submit">
      {{ submitting ? '제출 중…' : '분석 요청' }}
    </button>

    <UploadProgress :active="submitting" :progress="progress" />
    <UploadController
      :submitting="submitting"
      :progress="progress"
      :online="online"
      :error="uploadError"
      :auto-retry-scheduled="autoRetryScheduled"
      @cancel="cancelUpload"
      @retry="submit"
      @cancel-auto-retry="clearAutoRetry"
    />

    <router-link
      v-if="uploadError?.code === 'ANALYSIS_ALREADY_ACTIVE'"
      :to="{ name: 'recordingList' }"
    >
      진행 중인 기록 보기
    </router-link>
  </section>
</template>

<style scoped>
.record-page { display: grid; gap: var(--space-4); }
.record-card { display: grid; justify-items: center; gap: var(--space-4); padding: var(--space-6);
  background: var(--color-surface); border-radius: var(--radius-2); }
.divider { display: flex; align-items: center; gap: var(--space-3); color: var(--color-text-muted); }
.divider::before,
.divider::after { flex: 1; height: 1px; content: ''; background: var(--color-border); }
.file-picker { display: grid; gap: var(--space-2); }
.submit { width: fit-content; padding: var(--space-3) var(--space-6); border: 0;
  border-radius: var(--radius-1); color: white; background: var(--color-primary); font-weight: 700; }
.submit:disabled { cursor: not-allowed; opacity: 0.5; }
.error { color: var(--color-danger); }
</style>
