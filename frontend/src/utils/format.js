const KST = 'Asia/Seoul'

const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: KST,
  dateStyle: 'medium',
  timeStyle: 'short',
})
const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: KST,
  dateStyle: 'medium',
})

export const formatDateTime = (iso) =>
  iso ? dateTimeFormatter.format(new Date(iso)) : '—'
export const formatDate = (iso) => (iso ? dateFormatter.format(new Date(iso)) : '—')
export const formatDayLabel = (ymd) => (ymd ? ymd.slice(5).replace('-', '/') : '—')
export const formatMs = (ms) => (ms == null ? '—' : `${(ms / 1000).toFixed(1)}초`)
export const formatBytes = (bytes) => {
  if (bytes == null) return '—'
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
}
export const formatCount = (value) =>
  value == null ? '—' : `${value.toLocaleString('ko-KR')}회`
export const formatNumber = (value) =>
  value == null ? '—' : value.toLocaleString('ko-KR')
export const formatPercent = (value) =>
  value == null ? '—' : `${value > 0 ? '+' : ''}${value}%`

export const PLAN_LABEL = { FREE: '무료', PRO: 'PRO' }
export const STATUS_LABEL = {
  PENDING: '대기 중',
  PROCESSING: '분석 중',
  COMPLETED: '완료',
  FAILED: '실패',
}
