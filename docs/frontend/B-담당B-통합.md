# 담당 B — 녹음·분석·시각화 (통합본)

이 문서는 `Step 0(공통 기반)`과 `B1~B8` 8개 작업을 **순서대로 이어 붙인 것**입니다.
이 파일 하나만 열면 담당 B의 모든 작업에 착수할 수 있습니다.

구성 순서: Step 0(공통 기반) → B1 → B2 → B3 → B4 → B5 → B6 → B7 → B8

> **B1·B2는 Step 0을 기다리지 않습니다.** 순수 브라우저 API라 Vue·Vite 외 의존성이 0입니다.
> 저장소를 클론한 직후 바로 시작해서 끝까지 갈 수 있습니다.
> B3 이후의 선행 상태는 [`README.md`](README.md) §0을 보세요.

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

cd frontend && npm install
npm install pinia
```

> **선행 필요 — `docker-compose.yml` 미구축.** 아래는 갖춰진 뒤의 절차입니다.
> Step 0의 검증(§8)은 백엔드 없이도 전부 됩니다 — `reissue` 요청이 실패해도 앱이 뜨는 것까지가 이 절의 범위입니다.
>
> ```bash
> cd .. && docker compose down -v && docker compose up -d db backend
> # 대체 경로 (루트 AGENTS.md §4)
> cd backend && ./gradlew bootRun
> ```

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
# 선행 필요 — 루트 .env 가 만들어진 뒤에 의미가 있다
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

# B1 — 브라우저 녹음 composable (MediaRecorder)

우선순위 **P0** · 담당 **B** · 선행 **없음** · 브랜치 `feat/fe-recorder`

> **Step 0 도 백엔드도 기다리지 않습니다.** Vue·Vite 외 의존성이 0인 순수 브라우저 코드입니다.
> 저장소를 클론한 직후 바로 시작할 수 있습니다.

---

## 1. 착수 (그대로 복사)

```bash
node -v                                        # 20 이상
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-recorder

cd frontend && npm install && npm run dev
# 마이크 권한 때문에 http://localhost 또는 https 에서만 동작한다.
# 127.0.0.1 은 되고, LAN IP(192.168.x.x)는 브라우저가 막는다.
```

---

## 2. 목표

명세의 Vue 로컬 처리 항목(`REC_001`~`REC_005`)을 구현한다. 네트워크 호출이 **하나도 없다.**

- 마이크 권한 요청 → 녹음 시작/정지 → `Blob` 산출
- **최대 60초에서 자동 정지**, 1초 미만이면 제출 불가
- 실시간 레벨미터용 진폭 값 (`AnalyserNode`)
- 정지 시 마이크 트랙 해제 (항상 녹음 표시등이 꺼져야 한다)

---

## 3. 명세 근거

- 《분석 규칙 — speech-habits-v1》 규칙 1 → 길이 1000~60000ms
- 《공통 구현 계약》 *음성 제한* · *음성 형식* 절
- 《업로드 규격·Vue 요청 예시》 → multipart 필드는 `audio` 하나
- ERD `ck_recordings_mime` → 허용 MIME 5종

---

## 4. 소유 파일

```
src/composables/useRecorder.js
src/components/recorder/RecordButton.vue
src/components/recorder/LevelMeter.vue
src/components/recorder/RecordTimer.vue
```

`src/constants/audio.js` 는 Step 0 에서 B 가 붙여넣을 파일입니다. 그대로 씁니다.
Step 0 이 아직 없으면 상수를 파일 안에 임시로 두고, Step 0 이후 옮깁니다.

---

## 5. 구현 지침

### 5-1. MIME 선택 — 브라우저마다 다릅니다

Chrome 은 `audio/webm`, Safari 는 `audio/mp4` 만 됩니다.
**서버 허용 목록 안에서** 지원되는 첫 번째를 고릅니다.

```js
import { ALLOWED_MIME } from '@/constants/audio'

/** 브라우저가 실제로 만들 수 있고, 서버도 받는 후보 */
const CANDIDATES = [
  'audio/webm;codecs=opus',   // Chrome, Edge, Firefox
  'audio/webm',
  'audio/mp4',                // Safari
  'audio/ogg;codecs=opus',
]

export function pickMimeType() {
  for (const t of CANDIDATES) {
    if (MediaRecorder.isTypeSupported?.(t)) return t
  }
  return ''   // 빈 문자열이면 브라우저 기본값을 쓴다
}

/** 'audio/webm;codecs=opus' → 'audio/webm'. 서버 허용 목록과 비교할 때 쓴다. */
export const baseMime = (t) => (t || '').split(';')[0].trim().toLowerCase()
```

- **`;codecs=opus` 파라미터가 붙습니다.** 서버는 `;` 앞만 보고 판단하지만,
  프론트에서 허용 목록과 비교할 때도 반드시 잘라야 합니다.
- `isTypeSupported` 가 없는 오래된 브라우저를 대비해 `?.` 를 씁니다.

### 5-2. `useRecorder.js`

```js
import { ref, computed, onUnmounted } from 'vue'
import { MIN_DURATION_MS, MAX_DURATION_MS } from '@/constants/audio'
import { pickMimeType } from '@/utils/audio'    // B2 에서 옮긴다. 지금은 이 파일에 둬도 된다

export function useRecorder() {
  const state = ref('idle')        // idle | requesting | recording | stopping | ready | denied | error
  const elapsedMs = ref(0)
  const level = ref(0)             // 0~1 레벨미터
  const blob = ref(null)
  const mimeType = ref('')
  const error = ref(null)

  let stream = null
  let recorder = null
  let chunks = []
  let audioCtx = null
  let analyser = null
  let rafId = null
  let tickId = null
  let startedAt = 0

  const canSubmit = computed(() =>
    state.value === 'ready' && blob.value && elapsedMs.value >= MIN_DURATION_MS)

  /** 마이크·오디오 컨텍스트·타이머를 전부 놓는다. 어떤 경로로 끝나든 반드시 부른다. */
  function release() {
    cancelAnimationFrame(rafId); rafId = null
    clearInterval(tickId); tickId = null
    // 트랙을 멈추지 않으면 탭의 빨간 녹음 표시등이 계속 켜져 있다
    stream?.getTracks().forEach((t) => t.stop()); stream = null
    audioCtx?.close().catch(() => {}); audioCtx = null
    analyser = null
    recorder = null
  }

  function meter() {
    if (!analyser) return
    const buf = new Uint8Array(analyser.fftSize)
    analyser.getByteTimeDomainData(buf)
    let sum = 0
    for (const v of buf) { const x = (v - 128) / 128; sum += x * x }
    level.value = Math.min(1, Math.sqrt(sum / buf.length) * 3)   // 보기 좋게 3배
    rafId = requestAnimationFrame(meter)
  }

  async function start() {
    if (state.value === 'recording') return
    reset()
    state.value = 'requesting'
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, channelCount: 1 },
      })
    } catch (e) {
      // 권한 거부와 장치 없음을 구분한다 → 사용자가 할 일이 다르다
      state.value = e?.name === 'NotAllowedError' ? 'denied' : 'error'
      error.value = e?.name === 'NotAllowedError'
        ? '마이크 사용이 차단되어 있습니다. 브라우저 주소창의 권한 설정을 확인해 주세요.'
        : '마이크를 찾을 수 없습니다. 장치 연결을 확인해 주세요.'
      release()
      return
    }

    mimeType.value = pickMimeType()
    recorder = new MediaRecorder(stream, mimeType.value ? { mimeType: mimeType.value } : undefined)
    chunks = []
    recorder.ondataavailable = (e) => { if (e.data.size) chunks.push(e.data) }
    recorder.onstop = () => {
      // recorder.mimeType 이 우리가 요청한 것과 다를 수 있다. 실제 값을 쓴다.
      mimeType.value = recorder?.mimeType || mimeType.value
      blob.value = new Blob(chunks, { type: mimeType.value })
      chunks = []
      state.value = 'ready'
      release()
    }

    audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 2048
    audioCtx.createMediaStreamSource(stream).connect(analyser)

    startedAt = performance.now()
    recorder.start(250)          // 250ms 마다 chunk. 안 주면 stop 될 때 한 번에 몰린다
    state.value = 'recording'
    meter()

    tickId = setInterval(() => {
      elapsedMs.value = Math.round(performance.now() - startedAt)
      // 명세: 60초 초과는 거절이고 잘라내지 않는다. 애초에 넘기지 않는다.
      if (elapsedMs.value >= MAX_DURATION_MS) stop()
    }, 100)
  }

  function stop() {
    if (state.value !== 'recording') return
    state.value = 'stopping'
    elapsedMs.value = Math.min(MAX_DURATION_MS, Math.round(performance.now() - startedAt))
    recorder?.stop()             // onstop 에서 blob 을 만들고 release() 한다
  }

  function reset() {
    release()
    state.value = 'idle'
    elapsedMs.value = 0
    level.value = 0
    blob.value = null
    error.value = null
  }

  // FC4 계열 — 화면을 벗어나면 마이크를 반드시 놓는다
  onUnmounted(release)

  return { state, elapsedMs, level, blob, mimeType, error, canSubmit, start, stop, reset }
}
```

### 5-3. 화면 규칙

| 상태 | 버튼 | 표시 |
| --- | --- | --- |
| `idle` | "녹음 시작" | 안내 문구 |
| `requesting` | 비활성 | "마이크 권한을 확인하는 중…" |
| `recording` | "정지" | 경과 시간 + 레벨미터. **60초에 가까우면 색 변화** |
| `ready` | "다시 녹음" / "제출" | 길이·용량. 1초 미만이면 제출 비활성 |
| `denied` | "다시 시도" | 권한 안내 (주소창 자물쇠 아이콘) |

- **경과 시간은 `MM:SS` 로 표시**하고 60초 상한을 함께 보여줍니다 (`0:12 / 1:00`).
- 1초 미만에서 정지하면 "1초 이상 녹음해 주세요" 를 띄우고 제출 버튼을 잠급니다.
- **`recording` 상태에서 라우트를 벗어나려 하면 확인**을 받습니다 (`onBeforeRouteLeave`).

### 5-4. 제출은 이 파일의 일이 아닙니다

`useRecorder` 는 `Blob` 까지만 만듭니다. 업로드는 B3 입니다.
**여기서 `client` 를 import 하지 마세요.** 그러면 Step 0 의존이 생겨 이 작업의 독립성이 깨집니다.

---

## 6. 함정

- **트랙을 `stop()` 하지 않으면** 탭의 녹음 표시등이 계속 켜져 있습니다. 사용자가 무섭게 느낍니다.
  `release()` 를 `onstop`·`onUnmounted`·오류 경로 **전부**에서 부르세요.
- **`recorder.start()` 에 timeslice 를 안 주면** 긴 녹음에서 메모리가 한 번에 몰립니다. `250` 을 줍니다.
- **`recorder.mimeType` 이 요청값과 다를 수 있습니다.** `Blob` 을 만들 때는 실제 값을 쓰세요.
  안 그러면 B3 에서 서버가 `UNSUPPORTED_MEDIA_TYPE` 을 줍니다.
- **Safari 는 `audio/webm` 을 못 만듭니다.** `isTypeSupported` 로 골라야 합니다.
- **`getUserMedia` 는 보안 컨텍스트에서만** 됩니다. `localhost`·`127.0.0.1`·https 만 가능하고
  LAN IP 로 접속하면 조용히 실패합니다. 발표 PC 에서 이걸로 시간을 날리지 마세요.
- **60초를 넘겨 녹음한 뒤 잘라내지 마세요.** 명세: "초과 음성은 자동으로 자르지 않는다."
  애초에 60초에 자동 정지시킵니다.
- **`elapsedMs` 는 참고용입니다.** 진짜 길이는 서버가 ffmpeg 로 디코딩해서 잽니다.
  이 값을 요청에 실어 보내지 마세요 (B3).
- **권한 거부(`NotAllowedError`)와 장치 없음을 구분**하세요. 사용자가 할 일이 다릅니다.

---

## 7. 검증

브라우저에서 `/record` 를 열거나, 화면이 아직 없으면 임시 페이지에 붙여 확인합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 녹음 시작 | 권한 프롬프트 → 타이머가 흐르고 레벨미터가 목소리에 반응 |
| 2 | 3초 뒤 정지 | `state === 'ready'`, `blob.size > 0`, **탭 녹음 표시등이 꺼진다** |
| 3 | 권한 거부 | `denied` 상태 + 권한 안내. 콘솔에 처리 안 된 오류가 없다 |
| 4 | 0.5초만 녹음 | 제출 버튼 비활성 + "1초 이상" 안내 |
| 5 | 65초 녹음 시도 | **60초에서 자동 정지** |
| 6 | 녹음 중 다른 화면으로 이동 | 확인 후 이동하고 **마이크가 해제된다** |
| 7 | 녹음 → 다시 녹음 → 정지 | 이전 chunk 가 섞이지 않는다 (`reset` 이 돈다) |
| 8 | Safari 에서 1번 | `mimeType` 이 `audio/mp4` 로 잡힌다 |

```js
// 브라우저 콘솔에서 후보 확인
['audio/webm;codecs=opus','audio/webm','audio/mp4','audio/ogg;codecs=opus']
  .map(t => [t, MediaRecorder.isTypeSupported(t)])
