package com.skala.miniproject.recording;

import com.skala.miniproject.analysis.idempotency.IdempotencyKeyWriteRepository;
import com.skala.miniproject.analysis.idempotency.IdempotencyService;
import com.skala.miniproject.common.exception.BusinessException;
import com.skala.miniproject.common.exception.ErrorCode;
import com.skala.miniproject.domain.idempotency.ApiIdempotencyKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IdempotencyService 단위 테스트. 실제 DB 없이 IdempotencyKeyWriteRepository 를 목으로 대체한다.
 * 핵심 회귀 시나리오: "비어 있는 파일은 지문 조회 전에 거절한다"(API명세서.md 「멱등 접수 계약」) —
 * 이 순서가 깨지면 같은 키를 빈 파일로 재전송했을 때 IDEMPOTENCY_KEY_CONFLICT 가 먼저 나가 버린다.
 */
class IdempotencyTest {

    private final IdempotencyKeyWriteRepository repository = mock(IdempotencyKeyWriteRepository.class);
    private final IdempotencyService service = new IdempotencyService(repository);

    @Test
    void 빈_파일은_저장소를_조회하지_않고_바로_INVALID_AUDIO다() {
        assertThatThrownBy(() -> service.requireAudioSha256(new byte[0]))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AUDIO));

        verify(repository, never()).findByUserIdAndIdempotencyKey(any(), any());
    }

    @Test
    void 빈_파일이_아니면_소문자_64자_지문을_반환한다() {
        String sha256 = service.requireAudioSha256(new byte[]{1, 2, 3});

        assertThat(sha256).matches("^[0-9a-f]{64}$");
    }

    @Test
    void 만료된_키는_유효하지_않은_것으로_취급한다() {
        Instant now = Instant.now();
        ApiIdempotencyKey expired = mock(ApiIdempotencyKey.class);
        when(expired.getExpiresAt()).thenReturn(now.minusSeconds(1));
        when(repository.findByUserIdAndIdempotencyKey(1L, key())).thenReturn(Optional.of(expired));

        Optional<ApiIdempotencyKey> result = service.findValid(1L, key(), now);

        assertThat(result).isEmpty();
    }

    @Test
    void 유효기간이_남은_키는_그대로_찾는다() {
        Instant now = Instant.now();
        ApiIdempotencyKey valid = mock(ApiIdempotencyKey.class);
        when(valid.getExpiresAt()).thenReturn(now.plusSeconds(1));
        when(repository.findByUserIdAndIdempotencyKey(1L, key())).thenReturn(Optional.of(valid));

        Optional<ApiIdempotencyKey> result = service.findValid(1L, key(), now);

        assertThat(result).contains(valid);
    }

    @Test
    void 만료된_키는_잠금_안에서_삭제한다() {
        Instant now = Instant.now();
        ApiIdempotencyKey expired = mock(ApiIdempotencyKey.class);
        when(expired.getExpiresAt()).thenReturn(now.minusSeconds(1));
        when(repository.findByUserIdAndIdempotencyKey(1L, key())).thenReturn(Optional.of(expired));

        service.deleteIfExpired(1L, key(), now);

        verify(repository).delete(expired);
    }

    @Test
    void 유효한_키는_삭제하지_않는다() {
        Instant now = Instant.now();
        ApiIdempotencyKey valid = mock(ApiIdempotencyKey.class);
        when(valid.getExpiresAt()).thenReturn(now.plusSeconds(1));
        when(repository.findByUserIdAndIdempotencyKey(1L, key())).thenReturn(Optional.of(valid));

        service.deleteIfExpired(1L, key(), now);

        verify(repository, never()).delete(any());
    }

    @Test
    void 같은_지문이고_참조가_살아있으면_최초_응답을_그대로_반환한다() {
        ApiIdempotencyKey existing = mock(ApiIdempotencyKey.class);
        when(existing.getRequestFingerprint()).thenReturn("fp");
        when(existing.getRecordingId()).thenReturn(101L);
        when(existing.getAnalysisId()).thenReturn(5001L);
        when(existing.getResponseBody()).thenReturn("{\"success\":true}");

        String body = service.reproduceOrFail(existing, "fp");

        assertThat(body).isEqualTo("{\"success\":true}");
    }

    @Test
    void 지문이_다르면_IDEMPOTENCY_KEY_CONFLICT다() {
        ApiIdempotencyKey existing = mock(ApiIdempotencyKey.class);
        when(existing.getRequestFingerprint()).thenReturn("fp-original");

        assertThatThrownBy(() -> service.reproduceOrFail(existing, "fp-different"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT));
    }

    @Test
    void 지문은_같지만_참조가_삭제됐으면_RESOURCE_GONE이다() {
        ApiIdempotencyKey existing = mock(ApiIdempotencyKey.class);
        when(existing.getRequestFingerprint()).thenReturn("fp");
        when(existing.getRecordingId()).thenReturn(null);
        when(existing.getAnalysisId()).thenReturn(5001L);

        assertThatThrownBy(() -> service.reproduceOrFail(existing, "fp"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_GONE));
    }

    private UUID key() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
