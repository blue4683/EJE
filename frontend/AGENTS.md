# frontend/AGENTS.md

Vue 3 + Vite 프론트엔드 규칙입니다. 루트 `AGENTS.md`를 먼저 읽고 이 파일을 적용하세요.

---

## 1. 기술 스택

- Vue 3 (**Composition API + `<script setup>` 고정**, Options API 금지)
- Vite
- axios
- 라우팅: vue-router
- 상태관리: **Pinia 도입 확정.** 세션 토큰과 `plan`을 **라우터 가드와 axios 인터셉터가 동시에**
  읽어야 하므로 컴포넌트 트리 밖의 저장소가 필요합니다. 스토어는 `session`·`submission` 둘뿐이며,
  화면 지역 상태를 스토어로 올리지 않습니다.
- **차트·UI 프레임워크는 넣지 않습니다.** 추이 차트는 SVG, 음성 파형은 Canvas로 직접 그립니다 (§7).

---

## 2. 폴더 구조

```
frontend/src/
├── api/
│   ├── client.js              axios 인스턴스 (HTTP 설정은 여기 한 곳, 동결)
│   ├── errorCodes.js          오류 코드별 화면 행동 정책
│   └── <도메인>.js             auth · users · history · stats · submission · analysisStatus · proAnalysis · mock
├── stores/                    session.js · submission.js (Pinia)
├── composables/               useRecorder.js 같은 로직 재사용
├── components/
│   ├── common/                StateBlock · PageHeader (화면 상태 4종)
│   ├── layout/                AppHeader · PlanBadge
│   ├── chart/                 SVG 추이 차트
│   ├── waveform/              Canvas 음성 파형·타임라인
│   ├── recorder/              녹음 버튼·레벨미터·타이머
│   └── <도메인>/               auth · account · history · stats · practice
├── views/                     라우트에 1:1 대응하는 페이지
│   └── auth/ account/ dashboard/ stats/ practice/ dev/
├── constants/audio.js         허용 MIME·용량·길이·폴링 간격
├── utils/                     format.js · audio.js · validators.js · idempotency.js
├── styles/tokens.css          CSS 변수 (색·간격·반경)
├── router/index.js            라우트 전체를 여기서 선언 (동결)
├── App.vue
└── main.js
```

새 파일은 반드시 위 위치에 만듭니다. 최상위에 파일을 흩뿌리지 마세요.

**`api/client.js`·`router/index.js`·`stores/session.js`·`components/common/`은 공용이라 동결입니다.**
확장이 필요하면 고치지 말고 **자기 소유의 새 파일**을 만듭니다.
담당자별 파일 소유권은 `docs/frontend/README.md` §5 를 따릅니다.

---

## 3. HTTP 호출 — 가장 중요한 규칙

### 3.1 컴포넌트에서 `axios`나 `fetch`를 직접 부르지 않는다

모든 호출은 `src/api/` 안의 함수를 거칩니다. 이 규칙이 깨지면
"Mock에서 실제 백엔드로 코드 수정 없이 전환"이라는 이 프로젝트의 핵심 주장이 무너집니다.

```js
// src/api/client.js
import axios from 'axios'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // 절대 하드코딩하지 않는다 (/api/v1 까지 포함)
  timeout: 20000,
  withCredentials: true,   // Refresh 쿠키(HttpOnly)를 함께 보낸다
  headers: { 'Content-Type': 'application/json' },
})

// envelope { success, data, error } 를 { code, message, status } 로 납작하게 만든다
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const body = err.response?.data
    if (body?.error?.code) {
      return Promise.reject({
        code: body.error.code,          // ← data.code 가 아니다. 한 단계 더 들어간다
        message: body.error.message,
        status: err.response.status,
      })
    }
    return Promise.reject({
      code: 'NETWORK_ERROR',
      message: '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      status: err.response?.status ?? 0,
    })
  },
)

// 성공 응답에서 data 만 꺼낸다. 모든 api/*.js 가 이 함수를 통과한다.
// 204(본문 없음)는 null 이 된다.
export const unwrap = (promise) => promise.then((res) => res.data?.data ?? null)
```

**401 재발급은 여기 한 곳에서만 처리합니다.** 화면마다 재시도를 짜면 재발급 요청이 폭주하고,
늦게 도착한 응답이 새 토큰을 덮어씁니다. 진행 중인 재발급 Promise를 공유하는
**단일 비행 훅**을 두고, 끝나면 원요청을 재시도합니다.