```

---

## 8. 완료 기준 (DoD)

- [ ] Vue·Vite 외 의존성이 **0** 이고, `api/` 를 import 하지 않는다
- [ ] 정지·화면 이탈·오류 **모든 경로**에서 마이크 트랙이 해제된다
- [ ] **60초에서 자동 정지**하고 초과분을 잘라내지 않는다
- [ ] 1초 미만이면 제출이 잠긴다
- [ ] `MediaRecorder.isTypeSupported` 로 형식을 고르고, `Blob` 에 **실제 `mimeType`** 을 넣는다
- [ ] 권한 거부와 장치 없음이 다른 메시지다
- [ ] 레벨미터가 목소리에 반응하고 `requestAnimationFrame` 이 정리된다
- [ ] 재녹음 시 이전 chunk 가 섞이지 않는다
- [ ] `elapsedMs` 를 서버로 보내지 않는다 (B3 에서 확인)

커밋 예시:

```
feat(fe): 브라우저 녹음 composable 구현 (MediaRecorder)

명세는 60초 초과를 자르지 않고 거절하므로 60초에서 자동 정지시킨다.
브라우저마다 만들 수 있는 형식이 달라 isTypeSupported 로 고른다.
Safari 는 audio/webm 을 만들지 못한다.
정지·이탈·오류 모든 경로에서 트랙을 stop 한다. 안 그러면 탭 녹음 표시등이 꺼지지 않는다.
```

---

# B2 — 오디오 검증 유틸과 파형 렌더러

우선순위 **P0** · 담당 **B** · 선행 **없음** · 브랜치 `feat/fe-audio-utils`

> **Step 0 도 백엔드도 기다리지 않습니다.** 여기서 만드는 파형 렌더러를 **B3·B6·B7 이 전부 재사용**합니다.

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-audio-utils

cd frontend && npm run dev
```

---

## 2. 목표

1. **오디오 사전 검증** — 형식·용량·길이. 서버로 보내기 전에 거를 수 있는 건 거른다.
2. **파형 렌더러** — `{ timeMs, amplitude, type }` 배열을 Canvas 에 그린다. 최대 600점.

프론트 검증은 **편의일 뿐이고 서버 검증이 진짜**입니다
(`frontend/AGENTS.md` §5). 프론트에서 막았다고 서버 오류 처리를 빼지 마세요.

---

## 3. 명세 근거

- 《분석 규칙 — speech-habits-v1》 규칙 1 · 규칙 10 (100ms 구간, 최대 600점, SPEECH/SILENCE)
- 《공통 구현 계약》 *음성 형식* 절
- 《DTO 사전》 → `WaveformPoint`, `SpeechInterval`
- 오류: `UNSUPPORTED_MEDIA_TYPE`(415) · `FILE_TOO_LARGE`(413) · `INVALID_AUDIO`(422) ·
  `AUDIO_DURATION_OUT_OF_RANGE`(422)

---

## 4. 소유 파일

```
src/utils/audio.js                             MIME 정규화·용량·길이 측정
src/composables/useAudioFile.js                파일 선택 + 검증 (녹음 대신 업로드하는 경로)
src/components/waveform/WaveformCanvas.vue     ← B3·B6·B7 이 재사용
src/components/waveform/SpeechIntervalBar.vue
```

---

## 5. 구현 지침

### 5-1. `utils/audio.js`

```js
import { ALLOWED_MIME, MAX_FILE_BYTES, MIN_DURATION_MS, MAX_DURATION_MS } from '@/constants/audio'

/** 'audio/webm;codecs=opus' → 'audio/webm'. 서버도 ';' 앞만 보고 판단한다. */
export const baseMime = (t) => (t || '').split(';')[0].trim().toLowerCase()

export const isAllowedMime = (t) => ALLOWED_MIME.includes(baseMime(t))

/** 확장자로 MIME 을 보정한다. 파일 선택 경로에서 type 이 빈 문자열로 오는 브라우저가 있다. */
const EXT_MIME = {
  webm: 'audio/webm', mp4: 'audio/mp4', m4a: 'audio/mp4',
  ogg: 'audio/ogg', oga: 'audio/ogg', wav: 'audio/wav', mp3: 'audio/mpeg',
}
export function resolveMime(file) {
  if (isAllowedMime(file.type)) return baseMime(file.type)
  const ext = file.name?.split('.').pop()?.toLowerCase()
  return EXT_MIME[ext] ?? baseMime(file.type)
}

/**
 * 길이를 브라우저에서 재 본다. 어디까지나 사전 확인이다.
 * 진짜 길이는 서버가 ffmpeg 로 16kHz mono PCM 디코딩해서 잰다 → 명세가 클라이언트 값을 받지 않는다.
 */
export function probeDurationMs(blob) {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(blob)
    const el = new Audio()
    const done = (v) => { URL.revokeObjectURL(url); resolve(v) }
    el.preload = 'metadata'
    el.onloadedmetadata = () => {
      // MediaRecorder 로 만든 webm 은 duration 이 Infinity 로 오는 알려진 버그가 있다
      const d = el.duration
      done(Number.isFinite(d) && d > 0 ? Math.round(d * 1000) : null)
    }
    el.onerror = () => done(null)
    el.src = url
  })
}

/** 제출 전 검사. 통과하지 못하면 { code, message } 를 돌려준다 (서버 오류 코드와 같은 이름). */
export async function validateAudio(blobOrFile) {
  const mime = resolveMime(blobOrFile)

  if (!isAllowedMime(mime)) {
    return { code: 'UNSUPPORTED_MEDIA_TYPE', message: '지원하지 않는 음성 형식입니다.' }
  }
  if (blobOrFile.size === 0) {
    return { code: 'INVALID_AUDIO', message: '음성 파일을 읽을 수 없습니다. 다시 녹음해 주세요.' }
  }
  if (blobOrFile.size > MAX_FILE_BYTES) {
    return { code: 'FILE_TOO_LARGE', message: '파일이 너무 큽니다. 16MB 이하로 올려 주세요.' }
  }

  const ms = await probeDurationMs(blobOrFile)
  // 길이를 못 재는 경우가 정상적으로 있다(webm duration=Infinity). 그때는 서버 판정에 맡긴다.
  if (ms != null && (ms < MIN_DURATION_MS || ms > MAX_DURATION_MS)) {
    return { code: 'AUDIO_DURATION_OUT_OF_RANGE', message: '1초 이상 60초 이하로 녹음해 주세요.' }
  }
  return null
}
```

- **`MediaRecorder` 로 만든 webm 은 `duration` 이 `Infinity`** 로 나오는 브라우저 버그가 있습니다.
  길이를 못 재는 건 오류가 아니라 정상 상황입니다. **막지 말고 서버에 맡기세요.**
- 오류 코드 이름을 **서버와 같게** 씁니다. 그래야 B3 에서 프론트 검증과 서버 응답을 한 경로로 처리합니다.

### 5-2. `WaveformCanvas.vue` — 최대 600점, SPEECH/SILENCE 색 구분

`WaveformPoint` 는 `{ timeMs, amplitude, type }` 이고 `type` 은 `SPEECH` 또는 `SILENCE` 입니다.

```vue
<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  // [{ timeMs, amplitude(0~1), type: 'SPEECH'|'SILENCE' }] — 최대 600점
  points: { type: Array, default: () => [] },
  height: { type: Number, default: 120 },
  // 재생 위치 표시가 필요하면 (없으면 안 그림)
  cursorMs: { type: Number, default: null },
})

const canvas = ref(null)
let ro = null

function draw() {
  const el = canvas.value
  if (!el) return
  const dpr = window.devicePixelRatio || 1
  const w = el.clientWidth
  const h = props.height
  el.width = Math.floor(w * dpr)
  el.height = Math.floor(h * dpr)

  const ctx = el.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, w, h)

  const n = props.points.length
  if (!n) return

  const css = getComputedStyle(document.documentElement)
  const speech = css.getPropertyValue('--color-speech').trim() || '#2f6df6'
  const silence = css.getPropertyValue('--color-silence').trim() || '#c9ced6'

  const barW = w / n
  const mid = h / 2

  props.points.forEach((p, i) => {
    ctx.fillStyle = p.type === 'SPEECH' ? speech : silence
    // amplitude 는 0~1. 최소 1px 은 그려서 침묵 구간도 눈에 보이게 한다.
    const barH = Math.max(1, p.amplitude * (h - 4))
    ctx.fillRect(i * barW, mid - barH / 2, Math.max(1, barW - 0.5), barH)
  })

  if (props.cursorMs != null && n > 1) {
    const total = props.points[n - 1].timeMs + 100
    const x = (props.cursorMs / total) * w
    ctx.fillStyle = css.getPropertyValue('--color-text').trim() || '#000'
    ctx.fillRect(x, 0, 1, h)
  }
}

onMounted(() => {
  draw()
  // 컨테이너 폭이 바뀌면 다시 그린다. window resize 만 보면 레이아웃 변화를 놓친다.
  ro = new ResizeObserver(draw)
  ro.observe(canvas.value)
})
onUnmounted(() => ro?.disconnect())
watch(() => [props.points, props.cursorMs], draw, { deep: true })
</script>

<template>
  <canvas ref="canvas" class="waveform" :style="{ height: `${height}px` }"
          role="img" :aria-label="`음성 파형, ${points.length}개 구간`" />
</template>

<style scoped>
.waveform { display: block; width: 100%; background: var(--color-surface);
  border-radius: var(--radius-1); }
</style>
```

- **`devicePixelRatio` 를 안 쓰면 고해상도 화면에서 흐려집니다.**
- **`ResizeObserver`** 로 컨테이너 폭 변화를 잡습니다. `window.resize` 만 보면 사이드바 토글 등을 놓칩니다.
- 색은 **CSS 변수에서 읽습니다.** Canvas 는 CSS 를 못 받으니 `getComputedStyle` 로 꺼냅니다.
- **600점을 샘플링해서 줄이지 마세요.** 서버가 이미 최대 600점으로 만들어 줍니다.

### 5-3. `SpeechIntervalBar.vue` — 발화 구간 띠

`speechIntervals` 는 `[{ startMs, endMs }]` 입니다. 전체 길이 대비 위치로 그립니다.

```vue
<script setup>
const props = defineProps({
  intervals: { type: Array, default: () => [] },
  durationMs: { type: Number, required: true },
})
const pct = (v) => `${(v / props.durationMs) * 100}%`
</script>

<template>
  <div class="track" role="img" aria-label="발화 구간">
    <span v-for="(iv, i) in intervals" :key="i" class="seg"
          :style="{ left: pct(iv.startMs), width: pct(iv.endMs - iv.startMs) }" />
  </div>
</template>

<style scoped>
.track { position: relative; height: 10px; background: var(--color-silence);
  border-radius: 999px; overflow: hidden; }
.seg { position: absolute; top: 0; bottom: 0; background: var(--color-speech); }
</style>
```

