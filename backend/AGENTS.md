# backend/AGENTS.md

Java + Spring Boot 백엔드 규칙입니다. 루트 `AGENTS.md`를 먼저 읽고 이 파일을 적용하세요.
**백엔드는 이 프로젝트 하나뿐입니다.** 별도의 AI 서비스(FastAPI 등)를 만들지 마세요.

---

## 1. 버전 고정

| 항목 | 버전 | 비고 |
| --- | --- | --- |
| Java | **21** | 가상 스레드 사용 |
| Spring Boot | **4.1.x** | |
| Spring AI | **2.0.1** | Boot 4.1과 정합 |
| DB | PostgreSQL (Supabase / Neon) | |
| 빌드 | Gradle (Groovy DSL) | |

> **이 조합은 의도적으로 고른 것입니다.** Spring AI 2.0 스타터는 실제로 Spring Boot **4.1.0** 수준의
> 의존성을 요구해서, Boot 4.0.x와 함께 쓰면 버전 충돌이 납니다. Boot 4.1.x에서는 문제가 없습니다.
> **버전을 내리거나 올리자는 제안을 하지 마세요.** 물어보면 이 표를 근거로 거절합니다.

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.ai:spring-ai-starter-model-openai'
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

dependencyManagement {
    imports { mavenBom "org.springframework.ai:spring-ai-bom:2.0.1" }
}
```

### 1.1 Spring Boot 4에서 달라진 것 — 반드시 알고 있을 것

Boot 3 예제를 그대로 복사하면 깨지는 지점들입니다. 인터넷 예제는 대부분 Boot 3 기준입니다.

| 항목 | 주의 |
| --- | --- |
| **Jackson** | Boot 4는 **Jackson 3**을 씁니다. 패키지가 `com.fasterxml.jackson.*` → **`tools.jackson.*`** 으로 바뀌었습니다. `ObjectMapper` 대신 `JsonMapper`를 쓰고, `com.fasterxml`을 import 하면 안 됩니다. |
| **Java** | 21이 최소가 아니라 **우리 팀이 고정한 값**입니다. `record`, 패턴 매칭, 가상 스레드를 자유롭게 씁니다. |
| **가상 스레드** | `spring.threads.virtual.enabled: true`로 켭니다 (§4 참조). |
| **javax / jakarta** | 여전히 `jakarta.*` 입니다. `javax.*`를 import 하지 마세요. |

---

## 2. 패키지 구조

```
com.skala.miniproject
├── common
│   ├── exception/     GlobalExceptionHandler, BusinessException, ErrorCode
│   └── dto/           ErrorResponse
├── config/            CorsConfig, StorageProperties, AsyncConfig
├── transcription                        ← 도메인 단위로 묶는다
│   ├── controller/TranscriptionController.java
│   ├── service/TranscriptionService.java
│   ├── repository/TranscriptionRepository.java
│   ├── entity/Transcription.java
│   ├── dto/TranscriptionJobResponse.java, TranscriptionDetailResponse.java
│   └── client/
│       ├── TranscriptionClient.java          인터페이스
│       ├── TranscriptionResult.java          결과 record
│       ├── MockTranscriptionClient.java      기본 구현 (지금)
│       └── WhisperTranscriptionClient.java   Spring AI 구현 (확장 지점)
└── storage/AudioFileStorage.java       업로드 파일 저장·경로 관리
```

**계층별 책임을 넘지 않습니다.**

- `Controller` — 요청 받기, 검증, DTO 변환, 응답. **비즈니스 로직 금지.**
- `Service` — 로직과 트랜잭션. `@Transactional`은 여기에만.
- `Repository` — Spring Data JPA 인터페이스. 쿼리만.
- `client` — 외부 시스템 호출. **여기 밖에서는 Spring AI 클래스를 import 하지 않는다.**

---

## 3. Spring AI + Whisper — 가장 중요한 절

### 3.1 기본 상태는 "의존성은 있고 기능은 꺼짐"

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true              # Java 21 가상 스레드
  ai:
    model:
      audio:
        transcription: none      # 기본: 자동 구성 비활성 → API Key 없이 기동됨
    openai:
      api-key: ${OPENAI_API_KEY:}
      audio:
        transcription:
          model: ${TRANSCRIPTION_MODEL:whisper-1}
          language: ${TRANSCRIPTION_LANGUAGE:ko}
          response-format: json
          temperature: 0
```

