# 담당 A — 계정·이력·통계 (통합본)

이 문서는 `Step 0(공통 기반)`과 `A1~A8` 8개 작업을 **순서대로 이어 붙인 것**입니다.
이 파일 하나만 열면 담당 A의 모든 작업에 착수할 수 있습니다.

구성 순서: Step 0(공통 기반) → A1 → A2 → A3 → A4 → A5 → A6 → A7 → A8

> **선행 상태 요약** — 오늘 착수 가능한 것은 `Step 0`뿐입니다. A1 이후는 백엔드 API가 필요하고,
> A5 이후의 화면 검증은 `db/seed-dev.sql`이 필요합니다. 자세한 건 [`README.md`](README.md) §0.

---

# Step 0 — 공통 기반 (공동 작업, 딱 한 번)

우선순위 **P0** · 담당 **A + B 함께** · 선행 없음 · 예상 20~40분
브랜치 `feat/fe-foundation`

> 이 문서는 **붙여넣기 작업**입니다. 아래 소스를 그대로 옮기고 기동만 확인하면 됩니다.
> 여기서 만든 파일은 이후 **동결**됩니다. 확장이 필요하면 §6 규칙에 따라 자기 소유의 새 파일을 만듭니다.

---

## 1. 착수 (그대로 복사)

```bash
node -v                 # 20 이상. 아니면 여기서 멈추고 Node 를 맞춘다
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-foundation

docker compose down -v && docker compose up -d db backend

cd frontend && npm install
npm install pinia
```

---

## 2. 목표

명세 v3.0.0을 구현하기 위한 **공용 커널**을 만든다. 이후 A와 B는 이 커널만 공유하고
서로의 코드를 참조하지 않는다.

만드는 것: 빌드 설정 · 라우트 15개 사전 선언 · Pinia 세션 · axios envelope 언랩 ·
401 재발급 단일 비행 훅 · 에러 코드 27개 처리 정책 · 공통 상태 컴포넌트 · CSS 토큰 · 포맷 유틸.
그리고 **기존 전사 스캐폴딩 정리**(§4).

---

## 3. 명세 근거

- 《아키텍처·공통 HTTP 규약》 → envelope `{ success, data, error }`, `Cache-Control: no-store`
- 《JWT 인증 계약 — 현재 구현》 → Access는 `Authorization: Bearer`, Refresh는 HttpOnly 쿠키
- 《공통 오류 코드》 표 → 27개
- 루트 `AGENTS.md` §5.2 → JSON은 camelCase, 시간은 ISO 8601 UTC
- `frontend/AGENTS.md` §3 → HTTP 호출은 `src/api/` 를 반드시 거친다

---

## 4. 소유 파일

**A가 붙여넣기**

```
frontend/package.json                        pinia 추가
frontend/vite.config.js                      alias, strictPort
frontend/.env.example                        키만 추가, 값은 비운다
frontend/src/main.js
frontend/src/router/index.js                 라우트 15개 사전 선언 + 가드
frontend/src/stores/session.js
frontend/src/api/client.js
frontend/src/api/errorCodes.js
frontend/src/api/auth.js                     Step 0 이후 A 단독 소유
frontend/src/App.vue
frontend/src/components/layout/AppHeader.vue Step 0 이후 A 단독 소유 (여기선 뼈대만)
```

**B가 붙여넣기**

```
frontend/src/styles/tokens.css
frontend/src/utils/format.js
frontend/src/components/common/StateBlock.vue
frontend/src/components/common/PageHeader.vue
frontend/src/constants/audio.js              Step 0 이후 B 단독 소유
frontend/src/views/NotFoundView.vue
frontend/src/views/NotReadyView.vue          아직 안 만든 화면의 자리 표시
```

두 사람의 파일 집합이 겹치지 않으므로 **동시에 붙여넣고 같은 PR에 올려도 됩니다.**

### 4-1. 정리 대상 — 기존 전사(transcription) 스캐폴딩

현재 `frontend/src`에는 v3.0.0 이전의 전사 스캐폴딩이 남아 있습니다. **Step 0에서 함께 정리합니다.**
그대로 두면 죽은 코드가 남고, `main.js`의 `import './style.css'`가 깨집니다.

| 대상 | 조치 | 붙여넣는 사람 |
| --- | --- | --- |
| `src/api/transcriptions.js` | **삭제** — v3.0.0에 대응 API가 없다 | A |
| `src/views/HomeView.vue` | **삭제** — `views/dashboard/HomeView.vue`(A5)가 대체한다 | A |
| `src/style.css` | **삭제** → `src/styles/tokens.css`가 대체 (§5-12) | B |
| `src/api/client.js` | 전면 교체 (§5-5) — 현재는 flat 에러 매핑이다 | A |
| `src/router/index.js` | 전면 교체 (§5-7) — 현재는 `HomeView`를 정적 import 한다 | A |
| `src/main.js` | 전면 교체 (§5-8) — import 경로가 `./styles/tokens.css`로 바뀐다 | A |
| `src/App.vue` | 전면 교체 (§5-10) | A |

```bash
git rm frontend/src/api/transcriptions.js frontend/src/views/HomeView.vue frontend/src/style.css
```

**삭제를 미루지 마세요.** `router/index.js`가 `HomeView`를 정적 import 하고 있어서,
교체하지 않으면 라우트 15개 선언이 통째로 무시됩니다.

---

## 5. 구현 지침 — 소스

### 5-1. `frontend/package.json`

`pinia` 하나만 추가합니다. **차트 라이브러리·UI 프레임워크를 넣지 않습니다**
(`frontend/AGENTS.md` §7 — 팀 합의 없이 추가하지 않는다). 차트와 파형은 SVG·Canvas로 직접 그립니다.

```json
{
  "name": "frontend",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.20.0",
    "pinia": "^3.0.0",
    "vue": "^3.5.41",
    "vue-router": "^4.6.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.8",
    "vite": "^8.2.2"
  }
}
```

### 5-2. `frontend/vite.config.js`

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    // FC2 — 5173 이 점유되면 Vite 는 조용히 5174 로 옮겨 간다.
    // 그러면 Origin 이 백엔드의 APP_ORIGIN 과 달라져 /auth/* 4개가 전부 403 이 된다.
    strictPort: true,
  },
})
```

> **dev proxy 를 설정하지 않습니다.** `server.proxy` 를 쓰면 요청 Origin 이 바뀌거나 빠져서
> 로그인·회원가입·재발급·로그아웃이 `403 ORIGIN_NOT_ALLOWED` 로 막힙니다(FC2).

### 5-3. `frontend/.env.example` — 키만, 값은 비웁니다

```
VITE_API_BASE_URL=
```

값은 `.env.local` 에 각자 넣습니다. **`context-path` 까지 포함한 경로**입니다.

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

백엔드 `.env` 의 `APP_ORIGIN` 이 `http://localhost:5173` 인지 반드시 함께 확인합니다.

### 5-4. `src/stores/session.js`

```js
import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
  state: () => ({
    // FC1 — accessToken 은 메모리에만 둔다. localStorage·sessionStorage 에 쓰지 않는다.
    // 새로고침하면 사라지고, HttpOnly Refresh 쿠키로 POST /auth/reissue 해서 되살린다.
    accessToken: null,
    // 명세 DTO User 그대로. { id, email, name, profileImageUrl, plan, createdAt }
    user: null,
    // 새로고침 복구가 끝났는가. 끝나기 전에 라우팅하면 로그인 화면이 한 번 깜빡인다.
    bootstrapped: false,
  }),

  getters: {
    isAuthenticated: (s) => Boolean(s.accessToken && s.user),
    // 등급 판단의 유일한 출처는 서버가 준 users.plan 이다. JWT 를 파싱하지 않는다.
    isPro: (s) => s.user?.plan === 'PRO',
  },

  actions: {
    setSession({ accessToken, user }) {
      this.accessToken = accessToken
      this.user = user
    },
    setAccessToken(token) { this.accessToken = token },
    setUser(user) { this.user = user },
    clear() { this.accessToken = null; this.user = null },
    markBootstrapped() { this.bootstrapped = true },
  },
})
```

### 5-5. `src/api/client.js` — **동결. 여기가 HTTP 설정의 유일한 자리**

```js
import axios from 'axios'
import { useSessionStore } from '@/stores/session'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,  // 절대 하드코딩하지 않는다
  timeout: 20000,
  withCredentials: true,   // Refresh 쿠키(HttpOnly)를 함께 보낸다
  headers: { 'Content-Type': 'application/json' },
})

/** 재발급을 시도하지 않는 경로. 여기서 401 이 나면 그대로 실패다(무한 루프 방지). */
const NO_REISSUE = ['/auth/login', '/auth/signup', '/auth/reissue', '/auth/logout']
const isNoReissue = (url = '') => NO_REISSUE.some((p) => url.startsWith(p))

/** 세션이 끝났을 때 무엇을 할지는 main.js 가 주입한다(라우터와의 순환 import 회피). */
let sessionExpiredHandler = () => {}
export const onSessionExpired = (fn) => { sessionExpiredHandler = fn }

client.interceptors.request.use((config) => {
  const session = useSessionStore()
  if (session.accessToken && !isNoReissue(config.url)) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  return config
})

// FC3 — 단일 비행. 동시에 401 이 여러 개 떠도 reissue 는 한 번만 나간다.
let reissuing = null
const reissueOnce = () => {
  if (!reissuing) {
    reissuing = client
      .post('/auth/reissue')
      .then((res) => res.data.data.accessToken)
      .finally(() => { reissuing = null })
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
        const token = await reissueOnce()
        useSessionStore().setAccessToken(token)
        return client(config)          // 원요청 재시도
      } catch {
        useSessionStore().clear()
        sessionExpiredHandler()
      }
    }
    return Promise.reject(toApiError(err))
  },
)

/**
 * 명세의 envelope 를 { code, message, status } 하나로 납작하게 만든다.
 * 화면은 error.code 로 분기하고 error.message 를 그대로 노출한다 (AGENTS.md §5.4).
 * FE 가 별도 문구를 만들지 않는다.
 */
function toApiError(err) {
  const body = err.response?.data
  if (body?.error?.code) {
    return { code: body.error.code, message: body.error.message, status: err.response.status }
  }
  if (err.code === 'ECONNABORTED') {
    return { code: 'REQUEST_TIMEOUT', message: '요청 시간이 초과되었습니다. 다시 시도해 주세요.', status: 0 }
  }
  return {
    code: 'NETWORK_ERROR',
    message: '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    status: err.response?.status ?? 0,
  }
}

/**
 * 성공 응답에서 data 만 꺼낸다. 모든 api/*.js 는 이 함수를 통과한다.
 * 204(본문 없음)는 null 이 된다.
 */
export const unwrap = (promise) => promise.then((res) => res.data?.data ?? null)
```

### 5-6. `src/api/errorCodes.js` — 27개 처리 정책

`message` 는 서버가 준 한국어 문장을 그대로 씁니다. 이 파일이 정하는 것은 **문구가 아니라 행동**입니다.

```js
/**
 * 오류 코드별로 화면이 무엇을 해야 하는가.
 *   TOAST       그 자리에 메시지만 띄운다
 *   INLINE      폼 필드 옆에 붙인다
 *   LOGIN       세션을 비우고 /login 으로 보낸다
 *   UPGRADE     /upgrade 로 유도한다
 *   BACK        목록으로 돌려보낸다 (리소스가 없다)
 *   RETRY_NOW   같은 자리에서 "다시 시도" 버튼
 *   RETRY_LATER "잠시 후 다시" 안내 + 재시도 버튼
 *   FATAL       설정 문제. 개발자에게 보여야 한다
 */
export const ERROR_ACTION = {
  VALIDATION_ERROR: 'INLINE',
  UNAUTHORIZED: 'LOGIN',
  INVALID_CREDENTIALS: 'INLINE',
  INVALID_REFRESH_TOKEN: 'LOGIN',
  INVALID_PASSWORD: 'INLINE',
  EMAIL_ALREADY_EXISTS: 'INLINE',
  ORIGIN_NOT_ALLOWED: 'FATAL',
  RESOURCE_NOT_FOUND: 'BACK',
  RESOURCE_GONE: 'BACK',
  PRO_REQUIRED: 'UPGRADE',
  ANALYSIS_NOT_COMPLETED: 'TOAST',
  CANNOT_DELETE_WHILE_PROCESSING: 'TOAST',
  ANALYSIS_ALREADY_ACTIVE: 'TOAST',
  INVALID_ANALYSIS_STATE: 'TOAST',
  MANUAL_RETRY_LIMIT_EXCEEDED: 'TOAST',
  IDEMPOTENCY_KEY_CONFLICT: 'TOAST',
  AUDIO_MISMATCH: 'TOAST',
  AUDIO_DURATION_OUT_OF_RANGE: 'INLINE',
  INVALID_AUDIO: 'INLINE',
  UNSUPPORTED_MEDIA_TYPE: 'INLINE',
  FILE_TOO_LARGE: 'INLINE',
  REQUEST_TIMEOUT: 'RETRY_NOW',
  ANALYSIS_CAPACITY_EXCEEDED: 'RETRY_LATER',
  COMPARISON_TARGET_NOT_FOUND: 'TOAST',
  ANALYSIS_VERSION_MISMATCH: 'TOAST',
  SERVICE_UNAVAILABLE: 'RETRY_LATER',
  INTERNAL_ERROR: 'RETRY_NOW',
  NETWORK_ERROR: 'RETRY_NOW',   // 서버 코드가 아니라 client.js 가 만든 값
}

export const actionOf = (code) => ERROR_ACTION[code] ?? 'TOAST'
```

### 5-7. `src/router/index.js` — **라우트 15개를 미리 다 선언한다 (감사표 #1)**

**파일이 아직 없어도 앱이 정상 기동**해야 합니다. 그래야 두 사람이 자기 화면을
각자의 속도로 채워 넣을 수 있습니다. 이 분업 전체가 그 성질 위에 서 있습니다.

