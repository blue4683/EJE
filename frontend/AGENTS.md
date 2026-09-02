# frontend/AGENTS.md

Vue 3 + Vite 프론트엔드 규칙입니다. 루트 `AGENTS.md`를 먼저 읽고 이 파일을 적용하세요.

---

## 1. 기술 스택

- Vue 3 (**Composition API + `<script setup>` 고정**, Options API 금지)
- Vite
- axios
- 라우팅: vue-router
- 상태관리: 필요해지기 전까지 도입하지 않는다. 필요하면 Pinia.

---

## 2. 폴더 구조

```
frontend/src/
├── api/
│   ├── client.js              axios 인스턴스 (HTTP 설정은 여기 한 곳)
│   └── transcriptions.js      도메인별 API 함수
├── components/                재사용 UI 조각
├── views/                     라우트에 1:1 대응하는 페이지
├── composables/               useTranscriptionJob.js 같은 로직 재사용
├── router/index.js
├── assets/
├── App.vue
└── main.js
```

새 파일은 반드시 위 위치에 만듭니다. 최상위에 파일을 흩뿌리지 마세요.

---

## 3. HTTP 호출 — 가장 중요한 규칙

### 3.1 컴포넌트에서 `axios`나 `fetch`를 직접 부르지 않는다

모든 호출은 `src/api/` 안의 함수를 거칩니다. 이 규칙이 깨지면
"Mock에서 실제 백엔드로 코드 수정 없이 전환"이라는 이 프로젝트의 핵심 주장이 무너집니다.

```js
// src/api/client.js
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
```

```js
// src/api/transcriptions.js
import { client } from './client'

// 오디오 업로드 → 202 { jobId, status }
export const requestTranscription = (file) => {
  const form = new FormData()
  form.append('file', file)
  // multipart 는 Content-Type 을 브라우저가 boundary와 함께 자동 설정하게 둔다
  return client.post('/api/transcriptions', form, {
    headers: { 'Content-Type': undefined },
  }).then((r) => r.data)
}

// 상태 조회 → { jobId, status, text, errorMessage }
export const fetchTranscription = (jobId) =>
  client.get(`/api/transcriptions/${jobId}`).then((r) => r.data)
```

### 3.2 `baseURL`은 `.env`에서만 온다

```
# frontend/.env.example
VITE_API_BASE_URL=
```

```
# Mock 단계
VITE_API_BASE_URL=https://xxxx.mock.pstmn.io
# 실제 백엔드 전환 — 이 한 줄만 바꾼다
VITE_API_BASE_URL=http://localhost:8080
```

**전환 시 소스 코드는 단 한 줄도 바뀌지 않아야 합니다.** 발표에서 실제로 보여줄 부분입니다.

---

## 4. 화면 상태 4종은 필수

전사는 시간이 걸리는 작업입니다. 데이터를 불러오는 모든 화면은
**`loading` / `pending` / `error` / `empty`** 를 반드시 처리합니다. 하나라도 빠지면 미완성으로 봅니다.

`pending`은 백엔드가 202로 접수했지만 아직 전사가 끝나지 않은 상태입니다.
**이 상태의 UI가 있어야 "비동기 파이프라인을 설계했다"는 주장이 화면으로 증명됩니다.**

```js
// src/composables/useTranscriptionJob.js
import { ref, onUnmounted } from 'vue'
import { requestTranscription, fetchTranscription } from '@/api/transcriptions'

const POLL_INTERVAL = 2000

export function useTranscriptionJob() {
  const status = ref('idle')   // idle | uploading | pending | completed | failed
  const text = ref('')
  const error = ref(null)
  let timer = null

  const stop = () => { clearTimeout(timer); timer = null }

  const poll = async (jobId) => {
    try {
      const job = await fetchTranscription(jobId)
      status.value = job.status
      if (job.status === 'completed') { text.value = job.text; return }
      if (job.status === 'failed') { error.value = job.errorMessage; return }
      timer = setTimeout(() => poll(jobId), POLL_INTERVAL)   // pending 이면 계속 확인
    } catch (e) {
      status.value = 'failed'
      error.value = e.message
    }
  }

  const start = async (file) => {
    stop()
    status.value = 'uploading'
    error.value = null
    text.value = ''
    try {
      const job = await requestTranscription(file)
      status.value = 'pending'
      poll(job.jobId)
    } catch (e) {
      status.value = 'failed'
      error.value = e.message
    }
  }

  onUnmounted(stop)
  return { status, text, error, start, stop }
}
```

