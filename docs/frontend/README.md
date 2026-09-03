# 프론트엔드 구현 문서 — 2인 분업 상세

API명세서 v3.0.0(공개 API 21개)을 프론트 2명이 나눠 구현하기 위한 문서 세트입니다.
**작업 1개 = 문서 1개 절**이며, 각 절은 그것만 읽고 끝까지 갈 수 있게 되어 있습니다.

- 원본 명세: `docs/api/API명세서.md` (이 저장소에선 코드보다 명세가 우선합니다 → 루트 `AGENTS.md` §3-2)
- 백엔드 구현 문서: `docs/backend/README.md` (아직 없습니다 — 백엔드 담당이 작성합니다)
- 이 문서는 **상세**입니다. 실제 작업 문서는 담당자별로 **파일 1개씩** 통합되어 있습니다.

| 담당 | 파일 | 구성 |
| --- | --- | --- |
| A — 계정·이력·통계 | [`A-담당A-통합.md`](A-담당A-통합.md) | Step 0(공통 기반) + A1~A8 |
| B — 녹음·분석·시각화 | [`B-담당B-통합.md`](B-담당B-통합.md) | Step 0(공통 기반) + B1~B8 |

두 파일 모두 **Step 0을 맨 앞에 동일하게 포함**하고 있어, 자기 파일 하나만 열면 공통 기반부터
자기 작업까지 순서대로 볼 수 있습니다. 두 파일에 중복된 Step 0 절은 **완전히 동일한 내용**이며,
실제로는 딱 한 번만(공동으로) 수행합니다.

---

## 0. 이 문서의 위치 — 설계 근거이자 착수 안내서

> 이 문서 세트는 프론트 구현이 시작되기 전에 작성된 **분업 설계**입니다.
> `develop` 기준으로 백엔드 API·DB·프론트 화면이 이미 상당 부분 올라와 있으므로,
> 아직 손대지 않은 절은 **착수 안내서**로, 이미 구현된 절은 **설계 근거와 리뷰 기준**으로 쓰입니다.

| 선행 항목 | 상태 |
| --- | --- |
| `docker-compose.yml` · `db/schema.sql` · `db/seed-dev.sql` | **구축됨** |
| 백엔드 공개 API 21개 | **구현됨** (컨트롤러 16개) |
| 프론트 Step 0 공통 커널 · 화면 16개 | **`frontend` 브랜치에 구현됨** |
| `docs/api/API명세서.md` v3.0.0 | **아직 없음** |

**명세서만 아직 저장소에 없습니다.** 이 문서의 API 표·DTO 설명은 명세 요약이지 원본이 아닙니다.
명세서가 들어오면 **명세가 이깁니다** — 어긋나는 부분은 이 문서를 고칩니다
(루트 `AGENTS.md` §3-2: 코드보다 명세가 우선).

**설계 자체는 구현 진행과 무관하게 유효합니다.** §2 단면, §5 소유권, §7 간섭 감사, §8 결함 5건은
"왜 이렇게 나눴고 왜 이렇게 구현해야 하는가"의 근거이며, 리뷰와 회귀 방지의 기준입니다.

---

## 1. 시작하는 법

```bash
# 담당 A — 이 파일 하나만 연다
docs/frontend/A-담당A-통합.md

# 담당 B — 이 파일 하나만 연다 (Step 0 이후는 B1·B2부터 바로 착수 가능)
docs/frontend/B-담당B-통합.md
```

각 절 머리말의 "## 1. 착수" 절을 그대로 복사하면 됩니다. Step 0은 두 사람이 함께 딱 한 번만
처리하고(20~40분), 그 뒤부터는 자기 파일 안의 절 순서대로(또는 우선순위 순으로) 진행합니다.

**서로를 기다리지 않습니다.** 이것이 이 분업의 핵심 성질입니다.

- B1(녹음)·B2(오디오 검증·파형)는 **백엔드도 Step 0도 필요 없습니다.** 순수 브라우저 API입니다.
- A의 조회 화면 8개는 `db/seed-dev.sql` 의 데이터(COMPLETED 2건·PENDING 1건·FAILED 1건)로 전부 검증됩니다.

---

## 2. 분업의 단면 — A는 "계정과 기록", B는 "한 번의 연습"