> **`() => import('@/views/X.vue')` 를 그대로 쓰면 안 됩니다.**
> Vite 8(rolldown)은 동적 import 를 **transform 시점에 즉시 해석**합니다.
> 화면 파일이 하나라도 없으면 이렇게 됩니다 — 실제로 확인한 결과입니다.
>
> ```
> Failed to resolve import "@/views/auth/LoginView.vue" from "src/router/index.js".
> Does the file exist?
> ```
>
> `router/index.js` **모듈 전체가 500** 이 되어 앱이 아예 뜨지 않습니다.
> 특정 라우트만 실패하는 게 아니라 첫 화면부터 빈 페이지입니다.
> (Vite 5·6 의 esbuild 는 관대했지만 8 부터 달라졌습니다. 예전 예제를 그대로 옮기면 여기서 막힙니다.)
>
> 그래서 **`import.meta.glob` 으로 존재하는 화면만 매핑**하고, 없는 화면은
> `NotReadyView` 로 떨어뜨립니다. 없는 파일을 가리키는 정적 경로가 코드에 남지 않습니다.
> 화면을 새로 만들면 glob 이 자동으로 잡으므로 **이 파일을 고칠 일이 영영 없습니다.**
>
> 단, glob 은 **매칭되는 파일을 전부 번들에 넣습니다.** `views/dev/**` 를 그대로 포함시키면
> Mock 콘솔이 프로덕션 번들에 남아 B7 의 "prod 에서 제외" 가 깨집니다(빌드해서 확인했습니다).
> 그래서 메인 glob 에서 `dev/` 를 빼고, `import.meta.env.DEV` 블록 안에 **별도 glob** 을 둡니다.

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '@/stores/session'

/**
 * 존재하는 화면만 맵으로 만든다. 없는 화면은 키가 없으므로 NotReadyView 로 떨어진다.
 * Vite 8 은 () => import('@/views/X.vue') 를 transform 시점에 해석해서,
 * 파일이 하나라도 없으면 이 모듈 전체가 500 이 되고 앱이 아예 뜨지 않는다.
 * 화면 파일을 새로 만들면 glob 이 자동으로 잡으므로 이 파일을 고칠 일이 없다.
 */
// dev/ 는 뺀다 — glob 은 매칭되는 파일을 전부 번들에 넣으므로,
// 여기 포함시키면 Mock 콘솔이 프로덕션 번들에 남는다 (실제로 확인함).
const views = import.meta.glob(['/src/views/**/*.vue', '!/src/views/dev/**'])
const view = (path) => views[`/src/views/${path}`] ?? (() => import('@/views/NotReadyView.vue'))

const routes = [
  // ── 공개 ────────────────────────────────────────────────────── A
  { path: '/login',  name: 'login',  meta: { public: true },
    component: view('auth/LoginView.vue') },
  { path: '/signup', name: 'signup', meta: { public: true },
    component: view('auth/SignUpView.vue') },

  // ── 계정·이력·통계 ──────────────────────────────────────────── A
  { path: '/', name: 'home',
    component: view('dashboard/HomeView.vue') },
  { path: '/recordings', name: 'recordingList',
    component: view('dashboard/RecordingListView.vue') },
  { path: '/recordings/:recordingId', name: 'recordingDetail', props: true,
    component: view('dashboard/RecordingDetailView.vue') },
  { path: '/recordings/:recordingId/compare', name: 'compare', props: true,
    meta: { proFeature: true },
    component: view('stats/CompareView.vue') },
  { path: '/trends', name: 'trends',
    component: view('stats/TrendsView.vue') },
  { path: '/weekly-report', name: 'weeklyReport', meta: { proFeature: true },
    component: view('stats/WeeklyReportView.vue') },
  { path: '/me', name: 'me',
    component: view('account/MyPageView.vue') },
  { path: '/upgrade', name: 'upgrade',
    component: view('account/UpgradeView.vue') },

  // ── 녹음·분석·PRO 상세 ─────────────────────────────────────── B
  { path: '/record', name: 'record',
    component: view('practice/RecordView.vue') },
  { path: '/analyses/:analysisId', name: 'analysisProgress', props: true,
    component: view('practice/AnalysisProgressView.vue') },
  { path: '/recordings/:recordingId/pro', name: 'proAnalysis', props: true,
    meta: { proFeature: true },
    component: view('practice/ProAnalysisView.vue') },
]

// 개발용 Mock 콘솔은 프로덕션 번들에 넣지 않는다 (명세: prod 에서 /mock/** 은 404).
// import.meta.env.DEV 가 false 로 치환되면 이 블록이 통째로 죽은 코드가 되어
// 안쪽 glob 과 그 청크까지 함께 사라진다. 빌드 후 grep 으로 확인할 것 (B7 §7).
if (import.meta.env.DEV) {
  const devViews = import.meta.glob('/src/views/dev/**/*.vue')
  routes.push({ path: '/dev/mock', name: 'devMock',
    component: devViews['/src/views/dev/MockConsoleView.vue']
      ?? (() => import('@/views/NotReadyView.vue')) })
}

