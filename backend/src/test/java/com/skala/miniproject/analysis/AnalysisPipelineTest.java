package com.skala.miniproject.analysis;

import com.skala.miniproject.analysis.client.SpeechAnalysisClient;
import com.skala.miniproject.analysis.client.SpeechAnalysisClientException;
import com.skala.miniproject.analysis.pipeline.AnalysisExecutor;
import com.skala.miniproject.analysis.pipeline.AnalysisResultWriter;
import com.skala.miniproject.analysis.pipeline.AnalysisStateMachine;
import com.skala.miniproject.analysis.repository.AnalysisResultWriteRepository;
import com.skala.miniproject.analysis.repository.AnalysisWriteRepository;
import com.skala.miniproject.domain.analysis.Analysis;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.analysis.EngineType;
import com.skala.miniproject.domain.analysis.FailureCode;
import com.skala.miniproject.domain.recording.Recording;
import com.skala.miniproject.recording.repository.RecordingWriteRepository;
import com.skala.miniproject.speech.SpeechHabitsAnalyzer;
import com.skala.miniproject.speech.model.SpeechInterval;
import com.skala.miniproject.speech.model.SpeechMetrics;
import com.skala.miniproject.speech.model.TimedToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnalysisStateMachine·AnalysisResultWriter 를 직접 호출하는 동기 테스트. 짧은 트랜잭션이
 * TransactionTemplate(REQUIRED)로 이 테스트의 @Transactional 에 합류하므로 끝나면 롤백된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalysisPipelineTest {

    private static final Long PRO_USER_ID = 2L; // seed: 활성 분석 없음

    @Autowired
    private AnalysisStateMachine stateMachine;

    @Autowired
    private AnalysisResultWriter resultWriter;

    @Autowired
    private AnalysisWriteRepository analysisWriteRepository;

    @Autowired
    private AnalysisResultWriteRepository analysisResultWriteRepository;

    @Autowired
    private RecordingWriteRepository recordingWriteRepository;

    @Test
    void FAILED로_전이하면_workerId와_lease가_NULL이_된다() {
        UUID workerId = UUID.randomUUID();
        Analysis analysis = createPending(3000, workerId, Instant.now().plusSeconds(600));

        stateMachine.fail(analysis.getId(), workerId, FailureCode.INTERNAL_ERROR, Instant.now());

        Analysis reloaded = analysisResultWriteRepository.findById(analysis.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(reloaded.getFailureCode()).isEqualTo(FailureCode.INTERNAL_ERROR);
        assertThat(reloaded.getWorkerId()).isNull();
        assertThat(reloaded.getLeaseExpiresAt()).isNull();
        assertThat(reloaded.getFinishedAt()).isNotNull();
    }

    @Test
    void 다른_workerId의_요청은_상태를_바꾸지_못한다() {
        UUID owner = UUID.randomUUID();
        Analysis analysis = createPending(3000, owner, Instant.now().plusSeconds(600));

        boolean began = stateMachine.beginProcessing(analysis.getId(), UUID.randomUUID(), Instant.now());

        assertThat(began).isFalse();
        Analysis reloaded = analysisResultWriteRepository.findById(analysis.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void 이전_차수의_늦은_응답은_버린다() {
        UUID workerId = UUID.randomUUID();
        Analysis analysis = createPending(3000, workerId, Instant.now().plusSeconds(600));
        stateMachine.beginProcessing(analysis.getId(), workerId, Instant.now());
        // 그 사이 자동 재시도가 한 번 더 등록됐다고 가정한다 — DB 의 autoRetryCount 가 이미 1로 올라갔다.
        stateMachine.registerAutoRetry(analysis.getId(), workerId, Instant.now());

        // 늦게 도착한 응답은 이전 자동 재시도 차수(0)를 기준으로 만들어졌다.
        SpeechMetrics metrics = sampleMetrics();
        boolean saved = resultWriter.save(analysis.getId(), 1, 0, workerId, metrics, Instant.now());

        assertThat(saved).isFalse();
        Analysis reloaded = analysisResultWriteRepository.findById(analysis.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AnalysisStatus.PROCESSING); // 결과가 반영되지 않았다
    }

    @Test
    void 소유권이_맞으면_결과_5테이블과_COMPLETED를_한번에_커밋한다() {
        UUID workerId = UUID.randomUUID();
        Analysis analysis = createPending(3000, workerId, Instant.now().plusSeconds(600));
        stateMachine.beginProcessing(analysis.getId(), workerId, Instant.now());

        SpeechMetrics metrics = sampleMetrics();
        boolean saved = resultWriter.save(analysis.getId(), 1, 0, workerId, metrics, Instant.now());

        assertThat(saved).isTrue();
        Analysis reloaded = analysisResultWriteRepository.findById(analysis.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(reloaded.getWorkerId()).isNull();
        assertThat(reloaded.getLeaseExpiresAt()).isNull();
        assertThat(reloaded.getSpeechDurationMs() + reloaded.getSilenceDurationMs()).isEqualTo(3000);
    }

    private Analysis createPending(int durationMs, UUID workerId, Instant deadline) {
        Instant now = Instant.now();
        Recording recording = recordingWriteRepository.save(Recording.submit(
                PRO_USER_ID, durationMs, "audio/webm", 1000L, "0".repeat(64), now));
        return analysisWriteRepository.save(Analysis.pending(recording.getId(), workerId, now,
                now.plusSeconds(30), deadline, "speech-habits-v1", EngineType.MOCK, "mock-pipeline-v1"));
    }

    private SpeechMetrics sampleMetrics() {
        List<TimedToken> tokens = List.of(
                new TimedToken("음", 700, 800), new TimedToken("저는", 900, 1100),
                new TimedToken("저는", 1200, 1400), new TimedToken("사실", 1500, 1700),
                new TimedToken("어", 2100, 2200), new TimedToken("홍길동입니다", 2300, 2500));
        List<SpeechInterval> intervals = List.of(new SpeechInterval(500, 2500));
        double[] amplitudes = new double[30];
        for (int i = 0; i < 30; i++) {
            amplitudes[i] = (i >= 5 && i < 25) ? 0.65 : 0.02;
        }
        return SpeechHabitsAnalyzer.analyze(3000, intervals, tokens, amplitudes);
    }
}

/**
 * AnalysisExecutor 를 실제로 비동기 실행해서 관측하는 테스트. 실행이 다른 스레드·다른 트랜잭션에서
 * 커밋되므로 @Transactional 롤백에 기대지 않고 각 테스트가 만든 recording 을 직접 정리한다
 * (analyses·결과 5테이블은 recordings 의 ON DELETE CASCADE 로 함께 지워진다).
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisExecutorTest {

    private static final Long PRO_USER_ID = 2L;

    @Autowired
    private AnalysisExecutor analysisExecutor;

    @Autowired
    private AnalysisWriteRepository analysisWriteRepository;

    @Autowired
    private RecordingWriteRepository recordingWriteRepository;

    @MockitoBean
    private SpeechAnalysisClient speechAnalysisClient;

    private final List<Long> createdRecordingIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdRecordingIds.forEach(recordingWriteRepository::deleteById);
        createdRecordingIds.clear();
    }

    @Test
    void 일시오류_3종은_최대_3회_자동_재시도한다() throws InterruptedException {
        when(speechAnalysisClient.analyze(any()))
                .thenThrow(new SpeechAnalysisClientException(FailureCode.STT_TIMEOUT, "일시 오류"));

        Analysis analysis = runToTerminal(3000, Instant.now().plusSeconds(600));

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getFailureCode()).isEqualTo(FailureCode.STT_TIMEOUT);
        assertThat(analysis.getAutoRetryCount()).isEqualTo(3);
        verify(speechAnalysisClient, times(4)).analyze(any());
    }

    @Test
    void COACHING_FAILED는_자동_재시도하지_않는다() throws InterruptedException {
        when(speechAnalysisClient.analyze(any()))
                .thenThrow(new SpeechAnalysisClientException(FailureCode.COACHING_FAILED, "코칭 생성 실패"));

        Analysis analysis = runToTerminal(3000, Instant.now().plusSeconds(600));

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getFailureCode()).isEqualTo(FailureCode.COACHING_FAILED);
        assertThat(analysis.getAutoRetryCount()).isEqualTo(0);
        verify(speechAnalysisClient, times(1)).analyze(any());
    }

    @Test
    void 결과_불변식이_깨지면_INVALID_ANALYSIS_RESULT로_실패한다() throws InterruptedException {
        when(speechAnalysisClient.analyze(any()))
                .thenThrow(new SpeechAnalysisClientException(FailureCode.INVALID_ANALYSIS_RESULT, "불변식 위반"));

        Analysis analysis = runToTerminal(3000, Instant.now().plusSeconds(600));

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getFailureCode()).isEqualTo(FailureCode.INVALID_ANALYSIS_RESULT);
    }

    @Test
    void 전체_기한이_이미_지났으면_ANALYSIS_TIMEOUT이다() throws InterruptedException {
        Analysis analysis = runToTerminal(3000, Instant.now().minusSeconds(1));

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getFailureCode()).isEqualTo(FailureCode.ANALYSIS_TIMEOUT);
        verify(speechAnalysisClient, times(0)).analyze(any());
    }

    private Analysis runToTerminal(int durationMs, Instant deadline) throws InterruptedException {
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();
        Recording recording = recordingWriteRepository.save(Recording.submit(
                PRO_USER_ID, durationMs, "audio/webm", 1000L, "1".repeat(64), now));
        createdRecordingIds.add(recording.getId());
        Analysis analysis = analysisWriteRepository.save(Analysis.pending(recording.getId(), workerId, now,
                now.plusSeconds(30), deadline, "speech-habits-v1", EngineType.MOCK, "mock-pipeline-v1"));

        analysisExecutor.execute(PRO_USER_ID, recording.getId(), analysis.getId(), workerId, 1,
                "fixture".getBytes(), durationMs, deadline);

        return waitForTerminal(analysis.getId(), Duration.ofSeconds(20));
    }

    private Analysis waitForTerminal(Long analysisId, Duration timeout) throws InterruptedException {
        Instant limit = Instant.now().plus(timeout);
        while (Instant.now().isBefore(limit)) {
            Analysis analysis = analysisWriteRepository.findById(analysisId).orElseThrow();
            if (analysis.getStatus() == AnalysisStatus.COMPLETED || analysis.getStatus() == AnalysisStatus.FAILED) {
                return analysis;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("시간 내에 종료 상태에 도달하지 못했습니다: " + analysisId);
    }
}

/**
 * 실제 MockSpeechAnalysisClient(오버라이드 없음)로 PENDING → PROCESSING → COMPLETED 전이를
 * 직접 관측한다. durationMs=3000 은 API명세서.md API 17 예제와 같은 입력이라 결과 값을 그대로
 * 대조할 수 있다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AnalysisExecutorRealClientTest {

    private static final Long PRO_USER_ID = 2L;
    private static final int CANONICAL_DURATION_MS = 3000;

    @Autowired
    private AnalysisExecutor analysisExecutor;

    @Autowired
    private AnalysisWriteRepository analysisWriteRepository;

    @Autowired
    private RecordingWriteRepository recordingWriteRepository;

    @Test
    void PENDING에서_PROCESSING을_거쳐_COMPLETED로_전이한다() throws InterruptedException {
        UUID workerId = UUID.randomUUID();
        Instant now = Instant.now();
        Recording recording = recordingWriteRepository.save(Recording.submit(
                PRO_USER_ID, CANONICAL_DURATION_MS, "audio/webm", 1000L, "2".repeat(64), now));
        Analysis analysis = analysisWriteRepository.save(Analysis.pending(recording.getId(), workerId, now,
                now.plusSeconds(30), now.plusSeconds(600), "speech-habits-v1", EngineType.MOCK, "mock-pipeline-v1"));

        analysisExecutor.execute(PRO_USER_ID, recording.getId(), analysis.getId(), workerId, 1,
                "fixture".getBytes(), CANONICAL_DURATION_MS, now.plusSeconds(600));

        // PROCESSING 이 실제로 관측돼야 한다 (Mock 지연 2초가 있어야 가능하다)
        boolean observedProcessing = false;
        Instant limit = Instant.now().plusSeconds(5);
        while (Instant.now().isBefore(limit)) {
            if (analysisWriteRepository.findById(analysis.getId()).orElseThrow().getStatus()
                    == AnalysisStatus.PROCESSING) {
                observedProcessing = true;
                break;
            }
            Thread.sleep(100);
        }
        assertThat(observedProcessing).isTrue();

        Analysis completed = waitForCompletion(analysis.getId());
        assertThat(completed.getSpeechDurationMs()).isEqualTo(2000);
        assertThat(completed.getSilenceDurationMs()).isEqualTo(1000);
        assertThat(completed.getFillerTotalCount()).isEqualTo(2);
        assertThat(completed.getRepeatedExpressionCount()).isEqualTo(1);

        recordingWriteRepository.deleteById(recording.getId());
    }

    private Analysis waitForCompletion(Long analysisId) throws InterruptedException {
        Instant limit = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(limit)) {
            Analysis analysis = analysisWriteRepository.findById(analysisId).orElseThrow();
            if (analysis.getStatus() == AnalysisStatus.COMPLETED) {
                return analysis;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("시간 내에 COMPLETED 에 도달하지 못했습니다: " + analysisId);
    }
}
