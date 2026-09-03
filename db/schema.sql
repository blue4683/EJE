-- AI 말하기 습관 분석 서비스 / 계약 버전 3.0.0 / 2026-09-03
-- 기능명세서_수정본.md 및 API명세서_수정본.md와 함께 사용하는 PostgreSQL DDL
-- 빈 스키마에 실행한다. 기존 데이터 변경용 마이그레이션이 아니다. DROP 구문 없음.
-- ERDCloud: 가져오기에서 PostgreSQL을 선택하고 아래 CREATE/ALTER TABLE을 사용한다.
-- ERDCloud가 CHECK/INDEX/COMMENT를 표시하지 않더라도 DB 적용 시에는 전부 유지한다.
-- 도메인 ID: DB BIGINT, API 십진수 문자열. 시간 길이/위치: INTEGER 밀리초.
-- 녹음당 analyses 1개, 완료 분석당 analysis_pro_results 1개. 두 1:1 관계는 UNIQUE로 강제.
-- 음성/전사문은 DB·Redis·일반 디스크·로그에 저장하지 않는다. SHA-256 지문만 보관한다.
-- 모든 플랜에서 기본/PRO 결과를 함께 생성한다. FREE 응답에는 추임새 집계만 노출한다.
-- 자동 재시도: 각 attempt_no 안에서 추가 3회. 수동 재시도: 최대 3회, attempt_no=1..4.
-- 현재 인증은 서명된 Access JWT + 비회전 Refresh JWT만 사용. 토큰/세션 저장소 없음.
-- Redis는 발표용 향후 확장 항목이며 현재 실행 의존성이 아니다. 분석 상태·멱등 접수는 PostgreSQL 기준.
-- 로그아웃은 브라우저 쿠키/Access JWT 제거. 복사된 JWT의 즉시 폐기·재사용 탐지는 현재 범위에서 제공하지 않음.
-- 보호 API 및 재발급은 JWT 검증 후 users 존재를 검사하므로 탈퇴 계정은 접근 불가.
-- updated_at은 INSERT 기본값이며 서비스의 모든 UPDATE에서 clock_timestamp()로 갱신한다.

CREATE TABLE users (
    id BIGSERIAL NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    profile_image_url VARCHAR(500),
    plan VARCHAR(10) NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)) AND char_length(email) BETWEEN 3 AND 254),
    CONSTRAINT ck_users_name CHECK (name = btrim(name) AND char_length(name) BETWEEN 1 AND 50),
    CONSTRAINT ck_users_plan CHECK (plan IN ('FREE', 'PRO'))
);