routes.push({ path: '/:pathMatch(.*)*', name: 'notFound', meta: { public: true },
  component: view('NotFoundView.vue') })

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const session = useSessionStore()
  if (!session.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
```

> **`meta.proFeature` 로 막지 않습니다.** 등급 판정의 진짜 출처는 서버입니다.
> 가드가 미리 차단하면 등급이 방금 바뀐 사용자를 잘못 돌려보냅니다.
> 이 플래그는 **메뉴에 자물쇠 배지를 붙이는 용도**로만 씁니다. 화면은 `403 PRO_REQUIRED` 를
> 받아서 업그레이드 안내를 그립니다(§C9).

### 5-8. `src/main.js`

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { onSessionExpired } from '@/api/client'
import { restoreSession } from '@/api/auth'
import './styles/tokens.css'

const app = createApp(App)
app.use(createPinia())          // restoreSession 이 스토어를 쓰므로 반드시 먼저

// 401 이 재발급으로도 안 풀리면 로그인 화면으로 보낸다 (순환 import 회피용 주입)
onSessionExpired(() => {
  router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
})

// FC1 — 새로고침하면 accessToken 이 사라진다. HttpOnly Refresh 쿠키로 되살린다.
// mount 전에 끝내야 로그인 화면이 한 번 깜빡이지 않는다. 실패해도 그냥 진행한다.
await restoreSession()

app.use(router)
app.mount('#app')
```

### 5-9. `src/api/auth.js` — **Step 0 에선 뼈대만. 이후 A 단독 소유**

`restoreSession` 을 `main.js` 가 부르므로 Step 0 에 있어야 합니다. 내용은 A1 에서 채웁니다.

```js
import { client, unwrap } from './client'
import { useSessionStore } from '@/stores/session'

export const signUp = (payload) => unwrap(client.post('/auth/signup', payload))
export const logIn = (payload) => unwrap(client.post('/auth/login', payload))
export const reissue = () => unwrap(client.post('/auth/reissue'))
export const logOut = () => unwrap(client.post('/auth/logout'))

/** 앱 부팅 시 1회. 자세한 규칙은 A1 참조. */
export async function restoreSession() {
  const session = useSessionStore()
  try {
    // 백엔드가 죽어 있어도 앱이 20초 멈추지 않게 짧은 타임아웃을 준다
    const { accessToken } = await unwrap(client.post('/auth/reissue', null, { timeout: 3000 }))
    session.setAccessToken(accessToken)
    const me = await unwrap(client.get('/users/me'))
    session.setUser(me)
  } catch {
    session.clear()            // 비로그인으로 시작. 오류를 화면에 띄우지 않는다
  } finally {
    session.markBootstrapped()
  }
}
```

### 5-10. `src/App.vue`

```vue
<script setup>
import { useSessionStore } from '@/stores/session'
import AppHeader from '@/components/layout/AppHeader.vue'

const session = useSessionStore()
</script>

<template>
  <AppHeader v-if="session.isAuthenticated" />
  <main class="app-main">
    <router-view />
  </main>
</template>

<style scoped>
.app-main {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-4);
}
</style>
```

### 5-11. `src/components/layout/AppHeader.vue` — Step 0 엔 뼈대만, 이후 A 소유

```vue
<script setup>
import { useSessionStore } from '@/stores/session'
const session = useSessionStore()
</script>

<template>
  <header class="header">
    <router-link :to="{ name: 'home' }" class="brand">스피치 습관</router-link>
    <nav class="nav">
      <router-link :to="{ name: 'record' }">연습하기</router-link>
      <router-link :to="{ name: 'recordingList' }">기록</router-link>
      <router-link :to="{ name: 'trends' }">추이</router-link>
      <router-link :to="{ name: 'me' }">{{ session.user?.name }}</router-link>
    </nav>
  </header>
</template>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between;
  gap: var(--space-4); padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border); }
.nav { display: flex; gap: var(--space-4); }
</style>
```

### 5-12. `src/styles/tokens.css`

기존 `src/style.css` 를 대체합니다(§4-1). 삭제를 잊지 마세요.

```css
:root {
  --color-bg: #ffffff;
  --color-surface: #f7f8fa;
  --color-text: #1b1d21;
  --color-text-muted: #6b7280;
  --color-border: #e3e6ea;
  --color-primary: #2f6df6;
  --color-primary-weak: #e8f0ff;
  --color-danger: #d64545;
  --color-warning: #c77700;
  --color-success: #1f8f5f;
  --color-speech: #2f6df6;    /* 파형 SPEECH 구간 */
  --color-silence: #c9ced6;   /* 파형 SILENCE 구간 */

  --space-1: 4px;  --space-2: 8px;  --space-3: 12px;
  --space-4: 16px; --space-6: 24px; --space-8: 32px;

  --radius-1: 6px; --radius-2: 12px;
  --font-sans: system-ui, -apple-system, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif;
}

* { box-sizing: border-box; }
body { margin: 0; font-family: var(--font-sans); color: var(--color-text);
  background: var(--color-bg); }
button { font: inherit; cursor: pointer; }
```

인라인 `style` 속성은 쓰지 않습니다. 색·간격은 전부 이 변수에서 옵니다
(`frontend/AGENTS.md` §7).

### 5-13. `src/utils/format.js` — **FC5 가 사는 곳**

```js
const KST = 'Asia/Seoul'

const dateTimeFmt = new Intl.DateTimeFormat('ko-KR',
  { timeZone: KST, dateStyle: 'medium', timeStyle: 'short' })
const dateFmt = new Intl.DateTimeFormat('ko-KR',
  { timeZone: KST, dateStyle: 'medium' })

/** 서버는 UTC RFC3339 로 준다. 화면은 항상 Asia/Seoul 이다. */
export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '—')
export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '—')

/** YYYY-MM-DD 문자열(서버가 이미 KST 날짜로 준 것)은 Date 로 만들지 않는다 → 하루 밀린다 */
export const formatDayLabel = (ymd) => (ymd ? ymd.slice(5).replace('-', '/') : '—')

export const formatMs = (ms) => (ms == null ? '—' : `${(ms / 1000).toFixed(1)}초`)

/**
 * FC5 — 미완료는 null 이고, null 은 0회가 아니다.
 * `?? 0` 이나 `|| 0` 을 쓰지 않는다. 분석 중인 기록이 "추임새 0회"로 보이면 안 된다.
 */
export const formatCount = (n) => (n == null ? '—' : `${n.toLocaleString('ko-KR')}회`)
export const formatNumber = (n) => (n == null ? '—' : n.toLocaleString('ko-KR'))
export const formatPercent = (n) => (n == null ? '—' : `${n > 0 ? '+' : ''}${n}%`)

export const PLAN_LABEL = { FREE: '무료', PRO: 'PRO' }
export const STATUS_LABEL = {
  PENDING: '대기 중', PROCESSING: '분석 중', COMPLETED: '완료', FAILED: '실패',
}
```

### 5-14. `src/components/common/StateBlock.vue` — 화면 상태 4종

`frontend/AGENTS.md` §4 가 요구하는 `loading / pending / error / empty` 를 한 컴포넌트로 고정합니다.
**데이터를 불러오는 모든 화면이 이걸 씁니다.**

```vue
<script setup>
defineProps({
  // loading | pending | error | empty | ready
  state: { type: String, required: true },
  message: { type: String, default: '' },
  retryLabel: { type: String, default: '다시 시도' },
  canRetry: { type: Boolean, default: true },
})
defineEmits(['retry'])
</script>

<template>
  <div v-if="state === 'loading'" class="state" role="status" aria-live="polite">
    불러오는 중…
  </div>

  <!-- pending: 서버가 202 로 접수했지만 분석이 아직 안 끝난 상태. 이 UI 가 있어야
       "비동기 파이프라인을 설계했다"는 주장이 화면으로 증명된다 (AGENTS.md §4) -->
  <div v-else-if="state === 'pending'" class="state state--pending" role="status" aria-live="polite">
    <span class="spinner" aria-hidden="true" />
    <span>{{ message || '분석을 진행하고 있습니다. 잠시만 기다려 주세요…' }}</span>
  </div>

  <div v-else-if="state === 'error'" class="state state--error" role="alert">
    <p>{{ message }}</p>
    <button v-if="canRetry" type="button" @click="$emit('retry')">{{ retryLabel }}</button>
  </div>

  <div v-else-if="state === 'empty'" class="state state--empty">
    {{ message || '표시할 내용이 없습니다.' }}
  </div>

  <slot v-else />
</template>

<style scoped>
.state { display: flex; align-items: center; justify-content: center; gap: var(--space-3);
  flex-direction: column; padding: var(--space-8); color: var(--color-text-muted);
  background: var(--color-surface); border-radius: var(--radius-2); }
.state--error { color: var(--color-danger); }
.spinner { width: 18px; height: 18px; border: 2px solid var(--color-border);
  border-top-color: var(--color-primary); border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
```

### 5-15. `src/components/common/PageHeader.vue`

```vue
<script setup>
defineProps({ title: { type: String, required: true }, description: { type: String, default: '' } })
</script>

<template>
  <header class="page-header">
    <h1>{{ title }}</h1>
    <p v-if="description">{{ description }}</p>
    <slot name="actions" />
  </header>
</template>

<style scoped>
.page-header { margin-bottom: var(--space-6); }
.page-header h1 { margin: 0 0 var(--space-2); font-size: 1.5rem; }
.page-header p { margin: 0; color: var(--color-text-muted); }
</style>
```

### 5-16. `src/constants/audio.js` — Step 0 이후 B 단독 소유

ERD `ck_recordings_mime` 과 **정확히 일치**해야 합니다.

```js
export const ALLOWED_MIME = [
  'audio/webm', 'audio/mp4', 'audio/ogg', 'audio/wav', 'audio/mpeg',
]
export const MAX_FILE_BYTES = 16 * 1024 * 1024   // 16MiB
export const MIN_DURATION_MS = 1000
export const MAX_DURATION_MS = 60000
export const STATUS_POLL_INTERVAL_MS = 1000
```

### 5-17. `src/views/NotFoundView.vue`

```vue
<script setup></script>

<template>
  <div class="not-found">
    <h1>페이지를 찾을 수 없습니다.</h1>
    <router-link :to="{ name: 'home' }">홈으로 가기</router-link>
  </div>
</template>

<style scoped>
.not-found { padding: var(--space-8); text-align: center; }
</style>
```

### 5-18. `src/views/NotReadyView.vue` — 아직 안 만든 화면의 자리

§5-7 의 `view()` 가 없는 화면을 여기로 떨어뜨립니다. **이 파일 하나가 "상대를 기다리지 않는다"를
성립시킵니다.** 없으면 화면 파일이 다 채워질 때까지 앱이 뜨지 않습니다.

```vue
<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <div class="not-ready">
    <h1>준비 중인 화면입니다.</h1>
    <p>이 경로(<code>{{ route.path }}</code>)의 화면은 아직 만들어지지 않았습니다.</p>
    <router-link :to="{ name: 'home' }">홈으로 가기</router-link>
  </div>
</template>

<style scoped>
.not-ready { padding: var(--space-8); text-align: center; color: var(--color-text-muted); }
</style>
```

`NotFoundView`(없는 주소)와 **다른 화면**입니다. 이건 "주소는 맞는데 아직 구현 전"입니다.
둘을 합치면 개발 중에 오타와 미구현을 구분할 수 없습니다.

---

## 6. 함정

- **FC1 — `localStorage` 에 토큰을 쓰지 마세요.** 명세가 Refresh 를 HttpOnly 쿠키로 둔 이유가
  사라집니다. 새로고침 복구는 `restoreSession()` 하나로 됩니다.
- **FC2 — `server.proxy` 를 설정하지 마세요.** 프록시를 거치면 Origin 이 바뀌어
  `/auth/*` 4개가 전부 `403 ORIGIN_NOT_ALLOWED` 가 됩니다. "로그인만 안 되는" 형태라
  원인을 찾는 데 한 시간이 갑니다. `strictPort: true` 도 같은 이유입니다.
- **FC3 — 401 재시도를 각자 짜지 마세요.** `client.js` 의 단일 비행 훅 하나뿐입니다.
- **`baseURL` 에 `/api/v1` 이 들어 있습니다.** 그래서 호출은 `client.post('/auth/login')` 이
  맞습니다. `'/api/v1/auth/login'` 이라고 쓰면 경로가 두 번 붙습니다.
- **`withCredentials: true` 를 빼면** Refresh 쿠키가 안 실려서 재발급이 항상 실패합니다.
- **`res.data.data` 입니다.** envelope 이라 한 단계 더 들어갑니다. `unwrap()` 을 반드시 거치세요.
- **204 응답에는 본문이 없습니다.** `unwrap()` 이 `null` 을 돌려줍니다. 구조 분해하지 마세요.
- **라우트를 나중에 추가하지 마세요.** Step 0 의 15개가 전부입니다.
- **라우터에서 `() => import('@/views/X.vue')` 를 쓰지 마세요.** Vite 8 은 이걸 transform
  시점에 해석해서, 화면 파일이 하나라도 없으면 **`router/index.js` 모듈 전체가 500** 이 되고
  앱이 아예 뜨지 않습니다. `import.meta.glob` + `NotReadyView` 조합을 그대로 쓰세요 (§5-7).
  인터넷 예제는 대부분 Vite 5·6 기준이라 이 지점에서 막힙니다.
- **기존 전사 스캐폴딩 삭제를 빠뜨리지 마세요**(§4-1). `router/index.js` 가 `HomeView` 를
  정적 import 하고 있어서, 교체하지 않으면 라우트 선언이 통째로 무시됩니다.

---

## 7. 공통 규약 — 다른 절은 이 절을 **참조만** 합니다

### §C1 토큰 보관

- `accessToken` 은 **Pinia 메모리에만.** `localStorage`·`sessionStorage`·쿠키에 쓰지 않는다.
- Refresh 는 서버가 HttpOnly 쿠키로 관리한다. **JS 에서 읽을 수 없고, 읽으려 하지도 않는다.**
- 새로고침 복구는 `restoreSession()` 하나. 실패하면 조용히 비로그인으로 시작한다.

### §C2 응답 언랩

- 성공: `unwrap()` 이 `res.data.data` 를 준다.
- 실패: `{ code, message, status }` 로 reject 된다.
- 화면은 **`code` 로 분기하고 `message` 를 그대로 보여준다.** FE 에서 문구를 만들지 않는다
  (`AGENTS.md` §5.4).

### §C3 ID 는 전부 문자열

명세가 `id`·`recordingId`·`analysisId` 를 **문자열**로 줍니다.
`Number()` 로 바꾸거나 `==` 로 비교하지 마세요. 라우트 파라미터도 문자열이라 그대로 비교됩니다.

### §C4 시간·날짜

- 서버는 UTC RFC3339(`2026-09-03T03:05:00Z`), 화면은 **Asia/Seoul**.
- 변환은 `utils/format.js` 한 곳에서만. 화면에서 `new Date()` 로 직접 만들지 않는다.
- `trends`·`weekly-report` 가 주는 `date`·`weekStartDate` 는 **이미 KST 날짜 문자열**(`YYYY-MM-DD`)입니다.
  `new Date('2026-09-03')` 로 파싱하면 UTC 자정으로 읽혀 **하루 밀립니다.** `formatDayLabel()` 을 쓰세요.

### §C5 null 은 0이 아니다 (FC5)

| 필드 | `null` 의 뜻 |
| --- | --- |
| `basic` | 아직 분석이 안 끝났다 |
| `fillerTotalCount` | 미완료. **0회가 아니다** |
| `averageFillerCount` | 그날 연습을 안 했다. **평균 0이 아니다** |
| `wordsPerMinute` | 발화 시간이 0이었다 |
| `improvementRatePercent` | 비교 기준이 없다 |
| `startedAt` / `finishedAt` | 아직 시작/종료 전이다 |

전부 `formatCount()`·`formatNumber()` 를 거쳐 `—` 로 표시합니다.

### §C6 폴링 정리 (FC4)

- 폴링을 만드는 composable 은 **반드시 `onUnmounted` 에서 타이머를 정리**한다.
- 화면에 다시 들어왔을 때 이전 타이머가 살아 있으면 요청이 배로 늘어난다.

### §C7 화면 상태 4종

데이터를 불러오는 모든 화면은 `loading / pending / error / empty` 를 **전부** 처리합니다.
하나라도 빠지면 미완성입니다(`frontend/AGENTS.md` §4). `StateBlock.vue` 를 씁니다.

### §C8 404 는 403 이 아니다

서버는 남의 리소스와 없는 리소스를 **똑같이 404** 로 줍니다.
화면도 구분하지 말고 "요청한 정보를 찾을 수 없습니다" 로만 표시합니다.

### §C9 PRO 게이팅 — 서버가 진실

- 등급 판정은 **서버 응답**으로만 한다.
  - 목록·상세: `data.pro.locked` / `data.pro.available`
  - PRO 전용 API: `403 PRO_REQUIRED` 를 받으면 업그레이드 안내
- `session.isPro` 는 **메뉴에 자물쇠 배지를 붙이는 용도**로만 쓴다. 라우터 가드로 막지 않는다.
- `pro.detailUrl` 은 `/api/v1/...` **API 경로**다. 프론트 라우트(`/recordings/:id/pro`)와 다르다.
  **그대로 `href` 에 넣으면 안 됩니다.** "열 수 있다"는 신호로만 읽고, 이동은 라우터 `name` 으로 한다.

```vue
<!-- ❌ -->
<a :href="pro.detailUrl">자세히 보기</a>
<!-- ✅ -->
<router-link v-if="pro.available"
             :to="{ name: 'proAnalysis', params: { recordingId } }">자세히 보기</router-link>
```

### §C10 잠금 기능 9개 — 순서까지 명세대로

```
["waveform","silence","speed","timeline","segment","repetition","comparison","coaching","weeklyReport"]
```

서버가 준 배열을 **그대로 순서대로** 렌더합니다. 정렬하거나 재배치하지 않습니다.

---

## 8. 검증

```bash

docker compose down -v && docker compose up -d db backend
docker compose logs backend | grep -i "started"

cd frontend && npm run dev
# http://localhost:5173 로 접속
```

| 확인 | 기대 |
| --- | --- |
| 주소창에 `/` 입력 | 로그인 전이라면 `/login` 으로 이동 (아직 화면이 없으면 빈 화면 + 콘솔 import 오류 하나) |
| 주소창에 `/nope` | 404 화면 (`NotFoundView`) |
| 주소창에 `/login` (화면 파일 없을 때) | **"준비 중인 화면입니다"** (`NotReadyView`) — 앱이 죽지 않는다 |
| 터미널 기동 로그 | `Failed to resolve import` 나 `Failed to run dependency scan` 이 **없다** |
| `npm run build && grep -rl MockConsole dist/` | 아무 파일도 안 나온다 (dev glob 분리) |
| 브라우저 콘솔 | Vite 기동 오류·pinia 미설치 오류가 **없다** |
| 개발자도구 Network → 앱 로드 직후 | `POST /api/v1/auth/reissue` 가 **딱 한 번** 나가고, 401 이거나 실패해도 앱이 뜬다 |
| 터미널 | `Local: http://localhost:5173/` → **5174 가 아니다** (FC2) |
| `frontend/src` 검색 | `transcriptions`·`HomeView`·`style.css` 참조가 남아 있지 않다 (§4-1) |

```bash
# 백엔드 Origin 설정이 맞는지 (값을 출력하지 않고 존재만 확인)
grep -c '^APP_ORIGIN=' .env
```

---

## 9. 완료 기준 (DoD)

- [ ] `npm run dev` 가 **5173** 에서 돈다 (`strictPort`)
- [ ] `vite.config.js` 에 **`server.proxy` 가 없다** (FC2)
- [ ] `router/index.js` 에 라우트가 **15개**(+dev 1개) 선언돼 있다
- [ ] **화면 파일이 하나도 없는 상태에서 `npm run dev` 가 뜨고 `/` 가 200 이다** (glob + `NotReadyView`)
- [ ] 기동 로그에 `Failed to resolve import` 가 없다
- [ ] `npm run build` 후 `dist/` 에 `MockConsole` 청크가 **없다** (메인 glob 이 `dev/` 를 제외한다)
- [ ] `client.js` 에 `withCredentials: true` 와 **단일 비행 reissue 훅**이 있다 (FC3)
- [ ] `localStorage` / `sessionStorage` 를 쓰는 코드가 **한 줄도 없다** (FC1)
- [ ] `ERROR_ACTION` 이 명세 표와 **27개 정확히 일치**한다 (이름·행동)
- [ ] `formatCount(null)` 이 `'—'` 를 돌려준다. `?? 0` 이 어디에도 없다 (FC5)
- [ ] `StateBlock.vue` 가 `loading/pending/error/empty` 4종을 전부 갖는다
- [ ] `.env.example` 에 키만 있고 값이 비어 있다
- [ ] 컴포넌트에서 `axios` 를 직접 import 하는 코드가 없다
- [ ] **기존 전사 스캐폴딩 3개 파일이 삭제됐다** (§4-1)

확인 명령:

```bash
grep -rn "localStorage\|sessionStorage" frontend/src            # 출력이 비어야 한다
grep -rn "?? 0\|| 0" frontend/src --include=*.vue --include=*.js  # 검토 후 전부 제거
grep -rn "from 'axios'" frontend/src                            # api/client.js 한 줄만
ls frontend/src/api/transcriptions.js frontend/src/views/HomeView.vue 2>&1  # No such file
```

커밋 예시:

```
chore(fe): Pinia 도입과 빌드 설정 정리 (alias, strictPort)

포트가 5174 로 밀리면 Origin 이 달라져 /auth/* 4개가 403 이 된다.
dev proxy 대신 baseURL 직결을 쓰는 이유도 같다.

chore(fe): 전사 스캐폴딩 제거 (v3.0.0 에 대응 API 없음)
feat(fe): axios envelope 언랩과 401 재발급 단일 비행 훅 추가
feat(fe): 라우트 15개 사전 선언과 인증 가드 추가
feat(fe): 공통 상태 컴포넌트·에러 코드 27개 처리 정책·포맷 유틸 추가
```

---

# A1 — 인증 API와 세션 복구·라우터 가드

우선순위 **P0** · 담당 **A** · 선행 **Step 0** · 브랜치 `feat/fe-auth-base`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-auth-base

cd frontend && npm run dev
```

---

## 2. 목표

이 저장소 프론트 전체의 **인증 기반**을 만든다. 화면은 만들지 않는다.
A2 이후의 모든 작업과 담당 B 의 모든 화면이 이 위에 얹힙니다.

- `api/auth.js` 4개 함수 (01~04)
- **새로고침 세션 복구** — accessToken 이 메모리라서 새로고침하면 사라진다
- 로그인/로그아웃 시 세션 스토어를 채우고 비우는 단 하나의 경로
- 리다이렉트 복귀 (`?redirect=`)

---

## 3. 명세 근거

- 《공개 API 상세》 API 01~04
- 《JWT 인증 계약 — 현재 구현》 → 재발급·로그아웃 문단
- 오류: `UNAUTHORIZED` · `INVALID_REFRESH_TOKEN` · `ORIGIN_NOT_ALLOWED`

---

## 4. 소유 파일

```
src/api/auth.js                        Step 0 뼈대를 채운다
src/composables/useAuthActions.js      로그인·로그아웃 진입점 (화면이 이것만 부른다)
```

`src/api/client.js`·`stores/session.js`·`router/index.js` 는 Step 0 소유입니다. **읽기만** 합니다.

---

## 5. 구현 지침

### 5-1. `api/auth.js` — 4개 함수와 복구

Step 0 에 넣어 둔 뼈대에 주석과 규칙을 채웁니다.

| API | 호출 | 성공 | 응답 |
| --- | --- | --- | --- |
| 01 | `POST /auth/signup` | **201** | `LoginData` |
| 02 | `POST /auth/login` | 200 | `LoginData` |
| 03 | `POST /auth/reissue` | 200 | `AccessData` |
| 04 | `POST /auth/logout` | **204** | 본문 없음 |

```
LoginData  : accessToken, tokenType, expiresIn, refreshExpiresAt, user
AccessData : accessToken, tokenType, expiresIn        ← user 가 없다
```

- **`reissue` 는 요청 본문이 없고 `Authorization` 헤더도 필요 없습니다.** 쿠키만 갑니다.
- **`reissue` 응답에는 `user` 가 없습니다.** 그래서 복구는 `reissue` → `GET /users/me` **2단계**입니다.
- **`logout` 은 204 라 본문이 없습니다.** `unwrap()` 이 `null` 을 줍니다.

### 5-2. 세션 복구 — 앱 부팅 시 딱 한 번

```js
export async function restoreSession() {
  const session = useSessionStore()
  try {
    const { accessToken } = await unwrap(client.post('/auth/reissue', null, { timeout: 3000 }))
    session.setAccessToken(accessToken)
    session.setUser(await unwrap(client.get('/users/me')))
  } catch {
    // 쿠키가 없거나 만료됐다. 정상적인 "비로그인" 상태다.
    // 오류 화면을 띄우지 않는다 → 처음 방문한 사람에게 에러를 보여주면 안 된다.
    session.clear()
  } finally {
    session.markBootstrapped()
  }
}
```

- **`timeout: 3000`** — 백엔드가 죽어 있을 때 앱 전체가 20초 멈추는 걸 막습니다.
- **실패를 화면에 노출하지 않습니다.** 401 은 "로그인 안 됨"이지 오류가 아닙니다.
- `client.js` 의 401 인터셉터는 `/auth/reissue` 를 제외하므로 여기서 무한 루프가 되지 않습니다.

### 5-3. `composables/useAuthActions.js` — 화면이 부르는 유일한 진입점

```js
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { signUp, logIn, logOut } from '@/api/auth'
import { useSessionStore } from '@/stores/session'

export function useAuthActions() {
  const router = useRouter()
  const route = useRoute()
  const session = useSessionStore()
  const submitting = ref(false)
  const error = ref(null)

  /** 로그인·가입 성공 뒤 원래 가려던 곳으로 돌려보낸다 */
  const goAfterAuth = () => {
    const to = route.query.redirect
    router.replace(typeof to === 'string' && to.startsWith('/') ? to : { name: 'home' })
  }

  const run = async (fn) => {
    submitting.value = true
    error.value = null
    try {
      const data = await fn()
      session.setSession({ accessToken: data.accessToken, user: data.user })
      goAfterAuth()
    } catch (e) {
      error.value = e            // { code, message }
    } finally {
      submitting.value = false
    }
  }

  return {
    submitting, error,
    login: (payload) => run(() => logIn(payload)),
    signup: (payload) => run(() => signUp(payload)),
    logout: async () => {
      // 명세: logout 은 쿠키·토큰 유효성과 무관하게 항상 204 다. 실패해도 세션은 비운다.
      try { await logOut() } finally {
        session.clear()
        router.replace({ name: 'login' })
      }
    },
  }
}
```

> **`redirect` 쿼리를 그대로 `router.push` 에 넣지 마세요.** `//evil.com` 같은 값이 오면
> 외부로 튕깁니다. `startsWith('/')` 검사가 그 방어입니다.

### 5-4. 가드와의 관계

라우터 가드는 Step 0 에서 이미 돌았습니다. A1 이 할 일은 **가드가 볼 상태를 채우는 것**뿐입니다.

```
main.js: await restoreSession()   →  session.accessToken/user 확정
              ↓
router.beforeEach: session.isAuthenticated 로 판단
```

`restoreSession()` 이 `app.mount()` **전에** 끝나므로 첫 렌더에서 이미 판정이 나 있습니다.
로그인 화면이 깜빡였다가 홈으로 튀는 현상이 생기지 않습니다.

---

## 6. 함정

- **FC1 — 토큰을 저장소에 넣고 싶어집니다.** 새로고침에서 로그인이 풀리는 게 불편해 보이거든요.
  그건 `restoreSession()` 이 이미 해결합니다. `localStorage` 를 쓰는 순간 XSS 한 번에
  14일짜리 세션이 통째로 넘어갑니다.
- **`reissue` 응답엔 `user` 가 없습니다.** `data.user` 를 꺼내면 `undefined` 가 스토어에 들어가고
  `isAuthenticated` 가 false 로 떠서 무한 리다이렉트가 됩니다. 반드시 `/users/me` 를 이어서 부릅니다.
- **`reissue` 는 `Set-Cookie` 를 보내지 않습니다.** 이건 정상입니다. 만료된 Refresh 로 로그인
  기간을 연장하면 안 되기 때문입니다. 쿠키가 안 왔다고 실패로 처리하지 마세요.
- **로그아웃은 실패해도 세션을 비웁니다.** `finally` 에 넣으세요.
- **`Origin` 헤더는 브라우저가 붙입니다.** 코드로 설정할 수 없고, 설정하려 하면 무시됩니다.
  403 `ORIGIN_NOT_ALLOWED` 가 나면 코드가 아니라 **포트/`APP_ORIGIN`** 문제입니다 (FC2).
- **`restoreSession` 을 화면에서 다시 부르지 마세요.** 부팅 시 한 번입니다.

---

## 7. 검증

```bash
docker compose down -v && docker compose up -d db backend

cd frontend && npm run dev
```

> 아래 검증은 **백엔드 API 01~05 구현 후**에 의미가 있습니다.
> 그 전까지는 1번·5번만 확인 가능합니다(재발급이 실패해도 앱이 뜨는지).

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 비로그인 상태로 `/me` 접속 | `/login?redirect=/me` 로 이동 |
| 2 | 앱 로드 직후 Network 탭 | `POST /auth/reissue` 1회. 401 이어도 앱이 정상 표시 |
| 3 | 콘솔에서 로그인 후 **새로고침** | 로그인 상태 유지. `reissue` → `/users/me` 순으로 2회 요청 |
| 4 | 새로고침 시 화면 | 로그인 화면이 **깜빡이지 않는다** |
| 5 | Application → Local Storage | **비어 있다** (FC1) |
| 6 | Application → Cookies | `refreshToken` 이 `HttpOnly ✓`, `Path=/api/v1/auth` |
| 7 | 백엔드를 내리고 새로고침 | 3초 안에 로그인 화면이 뜬다 (20초 멈추지 않는다) |

콘솔에서 직접 눌러 보기 (A2 화면이 아직 없을 때):

```js
// 브라우저 콘솔
const { logIn } = await import('/src/api/auth.js')
await logIn({ email: 'user@example.com', password: 'P@ssw0rd123' })
```

---

## 8. 완료 기준 (DoD)

- [ ] 새로고침해도 로그인이 유지된다 (`reissue` → `/users/me` 2단계)
- [ ] `localStorage`·`sessionStorage` 사용이 **0줄**이다
- [ ] `reissue` 실패가 **오류 화면 없이** 비로그인으로 떨어진다
- [ ] 백엔드가 꺼져 있어도 앱이 **3초 안에** 뜬다
- [ ] 로그아웃이 서버 실패와 무관하게 세션을 비운다
- [ ] `?redirect=` 복귀가 동작하고, `/` 로 시작하지 않는 값은 무시된다
- [ ] 401 재발급 요청이 **동시 호출에서도 1회**만 나간다 (Network 탭으로 확인)

커밋 예시:

```
feat(fe): 인증 API 4종과 새로고침 세션 복구 구현

accessToken 은 메모리에만 두고 HttpOnly Refresh 쿠키로 되살린다.
localStorage 에 두면 XSS 한 번에 14일짜리 세션이 통째로 넘어간다.
reissue 응답에는 user 가 없어서 /users/me 를 이어서 부른다.
```

---

# A2 — 로그인·회원가입 화면 (API 01 · 02)

우선순위 **P0** · 담당 **A** · 선행 **A1** · 브랜치 `feat/fe-auth-view`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-auth-view

cd frontend && npm run dev
```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 성공 | data |
| --- | --- | --- | --- | --- | --- |
| 01 | AUTH_001 | POST | `/auth/signup` | **201** | `LoginData` |
| 02 | AUTH_002 | POST | `/auth/login` | 200 | `LoginData` |

---

## 3. 명세 근거

- 《공개 API 상세》 API 01 · 02
- 《JWT 인증 계약》 마지막 문단 → 회원가입 입력 규칙
- 오류: `VALIDATION_ERROR` · `EMAIL_ALREADY_EXISTS` · `INVALID_CREDENTIALS` · `ORIGIN_NOT_ALLOWED`

---

## 4. 소유 파일

```
src/views/auth/LoginView.vue
src/views/auth/SignUpView.vue
src/components/auth/AuthField.vue          라벨 + input + 오류 메시지
src/utils/validators.js                    A 소유
```

---

## 5. 구현 지침

### 5-1. 입력 검증 — 서버와 **같은 규칙**으로

프론트 검증은 편의일 뿐이고 서버 검증이 진짜입니다. 그래도 규칙은 정확히 같아야
사용자가 같은 값을 두 번 거절 당하지 않습니다.

| 필드 | 규칙 |
| --- | --- |
| `email` | ASCII 이메일 형식, **trim 후 소문자로 정규화**, 전체 최대 254자 |
| `password` | 8~64 **유니코드 문자**, UTF-8 최대 72바이트. **공백을 임의로 제거하지 않는다** |
| `name` | trim 후 1~50자 (회원가입만) |

```js
// src/utils/validators.js
const EMAIL_RE = /^[\x21-\x7e]+@[\x21-\x7e]+\.[A-Za-z]{2,}$/

export const normalizeEmail = (v) => v.trim().toLowerCase()

/** 길이는 char_length 가 아니라 코드포인트 수다. 이모지가 2로 세지면 안 된다. */
const codePoints = (s) => [...s].length
const utf8Bytes = (s) => new TextEncoder().encode(s).length

export function validateEmail(raw) {
  const v = normalizeEmail(raw)
  if (!v) return '이메일을 입력해 주세요.'
  if (v.length > 254) return '이메일이 너무 깁니다.'
  if (!EMAIL_RE.test(v)) return '이메일 형식이 올바르지 않습니다.'
  return null
}

export function validatePassword(raw) {
  // 비밀번호는 trim 하지 않는다. 사용자가 설정한 값과 달라진다.
  const n = codePoints(raw)
  if (n < 8 || n > 64) return '비밀번호는 8자 이상 64자 이하로 입력해 주세요.'
  // BCrypt 가 72바이트에서 자르기 때문에 서버도 여기서 막는다
  if (utf8Bytes(raw) > 72) return '비밀번호가 너무 깁니다. 더 짧게 입력해 주세요.'
  return null
}

export function validateName(raw) {
  const v = raw.trim()
  if (codePoints(v) < 1 || codePoints(v) > 50) return '이름은 1자 이상 50자 이하로 입력해 주세요.'
  return null
}
```

- **길이는 `String.length` 가 아니라 코드포인트 수**입니다. `[...s].length` 를 씁니다.
  `'😀'.length` 는 2 지만 코드포인트로는 1 입니다.
- **비밀번호를 `trim()` 하지 마세요.** 이메일만 trim 합니다.

### 5-2. `LoginView.vue`

```vue
<script setup>
import { reactive, computed } from 'vue'
import { useAuthActions } from '@/composables/useAuthActions'
import { normalizeEmail, validateEmail, validatePassword } from '@/utils/validators'
import AuthField from '@/components/auth/AuthField.vue'

const { login, submitting, error } = useAuthActions()
const form = reactive({ email: '', password: '' })
const touched = reactive({ email: false, password: false })

const emailError = computed(() => (touched.email ? validateEmail(form.email) : null))
const passwordError = computed(() => (touched.password ? validatePassword(form.password) : null))
const canSubmit = computed(() =>
  !submitting.value && !validateEmail(form.email) && !validatePassword(form.password))

const onSubmit = () => {
  touched.email = touched.password = true
  if (!canSubmit.value) return
  login({ email: normalizeEmail(form.email), password: form.password })
}
</script>

<template>
  <form class="auth" @submit.prevent="onSubmit">
    <h1>로그인</h1>

    <AuthField v-model="form.email" label="이메일" type="email"
               autocomplete="email" :error="emailError" @blur="touched.email = true" />
    <AuthField v-model="form.password" label="비밀번호" type="password"
               autocomplete="current-password" :error="passwordError"
               @blur="touched.password = true" />

    <!-- 서버 오류는 message 를 그대로 노출한다 (AGENTS.md §5.4) -->
    <p v-if="error" class="server-error" role="alert">{{ error.message }}</p>

    <button type="submit" :disabled="!canSubmit">
      {{ submitting ? '로그인 중…' : '로그인' }}
    </button>

    <router-link :to="{ name: 'signup' }">회원가입</router-link>
  </form>
</template>
```

### 5-3. `SignUpView.vue`

같은 형태에 `name` 필드가 추가됩니다. 성공은 **201** 이고 응답은 로그인과 같은 `LoginData` 라
`useAuthActions().signup()` 이 그대로 처리합니다. **가입 직후 로그인 상태로 진입합니다.**

- `plan` 은 항상 `FREE` 로 옵니다. 화면에서 고를 수 없습니다.
- `profileImageUrl` 은 `null` 입니다.

### 5-4. 오류 표시

| 코드 | 위치 | 문구 |
| --- | --- | --- |
| `VALIDATION_ERROR` (422) | 폼 상단 | 서버 `message` 그대로 |
| `EMAIL_ALREADY_EXISTS` (409) | **이메일 필드 아래** | 서버 `message` 그대로 |
| `INVALID_CREDENTIALS` (401) | 폼 상단 | 서버 `message` 그대로 |
| `ORIGIN_NOT_ALLOWED` (403) | 폼 상단 | 서버 `message` + "개발 환경 설정을 확인하세요" (개발 빌드에서만) |

`INVALID_CREDENTIALS` 에 **"이메일이 없습니다" 같은 힌트를 덧붙이지 마세요.**
서버가 일부러 구분하지 않는 값입니다.

---

## 6. 함정

- **비밀번호를 `trim()` 하면** 사용자가 설정한 비밀번호와 달라져 로그인이 안 됩니다. **이메일만** trim 합니다.
- **길이를 `.length` 로 세면** 이모지·한글 조합 문자에서 서버와 판정이 갈립니다. 코드포인트로 세세요.
- **회원가입 성공은 201 입니다.** `res.status === 200` 으로 분기하면 실패로 처리됩니다.
  (`unwrap()` 을 쓰면 애초에 상태 코드를 볼 일이 없습니다.)
- **`EMAIL_ALREADY_EXISTS` 를 미리 조회로 막으려 하지 마세요.** 서버가 DB UNIQUE 로 막고 409 를 줍니다.
  프론트에서 "중복 확인" 버튼을 만들 API 가 없습니다.
- **`autocomplete` 를 빼지 마세요.** 브라우저 비밀번호 관리자가 동작하지 않으면 발표 시연이 느려집니다.
- **로그인 폼에서 Enter 가 먹어야 합니다.** `<form @submit.prevent>` 를 씁니다.
  `<button @click>` 만 두면 Enter 로 제출이 안 됩니다.

---

## 7. 검증

> 백엔드 API 01·02 구현 후 수행합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | `user@example.com` / `P@ssw0rd123` 로 로그인 | 홈으로 이동, 헤더에 이름 표시 |
| 2 | 비밀번호를 `wrongpass1` 로 | 폼 상단에 "이메일 또는 비밀번호가 올바르지 않습니다." |
| 3 | 이미 있는 이메일로 가입 | 이메일 필드 아래에 "이미 사용 중인 이메일입니다." |
| 4 | 새 이메일로 가입 | **201**, 즉시 로그인 상태로 홈 진입 |
| 5 | 비밀번호 7자 | 제출 버튼 비활성 + 필드 오류 |
| 6 | 비밀번호 앞뒤에 공백을 넣고 가입 → 같은 값으로 로그인 | 성공 (공백이 보존된다) |
| 7 | `/me` 접속 → 로그인 | 로그인 후 **`/me`** 로 복귀 |
| 8 | Enter 키로 제출 | 동작한다 |

---

## 8. 완료 기준 (DoD)

- [ ] 회원가입이 **201** 이고 그대로 로그인 상태가 된다
- [ ] 이메일은 trim + 소문자 정규화, **비밀번호는 공백을 건드리지 않는다**
- [ ] 길이 검사가 **코드포인트 8~64** 와 **UTF-8 72바이트** 두 기준을 모두 본다
- [ ] `EMAIL_ALREADY_EXISTS` 가 이메일 필드에 붙는다
- [ ] `INVALID_CREDENTIALS` 메시지에 힌트를 덧붙이지 않았다
- [ ] 모든 서버 오류가 `error.message` 를 **그대로** 노출한다
- [ ] `?redirect=` 복귀가 동작한다
- [ ] Enter 키 제출이 동작하고 `autocomplete` 가 붙어 있다

커밋 예시:

```
feat(fe): 로그인·회원가입 화면 구현

비밀번호 길이는 코드포인트 8~64 와 UTF-8 72바이트 두 기준으로 검사한다.
서버가 BCrypt 72바이트 절단 때문에 같은 규칙을 쓰기 때문이다.
이메일만 trim 한다. 비밀번호를 trim 하면 사용자가 설정한 값과 달라진다.
```

---

# A3 — 앱 셸·헤더·내 정보 (API 05 · 04)

우선순위 **P0** · 담당 **A** · 선행 **A1** · 브랜치 `feat/fe-app-shell`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-app-shell

docker compose down -v && docker compose up -d db backend

cd frontend && npm run dev
```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 성공 | data |
| --- | --- | --- | --- | --- | --- |
| 05 | USER_005 | GET | `/users/me` | 200 | `User` |
| 04 | USER_006 | POST | `/auth/logout` | **204** | 없음 |

가장 작은 작업이면서, **A1 이 만든 인증 기반이 실제로 도는지 눈으로 확인하는 첫 화면**입니다.

---

## 3. 명세 근거

- 《공개 API 상세》 API 05
- 《DTO User》 표
- 《공통 구현 계약》 *권한* 절 → "매 요청 `users.plan` 을 검사한다. JWT 에 plan 을 넣지 않는다"

---

## 4. 소유 파일

```
src/api/users.js                            API 05·06 (06 은 A4 가 채운다)
src/components/layout/AppHeader.vue         Step 0 뼈대를 채운다 → A 단독 소유
src/components/layout/PlanBadge.vue
src/views/account/MyPageView.vue            A4 가 이 파일에 탈퇴를 추가한다
src/views/account/UpgradeView.vue           A6·A8·B6 가 링크로 이동해 온다
```

---

## 5. 구현 지침

### 5-1. `api/users.js`

```js
import { client, unwrap } from './client'

export const fetchMe = () => unwrap(client.get('/users/me'))
// withdraw 는 A4 에서 추가한다
```

```
User : id(문자열), email, name, profileImageUrl(nullable), plan, createdAt
```

- **`id` 는 문자열입니다.** `"2"` 이지 `2` 가 아닙니다 (§C3).
- **`profileImageUrl` 은 없으면 `null` 이고 필드 자체는 항상 옵니다.** `undefined` 검사 대신 `null` 검사.
- **`plan` 은 매 요청 서버 값**입니다. JWT 를 파싱하거나 캐시하지 않습니다.

### 5-2. 등급이 즉시 반영되어야 한다

명세: "등급 변경이 재로그인 없이 즉시 반영되어야 한다."
그래서 `MyPageView` 진입 시 **05 를 다시 호출**해 스토어를 갱신합니다.

```js
onMounted(async () => {
  state.value = 'loading'
  try {
    session.setUser(await fetchMe())   // 헤더 배지까지 같이 갱신된다
    state.value = 'ready'
  } catch (e) {
    error.value = e
    state.value = 'error'
  }
})
```

부팅 시 `restoreSession()` 이 이미 한 번 불렀지만, 그건 **부팅 시점의 값**입니다.
DB 에서 `plan` 을 `PRO` 로 바꾼 뒤 새로고침 없이 확인하려면 이 재조회가 있어야 합니다.

### 5-3. `AppHeader.vue`

```vue
<script setup>
import { useSessionStore } from '@/stores/session'
import { useAuthActions } from '@/composables/useAuthActions'
import PlanBadge from './PlanBadge.vue'

const session = useSessionStore()
const { logout } = useAuthActions()
</script>

<template>
  <header class="header">
    <router-link :to="{ name: 'home' }" class="brand">스피치 습관</router-link>

    <nav class="nav">
      <router-link :to="{ name: 'record' }">연습하기</router-link>
      <router-link :to="{ name: 'recordingList' }">기록</router-link>
      <router-link :to="{ name: 'trends' }">추이</router-link>
      <router-link :to="{ name: 'weeklyReport' }">
        주간 리포트
        <!-- meta.proFeature 는 자물쇠 배지 용도로만 쓴다. 차단은 서버가 한다 (§C9) -->
        <span v-if="!session.isPro" class="lock" aria-label="PRO 전용">🔒</span>
      </router-link>
    </nav>

    <div class="me">
      <PlanBadge :plan="session.user?.plan" />
      <router-link :to="{ name: 'me' }">{{ session.user?.name }}</router-link>
      <button type="button" @click="logout">로그아웃</button>
    </div>
  </header>
</template>
```

> **헤더는 A 단독 소유입니다.** B 는 이 파일을 고치지 않습니다.
> B 의 진입점(`/record`)은 이미 위 네비게이션에 있고, 라우트는 Step 0 에서 선언돼 있습니다 (감사표 #5).

### 5-4. `MyPageView.vue`

표시 항목: 이름 · 이메일 · 등급 · 가입일(`createdAt`).
A4 가 여기에 "회원 탈퇴" 영역을 추가합니다. **A4 를 위해 자리를 미리 비워 둡니다.**

```vue
<section class="danger-zone">
  <!-- A4 가 채운다 -->
</section>
```

### 5-5. `UpgradeView.vue`

PRO 안내 정적 화면입니다. **결제 API 가 없습니다.** 만들지 마세요.
`pro.upgradePath` 가 `"/upgrade"` 로 오므로 경로가 이미 맞습니다.

---

## 6. 함정

- **`plan` 을 JWT 에서 읽지 마세요.** 명세가 일부러 넣지 않았습니다. 항상 `users.plan` 입니다.
- **`profileImageUrl` 이 `null` 일 때 필드를 생략하지 마세요.** 명세: "명시한 필드는 항상 존재하고
  nullable 만 null 을 허용한다." 화면에서는 기본 아바타를 그립니다.
- **`id` 를 숫자로 바꾸지 마세요** (§C3).
- **`createdAt` 은 UTC 입니다.** `formatDate()` 를 거쳐 KST 로 표시합니다 (§C4).
- **로그아웃 버튼을 헤더 밖에 또 만들지 마세요.** 진입점이 둘이 되면 세션 정리 경로가 갈라집니다.
- **`/users/me` 가 401 이면** 인터셉터가 알아서 재발급합니다. 화면에서 401 을 따로 처리하지 마세요.

---

## 7. 검증

> 백엔드 API 04·05 구현 후 수행합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | `user@example.com` 로그인 | 헤더에 이름 + `무료` 배지 |
| 2 | `pro@example.com` 로그인 | 헤더에 `PRO` 배지, 주간 리포트에 자물쇠 **없음** |
| 3 | `/me` 진입 | 이름·이메일·등급·가입일 표시, 가입일이 KST |
| 4 | DB 에서 plan 을 PRO 로 바꾸고 `/me` 재진입 | **새로고침 없이** 헤더 배지가 PRO 로 바뀐다 |
| 5 | 로그아웃 | 204, `/login` 이동, 뒤로가기로 `/me` 접근 시 다시 `/login` |
| 6 | Network 탭 | 모든 응답에 `Cache-Control: no-store` |

```bash
# 4번 검증용 — DB 구축 후
docker compose exec db psql -U postgres -d miniproject \
  -c "update users set plan='PRO' where email='user@example.com';"
```

---

## 8. 완료 기준 (DoD)

- [ ] `data.id` 를 **문자열로** 다룬다
- [ ] `plan` 이 **DB 값**에서 오고, `/me` 재진입 시 헤더 배지가 즉시 갱신된다
- [ ] `profileImageUrl` 이 `null` 일 때 화면이 깨지지 않는다
- [ ] 로그아웃이 204 이고 이후 보호 화면이 `/login` 으로 튕긴다
- [ ] 가입일이 Asia/Seoul 로 표시된다
- [ ] 헤더가 **A 소유 파일 하나**이고 B 의 파일을 import 하지 않는다
- [ ] `/upgrade` 가 뜨고 결제 관련 호출이 **없다**

커밋 예시:

```
feat(fe): 앱 셸과 내 정보 화면 구현

plan 은 JWT 가 아니라 매 요청 GET /users/me 에서 읽는다.
등급 변경이 재로그인 없이 즉시 반영되어야 하기 때문이다.
```

---

# A4 — 회원 탈퇴 (API 06)

우선순위 **P1** · 담당 **A** · 선행 **A3** · 브랜치 `feat/fe-withdraw`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-withdraw

docker compose down -v && docker compose up -d db backend

cd frontend && npm run dev
```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 성공 |
| --- | --- | --- | --- | --- |
| 06 | USER_007 | DELETE | `/users/me` | **204** (본문 없음) |

요청 **본문에 `password` 가 있는 DELETE** 입니다. 명세가 그렇게 정했습니다.

---

## 3. 명세 근거

- 《공개 API 상세》 API 06
- 오류: `VALIDATION_ERROR` · `INVALID_PASSWORD` · `CANNOT_DELETE_WHILE_PROCESSING`

---

## 4. 소유 파일

```
src/api/users.js                        withdraw 추가 (A3 파일)
src/views/account/MyPageView.vue        danger-zone 을 채운다 (A3 파일)
src/components/account/WithdrawDialog.vue
```

---

## 5. 구현 지침

### 5-1. `api/users.js` 에 추가 — **본문 있는 DELETE**

axios 의 `delete` 는 두 번째 인자가 `config` 입니다. 본문은 `data` 키에 넣습니다.

```js
export const withdraw = (password) =>
  unwrap(client.delete('/users/me', { data: { password } }))
```

`client.delete('/users/me', { password })` 라고 쓰면 **본문이 안 실려서 422** 가 됩니다.

### 5-2. 확인 다이얼로그

- 비밀번호 입력 필수. **검증 규칙은 로그인과 동일**(코드포인트 8~64, UTF-8 72바이트)합니다.
- 되돌릴 수 없다는 문구를 명시합니다. 성공하면 모든 기록이 함께 지워집니다.
- `<dialog>` 를 쓰거나 오버레이를 직접 만듭니다. **Esc 로 닫히고 포커스가 갇혀야** 합니다.

### 5-3. 오류 분기

| 코드 | 상태 | 화면 |
| --- | --- | --- |
| `VALIDATION_ERROR` | 422 | 비밀번호 필드 아래 |
| `INVALID_PASSWORD` | 401 | 비밀번호 필드 아래. **`INVALID_CREDENTIALS` 가 아니다** |
| `CANNOT_DELETE_WHILE_PROCESSING` | 409 | 다이얼로그 상단. **계정은 그대로 남아 있다**는 안내 + "기록" 화면 링크 |

`CANNOT_DELETE_WHILE_PROCESSING` 의 판정 범위는 **그 사용자 전체**입니다.
특정 기록 하나가 아니라 어떤 녹음이든 분석 중이면 막힙니다. 문구도 그렇게 씁니다.

### 5-4. 성공 처리

```js
await withdraw(password)     // 204
session.clear()
router.replace({ name: 'login' })
```

- **본문이 없습니다.** `unwrap()` 이 `null` 을 줍니다. 구조 분해하지 마세요.
- 서버가 Refresh 쿠키를 `Max-Age=0` 으로 만료시킵니다. 프론트가 쿠키를 지울 필요도, 지울 수도 없습니다.
- 남아 있는 accessToken 은 서버에서 `users` 행이 없어 401 이 됩니다. 그래도 `session.clear()` 를 합니다.

---

## 6. 함정

- **axios `delete` 본문은 `{ data: ... }` 입니다.** 가장 자주 틀리는 지점입니다.
- **`INVALID_PASSWORD` 와 `INVALID_CREDENTIALS` 를 혼동하지 마세요.** 코드가 다릅니다.
- **409 를 받았을 때 "탈퇴됐다"고 표시하지 마세요.** 계정이 그대로 남아 있습니다.
- **탈퇴 성공 뒤 `/me` 를 다시 조회하지 마세요.** 401 이 나서 에러 화면이 깜빡입니다.
  `session.clear()` → `router.replace` 순서를 지키세요.
- **204 에 본문을 기대하지 마세요.**
- **탈퇴 버튼을 눈에 잘 띄는 자리에 두지 마세요.** `danger-zone` 안, 확인 다이얼로그 뒤입니다.

---

## 7. 검증

> 백엔드 API 06 구현 후 수행합니다.

```bash
# 활성 분석을 하나 만들어 둔다 (seed 의 PENDING 건이 이미 있다) — DB 구축 후
docker compose exec db psql -U postgres -d miniproject \
  -c "select id, status from analyses order by id;"
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 틀린 비밀번호로 탈퇴 | "비밀번호가 올바르지 않습니다." (401) |
| 2 | 분석 진행 중인 상태로 탈퇴 | 409 안내 + **계정이 남아 있다** |
| 3 | 분석을 정리한 뒤 탈퇴 | 204 → `/login` 이동 |
| 4 | 탈퇴 후 뒤로가기 | 보호 화면에 못 들어간다 |
| 5 | 탈퇴 후 같은 이메일로 로그인 | 401 `INVALID_CREDENTIALS` |
| 6 | Network 탭에서 DELETE 요청 | **요청 본문에 `password` 가 실려 있다** |

---

## 8. 완료 기준 (DoD)

- [ ] DELETE 요청에 **본문이 실린다** (`{ data: { password } }`)
- [ ] `INVALID_PASSWORD`(401)를 비밀번호 필드에 붙인다
- [ ] `CANNOT_DELETE_WHILE_PROCESSING`(409)에서 **계정이 남아 있다**고 안내한다
- [ ] 성공이 **204** 이고 본문을 기대하지 않는다
- [ ] 성공 후 세션이 비워지고 `/login` 으로 이동한다
- [ ] 확인 다이얼로그 없이는 탈퇴가 불가능하다
- [ ] 다이얼로그가 Esc 로 닫히고 포커스가 안에 갇힌다

커밋 예시:

```
feat(fe): 회원 탈퇴 화면 구현

axios delete 는 본문을 config.data 로 넘겨야 실린다.
두 번째 인자에 바로 객체를 주면 본문이 비어 422 가 된다.
활성 분석이 있으면 409 이고 계정은 그대로 남는다.
```

---

# A5 — 대시보드 홈·기록 목록 (API 12 · 15)

우선순위 **P0** · 담당 **A** · 선행 **Step 0** · 브랜치 `feat/fe-history-list`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-history-list

docker compose down -v && docker compose up -d db backend
docker compose exec db psql -U postgres -d miniproject \
  -c "select id, status from analyses order by id;"

cd frontend && npm run dev
```

> **담당 B 의 업로드를 기다리지 않습니다.** `db/seed-dev.sql` 에 기록이 들어 있습니다.

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 성공 | data |
| --- | --- | --- | --- | --- | --- |
| 12 | DASH_001 | GET | `/dashboard/recordings` | 200 | `RecordingPage` |
| 15 | DASH_008 | GET | `/dashboard/recent-analyses` | 200 | `RecentAnalyses` |

---

## 3. 명세 근거

- 《공개 API 상세》 API 12 · 15
- 《DTO 사전》 → `RecordingPage`, `RecordingSummary`, `RecentAnalyses`
- 《이력·통계·비교 계약》 첫 문단 → 정렬 규칙
- 오류: `VALIDATION_ERROR`

---

## 4. 소유 파일

```
src/api/history.js                        API 10·11·12·14·15 (10·11·14 는 A6)
src/composables/usePagination.js
src/views/dashboard/HomeView.vue
src/views/dashboard/RecordingListView.vue
src/components/history/RecordingCard.vue
src/components/history/StatusChip.vue
src/components/history/Pagination.vue
```

---

## 5. 구현 지침

### 5-1. `api/history.js`

```js
import { client, unwrap } from './client'

export const fetchRecordingPage = (page = 0, size = 20) =>
  unwrap(client.get('/dashboard/recordings', { params: { page, size } }))

export const fetchRecentAnalyses = () =>
  unwrap(client.get('/dashboard/recent-analyses'))

// 10·11·14 는 A6 에서 추가한다
```

### 5-2. API 12 — 페이지네이션

- 파라미터: `page`(≥0, 기본 0), `size`(1~100, 기본 20).
- **정렬 파라미터가 없습니다.** 서버가 `submittedAt DESC, id DESC` 로 고정합니다.
  정렬 UI 를 만들지 마세요.
- 범위를 벗어나면 **422** 입니다. 서버가 보정하지 않으므로 **프론트에서 미리 막습니다.**
- `totalPages` 는 **0건일 때 0** 입니다. 1이 아닙니다. 페이지네이션 컴포넌트가 이걸 처리해야 합니다.

```js
// composables/usePagination.js
import { ref, computed } from 'vue'

const SIZE = 20

export function usePagination(loader) {
  const page = ref(0)
  const size = ref(SIZE)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const items = ref([])
  const state = ref('loading')
  const error = ref(null)

  const canPrev = computed(() => page.value > 0)
  const canNext = computed(() => page.value + 1 < totalPages.value)

  async function load(next = page.value) {
    // 422 를 유발하는 값을 아예 보내지 않는다
    const p = Math.max(0, next)
    const s = Math.min(100, Math.max(1, size.value))
    state.value = 'loading'
    error.value = null
    try {
      const data = await loader(p, s)
      page.value = p
      items.value = data.content
      totalPages.value = data.totalPages
      totalElements.value = data.totalElements
      state.value = data.content.length ? 'ready' : 'empty'
    } catch (e) {
      error.value = e
      state.value = 'error'
    }
  }

  return { page, size, items, totalPages, totalElements, state, error, canPrev, canNext, load }
}
```

### 5-3. `RecordingSummary` 렌더링 — **FC5 가 걸리는 자리**

```
recordingId, submittedAt, durationMs, status, fillerTotalCount(nullable)
```

| 상태 | 추임새 표시 |
| --- | --- |
| `COMPLETED` | `fillerTotalCount` 값 |
| `PENDING` / `PROCESSING` / `FAILED` | **`—`** (`0회` 가 아니다) |

```vue
<!-- ❌ 미완료가 "0회" 로 보인다 -->
<span>{{ item.fillerTotalCount ?? 0 }}회</span>
<!-- ✅ -->
<span>{{ formatCount(item.fillerTotalCount) }}</span>
```

명세: "미완료는 0개 추임새를 뜻하지 않는다."

### 5-4. API 15 — 최근 분석

- **완료된 것만, 최대 3건**. 없으면 `items: []` 이고 **200** 입니다(404 아님).
- 정렬 기준은 `submittedAt DESC, id DESC` 로, **완료 시각이 아닙니다.**
  재시도한 기록이 목록 맨 위로 튀어 오르면 안 됩니다.
- 홈 화면의 "최근 연습" 섹션에 씁니다. 비어 있으면 "첫 연습을 시작해 보세요" + `/record` 링크.

### 5-5. 홈 화면 구성

```
[최근 분석 3건 카드]     ← API 15
[연습 시작 버튼]         ← router-link { name: 'record' }   (B 의 화면, 라우트는 Step 0 선언)
[전체 기록 보기]         ← router-link { name: 'recordingList' }
```

**B 의 컴포넌트를 import 하지 않습니다.** 라우터 링크로만 이어집니다 (감사표 #5).

---

## 6. 함정

- **정렬 UI 를 만들지 마세요.** 명세에 정렬 파라미터가 없습니다.
- **완료 시각으로 정렬하지 마세요.** `submittedAt` 입니다.
- **`totalPages` 가 0일 수 있습니다.** `totalPages - 1` 을 마지막 페이지로 계산하면 `-1` 이 됩니다.
- **`size=0` 이나 `size=101` 을 보내지 마세요.** 422 입니다. 서버가 보정해 주지 않습니다.
- **`fillerTotalCount` 에 `?? 0` 을 쓰지 마세요** (FC5).
- **빈 목록은 오류가 아닙니다.** 200 + `empty` 상태입니다.
- **`recordingId` 는 문자열입니다.** `:key` 에 그대로 씁니다 (§C3).
- 최근 분석이 3건 미만이어도 정상입니다. 자리를 억지로 채우지 마세요.

---

## 7. 검증

> 백엔드 API 12·15 + `db/seed-dev.sql` 이후 수행합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | `/recordings` 진입 | 기록 2건, 최신(`101`)이 위 |
| 2 | 각 카드의 추임새 | 완료 건은 숫자, **분석 중 건은 `—`** |
| 3 | 홈 진입 | 최근 분석이 **완료 건만**, 최대 3개 |
| 4 | 새 계정으로 로그인 후 `/recordings` | 빈 목록 + 안내 문구, 콘솔 오류 없음 |
| 5 | 주소창에 `?page=99` 로 진입 | 오류 화면이 아니라 빈 페이지 (또는 0페이지로 보정) |
| 6 | 날짜 표시 | Asia/Seoul (KST 09:00 미만 UTC 값이 전날로 밀리지 않는다) |
| 7 | Network 탭 | `size` 가 항상 1~100 범위 |

```bash
# DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select id, submitted_at, (submitted_at at time zone 'Asia/Seoul')::date from recordings order by id;"
# 화면의 날짜와 이 쿼리 결과가 일치해야 한다
```

---

## 8. 완료 기준 (DoD)

- [ ] 목록이 `submittedAt` 내림차순이고 정렬 UI 가 **없다**
- [ ] 미완료 항목의 추임새가 **`—`** 다 (`0회` 가 아니다)
- [ ] 0건일 때 `totalPages` 가 0이어도 페이지네이션이 깨지지 않는다
- [ ] `size` 가 1~100 을 벗어나는 요청을 보내지 않는다
- [ ] 빈 목록이 `empty` 상태로 표시되고 200 이다
- [ ] 최근 분석이 **완료 건만 최대 3건**이고, 0건이면 안내가 뜬다
- [ ] 날짜가 Asia/Seoul 이다
- [ ] `loading / error / empty` 3종이 전부 처리돼 있다
- [ ] B 소유 컴포넌트를 import 하지 않는다

커밋 예시:

```
feat(fe): 대시보드 홈과 기록 목록 화면 구현

미완료 기록의 fillerTotalCount 는 null 이고 0회가 아니다.
?? 0 으로 채우면 분석 중인 녹음이 "추임새 0회" 로 보인다.
정렬은 서버가 submitted_at DESC 로 고정하므로 정렬 UI 를 두지 않는다.
```

---

# A6 — 기록 상세·결과·삭제·PRO 잠금 (API 10 · 14 · 11)

우선순위 **P0**(10·14) / **P1**(11) · 담당 **A** · 선행 **A5** · 브랜치 `feat/fe-history-detail`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-history-detail

docker compose down -v && docker compose up -d db backend

cd frontend && npm run dev
```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 성공 | data |
| --- | --- | --- | --- | --- | --- |
| 10 | HIST_001 | GET | `/recordings/{recordingId}` | 200 | `RecordingDetail` |
| 14 | DASH_003 | GET | `/dashboard/recordings/{recordingId}/result` | 200 | `BasicResult` |
| 11 | HIST_002 | DELETE | `/recordings/{recordingId}` | **204** | 없음 |

---

## 3. 명세 근거

- 《공개 API 상세》 API 10 · 11 · 14
- 《DTO 사전》 → `RecordingDetail`, `BasicResult`, `Basic`, `FillerBreakdown`, `ProAccess`, `AnalysisStatus`
- 《미완료 상세와 PRO 접근 순서》
- 오류: `RESOURCE_NOT_FOUND` · `ANALYSIS_NOT_COMPLETED` · `CANNOT_DELETE_WHILE_PROCESSING`

---

## 4. 소유 파일

```
src/api/history.js                              10·11·14 추가 (A5 파일)
src/views/dashboard/RecordingDetailView.vue
src/components/history/BasicResultCard.vue
src/components/history/FillerBreakdownList.vue
src/components/history/ProLockCard.vue          ← A6 단독 소유 (감사표 #7)
src/components/history/DeleteRecordingDialog.vue
```

---

## 5. 구현 지침

### 5-1. `api/history.js` 에 추가

```js
export const fetchRecordingDetail = (recordingId) =>
  unwrap(client.get(`/recordings/${recordingId}`))

export const fetchBasicResult = (recordingId) =>
  unwrap(client.get(`/dashboard/recordings/${recordingId}/result`))

export const removeRecording = (recordingId) =>
  unwrap(client.delete(`/recordings/${recordingId}`))
```

### 5-2. API 10 이 화면의 주 데이터원입니다

```
recordingId, submittedAt, durationMs, mimeType, fileSizeBytes,
analysis(AnalysisStatus), basic(Basic?), pro(ProAccess),
algorithmVersion, engineType, engineVersion
```

**10 하나로 화면이 다 그려집니다.** 14 는 굳이 부르지 않아도 됩니다.

| 언제 14 를 부르나 | |
| --- | --- |
| 상세 진입 | **10 만** 부른다 |
| "결과 다시 불러오기" 버튼 | 14 를 부른다 (완료 건만) |

14 를 진입 시 같이 부르면 미완료 기록에서 **409 `ANALYSIS_NOT_COMPLETED`** 가 떠서
정상 화면에 오류 배너가 뜹니다. 그러지 마세요.

### 5-3. 미완료 상태 렌더링 — 명세 예시와 한 글자씩 대조

`PENDING` 인 기록의 응답:

```
basic: null
pro.available: false
analysis.startedAt: null, analysis.finishedAt: null
analysis.retryable: false
```

화면:

- `basic` 이 `null` 이면 **결과 카드를 그리지 않고** `StateBlock state="pending"` 을 보여줍니다.
- "분석 진행 상황 보기" 링크 → `{ name: 'analysisProgress', params: { analysisId: analysis.analysisId } }`
  (B4 의 화면. 라우트는 Step 0 선언 → **B 의 컴포넌트를 import 하지 않습니다**)

### 5-4. `retryable` 은 서버가 준 값을 그대로 씁니다

```
retryable          = status == FAILED && attemptNo < 4      ← 서버가 매번 계산해서 준다
retryRequiresAudio = retryable 과 같은 값
```

**프론트에서 다시 계산하지 마세요.** 서버가 이미 계산해서 보냅니다.
`retryable: true` 면 "다시 시도" 안내를 띄우고 **진행 화면(B4/B5)** 으로 보냅니다.
재시도 실행 자체는 B5 의 몫입니다.

### 5-5. `ProAccess` — 잠금 카드 (§C9 · §C10)

```
locked, available, detailUrl(nullable), upgradePath(nullable), lockedFeatures[]
```

| | FREE | PRO |
| --- | --- | --- |
| `locked` | `true` | `false` |
| `available` | `false` | `status == COMPLETED` 일 때 `true` |
| `detailUrl` | `null` | available 일 때 `/api/v1/recordings/{id}/pro-analysis` |
| `upgradePath` | `"/upgrade"` | `null` |
| `lockedFeatures` | 9개 | `[]` |

```vue
<script setup>
const props = defineProps({ pro: { type: Object, required: true }, recordingId: { type: String, required: true } })

const FEATURE_LABEL = {
  waveform: '음성 파형', silence: '침묵 구간', speed: '말하기 속도',
  timeline: '추임새 타임라인', segment: '구간별 분석', repetition: '반복 표현',
  comparison: '이전 기록 비교', coaching: '맞춤 코칭', weeklyReport: '주간 리포트',
}
</script>

<template>
  <!-- PRO + 완료: 상세로 보낸다. detailUrl 을 href 로 쓰지 않는다 (§C9) -->
  <router-link v-if="pro.available"
               :to="{ name: 'proAnalysis', params: { recordingId } }" class="pro-cta">
    상세 분석 보기
  </router-link>

  <!-- PRO 인데 아직 미완료 -->
  <p v-else-if="!pro.locked">분석이 끝나면 상세 분석을 볼 수 있습니다.</p>

  <!-- FREE: 잠금 목록. 서버가 준 순서 그대로 (§C10) -->
  <div v-else class="pro-lock">
    <h3>PRO 등급에서 이용할 수 있습니다</h3>
    <ul>
      <li v-for="f in pro.lockedFeatures" :key="f">🔒 {{ FEATURE_LABEL[f] ?? f }}</li>
    </ul>
    <router-link :to="{ name: 'upgrade' }">업그레이드 안내</router-link>
  </div>
</template>
```

- **`lockedFeatures` 를 정렬하거나 재배치하지 마세요.** 순서까지 명세입니다.
- **`FEATURE_LABEL` 에 없는 키가 오면 키 그대로** 보여줍니다. 화면을 깨뜨리지 않습니다.
- `upgradePath` 가 `"/upgrade"` 라 라우트와 일치하지만, **라우터 `name` 으로 이동**합니다.

### 5-6. `Basic` 렌더링

```
Basic : fillerTotalCount, fillerBreakdown[]{ word, count }
```

- `fillerBreakdown` 은 이미 `count` 내림차순으로 옵니다. **다시 정렬하지 마세요.**
- 화면에서 `fillerTotalCount == sum(count)` 를 검증할 필요는 없지만, 개발 중에는
  콘솔에서 한 번 확인해 두면 백엔드 버그를 빨리 잡습니다.
- **`basic` 에는 추임새만 들어 있습니다.** 침묵·속도·파형은 API 17(B6)에서만 옵니다.

### 5-7. API 11 — 삭제

- 확인 다이얼로그 필수. 되돌릴 수 없습니다.
- 성공은 **204**, 본문 없음 → `router.replace({ name: 'recordingList' })`
- `409 CANNOT_DELETE_WHILE_PROCESSING`: **그 녹음 하나**의 상태 때문입니다(A4 의 탈퇴와 범위가 다릅니다).
  "분석이 끝난 뒤 삭제할 수 있습니다" 로 안내합니다.
- 이미 지운 기록을 다시 지우면 **404** 입니다. `410` 이 아닙니다.
- 삭제 후 목록 캐시를 들고 있지 않으므로 재조회로 자연히 사라집니다.

---

## 6. 함정

- **상세 진입 시 14 를 같이 부르지 마세요.** 미완료 건에서 409 가 뜹니다.
- **`pro.detailUrl` 을 `href` 에 넣지 마세요.** `/api/v1/...` 는 API 경로입니다.
  브라우저가 그리로 이동하면 JSON 이 뜨거나 404 가 됩니다 (§C9, 감사표 #8).
- **`lockedFeatures` 를 정렬하지 마세요** (§C10).
- **`retryable` 을 프론트에서 다시 계산하지 마세요.** 서버 값을 그대로 씁니다.
- **삭제 후 404 를 "이미 지워짐(410)"으로 표시하지 마세요.** 서버가 404 를 줍니다.
- **없는 기록과 남의 기록이 똑같이 404** 입니다. 화면에서 구분하지 마세요 (§C8).
- **`basic` 이 `null` 인 걸 오류로 처리하지 마세요.** 정상적인 미완료 상태입니다.
- **`mimeType`·`fileSizeBytes` 를 안 쓰더라도 응답에서 지우지 마세요.** 상세 정보로 표시합니다.

---

## 7. 검증

> 백엔드 API 10·11·14 + seed 이후 수행합니다.

```bash
# DB 구축 후
docker compose exec db psql -U postgres -d miniproject \
  -c "select id, recording_id, status from analyses order by id;"
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 완료 기록(101) 상세 | 결과 카드 + 추임새 분해 표시 |
| 2 | FREE 계정에서 1번 | 잠금 기능 **9개**가 명세 순서대로 |
| 3 | PRO 계정에서 1번 | "상세 분석 보기" 링크가 `/recordings/101/pro` 로 이동 |
| 4 | 미완료 기록 상세 | 결과 카드 없음, `pending` 상태, "진행 상황 보기" 링크 |
| 5 | 없는 ID(`/recordings/99999`) | "요청한 정보를 찾을 수 없습니다." + 목록 링크 |
| 6 | 분석 중 기록 삭제 | 409 안내, **기록이 남아 있다** |
| 7 | 완료 기록 삭제 | 204 → 목록으로 이동, 목록에서 사라짐 |
| 8 | 삭제된 기록 URL 재접속 | 404 화면 |
| 9 | 개발자도구 | 상세 진입 시 **14 를 호출하지 않는다** |

브라우저 콘솔에서 불변식 확인:

```js
const d = await (await import('/src/api/history.js')).fetchRecordingDetail('101')
console.log(d.basic.fillerTotalCount === d.basic.fillerBreakdown.reduce((s, x) => s + x.count, 0))
// true
```

---

## 8. 완료 기준 (DoD)

- [ ] 상세 진입이 **API 10 한 번**이고 14 를 부르지 않는다
- [ ] 미완료 건에서 `basic: null` 을 `pending` 상태로 그린다 (오류가 아니다)
- [ ] FREE 에서 잠금 기능 **9개**가 **서버가 준 순서 그대로** 나온다
- [ ] PRO + 완료에서 상세 분석 링크가 **라우터 `name`** 으로 이동한다 (`detailUrl` href 아님)
- [ ] `retryable` 을 프론트에서 재계산하지 않는다
- [ ] 삭제 성공이 **204** 이고 목록으로 이동한다
- [ ] 삭제 409 에서 기록이 남아 있다
- [ ] 재삭제·없는 ID 가 모두 **404** 로 같게 표시된다
- [ ] `fillerBreakdown` 을 재정렬하지 않는다

커밋 예시:

```
feat(fe): 기록 상세·결과·삭제 화면과 PRO 잠금 카드 구현

상세는 API 10 한 번으로 그린다. 진입 시 14 를 같이 부르면
미완료 기록에서 409 ANALYSIS_NOT_COMPLETED 가 떠 정상 화면에 오류가 뜬다.
pro.detailUrl 은 /api/v1 API 경로라 href 로 쓰지 않고 라우터 name 으로 이동한다.
```

---

# A7 — 추이 통계 화면 (API 16)

우선순위 **P1** · 담당 **A** · 선행 **Step 0** · 브랜치 `feat/fe-trends`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-trends

docker compose down -v && docker compose up -d db backend

cd frontend && npm run dev
```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 인증 | 성공 | data |
| --- | --- | --- | --- | --- | --- | --- |
| 16 | DASH_009 | GET | `/dashboard/trends` | Access JWT | 200 | `Trends` |

**FREE 도 이용 가능합니다.** PRO 전용이 아닙니다(A8 과 다른 점).
여기서 만드는 **SVG 라인 차트 컴포넌트를 A8 이 재사용**합니다.

---

## 3. 명세 근거

- 《공개 API 상세》 API 16
- 《DTO 사전》 → `Trends`, `DailyPoint`, `Period`
- 《이력·통계·비교 계약》 2~3문단
- 오류: `VALIDATION_ERROR`

---

## 4. 소유 파일

```
src/api/stats.js                          API 16·18·19 (18·19 는 A8)
src/composables/useSeoulDate.js           한국 오늘·월요일 계산. A8 이 재사용
src/views/stats/TrendsView.vue
src/components/chart/LineChart.vue        ← A 단독 소유. A8 이 재사용
src/components/chart/ChartAxis.vue
src/components/stats/PeriodPicker.vue
```

---

## 5. 구현 지침

### 5-1. `api/stats.js`

```js
import { client, unwrap } from './client'

export const fetchTrends = (params) =>
  unwrap(client.get('/dashboard/trends', { params }))
// 18·19 는 A8 에서 추가한다
```

`params` 는 `{ period }` 또는 `{ period: 'CUSTOM', startDate, endDate }`.
**`CUSTOM` 이 아닐 때 `startDate`/`endDate` 를 같이 보내면 422** 입니다.
`undefined` 인 키는 axios 가 알아서 뺍니다 → `null` 을 넣지 마세요(`null` 은 실려 갑니다).

### 5-2. 기간 선택

| `period` | 범위 | 필수 파라미터 |
| --- | --- | --- |
| `WEEK` (기본) | 요청 시점 한국 날짜를 **포함한** 최근 **7일** | 없음 |
| `MONTH` | 최근 **30일** | 없음 |
| `CUSTOM` | `startDate` ~ `endDate` | **둘 다 필수** |

`CUSTOM` 검증(프론트에서 미리 막습니다):

- `startDate ≤ endDate ≤ 한국 오늘`
- 양끝 포함 **최대 366일**

```js
// composables/useSeoulDate.js
const KST_OFFSET_MIN = 9 * 60

/** 한국 기준 오늘을 YYYY-MM-DD 문자열로. Date 연산을 KST 로 하지 않는다. */
export function seoulToday() {
  const now = new Date()
  const kst = new Date(now.getTime() + (KST_OFFSET_MIN + now.getTimezoneOffset()) * 60000)
  return toYmd(kst)
}

export const toYmd = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

/** 양끝 포함 일수 */
export function daysBetween(startYmd, endYmd) {
  const ms = Date.parse(`${endYmd}T00:00:00Z`) - Date.parse(`${startYmd}T00:00:00Z`)
  return Math.floor(ms / 86400000) + 1
}
```

> **`new Date('2026-09-03')` 은 UTC 자정으로 파싱됩니다.** 한국 시간대 브라우저에서
> `getDate()` 를 부르면 **3일이 아니라 3일 09:00** 이라 괜찮지만, 시간대가 UTC-5 인 환경에서는
> **2일**이 됩니다. 날짜 문자열은 문자열로 다루고, 꼭 비교해야 하면 `Date.parse(...+'T00:00:00Z')`
> 처럼 **UTC 로 못 박아** 계산하세요 (§C4).

### 5-3. `DailyPoint` 렌더링 — **FC5 가 걸리는 자리**

```
date               : Asia/Seoul 날짜 (YYYY-MM-DD)
practiceCount      : 그날 완료 녹음 수
averageFillerCount : 그 녹음들의 추임새 평균. 0건이면 null
```

- **범위 안의 모든 날짜가 옵니다.** 미연습일은 `practiceCount: 0`, `averageFillerCount: null`.
- **`null` 을 `0` 으로 그리면 그래프가 바닥을 칩니다.** "그날 추임새가 0회였다"와
  "그날 연습을 안 했다"는 다릅니다.
- 라인 차트에서 `null` 인 날은 **점을 찍지 않고 선을 끊습니다.**

### 5-4. `LineChart.vue` — 의존성 없이 SVG 로

```vue
<script setup>
import { computed } from 'vue'

const props = defineProps({
  points: { type: Array, required: true },   // [{ date, value|null, label }]
  height: { type: Number, default: 220 },
})

const W = 720
const PAD = { top: 16, right: 16, bottom: 28, left: 36 }

const values = computed(() => props.points.map((p) => p.value).filter((v) => v != null))
const maxY = computed(() => Math.max(1, ...values.value))

const x = (i) => PAD.left + (i * (W - PAD.left - PAD.right)) / Math.max(1, props.points.length - 1)
const y = (v) => props.height - PAD.bottom
  - (v / maxY.value) * (props.height - PAD.top - PAD.bottom)

/** null 에서 선을 끊는다 → "연습 안 한 날"과 "추임새 0회"는 다르다 */
const segments = computed(() => {
  const out = []
  let cur = []
  props.points.forEach((p, i) => {
    if (p.value == null) { if (cur.length > 1) out.push(cur); cur = []; return }
    cur.push([x(i), y(p.value)])
  })
  if (cur.length > 1) out.push(cur)
  return out.map((seg) => seg.map(([px, py]) => `${px},${py}`).join(' '))
})

/** 점 하나만 있는 구간은 선이 안 그려지므로 점으로 찍는다 */
const dots = computed(() =>
  props.points.map((p, i) => (p.value == null ? null : { cx: x(i), cy: y(p.value), ...p }))
    .filter(Boolean))
</script>

<template>
  <svg class="chart" :viewBox="`0 0 ${W} ${height}`" role="img"
       :aria-label="`추이 그래프, ${points.length}일`">
    <line :x1="PAD.left" :y1="height - PAD.bottom" :x2="W - PAD.right" :y2="height - PAD.bottom"
          stroke="var(--color-border)" />
    <polyline v-for="(pts, i) in segments" :key="i" :points="pts"
              fill="none" stroke="var(--color-primary)" stroke-width="2" />
    <circle v-for="(d, i) in dots" :key="`d${i}`" :cx="d.cx" :cy="d.cy" r="3"
            fill="var(--color-primary)" />
    <text v-for="(p, i) in points" :key="`t${i}`" :x="x(i)" :y="height - 8"
          text-anchor="middle" class="tick">{{ p.label }}</text>
  </svg>
</template>

<style scoped>
.chart { width: 100%; height: auto; }
.tick { font-size: 10px; fill: var(--color-text-muted); }
</style>
```

**이건 최소안입니다.** 축 라벨 밀도, 툴팁, 반응형 처리는 화면에 맞게 조정하세요.
핵심은 두 가지입니다 — **`null` 에서 선을 끊을 것**, **라이브러리를 추가하지 않을 것**.

### 5-5. 응답의 다른 필드

- `timezone` 은 항상 `"Asia/Seoul"`. 화면 어딘가에 "한국 시간 기준" 으로 표시합니다.
- `algorithmVersion` 은 `"speech-habits-v1"`. 통계 기준을 밝히는 값으로 씁니다.
  다른 버전 결과는 **집계에서 빠져 있습니다** → "이전 버전 기록은 제외됩니다" 라고 안내하면 정확합니다.

---

## 6. 함정

- **`averageFillerCount: null` 을 0으로 그리지 마세요** (FC5). 선을 끊습니다.
- **빈 날짜를 빼지 마세요.** 서버가 범위 안의 모든 날짜를 줍니다. 그대로 다 그려야 간격이 맞습니다.
- **`CUSTOM` 이 아닌데 `startDate`/`endDate` 를 보내면 422** 입니다.
  `null` 대신 키를 아예 빼세요(`undefined`).
- **367일을 보내면 422** 입니다. 프론트에서 미리 막습니다.
- **미래 날짜를 보내면 422** 입니다. 날짜 입력의 `max` 를 한국 오늘로 잡으세요.
- **WEEK 는 "이번 주 월~일" 이 아닙니다.** 오늘을 포함한 최근 7일입니다.
  월요일 기준 주는 A8 의 주간 리포트입니다.
- **`new Date('YYYY-MM-DD')` 로 라벨을 만들지 마세요.** UTC 로 파싱돼 시간대에 따라 하루 밀립니다.
  `formatDayLabel()` 로 문자열을 잘라 씁니다.
- **차트 라이브러리를 추가하지 마세요** (`frontend/AGENTS.md` §7).

---

## 7. 검증

> 백엔드 API 16 구현 후 수행합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | `/trends` 진입 | 점 **7개**, `WEEK`, "한국 시간 기준" 표시 |
| 2 | MONTH 선택 | 점 **30개** |
| 3 | CUSTOM `2026-08-28` ~ `2026-09-03` | 점 7개, 첫 라벨 08/28, 끝 09/03 |
| 4 | CUSTOM 에서 날짜 하나만 입력 | 조회 버튼 비활성 (422 를 보내지 않는다) |
| 5 | 미래 날짜 선택 시도 | 입력에서 막힌다 |
| 6 | 366일 / 367일 | 366 통과, 367 은 조회 버튼 비활성 |
| 7 | 연습 없는 날 | 점이 **없고 선이 끊긴다** (0으로 바닥을 치지 않는다) |
| 8 | FREE 계정 | 200, 점 전부 표시 (PRO 전용이 아니다) |
| 9 | Network 탭 | `WEEK` 요청에 `startDate` 가 **실리지 않는다** |

```bash
# DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select (r.submitted_at at time zone 'Asia/Seoul')::date d, count(*)
     from recordings r join analyses a on a.recording_id = r.id
    where a.status='COMPLETED' and a.algorithm_version='speech-habits-v1'
    group by 1 order by 1;"
# 화면의 practiceCount 와 일치해야 한다
```

---

## 8. 완료 기준 (DoD)

- [ ] WEEK 7개 · MONTH 30개 · CUSTOM 최대 366개 점이 그려진다
- [ ] 미연습일이 **점 없고 선 끊김**으로 표시된다 (0이 아니다)
- [ ] `CUSTOM` 이 아닐 때 날짜 파라미터를 **보내지 않는다**
- [ ] 날짜 누락 / 미래 / 367일을 **프론트에서 먼저 막는다**
- [ ] 날짜 라벨이 문자열 처리라 시간대에 따라 밀리지 않는다
- [ ] `timezone` 과 `algorithmVersion` 이 화면에 드러난다
- [ ] FREE 계정에서 정상 동작한다
- [ ] 차트 라이브러리 의존성이 **없다**
- [ ] `loading / error / empty` 3종이 처리돼 있다

커밋 예시:

```
feat(fe): 추임새 추이 통계 화면 구현

averageFillerCount 가 null 인 날은 점을 안 찍고 선을 끊는다.
0 으로 채우면 "연습 안 한 날"이 "추임새 0회인 날"로 보인다.
날짜는 서버가 준 KST 문자열을 문자열로 다룬다. Date 로 파싱하면 시간대에 따라 하루 밀린다.
```

---

# A8 — 기록 비교·주간 리포트 (API 18 · 19)

우선순위 **P2** · 담당 **A** · 선행 **A7**(`LineChart`·`useSeoulDate` 재사용) · 브랜치 `feat/fe-stats-pro`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-stats-pro

docker compose down -v && docker compose up -d db backend
docker compose exec db psql -U postgres -d miniproject -c "select id, email, plan from users;"

cd frontend && npm run dev
```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 인증 | 성공 | data |
| --- | --- | --- | --- | --- | --- | --- |
| 18 | PRO_008 | GET | `/recordings/{recordingId}/compare` | Access JWT **+ PRO** | 200 | `Comparison` |
| 19 | PRO_009 | GET | `/dashboard/weekly-report` | Access JWT **+ PRO** | 200 | `WeeklyReport` |

**둘 다 PRO 전용**입니다(A7 추이와 다른 점).

---

## 3. 명세 근거

- 《공개 API 상세》 API 18 · 19
- 《DTO 사전》 → `Comparison`, `ComparisonItem`, `ComparisonDelta`, `WeeklyReport`, `DailyPoint`
- 《이력·통계·비교 계약》 4~5문단
- 오류: `VALIDATION_ERROR` · `RESOURCE_NOT_FOUND` · `PRO_REQUIRED` · `ANALYSIS_NOT_COMPLETED` ·
  `COMPARISON_TARGET_NOT_FOUND` · `ANALYSIS_VERSION_MISMATCH`

---

## 4. 소유 파일

```
src/api/stats.js                             18·19 추가 (A7 파일)
src/views/stats/CompareView.vue
src/views/stats/WeeklyReportView.vue
src/components/stats/DeltaBadge.vue
src/components/stats/WeekPicker.vue
```

`components/chart/LineChart.vue` 와 `composables/useSeoulDate.js` 는 **A7 것을 그대로 재사용**합니다.
복사하지 마세요.

---

## 5. 구현 지침 — API 18 비교

### 5-1. 호출

```js
export const fetchComparison = (recordingId, targetRecordingId) =>
  unwrap(client.get(`/recordings/${recordingId}/compare`,
    { params: targetRecordingId ? { targetRecordingId } : undefined }))

export const fetchWeeklyReport = (weekStartDate) =>
  unwrap(client.get('/dashboard/weekly-report',
    { params: weekStartDate ? { weekStartDate } : undefined }))
```

### 5-2. 대상 선택

| `targetRecordingId` | 동작 |
| --- | --- |
| **생략** | 서버가 같은 알고리즘 버전의 **직전 완료 기록**을 자동으로 고른다 |
| **명시** | 본인 소유 + COMPLETED + 같은 버전 + **현재보다 이전** 이어야 한다 |

- **자기 자신을 대상으로 지정하면 422** 입니다. 선택 목록에서 현재 기록을 빼세요.
- **현재보다 이후 기록을 지정해도 422** 입니다. 선택 목록을 `submittedAt` 이 더 이른 것으로 제한합니다.
- **기본은 생략**입니다. 대부분의 화면은 "직전 기록과 비교"만 보여주면 됩니다.

### 5-3. 변화량 — 부호를 뒤집지 마세요

```
fillerCountChange       = current - target
silenceDurationMsChange = current - target
wordsPerMinuteChange    = current - target,  단 한쪽이라도 null 이면 null
```

**추임새가 줄었으면 음수입니다.** 명세 예시가 `-2` 입니다.
"좋아졌다"를 양수로 만들려고 부호를 뒤집으면 안 됩니다.

```vue
<!-- DeltaBadge: 값의 부호는 그대로 두고, 좋고 나쁨은 색으로만 표현한다 -->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: null },
  // 이 지표는 줄어드는 게 좋은가? (추임새·침묵은 true, WPM 은 판단하지 않음)
  lowerIsBetter: { type: Boolean, default: true },
  unit: { type: String, default: '' },
})
const tone = computed(() => {
  if (props.value == null || props.value === 0) return 'flat'
  const improved = props.lowerIsBetter ? props.value < 0 : props.value > 0
  return improved ? 'good' : 'bad'
})
</script>