두 축은 **라우트에서만 만나고 컴포넌트로는 만나지 않습니다.**

| | 담당 A — 계정·이력·통계 | 담당 B — 녹음·분석·PRO 상세 |
| --- | --- | --- |
| 공개 API | 01~06, 10, 11, 12, 14, 15, 16, 18, 19 (14개) | 07, 08, 09, 13, 17, 20, 21 (7개) |
| 라우트 | `/login` `/signup` `/` `/recordings` `/recordings/:id` `/recordings/:id/compare` `/trends` `/weekly-report` `/me` `/upgrade` | `/record` `/analyses/:id` `/recordings/:id/pro` `/dev/mock` |
| api 모듈 | `api/auth.js` `api/users.js` `api/history.js` `api/stats.js` | `api/submission.js` `api/analysisStatus.js` `api/proAnalysis.js` `api/mock.js` |
| views 폴더 | `views/auth/` `views/account/` `views/dashboard/` `views/stats/` | `views/practice/` `views/dev/` |
| 컴포넌트 폴더 | `components/chart/**` (SVG 라인·바) | `components/waveform/**` (Canvas 파형·타임라인) |
| 성격 | 토큰·라우터 가드·페이지네이션·Asia/Seoul 집계·PRO 게이팅 | MediaRecorder·멀티파트·멱등키·폴링·시각화 |

API 개수는 14:7이지만 부하는 대등합니다. B의 7개 뒤에는 브라우저 녹음(MediaRecorder),
오디오 사전 검증, Canvas 파형 렌더러, 폴링 상태머신이 통째로 붙어 있습니다.

> **API 17(PRO 상세)이 B에 있는 이유** — 백엔드에서는 A가 만든 조회 API지만, 프론트에서는
> `waveform` 600점 + `fillerTimeline` + `segmentAnalysis` 시각화입니다. B가 녹음 화면에서
> 이미 Canvas 파형 렌더러를 만들기 때문에 **같은 렌더러를 두 번 만들지 않으려고** B가 가집니다.
>
> **API 18(비교)은 A에 있습니다.** PRO 전용이지만 화면은 숫자 델타 표시라 A의 통계 계열입니다.

---

## 3. 작업 목록

아래 "절" 열은 모두 각자의 통합 파일([`A-담당A-통합.md`](A-담당A-통합.md) /
[`B-담당B-통합.md`](B-담당B-통합.md)) **안의 절**입니다. `# A1 — …` `# B4 — …` 같은 절 제목이
그대로 남아 있으니 편집기 찾기(Ctrl/Cmd+F)로 이동하면 됩니다.

### 공동 (딱 한 번)

| 절 | 우선 | 내용 |
| --- | --- | --- |
| `Step 0 — 공통 기반` (두 파일 맨 앞에 동일하게 포함) | **P0** | package.json·vite.config·라우트 15개 사전 선언·Pinia 세션·axios envelope 언랩·401 재발급 훅·ErrorCode 27개·StateBlock·CSS 토큰·포맷 유틸·**기존 전사 스캐폴딩 정리** |

### 담당 A — 계정·이력·통계

| 절 | 우선 | 대상 API | 선행 |
| --- | --- | --- | --- |
| `A1 — 인증 API와 세션 복구·라우터 가드` | **P0** | (기반) 01~04 호출부, 새로고침 복구 | Step 0 |
| `A2 — 로그인·회원가입 화면` | **P0** | 01 signup · 02 login | A1 |
| `A3 — 앱 셸·헤더·내 정보` | **P0** | 05 `GET /users/me` · 04 logout | A1 |
| `A4 — 회원 탈퇴` | P1 | 06 `DELETE /users/me` | A3 |
| `A5 — 대시보드 홈·기록 목록` | **P0** | 12 목록 · 15 최근 | Step 0 |
| `A6 — 기록 상세·결과·삭제·PRO 잠금` | **P0** | 10 상세 · 14 결과 · 11 삭제 | A5 |
| `A7 — 추이 통계 화면` | P1 | 16 trends | Step 0 |
| `A8 — 기록 비교·주간 리포트` | P2 | 18 compare · 19 weekly-report | A7 |

### 담당 B — 녹음·분석·시각화

