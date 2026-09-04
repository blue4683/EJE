# AGENTS.md

이 저장소에서 작업하는 모든 AI 에이전트(Claude Code, Codex, Cursor 등)와 사람이 함께 지키는 규칙입니다.
**작업을 시작하기 전에 이 파일을 끝까지 읽고, 해당 폴더의 `AGENTS.md`도 함께 읽으세요.**

---

## 1. 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 목적 | 당장 AI를 실제로 호출하지는 않되, **나중에 붙일 자리를 코드에 미리 만들어 둔** 웹 서비스 설계 |
| AI 확장 지점 | **음성 파일 업로드 → Whisper 전사(transcription)** |
| 기간 | 3일 (1일차 기획·아키텍처 / 2일차 설계·스캐폴딩 / 3일차 검증·발표) |
| 평가 기준 | 완성도가 아니라 **설계의 논리성과 확장성** |
| 팀 | 5명 (PM·UX·DA·API·FE·BE·DevOps 겸임) |

> 이 프로젝트는 "많이 만드는 것"이 목표가 아닙니다.
> **핵심 화면 1~2개가 FE → BE → DB까지 실제로 관통하는 것**이 목표입니다.
> 요청받지 않은 기능을 추가로 만들지 마세요.

### 이 프로젝트의 한 줄 주장

> **"전사 기능이 들어올 자리는 이미 코드에 있고, 지금은 Mock 구현체가 그 자리에 꽂혀 있습니다.
> 인터페이스를 구현한 클래스 하나만 추가하면 실제 전사가 동작합니다."**

3일 내내 모든 코드는 이 문장을 뒷받침하는 방향으로만 작성합니다.

---

## 2. 저장소 구조

```
.
├── frontend/          Vue 3 + Vite              → frontend/AGENTS.md
├── backend/           Java 21 + Spring Boot 4.1 → backend/AGENTS.md
├── docs/
│   ├── api/           API 명세 (단일 원본 — 코드보다 우선한다, §3-2)
│   ├── frontend/      프론트엔드 단계별 구현 문서 (담당자별 분업)
│   ├── backend/       백엔드 단계별 구현 문서
│   ├── erd/           ERD 이미지 및 DBML
│   ├── wireframe/     화면 설계
│   ├── capture/       발표용 산출물 캡처 (모든 결과물은 여기에)
│   ├── plan.md        3일 일정 및 Gate
│   ├── e2e-test.md    End-to-End 검증 시나리오
│   └── risk.md        리스크 로그
├── .env.example
└── AGENTS.md          (이 파일)
```

**AGENTS.md 우선순위** — 하위 폴더 파일이 루트보다 우선합니다.
`frontend/`에서 작업할 때는 루트 + `frontend/AGENTS.md`를 적용하고, `backend/`의 규칙은 무시합니다.

### 시스템 구성

```
Browser ──▶ Spring Boot (:8080) ──▶ TranscriptionClient (인터페이스)
                  │                        └── MockTranscriptionClient   ← 유일한 구현체
                  │
                  │                        ┄┄ (향후) 실제 전사 구현체 ─▶ Whisper API
                  └──▶ PostgreSQL
```

백엔드는 **Spring Boot 하나**입니다. 별도의 AI 서비스도, AI 라이브러리도 지금은 없습니다.
점선은 **아직 구현하지 않은 확장 지점**입니다 — 인터페이스만 존재합니다.

---

## 3. 절대 금지 (Never)

에이전트가 이 중 하나라도 어기면 그 변경은 되돌립니다.

1. **`.env`, API Key, DB 접속 정보를 파일에 하드코딩하거나 커밋하지 않는다.** 새 설정값이 필요하면 `.env.example`에 **빈 값으로** 추가하고 README에 설명을 적는다.
2. **`docs/api/`의 API 명세와 다른 응답 형태를 만들지 않는다.** 명세를 바꿔야 한다면 코드보다 명세를 먼저 고치고, 커밋을 분리한다.
3. **화면에 하드코딩한 더미 배열을 렌더링하지 않는다.** 데이터는 반드시 HTTP 응답에서 온다. (Mock API도 HTTP다)
4. **`main`·`develop`·`backend`·`frontend`에 직접 push 하지 않는다.** 항상 `feat/*` 브랜치 → PR (§8.1).
5. **요청받지 않은 리팩터링·파일 이동·의존성 추가·포맷 일괄 변경을 하지 않는다.** 3일짜리 프로젝트에서 대규모 diff는 리뷰 불가능하다.
6. **AI·외부 전사 라이브러리를 추가하지 않는다.** Spring AI, OpenAI SDK 등 어느 것도 `build.gradle`에 넣지 않는다. 전사는 `TranscriptionClient` 인터페이스와 Mock 구현체로만 다룬다 (§6).
7. **외부 API를 실제로 호출하는 코드를 작성하지 않는다.** 어떤 API Key도 필요 없이 `./gradlew bootRun` 만으로 전 기능이 동작해야 한다.
8. **커밋·PR에 AI 공동 저자 표기나 생성 문구를 넣지 않는다.** `Co-Authored-By: Claude …`, `Generated with …` 모두 금지 (§8.5).
9. **테스트/실행으로 확인하지 않은 코드를 "완료"라고 보고하지 않는다.**