<template>
  <span :class="['delta', `delta--${tone}`]">
    {{ value == null ? '—' : `${value > 0 ? '+' : ''}${value}${unit}` }}
  </span>
</template>
```

- `wordsPerMinuteChange` 는 **좋고 나쁨을 판단하지 않습니다.** 너무 빠른 것도 느린 것도 문제라
  중립 색으로 둡니다.
- **한쪽 WPM 이 `null` 이면 변화량도 `null`** 입니다. `—` 로 표시합니다.
- `durationMs` 를 두 항목에 함께 보여줍니다. 길이가 다른 녹음을 비교할 때
  **단순 절대 변화가 길이 차이의 영향을 받는다**는 걸 화면이 스스로 알려 줘야 합니다.

### 5-4. 오류 분기 — 코드가 비슷해서 헷갈립니다

| 코드 | 상태 | 뜻 | 화면 |
| --- | --- | --- | --- |
| `RESOURCE_NOT_FOUND` | 404 | **현재 기록**이 없다 | 목록으로 |
| `COMPARISON_TARGET_NOT_FOUND` | 404 | **비교 대상**이 없다 | "비교할 이전 기록이 없습니다" + 첫 연습 안내 |
| `ANALYSIS_VERSION_MISMATCH` | 409 | 분석 기준이 다르다 | "분석 기준이 달라 비교할 수 없습니다" |
| `ANALYSIS_NOT_COMPLETED` | 409 | 현재 기록이 미완료 | 진행 화면 링크 |
| `PRO_REQUIRED` | 403 | 등급 부족 | `/upgrade` 유도 |

**첫 녹음에서는 `COMPARISON_TARGET_NOT_FOUND` 가 정상**입니다. 오류 화면이 아니라
"아직 비교할 기록이 없어요" 안내로 그립니다.

---

## 6. 구현 지침 — API 19 주간 리포트

### 6-1. 주 경계

- `weekStartDate` 는 **반드시 월요일**이고 **한국 오늘 이하**여야 합니다. 아니면 422.
- 생략하면 **현재 한국 주의 월요일**입니다.
- `weekEndDate` 는 그 주 일요일입니다. 서버가 줍니다.

```js
/** 한국 기준 이번 주 월요일 (YYYY-MM-DD) */
export function seoulWeekStart(ymd = seoulToday()) {
  const d = new Date(`${ymd}T00:00:00Z`)          // UTC 로 못 박는다
  const dow = (d.getUTCDay() + 6) % 7             // 월=0 … 일=6
  d.setUTCDate(d.getUTCDate() - dow)
  return d.toISOString().slice(0, 10)
}
```

**월요일이 아닌 날짜를 가까운 월요일로 보정하지 마세요.** 명세는 422 를 요구합니다.
`<input type="date">` 만 두면 사용자가 화요일을 고를 수 있으니, **주 단위 선택 UI**(WeekPicker)로
애초에 월요일만 고르게 만드는 게 맞습니다.

### 6-2. `isPartial` 과 `trend`

- `trend` 는 **항상 월~일 7개**입니다. 아직 오지 않은 날도 `practiceCount: 0`,
  `averageFillerCount: null` 로 채워져 옵니다.
- 진행 중인 주면 `isPartial: true` → **"진행 중인 주입니다"** 배지를 답니다.
  이게 없으면 이번 주 평균이 낮게 나온 이유를 사용자가 오해합니다.
- 미래 날짜 구간은 시각적으로 흐리게 처리합니다. A7 의 `LineChart` 를 그대로 씁니다.

### 6-3. 개선율

```
improvementRatePercent = (이전 주 평균 - 선택 주 평균) / 이전 주 평균 × 100
```

- **양수 = 개선**입니다(추임새가 줄었다). 부호를 뒤집지 마세요.
- **음수가 나와도 0으로 자르지 마세요.** 악화도 사실입니다.
- `null` 이 되는 경우가 **3가지**이고, 화면 문구가 각각 달라야 합니다:

| 조건 | 문구 |
| --- | --- |
| 선택 주 0건 | "이번 주 연습 기록이 없습니다." |
| 이전 주 0건 | "지난주 기록이 없어 비교할 수 없습니다." |
| 이전 주 평균이 0 | "지난주 추임새가 0회라 개선율을 계산할 수 없습니다." |

응답에는 `practiceCount`·`averageFillerCount` 가 함께 오므로 세 경우를 프론트에서 구분할 수 있습니다.

---

## 7. 함정

- **개선율의 부호를 뒤집지 마세요.** 양수가 개선입니다.
- **음수 개선율을 0으로 자르지 마세요.**
- **`improvementRatePercent: null` 을 "0%"로 표시하지 마세요.** 3가지 조건을 구분해 안내합니다.
- **`weekStartDate` 를 가까운 월요일로 보정하지 마세요.** 422 가 정답입니다. UI 로 막으세요.
- **`COMPARISON_TARGET_NOT_FOUND` 는 `RESOURCE_NOT_FOUND` 와 다릅니다.** 문구를 나누세요.
- **`ANALYSIS_VERSION_MISMATCH` 는 대상을 명시했을 때만** 납니다. 자동 선택은 같은 버전만 고릅니다.
- **`wordsPerMinuteChange` 에 좋고 나쁨 색을 치지 마세요.**
- **A7 의 `LineChart` 를 복사하지 마세요.** import 해서 씁니다 (같은 담당의 파일이므로 자유).
- **WEEK(A7, 최근 7일)와 주간 리포트(월~일)는 다른 개념입니다.** 경계 계산을 공유하지 마세요.
- **403 을 라우터 가드로 미리 막지 마세요** (§C9). 서버 응답으로 처리합니다.

---

## 8. 검증

> 백엔드 API 18·19 구현 후 수행합니다.

```bash
PRO 계정: pro@example.com / P@ssw0rd123
FREE 계정: user@example.com / P@ssw0rd123
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | PRO 로 `/recordings/101/compare` | 현재 101, 대상 88, 델타 표시 |
| 2 | 1번의 추임새 델타 | **음수면 "줄었다"** 로 보이고 값은 `-2` 그대로 |
| 3 | PRO 로 `/recordings/88/compare` (첫 기록) | "비교할 이전 기록이 없습니다" **안내** (오류 화면 아님) |
| 4 | FREE 로 1번 | 업그레이드 안내 (`PRO_REQUIRED`) |
| 5 | 없는 기록으로 compare | "요청한 정보를 찾을 수 없습니다" |
| 6 | PRO 로 `/weekly-report` | 월~일 **7개** 점, `isPartial` 배지 |
| 7 | 화요일을 주 시작으로 고르려 시도 | UI 에서 막힌다 (422 를 보내지 않는다) |
| 8 | 기록 없는 과거 주 선택 | 200, 평균 `—`, 개선율 `—` + **3가지 중 맞는 문구** |
| 9 | 개선율이 음수인 주 | 음수 그대로 표시 |
| 10 | FREE 로 `/weekly-report` | 업그레이드 안내 |

