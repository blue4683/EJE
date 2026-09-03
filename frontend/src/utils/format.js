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

export const formatDateTime = (iso) => (
  iso ? dateTimeFormatter.format(new Date(iso)) : '—'
)
export const formatDate = (iso) => (
  iso ? dateFormatter.format(new Date(iso)) : '—'
)
export const formatDayLabel = (ymd) => (
  ymd ? ymd.slice(5).replace('-', '/') : '—'
)
export const formatMs = (milliseconds) => (
  milliseconds == null ? '—' : `${(milliseconds / 1000).toFixed(1)}초`
)
export const formatCount = (count) => (
  count == null ? '—' : `${count.toLocaleString('ko-KR')}회`
)
export const formatNumber = (number) => (
  number == null ? '—' : number.toLocaleString('ko-KR')
)
export const formatPercent = (number) => (
  number == null ? '—' : `${number > 0 ? '+' : ''}${number}%`
)

export const PLAN_LABEL = { FREE: '무료', PRO: 'PRO' }
export const STATUS_LABEL = {
  PENDING: '대기 중',
  PROCESSING: '분석 중',
  COMPLETED: '완료',
  FAILED: '실패',
}
