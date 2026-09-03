const EMAIL_RE = /^[\x21-\x7e]+@[\x21-\x7e]+\.[A-Za-z]{2,}$/

const codePoints = (value) => [...value].length
const utf8Bytes = (value) => new TextEncoder().encode(value).length

export const normalizeEmail = (value) => value.trim().toLowerCase()

export function validateEmail(raw) {
  const value = normalizeEmail(raw)
  if (!value) return '이메일을 입력해 주세요.'
  if (value.length > 254) return '이메일이 너무 깁니다.'
  if (!EMAIL_RE.test(value)) return '이메일 형식이 올바르지 않습니다.'
  return null
}

export function validatePassword(raw) {
  const length = codePoints(raw)
  if (length < 8 || length > 64) {
    return '비밀번호는 8자 이상 64자 이하로 입력해 주세요.'
  }
  if (utf8Bytes(raw) > 72) return '비밀번호가 너무 깁니다. 더 짧게 입력해 주세요.'
  return null
}

export function validateName(raw) {
  const value = raw.trim()
  const length = codePoints(value)
  if (length < 1 || length > 50) return '이름은 1자 이상 50자 이하로 입력해 주세요.'
  return null
}
