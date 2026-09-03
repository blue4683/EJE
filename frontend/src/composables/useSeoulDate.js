const KST_OFFSET_MINUTES = 9 * 60

export const toYmd = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

export function seoulToday() {
  const now = new Date()
  const kst = new Date(
    now.getTime() + (KST_OFFSET_MINUTES + now.getTimezoneOffset()) * 60_000,
  )
  return toYmd(kst)
}

export function daysBetween(startYmd, endYmd) {
  const milliseconds =
    Date.parse(`${endYmd}T00:00:00Z`) - Date.parse(`${startYmd}T00:00:00Z`)
  return Math.floor(milliseconds / 86_400_000) + 1
}

export function seoulWeekStart(ymd = seoulToday()) {
  const date = new Date(`${ymd}T00:00:00Z`)
  const dayFromMonday = (date.getUTCDay() + 6) % 7
  date.setUTCDate(date.getUTCDate() - dayFromMonday)
  return date.toISOString().slice(0, 10)
}

export function moveYmd(ymd, days) {
  const date = new Date(`${ymd}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

export const isMonday = (ymd) =>
  Boolean(ymd) && new Date(`${ymd}T00:00:00Z`).getUTCDay() === 1
