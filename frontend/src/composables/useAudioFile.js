import { ref } from 'vue'
import { validateAudio } from '@/utils/audio'

export function useAudioFile() {
  const file = ref(null)
  const error = ref(null)
  const validating = ref(false)

  async function select(selectedFile) {
    error.value = null
    file.value = null
    if (!selectedFile) return

    validating.value = true
    try {
      const validationError = await validateAudio(selectedFile)
      if (validationError) {
        error.value = validationError
        return
      }
      file.value = selectedFile
    } finally {
      validating.value = false
    }
  }

  function reset() {
    file.value = null
    error.value = null
    validating.value = false
  }

  return { file, error, validating, select, reset }
}