| 절 | 우선 | 대상 API | 선행 |
| --- | --- | --- | --- |
| `B1 — 브라우저 녹음 composable` | **P0** | (기반) MediaRecorder·1~60초·레벨미터 | **없음** |
| `B2 — 오디오 검증 유틸과 파형 렌더러` | **P0** | (기반) MIME 정규화·용량·Canvas 파형 | **없음** |
| `B3 — 녹음 제출 화면과 멱등키` | **P0** | 07 `POST /recordings` | Step 0, B1, B2 |
| `B4 — 분석 진행 화면과 상태 폴링` | **P0** | 08 status · 13 dashboard status | Step 0 |
| `B5 — 수동 재시도` | P1 | 09 retry | B3, B4 |
| `B6 — PRO 상세 분석 화면` | P1 | 17 pro-analysis | B2 |
| `B7 — Mock 분석 콘솔 (개발용)` | P2 | 20 waveform · 21 transcript | B2 |
| `B8 — 업로드·네트워크 회복 UX` | P2 | (기반) 오프라인·타임아웃·용량 | B3, B4 |

**선행 관계는 같은 담당의 안에서만 존재합니다.** A의 어떤 작업도 B를 기다리지 않고,
B의 어떤 작업도 A를 기다리지 않습니다.

**우선순위** — P0: 데모 관통에 반드시 필요 / P1: 명세 완성 / P2: 여유가 있을 때.
시간이 부족하면 **P2 → P1 순으로** 잘라냅니다.

---

## 4. API 21개 배분 대조표

빠지거나 중복된 게 없는지 확인하는 표입니다.

| # | 기능 ID | Method | Endpoint | 담당 | 절 | 화면 |
| --- | --- | --- | --- | --- | --- | --- |
| 01 | AUTH_001 | POST | `/auth/signup` | A | A2 | 회원가입 |
| 02 | AUTH_002 | POST | `/auth/login` | A | A2 | 로그인 |
| 03 | AUTH_003 | POST | `/auth/reissue` | A | A1 | (화면 없음 — 부팅 복구·401 재시도) |
| 04 | USER_006 | POST | `/auth/logout` | A | A3 | 헤더 |
| 05 | USER_005 | GET | `/users/me` | A | A3 | 헤더·내 정보 |
| 06 | USER_007 | DELETE | `/users/me` | A | A4 | 내 정보 |
| 07 | REC_006 | POST | `/recordings` | B | B3 | 녹음·제출 |
| 08 | STT_009 | GET | `/analyses/{analysisId}/status` | B | B4 | 분석 진행 |
| 09 | STT_010 | POST | `/analyses/{analysisId}/retry` | B | B5 | 분석 진행(실패) |
| 10 | HIST_001 | GET | `/recordings/{recordingId}` | A | A6 | 기록 상세 |
| 11 | HIST_002 | DELETE | `/recordings/{recordingId}` | A | A6 | 기록 상세 |
| 12 | DASH_001 | GET | `/dashboard/recordings` | A | A5 | 기록 목록 |
| 13 | DASH_002 | GET | `/dashboard/recordings/{recordingId}/status` | B | B4 | 분석 진행(딥링크) |
| 14 | DASH_003 | GET | `/dashboard/recordings/{recordingId}/result` | A | A6 | 기록 상세 |
| 15 | DASH_008 | GET | `/dashboard/recent-analyses` | A | A5 | 홈 |
| 16 | DASH_009 | GET | `/dashboard/trends` | A | A7 | 추이 |
| 17 | PRO_003~007 | GET | `/recordings/{recordingId}/pro-analysis` | B | B6 | PRO 상세 |
| 18 | PRO_008 | GET | `/recordings/{recordingId}/compare` | A | A8 | 비교 |
| 19 | PRO_009 | GET | `/dashboard/weekly-report` | A | A8 | 주간 리포트 |
| 20 | MOCK_001 | POST | `/mock/waveform-analysis` | B | B7 | 개발용 콘솔 |
| 21 | MOCK_002 | POST | `/mock/transcript-analysis` | B | B7 | 개발용 콘솔 |

명세의 `REC_001~011` · `DASH_010` · `PRO_001·002`는 **Vue 로컬 처리**로, 네트워크 호출이 없습니다.
녹음 UI·레벨미터·파일 검증·업로드 진행률이 여기에 해당하며 전부 B의 B1·B2·B3·B8에 들어 있습니다.

