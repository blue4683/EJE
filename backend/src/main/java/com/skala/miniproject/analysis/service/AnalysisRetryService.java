package com.skala.miniproject.analysis.service;

import com.skala.miniproject.analysis.AnalysisSlotGuard;
import com.skala.miniproject.analysis.idempotency.IdempotencyService;
import com.skala.miniproject.analysis.idempotency.RequestFingerprint;
import com.skala.miniproject.analysis.pipeline.AnalysisExecutor;
import com.skala.miniproject.analysis.repository.AnalysisWriteRepository;
import com.skala.miniproject.analysis.repository.UserLockRepository;
import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.config.AnalysisProperties;
import com.skala.miniproject.domain.analysis.Analysis;
import com.skala.miniproject.domain.analysis.AnalysisStatus;
import com.skala.miniproject.domain.idempotency.ApiIdempotencyKey;
import com.skala.miniproject.domain.idempotency.IdempotencyOperation;
import com.skala.miniproject.domain.recording.Recording;
import com.skala.miniproject.recording.dto.AcceptedResponse;
import com.skala.miniproject.recording.repository.RecordingWriteRepository;
import com.skala.miniproject.recording.service.UploadSlotGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * API 09 {@code POST /analyses/{analysisId}/retry}. API명세서.md 「멱등 접수 계약」·
 * 「상태·작업 수명·재시도 계약」의 FAILED → PENDING 행, 00-공통기반.md §C4(락 순서)를 따른다.
 *
 * 검사 순서(B7 §5-1, 바꾸면 안 된다): ① 멱등 재전송 판정 → ② 소유권 → ③ FAILED 상태 →
 * ④ attemptNo&lt;4 → ⑤ 첨부 파일이 최초와 동일한가 → ⑥ 사용자 활성 작업 부재.
 * ①이 가장 먼저인 이유: 재전송을 상태 검사보다 먼저 걸러야 네트워크 재시도가 409로 튀지 않는다.
 *
 * B4(RecordingSubmitService)와 달리 새 행을 만들지 않고 기존 {@code analyses} 행을 갱신하므로,
 * §C4의 "users → analyses" 잠금 순서를 그대로 지키기 위해 짧은 트랜잭션 안에서 users 를 먼저
 * 잠근 뒤 대상 analyses 행도 잠그고 상태·attemptNo 를 다시 확인한다 — 사전 검사와 커밋 사이의
 * 경합(서로 다른 Idempotency-Key 로 온 동시 재시도 등)이 같은 행을 이중으로 갱신하지 못하게 한다.
 */
@Service
public class AnalysisRetryService {

    /** 이 인스턴스가 재시도로 전이하는 분석의 worker_id. 프로세스 생존 동안 고정된 값이다. */
    private final UUID workerId = UUID.randomUUID();

    private final UploadSlotGuard uploadSlotGuard;
    private final AnalysisSlotGuard analysisSlotGuard;
    private final IdempotencyService idempotencyService;
    private final AnalysisWriteRepository analysisWriteRepository;
    private final RecordingWriteRepository recordingWriteRepository;
    private final UserLockRepository userLockRepository;
    private final AnalysisProperties analysisProperties;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate shortTransaction;
    private final AnalysisExecutor analysisExecutor;

    public AnalysisRetryService(
            UploadSlotGuard uploadSlotGuard,
            AnalysisSlotGuard analysisSlotGuard,
            IdempotencyService idempotencyService,
            AnalysisWriteRepository analysisWriteRepository,
            RecordingWriteRepository recordingWriteRepository,
            UserLockRepository userLockRepository,
            AnalysisProperties analysisProperties,
            JsonMapper jsonMapper,
            PlatformTransactionManager transactionManager,
            AnalysisExecutor analysisExecutor
    ) {
        this.uploadSlotGuard = uploadSlotGuard;
        this.analysisSlotGuard = analysisSlotGuard;
        this.idempotencyService = idempotencyService;
        this.analysisWriteRepository = analysisWriteRepository;
        this.recordingWriteRepository = recordingWriteRepository;
        this.userLockRepository = userLockRepository;
        this.analysisProperties = analysisProperties;
        this.jsonMapper = jsonMapper;
        this.shortTransaction = new TransactionTemplate(transactionManager);
        this.analysisExecutor = analysisExecutor;
    }