```yaml
# application-real-ai.yml — 확장 시연용 프로필
spring:
  ai:
    model:
      audio:
        transcription: openai
```

> **프로퍼티 경로 주의** — Spring AI 2.0부터 `spring.ai.openai.audio.transcription.**options**.*` 형태는
> **deprecated**이고 `spring.ai.openai.audio.transcription.*` 가 정식입니다.
> 인터넷 예제에 `options.` 가 붙어 있으면 그건 1.x 문서입니다.

**`OPENAI_API_KEY` 없이 `./gradlew bootRun` 이 성공해야 합니다.** 이 조건을 깨는 변경은 금지입니다.

### 3.2 인터페이스와 두 구현체

```java
// client/TranscriptionClient.java
public interface TranscriptionClient {
    /** 오디오 파일을 텍스트로 전사한다. */
    TranscriptionResult transcribe(Path audioFile, String languageCode);
}

// client/TranscriptionResult.java
public record TranscriptionResult(String text, String model) {}
```

```java
// client/MockTranscriptionClient.java  — 기본 구현
@Slf4j
@Component
@Profile("!real-ai")
public class MockTranscriptionClient implements TranscriptionClient {

    @Override
    public TranscriptionResult transcribe(Path audioFile, String languageCode) {
        log.info("Mock 전사 수행: {}", audioFile.getFileName());
        sleepQuietly(2_000);   // 실제 지연을 흉내 내어 pending 상태를 검증 가능하게 한다
        return new TranscriptionResult(
                "안녕하세요. 이것은 Mock 전사 결과입니다. 실제 Whisper 연동 시 이 자리에 전사 텍스트가 들어갑니다.",
                "mock");
    }
}
```

```java
// client/WhisperTranscriptionClient.java  — 확장 지점. 이 파일이 발표의 핵심이다.
import com.openai.models.audio.AudioResponseFormat;                       // ← Spring AI 2.0
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.audio.transcription.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.transcription.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.FileSystemResource;

@Component
@Profile("real-ai")
@RequiredArgsConstructor
public class WhisperTranscriptionClient implements TranscriptionClient {

    private final OpenAiAudioTranscriptionModel transcriptionModel;

    @Override
    public TranscriptionResult transcribe(Path audioFile, String languageCode) {
        var options = OpenAiAudioTranscriptionOptions.builder()
                .language(languageCode)
                .temperature(0f)
                .responseFormat(AudioResponseFormat.JSON)
                .build();

        var prompt = new AudioTranscriptionPrompt(new FileSystemResource(audioFile), options);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);

        // getOutput() 은 전사된 텍스트(String)를 그대로 돌려준다
        return new TranscriptionResult(response.getResult().getOutput(), "whisper-1");
    }
}
```

### 3.3 Spring AI 2.0 시그니처 — 기억에 의존하지 말고 이 표를 쓸 것

| 용도 | 클래스 / 메서드 |
| --- | --- |
| 모델 빈 | `org.springframework.ai.openai.audio.transcription.OpenAiAudioTranscriptionModel` |
| 옵션 빌더 | `OpenAiAudioTranscriptionOptions.builder()` — `.language()` `.temperature()` `.responseFormat()` `.prompt()` |
| 응답 포맷 | **`com.openai.models.audio.AudioResponseFormat`** (`JSON`, `TEXT`, `SRT`, `VTT`, `VERBOSE_JSON`) |
| 요청 | `new AudioTranscriptionPrompt(Resource, options)` |
| 응답 | `AudioTranscriptionResponse` → `.getResult().getOutput()` → **`String`** |
| 활성/비활성 | `spring.ai.model.audio.transcription` = `openai` \| `none` |
| 설정 접두어 | `spring.ai.openai.audio.transcription.*` |

