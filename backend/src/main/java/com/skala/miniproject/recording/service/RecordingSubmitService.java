package com.skala.miniproject.recording.service;

import com.skala.miniproject.analysis.AnalysisSlotGuard;
import com.skala.miniproject.analysis.idempotency.IdempotencyService;
import com.skala.miniproject.analysis.idempotency.RequestFingerprint;
import com.skala.miniproject.analysis.pipeline.AnalysisExecutor;
import com.skala.miniproject.analysis.repository.AnalysisWriteRepository;
import com.skala.miniproject.analysis.repository.UserLockRepository;
import com.skala.miniproject.audio.AudioDecodeException;
import com.skala.miniproject.audio.AudioDecoder;
import com.skala.miniproject.audio.DecodedAudio;
import com.skala.miniproject.audio.MimeTypeNormalizer;
import com.skala.miniproject.common.dto.ApiResponse;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.config.AnalysisProperties;
import com.skala.miniproject.domain.analysis.Analysis;
import com.skala.miniproject.domain.analysis.EngineType;
import com.skala.miniproject.domain.idempotency.ApiIdempotencyKey;
import com.skala.miniproject.domain.idempotency.IdempotencyOperation;
import com.skala.miniproject.domain.recording.Recording;
import com.skala.miniproject.recording.dto.AcceptedResponse;
import com.skala.miniproject.recording.repository.RecordingWriteRepository;
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
 * API 07 {@code POST /recordings}. API명세서.md 「멱등 접수 계약」 규칙 1~5,
 * 00-공통기반.md §C1(비동기 경계)·§C4(락 순서)를 그대로 따른다.
 *
 * 흐름: 업로드 슬롯 확보 → 바이트 읽기 → (지문 조회 전) 빈 파일 거절 → 지문 계산·기존 키 조회
 * → 신규 키면 디코딩(트랜잭션 밖) → 분석 슬롯 확보 → 짧은 트랜잭션(users 잠금 → 재확인 → 커밋)
 * → 커밋 성공 시 비동기 실행 등록(B5).
 *
 * 분석 슬롯의 소유권: 신규 생성이 성공해 AnalysisExecutor 에 실행을 넘기면, 그 순간부터는
 * AnalysisExecutor 가 실행이 끝날 때(성공·실패) 슬롯을 해제한다 — 여기서는 넘기지 못한
 * 경우(멱등 재전송·검증 실패 등)에만 슬롯을 해제한다. 접수 단계에서 무조건 해제해 버리면
 * "인스턴스당 동시 분석 2개" 제한이 실제 처리 시간 동안은 지켜지지 않는다.
 */
@Service
public class RecordingSubmitService {

    private static final String CREATE_TARGET_ANALYSIS_ID = "-";
    private static final int INITIAL_ATTEMPT_NO = 1;

    /** 이 인스턴스가 접수하는 분석의 worker_id. 프로세스 생존 동안 고정된 값이다. */
    private final UUID workerId = UUID.randomUUID();

    private final UploadSlotGuard uploadSlotGuard;
    private final AnalysisSlotGuard analysisSlotGuard;
    private final AudioDecoder audioDecoder;
    private final IdempotencyService idempotencyService;
    private final RecordingWriteRepository recordingWriteRepository;
    private final AnalysisWriteRepository analysisWriteRepository;
    private final UserLockRepository userLockRepository;
    private final AnalysisProperties analysisProperties;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate shortTransaction;
    private final AnalysisExecutor analysisExecutor;

    public RecordingSubmitService(
            UploadSlotGuard uploadSlotGuard,
            AnalysisSlotGuard analysisSlotGuard,
            AudioDecoder audioDecoder,
            IdempotencyService idempotencyService,
            RecordingWriteRepository recordingWriteRepository,
            AnalysisWriteRepository analysisWriteRepository,
            UserLockRepository userLockRepository,
            AnalysisProperties analysisProperties,
            JsonMapper jsonMapper,
            PlatformTransactionManager transactionManager,
            AnalysisExecutor analysisExecutor
    ) {
        this.uploadSlotGuard = uploadSlotGuard;
        this.analysisSlotGuard = analysisSlotGuard;
        this.audioDecoder = audioDecoder;
        this.idempotencyService = idempotencyService;
        this.recordingWriteRepository = recordingWriteRepository;
        this.analysisWriteRepository = analysisWriteRepository;
        this.userLockRepository = userLockRepository;
        this.analysisProperties = analysisProperties;
        this.jsonMapper = jsonMapper;
        this.shortTransaction = new TransactionTemplate(transactionManager);
        this.analysisExecutor = analysisExecutor;
    }