    /** @return 최종 202 envelope 의 JSON 문자열. 컨트롤러가 그대로 응답 본문에 쓴다. */
    public String retry(Long userId, Long analysisId, UUID idempotencyKey, MultipartFile audioPart) {
        if (!uploadSlotGuard.tryAcquire()) {
            throw new BusinessException(ErrorCode.ANALYSIS_CAPACITY_EXCEEDED);
        }
        boolean analysisSlotAcquired = false;
        boolean handedOffToPipeline = false;
        try {
            byte[] audioBytes = readBytes(audioPart);

            // 멱등 접수 계약: 빈 파일은 지문 조회 전에 거절한다.
            String audioSha256 = idempotencyService.requireAudioSha256(audioBytes);
            String targetAnalysisId = String.valueOf(analysisId);
            String requestFingerprint = RequestFingerprint.build(
                    IdempotencyOperation.RETRY_ANALYSIS, targetAnalysisId, audioSha256);

            Instant now = Instant.now();

            // ① 유효한 멱등 키 재전송인지 가장 먼저 판정한다.
            Optional<ApiIdempotencyKey> existing = idempotencyService.findValid(userId, idempotencyKey, now);
            if (existing.isPresent()) {
                return idempotencyService.reproduceOrFail(existing.get(), requestFingerprint);
            }

            // ② 소유권 — 없거나 남의 것이면 404.
            Analysis analysis = analysisWriteRepository.findByIdAndUserId(analysisId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

            // ③ FAILED 상태인가.
            if (analysis.getStatus() != AnalysisStatus.FAILED) {
                throw new BusinessException(ErrorCode.INVALID_ANALYSIS_STATE);
            }
            // ④ attemptNo < 4 인가 (명세: 수동 재시도 3회 소진 = attemptNo 4).
            if (analysis.getAttemptNo() >= 4) {
                throw new BusinessException(ErrorCode.MANUAL_RETRY_LIMIT_EXCEEDED);
            }
            // ⑤ 첨부 파일이 최초와 동일한가. INVALID_AUDIO 가 아니라 AUDIO_MISMATCH 다.
            Recording recording = recordingWriteRepository.findById(analysis.getRecordingId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            if (!audioSha256.equals(recording.getAudioSha256())) {
                throw new BusinessException(ErrorCode.AUDIO_MISMATCH);
            }

            if (!analysisSlotGuard.tryAcquire()) {
                throw new BusinessException(ErrorCode.ANALYSIS_CAPACITY_EXCEEDED);
            }
            analysisSlotAcquired = true;

            RetryOutcome outcome = shortTransaction.execute(status -> commitRetry(
                    userId, analysisId, idempotencyKey, requestFingerprint));

            if (outcome.newlyRetried()) {
                analysisExecutor.execute(userId, outcome.recordingId(), outcome.analysisId(), workerId,
                        outcome.attemptNo(), audioBytes, recording.getDurationMs(), outcome.executionDeadlineAt());
                handedOffToPipeline = true;
            }
            return outcome.responseBody();
        } finally {
            if (analysisSlotAcquired && !handedOffToPipeline) {
                analysisSlotGuard.release();
            }
            uploadSlotGuard.release();
        }
    }

    /** §C4 — users 를 먼저 잠근 뒤 대상 analyses 행을 잠그고, 상태·attemptNo·사용자 활성 개수를 다시 확인한다. */
    private RetryOutcome commitRetry(Long userId, Long analysisId, UUID idempotencyKey, String requestFingerprint) {
        Instant now = Instant.now();
        userLockRepository.lockById(userId);

        // 잠금 안에서 재확인 (규칙 3) — 경쟁 요청이 먼저 재시도를 접수했거나, 만료된 동일 키가 남아 있을 수 있다.
        idempotencyService.deleteIfExpired(userId, idempotencyKey, now);
        Optional<ApiIdempotencyKey> racedKey = idempotencyService.findValid(userId, idempotencyKey, now);
        if (racedKey.isPresent()) {
            return RetryOutcome.replay(idempotencyService.reproduceOrFail(racedKey.get(), requestFingerprint));
        }

        // ③④ 재확인 — 사전 검사(락 밖) 통과 후 이 잠금을 얻기까지 경쟁 요청이 먼저 커밋했을 수 있다.
        Analysis analysis = analysisWriteRepository.lockById(analysisId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (analysis.getStatus() != AnalysisStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ANALYSIS_STATE);
        }
        if (analysis.getAttemptNo() >= 4) {
            throw new BusinessException(ErrorCode.MANUAL_RETRY_LIMIT_EXCEEDED);
        }
        // ⑥ 사용자 활성 작업 부재.
        if (analysisWriteRepository.countActiveByUserId(userId) > 0) {
            throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_ACTIVE);
        }

        Instant lease = now.plusSeconds(analysisProperties.leaseSeconds());
        Instant deadline = now.plusSeconds(analysisProperties.executionDeadlineSeconds());
        analysis.retry(workerId, lease, deadline, now);

        AcceptedResponse accepted = new AcceptedResponse(
                String.valueOf(analysis.getRecordingId()), String.valueOf(analysis.getId()),
                analysis.getStatus().name(), analysis.getAttemptNo(), analysis.getAutoRetryCount());
        String responseBody = jsonMapper.writeValueAsString(ApiResponse.ok(accepted));

        idempotencyService.saveAccepted(userId, idempotencyKey, IdempotencyOperation.RETRY_ANALYSIS,
                requestFingerprint, analysis.getRecordingId(), analysis.getId(), responseBody, now);

        return new RetryOutcome(responseBody, true, analysis.getRecordingId(), analysis.getId(),
                analysis.getAttemptNo(), deadline);
    }

    /**
     * newlyRetried=false(멱등 재전송)면 나머지 필드는 쓰지 않는다 — 그 경우 새로 실행을 등록할
     * 대상이 없기 때문이다.
     */
    private record RetryOutcome(String responseBody, boolean newlyRetried, Long recordingId, Long analysisId,
                                 Integer attemptNo, Instant executionDeadlineAt) {
        static RetryOutcome replay(String responseBody) {
            return new RetryOutcome(responseBody, false, null, null, null, null);
        }
    }

    private byte[] readBytes(MultipartFile audioPart) {
        try {
            return audioPart.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 스트림을 읽을 수 없습니다.", e);
        }
    }
}
