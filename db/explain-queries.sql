-- SpeechCoach PostgreSQL 실행계획 점검용 SQL
--
-- 사용 방법
-- 1. 실제 운영과 비슷한 데이터가 들어 있는 별도 성능 테스트 DB에서 실행한다.
-- 2. 데이터 대량 적재 직후라면 먼저 ANALYZE; 를 실행한다.
-- 3. 아래 EXPLAIN 블록을 한 번에 하나씩 실행한다.
-- 4. 출력된 JSON 배열 전체를 https://explain.dalibo.com 에 붙여 넣는다.
--
-- 주의
-- - ANALYZE 옵션은 쿼리를 실제 실행한다. 아래 파일은 SELECT만 포함한다.
-- - 현재 seed-dev.sql 기준 식별자는 user=1, recording=101, analysis=5001이다.
-- - 작은 시드 데이터에서는 Seq Scan이 Index Scan보다 정상적으로 저렴할 수 있다.
-- - N+1은 여러 SQL의 실행 횟수 문제이므로 이 파일만으로 판정하지 않는다.
--
-- 권장 psql 출력 설정(JSON만 복사하기 쉬운 형태)
-- \pset format unaligned
-- \pset tuples_only on


-- =============================================================================
-- PLAN 01. 사용자별 기록 목록
-- 원본: RecordingQueryRepository.findOwnedPage
-- 기대 인덱스:
--   ix_recordings_user_submitted (user_id, submitted_at DESC, id DESC)
--   uq_analyses_recording (recording_id)
-- 확인:
--   별도 Sort 없이 LIMIT까지 도달하는지, Rows Removed by Filter가 과도하지 않은지
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT r.id AS "recordingId",
       r.submitted_at AS "submittedAt",
       r.duration_ms AS "durationMs",
       r.mime_type AS "mimeType",
       r.file_size_bytes AS "fileSizeBytes",
       a.id AS "analysisId",
       a.status AS "status",
       a.attempt_no AS "attemptNo",
       a.auto_retry_count AS "autoRetryCount",
       a.failure_code AS "failureCode",
       a.started_at AS "startedAt",
       a.finished_at AS "finishedAt",
       a.filler_total_count AS "fillerTotalCount",
       a.algorithm_version AS "algorithmVersion",
       a.engine_type AS "engineType",
       a.engine_version AS "engineVersion"
FROM recordings r
JOIN analyses a ON a.recording_id = r.id
WHERE r.user_id = 1
ORDER BY r.submitted_at DESC, r.id DESC
LIMIT 20 OFFSET 0;


-- =============================================================================
-- PLAN 02. 사용자별 기록 수
-- 원본: RecordingQueryRepository.countOwned
-- 기대 인덱스: ix_recordings_user_submitted의 선두 컬럼 user_id
-- 확인: 기록이 많을 때 Index Only Scan 여부와 Heap Fetches 수
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT count(*)
FROM recordings
WHERE user_id = 1;


-- =============================================================================
-- PLAN 03. 기록 상세 + 분석 단건
-- 원본: RecordingQueryRepository.findOwnedDetail
-- 기대 인덱스: pk_recordings, uq_analyses_recording
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT r.id AS "recordingId",
       r.submitted_at AS "submittedAt",
       r.duration_ms AS "durationMs",
       r.mime_type AS "mimeType",
       r.file_size_bytes AS "fileSizeBytes",
       a.id AS "analysisId",
       a.status AS "status",
       a.attempt_no AS "attemptNo",
       a.auto_retry_count AS "autoRetryCount",
       a.failure_code AS "failureCode",
       a.started_at AS "startedAt",
       a.finished_at AS "finishedAt",
       a.filler_total_count AS "fillerTotalCount",
       a.algorithm_version AS "algorithmVersion",
       a.engine_type AS "engineType",
       a.engine_version AS "engineVersion"
FROM recordings r
JOIN analyses a ON a.recording_id = r.id
WHERE r.id = 101
  AND r.user_id = 1;


-- =============================================================================
-- PLAN 04. 분석 상태 폴링
-- 원본: AnalysisStatusQueryRepository.findByIdAndUserId
-- 기대 인덱스: pk_analyses, pk_recordings
-- 확인: 폴링 1회가 단건 인덱스 조회 2개 이내로 끝나는지
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT a.*
FROM analyses a
JOIN recordings r ON r.id = a.recording_id
WHERE a.id = 5001
  AND r.user_id = 1;


