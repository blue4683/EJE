import axios from 'axios'
import { useSessionStore } from '@/stores/session'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 20000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

const NO_REISSUE = ['/auth/login', '/auth/signup', '/auth/reissue', '/auth/logout']
const isNoReissue = (url = '') => NO_REISSUE.some((path) => url.startsWith(path))

let sessionExpiredHandler = () => {}
let reissuing = null

export const onSessionExpired = (handler) => {
  sessionExpiredHandler = handler
}

client.interceptors.request.use((config) => {
  const session = useSessionStore()
  if (session.accessToken && !isNoReissue(config.url)) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  return config
})

const reissueOnce = () => {
  if (!reissuing) {
    reissuing = client
      .post('/auth/reissue')
      .then((response) => response.data.data.accessToken)
      .finally(() => {
        reissuing = null
      })
  }
  return reissuing
}

client.interceptors.response.use(
  (res) => res,
  async (err) => {
    const { response, config } = err

    if (response?.status === 401 && config && !config._retried && !isNoReissue(config.url)) {
      config._retried = true
      try {
        const accessToken = await reissueOnce()
        useSessionStore().setAccessToken(accessToken)
        return client(config)
      } catch {
        useSessionStore().clear()
        sessionExpiredHandler()
      }
    }

    return Promise.reject(toApiError(err))
  },
)

export function toApiError(err) {
  const body = err.response?.data
  if (body?.error?.code) {
    return {
      code: body.error.code,
      message: body.error.message,
      detail: body.error.detail ?? null,
      status: err.response.status,
    }
  }
  if (err.code === 'ECONNABORTED') {
    return {
      code: 'REQUEST_TIMEOUT',
      message: '요청 시간이 초과되었습니다. 다시 시도해 주세요.',
      detail: null,
      status: 0,
    }
  }
  return {
    code: 'NETWORK_ERROR',
    message: '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    detail: null,
    status: err.response?.status == null ? 0 : err.response.status,
  }
}

export const unwrap = (promise) => promise.then((response) => response.data?.data ?? null)
