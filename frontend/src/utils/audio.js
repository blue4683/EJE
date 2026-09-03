import {
  ALLOWED_MIME,
  MAX_DURATION_MS,
  MAX_FILE_BYTES,
  MIN_DURATION_MS,
} from '@/constants/audio'

const RECORDING_MIME_CANDIDATES = [
  'audio/webm;codecs=opus',
  'audio/webm',
  'audio/mp4',
  'audio/ogg;codecs=opus',
]

export const baseMime = (type) => (type || '').split(';')[0].trim().toLowerCase()

export const isAllowedMime = (type) => ALLOWED_MIME.includes(baseMime(type))

const EXTENSION_MIME = {
  webm: 'audio/webm',
  mp4: 'audio/mp4',
  m4a: 'audio/mp4',
  ogg: 'audio/ogg',
  oga: 'audio/ogg',
  wav: 'audio/wav',
  mp3: 'audio/mpeg',
}

export function pickMimeType() {
  if (typeof MediaRecorder === 'undefined') return ''

  return RECORDING_MIME_CANDIDATES.find((type) => (
    ALLOWED_MIME.includes(baseMime(type)) && MediaRecorder.isTypeSupported?.(type)
  )) || ''
}

export function resolveMime(file) {
  if (isAllowedMime(file.type)) return baseMime(file.type)
  const extension = file.name?.split('.').pop()?.toLowerCase()
  return EXTENSION_MIME[extension] ?? baseMime(file.type)
}

export function probeDurationMs(blob) {
  return new Promise((resolve) => {
    const objectUrl = URL.createObjectURL(blob)
    const audio = new Audio()
    const finish = (duration) => {
      URL.revokeObjectURL(objectUrl)
      resolve(duration)
    }

    audio.preload = 'metadata'
    audio.onloadedmetadata = () => {
      const duration = audio.duration
      finish(Number.isFinite(duration) && duration > 0
        ? Math.round(duration * 1000)
        : null)
    }
    audio.onerror = () => finish(null)
    audio.src = objectUrl
  })
}

export async function validateAudio(file) {
  const mime = resolveMime(file)

  if (!isAllowedMime(mime)) {
    return { code: 'UNSUPPORTED_MEDIA_TYPE', message: '지원하지 않는 음성 형식입니다.' }
  }
  if (file.size === 0) {
    return { code: 'INVALID_AUDIO', message: '음성 파일을 읽을 수 없습니다. 다시 녹음해 주세요.' }
  }
  if (file.size > MAX_FILE_BYTES) {
    return { code: 'FILE_TOO_LARGE', message: '파일이 너무 큽니다. 16MB 이하로 올려 주세요.' }
  }

  const durationMs = await probeDurationMs(file)
  if (durationMs != null && (durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS)) {
    return {
      code: 'AUDIO_DURATION_OUT_OF_RANGE',
      message: '1초 이상 60초 이하로 녹음해 주세요.',
    }
  }
  return null
}