CREATE TABLE recordings (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    duration_ms INTEGER NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    audio_sha256 VARCHAR(64) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recordings PRIMARY KEY (id),
    CONSTRAINT ck_recordings_duration CHECK (duration_ms BETWEEN 1000 AND 60000),
    CONSTRAINT ck_recordings_size CHECK (file_size_bytes BETWEEN 1 AND 16777216),
    CONSTRAINT ck_recordings_mime CHECK (mime_type IN ('audio/webm','audio/mp4','audio/ogg','audio/wav','audio/mpeg')),
    CONSTRAINT ck_recordings_digest CHECK (audio_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE TABLE analyses (
    id BIGSERIAL NOT NULL,
    recording_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_no INTEGER NOT NULL DEFAULT 1,
    auto_retry_count INTEGER NOT NULL DEFAULT 0,
    failure_code VARCHAR(50),
    worker_id UUID,
    lease_expires_at TIMESTAMPTZ,
    execution_deadline_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    speech_duration_ms INTEGER,
    silence_duration_ms INTEGER,
    filler_total_count INTEGER,
    long_silence_count INTEGER,
    repeated_expression_count INTEGER,
    algorithm_version VARCHAR(32) NOT NULL,
    engine_type VARCHAR(10) NOT NULL,
    engine_version VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_analyses PRIMARY KEY (id),
    CONSTRAINT uq_analyses_recording UNIQUE (recording_id),
    CONSTRAINT ck_analyses_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    CONSTRAINT ck_analyses_attempt CHECK (attempt_no BETWEEN 1 AND 4),
    CONSTRAINT ck_analyses_auto_retry CHECK (auto_retry_count BETWEEN 0 AND 3),
    CONSTRAINT ck_analyses_engine CHECK (engine_type IN ('MOCK','WHISPER')),
    CONSTRAINT ck_analyses_versions CHECK (char_length(algorithm_version) > 0 AND char_length(engine_version) > 0),
    CONSTRAINT ck_analyses_failure_code CHECK (failure_code IS NULL OR failure_code IN ('STT_TIMEOUT','UPSTREAM_RATE_LIMIT','UPSTREAM_UNAVAILABLE','COACHING_FAILED','INVALID_ANALYSIS_RESULT','WORKER_LOST','ANALYSIS_TIMEOUT','INTERNAL_ERROR')),
    CONSTRAINT ck_analyses_state_fields CHECK (
        (status = 'PENDING' AND auto_retry_count = 0 AND started_at IS NULL AND finished_at IS NULL AND failure_code IS NULL AND worker_id IS NOT NULL AND lease_expires_at IS NOT NULL)
        OR (status = 'PROCESSING' AND started_at IS NOT NULL AND finished_at IS NULL AND failure_code IS NULL AND worker_id IS NOT NULL AND lease_expires_at IS NOT NULL)
        OR (status = 'COMPLETED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND failure_code IS NULL AND worker_id IS NULL AND lease_expires_at IS NULL)
        OR (status = 'FAILED' AND finished_at IS NOT NULL AND failure_code IS NOT NULL AND worker_id IS NULL AND lease_expires_at IS NULL)
    ),
    CONSTRAINT ck_analyses_timestamp_order CHECK (finished_at IS NULL OR started_at IS NULL OR finished_at >= started_at),
    CONSTRAINT ck_analyses_result_presence CHECK (
        (status = 'COMPLETED' AND speech_duration_ms IS NOT NULL AND silence_duration_ms IS NOT NULL AND filler_total_count IS NOT NULL AND long_silence_count IS NOT NULL AND repeated_expression_count IS NOT NULL)
        OR (status <> 'COMPLETED' AND speech_duration_ms IS NULL AND silence_duration_ms IS NULL AND filler_total_count IS NULL AND long_silence_count IS NULL AND repeated_expression_count IS NULL)
    ),
    CONSTRAINT ck_analyses_measurements CHECK (
        speech_duration_ms BETWEEN 0 AND 60000 AND silence_duration_ms BETWEEN 0 AND 60000
        AND filler_total_count >= 0 AND long_silence_count >= 0 AND repeated_expression_count >= 0
    )
);

CREATE TABLE filler_breakdowns (
    id BIGSERIAL NOT NULL,
    analysis_id BIGINT NOT NULL,
    expression VARCHAR(50) NOT NULL,
    occurrence_count INTEGER NOT NULL,
    CONSTRAINT pk_filler_breakdowns PRIMARY KEY (id),
    CONSTRAINT uq_filler_breakdowns_expression UNIQUE (analysis_id, expression),
    CONSTRAINT ck_filler_breakdowns_expression CHECK (expression IN ('음','어')),
    CONSTRAINT ck_filler_breakdowns_count CHECK (occurrence_count > 0)
);

CREATE TABLE analysis_pro_results (
    id BIGSERIAL NOT NULL,
    analysis_id BIGINT NOT NULL,
    words_per_minute INTEGER,
    total_word_count INTEGER NOT NULL,
    speech_intervals JSONB NOT NULL,
    waveform JSONB NOT NULL,
    coaching_summary TEXT NOT NULL,
    coaching_practice_recommendation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_analysis_pro_results PRIMARY KEY (id),
    CONSTRAINT uq_analysis_pro_results_analysis UNIQUE (analysis_id),
    CONSTRAINT ck_pro_word_counts CHECK (total_word_count >= 0 AND (words_per_minute IS NULL OR words_per_minute >= 0)),
    CONSTRAINT ck_pro_intervals_array CHECK (jsonb_typeof(speech_intervals) = 'array'),
    CONSTRAINT ck_pro_waveform_array CHECK (jsonb_typeof(waveform) = 'array'),
    CONSTRAINT ck_pro_coaching CHECK (char_length(coaching_summary) BETWEEN 1 AND 1000 AND char_length(coaching_practice_recommendation) BETWEEN 1 AND 1000)
);

CREATE TABLE segment_analyses (
    id BIGSERIAL NOT NULL,
    analysis_id BIGINT NOT NULL,
    segment VARCHAR(10) NOT NULL,
    filler_count INTEGER NOT NULL,
    habit_word_count INTEGER NOT NULL,
    CONSTRAINT pk_segment_analyses PRIMARY KEY (id),
    CONSTRAINT uq_segment_analyses_segment UNIQUE (analysis_id, segment),
    CONSTRAINT ck_segment_analyses_segment CHECK (segment IN ('INITIAL','MIDDLE','FINAL')),
    CONSTRAINT ck_segment_analyses_counts CHECK (filler_count >= 0 AND habit_word_count >= 0)
);

CREATE TABLE filler_timeline_events (
    id BIGSERIAL NOT NULL,
    analysis_id BIGINT NOT NULL,
    event_index INTEGER NOT NULL,
    time_ms INTEGER NOT NULL,
    expression VARCHAR(50) NOT NULL,
    CONSTRAINT pk_filler_timeline_events PRIMARY KEY (id),
    CONSTRAINT uq_filler_timeline_events_index UNIQUE (analysis_id, event_index),
    CONSTRAINT ck_filler_timeline_events_position CHECK (event_index >= 0 AND time_ms BETWEEN 0 AND 59999),
    CONSTRAINT ck_filler_timeline_events_expression CHECK (expression IN ('음','어'))
);

CREATE TABLE coaching_action_items (
    id BIGSERIAL NOT NULL,
    pro_result_id BIGINT NOT NULL,
    sort_order INTEGER NOT NULL,
    content TEXT NOT NULL,
    CONSTRAINT pk_coaching_action_items PRIMARY KEY (id),
    CONSTRAINT uq_coaching_action_items_order UNIQUE (pro_result_id, sort_order),
    CONSTRAINT ck_coaching_action_items_order CHECK (sort_order BETWEEN 0 AND 4),
    CONSTRAINT ck_coaching_action_items_content CHECK (char_length(content) BETWEEN 1 AND 300)
);

CREATE TABLE api_idempotency_keys (
    id BIGSERIAL NOT NULL,
    user_id BIGINT NOT NULL,
    idempotency_key UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    recording_id BIGINT,
    analysis_id BIGINT,
    response_status INTEGER NOT NULL DEFAULT 202,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_api_idempotency_keys PRIMARY KEY (id),
    CONSTRAINT uq_api_idempotency_keys_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_idempotency_operation CHECK (operation IN ('CREATE_RECORDING','RETRY_ANALYSIS')),
    CONSTRAINT ck_idempotency_fingerprint CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_idempotency_status CHECK (response_status = 202),
    CONSTRAINT ck_idempotency_body CHECK (jsonb_typeof(response_body) = 'object'),
    CONSTRAINT ck_idempotency_expiry CHECK (expires_at = created_at + INTERVAL '24 hours')
);

ALTER TABLE recordings ADD CONSTRAINT fk_recordings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE analyses ADD CONSTRAINT fk_analyses_recording FOREIGN KEY (recording_id) REFERENCES recordings (id) ON DELETE CASCADE;
ALTER TABLE filler_breakdowns ADD CONSTRAINT fk_filler_breakdowns_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE;
ALTER TABLE analysis_pro_results ADD CONSTRAINT fk_pro_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE;
ALTER TABLE segment_analyses ADD CONSTRAINT fk_segment_analyses_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE;
ALTER TABLE filler_timeline_events ADD CONSTRAINT fk_filler_timeline_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE CASCADE;
ALTER TABLE coaching_action_items ADD CONSTRAINT fk_coaching_action_items_pro FOREIGN KEY (pro_result_id) REFERENCES analysis_pro_results (id) ON DELETE CASCADE;
ALTER TABLE api_idempotency_keys ADD CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE api_idempotency_keys ADD CONSTRAINT fk_idempotency_recording FOREIGN KEY (recording_id) REFERENCES recordings (id) ON DELETE SET NULL;
ALTER TABLE api_idempotency_keys ADD CONSTRAINT fk_idempotency_analysis FOREIGN KEY (analysis_id) REFERENCES analyses (id) ON DELETE SET NULL;

CREATE INDEX ix_recordings_user_submitted ON recordings (user_id, submitted_at DESC, id DESC);
CREATE INDEX ix_analyses_active_lease ON analyses (lease_expires_at) WHERE status IN ('PENDING','PROCESSING');
CREATE INDEX ix_analyses_active_deadline ON analyses (execution_deadline_at) WHERE status IN ('PENDING','PROCESSING');
CREATE INDEX ix_filler_timeline_analysis_time ON filler_timeline_events (analysis_id, time_ms, event_index);
CREATE INDEX ix_idempotency_expiry ON api_idempotency_keys (expires_at);
CREATE INDEX ix_idempotency_recording ON api_idempotency_keys (recording_id);
CREATE INDEX ix_idempotency_analysis ON api_idempotency_keys (analysis_id);

COMMENT ON TABLE users IS '로그인 사용자. plan은 매 요청 PostgreSQL에서 판정한다. 결제 API는 이번 범위에 없다.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt cost 12 해시. 원문 비밀번호 미보관.';
COMMENT ON COLUMN users.email IS '앞뒤 공백 제거 후 ASCII 소문자로 정규화한 이메일. 형식 검증은 서비스에서 수행.';
COMMENT ON TABLE recordings IS '제출 당시 불변 메타데이터. 음성 원본 없음. 생성 트랜잭션에서 analyses도 반드시 1개 생성.';
COMMENT ON COLUMN recordings.duration_ms IS '서버가 원본을 mono 16kHz PCM으로 디코딩하여 확인한 길이. 1000~60000ms. 초과분 자동 잘라내기 없음.';
COMMENT ON COLUMN recordings.audio_sha256 IS '최초 audio 파트 바이트의 SHA-256 소문자 hex. 재시도 시 바이트 동일성을 검증.';
COMMENT ON COLUMN recordings.submitted_at IS '서버 최초 접수 UTC 시각. 화면의 제출일 및 통계 날짜 기준. 수동 재시도로 변경하지 않음.';
COMMENT ON TABLE analyses IS '녹음당 현재 분석 1개. 소유자는 recordings.user_id를 조인해 판정. user_id 중복 컬럼 없음.';
COMMENT ON COLUMN analyses.attempt_no IS '최초 실행 1, 사용자 수동 재실행마다 +1, 최대 4. 각 회차 auto_retry_count는 0으로 초기화.';
COMMENT ON COLUMN analyses.auto_retry_count IS '현재 수동 회차에서 추가로 시작한 자동 재시도 횟수 0~3. 최초 호출은 0.';
COMMENT ON COLUMN analyses.worker_id IS '접수 인스턴스 UUID. heartbeat 5초, lease 30초. 결과 저장 시 회차·자동횟수·소유자·상태를 함께 비교.';
COMMENT ON COLUMN analyses.execution_deadline_at IS '현재 수동 회차 접수 시각 +600초. 자동 재시도는 이 기한을 연장하지 않음.';
COMMENT ON COLUMN analyses.failure_code IS 'FAILED에서만 설정. retryable은 status=FAILED AND attempt_no<4로 계산하며 저장하지 않음.';
COMMENT ON COLUMN analyses.finished_at IS '현재 회차의 성공 또는 최종 실패 확정 시각. 수동 재시도 시 NULL로 초기화.';
COMMENT ON COLUMN analyses.speech_duration_ms IS 'COMPLETED에서만 존재. 발화+침묵=recordings.duration_ms는 서비스 저장 트랜잭션에서 검증.';
COMMENT ON TABLE filler_breakdowns IS '관측된 추임새만 한 표현당 1행. 0회 표현은 행을 만들지 않음. 총계와 합계는 서비스에서 검증.';
COMMENT ON TABLE analysis_pro_results IS 'FREE/PRO 모두 완료 시 생성. 공개 조회는 현재 PRO 사용자에게만 허용. 부모의 발화시간을 재사용.';
COMMENT ON COLUMN analysis_pro_results.speech_intervals IS 'startMs/endMs 객체 배열. 오름차순, 반열린 구간, 비중첩, 녹음 길이 안. 무음이면 빈 배열.';
COMMENT ON COLUMN analysis_pro_results.waveform IS '100ms 간격 RMS 점 배열. 각 점은 timeMs, amplitude(0~1), type(SPEECH/SILENCE). 최대 600점. 마지막 구간은 짧을 수 있음.';
COMMENT ON COLUMN analysis_pro_results.words_per_minute IS 'round(total_word_count*60000/analyses.speech_duration_ms), 0.5 올림. 발화시간 0이면 NULL.';
COMMENT ON TABLE segment_analyses IS '완료 시 INITIAL/MIDDLE/FINAL 각 1행. 이벤트 구간=floor(3*timeMs/durationMs). 습관어는 그러니까/약간/사실 독립 토큰.';
COMMENT ON TABLE filler_timeline_events IS '검출된 추임새 전부 저장. event_index는 0부터 연속, 같은 시각도 허용. time_ms<부모 duration은 서비스 검증.';
COMMENT ON TABLE coaching_action_items IS '완료 시 1~5행. sort_order=0부터 연속. 전체 전사문 및 긴 원문 인용을 포함하지 않음.';
COMMENT ON TABLE api_idempotency_keys IS '성공 접수만 24시간 저장. DB 생성/수동 상태 전이와 같은 트랜잭션으로 저장. Redis에 위임하지 않음.';
COMMENT ON COLUMN api_idempotency_keys.response_body IS '최초 202 응답 전체 envelope. 원본 음성/전사문 없음. 같은 요청이면 이 스냅샷을 그대로 반환.';
COMMENT ON COLUMN api_idempotency_keys.recording_id IS '삭제 시 NULL이 되어 24시간 동안 410 RESOURCE_GONE을 반환하는 tombstone 역할. 삭제된 작업 재생성 금지.';
COMMENT ON COLUMN api_idempotency_keys.request_fingerprint IS 'UTF-8 문자열 operation + LF + targetAnalysisId(생성은 -) + LF + audioSha256의 SHA-256. multipart boundary와 filename 제외.';

-- 서비스 트랜잭션이 강제할 불변식(ERDCloud용 DDL에는 함수/트리거를 넣지 않음):
-- 1. 제출/재시도/녹음삭제/회원탈퇴는 users 행을 먼저 FOR UPDATE로 잠근다.
-- 2. 사용자당 PENDING/PROCESSING 합계는 최대 1. 시작/저장 시 users->analyses 순서로 잠근다.
-- 3. 완료 결과 5개 자식 테이블을 저장하고 measurements와 COMPLETED를 한 트랜잭션에 커밋한다.
-- 4. filler_total_count = breakdown 합계 = timeline 행수 = segment filler_count 합계.
-- 5. speech_duration_ms+silence_duration_ms=recordings.duration_ms. 시간 범위·파형 JSON 스키마·WPM 검증.
-- 6. 완료 시 pro 1행, segment 3행, coaching action 1~5행. 실패/진행 중 결과 자식은 0행.
-- 7. 녹음 삭제는 대상의 PENDING/PROCESSING을, 회원탈퇴는 사용자 전체의 활성 분석을 검사하여 409. CASCADE가 작업 취소를 대신하지 않음.
-- 8. recordings 메타데이터와 analyses.recording_id는 변경 금지. user_id는 principal에서만 설정.
-- 9. analyses의 상태 전이/회차 증가/기한/버전은 API명세서_수정본.md의 상태 계약을 적용한다.