---

## 5. 파일 소유권 선언

**규칙: 파일 1개 = 소유자 1명.** 소유자가 아닌 사람은 그 파일을 *읽기만* 합니다.
아래 표에 같은 경로가 두 번 나오지 않는 것이 이 분업의 근거입니다.

| 경로 | 소유자 |
| --- | --- |
| `frontend/package.json`, `vite.config.js`, `.env.example` | Step 0 (이후 동결) |
| `src/main.js` | Step 0 (이후 동결) |
| `src/router/index.js` | Step 0 (이후 **동결** — 라우트 15개를 미리 다 선언한다) |
| `src/stores/session.js` | Step 0 (이후 동결) |
| `src/api/client.js`, `src/api/errorCodes.js` | Step 0 (이후 동결) |
| `src/components/common/**` (StateBlock 등) | Step 0 (이후 동결) |
| `src/styles/tokens.css`, `src/utils/format.js` | Step 0 (이후 동결) |
| `src/App.vue` | Step 0 (이후 동결) |
| `src/components/layout/AppHeader.vue` | Step 0 → 이후 **A** |
| `src/api/auth.js` | Step 0 → 이후 **A** |
| `src/constants/audio.js` | Step 0 → 이후 **B** |
| `src/api/users.js`, `api/history.js`, `api/stats.js` | **A** |
| `src/views/auth/**`, `views/account/**`, `views/dashboard/**`, `views/stats/**` | **A** |
| `src/components/chart/**`, `components/history/**`, `components/auth/**`, `components/stats/**`, `components/account/**` | **A** |
| `src/composables/useAuthActions.js`, `usePagination.js`, `useSeoulDate.js` | **A** |
| `src/utils/validators.js` | **A** |
| `src/api/submission.js`, `api/analysisStatus.js`, `api/proAnalysis.js`, `api/mock.js` | **B** |
| `src/stores/submission.js` | **B** |
| `src/views/practice/**`, `views/dev/**` | **B** |
| `src/components/waveform/**`, `components/recorder/**`, `components/practice/**` | **B** |
| `src/composables/useRecorder.js`, `useAnalysisStatus.js`, `useAudioFile.js`, `useOnlineStatus.js` | **B** |
| `src/utils/audio.js`, `src/utils/idempotency.js` | **B** |
| `src/views/NotFoundView.vue`, `src/views/NotReadyView.vue` | Step 0 (이후 동결) |

`components/` · `composables/` · `api/` 폴더는 **파일 단위로** 나눠 가집니다.
폴더가 같아도 파일이 다르면 충돌하지 않습니다.

---

## 6. 충돌 방지 3규칙

1. **파일 1개 = 소유자 1명.** §5 표 밖의 파일을 만들거나 고치지 않습니다.
2. **공용 파일은 Step 0에서 동결합니다.** 이후 필요한 확장은 **자기 소유의 새 파일**로 만듭니다.
   - 특히 `router/index.js`: Step 0에서 라우트 15개를 **미리 전부 선언**합니다.
     화면을 나중에 만들더라도 라우트를 추가할 일이 없습니다.
   - 특히 `api/client.js`: 헤더·토큰·에러 매핑은 여기 한 곳입니다.
     각자는 `api/<자기모듈>.js`만 추가합니다.
3. **상대 코드를 기다리지 않습니다.** B1·B2는 백엔드도 Step 0도 없이 착수합니다.
   A는 백엔드 `db/seed-dev.sql`이 넣어 줄 데이터로 조회 화면 8개를 전부 검증합니다.

---

## 7. 간섭 감사표 — 공유 지점 9개와 제거 방법

설계 단계에서 "두 사람이 같은 파일을 만지게 될 지점"을 미리 조사해 구조로 없앤 기록입니다.