    /** @return 최종 202 envelope 의 JSON 문자열. 컨트롤러가 그대로 응답 본문에 쓴다. */
    public String submit(Long userId, UUID idempotencyKey, MultipartFile audioPart) {
        if (!uploadSlotGuard.tryAcquire()) {
            throw new BusinessException(ErrorCode.ANALYSIS_CAPACITY_EXCEEDED);
        }
        boolean analysisSlotAcquired = false;
        boolean handedOffToPipeline = false;
        try {
            byte[] audioBytes = readBytes(audioPart);

            // 멱등 접수 계약: 빈 파일은 지문 조회 전에 거절한다. IdempotencyService 가 이 순서를 강제하므로
            // 여기서 audioSha256 을 얻기 전에는 어떤 키 조회도 일어나지 않는다.
            String audioSha256 = idempotencyService.requireAudioSha256(audioBytes);
            String requestFingerprint = RequestFingerprint.build(
                    IdempotencyOperation.CREATE_RECORDING, CREATE_TARGET_ANALYSIS_ID, audioSha256);

            Instant now = Instant.now();
            Optional<ApiIdempotencyKey> existing = idempotencyService.findValid(userId, idempotencyKey, now);
            if (existing.isPresent()) {
                return idempotencyService.reproduceOrFail(existing.get(), requestFingerprint);
            }

            // 신규 키 — 포맷·디코딩 길이 검증(B2)은 최대 10초 걸릴 수 있어 트랜잭션 밖에서 수행한다.
            DecodedAudio decoded = decode(audioBytes, audioPart.getContentType());
            String normalizedMime = MimeTypeNormalizer.normalize(audioPart.getContentType());

            if (!analysisSlotGuard.tryAcquire()) {
                throw new BusinessException(ErrorCode.ANALYSIS_CAPACITY_EXCEEDED);
            }
            analysisSlotAcquired = true;

            SubmissionOutcome outcome = shortTransaction.execute(status -> commitNewSubmission(
                    userId, idempotencyKey, requestFingerprint, audioSha256,
                    decoded, normalizedMime, audioBytes.length));

            if (outcome.newlyCreated()) {
                analysisExecutor.execute(userId, outcome.recordingId(), outcome.analysisId(), workerId,
                        INITIAL_ATTEMPT_NO, audioBytes, decoded.durationMs(), outcome.executionDeadlineAt());
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

    /** §C4 — users 를 가장 먼저 잠근 뒤 recordings·analyses·멱등 키를 원자적으로 커밋한다. */
    private SubmissionOutcome commitNewSubmission(Long userId, UUID idempotencyKey, String requestFingerprint,
                                                   String audioSha256, DecodedAudio decoded, String normalizedMime,
                                                   long fileSizeBytes) {
        Instant now = Instant.now();
        userLockRepository.lockById(userId);

        // 잠금 안에서 재확인 (규칙 3) — 경쟁 요청이 먼저 접수했거나, 만료된 동일 키가 남아 있을 수 있다.
        idempotencyService.deleteIfExpired(userId, idempotencyKey, now);
        Optional<ApiIdempotencyKey> racedKey = idempotencyService.findValid(userId, idempotencyKey, now);
        if (racedKey.isPresent()) {
            return SubmissionOutcome.replay(idempotencyService.reproduceOrFail(racedKey.get(), requestFingerprint));
        }

        if (analysisWriteRepository.countActiveByUserId(userId) > 0) {
            throw new BusinessException(ErrorCode.ANALYSIS_ALREADY_ACTIVE);
        }

        Recording recording = recordingWriteRepository.save(
                Recording.submit(userId, decoded.durationMs(), normalizedMime, fileSizeBytes, audioSha256, now));

        Instant lease = now.plusSeconds(analysisProperties.leaseSeconds());
        Instant deadline = now.plusSeconds(analysisProperties.executionDeadlineSeconds());
        Analysis analysis = analysisWriteRepository.save(Analysis.pending(
                recording.getId(), workerId, now, lease, deadline,
                analysisProperties.algorithmVersion(), EngineType.MOCK, analysisProperties.engineVersion()));

        AcceptedResponse accepted = new AcceptedResponse(
                String.valueOf(recording.getId()), String.valueOf(analysis.getId()),
                analysis.getStatus().name(), analysis.getAttemptNo(), analysis.getAutoRetryCount());
        String responseBody = jsonMapper.writeValueAsString(ApiResponse.ok(accepted));

        idempotencyService.saveAccepted(userId, idempotencyKey, IdempotencyOperation.CREATE_RECORDING,
                requestFingerprint, recording.getId(), analysis.getId(), responseBody, now);

        return new SubmissionOutcome(responseBody, true, recording.getId(), analysis.getId(), deadline);
    }

    /**
     * newlyCreated=false(멱등 재전송)면 recordingId·analysisId·executionDeadlineAt 은 쓰지 않는다 —
     * 그 경우 새로 실행을 등록할 대상이 없기 때문이다.
     */
    private record SubmissionOutcome(String responseBody, boolean newlyCreated, Long recordingId,
                                      Long analysisId, Instant executionDeadlineAt) {
        static SubmissionOutcome replay(String responseBody) {
            return new SubmissionOutcome(responseBody, false, null, null, null);
        }
    }

    private byte[] readBytes(MultipartFile audioPart) {
        try {
            return audioPart.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 스트림을 읽을 수 없습니다.", e);
        }
    }

    private DecodedAudio decode(byte[] audioBytes, String contentType) {
        try {
            return audioDecoder.decode(audioBytes, contentType);
        } catch (AudioDecodeException e) {
            throw new BusinessException(mapDecodeError(e.reason()));
        }
    }

    private ErrorCode mapDecodeError(AudioDecodeException.Reason reason) {
        return switch (reason) {
            case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case INVALID_AUDIO -> ErrorCode.INVALID_AUDIO;
            case DURATION_OUT_OF_RANGE -> ErrorCode.AUDIO_DURATION_OUT_OF_RANGE;
            case TIMEOUT -> ErrorCode.REQUEST_TIMEOUT;
        };
    }
}