> `style` 바인딩으로 위치를 넣는 건 **동적 레이아웃이라 예외**입니다.
> `frontend/AGENTS.md` §7 이 금지하는 건 **색·간격을 인라인으로 박는 것**입니다.

### 5-4. `useAudioFile.js` — 파일 선택 경로

녹음이 안 되는 환경(마이크 없음)을 위한 대체 경로입니다.

```js
import { ref } from 'vue'
import { validateAudio } from '@/utils/audio'

export function useAudioFile() {
  const file = ref(null)
  const error = ref(null)

  async function select(f) {
    error.value = null
    file.value = null
    const bad = await validateAudio(f)
    if (bad) { error.value = bad; return }
    file.value = f
  }
  return { file, error, select }
}
```

`<input type="file" accept="audio/webm,audio/mp4,audio/ogg,audio/wav,audio/mpeg">` 를 씁니다.
`accept="audio/*"` 는 서버가 안 받는 형식(예: `audio/aac`)까지 열어 줍니다.

---

## 6. 함정

- **`audio/webm` 의 `duration` 이 `Infinity` 로 나옵니다.** 길이를 못 재는 걸 실패로 처리하면
  정상 녹음이 전부 막힙니다. `null` 이면 서버 판정에 맡기세요.
- **프론트 검증을 이유로 서버 오류 처리를 빼지 마세요** (`frontend/AGENTS.md` §5).
  415·413·422 는 B3 에서 반드시 처리합니다.
- **415 와 422 를 헷갈리지 마세요.**
  - 형식이 허용 목록에 없다 → **415** `UNSUPPORTED_MEDIA_TYPE`
  - 형식은 맞는데 손상·0바이트·디코딩 실패 → **422** `INVALID_AUDIO`
- **`accept="audio/*"` 를 쓰지 마세요.** 허용 5종만 나열합니다.
- **파형을 다시 샘플링하지 마세요.** 서버가 이미 최대 600점입니다.
- **Canvas 색을 하드코딩하지 마세요.** CSS 변수에서 읽습니다.
- **`devicePixelRatio` 를 빼먹으면** Retina 화면에서 파형이 뭉갭니다.
- **업로드한 파일을 `localStorage` 나 전역 변수에 보관하지 마세요** (`frontend/AGENTS.md` §5).
  B5 재시도용 `Blob` 은 **메모리(스토어)에만** 둡니다.

---

## 7. 검증

임시 페이지나 `/dev/mock`(B7) 에서 확인합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 3초 녹음 Blob 을 `validateAudio` | `null` (통과) |
| 2 | 0.5초 wav 파일 | `AUDIO_DURATION_OUT_OF_RANGE` |
| 3 | `.txt` 파일 이름을 `.webm` 으로 바꿔 선택 | 415 또는 서버 422 (프론트에서 못 잡을 수 있음) |
| 4 | 20MB 파일 | `FILE_TOO_LARGE` |
| 5 | 0바이트 파일 | `INVALID_AUDIO` |
| 6 | `audio/aac` 파일 | 파일 선택창에 안 뜬다 (`accept`) |
| 7 | 600점 파형 렌더 | 침묵 구간이 회색, 발화 구간이 파랑 |
| 8 | 브라우저 창 폭 변경 | 파형이 다시 그려지고 흐려지지 않는다 |
| 9 | 다크 모드(있다면) | 색이 CSS 변수를 따라 바뀐다 |

```js
// 콘솔에서 파형 렌더러 확인용 더미
const pts = Array.from({ length: 30 }, (_, i) => ({
  timeMs: i * 100,
  amplitude: i >= 5 && i < 25 ? 0.65 : 0.02,
  type: i >= 5 && i < 25 ? 'SPEECH' : 'SILENCE',
}))
```

---

## 8. 완료 기준 (DoD)

- [ ] `api/` 를 import 하지 않는다 (네트워크 호출 0)
- [ ] MIME 비교가 **`;` 앞만** 본다
- [ ] 길이를 못 재는 경우(`Infinity`)를 **실패로 처리하지 않는다**
- [ ] 415 / 413 / 422 분기가 명세 표와 일치한다
- [ ] 오류 코드 이름이 **서버 코드와 같다**
- [ ] `accept` 에 허용 5종만 나열돼 있다 (`audio/*` 아님)
- [ ] 파형이 `devicePixelRatio` 를 반영하고 `ResizeObserver` 로 다시 그린다
- [ ] 파형 색이 CSS 변수에서 온다
- [ ] 600점을 재샘플링하지 않는다
- [ ] 파일을 `localStorage` 에 보관하지 않는다

커밋 예시:

```
feat(fe): 오디오 사전 검증 유틸과 파형 렌더러 추가

MediaRecorder 로 만든 webm 은 duration 이 Infinity 로 나온다.
길이를 못 재는 걸 실패로 처리하면 정상 녹음이 전부 막히므로 서버 판정에 맡긴다.
파형은 서버가 이미 최대 600점으로 주므로 재샘플링하지 않는다.
```

---

# B3 — 녹음 제출 화면과 멱등키 (API 07)

우선순위 **P0** · 담당 **B** · 선행 **Step 0, B1, B2** · 브랜치 `feat/fe-upload`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-upload

cd frontend && npm run dev
```

> **선행 필요 — 백엔드 API 07 미구현 / `docker-compose.yml` 미구축.**
>
> ```bash
> docker compose down -v && docker compose up -d db backend
> docker compose logs backend | grep -i "started"
> # 대체 경로 (루트 AGENTS.md §4)
> cd backend && ./gradlew bootRun
> ```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 인증 | 성공 | data |
| --- | --- | --- | --- | --- | --- | --- |
| 07 | REC_006 | POST | `/recordings` | Access JWT | **202** | `Accepted` |

헤더: `Idempotency-Key`(UUID) **필수** · `Content-Type: multipart/form-data`
본문: `audio`(file) 필드 **하나뿐**

---

## 3. 명세 근거

- 《공개 API 상세》 API 07
- 《멱등 저장 계약 — PostgreSQL》 규칙 1~5
- 《업로드 규격·Vue 요청 예시》
- 오류: `VALIDATION_ERROR` · `IDEMPOTENCY_KEY_CONFLICT` · `RESOURCE_GONE` · `FILE_TOO_LARGE` ·
  `UNSUPPORTED_MEDIA_TYPE` · `INVALID_AUDIO` · `AUDIO_DURATION_OUT_OF_RANGE` · `REQUEST_TIMEOUT` ·
  `ANALYSIS_ALREADY_ACTIVE` · `ANALYSIS_CAPACITY_EXCEEDED`

---

## 4. 소유 파일

```
src/api/submission.js                    API 07·09 (09 는 B5)
src/utils/idempotency.js
src/stores/submission.js                 재시도용 Blob 보관 → B5 가 읽는다
src/views/practice/RecordView.vue
src/components/recorder/UploadProgress.vue
```

`composables/useRecorder.js`(B1)·`utils/audio.js`·`components/waveform/**`(B2)를 재사용합니다.

---

## 5. 구현 지침

### 5-1. 보내는 것과 **보내지 않는 것**

```
보낸다      : audio (file) 필드
보내지 않는다 : durationSec · durationMs · userId · recordedAt · mimeType
```

명세: "서버는 클라이언트가 보낸 duration 을 받지 않는다." **B1 의 `elapsedMs` 를 실어 보내지 마세요.**
서버가 ffmpeg 로 디코딩해서 직접 잽니다.

### 5-2. `utils/idempotency.js`

```js
/**
 * 멱등 키. 한 번의 "제출 시도"에 하나를 만들고, 네트워크 재전송에도 같은 값을 유지한다.
 * 사용자가 "다시 시도"를 누르는 건 새 시도이므로 새 키를 만든다 (명세 규칙 2).
 */
export const newIdempotencyKey = () =>
  (crypto.randomUUID?.() ?? fallbackUuid())