-- =============================================================================
-- PLAN 05. 최근 완료 분석 3건
-- 원본: RecordingQueryRepository.findRecentCompleted
-- 기대 인덱스:
--   ix_recordings_user_submitted, uq_analyses_recording
-- 확인:
--   완료되지 않은 기록이 많을 때 Rows Removed by Filter와 읽은 Buffer 수
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT r.id AS "recordingId",
       r.submitted_at AS "submittedAt",
       r.duration_ms AS "durationMs",
       r.mime_type AS "mimeType",
       r.file_size_bytes AS "fileSizeBytes",
       a.id AS "analysisId",
       a.status AS "status",
       a.attempt_no AS "attemptNo",
       a.auto_retry_count AS "autoRetryCount",
       a.failure_code AS "failureCode",
       a.started_at AS "startedAt",
       a.finished_at AS "finishedAt",
       a.filler_total_count AS "fillerTotalCount",
       a.algorithm_version AS "algorithmVersion",
       a.engine_type AS "engineType",
       a.engine_version AS "engineVersion"
FROM recordings r
JOIN analyses a ON a.recording_id = r.id
WHERE r.user_id = 1
  AND a.status = 'COMPLETED'
ORDER BY r.submitted_at DESC, r.id DESC
LIMIT 3;


-- =============================================================================
-- PLAN 06. PRO 상세의 부모 결과
-- 원본: ProResultQueryRepository.findOwnedResult
-- 기대 인덱스:
--   pk_recordings, pk_users, uq_analyses_recording,
--   uq_analysis_pro_results_analysis
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT r.id AS "recordingId",
       r.duration_ms AS "durationMs",
       u.plan AS "plan",
       a.id AS "analysisId",
       a.status AS "status",
       a.speech_duration_ms AS "speechDurationMs",
       a.silence_duration_ms AS "silenceDurationMs",
       a.filler_total_count AS "fillerTotalCount",
       a.long_silence_count AS "longSilenceCount",
       a.repeated_expression_count AS "repeatedExpressionCount",
       a.algorithm_version AS "algorithmVersion",
       a.engine_type AS "engineType",
       a.engine_version AS "engineVersion",
       p.id AS "proResultId",
       p.words_per_minute AS "wordsPerMinute",
       p.total_word_count AS "totalWordCount",
       p.speech_intervals AS "speechIntervals",
       p.waveform AS "waveform",
       p.coaching_summary AS "coachingSummary",
       p.coaching_practice_recommendation AS "coachingPracticeRecommendation"
FROM recordings r
JOIN users u ON u.id = r.user_id
JOIN analyses a ON a.recording_id = r.id
LEFT JOIN analysis_pro_results p ON p.analysis_id = a.id
WHERE r.id = 101
  AND r.user_id = 1;


-- =============================================================================
-- PLAN 07-A. PRO 상세 - 추임새 표현별 집계
-- 기대 인덱스: uq_filler_breakdowns_expression (analysis_id, expression)
-- 행이 최대 2개이므로 작은 Sort는 정상이다.
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT expression AS "expression",
       occurrence_count AS "count"
FROM filler_breakdowns
WHERE analysis_id = 5001
ORDER BY occurrence_count DESC, expression ASC;


-- =============================================================================
-- PLAN 07-B. PRO 상세 - 추임새 타임라인
-- 기대 인덱스: uq_filler_timeline_events_index (analysis_id, event_index)
-- 확인: 별도 Sort 없이 event_index 순으로 반환되는지
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT event_index AS "eventIndex",
       time_ms AS "timeMs",
       expression AS "expression"
FROM filler_timeline_events
WHERE analysis_id = 5001
ORDER BY event_index ASC;


-- =============================================================================
-- PLAN 07-C. PRO 상세 - 구간별 분석
-- 기대 인덱스: uq_segment_analyses_segment (analysis_id, segment)
-- 행이 항상 3개이므로 CASE 정렬을 위한 작은 Sort는 정상이다.
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT segment AS "segment",
       filler_count AS "fillerCount",
       habit_word_count AS "habitWordCount"
FROM segment_analyses
WHERE analysis_id = 5001
ORDER BY CASE segment
             WHEN 'INITIAL' THEN 0
             WHEN 'MIDDLE' THEN 1
             ELSE 2
         END;