| # | 공유 지점 | 왜 충돌하는가 | 제거 방법 |
| --- | --- | --- | --- |
| 1 | `router/index.js` | 화면을 하나 만들 때마다 라우트를 추가 → 매번 충돌 | **Step 0에서 라우트 15개를 전부 선언**하고, 컴포넌트는 `import.meta.glob`으로 **존재하는 화면만** 매핑한다. 없는 화면은 `NotReadyView`로 떨어져 앱이 정상 기동하고, 화면을 새로 만들면 glob이 자동으로 잡으므로 **이 파일을 고칠 일이 영영 없다** |
| 2 | axios 인스턴스 | A는 Authorization, B는 multipart 헤더를 각자 넣고 싶어진다 | `api/client.js` 동결. 요청별 헤더는 **호출부에서 `config` 인자로** 넘긴다 |
| 3 | 401 재발급 | 두 사람이 각자 재시도를 짜면 reissue가 폭주한다 | Step 0에 **단일 비행 훅** 하나. 각자는 401을 아예 보지 않는다 |
| 4 | 세션 상태 | 둘 다 `user`·`plan`을 읽는다 | `stores/session.js` 동결. **쓰기는 A1만**, B는 읽기만 |
| 5 | 앱 헤더 | plan 배지·로그아웃은 A, 녹음 버튼은 B가 넣고 싶어진다 | `AppHeader.vue`는 **A 단독 소유**. B의 진입점은 절 화면의 `router-link`로, 링크 대상 라우트는 Step 0에서 이미 선언됨 |
| 6 | 그래프 코드 | A의 추이 차트와 B의 파형이 같은 `components/chart`에 몰린다 | A는 `components/chart/**`(SVG), B는 `components/waveform/**`(Canvas). **폴더로 분리** |
| 7 | PRO 잠금 UI | 상세 화면(A6)과 PRO 화면(B6) 양쪽에 필요 | `ProLockCard.vue`는 **A6 소유**. B6은 403을 받으면 `/upgrade` 라우트로 **이동만** 한다 |
| 8 | `pro.detailUrl` | 서버가 준 `/api/v1/recordings/101/pro-analysis`를 그대로 `href`로 쓰면 404 | **라우터 `name`으로 이동한다**(`{ name: 'proAnalysis', params }`). `detailUrl`은 "열 수 있다"는 신호로만 읽는다 |
| 9 | 검증할 데이터 | A의 조회 화면에 B의 업로드가 있어야 볼 게 생긴다 | 백엔드 Step 0의 `db/seed-dev.sql` (COMPLETED 2건·PENDING 1건·FAILED 1건) |

### 단 하나의 결합: Step 0

axios 인스턴스·세션 스토어·라우트 표·에러 코드 27개는 논리적으로 먼저 있어야 합니다.
이건 없앨 수 없어서, 대신 **붙여넣기 20~40분짜리 1회 체크포인트**로 만들었습니다.
B는 그마저도 기다릴 필요가 없습니다 — **B1·B2는 Vue·Vite 외 의존성이 0**인 순수 브라우저 코드입니다.

### 작업 독립성이 성립하는 근거

1. **빌드 독립** — A의 화면은 A 소유 파일만 import하고, B도 대칭입니다.
   서로의 컴포넌트·composable·api 모듈을 import하는 코드가 **한 줄도 없습니다.**
   화면 파일이 하나도 없는 상태에서도 앱이 뜬다는 것을 **실제로 확인했습니다**
   (Vite 8 + `import.meta.glob`, Step 0 §5-7).
2. **런타임 독립** — 남는 건 코드 의존이 아니라 *데이터* 의존이고, `seed-dev.sql`이 덮습니다.
3. **머지 독립** — §5 소유권 표에 같은 경로가 두 번 나오지 않습니다.

확인 명령 (출력이 비어 있어야 성공):

```bash
comm -12 <(git diff --name-only frontend...feat/fe-auth-base | sort) \
         <(git diff --name-only frontend...feat/fe-recorder | sort)
```

---

## 8. 설계 단계에서 잡은 치명적 결함 5건

각 작업 문서의 `## 6. 함정` 절에 다시 실려 있습니다.