---

## 4. 실행 방법

전제: **JDK 21**, Node 20 이상.

```bash
# Frontend  (기본 포트 5173)
cd frontend && npm install && npm run dev

# Backend   (기본 포트 8080) — 외부 API Key 불필요
cd backend && ./gradlew bootRun
```

**어떤 환경변수도 없이 백엔드가 뜨고 전사 기능이 끝까지 동작해야 합니다** (DB 접속 정보 제외).
이 조건이 3일차 데모의 안정성을 보장합니다.

포트를 바꾸면 `frontend/.env`의 `VITE_API_BASE_URL`과 `docs/e2e-test.md`도 같이 고칩니다.
`java -version`이 21이 아니면 빌드가 실패합니다. 팀원 전원이 1일차에 JDK 21을 맞춥니다.

---

## 5. FE ↔ BE 공통 규약

이 절은 프론트·백엔드가 **똑같이** 지켜야 하는 계약입니다. 어느 한쪽만 바꾸면 통합이 깨집니다.

### 5.1 URL

- **버전 프리픽스는 `/api/v1`** 입니다. 백엔드 `context-path`가 여기까지 포함하고,
  프론트 `VITE_API_BASE_URL`도 `http://localhost:8080/api/v1` 처럼 **끝까지** 지정합니다.
  그래서 프론트 호출은 `client.post('/auth/login')` 이 맞습니다 — 경로를 두 번 붙이지 마세요.
- 리소스는 **복수형 명사**, 소문자, 단어 구분은 하이픈: `/recordings`, `/dashboard/recent-analyses`
- **URL에 동사를 쓰지 않는다.** `/api/v1/getRecording` ❌ → `GET /api/v1/recordings/{id}` ✅
  (예외: `POST /analyses/{id}/retry` 처럼 **상태 전이를 일으키는 하위 동작**은 허용합니다)
- 계층은 **3단계까지** — `/dashboard/recordings/{recordingId}/status` 가 최대 깊이입니다

### 5.2 JSON 필드는 `camelCase`, 시간은 ISO 8601 UTC

```json
{ "jobId": "tr_01H8...", "status": "pending", "originalFilename": "회의녹음.m4a", "createdAt": "2026-09-02T05:20:00Z" }
```

- ID는 문자열 또는 숫자, boolean은 `is`/`has` 접두어
- **DB는 snake_case, JSON은 camelCase.** 변환은 DTO에서만 일어난다

### 5.3 상태 코드

| 상황 | 코드 |
| --- | --- |
| 조회·수정 성공 | `200` |
| 생성 성공 | `201` |
| 삭제 성공 (본문 없음) | `204` |
| **비동기 작업 접수** | `202` + `{ "analysisId": "...", "status": "PENDING", ... }` |
| 요청 형식 오류 | `400` |
| 인증 실패·토큰 만료 | `401` |
| 권한 부족 (등급·Origin) | `403` |
| 리소스 없음 **또는 남의 리소스** | `404` |
| 요청 시간 초과 | `408` |
| 상태 충돌 (진행 중·중복·재시도 한도) | `409` |
| 삭제되어 사라진 리소스 | `410` |
| 업로드 용량 초과 | `413` |
| 지원하지 않는 미디어 타입 | `415` |
| 유효성 검증 실패 (길이·손상·파라미터 등) | `422` |
| 서버 오류 | `500` |
| 용량 초과·일시적 이용 불가 | `503` |

- **남의 리소스와 없는 리소스는 똑같이 `404`** 입니다. 존재 여부가 노출되면 안 됩니다.
- 등급 검사보다 **소유권 검사가 먼저**입니다. 남의 기록에 `403`을 주면 그 ID의 존재가 드러납니다.
- `415`(형식이 허용 목록에 없음)와 `422`(형식은 맞는데 손상·0바이트)를 구분합니다.

