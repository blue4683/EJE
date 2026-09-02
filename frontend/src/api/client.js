import axios from 'axios'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // 절대 하드코딩하지 않는다
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 서버의 공통 에러 포맷 { code, message, detail } 을 그대로 꺼내 준다
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const data = err.response?.data
    return Promise.reject({
      code: data?.code ?? 'NETWORK_ERROR',
      message: data?.message ?? '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    })
  },
)
