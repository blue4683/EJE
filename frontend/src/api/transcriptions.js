import { client } from './client'

// 오디오 업로드 → 202 { jobId, status }
export const requestTranscription = (file) => {
  const form = new FormData()
  form.append('file', file)
  // multipart 는 Content-Type 을 브라우저가 boundary와 함께 자동 설정하게 둔다
  return client.post('/api/transcriptions', form, {
    headers: { 'Content-Type': undefined },
  }).then((r) => r.data)
}

// 상태 조회 → { jobId, status, text, errorMessage }
export const fetchTranscription = (jobId) =>
  client.get(`/api/transcriptions/${jobId}`).then((r) => r.data)