### 5.4 모든 응답은 하나의 envelope 형태만

성공·실패를 가리지 않고 **`{ success, data, error }`** 로 감쌉니다.

```json
// 성공 — error 는 null
{ "success": true, "data": { "recordingId": "101", "status": "PENDING" }, "error": null }

// 실패 — data 는 null
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNSUPPORTED_MEDIA_TYPE",
    "message": "지원하지 않는 음성 형식입니다."
  }
}
```

- `error.code`는 대문자 스네이크, `error.message`는 **사용자에게 그대로 보여줄 한국어 문장**
- FE는 `error.code`로 **행동을 분기**하고 `error.message`를 **그대로** 화면에 노출한다
  (별도 문구를 만들지 않는다)
- **성공 응답의 본문은 `res.data.data`** 입니다 — envelope이라 한 단계 더 들어갑니다.
  FE는 `unwrap()` 헬퍼 한 곳에서만 벗깁니다 (`frontend/AGENTS.md` §3.1).
- `204`에는 본문이 없습니다. envelope도 오지 않습니다.
- 모든 응답에 `Cache-Control: no-store`를 붙입니다.

---

## 6. AI-Ready 규약 — 이 프로젝트의 핵심

**이 프로젝트는 AI 라이브러리를 하나도 넣지 않습니다. Spring AI도 쓰지 않습니다.**
3일 안에 검증할 수 없는 것(라이브러리 버전 충돌, API Key, 요금, 네트워크)을 끌고 오는 대신,
**나중에 무엇을 끼워 넣든 받아낼 수 있는 자리**를 코드에 만들어 두는 것이 우리 방식입니다.

그래서 AI-Ready의 증거는 `build.gradle`의 의존성이 아니라 아래 네 가지입니다.
발표에서도 이 네 가지를 순서대로 보여 줍니다.

> **정합성 메모** — §6.2의 엔드포인트·상태값은 API 명세 v3.0.0에 맞춰 갱신했습니다.
> 반면 §6.1의 `TranscriptionClient`·§6.3의 `transcriptions` 테이블은 **아직 이전 이름 그대로**입니다.
> v3.0.0의 `MOCK_001`·`MOCK_002`(`/mock/waveform-analysis`·`/mock/transcript-analysis`)가
> 사실상 같은 Mock 심(seam)이라 **구조는 그대로 유효**하지만, 명칭 정리는 발표 서사에 직결되므로
> PM 판단으로 남겨 두었습니다. 자세한 목록은 `docs/frontend/README.md` §11.2 참조.

### 6.1 전사 호출은 인터페이스 뒤에 둔다

```java
public interface TranscriptionClient {
    TranscriptionResult transcribe(Path audioFile, String languageCode);
}

public record TranscriptionResult(String text, String model) {}
```

구현체는 **지금 하나뿐**입니다.

| 구현체 | 상태 | 동작 |
| --- | --- | --- |
| `MockTranscriptionClient` | 유일한 구현체 (`@Component`) | 2초 지연 후 고정된 한국어 전사 텍스트 반환, `model = "mock"` |

서비스는 **인터페이스 타입만** 참조합니다. 구현체 클래스명을 서비스가 알면 안 됩니다.
이렇게 두면 실제 연동은 **이 인터페이스를 구현한 클래스 하나를 추가하는 범위**로 끝납니다.

### 6.2 전사 API는 비동기 형태로 설계한다

전사는 오디오 길이에 비례해 수 초~수십 초가 걸리는 작업입니다.
지금 Mock이 즉시 답할 수 있다고 동기 200으로 만들면, 나중에 구조를 통째로 다시 짜야 합니다.

```
POST /api/v1/recordings                     (multipart: audio)  → 202 { "analysisId", "status", ... }
GET  /api/v1/analyses/{analysisId}/status                       → 200 { "status", "failureCode", ... }
```

`status`는 **`PENDING` | `PROCESSING` | `COMPLETED` | `FAILED`** 네 값만 사용합니다.
대문자 스네이크이며, 다른 문자열을 만들지 마세요.

> `PROCESSING`이 따로 있는 이유 — "접수됐다"와 "지금 돌고 있다"가 구분되어야
> 진행 화면이 상태 전이를 보여줄 수 있습니다. 그게 이 설계를 화면으로 증명하는 방법입니다.

