<script setup>
import { nextTick, ref, watch } from 'vue'
import { removeRecording } from '@/api/history'

const props = defineProps({ open: { type: Boolean, required: true }, recordingId: { type: String, required: true } })
const emit = defineEmits(['close', 'deleted'])
const dialog = ref(null)
const submitting = ref(false)
const error = ref(null)

watch(() => props.open, async (open) => {
  await nextTick()
  if (open && !dialog.value?.open) dialog.value?.showModal()
  if (!open && dialog.value?.open) dialog.value.close()
})

function close() {
  if (submitting.value) return
  error.value = null
  emit('close')
}

async function confirm() {
  submitting.value = true
  error.value = null
  try {
    await removeRecording(props.recordingId)
    emit('deleted')
  } catch (caught) {
    error.value = caught
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <dialog ref="dialog" class="dialog" @cancel.prevent="close" @close="emit('close')">
    <form @submit.prevent="confirm">
      <h2>이 기록을 삭제할까요?</h2>
      <p>삭제한 기록과 분석 결과는 복구할 수 없습니다.</p>
      <p v-if="error" class="error" role="alert">{{ error.message }}</p>
      <p v-if="error?.code === 'CANNOT_DELETE_WHILE_PROCESSING'" class="help">분석이 끝난 뒤 삭제할 수 있습니다. 기록은 그대로 유지됩니다.</p>
      <div class="actions">
        <button type="button" class="secondary" :disabled="submitting" @click="close">취소</button>
        <button type="submit" class="danger" :disabled="submitting">{{ submitting ? '삭제 중…' : '삭제' }}</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.dialog { width: min(calc(100% - 32px), 430px); padding: 0; color: var(--color-text); border: 0; border-radius: var(--radius-2); box-shadow: 0 24px 80px rgba(20, 28, 48, 0.28); }
.dialog::backdrop { background: rgba(24, 32, 51, 0.55); }
form { display: grid; gap: var(--space-4); padding: var(--space-6); }
h2, p { margin: 0; }
p { color: var(--color-text-muted); }
.error { color: var(--color-danger); }
.help { padding: var(--space-3); background: var(--color-warning-weak); border-radius: var(--radius-1); }
.actions { display: flex; justify-content: end; gap: var(--space-2); }
button { padding: 10px 14px; font-weight: 800; border-radius: var(--radius-1); }
.secondary { color: var(--color-text); background: white; border: 1px solid var(--color-border); }
.danger { color: white; background: var(--color-danger); border: 1px solid var(--color-danger); }
</style>