> ⚠️ **1.x 예제와 헷갈리지 말 것.** Spring AI 1.x는 `OpenAiAudioApi.TranscriptResponseFormat`을 썼지만,
> 2.0부터 공식 OpenAI Java SDK의 **`com.openai.models.audio.AudioResponseFormat`** 으로 대체되었습니다.
> 블로그 예제 대부분이 1.x 기준이므로, 컴파일 에러가 나면 먼저 이 표와 대조하세요.

### 3.4 서비스는 인터페이스만 안다

```java
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final TranscriptionClient transcriptionClient;   // ✅ 인터페이스
    // private final OpenAiAudioTranscriptionModel model;    // ❌ 절대 금지
}
```

이 규칙 하나가 "구현체만 갈아끼우면 됩니다"라는 발표 주장의 전부입니다.

---

## 4. 비동기 전사 흐름

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<TranscriptionJobResponse> create(@RequestParam("file") MultipartFile file) {
    String jobId = transcriptionService.accept(file);      // 파일 저장 + status=pending 행 생성
    return ResponseEntity.accepted()                       // 202
            .body(new TranscriptionJobResponse(jobId, "pending", null));
}

@GetMapping("/{jobId}")
public ResponseEntity<TranscriptionDetailResponse> get(@PathVariable String jobId) {
    return ResponseEntity.ok(transcriptionService.getJob(jobId));
}
```

- 실제 전사는 `@Async` 메서드에서 수행하고, 끝나면 행을 `completed` 또는 `failed`로 갱신합니다
- **가상 스레드를 켰으므로 별도 스레드풀 튜닝이 필요 없습니다.** Whisper 호출이 블로킹이어도 플랫폼 스레드를 붙잡지 않습니다. 발표에서 "큐 없이도 버티는 이유"로 설명할 수 있는 지점입니다
- `pending` 상태를 실제로 관측할 수 있어야 합니다. 즉시 `completed`로 만들지 마세요 — 프론트의 폴링 UI가 검증되지 않습니다
- 예외가 나면 `status=failed`, `error_message`에 사용자용 한국어 문장을 저장합니다

### 업로드 제약

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
app:
  storage:
    location: ${UPLOAD_DIR:./uploads}
    allowed-content-types: audio/mpeg,audio/mp4,audio/wav,audio/webm,audio/x-m4a
```

- 허용 확장자: `mp3`, `m4a`, `wav`, `webm`
- 저장 파일명은 **UUID로 새로 만든다.** 사용자가 올린 이름을 그대로 경로에 쓰지 않는다 (경로 조작 방지)
- 원본 파일명은 DB `original_filename` 컬럼에만 보관
- `backend/uploads/` 는 `.gitignore`에 넣는다

---

## 5. DTO·엔티티·에러

### 5.1 Entity를 그대로 응답하지 않는다

```java
// ❌ 금지
@GetMapping("/{jobId}")
public Transcription get(@PathVariable String jobId) { ... }

// ✅ 항상 응답 DTO로 변환
public record TranscriptionDetailResponse(
        String jobId, String status, String originalFilename,
        String text, String model, String errorMessage, Instant createdAt
) {
    public static TranscriptionDetailResponse from(Transcription t) { ... }
}
```

DTO는 `record`로 만들고 정적 팩터리 `from(Entity)`를 둡니다.