| # | 결함 | 증상 | 조치 |
| --- | --- | --- | --- |
| **FC1** | `accessToken`을 `localStorage`에 저장 | XSS 한 번에 14일짜리 세션이 통째로 털린다. 명세가 Refresh를 HttpOnly 쿠키로 둔 이유가 무의미해진다 | **메모리(Pinia)에만** 둔다. 새로고침 복구는 `POST /auth/reissue` — Refresh 쿠키는 이제 있다 |
| **FC2** | Vite dev proxy로 `/api`를 프록시 | 프록시를 거치면 요청 `Origin`이 바뀌거나 사라진다 → `/auth/*` **4개가 전부 `403 ORIGIN_NOT_ALLOWED`**. "로그인만 안 되는" 형태라 원인을 찾기 어렵다 | **프록시를 쓰지 않는다.** `VITE_API_BASE_URL`로 `http://localhost:8080/api/v1` 직결 + 백엔드 `APP_ORIGIN=http://localhost:5173`. Vite는 `strictPort: true`로 5174 밀림을 막는다 |
| **FC3** | 401마다 각자 재발급 | 화면 하나가 4개를 병렬 호출하면 reissue가 4번 나가고, 늦게 도착한 응답이 새 토큰을 덮어쓴다 | Step 0의 **단일 비행 훅**. 진행 중인 reissue Promise를 공유하고 끝나면 원요청을 재시도 |
| **FC4** | 폴링 타이머 미정리 | 분석 진행 화면을 벗어나도 1초마다 요청이 계속 나가고, 여러 번 들어오면 타이머가 누적된다 | `onUnmounted`에서 반드시 `stop()`. 라우트 이탈·탭 비활성 둘 다 처리 |
| **FC5** | `?? 0`로 null을 0으로 | 명세의 "미완료는 `null`이지 0회가 아니다"가 깨진다. **분석 중인 기록이 "추임새 0회"로 보인다** | `formatCount()` 한 곳에서 `null → "—"`. `?? 0`, `\|\| 0`을 쓰지 않는다 |

---

## 9. 브랜치·커밋 규칙

루트 `AGENTS.md` §8을 그대로 따릅니다.

| 작업 | 분기 기준 | 브랜치명 | 커밋 scope |
| --- | --- | --- | --- |
| Step 0 | `frontend` | `feat/fe-foundation` | `fe` |
| A1~A8 | `frontend` | `feat/fe-<작업>` | `fe` |
| B1~B8 | `frontend` | `feat/fe-<작업>` | `fe` |
| 문서·명세 | `develop` | `docs/fe-<작업>` | `docs` |

- `main` · `develop` · `backend` · `frontend`에 **직접 push하지 않습니다.** 항상 PR입니다.
- 커밋 메시지는 한국어 Conventional Commits. **AI 공동 저자 표기·생성 문구를 넣지 않습니다**
  (`AGENTS.md` §8.5).
- FE↔BE 통합 검증은 `develop`에서만 됩니다. `frontend` 브랜치에 머지해 놓고 통합이 됐다고
  착각하지 마세요.

---

## 10. 통합 체크리스트

양쪽 P0가 끝난 시점에 `develop`에서 확인합니다.

- [ ] `docker compose down -v && docker compose up -d` → `npm run dev` 로 클린 기동
- [ ] 회원가입 201 → 헤더에 이름·`FREE` 배지 표시
- [ ] **브라우저 새로고침 → 로그인 화면으로 튕기지 않는다** (FC1 복구 경로)
- [ ] 로그아웃 204 → 보호 화면 접근 시 `/login`으로 이동
- [ ] 녹음 3초 → 제출 202 → `/analyses/{id}`로 이동
- [ ] 진행 화면에서 **`PENDING` → `PROCESSING` → `COMPLETED` 전이가 눈으로 보인다**
- [ ] 완료 후 상세 화면에서 `fillerTotalCount == sum(fillerBreakdown.count)`
- [ ] 목록이 `submittedAt` 내림차순이고, 분석 중 항목의 추임새가 **`0`이 아니라 `—`**
- [ ] FREE 계정에서 잠금 기능 **9개**가 표시되고 `/upgrade`로 유도된다
- [ ] PRO 계정에서 `/recordings/{id}/pro`가 파형·타임라인·구간 3개를 그린다
- [ ] 분석 진행 화면을 벗어난 뒤 **네트워크 탭에 status 요청이 더 이상 쌓이지 않는다**
- [ ] 실패한 분석에서 **재시도 버튼**이 보이고, 같은 파일로 재시도하면 202

---

## 11. `AGENTS.md` 정합성 — 이 PR에서 반영한 것과 남은 것