```vue
<template>
  <input type="file" accept="audio/*" @change="onSelect" />

  <p v-if="status === 'uploading'">음성 파일을 업로드하고 있습니다…</p>
  <p v-else-if="status === 'pending'">전사를 진행하고 있습니다. 잠시만 기다려 주세요…</p>
  <div v-else-if="status === 'failed'">
    <p>{{ error }}</p>
    <button @click="retry">다시 시도</button>
  </div>
  <p v-else-if="status === 'completed' && !text">전사된 내용이 없습니다.</p>
  <pre v-else-if="status === 'completed'">{{ text }}</pre>
</template>
```

**폴링은 컴포넌트가 사라질 때 반드시 멈춥니다** (`onUnmounted`). 안 그러면 요청이 계속 쌓입니다.

---

## 5. 파일 업로드 규칙

- `accept="audio/*"` 를 지정하고, 전송 전에 확장자·용량을 프론트에서도 검사한다
  - 허용: `mp3`, `m4a`, `wav`, `webm` / 최대 10MB
- 프론트 검사는 편의일 뿐이다. **서버 검증이 진짜다** — 프론트에서 막았다고 서버 검증을 빼지 않는다
- 용량 초과 시 서버는 `413`, 형식 오류는 `422`를 준다. 두 경우 모두 `message`를 그대로 노출한다
- 업로드한 파일을 `localStorage`나 전역 변수에 보관하지 않는다

---

## 6. 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 컴포넌트 파일 | PascalCase, 두 단어 이상 | `TranscriptionResultCard.vue` |
| 뷰(페이지) 파일 | PascalCase + `View` | `TranscriptionUploadView.vue` |
| composable | `use` 접두어 | `useTranscriptionJob.js` |
| API 함수 | `fetch` / `create` / `request` / `update` / `remove` + 리소스 | `fetchTranscription` |
| 변수·함수 | camelCase | `isUploading` |
| 상수 | UPPER_SNAKE | `POLL_INTERVAL` |

응답 객체의 필드명은 **서버가 준 camelCase를 그대로** 씁니다. FE에서 이름을 바꾸지 마세요.

---

## 7. 스타일

- `<style scoped>` 사용. 전역 스타일은 `assets/`의 공통 파일에만.
- 인라인 `style` 속성 금지. 색·간격은 CSS 변수로 뺀다.
- UI 프레임워크는 팀 합의 없이 추가하지 않는다.

---

## 8. 하지 말 것

- ❌ 컴포넌트 안에 더미 배열·가짜 전사 텍스트를 만들어 렌더링 — **반드시 HTTP로 받는다**
- ❌ `pending` 상태를 건너뛰고 바로 결과를 보여주기 (비동기 설계가 화면에서 증명되지 않는다)
- ❌ 폴링 타이머를 정리하지 않고 화면 이동
- ❌ `localStorage`에 서비스 데이터를 저장 (DB가 저장소다)
- ❌ 컴포넌트에서 `axios` 직접 import
- ❌ `console.log` 를 남긴 채 커밋
- ❌ 요청받지 않은 화면·라우트 추가
- ❌ API 명세에 없는 필드를 임의로 가정

---

## 9. 커밋 예시

```
feat(fe): 음성 파일 업로드 화면 구현
feat(fe): 전사 상태 폴링 composable 추가 (pending → completed)
feat(fe): 전사 결과 표시 카드 컴포넌트 추가
fix(fe): 화면 이동 시 폴링 타이머가 정리되지 않던 문제 수정
fix(fe): multipart 요청에 Content-Type 이 강제되어 boundary 가 누락되던 문제 수정
chore(fe): axios 인터셉터에 공통 에러 포맷 매핑 추가
```