-- =============================================================================
-- PLAN 07-D. PRO 상세 - 코칭 행동 항목
-- 현재 애플리케이션은 PLAN 06에서 구한 proResultId를 바인딩한다.
-- 아래 서브쿼리는 seed 데이터의 id를 하드코딩하지 않기 위한 점검용이다.
-- 실제 애플리케이션 쿼리만 정확히 보고 싶으면 먼저 pro_result_id를 조회해 숫자로 교체한다.
-- 기대 인덱스:
--   uq_analysis_pro_results_analysis, uq_coaching_action_items_order
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT content
FROM coaching_action_items
WHERE pro_result_id = (
    SELECT id
    FROM analysis_pro_results
    WHERE analysis_id = 5001
)
ORDER BY sort_order ASC;


-- =============================================================================
-- PLAN 08-A. 사용자별 활성 분석 수 - 현재 쿼리
-- 원본: AnalysisWriteRepository.countActiveByUserId
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT count(*)
FROM analyses a
JOIN recordings r ON r.id = a.recording_id
WHERE r.user_id = 1
  AND a.status IN ('PENDING', 'PROCESSING');


-- =============================================================================
-- PLAN 08-B. 사용자별 활성 분석 존재 여부 - 비교 후보
-- PLAN 08-A와 Execution Time, Actual Rows, Shared Blocks를 비교한다.
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT EXISTS (
    SELECT 1
    FROM analyses a
    JOIN recordings r ON r.id = a.recording_id
    WHERE r.user_id = 1
      AND a.status IN ('PENDING', 'PROCESSING')
);


-- =============================================================================
-- PLAN 09. 일별 통계
-- 원본: DailyStatsRepository.findDailyStats
-- 기대 인덱스:
--   ix_recordings_user_submitted의 user_id + submitted_at 범위,
--   uq_analyses_recording
-- 확인: 날짜 변환은 GROUP BY에만 있고 인덱스 범위 조건을 방해하지 않는지
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT (r.submitted_at AT TIME ZONE 'Asia/Seoul')::date AS "date",
       count(*) AS "practiceCount",
       sum(a.filler_total_count) AS "fillerTotalCount"
FROM recordings r
JOIN analyses a ON a.recording_id = r.id
WHERE r.user_id = 1
  AND r.submitted_at >= TIMESTAMPTZ '2026-08-01T15:00:00Z'
  AND r.submitted_at < TIMESTAMPTZ '2026-09-05T15:00:00Z'
  AND a.status = 'COMPLETED'
  AND a.algorithm_version = 'speech-habits-v1'
GROUP BY (r.submitted_at AT TIME ZONE 'Asia/Seoul')::date
ORDER BY (r.submitted_at AT TIME ZONE 'Asia/Seoul')::date;


-- =============================================================================
-- PLAN 10. 이전 완료 기록 자동 비교 대상
-- 원본: CompareRepository.findPreviousCompleted
-- 기대 인덱스:
--   ix_recordings_user_submitted, uq_analyses_recording,
--   uq_analysis_pro_results_analysis
-- 확인:
--   상태/알고리즘 버전이 다른 기록이 많을 때 필터 제거 행과 Buffer가 커지는지
-- =============================================================================
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, SETTINGS, FORMAT JSON)
SELECT r.id AS "recordingId",
       r.submitted_at AS "submittedAt",
       r.duration_ms AS "durationMs",
       u.plan AS "plan",
       a.status AS "status",
       a.filler_total_count AS "fillerTotalCount",
       a.silence_duration_ms AS "silenceDurationMs",
       a.algorithm_version AS "algorithmVersion",
       p.words_per_minute AS "wordsPerMinute"
FROM recordings r
JOIN users u ON u.id = r.user_id
JOIN analyses a ON a.recording_id = r.id
LEFT JOIN analysis_pro_results p ON p.analysis_id = a.id
WHERE r.user_id = 1
  AND a.status = 'COMPLETED'
  AND a.algorithm_version = 'speech-habits-v1'
  AND (
      r.submitted_at < TIMESTAMPTZ '2026-09-03T03:00:00Z'
      OR (
          r.submitted_at = TIMESTAMPTZ '2026-09-03T03:00:00Z'
          AND r.id < 101
      )
  )
ORDER BY r.submitted_at DESC, r.id DESC
LIMIT 1;
