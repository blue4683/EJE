<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { submitRecording } from '@/api/submission'
import { logIn, signUp } from '@/api/auth'
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
import { useSessionStore } from '@/stores/session'
import { normalizeEmail, validateEmail, validatePassword } from '@/utils/validators'

const router = useRouter()
const recorder = useRecorder()
const audioFile = useAudioFile()
const { online } = useOnlineStatus()
const submission = useSubmissionStore()
const session = useSessionStore()

const submitting = ref(false)
const progress = ref(0)
const uploadError = ref(null)
const autoRetryScheduled = ref(false)
const authOpen = ref(false)
const authMode = ref('login')
const authSubmitting = ref(false)
const authError = ref(null)
const authForm = reactive({ name: '', email: '', password: '' })
const authTouched = reactive({ email: false, password: false })
const previewUrl = ref('')
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
const authCanSubmit = computed(() => (
  !authSubmitting.value && (authMode.value === 'login' || authForm.name.trim().length > 0)
    && !validateEmail(authForm.email) && !validatePassword(authForm.password)
))

watch(selectedAudio, (blob, previous) => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = blob instanceof Blob ? URL.createObjectURL(blob) : ''
})

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
  if (!session.isAuthenticated) {
    authError.value = null
    authOpen.value = true
    return
  }

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

function closeAuth() {
  if (!authSubmitting.value) authOpen.value = false
}

function switchAuthMode(mode) {
  authMode.value = mode
  authError.value = null
  authTouched.email = false
  authTouched.password = false
}

async function loginAndSubmit() {
  authTouched.email = true
  authTouched.password = true
  if (!authCanSubmit.value) return
  authSubmitting.value = true
  authError.value = null
  try {
    const request = authMode.value === 'login'
      ? logIn({ email: normalizeEmail(authForm.email), password: authForm.password })
      : signUp({ name: authForm.name.trim(), email: normalizeEmail(authForm.email), password: authForm.password })
    const data = await request
    session.setSession({ accessToken: data.accessToken, user: data.user })
    authOpen.value = false
    await submit()
  } catch (caught) {
    authError.value = caught
  } finally {
    authSubmitting.value = false
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
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <section class="record-page">
    <PageHeader
      title="1분 자기소개"
      description="최대 1분 동안 자유롭게 자신을 소개해보세요."
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
      <audio v-if="previewUrl" class="preview-player" controls :src="previewUrl" aria-label="녹음 미리듣기" />
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

    <div v-if="authOpen" class="auth-backdrop" role="presentation" @click.self="closeAuth">
      <section class="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button class="auth-modal__close" type="button" aria-label="로그인 모달 닫기" @click="closeAuth">×</button>
        <p class="eyebrow">분석 결과를 저장할까요?</p>
        <h2 id="auth-title">{{ authMode === 'login' ? '로그인 후 녹음 분석을 계속합니다' : '회원가입 후 녹음 분석을 계속합니다' }}</h2>
        <p class="auth-modal__notice">현재 녹음은 브라우저 메모리에만 있습니다. 새로고침하면 사라집니다.</p>
        <form @submit.prevent="loginAndSubmit">
          <label v-if="authMode === 'signup'">이름<input v-model="authForm.name" type="text" autocomplete="name"></label>
          <label>이메일<input v-model="authForm.email" type="email" autocomplete="email" @blur="authTouched.email = true"></label>
          <p v-if="authTouched.email && validateEmail(authForm.email)" class="error">{{ validateEmail(authForm.email) }}</p>
          <label>비밀번호<input v-model="authForm.password" type="password" autocomplete="current-password" @blur="authTouched.password = true"></label>
          <p v-if="authTouched.password && validatePassword(authForm.password)" class="error">{{ validatePassword(authForm.password) }}</p>
          <p v-if="authError" class="error" role="alert">{{ authError.message }}</p>
          <button class="modal-submit" type="submit" :disabled="!authCanSubmit">{{ authSubmitting ? '처리 중…' : authMode === 'login' ? '로그인 후 분석하기' : '회원가입 후 분석하기' }}</button>
        </form>
        <button class="auth-switch" type="button" @click="switchAuthMode(authMode === 'login' ? 'signup' : 'login')">{{ authMode === 'login' ? '회원가입으로 계속하기' : '로그인으로 계속하기' }}</button>
      </section>
    </div>

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
.record-page { display: grid; width: 100%; gap: var(--space-4); margin: 0; }
.record-card { display: grid; justify-items: center; gap: var(--space-5); padding: 36px var(--space-5) 28px;
  background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 7px; }
.preview-player { width: min(100%, 440px); }
.divider { display: flex; align-items: center; gap: var(--space-3); color: var(--color-text-muted); }
.divider::before,
.divider::after { flex: 1; height: 1px; content: ''; background: var(--color-border); }
.file-picker { display: grid; gap: var(--space-2); padding: var(--space-4); color: var(--color-text-muted); border: 1px dashed var(--color-border); border-radius: 7px; }
.submit { width: fit-content; padding: var(--space-3) var(--space-6); border: 0;
  border: 1px solid #222; border-radius: 7px; color: white; background: #222; font-weight: 700; }
.submit:disabled { cursor: not-allowed; opacity: 0.5; }
.error { color: var(--color-danger); }
.auth-backdrop { position: fixed; z-index: 20; inset: 0; display: grid; place-items: center; padding: var(--space-5); background: rgba(0, 0, 0, .42); }
.auth-modal { position: relative; width: min(100%, 480px); padding: 36px; background: white; border: 1px solid var(--color-border); border-radius: 7px; box-shadow: 0 18px 50px rgba(0,0,0,.2); }.auth-modal__close { position: absolute; top: 12px; right: 14px; border: 0; color: #777; background: transparent; font-size: 1.6rem; }.auth-modal h2 { margin: var(--space-2) 0; }.auth-modal__notice { color: var(--color-text-muted); font-size: .9rem; }.auth-modal form { display: grid; gap: var(--space-3); margin-top: var(--space-5); }.auth-modal label { display: grid; gap: var(--space-1); color: var(--color-text-muted); font-size: .85rem; }.auth-modal input { padding: 11px; border: 1px solid var(--color-border); border-radius: 6px; }.auth-modal .error { margin: -6px 0 0; font-size: .82rem; }.modal-submit { padding: 12px; color: white; border: 1px solid #222; border-radius: 6px; background: #222; font-weight: 800; }.auth-switch { display: block; width: 100%; margin-top: var(--space-4); padding: 0; color: var(--color-text-muted); text-align: center; border: 0; background: transparent; text-decoration: underline; }
</style>
