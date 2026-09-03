import { onMounted, onUnmounted, ref } from 'vue'

export function useOnlineStatus() {
  const online = ref(navigator.onLine)
  const markOnline = () => { online.value = true }
  const markOffline = () => { online.value = false }

  onMounted(() => {
    window.addEventListener('online', markOnline)
    window.addEventListener('offline', markOffline)
  })
  onUnmounted(() => {
    window.removeEventListener('online', markOnline)
    window.removeEventListener('offline', markOffline)
  })

  return { online }
}
