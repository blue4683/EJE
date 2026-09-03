<script setup>
defineProps({
  state: { type: String, required: true },
  message: { type: String, default: '' },
  retryLabel: { type: String, default: '다시 시도' },
  canRetry: { type: Boolean, default: true },
})
defineEmits(['retry'])
</script>

<template>
  <div v-if="state === 'loading'" class="state" role="status" aria-live="polite">
    불러오는 중…
  </div>
  <div
    v-else-if="state === 'pending'"
    class="state state--pending"
    role="status"
    aria-live="polite"
  >
    <span class="spinner" aria-hidden="true" />
    <span>{{ message || '분석을 진행하고 있습니다. 잠시만 기다려 주세요…' }}</span>
  </div>
  <div v-else-if="state === 'error'" class="state state--error" role="alert">
    <p>{{ message }}</p>
    <button v-if="canRetry" type="button" @click="$emit('retry')">
      {{ retryLabel }}
    </button>
  </div>
  <div v-else-if="state === 'empty'" class="state state--empty">
    {{ message || '표시할 내용이 없습니다.' }}
  </div>
  <slot v-else />
</template>

<style scoped>
.state {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-8);
  color: var(--color-text-muted);
  background: var(--color-surface);
  border-radius: var(--radius-2);
}
.state--error { color: var(--color-danger); }
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
