import { onMounted, onUnmounted, ref } from 'vue'
import { STATUS_POLL_INTERVAL_MS } from '@/constants/audio'

const TERMINAL_STATUS = ['COMPLETED', 'FAILED']
const HIDDEN_INTERVAL_MS = 5000

export function useAnalysisStatus(fetcher) {
  const data = ref(null)
  const state = ref('loading')
  const error = ref(null)
  let timerId = null
  let stopped = true
  let requestSequence = 0

  function clearTimer() {
    clearTimeout(timerId)
    timerId = null
  }

  function stop() {
    stopped = true
    requestSequence += 1
    clearTimer()
  }

  function schedule() {
    if (stopped) return
    clearTimer()
    const delay = document.hidden ? HIDDEN_INTERVAL_MS : STATUS_POLL_INTERVAL_MS
    timerId = setTimeout(tick, delay)
  }

  async function tick() {
    if (stopped) return
    const sequence = ++requestSequence
    clearTimer()

    try {
      const next = await fetcher()
      if (stopped || sequence !== requestSequence) return
      data.value = next
      error.value = null
      if (TERMINAL_STATUS.includes(next.status)) {
        state.value = 'ready'
        clearTimer()
        return
      }
      state.value = 'pending'
      schedule()
    } catch (caught) {
      if (stopped || sequence !== requestSequence) return
      error.value = caught
      if (caught.status === 404) {
        state.value = 'error'
        clearTimer()
        return
      }
      state.value = data.value ? 'pending' : 'error'
      schedule()
    }
  }

  function start() {
    stop()
    stopped = false
    state.value = 'loading'
    data.value = null
    error.value = null
    tick()
  }

  function handleVisibilityChange() {
    if (!document.hidden && !stopped && timerId != null) tick()
  }

  onMounted(() => document.addEventListener('visibilitychange', handleVisibilityChange))
  onUnmounted(() => {
    stop()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return { data, state, error, start, stop }
}
