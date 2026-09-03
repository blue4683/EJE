import { computed, onUnmounted, ref } from 'vue'
import { MAX_DURATION_MS, MIN_DURATION_MS } from '@/constants/audio'
import { pickMimeType } from '@/utils/audio'

export function useRecorder() {
  const state = ref('idle')
  const elapsedMs = ref(0)
  const level = ref(0)
  const blob = ref(null)
  const mimeType = ref('')
  const error = ref(null)

  let stream = null
  let recorder = null
  let chunks = []
  let audioContext = null
  let analyser = null
  let animationFrameId = null
  let timerId = null
  let startedAt = 0
  let failed = false

  const canSubmit = computed(() => (
    state.value === 'ready'
      && Boolean(blob.value)
      && elapsedMs.value >= MIN_DURATION_MS
  ))

  function release() {
    if (animationFrameId != null) cancelAnimationFrame(animationFrameId)
    animationFrameId = null
    clearInterval(timerId)
    timerId = null
    stream?.getTracks().forEach((track) => track.stop())
    stream = null
    audioContext?.close().catch(() => {})
    audioContext = null
    analyser = null
    recorder = null
    level.value = 0
  }

  function updateLevel() {
    if (!analyser) return

    const samples = new Uint8Array(analyser.fftSize)
    analyser.getByteTimeDomainData(samples)
    let sum = 0
    for (const sample of samples) {
      const normalized = (sample - 128) / 128
      sum += normalized * normalized
    }
    level.value = Math.min(1, Math.sqrt(sum / samples.length) * 3)
    animationFrameId = requestAnimationFrame(updateLevel)
  }

  async function start() {
    if (['requesting', 'recording', 'stopping'].includes(state.value)) return

    reset()
    state.value = 'requesting'

    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      state.value = 'error'
      error.value = '이 브라우저에서는 음성 녹음을 사용할 수 없습니다.'
      return
    }

    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          channelCount: 1,
        },
      })

      mimeType.value = pickMimeType()
      recorder = new MediaRecorder(
        stream,
        mimeType.value ? { mimeType: mimeType.value } : undefined,
      )
      chunks = []
      failed = false

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) chunks.push(event.data)
      }
      recorder.onerror = () => {
        failed = true
        state.value = 'error'
        error.value = '녹음 중 문제가 발생했습니다. 다시 시도해 주세요.'
        chunks = []
        release()
      }
      recorder.onstop = () => {
        if (failed) return
        const actualMimeType = recorder?.mimeType || mimeType.value
        mimeType.value = actualMimeType
        blob.value = new Blob(chunks, { type: actualMimeType })
        chunks = []
        state.value = 'ready'
        release()
      }

      const AudioContext = window.AudioContext || window.webkitAudioContext
      if (AudioContext) {
        audioContext = new AudioContext()
        analyser = audioContext.createAnalyser()
        analyser.fftSize = 2048
        audioContext.createMediaStreamSource(stream).connect(analyser)
      }

      startedAt = performance.now()
      recorder.start(250)
      state.value = 'recording'
      updateLevel()

      timerId = setInterval(() => {
        elapsedMs.value = Math.round(performance.now() - startedAt)
        if (elapsedMs.value >= MAX_DURATION_MS) stop()
      }, 100)
    } catch (caught) {
      const denied = caught?.name === 'NotAllowedError'
      state.value = denied ? 'denied' : 'error'
      error.value = denied
        ? '마이크 사용이 차단되어 있습니다. 브라우저 주소창의 권한 설정을 확인해 주세요.'
        : '마이크를 찾을 수 없습니다. 장치 연결을 확인해 주세요.'
      release()
    }
  }

  function stop() {
    if (state.value !== 'recording') return

    state.value = 'stopping'
    elapsedMs.value = Math.min(
      MAX_DURATION_MS,
      Math.round(performance.now() - startedAt),
    )
    clearInterval(timerId)
    timerId = null
    recorder?.stop()
  }

  function reset() {
    release()
    chunks = []
    failed = false
    state.value = 'idle'
    elapsedMs.value = 0
    level.value = 0
    blob.value = null
    mimeType.value = ''
    error.value = null
  }

  onUnmounted(() => {
    if (recorder?.state === 'recording') recorder.stop()
    release()
  })

  return {
    state,
    elapsedMs,
    level,
    blob,
    mimeType,
    error,
    canSubmit,
    start,
    stop,
    reset,
  }
}
