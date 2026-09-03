<script setup>
const props = defineProps({
  state: { type: String, required: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['start', 'stop', 'reset'])

function handleClick() {
  if (props.state === 'recording') emit('stop')
  else if (props.state === 'ready') emit('reset')
  else emit('start')
}
</script>

<template>
  <button
    type="button"
    class="record-button"
    :class="{ 'record-button--active': state === 'recording' }"
    :disabled="disabled || state === 'requesting' || state === 'stopping'"
    @click="handleClick"
  >
    <span class="record-button__dot" aria-hidden="true" />
    <span v-if="state === 'requesting'">마이크 권한 확인 중…</span>
    <span v-else-if="state === 'stopping'">녹음 정리 중…</span>
    <span v-else-if="state === 'recording'">녹음 정지</span>
    <span v-else-if="state === 'ready'">다시 녹음</span>
    <span v-else>녹음 시작</span>
  </button>
</template>

<style scoped>
.record-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 180px;
  padding: 14px 22px;
  border: 0;
  border-radius: 999px;
  color: white;
  background: var(--color-primary, var(--accent));
  font-weight: 700;
}
.record-button:disabled { cursor: wait; opacity: 0.65; }
.record-button--active { background: var(--color-danger, #d64545); }
.record-button__dot { width: 12px; height: 12px; border-radius: 50%; background: currentColor; }
</style>
