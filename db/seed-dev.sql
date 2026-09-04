-- 개발용 시드 데이터. schema.sql 다음에 실행된다 (docker-entrypoint-initdb.d 마운트 순서로 보장).
-- API명세서.md의 성공 응답 예제(레코딩 101 / 분석 5001)를 한 글자씩 대조할 수 있도록 값을 그대로 사용한다.
-- 비밀번호는 두 계정 모두 P@ssw0rd123 (BCrypt cost 12).

INSERT INTO users (id, email, password_hash, name, profile_image_url, plan, created_at, updated_at) VALUES
    (1, 'user@example.com', '$2a$12$ZgH8t9.skqbuAYP1/ADm6.a3A3gUOdCWDkk9Zq2xt/Ju6KJOc6YHa', '사용자', NULL, 'FREE', '2026-08-27 09:00:00+00', '2026-08-27 09:00:00+00'),
    (2, 'pro@example.com', '$2a$12$ZgH8t9.skqbuAYP1/ADm6.a3A3gUOdCWDkk9Zq2xt/Ju6KJOc6YHa', '프로사용자', NULL, 'PRO', '2026-08-27 09:00:00+00', '2026-08-27 09:00:00+00');

-- recordings: 88(완료, 08-27) · 101(완료, 09-03, 명세 예제와 동일) · 102(PENDING) · 103(FAILED)
INSERT INTO recordings (id, user_id, duration_ms, mime_type, file_size_bytes, audio_sha256, submitted_at) VALUES
    (88,  1, 3000, 'audio/webm', 24000, 'cd433fa901da054f0956e18b5f15998e4961ad10d08268936d54eafec5a85688', '2026-08-27 09:15:00+00'),
    (101, 1, 3000, 'audio/webm', 24000, '12d738daff7e7533d13d1e8a5e09a1f716a14e15a2cacf46441441770855bd12', '2026-09-03 03:00:00+00'),
    (102, 1, 3000, 'audio/webm', 24000, '9feb80a3633447a2da24598dbe9bd0e9e4ecfbf1c452f0c9307e4fbaee726d6c', '2026-09-03 03:05:00+00'),
    (103, 1, 3000, 'audio/webm', 24000, 'df175811d1a0ad1bd890c5009af63f00528948f8cfe5b442d8260c77caa40a02', '2026-09-03 03:06:00+00');

-- analyses: 5000(88, COMPLETED) · 5001(101, COMPLETED, 명세 API17 예제와 동일) · 5002(102, PENDING) · 5003(103, FAILED)
INSERT INTO analyses (
    id, recording_id, status, attempt_no, auto_retry_count, failure_code,
    worker_id, lease_expires_at, execution_deadline_at, started_at, finished_at,
    speech_duration_ms, silence_duration_ms, filler_total_count, long_silence_count, repeated_expression_count,
    algorithm_version, engine_type, engine_version, created_at, updated_at
) VALUES
    (5000, 88, 'COMPLETED', 1, 0, NULL,
     NULL, NULL, '2026-08-27 09:25:00+00', '2026-08-27 09:15:01+00', '2026-08-27 09:15:05+00',
     1500, 1500, 4, 0, 0,
     'speech-habits-v1', 'MOCK', 'mock-pipeline-v1', '2026-08-27 09:15:00+00', '2026-08-27 09:15:05+00'),

    (5001, 101, 'COMPLETED', 1, 0, NULL,
     NULL, NULL, '2026-09-03 03:10:00+00', '2026-09-03 03:00:01+00', '2026-09-03 03:00:05+00',
     2000, 1000, 2, 0, 1,
     'speech-habits-v1', 'MOCK', 'mock-pipeline-v1', '2026-09-03 03:00:00+00', '2026-09-03 03:00:05+00'),

    (5002, 102, 'PENDING', 1, 0, NULL,
     gen_random_uuid(), '2026-09-03 03:05:30+00', '2026-09-03 03:15:00+00', NULL, NULL,
     NULL, NULL, NULL, NULL, NULL,
     'speech-habits-v1', 'MOCK', 'mock-pipeline-v1', '2026-09-03 03:05:00+00', '2026-09-03 03:05:00+00'),

    (5003, 103, 'FAILED', 1, 3, 'STT_TIMEOUT',
     NULL, NULL, '2026-09-03 03:16:00+00', '2026-09-03 03:06:01+00', '2026-09-03 03:07:30+00',
     NULL, NULL, NULL, NULL, NULL,
     'speech-habits-v1', 'MOCK', 'mock-pipeline-v1', '2026-09-03 03:06:00+00', '2026-09-03 03:07:30+00');

-- filler_breakdowns
INSERT INTO filler_breakdowns (analysis_id, expression, occurrence_count) VALUES
    (5000, '어', 3),
    (5000, '음', 1),
    (5001, '음', 1),
    (5001, '어', 1);