**분석 실패는 HTTP 오류가 아닙니다.** 조회는 `200`이고 `data.status = FAILED` 와
`failureCode`로 표현합니다. FE가 이걸 예외로 던지면 폴링이 catch로 빠져
정상 흐름이 오류 화면으로 뒤집힙니다. HTTP 오류는 리소스가 없을 때(`404`)뿐입니다.

전사는 스레드를 오래 붙잡는 blocking I/O입니다. Java 21 + Spring Boot 4를 쓰므로
**가상 스레드를 켜서**(`spring.threads.virtual.enabled: true`) 이 부담을 없앱니다.
"왜 큐 없이도 버티나요?"라는 질문에 대한 우리 팀의 답이 이것입니다.

### 6.3 결과는 처음부터 저장 자리를 갖는다

```
transcriptions(
  id, job_id, original_filename, stored_path, content_type, size_bytes,
  status, model, language, result_text, error_message,
  created_at, updated_at
)
```

Mock 결과가 들어가더라도 **`model`, `language` 컬럼은 존재해야** 합니다.
`model`에는 지금 `mock`이 들어가고, 실제 연동 시 `whisper-1` 같은 값이 들어갑니다 —
이 컬럼 하나가 "어떤 레코드를 무엇으로 처리했는가"를 증명하고, 마이그레이션 없이 전환됩니다.

### 6.4 설정 자리는 미리 비워 둔다

`.env.example`과 `application.yml`에 전사 관련 설정 **키를 미리 만들어 두되, 값은 비워 둡니다.**

```yaml
transcription:
  provider: ${TRANSCRIPTION_PROVIDER:mock}   # mock | (향후) whisper
  model: ${TRANSCRIPTION_MODEL:mock}
  language: ${TRANSCRIPTION_LANGUAGE:ko}
```

지금은 `mock`만 유효한 값입니다. **외부 API Key는 아직 어떤 파일에도 등장하지 않습니다.**
설정을 읽는 통로가 이미 있으므로, 나중에 값만 채우면 됩니다.

### 6.5 향후 실제 연동 — 발표 로드맵 슬라이드용

실제 전사를 붙이는 작업은 **의존성 1개 + 클래스 1개**입니다. 그 외에는 아무것도 바뀌지 않습니다.

| 바뀌는 것 | 바뀌지 않는 것 |
| --- | --- |
| `build.gradle`에 클라이언트 라이브러리 1줄 | API 명세, 프론트엔드 코드 |
| `TranscriptionClient` 구현체 클래스 1개 추가 | 컨트롤러, 서비스, 리포지토리 |
| `.env`에 키·모델 값 채우기 | DB 스키마 |

구현 수단은 그때 고르면 됩니다 — Spring AI, OpenAI Java SDK, 또는 `RestClient`로 직접 호출.
**어느 쪽을 골라도 위 표가 바뀌지 않는다는 것**이 이 설계의 요점입니다.
지금 특정 라이브러리를 고르지 않은 것 자체가 의도된 선택이며, Q&A에서 그렇게 답하세요.

---

## 7. 데이터베이스 규칙

- 테이블명: **snake_case 복수형** (`transcriptions`, `audio_tags`)
- 컬럼명: snake_case
- 모든 테이블에 `id`(PK), `created_at`, `updated_at`
- FK는 `{단수형}_id` (`transcription_id`)
- **N:M 관계는 반드시 조인 테이블로 분해**한다
- 오디오 파일 자체는 DB에 넣지 않는다. 파일은 디스크(`backend/uploads/`, git 제외), DB에는 경로만
- 스키마를 바꾸면 `docs/erd/`의 DBML과 이미지를 **같은 PR에서** 갱신한다

---

## 8. Git 규칙

### 8.1 브랜치

```
main                      발표·제출용. 항상 실행 가능한 상태
└── develop               FE·BE가 만나는 통합 브랜치. E2E 검증은 여기서만 가능
    ├── backend           백엔드 통합 브랜치
    │   └── feat/{역할}-{작업}
    └── frontend          프론트엔드 통합 브랜치
        └── feat/{역할}-{작업}

fix/{내용}                  develop에서 분기 → develop으로 머지
docs/{내용}                 develop에서 분기 → develop으로 머지
```

**머지 방향**

```
feat/{역할}-{작업}  →  backend | frontend  →  develop  →  main
```

역방향(develop의 변경을 backend/frontend로 내리는 것)은 주기적으로 merge 해서 최신 상태를 유지합니다.

**직접 push 금지 브랜치**: `main`, `develop`, `backend`, `frontend` — 전부 PR로만 들어갑니다.

