<script setup>
defineProps({
  submitting: { type: Boolean, default: false },
  progress: { type: Number, default: 0 },
  online: { type: Boolean, default: true },
  error: { type: Object, default: null },
  autoRetryScheduled: { type: Boolean, default: false },
})

defineEmits(['cancel', 'retry', 'cancel-auto-retry'])
</script>

<template>
  <section class="controller" aria-label="업로드 상태">
    <p v-if="!online" class="banner" role="status">
      오프라인 상태입니다. 녹음은 유지되며 온라인으로 돌아온 뒤 직접 제출할 수 있습니다.
    </p>

    <div v-if="submitting" class="actions">
      <span>업로드 {{ progress }}%</span>
      <button type="button" class="secondary" @click="$emit('cancel')">업로드 취소</button>
    </div>

    <div v-if="error" class="error" role="alert">
      <p>{{ error.message }}</p>
      <div class="actions">
        <button type="button" :disabled="!online" @click="$emit('retry')">다시 시도</button>
        <button
          v-if="autoRetryScheduled"
          type="button"
          class="secondary"
          @click="$emit('cancel-auto-retry')"
        >
          자동 재시도 취소
        </button>
      </div>
      <small v-if="autoRetryScheduled">10초 뒤 한 번 자동으로 다시 시도합니다.</small>
    </div>
  </section>
</template>

<style scoped>
.controller { display: grid; gap: var(--space-3); }
.banner,
.error { padding: var(--space-3); border-radius: var(--radius-1); }
.banner { color: var(--color-warning); background: var(--color-surface); }
.error { color: var(--color-danger); background: var(--color-surface); }
.error p { margin-top: 0; }
.actions { display: flex; align-items: center; gap: var(--space-3); }
button { padding: var(--space-2) var(--space-4); border: 0; border-radius: var(--radius-1);
  color: white; background: var(--color-primary); }
button:disabled { cursor: not-allowed; opacity: 0.5; }
.secondary { color: var(--color-text); background: var(--color-border); }
</style>
