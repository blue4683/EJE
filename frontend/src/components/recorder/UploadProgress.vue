<script setup>
defineProps({
  progress: { type: Number, default: 0 },
  active: { type: Boolean, default: false },
})
</script>

<template>
  <div v-if="active" class="upload-progress" role="status" aria-live="polite">
    <div class="upload-progress__label">
      <span>음성 업로드 중</span>
      <strong>{{ progress }}%</strong>
    </div>
    <progress :value="progress" max="100">{{ progress }}%</progress>
    <p>업로드가 끝나면 분석 대기 단계로 넘어갑니다.</p>
  </div>
</template>

<style scoped>
.upload-progress { display: grid; gap: var(--space-2); }
.upload-progress__label { display: flex; justify-content: space-between; }
progress { width: 100%; accent-color: var(--color-primary); }
.upload-progress p { margin: 0; color: var(--color-text-muted); font-size: 0.875rem; }
</style>