-- analysis_pro_results — 5001은 API명세서.md API 17 성공 응답 예제의 metrics와 동일하다.
INSERT INTO analysis_pro_results (analysis_id, words_per_minute, total_word_count, speech_intervals, waveform, coaching_summary, coaching_practice_recommendation) VALUES
    (5000, 120, 3,
     '[{"startMs":1500,"endMs":3000}]'::jsonb,
     '[{"timeMs":0,"amplitude":0.02,"type":"SILENCE"},{"timeMs":100,"amplitude":0.02,"type":"SILENCE"},{"timeMs":200,"amplitude":0.02,"type":"SILENCE"},{"timeMs":300,"amplitude":0.02,"type":"SILENCE"},{"timeMs":400,"amplitude":0.02,"type":"SILENCE"},{"timeMs":500,"amplitude":0.02,"type":"SILENCE"},{"timeMs":600,"amplitude":0.02,"type":"SILENCE"},{"timeMs":700,"amplitude":0.02,"type":"SILENCE"},{"timeMs":800,"amplitude":0.02,"type":"SILENCE"},{"timeMs":900,"amplitude":0.02,"type":"SILENCE"},{"timeMs":1000,"amplitude":0.02,"type":"SILENCE"},{"timeMs":1100,"amplitude":0.02,"type":"SILENCE"},{"timeMs":1200,"amplitude":0.02,"type":"SILENCE"},{"timeMs":1300,"amplitude":0.02,"type":"SILENCE"},{"timeMs":1400,"amplitude":0.02,"type":"SILENCE"},{"timeMs":1500,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1600,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1700,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1800,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1900,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2000,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2100,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2200,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2300,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2400,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2500,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2600,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2700,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2800,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2900,"amplitude":0.65,"type":"SPEECH"}]'::jsonb,
     '후반부에 추임새가 집중되어 있습니다.',
     '문장을 마치기 전에 한 박자 쉬는 연습을 해 보세요.'),

    (5001, 120, 4,
     '[{"startMs":500,"endMs":2500}]'::jsonb,
     '[{"timeMs":0,"amplitude":0.02,"type":"SILENCE"},{"timeMs":100,"amplitude":0.02,"type":"SILENCE"},{"timeMs":200,"amplitude":0.02,"type":"SILENCE"},{"timeMs":300,"amplitude":0.02,"type":"SILENCE"},{"timeMs":400,"amplitude":0.02,"type":"SILENCE"},{"timeMs":500,"amplitude":0.65,"type":"SPEECH"},{"timeMs":600,"amplitude":0.65,"type":"SPEECH"},{"timeMs":700,"amplitude":0.65,"type":"SPEECH"},{"timeMs":800,"amplitude":0.65,"type":"SPEECH"},{"timeMs":900,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1000,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1100,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1200,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1300,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1400,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1500,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1600,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1700,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1800,"amplitude":0.65,"type":"SPEECH"},{"timeMs":1900,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2000,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2100,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2200,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2300,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2400,"amplitude":0.65,"type":"SPEECH"},{"timeMs":2500,"amplitude":0.02,"type":"SILENCE"},{"timeMs":2600,"amplitude":0.02,"type":"SILENCE"},{"timeMs":2700,"amplitude":0.02,"type":"SILENCE"},{"timeMs":2800,"amplitude":0.02,"type":"SILENCE"},{"timeMs":2900,"amplitude":0.02,"type":"SILENCE"}]'::jsonb,
     '초반과 후반에 추임새가 관측되었습니다.',
     '같은 자기소개를 다시 녹음하고 추임새 변화를 확인하세요.');

-- segment_analyses (완료 시 항상 3행: INITIAL/MIDDLE/FINAL)
INSERT INTO segment_analyses (analysis_id, segment, filler_count, habit_word_count) VALUES
    (5000, 'INITIAL', 2, 0),
    (5000, 'MIDDLE', 1, 1),
    (5000, 'FINAL', 1, 0),
    (5001, 'INITIAL', 1, 0),
    (5001, 'MIDDLE', 0, 1),
    (5001, 'FINAL', 1, 0);

-- filler_timeline_events (검출된 추임새 전부, event_index 0부터 연속)
INSERT INTO filler_timeline_events (analysis_id, event_index, time_ms, expression) VALUES
    (5000, 0, 200, '어'),
    (5000, 1, 900, '어'),
    (5000, 2, 1600, '어'),
    (5000, 3, 2600, '음'),
    (5001, 0, 700, '음'),
    (5001, 1, 2100, '어');

-- coaching_action_items (완료 시 1~5행, sort_order 0부터 연속)
INSERT INTO coaching_action_items (pro_result_id, sort_order, content)
SELECT id, 0, '말하기 전 숨을 한 번 고르고 시작해 보세요.' FROM analysis_pro_results WHERE analysis_id = 5000
UNION ALL
SELECT id, 1, '문장 끝을 흐리지 않고 또렷하게 마무리해 보세요.' FROM analysis_pro_results WHERE analysis_id = 5000
UNION ALL
SELECT id, 0, '문장 시작 전 짧게 멈춘 뒤 말해 보세요.' FROM analysis_pro_results WHERE analysis_id = 5001;

-- 명시적으로 지정한 ID 뒤로 시퀀스를 맞춘다. 이후 애플리케이션 INSERT가 이어서 채번한다.
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('recordings_id_seq', (SELECT MAX(id) FROM recordings));
SELECT setval('analyses_id_seq', (SELECT MAX(id) FROM analyses));