### 5.2 에러 처리는 한 곳에서

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handle(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("FILE_TOO_LARGE",
                        "파일이 너무 큽니다. 10MB 이하의 음성 파일을 올려 주세요.", null));
    }
}
```

컨트롤러·서비스에서 `try-catch`로 에러 응답을 직접 만들지 않습니다.

---

## 6. 설정과 키

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate.ddl-auto: update      # 3일 프로젝트 한정. 운영이라면 validate
```

- `application.yml`에 **값을 직접 적지 않는다.** 전부 환경변수 참조
- `@Value`를 서비스 곳곳에 흩뿌리지 말고 `@ConfigurationProperties` 레코드 하나로 모은다

```java
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String location, List<String> allowedContentTypes) {}
```

---

## 7. 코드 컨벤션

- 들여쓰기 4칸, 한 줄 120자
- **DTO·값 객체는 `record`로 만든다.** Java 21이므로 클래스 + getter 보일러플레이트를 쓰지 않는다
- Lombok은 Entity와 서비스에서 `@Getter`, `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@Slf4j`만. **`@Data`·`@Setter`는 Entity에 금지**
- 의존성 주입은 **생성자 주입만** (`@RequiredArgsConstructor` + `private final`). `@Autowired` 필드 주입 금지
- 클래스 PascalCase / 메서드·변수 camelCase / 상수 UPPER_SNAKE
- 메서드 이름: `getX`(단건, 없으면 예외) · `findX`(Optional) · `createX` · `updateX` · `deleteX`
- `System.out.println` 금지 → `@Slf4j` + `log.info`
- 주석은 한국어. "무엇을"이 아니라 **"왜"** 를 적는다

---

## 8. CORS

개발 중에는 `http://localhost:5173`만 허용합니다. `*`로 열지 마세요 — 자격증명 요청에서 막힙니다.

---

## 9. 하지 말 것

- ❌ `OpenAiAudioTranscriptionModel`을 서비스·컨트롤러에 직접 주입
- ❌ 기본 실행에 `OPENAI_API_KEY`를 필수로 만들기
- ❌ Spring Boot·Spring AI 버전을 §1 표와 다르게 바꾸기
- ❌ `com.fasterxml.jackson.*` import (Boot 4는 Jackson 3 = `tools.jackson.*`)
- ❌ `javax.*` import (`jakarta.*` 를 쓴다)
- ❌ 1.x 예제의 `OpenAiAudioApi.TranscriptResponseFormat` 사용
- ❌ Entity를 API 응답으로 그대로 반환
- ❌ 컨트롤러에 비즈니스 로직 작성
- ❌ API Key·DB 비밀번호를 `application.yml`에 직접 기입
- ❌ 업로드 원본 파일명을 저장 경로에 그대로 사용
- ❌ `uploads/` 폴더를 커밋
- ❌ `ddl-auto: create` 또는 운영 DB에 대한 파괴적 작업
- ❌ 명세에 없는 응답 필드를 임의 추가

---

## 10. 커밋 예시

```
chore(be): Spring Boot 4.1 + Java 21 프로젝트 초기 생성
chore(ai): Spring AI 2.0.1 OpenAI 스타터 및 BOM 추가

의존성은 1일차에 넣되 spring.ai.model.audio.transcription=none 으로
비활성화해 API Key 없이도 기동되도록 했다.

feat(ai): TranscriptionClient 인터페이스와 Mock 구현체 추가
feat(ai): 전사 요청 API 구현 (202 + jobId, 가상 스레드 기반 비동기 처리)
feat(be): 오디오 업로드 검증 및 파일 저장 로직 구현
feat(be): GlobalExceptionHandler 및 공통 에러 응답 포맷 추가
refactor(ai): 서비스가 구현체 대신 TranscriptionClient만 참조하도록 변경
fix(be): 10MB 초과 업로드 시 500이 반환되던 문제 수정
fix(be): Jackson 3 전환에 따라 tools.jackson 패키지로 import 정정
docs(api): OpenAPI 명세에 전사 엔드포인트 추가
```