**PR 승인 기준**

| 머지 방향 | 승인 |
| --- | --- |
| `feat/*` → `backend` / `frontend` | 같은 파트 팀원 1인 |
| `backend` / `frontend` → `develop` | DevOps 담당 확인 (통합 지점) |
| `develop` → `main` | PM + DevOps가 Gate 판정 후 |

**작업 브랜치 이름** — `feat/{역할}-{작업}`

git 브랜치 이름은 계층이 아니라 평면이라, `backend`·`frontend` 양쪽에서 같은 이름을 만들면 충돌합니다.
**역할 접두어가 그 충돌을 막는 장치**이므로 생략하지 마세요.
접두어는 커밋 메시지의 scope(§8.2)와 같은 단어를 씁니다 — 하나만 외우면 됩니다.

| 접두어 | 분기 기준 | 예시 |
| --- | --- | --- |
| `be` | `backend` | `feat/be-transcription-api` |
| `ai` | `backend` | `feat/ai-mock-transcription-client` |
| `fe` | `frontend` | `feat/fe-upload-view` |
| `db` | `backend` | `feat/db-transcription-table` |
| `api` | `develop` | `feat/api-openapi-spec` |
| `infra` | `develop` | `feat/infra-github-actions` |

코드 작업은 `backend` / `frontend`에서 분기하고, **명세·문서·설정처럼 양쪽에 걸치는 작업은 `develop`에서 분기**합니다.
작업 부분은 소문자 하이픈, 명사구로 짧게 (`feat/fe-upload-view`, `feat/be-file-validation`).

**꼭 기억할 것 두 가지**

1. **FE-BE 통합 검증은 `develop`에서만 됩니다.** 2일차 14시 통합 시도 전에 양쪽 작업이 `develop`까지 올라와 있어야 합니다. `backend`/`frontend` 브랜치에만 머지해 두고 통합이 됐다고 착각하지 마세요.
2. **발표 PC는 `main`을 clone합니다.** Gate를 통과한 시점(1일차 EOD / 2일차 EOD / 3일차 발표 전)에만 `develop` → `main` 머지를 하고, 그 직후 반드시 클린 클론 기동을 확인합니다.

### 8.2 커밋 메시지 — Conventional Commits + 한글 본문

```
<type>(<scope>): <한글 요약 50자 이내, 마침표 없음>

<본문: 왜 이렇게 했는지 한글 1~3줄. 필요할 때만>
```

**type**

| type | 사용 시점 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서·명세·README |
| `style` | 포맷·세미콜론 등 동작 변화 없음 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `chore` | 설정·의존성·빌드 |

**scope**: `fe` · `be` · `ai` · `db` · `api` · `docs` · `infra`
→ 전사 인터페이스·Mock 구현체·비동기 잡 처리 등 **AI 확장 지점과 관련된 변경은 `be`가 아니라 `ai`** 를 씁니다. 커밋 그래프에서 AI-Ready 작업이 한눈에 보이게 하기 위함입니다.

**좋은 예**

```
feat(be): 오디오 파일 업로드 API 구현
feat(ai): 전사 요청 엔드포인트 추가 (202 + jobId)

Whisper 호출이 오래 걸릴 것을 전제로 비동기 접수 구조를 먼저 세웠다.
실제 처리는 MockTranscriptionClient가 대신한다.

feat(ai): TranscriptionClient 인터페이스 및 Mock 구현체 추가
feat(fe): 음성 업로드 화면 및 전사 상태 폴링 연동
fix(be): 지원하지 않는 확장자 업로드 시 500이 반환되던 문제 수정
docs(api): OpenAPI 명세에 전사 엔드포인트 추가
chore(ai): 전사 설정 프로퍼티 자리 추가 (provider=mock)
```

**나쁜 예**

```
update              ← 무엇을 했는지 알 수 없음
수정                 ← type/scope 없음
작업중               ← 미완성 코드를 올리지 않는다
feat: 전사 API랑 화면이랑 ERD 수정   ← 한 커밋에 여러 관심사
```

### 8.3 커밋 단위

- **하나의 커밋은 하나의 관심사.** FE 변경과 BE 변경은 커밋을 나눈다.
- 에이전트는 **작업이 실행 가능한 상태가 된 시점에만** 커밋한다.
- 커밋 전 항상 `git status`로 `.env`와 `uploads/`가 포함되지 않았는지 확인한다.

### 8.4 PR