function fallbackUuid() {
  // crypto.randomUUID 는 보안 컨텍스트에서만 있다. localhost 는 되지만 대비해 둔다.
  const b = crypto.getRandomValues(new Uint8Array(16))
  b[6] = (b[6] & 0x0f) | 0x40
  b[8] = (b[8] & 0x3f) | 0x80
  const h = [...b].map((x) => x.toString(16).padStart(2, '0')).join('')
  return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`
}
```

### 5-3. `api/submission.js` — multipart 요청

```js
import { client, unwrap } from './client'

const EXT = { 'audio/webm': 'webm', 'audio/mp4': 'm4a', 'audio/ogg': 'ogg',
              'audio/wav': 'wav', 'audio/mpeg': 'mp3' }

export function submitRecording(blob, idempotencyKey, onProgress) {
  const form = new FormData()
  // 필드명은 'audio' 하나뿐이다. 파일명은 서버가 질문에서 제외하므로 아무거나 좋다.
  form.append('audio', blob, `recording.${EXT[blob.type.split(';')[0]] ?? 'webm'}`)

  return unwrap(client.post('/recordings', form, {
    headers: {
      // multipart 는 Content-Type 을 브라우저가 boundary 와 함께 붙이게 둔다.
      // 직접 'multipart/form-data' 를 넣으면 boundary 가 빠져 서버가 파싱하지 못한다.
      'Content-Type': undefined,
      'Idempotency-Key': idempotencyKey,
    },
    // 기본 20초로는 부족하다. 업로드 + 서버 ffmpeg 디코딩(최대 10초)이 함께 걸린다.
    timeout: 60000,
    onUploadProgress: onProgress,
  }))
}
```

- **`Content-Type` 을 `undefined` 로 지워야** 브라우저가 `boundary` 를 붙입니다.
  `client.js` 의 기본값이 `application/json` 이라 지우지 않으면 서버가 파일을 못 찾습니다.
- **`timeout` 을 늘립니다.** 기본 20초로는 서버 디코딩까지 못 기다립니다.
- `onUploadProgress` 로 진행률을 받습니다.

### 5-4. `stores/submission.js` — 재시도용 원본 보관

명세: 재시도(API 09)는 **최초와 동일한 바이트**여야 합니다(`AUDIO_MISMATCH`).
그래서 제출한 `Blob` 을 들고 있어야 합니다.

```js
import { defineStore } from 'pinia'

export const useSubmissionStore = defineStore('submission', {
  state: () => ({
    // 재시도용 원본. 메모리에만 둔다 → localStorage 금지 (AGENTS.md §5)
    blob: null,
    analysisId: null,
    recordingId: null,
  }),
  actions: {
    keep({ blob, analysisId, recordingId }) {
      this.blob = blob; this.analysisId = analysisId; this.recordingId = recordingId
    },
    /** 이 분석에 대한 원본을 갖고 있는가 (B5 가 묻는다) */
    hasAudioFor(analysisId) { return Boolean(this.blob) && this.analysisId === analysisId },
    clear() { this.blob = null; this.analysisId = null; this.recordingId = null },
  },
})
```

- **`localStorage` 에 넣지 마세요.** 파일을 저장소에 보관하지 않는 것이 규칙입니다.
- **새로고침하면 사라집니다.** 그건 정상이고, B5 가 "다시 녹음해 주세요" 로 안내합니다.

### 5-5. 제출 흐름

```
① B2 의 validateAudio() 로 사전 검사        → 실패하면 요청을 아예 보내지 않는다
② newIdempotencyKey() 를 한 번 만든다        → 이 시도 동안 고정
③ submitRecording(blob, key, onProgress)
④ 202 → submissionStore.keep(...)          → B5 재시도용
⑤ router.push({ name: 'analysisProgress', params: { analysisId } })
```

```js
const submitting = ref(false)
const progress = ref(0)
const error = ref(null)
let key = null

async function submit(blob) {
  error.value = await validateAudio(blob)
  if (error.value) return

  submitting.value = true
  key ??= newIdempotencyKey()          // 재전송에도 같은 키를 유지한다
  try {
    const data = await submitRecording(blob, key, (e) => {
      progress.value = e.total ? Math.round((e.loaded / e.total) * 100) : 0
    })
    submission.keep({ blob, analysisId: data.analysisId, recordingId: data.recordingId })
    key = null                          // 성공했으니 다음 제출은 새 키
    router.push({ name: 'analysisProgress', params: { analysisId: data.analysisId } })
  } catch (e) {
    error.value = e
  } finally {
    submitting.value = false
  }
}
```

**`key ??=` 가 핵심입니다.** 네트워크 오류로 다시 눌러도 같은 키로 가서
서버가 "같은 요청"으로 인식해 202 를 그대로 돌려줍니다. 새 키를 만들면 두 번 제출됩니다.

### 5-6. 응답

```json
{ "recordingId": "101", "analysisId": "5001", "status": "PENDING",
  "attemptNo": 1, "autoRetryCount": 0 }
```

**ID 는 문자열**입니다 (§C3).

### 5-7. 오류 분기 — 화면 문구가 전부 달라야 합니다

| 코드 | 상태 | 화면 |
| --- | --- | --- |
| `VALIDATION_ERROR` | 422 | 멱등 키 누락 등. 개발 실수라 콘솔에도 남긴다 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 형식 안내 + 다시 녹음 |
| `FILE_TOO_LARGE` | 413 | "16MB 이하" + 다시 녹음 |
| `INVALID_AUDIO` | 422 | "다시 녹음해 주세요" |
| `AUDIO_DURATION_OUT_OF_RANGE` | 422 | "1초 이상 60초 이하" |
| `ANALYSIS_ALREADY_ACTIVE` | 409 | **"이미 진행 중인 분석이 있습니다"** + 진행 화면 링크 |
| `ANALYSIS_CAPACITY_EXCEEDED` | 503 | "잠시 후 다시" + **재시도 버튼**(같은 키로) |
| `IDEMPOTENCY_KEY_CONFLICT` | 409 | 개발 실수. 새 키로 다시 제출 |
| `RESOURCE_GONE` | 410 | 삭제된 기록을 가리키는 키. 새 키로 다시 제출 |
| `REQUEST_TIMEOUT` | 408 | 재시도 버튼 (같은 키로) |

- **`ANALYSIS_ALREADY_ACTIVE` 는 사용자 전체 범위**입니다. 다른 녹음이 분석 중이어도 납니다.
  진행 중인 분석으로 갈 수 있게 링크를 주면 좋지만, **그 `analysisId` 를 응답이 주지 않습니다.**
  `/recordings` 목록으로 보내세요.
- **`ANALYSIS_CAPACITY_EXCEEDED` 는 실패가 아니라 "나중에"** 입니다. 녹음을 버리지 마세요.
  `Blob` 을 그대로 들고 재시도 버튼을 띄웁니다.

---

## 6. 함정

- **`Content-Type` 을 지우지 않으면** boundary 가 빠져 서버가 파일을 못 찾습니다. 가장 자주 나는 실수입니다.
- **`durationSec`·`durationMs` 를 같이 보내지 마세요.** 명세가 받지 않습니다.
- **`Idempotency-Key` 를 매번 새로 만들지 마세요.** 재전송에 같은 키를 유지해야
  "같은 요청에 같은 응답" 계약이 성립합니다.
- **성공한 뒤에는 키를 버리세요.** 다음 녹음에 같은 키를 쓰면 `IDEMPOTENCY_KEY_CONFLICT` 입니다.
- **`timeout` 이 기본 20초면 부족합니다.** 60초로 늘립니다.
- **`Blob` 을 `localStorage` 에 넣지 마세요.** 메모리(스토어)에만 둡니다.
- **503 에서 녹음을 지우지 마세요.** 재시도할 수 있어야 합니다.
- **202 는 "분석 완료"가 아니라 "접수"입니다.** 바로 결과 화면으로 보내면
  비동기 설계가 화면에서 증명되지 않습니다(`frontend/AGENTS.md` §8). **진행 화면으로 보냅니다.**
- **`recordingId`·`analysisId` 를 숫자로 바꾸지 마세요** (§C3).

---

## 7. 검증

> 백엔드 API 07 구현 후 수행합니다.

```bash
# 확인용 음성
ffmpeg -f lavfi -i "sine=frequency=440:duration=3" -c:a libopus /tmp/sample.webm
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 3초 녹음 후 제출 | 202 → `/analyses/{id}` 이동. Network 탭에 `Idempotency-Key` 헤더 |
| 2 | 1번 요청 헤더 | `Content-Type: multipart/form-data; boundary=----...` (boundary 가 있다) |
| 3 | 1번 요청 본문 | `audio` 필드 **하나뿐**, `durationMs` 없음 |
| 4 | 서버를 잠깐 내리고 제출 → 올리고 재시도 | **같은 `Idempotency-Key`** 로 나가고 202 |
| 5 | 분석 진행 중에 새 녹음 제출 | 409 `ANALYSIS_ALREADY_ACTIVE` + 기록 목록 링크 |
| 6 | 0.5초 녹음 제출 | 요청이 **나가지 않고** 프론트에서 막힌다 |
| 7 | 20MB 파일 선택 | 요청이 나가지 않고 "16MB 이하" |
| 8 | 업로드 중 | 진행률이 0→100 으로 움직인다 |
| 9 | 제출 성공 후 다시 녹음·제출 | **다른** `Idempotency-Key` |

```bash
# DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select id, status, attempt_no from analyses order by id desc limit 1;"
docker compose exec db psql -U postgres -d miniproject -c \
  "select expires_at - created_at from api_idempotency_keys;"
# 24:00:00
```

---

## 8. 완료 기준 (DoD)

- [ ] 요청 본문이 **`audio` 필드 하나**뿐이고 duration 계열을 보내지 않는다
- [ ] `Content-Type` 을 지워 **boundary 가 붙는다**
- [ ] `Idempotency-Key` 가 UUID 이고 **재전송에서 유지**된다
- [ ] 성공 후 **새 제출은 새 키**를 쓴다
- [ ] `timeout` 이 60초다
- [ ] 202 가 **진행 화면**으로 이동한다 (결과 화면이 아니다)
- [ ] 재시도용 `Blob` 을 메모리 스토어에만 보관한다
- [ ] 10가지 오류 코드의 화면 문구가 **각각 다르다**
- [ ] 503 에서 녹음이 유지되고 재시도 버튼이 뜬다
- [ ] 사전 검증에 걸린 요청은 **네트워크로 나가지 않는다**
- [ ] ID 를 문자열로 다룬다

커밋 예시:

```
feat(fe): 녹음 제출 화면과 멱등 키 처리 구현

multipart 는 Content-Type 을 지워야 브라우저가 boundary 를 붙인다.
client 기본값이 application/json 이라 지우지 않으면 서버가 파일을 찾지 못한다.
멱등 키는 한 번의 제출 시도에 하나이고 네트워크 재전송에서 유지한다.
매번 새로 만들면 같은 녹음이 두 번 제출된다.
```

---

# B4 — 분석 진행 화면과 상태 폴링 (API 08 · 13)

우선순위 **P0** · 담당 **B** · 선행 **Step 0** · 브랜치 `feat/fe-analysis-status`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-analysis-status

cd frontend && npm run dev
```

> **선행 필요 — 백엔드 API 08·13 미구현 / `db/seed-dev.sql` 미구축.**
> seed 에 PENDING·FAILED·COMPLETED 가 들어오면 네 가지 상태를 전부 검증할 수 있습니다.
>
> ```bash
> docker compose down -v && docker compose up -d db backend
> docker compose exec db psql -U postgres -d miniproject -c \
>   "select id, recording_id, status, attempt_no, auto_retry_count, failure_code from analyses;"
> ```

> **B3 을 기다리지 않습니다.** 시드된 분석 ID 를 주소창에 직접 넣어
> `PENDING`·`FAILED`·`COMPLETED` 세 가지 상태를 전부 검증할 수 있습니다.

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 성공 | data |
| --- | --- | --- | --- | --- | --- |
| 08 | STT_009 | GET | `/analyses/{analysisId}/status` | 200 | `AnalysisStatus` |
| 13 | DASH_002 | GET | `/dashboard/recordings/{recordingId}/status` | 200 | `AnalysisStatus` |

**두 API 는 완전히 같은 응답**을 줍니다. 입력 키만 다릅니다(분석 ID vs 녹음 ID).
**조회 함수를 두 번 만들지 마세요.**

---

## 3. 명세 근거

- 《공개 API 상세》 API 08 · 13
- 《DTO 사전》 → `AnalysisStatus`, `Failure`
- 《상태·자원 수명·재시도 계약》 마지막 문단 → `retryable` 계산식
- 《비동기 failureCode》 표
- 오류: `RESOURCE_NOT_FOUND`

---

## 4. 소유 파일

```
src/api/analysisStatus.js
src/composables/useAnalysisStatus.js
src/views/practice/AnalysisProgressView.vue
src/components/practice/StatusTimeline.vue
src/components/practice/FailureNotice.vue
```

---

## 5. 구현 지침

### 5-1. `api/analysisStatus.js` — 함수 둘, 처리 하나

```js
import { client, unwrap } from './client'

export const fetchStatusByAnalysis = (analysisId) =>
  unwrap(client.get(`/analyses/${analysisId}/status`))

export const fetchStatusByRecording = (recordingId) =>
  unwrap(client.get(`/dashboard/recordings/${recordingId}/status`))
```

응답이 같으므로 **화면과 composable 은 하나**입니다.

### 5-2. 응답 DTO

```
analysisId          문자열
recordingId         문자열
status              PENDING | PROCESSING | COMPLETED | FAILED
attemptNo           1~4
autoRetryCount      0~3
failureCode         FAILED 에서만 존재, 아니면 null
retryable           boolean   ← 서버가 계산해서 준다
retryRequiresAudio  boolean   ← retryable 과 항상 같은 값
startedAt           nullable  현재 차수 시작 시각
finishedAt          nullable  현재 차수 종료 시각
```

### 5-3. **실패도 HTTP 200 입니다**

명세: "실패도 HTTP 200이며 `data.status=FAILED` 와 `failureCode` 로 표현한다."

```js
// ❌ 폴링이 catch 로 빠져 화면이 오류로 뒤집힌다
if (data.status === 'FAILED') throw new Error(...)

// ✅ 정상적인 종료 상태 중 하나다
if (data.status === 'FAILED') { stop(); return }
```

**조회 자체는 성공했습니다.** HTTP 오류는 리소스가 없거나 남의 것일 때(404)뿐입니다.

### 5-4. `useAnalysisStatus.js` — 폴링 (FC4)

```js
import { ref, onUnmounted } from 'vue'
import { STATUS_POLL_INTERVAL_MS } from '@/constants/audio'

const TERMINAL = ['COMPLETED', 'FAILED']
const HIDDEN_INTERVAL_MS = 5000     // 탭이 안 보일 때는 느리게

export function useAnalysisStatus(fetcher) {
  const data = ref(null)
  const state = ref('loading')      // loading | pending | ready | error
  const error = ref(null)
  let timer = null
  let stopped = false

  function clear() { clearTimeout(timer); timer = null }

  // FC4 — 화면을 벗어나면 반드시 멈춘다. 안 그러면 요청이 계속 쌓인다.
  function stop() { stopped = true; clear() }

  function schedule() {
    if (stopped) return
    clear()
    const wait = document.hidden ? HIDDEN_INTERVAL_MS : STATUS_POLL_INTERVAL_MS
    timer = setTimeout(tick, wait)
  }

  async function tick() {
    if (stopped) return
    try {
      const d = await fetcher()
      data.value = d
      error.value = null
      if (TERMINAL.includes(d.status)) {
        state.value = 'ready'       // FAILED 도 정상 종료다. 오류가 아니다.
        clear()
        return
      }
      state.value = 'pending'       // PENDING / PROCESSING
      schedule()
    } catch (e) {
      error.value = e
      if (e.status === 404) { state.value = 'error'; clear(); return }
      // 일시적 네트워크 오류로 폴링을 멈추지 않는다. 다음 차례에 다시 시도한다.
      state.value = data.value ? 'pending' : 'error'
      schedule()
    }
  }

  function start() { stopped = false; state.value = 'loading'; tick() }

  // 탭으로 돌아오면 즉시 한 번 확인한다 (5초를 더 기다리지 않게)
  const onVisible = () => { if (!document.hidden && !stopped && timer) tick() }
  document.addEventListener('visibilitychange', onVisible)

  onUnmounted(() => {
    stop()
    document.removeEventListener('visibilitychange', onVisible)
  })

  return { data, state, error, start, stop }
}
```

- **1초 간격**입니다(`STATUS_POLL_INTERVAL_MS`). 명세가 프론트 폴링을 전제로 설계됐습니다.
- **탭이 숨겨지면 5초로 늦춥니다.** 백그라운드 탭이 서버를 두드릴 이유가 없습니다.
- **일시적 네트워크 오류로 폴링을 멈추지 마세요.** 404 만 종료 사유입니다.
- **`onUnmounted` 에서 리스너까지 정리**합니다. 타이머만 지우면 리스너가 샙니다.

### 5-5. 화면 구성

```
[상태 타임라인]  대기 중 → 분석 중 → 완료
[진행 안내]      StateBlock state="pending"
[차수 정보]      2차 시도 · 자동 재시도 1회   ← attemptNo > 1 이거나 autoRetryCount > 0 일 때만
[완료]           결과 보기 → { name: 'recordingDetail', params: { recordingId } }
[실패]           FailureNotice + 재시도 버튼(B5)
```

- `COMPLETED` 가 되면 **자동으로 이동하지 말고** "결과 보기" 버튼을 띄웁니다.
  자동 이동은 사용자가 상태 전이를 못 보고 지나칩니다 — 이 프로젝트가 화면으로 증명하려는 게 그것입니다.
- `startedAt` 이 `null` 이면 아직 워커가 안 잡은 `PENDING` 입니다. "대기 중" 으로 표시합니다.

### 5-6. `failureCode` 표시

| 코드 | 문구 |
| --- | --- |
| `STT_TIMEOUT` | "음성 인식이 시간 안에 끝나지 않았습니다." |
| `UPSTREAM_RATE_LIMIT` | "요청이 몰려 처리하지 못했습니다." |
| `UPSTREAM_UNAVAILABLE` | "분석 서비스에 일시적인 문제가 있었습니다." |
| `ANALYSIS_TIMEOUT` | "분석이 제한 시간을 넘겼습니다." |
| `WORKER_LOST` | "분석이 중단되었습니다." |
| `COACHING_FAILED` | "코칭 문구를 만들지 못했습니다." |
| `INVALID_ANALYSIS_RESULT` | "분석 결과가 올바르지 않습니다." |
| `INTERNAL_ERROR` | "알 수 없는 오류가 발생했습니다." |

모르는 코드가 오면 **"분석에 실패했습니다"** 로 떨어뜨리고 화면을 깨뜨리지 않습니다.

### 5-7. `retryable` 를 다시 계산하지 마세요

```
retryable = status == FAILED && attemptNo < 4     ← 서버가 이미 계산한다
```

- **모든 `failureCode` 에서 동일한 조건**입니다. 자동 재시도 대상 3종과 혼동하지 마세요.
- `retryable: false` 면 재시도 버튼을 **숨깁니다**(비활성이 아니라).
  `attemptNo === 4` 면 "재시도 횟수를 모두 사용했습니다. 새로 녹음해 주세요" + `/record` 링크.

### 5-8. API 13 을 쓰는 자리

`/recordings/:id` 상세(A6)에서 "진행 상황 보기"로 올 때는 `analysisId` 를 알고 있으므로 08 을 씁니다.
**13 은 `recordingId` 만 아는 딥링크 경로**를 위한 것입니다.
진행 화면이 `?recordingId=` 로도 열릴 수 있게 되면 A 의 화면이 링크하기 편합니다.

---

## 6. 함정

- **`FAILED` 를 예외로 던지지 마세요.** HTTP 200 입니다 (§5-3).
- **폴링 정리를 빠뜨리지 마세요** (FC4). 화면에 여러 번 들어오면 타이머가 누적됩니다.
- **`retryable` 을 프론트에서 계산하지 마세요.**
- **`retryable` 을 `failureCode` 별로 다르게 판정하지 마세요.** 전부 같은 조건입니다.
- **`COMPLETED` 에서 자동 이동하지 마세요.** 상태 전이가 정보입니다.
- **404 를 제외한 오류로 폴링을 멈추지 마세요.** 잠깐 끊긴 네트워크에서 화면이 죽습니다.
- **`failureCode` 를 FAILED 가 아닌 상태에서 읽지 마세요.** `null` 입니다.
- **컨트롤러/화면 이름을 A5 의 대시보드 화면과 겹치게 짓지 마세요.**
  `AnalysisProgressView.vue` 는 `views/practice/` 에 있습니다.
- **폴링 간격을 100ms 같은 값으로 줄이지 마세요.** 1초입니다.

---

## 7. 검증

> 백엔드 API 08·13 + seed 이후 수행합니다.

```bash
# DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select id, status, attempt_no, failure_code from analyses order by id;"
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 완료된 분석 ID 로 진입 | 바로 `COMPLETED`, 폴링이 **멈춘다** |
| 2 | 실패한 분석 ID 로 진입 | **HTTP 200**, 실패 안내 + 재시도 버튼 |
| 3 | 대기 중 분석 ID 로 진입 | `pending` 상태, 1초마다 요청 |
| 4 | 3번 상태에서 다른 화면으로 이동 | **Network 탭에 요청이 더 이상 안 쌓인다** (FC4) |
| 5 | 4번 뒤 다시 진입 | 요청이 **1초에 한 번**이다 (두 번이면 타이머 누적) |
| 6 | 탭을 백그라운드로 | 요청 간격이 5초로 늘어난다 |
| 7 | `attemptNo=4` 인 실패 건 | 재시도 버튼이 **없고** "새로 녹음" 안내 |
| 8 | 없는 분석 ID | 404 화면, 폴링 정지 |
| 9 | 08 과 13 의 응답 비교 | **완전히 동일** |
| 10 | B3 으로 새로 제출 | `PENDING → PROCESSING → COMPLETED` 가 **눈으로 보인다** |

```bash
# 9번 검증 — DB 구축 후
docker compose exec db psql -U postgres -d miniproject -tAc \
  "select id, recording_id from analyses where status='COMPLETED' limit 1;"
# 두 URL 을 브라우저에서 열어 응답을 비교한다
```

```bash
# 7번 준비 — DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "update analyses set attempt_no=4 where status='FAILED';"
```

---

## 8. 완료 기준 (DoD)

- [ ] API 08 과 13 이 **같은 composable·같은 화면**을 쓴다
- [ ] `FAILED` 가 **HTTP 200** 으로 처리되고 오류 화면이 아니다
- [ ] 화면을 벗어나면 폴링이 **완전히 멈춘다** (FC4)
- [ ] 재진입 시 타이머가 누적되지 않는다
- [ ] 탭이 숨겨지면 간격이 늘어난다
- [ ] `retryable` 을 프론트에서 재계산하지 않는다
- [ ] `attemptNo=4` 실패에서 재시도 버튼이 숨겨진다
- [ ] `failureCode` 8종의 문구가 각각 있고, 모르는 코드에서도 화면이 안 깨진다
- [ ] `COMPLETED` 에서 자동 이동하지 않는다
- [ ] 404 외의 오류로 폴링이 멈추지 않는다
- [ ] `PENDING → PROCESSING → COMPLETED` 전이가 화면에 보인다

커밋 예시:

```
feat(fe): 분석 진행 화면과 상태 폴링 구현

분석 실패는 HTTP 오류가 아니라 200 + status=FAILED 다.
예외로 던지면 폴링이 catch 로 빠져 정상 흐름이 오류 화면으로 뒤집힌다.
화면을 벗어나면 타이머와 visibilitychange 리스너를 함께 정리한다.
```

---

# B5 — 수동 재시도 (API 09)

우선순위 **P1** · 담당 **B** · 선행 **B3, B4** · 브랜치 `feat/fe-manual-retry`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-manual-retry

cd frontend && npm run dev
```

> **선행 필요 — 백엔드 API 09 미구현.**
> `docker compose down -v && docker compose up -d db backend`

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 인증 | 성공 | data |
| --- | --- | --- | --- | --- | --- | --- |
| 09 | STT_010 | POST | `/analyses/{analysisId}/retry` | Access JWT | **202** | `Accepted` |

헤더: `Idempotency-Key`(UUID) — **한 번 누를 때마다 새 키**
본문: `audio`(file) — **최초와 동일한 바이트여야 한다**

---

## 3. 명세 근거

- 《공개 API 상세》 API 09
- 《상태·자원 수명·재시도 계약》 → `FAILED → PENDING`
- 《멱등 저장 계약》 규칙 2 · 마지막 문단
- 오류: B3 의 것 전부 + `RESOURCE_NOT_FOUND` · `INVALID_ANALYSIS_STATE` ·
  `MANUAL_RETRY_LIMIT_EXCEEDED` · `AUDIO_MISMATCH`

---

## 4. 소유 파일

```
src/api/submission.js                        retryAnalysis 추가 (B3 파일)
src/components/practice/RetryPanel.vue
src/views/practice/AnalysisProgressView.vue  B4 파일에 재시도 영역 연결
```

---

## 5. 구현 지침

### 5-1. `api/submission.js` 에 추가

```js
export function retryAnalysis(analysisId, blob, idempotencyKey, onProgress) {
  const form = new FormData()
  form.append('audio', blob, `recording.${EXT[blob.type.split(';')[0]] ?? 'webm'}`)

  return unwrap(client.post(`/analyses/${analysisId}/retry`, form, {
    headers: { 'Content-Type': undefined, 'Idempotency-Key': idempotencyKey },
    timeout: 60000,
    onUploadProgress: onProgress,
  }))
}
```

### 5-2. **같은 바이트여야 합니다** — `AUDIO_MISMATCH`

서버가 `recordings.audio_sha256` 과 대조합니다. 다른 파일이면 **422 `AUDIO_MISMATCH`** 입니다.

- 그래서 B3 의 `useSubmissionStore` 가 원본 `Blob` 을 들고 있습니다.
- **새로고침하면 `Blob` 이 사라집니다.** 그건 정상이고, 이때는 재시도할 방법이 없습니다.

```js
const canRetry = computed(() =>
  status.value?.retryable && submission.hasAudioFor(status.value.analysisId))
```

| 조건 | 화면 |
| --- | --- |
| `retryable && 원본 있음` | **"다시 시도"** 버튼 |
| `retryable && 원본 없음` | "재시도하려면 원본 음성이 필요합니다. 새로 녹음해 주세요." + `/record` 링크 |
| `!retryable && attemptNo === 4` | "재시도 횟수를 모두 사용했습니다." + `/record` 링크 |
| `!retryable` (그 외) | 재시도 영역을 **숨긴다** |

`retryRequiresAudio` 가 `retryable` 과 항상 같은 값이므로, 사실상 **재시도에는 언제나 원본이 필요**합니다.

### 5-3. 멱등 키 — B3 과 규칙이 다릅니다

```
operation        = RETRY_ANALYSIS      (제출은 CREATE_RECORDING)
targetAnalysisId = 재시도 대상 analysisId
```

**"다시 누르기" = 새 키**입니다. 그 요청의 **네트워크 재전송만** 같은 키를 유지합니다.

```js
let key = null
async function retry() {
  key ??= newIdempotencyKey()        // 이 시도 동안 고정
  try {
    const data = await retryAnalysis(analysisId, submission.blob, key, onProgress)
    key = null                        // 성공했으니 다음 "다시 누르기"는 새 키
    restartPolling()                  // B4 의 폴링을 다시 돌린다
  } catch (e) { error.value = e }
}
```

**같은 키로 재전송하면 409 가 아니라 최초 202 본문이 그대로 옵니다.** 이건 정상입니다.
`attemptNo` 가 이미 올라간 값으로 오므로 화면이 그대로 반영하면 됩니다.

### 5-4. 응답과 후처리

```json
{ "recordingId": "101", "analysisId": "5001", "status": "PENDING",
  "attemptNo": 2, "autoRetryCount": 0 }
```

- **`analysisId` 가 같습니다.** 새 분석이 생기지 않습니다. 라우트를 바꾸지 마세요.
- `attemptNo` 가 +1, `autoRetryCount` 가 0 으로 초기화됩니다.
- 202 를 받으면 **B4 의 폴링을 다시 시작**합니다. 화면 이동은 없습니다.

### 5-5. 오류 분기

| 코드 | 상태 | 화면 |
| --- | --- | --- |
| `RESOURCE_NOT_FOUND` | 404 | 기록 목록으로 |
| `INVALID_ANALYSIS_STATE` | 409 | "실패한 분석만 다시 시도할 수 있습니다." → 상태를 다시 조회한다 |
| `MANUAL_RETRY_LIMIT_EXCEEDED` | 409 | "재시도 횟수를 모두 사용했습니다." + `/record` |
| `AUDIO_MISMATCH` | 422 | "처음 제출한 음성과 다른 파일입니다." → 원본 보관이 깨진 것이므로 새로 녹음 안내 |
| `ANALYSIS_ALREADY_ACTIVE` | 409 | "이미 진행 중인 분석이 있습니다." |
| `ANALYSIS_CAPACITY_EXCEEDED` | 503 | "잠시 후 다시" + 재시도 버튼(같은 키) |

**`AUDIO_MISMATCH` 와 `INVALID_AUDIO` 는 다릅니다.**
전자는 "최초와 다른 파일", 후자는 "읽을 수 없는 파일" 입니다.

---

## 6. 함정

- **`AUDIO_MISMATCH` 와 `INVALID_AUDIO` 를 혼동하지 마세요.**
- **재시도 때 다시 녹음한 파일을 보내지 마세요.** 반드시 원본 `Blob` 입니다.
- **원본을 `localStorage` 에 저장해서 새로고침 문제를 "해결"하지 마세요.**
  파일을 저장소에 두지 않는 것이 규칙입니다(`frontend/AGENTS.md` §5). 안내로 처리합니다.
- **재시도 키를 B3 의 제출 키와 공유하지 마세요.** `operation` 이 달라 서버가 다른 질문으로 봅니다.
- **성공 후 라우트를 바꾸지 마세요.** `analysisId` 가 그대로입니다.
- **`attemptNo` 를 프론트에서 +1 하지 마세요.** 서버 응답 값을 씁니다.
- **`retryable: false` 에서 버튼을 비활성으로 두지 말고 숨기세요.** 누를 수 없는 버튼은 혼란만 줍니다.
- **재시도 후 폴링을 다시 켜는 걸 잊지 마세요.** `FAILED` 에서 멈춰 있던 타이머입니다.

---

## 7. 검증

> 백엔드 API 09 구현 후 수행합니다.

```bash
# DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select id, status, attempt_no, auto_retry_count from analyses where status='FAILED';"
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 제출 → 실패 유도 → 재시도 | 202, `attemptNo` 2, **같은 `analysisId`**, 폴링 재개 |
| 2 | 1번 진행 새로고침 → 재시도 시도 | 버튼 대신 "새로 녹음해 주세요" 안내 |
| 3 | 완료된 분석에서 재시도 | 409 `INVALID_ANALYSIS_STATE` |
| 4 | `attemptNo=4` 에서 | 버튼이 **없다** |
| 5 | 같은 키로 재전송 | 409 가 아니라 **최초 202 본문** |
| 6 | 두 번 연속 "다시 시도" | **다른** `Idempotency-Key` |
| 7 | DB 에서 `audio_sha256` 을 바꾸고 재시도 | 422 `AUDIO_MISMATCH` |
| 8 | 재시도 후 DB | `submitted_at` 이 **그대로**, `attempt_no` +1, `auto_retry_count` 0 |

```bash
# 8번 — DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select r.submitted_at, a.attempt_no, a.auto_retry_count, a.finished_at
     from recordings r join analyses a on a.recording_id=r.id order by a.id desc limit 1;"
```

---

## 8. 완료 기준 (DoD)

- [ ] 재시도에 **최초와 동일한 `Blob`** 을 보낸다
- [ ] 원본이 없으면 버튼 대신 **안내**가 뜬다 (localStorage 로 우회하지 않는다)
- [ ] `Idempotency-Key` 가 **누를 때마다 새로** 생기고, 네트워크 재전송에서만 유지된다
- [ ] 같은 키 재전송이 **202** 다 (409 아님)
- [ ] 성공 후 `analysisId` 가 같고 라우트가 바뀌지 않는다
- [ ] 성공 후 **폴링이 다시 시작**된다
- [ ] `retryable: false` 에서 버튼이 **숨겨진다**
- [ ] `AUDIO_MISMATCH`(422)와 `INVALID_ANALYSIS_STATE`(409) 문구가 다르다
- [ ] `attemptNo` 를 서버 응답에서만 읽는다

커밋 예시:

```
feat(fe): 분석 수동 재시도 구현

재시도는 최초와 동일한 바이트여야 한다. 서버가 audio_sha256 으로 대조해
다르면 AUDIO_MISMATCH 를 준다. 그래서 원본 Blob 을 메모리에 들고 있는다.
새로고침으로 원본이 사라지면 재시도 대신 새로 녹음하도록 안내한다.
파일을 localStorage 에 저장해 우회하지 않는다.
```

---

# B6 — PRO 상세 분석 화면 (API 17)

우선순위 **P1** · 담당 **B** · 선행 **B2**(파형 렌더러) · 브랜치 `feat/fe-pro-analysis`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-pro-analysis

cd frontend && npm run dev
```

> **선행 필요 — 백엔드 API 17 미구현 / `db/seed-dev.sql` 미구축.**
>
> ```bash
> docker compose down -v && docker compose up -d db backend
> docker compose exec db psql -U postgres -d miniproject -c "select id, email, plan from users;"
> # pro@example.com / P@ssw0rd123 로 로그인해서 확인한다
> ```

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 인증 | 성공 | data |
| --- | --- | --- | --- | --- | --- | --- |
| 17 | PRO_003~007 | GET | `/recordings/{recordingId}/pro-analysis` | Access JWT **+ PRO** | 200 | `ProResult` |

명세의 PRO 기능 5개(파형·침묵·속도·타임라인·구간)를 **한 화면**이 함께 그립니다.
API 를 쪼개지 마세요. 한 번의 호출입니다.

---

## 3. 명세 근거

- 《공개 API 상세》 API 17
- 《DTO 사전》 → `ProResult`, `Metrics`, `SpeechInterval`, `WaveformPoint`, `FillerEvent`,
  `SpeechRate`, `Segment`, `Coaching`
- 《분석 규칙 — speech-habits-v1》 규칙 11 → 불변식
- 오류: `RESOURCE_NOT_FOUND` · `PRO_REQUIRED` · `ANALYSIS_NOT_COMPLETED`

---

## 4. 소유 파일

```
src/api/proAnalysis.js
src/views/practice/ProAnalysisView.vue
src/components/waveform/FillerTimeline.vue
src/components/practice/SegmentBars.vue
src/components/practice/MetricGrid.vue
src/components/practice/CoachingCard.vue
```

`components/waveform/WaveformCanvas.vue` 와 `SpeechIntervalBar.vue`(B2)를 **재사용**합니다.

---

## 5. 구현 지침

### 5-1. `api/proAnalysis.js`

```js
import { client, unwrap } from './client'

export const fetchProAnalysis = (recordingId) =>
  unwrap(client.get(`/recordings/${recordingId}/pro-analysis`))
```

### 5-2. 응답 구조

```
recordingId, analysisId, status, algorithmVersion, engineType, engineVersion,
metrics: {
  durationMs, speechDurationMs, silenceDurationMs, longSilenceCount, repeatedExpressionCount,
  basic: { fillerTotalCount, fillerBreakdown[] },
  speechIntervals: [{ startMs, endMs }],
  waveform: [{ timeMs, amplitude, type }],          최대 600점
  speechRate: { wordsPerMinute(nullable), totalWordCount },
  fillerTimeline: [{ eventIndex, word, timeMs, segment }],
  segmentAnalysis: [{ segment, fillerCount }],      항상 3개
  coaching: { summary, practiceRecommendation, actionItems[] }
}
```

### 5-3. 화면 구성

```
[요약 지표]      길이 · 발화/침묵 · 긴 침묵 수 · 반복 표현 수 · WPM · 총 단어 수
[파형]           WaveformCanvas (B2) + SpeechIntervalBar (B2)
[추임새 타임라인] FillerTimeline — 파형과 같은 시간축에 마커
[구간별]         SegmentBars — INITIAL / MIDDLE / FINAL 3개
[추임새 분해]    fillerBreakdown 목록
[코칭]           summary · practiceRecommendation · actionItems
```

### 5-4. **`fillerTimeline` 을 자르지 마세요**

명세의 불변식:

```
fillerTotalCount == sum(fillerBreakdown.count)
                 == fillerTimeline.length
                 == sum(segmentAnalysis.fillerCount)
```

이벤트가 100개여도 **전부 렌더**합니다. "상위 20개만" 같은 처리를 넣으면
화면의 숫자가 서로 안 맞습니다. 목록이 길면 스크롤로 처리합니다.

```js
// 개발 중에만 확인해 두면 백엔드 버그를 빨리 잡는다
if (import.meta.env.DEV) {
  const m = data.metrics
  console.assert(
    m.basic.fillerTotalCount === m.fillerTimeline.length &&
    m.basic.fillerTotalCount === m.segmentAnalysis.reduce((s, x) => s + x.fillerCount, 0),
    '추임새 합계 불변식이 깨졌습니다', m,
  )
}
```

### 5-5. `segmentAnalysis` 는 **항상 3개**

`INITIAL` · `MIDDLE` · `FINAL` 이 **횟수가 0이어도 옵니다.**
0인 구간을 필터링하면 막대가 두 개만 떠서 "초반·중반·후반" 구조가 안 보입니다.

```js
const SEGMENT_LABEL = { INITIAL: '초반', MIDDLE: '중반', FINAL: '후반' }
// 서버가 준 순서 그대로 쓴다. 정렬하지 않는다.
```

### 5-6. `FillerTimeline.vue` — 파형과 **같은 시간축**

```vue
<script setup>
const props = defineProps({
  events: { type: Array, default: () => [] },   // [{ eventIndex, word, timeMs, segment }]
  durationMs: { type: Number, required: true },
})
const left = (ms) => `${(ms / props.durationMs) * 100}%`
</script>

<template>
  <div class="timeline" role="img" :aria-label="`추임새 ${events.length}회`">
    <span v-for="e in events" :key="e.eventIndex" class="marker"
          :style="{ left: left(e.timeMs) }" :title="`${e.word} · ${(e.timeMs / 1000).toFixed(1)}초`">
      <span class="dot" />
      <span class="word">{{ e.word }}</span>
    </span>
  </div>
</template>
```

- **`durationMs` 로 나눈 백분율**을 씁니다. 파형과 축이 어긋나면 안 됩니다.
- `eventIndex` 는 0부터 연속입니다. `:key` 로 그대로 씁니다.
- 마커가 겹치면 라벨을 숨기고 점만 남깁니다(밀도 높은 구간).

### 5-7. `wordsPerMinute` 는 nullable

발화 시간이 0이면 `null` 입니다. **`0` 으로 바꾸지 마세요** (§C5).
"말하기 속도를 계산할 수 없습니다(발화 구간 없음)" 로 표시합니다.

### 5-8. 시간 합 검증

```
speechDurationMs + silenceDurationMs == durationMs
```

화면에 발화·침묵 비율 막대를 그릴 때 이 등식을 그대로 씁니다.
합이 안 맞으면 백엔드 버그이므로 개발 빌드에서 콘솔에 남깁니다.

### 5-9. 오류 분기 — **순서가 있습니다**

서버 검사 순서: 소유권 → PRO → 완료

| 코드 | 상태 | 화면 |
| --- | --- | --- |
| `RESOURCE_NOT_FOUND` | 404 | "요청한 정보를 찾을 수 없습니다" + 목록 |
| `PRO_REQUIRED` | 403 | 업그레이드 안내 → `{ name: 'upgrade' }` |
| `ANALYSIS_NOT_COMPLETED` | 409 | "분석이 끝나면 볼 수 있습니다" + 진행 화면 링크 |

- **404 가 403 보다 먼저**입니다. 남의 기록에 `PRO_REQUIRED` 를 주면 그 ID 의 존재가 노출되므로
  서버가 일부러 404 를 먼저 줍니다. 화면도 이 순서를 그대로 반영하면 됩니다.
- **라우터 가드로 PRO 를 미리 막지 마세요** (§C9). 서버 응답으로 처리합니다.
- **`ProLockCard.vue`(A6 소유)를 import 하지 마세요.** 여기서는 `/upgrade` 로 **이동**만 합니다
  (감사표 #7).

### 5-10. 없는 것

- **음성 재생 URL 이 없습니다.** 서버가 원본을 보관하지 않습니다.
  재생 버튼·오디오 플레이어를 만들지 마세요.
- **전사 문장이 없습니다.** 명세가 저장하지 않습니다.

---

## 6. 함정

- **`fillerTimeline` 을 자르거나 샘플링하지 마세요.** 불변식이 깨집니다.
- **`segmentAnalysis` 를 0인 구간 제외로 필터링하지 마세요.** 항상 3개입니다.
- **`wordsPerMinute: null` 을 0으로 바꾸지 마세요** (§C5).
- **`waveform` 을 재샘플링하지 마세요.** 이미 최대 600점입니다.
- **음성 재생 UI 를 만들지 마세요.** 원본이 없습니다.
- **PRO 를 라우터 가드로 막지 마세요.** 403 을 화면에서 처리합니다 (§C9).
- **`ProLockCard`(A 소유)를 import 하지 마세요** (감사표 #7).
- **`fillerBreakdown` 을 재정렬하지 마세요.** 이미 `count` 내림차순입니다.
- **`coaching.actionItems` 는 1~5개**입니다. 빈 배열이 오면 백엔드 버그이니 화면은 안 깨지게 두고
  개발 빌드에서 경고를 남기세요.

---

## 7. 검증

> 백엔드 API 17 + seed 이후 수행합니다.

```bash
# PRO 계정 소유의 완료된 기록 ID 를 찾는다 — DB 구축 후
docker compose exec db psql -U postgres -d miniproject -c \
  "select r.id, u.email, a.status from recordings r
     join users u on u.id=r.user_id join analyses a on a.recording_id=r.id
    where a.status='COMPLETED';"
```

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | PRO 로 완료 기록 진입 | 파형·타임라인·구간 3개·코칭이 모두 그려진다 |
| 2 | FREE 로 같은 URL | 업그레이드 안내 (`PRO_REQUIRED`) |
| 3 | 없는 기록 ID | **404** ("요청한 정보를 찾을 수 없습니다") — 등급과 무관 |
| 4 | PRO 로 미완료 기록 | 409 + 진행 화면 링크 |
| 5 | 개발자도구 콘솔 | 불변식 `console.assert` 가 통과 |
| 6 | 구간 막대 | 횟수가 0인 구간도 **막대 자리가 있다** |
| 7 | 타임라인 마커 수 | `fillerTotalCount` 와 **같다** |
| 8 | 발화+침묵 비율 막대 | 합이 전체 길이와 같다 |
| 9 | 화면 어디에도 | **재생 버튼이 없다** |

브라우저 콘솔:

```js
const d = await (await import('/src/api/proAnalysis.js')).fetchProAnalysis('101')
const m = d.metrics
console.log({
  합계일치: m.basic.fillerTotalCount === m.basic.fillerBreakdown.reduce((s,x)=>s+x.count,0),
  타임라인: m.basic.fillerTotalCount === m.fillerTimeline.length,
  구간합:   m.basic.fillerTotalCount === m.segmentAnalysis.reduce((s,x)=>s+x.fillerCount,0),
  시간합:   m.speechDurationMs + m.silenceDurationMs === m.durationMs,
  구간3개:  m.segmentAnalysis.length === 3,
  파형점수: m.waveform.length,          // <= 600
})
```

---

## 8. 완료 기준 (DoD)

- [ ] 한 번의 호출로 5개 기능을 모두 그린다 (API 를 쪼개지 않았다)
- [ ] `fillerTimeline` 을 **전부** 렌더하고 자르지 않는다
- [ ] `segmentAnalysis` 가 **항상 3개**로 그려진다 (0인 구간 포함)
- [ ] `wordsPerMinute: null` 이 `—` 로 표시된다
- [ ] 파형이 최대 600점을 그대로 그리고 재샘플링하지 않는다
- [ ] 타임라인 마커가 파형과 **같은 시간축**에 있다
- [ ] 404 → 403 → 409 순서로 오류가 구분된다
- [ ] PRO 를 라우터 가드로 막지 않는다
- [ ] A 소유 컴포넌트를 import 하지 않는다
- [ ] 음성 재생 UI 가 **없다**
- [ ] 개발 빌드에서 불변식 검사가 돈다

커밋 예시:

```
feat(fe): PRO 상세 분석 화면 구현

fillerTimeline 은 전부 렌더한다. 명세의 불변식이
fillerTotalCount == fillerTimeline.length 라 자르면 화면의 숫자가 서로 어긋난다.
segmentAnalysis 는 횟수가 0인 구간도 오므로 필터링하지 않는다.
서버가 원본 음성을 보관하지 않으므로 재생 UI 를 만들지 않는다.
```

---

# B7 — Mock 분석 콘솔 (API 20 · 21, 개발용)

우선순위 **P2** · 담당 **B** · 선행 **B2** · 브랜치 `feat/fe-mock-console`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-mock-console

cd frontend && npm run dev
```

> **선행 필요 — 백엔드 API 20·21 미구현 / `docker-compose.yml` 미구축.**
>
> ```bash
> docker compose down -v && docker compose up -d db
> cd backend && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun &
> ```

Mock API 는 **local/test 프로파일에서만 등록**됩니다. prod 에서는 404 입니다.

---

## 2. 목표와 대상 API

| # | 기능 ID | Method | Endpoint | 인증 | 성공 | data |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | MOCK_001 | POST | `/mock/waveform-analysis` | Access JWT | 200 | `MockWaveform` |
| 21 | MOCK_002 | POST | `/mock/transcript-analysis` | Access JWT | 200 | `MockTranscript` |

백엔드 규칙 엔진에 JSON 을 직접 넣어 결과를 보는 **개발·검증용** 화면입니다.
사용자 기능이 아닙니다.

---

## 3. 명세 근거

- 《공개 API 상세》 API 20 · 21
- 《DTO 사전》 → `MockWaveform`, `MockTranscript`, `AmplitudePoint`, `TimedToken`
- 《Phase 1 Mock 및 Phase 2 내부 분석 API》 → prod 404 규칙
- 오류: `VALIDATION_ERROR`

---

## 4. 소유 파일

```
src/api/mock.js
src/views/dev/MockConsoleView.vue
```

`components/waveform/WaveformCanvas.vue`(B2)를 재사용해 결과를 그립니다.

---

## 5. 구현 지침

### 5-1. 프로덕션 번들에서 제외

Step 0 의 라우터가 이미 처리합니다.

```js
// 메인 glob 은 dev/ 를 제외한다 — 안 그러면 이 컴포넌트가 프로덕션 번들에 남는다
const views = import.meta.glob(['/src/views/**/*.vue', '!/src/views/dev/**'])

if (import.meta.env.DEV) {
  const devViews = import.meta.glob('/src/views/dev/**/*.vue')
  routes.push({ path: '/dev/mock', name: 'devMock',
    component: devViews['/src/views/dev/MockConsoleView.vue']
      ?? (() => import('@/views/NotReadyView.vue')) })
}
```

`import.meta.env.DEV` 가 프로덕션에서 `false` 로 치환되면 이 블록이 죽은 코드가 되어
**안쪽 glob 과 그 청크까지 함께 사라집니다.** 번들에 Mock 화면이 들어가지 않습니다.

> **메인 glob 에서 `dev/` 를 빼는 게 핵심입니다.** `import.meta.glob` 은 매칭되는 파일을
> **전부** 번들에 넣기 때문에, `'/src/views/**/*.vue'` 하나로 두면 이 블록과 무관하게
> `MockConsoleView` 청크가 `dist/` 에 남습니다. 실제로 빌드해서 확인한 결과입니다 —
> 제외 패턴이 없으면 §7 의 6번 검증이 실패합니다.

**헤더에 링크를 넣지 마세요.** 주소창으로만 들어갑니다.

### 5-2. `api/mock.js`

```js
import { client, unwrap } from './client'

export const analyzeMockWaveform = (payload) =>
  unwrap(client.post('/mock/waveform-analysis', payload))

export const analyzeMockTranscript = (payload) =>
  unwrap(client.post('/mock/transcript-analysis', payload))
```

**둘 다 Access JWT 가 필요합니다.** 공개 API 가 아닙니다.

### 5-3. API 20 — 파형 입력

```
durationMs : 1000~60000
waveform   : [{ timeMs, amplitude }]  0ms 부터 100ms 간격, 전체 길이의 모든 점
```

응답 `MockWaveform`: `durationMs`, `speechDurationMs`, `silenceDurationMs`,
`longSilenceCount`, `speechIntervals`, `waveform`(입력에 `type` 이 붙은 것).

**토큰·속도·코칭은 안 옵니다.** 입력에 그런 정보가 없습니다.

화면:
- JSON 텍스트 영역 + "명세 예시 채우기" 버튼 (3000ms, 500~2500ms 만 0.65)
- 결과를 `WaveformCanvas` 로 그린다 → **B2 렌더러가 실제 데이터로 검증됩니다**
- `speechDurationMs + silenceDurationMs === durationMs` 를 화면에 표시

### 5-4. API 21 — 토큰 입력

```
durationMs : 1000~60000
tokens     : [{ text, startMs, endMs }]  최대 1000개
```

응답 `MockTranscript`: `basic`, `totalWordCount`, `repeatedExpressionCount`,
`fillerTimeline`, `segmentAnalysis`.

**WPM·침묵·파형·코칭은 안 옵니다.** 발화 구간이 없어 계산할 수 없습니다.

명세 예시 입력(추임새 2회, 단어 4개, 반복 1회, 구간 `[1,0,1]`)을 버튼 하나로 채워 넣으면
백엔드 규칙 엔진의 회귀를 30초 만에 확인할 수 있습니다.

### 5-5. 화면 규칙

- 두 API 를 탭으로 나눕니다.
- 요청 JSON 을 편집할 수 있게 하고, **파싱 실패는 전송 전에** 막습니다.
- 응답 JSON 원문을 그대로 보여주는 영역을 둡니다. 디버깅에 필요합니다.
- `422 VALIDATION_ERROR` 를 그대로 표시합니다.

---

## 6. 함정

- **프로덕션 번들에 넣지 마세요.** `import.meta.env.DEV` 가드가 없으면 명세의
  "prod 에서 `/mock/**` 은 404" 취지가 프론트에서 깨집니다.
- **헤더나 하단 링크를 만들지 마세요.** 사용자 기능이 아닙니다.
- **토큰 없이 부를 수 있다고 생각하지 마세요.** 401 입니다.
- **각 API 가 주지 않는 필드를 화면에 자리로 만들어 두지 마세요.**
  20 에 "단어 수", 21 에 "WPM" 칸을 만들면 항상 비어 있어 오해를 만듭니다.
- **백엔드가 `local` 프로파일이 아니면 404** 입니다. 화면에 그 사실을 안내하세요.
- 이 화면의 결과를 **사용자 화면 검증의 근거로 쓰지 마세요.** 실제 파이프라인은 B3~B6 입니다.

---

## 7. 검증

> 백엔드 API 20·21 구현 후 수행합니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 20 에 명세 예시 입력 | `speech 2000 / silence 1000 / longSilenceCount 0`, 구간 `[500, 2500)` |
| 2 | 1번 결과 파형 | `WaveformCanvas` 가 발화/침묵 색을 정확히 나눈다 |
| 3 | 21 에 명세 예시 입력 | `fillerTotalCount 2`, `totalWordCount 4`, `repeated 1`, 구간 `[1,0,1]` |
| 4 | `durationMs: 999` | 422 `VALIDATION_ERROR` |
| 5 | 잘못된 JSON | 전송 전에 프론트에서 막힌다 |
| 6 | `npm run build` 후 번들 검색 | `MockConsoleView` 문자열이 **없다** |
| 7 | prod 프로파일 백엔드 | 404 안내가 뜬다 |

```bash
npm run build && grep -rl "MockConsole" dist/ ; echo "exit=$?"
# 아무 파일도 안 나와야 한다
```

---

## 8. 완료 기준 (DoD)

- [ ] 프로덕션 빌드 번들에 **포함되지 않는다** (grep 으로 확인)
- [ ] 헤더·하단 링크가 **없다**
- [ ] 두 API 모두 Access JWT 로 호출한다
- [ ] 각 API 가 주지 않는 필드의 자리를 만들지 않았다
- [ ] 명세 예시 입력이 **한 글자씩 일치**하는 결과를 낸다
- [ ] 잘못된 JSON 을 전송 전에 막는다
- [ ] 422 를 그대로 표시한다
- [ ] `WaveformCanvas`(B2)를 재사용한다

커밋 예시:

```
chore(fe): Mock 분석 콘솔 추가 (개발 빌드 전용)

백엔드 규칙 엔진을 사용자 화면 없이 검증하는 창구다.
import.meta.env.DEV 가드로 프로덕션 번들에서 통째로 제거된다.
명세가 prod 에서 /mock/** 을 404 로 두는 것과 같은 취지다.
```

---

# B8 — 업로드·네트워크 회복 UX

우선순위 **P2** · 담당 **B** · 선행 **B3, B4** · 브랜치 `feat/fe-upload-recovery`

---

## 1. 착수 (그대로 복사)

```bash
git fetch origin && git switch frontend && git pull
git switch -c feat/fe-upload-recovery

cd frontend && npm run dev
```

> **선행 필요 — 백엔드 API 07·08 미구현.**
> `docker compose down -v && docker compose up -d db backend`

---

## 2. 목표

새 API 를 붙이지 않습니다. B3·B4 가 만든 흐름이 **나쁜 네트워크에서도 사용자를 잃지 않게** 다듬습니다.

- 오프라인 감지와 복귀
- 업로드 진행률·취소
- 503 `ANALYSIS_CAPACITY_EXCEEDED` 수동 재시도 안내
- 녹음 중 이탈 방지
- 긴 대기에서의 안내 문구

---

## 3. 명세 근거

- 《공통 구현 계약》 *동시 실행* 절 → 업로드 슬롯 4개, 분석 슬롯 2개
- 《업로드 규격·Vue 요청 예시》 → 상원 타임아웃 30초
- 《상태·자원 수명·재시도 계약》 → 전체 기한 600초
- 오류: `ANALYSIS_CAPACITY_EXCEEDED` · `REQUEST_TIMEOUT` · `SERVICE_UNAVAILABLE`

---

## 4. 소유 파일

```
src/composables/useOnlineStatus.js
src/components/practice/UploadController.vue
src/views/practice/RecordView.vue                  B3 파일에 연결
src/views/practice/AnalysisProgressView.vue        B4 파일에 연결
```

---

## 5. 구현 지침

### 5-1. 오프라인 감지

```js
import { ref, onMounted, onUnmounted } from 'vue'

export function useOnlineStatus() {
  const online = ref(navigator.onLine)
  const on = () => { online.value = true }
  const off = () => { online.value = false }

  onMounted(() => {
    window.addEventListener('online', on)
    window.addEventListener('offline', off)
  })
  onUnmounted(() => {
    window.removeEventListener('online', on)
    window.removeEventListener('offline', off)
  })
  return { online }
}
```

- 오프라인이면 제출 버튼을 잠그고 배너를 띄웁니다. **녹음은 그대로 유지**합니다.
- 복귀하면 배너를 내리고 버튼을 풉니다. **자동으로 제출하지 마세요** — 사용자가 놀랍니다.
- `navigator.onLine` 은 "랜선이 꽂혀 있다" 수준이라 완벽하지 않습니다. **보조 신호**로만 씁니다.

### 5-2. 업로드 취소

axios `AbortController` 를 씁니다.

```js
let controller = null

async function submit(blob) {
  controller = new AbortController()
  try {
    await submitRecording(blob, key, onProgress, controller.signal)
  } catch (e) {
    if (e.code === 'ERR_CANCELED') return    // 사용자가 취소한 것. 오류가 아니다.
    error.value = e
  } finally { controller = null }
}

const cancel = () => controller?.abort()
```

`api/submission.js` 의 시그니처에 `signal` 을 추가합니다(B 소유 파일이라 자유롭게 확장).

- **취소를 오류로 표시하지 마세요.**
- **취소해도 서버엔 이미 접수됐을 수 있습니다.** 같은 멱등 키로 다시 보내면
  서버가 최초 202 를 돌려주므로 중복 제출이 되지 않습니다. **키를 버리지 마세요.**

### 5-3. 503 — 용량 초과는 실패가 아닙니다

명세: 인스턴스당 업로드 4개 · 분석 2개. 초과하면 즉시 503 이고 **대기 큐가 없습니다.**

```
[안내] 지금은 분석 요청이 많습니다. 잠시 후 다시 시도해 주세요.
[버튼] 다시 시도            ← 같은 멱등 키로
[자동] 10초 뒤 자동 재시도 1회 (1회만, 사용자가 끌 수 있게)
```

- **무한 자동 재시도를 만들지 마세요.** 서버 슬롯을 더 막습니다.
- **녹음을 버리지 마세요.**

### 5-4. 긴 대기 안내

분석은 최대 600초까지 갑니다. 폴링 화면이 30초 넘게 `pending` 이면 문구를 바꿉니다.

| 경과 | 문구 |
| --- | --- |
| 0~10초 | "분석을 진행하고 있습니다…" |
| 10~60초 | "분석 중입니다. 잠시만 기다려 주세요." |
| 60초~ | "시간이 조금 더 걸리고 있습니다. 이 화면을 닫아도 분석은 계속됩니다." |

**"닫아도 계속됩니다"가 중요합니다.** 서버가 비동기로 돌고 있고, 나중에 기록 목록에서 확인할 수 있습니다.
이 문구가 없으면 사용자가 화면을 지키고 앉아 있습니다.

### 5-5. 녹음 중 이탈 방지

```js
import { onBeforeRouteLeave } from 'vue-router'

onBeforeRouteLeave(() => {
  if (recorder.state.value === 'recording' || submitting.value) {
    return window.confirm('진행 중인 녹음이 있습니다. 화면을 나가시겠습니까?')
  }
  return true
})
```

`beforeunload` 도 같이 걸되, **분석 진행 화면에는 걸지 마세요.**
그 화면은 나가도 되고, 나가도 된다고 안내하고 있습니다.

### 5-6. 타임아웃 문구

- 업로드 `timeout: 60000` 초과 → `REQUEST_TIMEOUT` → "네트워크가 느립니다. 다시 시도해 주세요."
  **같은 멱등 키로** 재시도합니다.
- 폴링 요청 타임아웃에서 폴링을 멈추지 않습니다 (B4 §5-4).

---

## 6. 함정

- **취소(`ERR_CANCELED`)를 오류로 표시하지 마세요.**
- **취소·타임아웃 후 멱등 키를 버리지 마세요.** 서버엔 이미 접수됐을 수 있어 중복 제출이 됩니다.
- **오프라인 복귀에서 자동 제출하지 마세요.** 사용자가 놀랍니다.
- **503 에서 무한 재시도를 만들지 마세요.** 슬롯을 더 막습니다. 1회 자동 + 수동 버튼입니다.
- **분석 진행 화면에 `beforeunload` 를 걸지 마세요.** 나가도 되는 화면입니다.
- **`navigator.onLine` 을 유일한 판단 근거로 쓰지 마세요.** 실제 요청 실패가 진짜 신호입니다.
- **진행률 100% 를 "완료"로 표시하지 마세요.** 업로드가 끝난 것이지 분석은 이제 시작입니다.
- **오프라인일 때 녹음을 지우지 마세요.**

---

## 7. 검증

개발자도구 Network 탭의 스로틀링·오프라인 기능을 씁니다.

| # | 조작 | 기대 |
| --- | --- | --- |
| 1 | 오프라인으로 전환 | 배너 + 제출 버튼 잠김, **녹음은 유지** |
| 2 | 1번에서 온라인 복귀 | 배너 사라짐, 버튼 풀림, **자동 제출 안 됨** |
| 3 | Slow 3G 로 업로드 | 진행률이 천천히 증가, 취소 버튼 동작 |
| 4 | 3번에서 취소 | 오류 문구가 **뜨지 않는다** |
| 5 | 4번 뒤 다시 제출 | **같은 멱등 키**, 202 (중복 생성 없음) |
| 6 | 서버 슬롯을 채우고 제출 | 503 안내 + 재시도 버튼, 녹음 유지 |
| 7 | 분석을 60초 이상 대기 | "닫아도 분석은 계속됩니다" 문구 |
| 8 | 녹음 중 다른 화면으로 | 확인 대화상자 |
| 9 | 분석 진행 화면에서 새로고침 | 확인 없이 새로고침되고, 다시 들어오면 상태가 이어진다 |
| 10 | 진행률 100% 직후 | "완료"가 아니라 "분석 대기 중"으로 넘어간다 |

```bash
# 6번 준비 — 분석 슬롯 2개를 채운다 (다른 계정으로 동시 제출하거나 DB 로 PROCESSING 을 만든다)
docker compose exec db psql -U postgres -d miniproject -c \
  "select count(*) from analyses where status in ('PENDING','PROCESSING');"
```

---

## 8. 완료 기준 (DoD)

- [ ] 오프라인에서 제출이 잠기고 **녹음이 유지**된다
- [ ] 복귀 시 자동 제출하지 않는다
- [ ] 업로드 취소가 동작하고 **오류로 표시되지 않는다**
- [ ] 취소·타임아웃 후 **같은 멱등 키**로 재시도해 중복 제출이 없다
- [ ] 503 이 실패가 아니라 "잠시 후"로 표시되고 재시도가 **1회 자동 + 수동**이다
- [ ] 60초 이상 대기에서 "닫아도 계속됩니다" 문구가 뜬다
- [ ] 녹음 중 이탈에 확인이 뜨고, **분석 진행 화면에는 안 뜬다**
- [ ] 진행률 100% 를 "완료"로 표시하지 않는다
- [ ] 추가한 이벤트 리스너가 전부 `onUnmounted` 에서 정리된다

커밋 예시:

```
feat(fe): 업로드·네트워크 회복 UX 보강

업로드를 취소해도 서버엔 이미 접수됐을 수 있으므로 멱등 키를 버리지 않는다.
같은 키로 다시 보내면 서버가 최초 202 를 돌려줘 중복 제출이 되지 않는다.

feat(fe): 긴 분석 대기 안내와 녹음 중 이탈 방지 추가

분석은 최대 600초까지 간다. 화면을 닫아도 계속된다는 안내가 없으면
사용자가 화면을 지키고 앉아 있게 된다.
```
