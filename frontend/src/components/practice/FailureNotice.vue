<script setup>
const FAILURE_MESSAGE = {
  STT_TIMEOUT: '음성 인식이 시간 안에 끝나지 않았습니다.',
  UPSTREAM_RATE_LIMIT: '요청이 몰려 처리하지 못했습니다.',
  UPSTREAM_UNAVAILABLE: '분석 서비스에 일시적인 문제가 있었습니다.',
  ANALYSIS_TIMEOUT: '분석이 제한 시간을 넘겼습니다.',
  WORKER_LOST: '분석이 중단되었습니다.',
  COACHING_FAILED: '코칭 문구를 만들지 못했습니다.',
  INVALID_ANALYSIS_RESULT: '분석 결과가 올바르지 않습니다.',
  INTERNAL_ERROR: '알 수 없는 오류가 발생했습니다.',
}

defineProps({
  failureCode: { type: String, default: null },
})
</script>

<template>
  <div class="notice" role="alert">
    <strong>분석에 실패했습니다.</strong>
    <p>{{ FAILURE_MESSAGE[failureCode] || '분석에 실패했습니다.' }}</p>
  </div>
</template>

<style scoped>
.notice { padding: var(--space-4); color: var(--color-danger); background: var(--color-surface);
  border-radius: var(--radius-2); }
.notice p { margin-bottom: 0; }
</style>