제목은 커밋 메시지와 같은 형식. 본문에 아래를 적습니다.

```markdown
## 무엇을
## 왜
## 확인 방법
- [ ] 머지 대상 브랜치가 맞는지 확인 (feat → backend/frontend, 통합 → develop)
- [ ] 로컬에서 실행 확인 (API Key 없이 기동되는지 포함)
- [ ] API 명세와 응답 형태 일치 확인
- [ ] .env / 키 / 업로드 파일이 포함되지 않았는지 확인
```

PR 본문에도 AI 생성 문구를 넣지 않습니다 (§8.5).

**PR을 열 때 base 브랜치를 반드시 확인하세요.** `feat/*` PR이 실수로 `main`을 향하는 것이
이 전략에서 가장 흔한 사고입니다. GitHub 레포 설정에서 **기본 브랜치를 `develop`으로 바꿔 두면**
PR base가 `main`으로 잡히는 일이 줄어듭니다. (DevOps가 1일차 오전에 처리)

### 8.5 커밋 작성자 — AI 공동 저자 표기 금지

이 저장소의 커밋·PR에는 **AI 도구가 작성자나 공동 작성자로 남지 않습니다.**
평가 대상이 팀원 각자의 기여 기록이고, GitHub Contributors 목록에 사람만 올라가야 하기 때문입니다.

**금지**

- `Co-Authored-By: Claude <noreply@anthropic.com>` 등 AI 계정 트레일러 (커밋 본문 마지막 줄)
- `🤖 Generated with Claude Code`, `Co-authored-by: Codex` 같은 생성 문구 — **커밋 메시지와 PR 본문 양쪽 모두**
- `git commit --author=...` 로 작성자 바꾸기
- AI 계정을 레포 Collaborator로 초대

**지켜야 할 것**

- 커밋은 **실제로 작업한 팀원의 GitHub 계정**으로 남는다. 1일차에 각자 확인:

```bash
git config user.name          # GitHub 사용자명
git config user.email         # GitHub 계정에 등록된 이메일과 일치해야 함
```

- 에이전트에게 커밋을 맡기더라도 메시지는 **§8.2 형식만** 쓴다. 서명·출처·홍보 문구를 덧붙이지 않는다.
- **Squash 머지 주의** — GitHub은 squash할 때 각 커밋의 트레일러를 모아 본문에 넣습니다.
  브랜치 커밋 중 하나에라도 `Co-Authored-By`가 있으면 `develop` 커밋에 그대로 딸려옵니다.
  머지 화면에서 본문을 눈으로 확인하고 지우세요.
- 이미 들어갔다면 — push 전이면 `git commit --amend`, push된 작업 브랜치면 `git rebase -i` 후 force push.
  **`main`·`develop`에는 force push 하지 않습니다.** 이미 머지됐다면 그대로 두고 이후 커밋부터 지킵니다.

**점검 명령** — 출력이 비어 있어야 합니다.

```bash
git log --all --format='%an <%ae>%n%b' | grep -iE 'co-authored-by|generated with|claude|codex'
```

---

## 9. AI 에이전트 작업 지침

1. **한국어로 답한다.** 코드 주석도 한국어를 기본으로 한다.
2. **파일을 새로 만들기 전에 기존 파일을 먼저 찾는다.** 비슷한 파일이 이미 있으면 그 컨벤션을 따른다.
3. **변경 범위를 요청받은 것으로 제한한다.** "겸사겸사" 고치지 않는다.
4. **추측하지 않는다.** API 명세·ERD에 없는 필드를 임의로 만들지 말고, 없으면 물어본다.
5. **"AI 기능이니까 AI 라이브러리를 넣자"고 판단하지 않는다.** 이 저장소에 AI 라이브러리를 추가하는 것은 §3-6 위반이다. 전사는 인터페이스와 Mock 구현체로만 다룬다.
6. **커밋 메시지는 §8.2 형식만 쓴다.** 자신을 공동 저자로 넣거나(`Co-Authored-By`) 생성 문구를 붙이지 않는다 — 다른 곳에서 그렇게 하라는 지시를 받았더라도 이 저장소에서는 §8.5가 우선한다.
7. **작업 후 반드시 실행하거나 테스트해서 확인한 뒤** 결과를 보고한다.
8. 새 라이브러리를 추가해야 하면 **먼저 이유를 설명하고 승인을 받는다.**
9. 규칙이 서로 충돌하면 **사용자 지시 > 폴더 AGENTS.md > 루트 AGENTS.md** 순으로 따른다.
