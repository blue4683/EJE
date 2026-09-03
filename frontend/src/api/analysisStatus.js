import { client, unwrap } from './client'

export const fetchStatusByAnalysis = (analysisId) => (
  unwrap(client.get(`/analyses/${analysisId}/status`))
)

export const fetchStatusByRecording = (recordingId) => (
  unwrap(client.get(`/dashboard/recordings/${recordingId}/status`))
)
