<script setup>
defineProps({
  retryable: { type: Boolean, default: false },
  hasAudio: { type: Boolean, default: false },
  exhausted: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  progress: { type: Number, default: 0 },
  error: { type: Object, default: null },
})

defineEmits(['retry'])
</script>

<template>
  <section v-if="retryable || exhausted" class="retry-panel">
    <template v-if="retryable && hasAudio">
      <p v-if="error" class="error" role="alert">{{ error.message }}</p>
      <p v-if="busy">원본 음성을 업로드하고 있습니다. {{ progress }}%</p>
      <button type="button" :disabled="busy" @click="$emit('retry')">
        {{ busy ? '다시 시도 중…' : '다시 시도' }}
      </button>
    </template>
    <template v-else>
      <p>
        {{ exhausted
          ? '재시도 횟수를 모두 사용했습니다. 새로 녹음해 주세요.'
          : '재시도하려면 원본 음성이 필요합니다. 새로 녹음해 주세요.' }}
      </p>
      <router-link :to="{ name: 'record' }">새로 녹음하기</router-link>
    </template>
  </section>
</template>

<style scoped>
.retry-panel { display: grid; gap: var(--space-3); padding: var(--space-4);
  background: var(--color-surface); border-radius: var(--radius-2); }
.retry-panel p { margin: 0; }
.error { color: var(--color-danger); }
button { width: fit-content; padding: var(--space-2) var(--space-4); border: 0;
  border-radius: var(--radius-1); color: white; background: var(--color-primary); }
button:disabled { cursor: wait; opacity: 0.6; }
</style>