**`accessToken`을 `localStorage`·`sessionStorage`에 두지 않습니다.** Pinia 메모리에만 둡니다.
명세가 Refresh를 HttpOnly 쿠키로 둔 이유가 사라지고, XSS 한 번에 세션이 통째로 넘어갑니다.
새로고침 복구는 앱 부팅 시 `POST /auth/reissue` → `GET /users/me` 2단계로 합니다.

```js
// src/api/submission.js
import { client, unwrap } from './client'

// 녹음 제출 → 202 { recordingId, analysisId, status, attemptNo, autoRetryCount }
export const submitRecording = (blob, idempotencyKey, onProgress) => {
  const form = new FormData()
  form.append('audio', blob, 'recording.webm')   // 필드명은 'audio' 하나뿐이다
  return unwrap(client.post('/recordings', form, {
    headers: {
      // multipart 는 Content-Type 을 브라우저가 boundary 와 함께 붙이게 둔다.
      // client 기본값이 application/json 이라 지우지 않으면 서버가 파일을 못 찾는다.
      'Content-Type': undefined,
      'Idempotency-Key': idempotencyKey,          // UUID, 필수
    },
    timeout: 60000,        // 업로드 + 서버 디코딩까지 기다린다
    onUploadProgress: onProgress,
  }))
}
```

```js
// src/api/analysisStatus.js
import { client, unwrap } from './client'

// 상태 조회 → { status, attemptNo, failureCode, retryable, startedAt, finishedAt, ... }
export const fetchStatusByAnalysis = (analysisId) =>
  unwrap(client.get(`/analyses/${analysisId}/status`))
```

- **`baseURL`에 `/api/v1`이 들어 있습니다.** 그래서 `client.post('/recordings')` 가 맞습니다.
  `'/api/v1/recordings'` 라고 쓰면 경로가 두 번 붙습니다.
- **`Idempotency-Key`는 한 번의 제출 시도에 하나**입니다. 네트워크 재전송에는 같은 값을 유지하고,
  성공한 뒤에는 버립니다. 매번 새로 만들면 같은 녹음이 두 번 제출됩니다.
- **클라이언트가 잰 길이(`durationMs`)를 보내지 않습니다.** 서버가 직접 디코딩해서 잽니다.
- **ID는 전부 문자열**입니다. `Number()`로 바꾸거나 `==`로 비교하지 마세요.

### 3.2 `baseURL`은 `.env`에서만 온다

```
# frontend/.env.example
VITE_API_BASE_URL=
```

