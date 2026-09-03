import { client, unwrap } from './client'

export const fetchProAnalysis = (recordingId) => (
  unwrap(client.get(`/recordings/${recordingId}/pro-analysis`))
)
