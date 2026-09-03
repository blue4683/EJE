import { client, unwrap } from './client'

export const fetchRecordingPage = (page = 0, size = 20) =>
  unwrap(client.get('/dashboard/recordings', { params: { page, size } }))

export const fetchRecentAnalyses = () =>
  unwrap(client.get('/dashboard/recent-analyses'))

export const fetchRecordingDetail = (recordingId) =>
  unwrap(client.get(`/recordings/${recordingId}`))

export const fetchBasicResult = (recordingId) =>
  unwrap(client.get(`/dashboard/recordings/${recordingId}/result`))

export const removeRecording = (recordingId) =>
  unwrap(client.delete(`/recordings/${recordingId}`))
