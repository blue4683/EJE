import { client, unwrap } from './client'

export const fetchTrends = (params) =>
  unwrap(client.get('/dashboard/trends', { params }))

export const fetchComparison = (recordingId, targetRecordingId) =>
  unwrap(
    client.get(`/recordings/${recordingId}/compare`, {
      params: targetRecordingId ? { targetRecordingId } : undefined,
    }),
  )

export const fetchWeeklyReport = (weekStartDate) =>
  unwrap(
    client.get('/dashboard/weekly-report', {
      params: weekStartDate ? { weekStartDate } : undefined,
    }),
  )
