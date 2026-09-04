import { defineStore } from 'pinia'

export const useSubmissionStore = defineStore('submission', {
  state: () => ({
    blob: null,
    analysisId: null,
    recordingId: null,
  }),
  actions: {
    keep({ blob, analysisId, recordingId }) {
      this.blob = blob
      this.analysisId = analysisId
      this.recordingId = recordingId
    },
    hasAudioFor(analysisId) {
      return Boolean(this.blob) && this.analysisId === analysisId
    },
    clear() {
      this.blob = null
      this.analysisId = null
      this.recordingId = null
    },
  },
})
