# Frontend

Vue 3와 Vite 기반의 프론트엔드입니다.

## 실행 모드

환경 파일을 최초 한 번 준비합니다.

```bash
cp .env.mock.example .env.mock.local
cp .env.real.example .env.real.local
```

각 파일의 `VITE_API_BASE_URL`을 다음처럼 설정합니다.

```dotenv
# .env.mock.local
VITE_API_BASE_URL=http://127.0.0.1:18080/api/v1
VITE_WIREFRAME_PREVIEW=true

# .env.real.local
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WIREFRAME_PREVIEW=false
```

Mock 모드는 터미널 두 개에서 실행합니다.

```bash
npm install
npm run mock:api
npm run dev:mock
```

실제 백엔드 모드는 Spring Boot를 먼저 실행한 뒤 프론트엔드를 실행합니다.

```bash
npm install
npm run dev:real
```

기본 개발 서버 주소는 `http://localhost:5173`입니다.

- `dev:mock`: 별도 Node Mock API와 통신
- `dev:real`: Spring Boot JWT API와 통신
- 두 모드 모두 동일한 `src/api/*`와 axios 인터셉터를 사용합니다.
- 실제 모드의 Access JWT는 Pinia 메모리에만 보관하고, Refresh JWT는 백엔드의 HttpOnly 쿠키를 사용합니다.
- Vite 프록시는 사용하지 않습니다. 실제 백엔드의 `APP_ORIGIN`은 `http://localhost:5173`이어야 합니다.