기존 `AGENTS.md`들은 v3.0.0 이전, `/api/transcriptions` 한 개 API를 전제로 쓰여 있었습니다.
그대로 두면 두 문서를 동시에 따르는 것이 불가능하므로, **FE↔BE 계약에 해당하는 항목은
이 PR에서 함께 고쳤습니다.** 도메인 서술은 발표 서사에 직결되어 PM 판단으로 남깁니다.

### 11.1 이 PR에서 반영 완료

| # | 파일·절 | 이전 | v3.0.0 기준 |
| --- | --- | --- | --- |
| 1 | 루트 `AGENTS.md` §5.4 | 에러가 flat `{ code, message, detail }` | envelope `{ success, data, error }` → 에러는 `error.code` / `error.message` |
| 2 | 루트 `AGENTS.md` §5.1 | `/api` 프리픽스, 계층 2단계까지 | `/api/v1` 프리픽스, 3단계 허용 (`/dashboard/recordings/{id}/status`) |
| 3 | 루트 `AGENTS.md` §5.3 | 상태코드 표에 409·410·415·503·408 부재 | 비동기·충돌·게이팅 코드까지 정리 |
| 4 | 루트 `AGENTS.md` §6.2 | `status`는 `pending\|completed\|failed` 3값 고정 | `PENDING\|PROCESSING\|COMPLETED\|FAILED` 4값 |
| 5 | 루트 `AGENTS.md` §2 | 저장소 구조에 `docs/frontend/`·`docs/backend/` 없음 | 추가 |
| 6 | `frontend/AGENTS.md` §1 | "상태관리는 필요해지기 전까지 도입하지 않는다" | **Pinia 도입 확정** — 세션 토큰·plan을 라우터 가드와 axios 인터셉터가 동시에 읽어야 한다 |
| 7 | `frontend/AGENTS.md` §2 | 폴더 구조가 `api/transcriptions.js` 기준 | `stores/`·`constants/`·`styles/`·`utils/`와 컴포넌트 하위 폴더 반영 |
| 8 | `frontend/AGENTS.md` §3.1 | `client.js` 인터셉터가 `err.response.data.code`를 읽음 | `err.response.data.error.code`로 한 단계 깊어짐. 성공은 `res.data.data` |
| 9 | `frontend/AGENTS.md` §3.1 예시 | `/api/transcriptions`, 필드 `file`, `{ jobId, status, text }` | `/recordings`, 필드 `audio`, `Idempotency-Key` 필수, `{ recordingId, analysisId, status, attemptNo, autoRetryCount }` |
| 10 | `frontend/AGENTS.md` §5 | 허용 `mp3·m4a·wav·webm` / 최대 **10MB** | 허용 `audio/webm·mp4·ogg·wav·mpeg` / 최대 **16MiB** / 길이 **1~60초** |
| 11 | 루트 `.env.example` | `APP_ORIGIN` 없음 | 빈 값으로 자리 추가 (FC2 검증의 근거) |

### 11.2 남은 항목 — PM 확인 후 별도 PR

도메인 서술은 이 프로젝트의 "한 줄 주장"과 발표 로드맵에 직결되므로 임의로 바꾸지 않았습니다.

| 파일·절 | 현재 | 검토가 필요한 이유 |
| --- | --- | --- |
| 루트 `AGENTS.md` §1 | AI 확장 지점 = "음성 파일 업로드 → Whisper 전사" | v3.0.0은 스피치 습관 분석이다. 다만 확장 지점의 **구조**는 동일하다 |
| 루트 `AGENTS.md` §6.1 | `TranscriptionClient` 인터페이스 + `MockTranscriptionClient` | v3.0.0의 `MOCK_001`·`MOCK_002`(`/mock/waveform-analysis`·`/mock/transcript-analysis`)가 **사실상 같은 Mock 심(seam)**이다. §6 구조는 유지한 채 명칭만 옮기면 된다 |
| 루트 `AGENTS.md` §6.3 | `transcriptions` 단일 테이블 | v3.0.0은 `recordings` / `analyses` 분리 |
| `backend/AGENTS.md` 전반 | 전사 도메인 기준 | 백엔드 담당·PM 합의 사항 |

`baseURL` 한 줄만 바꿔 Mock↔실서버를 전환한다는 `frontend/AGENTS.md` §3.2의 주장은
**그대로 유효합니다.** 다만 기존 Postman Mock 응답을 envelope 형태로 다시 만들어야 합니다.
