import { client, unwrap } from './client'

export const analyzeMockWaveform = (payload) => (
  unwrap(client.post('/mock/waveform-analysis', payload))
)

export const analyzeMockTranscript = (payload) => (
  unwrap(client.post('/mock/transcript-analysis', payload))
)
