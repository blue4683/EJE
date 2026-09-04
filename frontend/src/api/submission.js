import { client, unwrap } from './client'
import { baseMime, resolveMime } from '@/utils/audio'

const EXTENSION_BY_MIME = {
  'audio/webm': 'webm',
  'audio/mp4': 'm4a',
  'audio/ogg': 'ogg',
  'audio/wav': 'wav',
  'audio/mpeg': 'mp3',
}

function audioForm(blob) {
  const form = new FormData()
  const mime = resolveMime(blob)
  const uploadBlob = baseMime(blob.type) === mime
    ? blob
    : new Blob([blob], { type: mime })
  form.append('audio', uploadBlob, `recording.${EXTENSION_BY_MIME[mime] ?? 'webm'}`)
  return form
}

function uploadConfig(idempotencyKey, onProgress, signal) {
  return {
    headers: {
      'Content-Type': undefined,
      'Idempotency-Key': idempotencyKey,
    },
    timeout: 60000,
    onUploadProgress: onProgress,
    signal,
  }
}

export const submitRecording = (blob, idempotencyKey, onProgress, signal) => (
  unwrap(client.post(
    '/recordings',
    audioForm(blob),
    uploadConfig(idempotencyKey, onProgress, signal),
  ))
)

export const retryAnalysis = (analysisId, blob, idempotencyKey, onProgress, signal) => (
  unwrap(client.post(
    `/analyses/${analysisId}/retry`,
    audioForm(blob),
    uploadConfig(idempotencyKey, onProgress, signal),
  ))
)