```
# Mock 단계 — Mock 응답도 envelope { success, data, error } 형태여야 한다
VITE_API_BASE_URL=https://xxxx.mock.pstmn.io/api/v1
# 실제 백엔드 전환 — 이 한 줄만 바꾼다
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

**전환 시 소스 코드는 단 한 줄도 바뀌지 않아야 합니다.** 발표에서 실제로 보여줄 부분입니다.
`context-path`까지 포함한 **끝까지의 경로**를 넣습니다.

### 3.3 Vite dev proxy를 쓰지 않는다

`vite.config.js`에 `server.proxy`를 설정하면 요청 `Origin`이 바뀌거나 사라져서
**`/auth/*` 4개가 전부 `403 ORIGIN_NOT_ALLOWED`** 가 됩니다.
"로그인만 안 되는" 형태로 나타나 원인을 찾기 어렵습니다.

프록시 대신 `VITE_API_BASE_URL` 직결 + 백엔드 `APP_ORIGIN=http://localhost:5173` 을 씁니다.
같은 이유로 **`server.strictPort: true`** 를 켭니다 — 5173이 점유되면 Vite가 조용히 5174로 옮겨 가고,
그러면 Origin이 달라져 똑같은 403이 납니다.

---

## 4. 화면 상태 4종은 필수

분석은 시간이 걸리는 작업입니다. 데이터를 불러오는 모든 화면은
**`loading` / `pending` / `error` / `empty`** 를 반드시 처리합니다. 하나라도 빠지면 미완성으로 봅니다.

`pending`은 백엔드가 202로 접수했지만 아직 분석이 끝나지 않은 상태입니다.
**이 상태의 UI가 있어야 "비동기 파이프라인을 설계했다"는 주장이 화면으로 증명됩니다.**

네 상태는 **`components/common/StateBlock.vue` 한 곳에 고정**합니다.
화면마다 로딩·빈 목록 문구를 새로 만들지 말고 이 컴포넌트를 쓰세요.

```js
// src/composables/useAnalysisStatus.js
import { ref, onUnmounted } from 'vue'
import { STATUS_POLL_INTERVAL_MS } from '@/constants/audio'   // 1000

const TERMINAL = ['COMPLETED', 'FAILED']
const HIDDEN_INTERVAL_MS = 5000        // 탭이 안 보이면 느리게

export function useAnalysisStatus(fetcher) {
  const data = ref(null)
  const state = ref('loading')         // loading | pending | ready | error
  const error = ref(null)
  let timer = null
  let stopped = false

  const clear = () => { clearTimeout(timer); timer = null }
  const stop = () => { stopped = true; clear() }

  const schedule = () => {
    if (stopped) return
    clear()
    timer = setTimeout(tick, document.hidden ? HIDDEN_INTERVAL_MS : STATUS_POLL_INTERVAL_MS)
  }

  async function tick() {
    if (stopped) return
    try {
      const d = await fetcher()
      data.value = d
      error.value = null
      if (TERMINAL.includes(d.status)) {
        state.value = 'ready'          // FAILED 도 정상 종료다. 예외로 던지지 않는다.
        clear()
        return
      }
      state.value = 'pending'          // PENDING / PROCESSING
      schedule()
    } catch (e) {
      error.value = e
      if (e.status === 404) { state.value = 'error'; clear(); return }
      // 일시적 네트워크 오류로 폴링을 멈추지 않는다. 다음 차례에 다시 시도한다.
      state.value = data.value ? 'pending' : 'error'
      schedule()
    }
  }

  onUnmounted(stop)
  return { data, state, error, start: () => { stopped = false; tick() }, stop }
}
```

```vue
<template>
  <StateBlock :state="state" :message="error?.message" @retry="start">
    <!-- ready 일 때만 슬롯이 렌더된다 -->
    <AnalysisResult :data="data" />
  </StateBlock>
</template>
```

**분석 실패(`status === 'FAILED'`)를 예외로 던지지 마세요.** HTTP 200입니다.
예외로 만들면 폴링이 `catch`로 빠져 정상 흐름이 오류 화면으로 뒤집힙니다.

**폴링은 컴포넌트가 사라질 때 반드시 멈춥니다** (`onUnmounted`). 안 그러면 요청이 계속 쌓이고,
화면에 다시 들어올 때마다 타이머가 누적됩니다. 등록한 이벤트 리스너도 같이 정리하세요.

**`COMPLETED`가 되어도 자동으로 결과 화면으로 넘기지 마세요.** 사용자가 상태 전이를 못 보고
지나갑니다 — 그게 이 프로젝트가 화면으로 증명하려는 것입니다. "결과 보기" 버튼을 띄웁니다.
---

## 5. 음성 업로드 규칙

허용 조건은 ERD 제약(`ck_recordings_mime`)과 **정확히 일치**해야 합니다.
`src/constants/audio.js` 한 곳에 두고 거기서만 읽습니다.

| 항목 | 값 |
| --- | --- |
| 허용 MIME | `audio/webm` · `audio/mp4` · `audio/ogg` · `audio/wav` · `audio/mpeg` (5종) |
| 최대 용량 | **16MiB** (`16 * 1024 * 1024`) |
| 길이 | **1초 이상 60초 이하** (1000~60000ms) |
| multipart 필드 | `audio` **하나뿐** |

- **`accept="audio/*"` 를 쓰지 않습니다.** 서버가 안 받는 형식(`audio/aac` 등)까지 열립니다.
  허용 5종을 그대로 나열하세요.
- MIME 비교는 **`;` 앞만** 봅니다. `audio/webm;codecs=opus` → `audio/webm`.
- 프론트 검사는 편의일 뿐이다. **서버 검증이 진짜다** — 프론트에서 막았다고 서버 검증을 빼지 않는다
- 오류 코드를 구분합니다. 네 경우 모두 `error.message`를 그대로 노출합니다.

| 상황 | 코드 |
| --- | --- |
| 형식이 허용 목록에 없음 | `415 UNSUPPORTED_MEDIA_TYPE` |
| 16MiB 초과 | `413 FILE_TOO_LARGE` |
| 형식은 맞는데 손상·0바이트·디코딩 실패 | `422 INVALID_AUDIO` |
| 1초 미만 또는 60초 초과 | `422 AUDIO_DURATION_OUT_OF_RANGE` |

- **60초 초과를 잘라내지 않습니다.** 명세가 거절이지 절단이 아닙니다. 녹음은 60초에서 자동 정지시킵니다.
- **클라이언트가 잰 길이를 서버로 보내지 않습니다.** 서버가 직접 디코딩해서 잽니다.
  `MediaRecorder`로 만든 webm은 `duration`이 `Infinity`로 나오는 브라우저 버그가 있어서,
  **길이를 못 재는 것은 오류가 아닙니다.** 막지 말고 서버 판정에 맡기세요.
- 업로드한 파일을 `localStorage`나 전역 변수에 보관하지 않는다.
  재시도용 원본 `Blob`은 **Pinia 메모리에만** 둡니다 — 새로고침으로 사라지면 다시 녹음하도록 안내합니다.
---

## 6. 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 컴포넌트 파일 | PascalCase, 두 단어 이상 | `BasicResultCard.vue` |
| 뷰(페이지) 파일 | PascalCase + `View` | `AnalysisProgressView.vue` |
| composable | `use` 접두어 | `useAnalysisStatus.js` |
| API 함수 | `fetch` / `submit` / `remove` + 리소스 | `fetchRecordingDetail` · `submitRecording` |
| Pinia 스토어 | `use` + 도메인 + `Store` | `useSessionStore` |
| 변수·함수 | camelCase | `isUploading` |
| 상수 | UPPER_SNAKE | `STATUS_POLL_INTERVAL_MS` |

응답 객체의 필드명은 **서버가 준 camelCase를 그대로** 씁니다. FE에서 이름을 바꾸지 마세요.

---

## 7. 스타일

- `<style scoped>` 사용. 전역 스타일은 `styles/tokens.css` 한 곳에만.
- 인라인 `style` 속성으로 **색·간격을 박지 않는다.** 전부 `tokens.css`의 CSS 변수에서 온다.
  - 예외: 파형 마커 위치처럼 **값이 런타임에 계산되는 동적 레이아웃**은 `:style` 바인딩이 맞다.
- Canvas는 CSS를 못 받으므로 `getComputedStyle`로 CSS 변수를 읽어 씁니다. 색을 하드코딩하지 않는다.
- **UI·차트 라이브러리를 팀 합의 없이 추가하지 않는다.** 차트는 SVG, 파형은 Canvas로 직접 그린다.

---

## 8. 하지 말 것

- ❌ 컴포넌트 안에 더미 배열·가짜 분석 결과를 만들어 렌더링 — **반드시 HTTP로 받는다**
- ❌ `pending` 상태를 건너뛰고 바로 결과를 보여주기 (비동기 설계가 화면에서 증명되지 않는다)
- ❌ 폴링 타이머를 정리하지 않고 화면 이동
- ❌ `localStorage`·`sessionStorage`에 토큰이나 음성 파일을 저장
- ❌ 컴포넌트에서 `axios` 직접 import
- ❌ `null`을 `?? 0` / `|| 0`으로 채우기 — **미완료는 0이 아니다.** `—`로 표시한다
- ❌ 서버가 준 `pro.detailUrl`(API 경로)을 `href`에 그대로 넣기 — 라우터 `name`으로 이동한다
- ❌ 라우터 가드로 PRO 등급을 미리 차단 — 등급 판정은 서버 응답(`403 PRO_REQUIRED`)으로만 한다
- ❌ 서버가 이미 계산해 준 값(`retryable`, 정렬 순서, 파형 600점)을 프론트에서 다시 계산·정렬·샘플링
- ❌ `console.log` 를 남긴 채 커밋
- ❌ 요청받지 않은 화면·라우트 추가 (라우트는 `router/index.js`에 미리 전부 선언되어 있다)
- ❌ API 명세에 없는 필드를 임의로 가정

---

## 9. 커밋 예시

```
feat(fe): 녹음 제출 화면과 멱등 키 처리 구현
feat(fe): 분석 상태 폴링 composable 추가 (PENDING → PROCESSING → COMPLETED)
feat(fe): PRO 상세 분석 화면 구현
fix(fe): 화면 이동 시 폴링 타이머가 정리되지 않던 문제 수정
fix(fe): multipart 요청에 Content-Type 이 강제되어 boundary 가 누락되던 문제 수정
fix(fe): 미완료 기록의 추임새가 0회로 표시되던 문제 수정
chore(fe): axios 인터셉터에 envelope 언랩과 401 단일 비행 재발급 추가
```