---

## 9. 완료 기준 (DoD)

- [ ] 변화량이 `current - target` 이고 **부호를 뒤집지 않았다**
- [ ] 한쪽 WPM 이 `null` 이면 변화량이 `—` 다
- [ ] `COMPARISON_TARGET_NOT_FOUND` 가 **오류가 아니라 안내**로 보인다
- [ ] `ANALYSIS_VERSION_MISMATCH` 문구가 따로 있다
- [ ] `weekStartDate` 가 **월요일만** 선택되고 보정하지 않는다
- [ ] `trend` 가 **항상 7개**이고 미래 날짜가 `0`/`—` 다
- [ ] 진행 중인 주에 `isPartial` 배지가 달린다
- [ ] 개선율 **양수 = 개선**이고 음수를 0으로 자르지 않는다
- [ ] 개선율 `null` **3가지 조건**의 문구가 각각 다르다
- [ ] 둘 다 FREE 에서 업그레이드 안내가 뜬다 (라우터 가드로 막지 않는다)
- [ ] A7 의 `LineChart`·`useSeoulDate` 를 **재사용**하고 복사하지 않았다

커밋 예시:

```
feat(fe): 기록 비교 화면 구현

변화량은 current - target 이라 추임새가 줄면 음수다. 부호를 뒤집지 않는다.
값의 부호는 그대로 두고 좋고 나쁨은 색으로만 구분한다.

feat(fe): 주간 리포트 화면 구현

개선율이 null 인 조건이 세 가지라 문구를 각각 나눈다.
"이번 주 기록 없음"과 "지난주 기록 없음"을 같은 0% 로 보여주면 사용자가 오해한다.
```
