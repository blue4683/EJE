<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { withdraw } from '@/api/users'
import { validatePassword } from '@/utils/validators'

const props = defineProps({ open: { type: Boolean, required: true } })
const emit = defineEmits(['close', 'withdrawn'])
const dialog = ref(null)
const password = ref('')
const touched = ref(false)
const submitting = ref(false)
const error = ref(null)

const passwordError = computed(() => {
  if (error.value?.code === 'INVALID_PASSWORD' || error.value?.code === 'VALIDATION_ERROR') {
    return error.value.message
  }
  return touched.value ? validatePassword(password.value) : null
})

watch(
  () => props.open,
  async (open) => {
    await nextTick()
    if (open && !dialog.value?.open) dialog.value?.showModal()
    if (!open && dialog.value?.open) dialog.value.close()
  },
)

const close = () => {
  if (submitting.value) return
  password.value = ''
  touched.value = false
  error.value = null
  emit('close')
}

const submit = async () => {
  touched.value = true
  if (validatePassword(password.value)) return
  submitting.value = true
  error.value = null
  try {
    await withdraw(password.value)
    emit('withdrawn')
  } catch (caught) {
    error.value = caught
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <dialog ref="dialog" class="dialog" @cancel.prevent="close" @close="emit('close')">
    <form class="dialog__body" @submit.prevent="submit">
      <div>
        <p class="dialog__eyebrow">되돌릴 수 없는 작업</p>
        <h2>회원 탈퇴</h2>
        <p>계정과 모든 연습 기록이 함께 삭제됩니다. 계속하려면 비밀번호를 입력해 주세요.</p>
      </div>
      <label>
        <span>비밀번호</span>
        <input v-model="password" type="password" autocomplete="current-password" :aria-invalid="Boolean(passwordError)" @blur="touched = true" />
      </label>
      <p v-if="passwordError" class="error" role="alert">{{ passwordError }}</p>
      <div v-if="error?.code === 'CANNOT_DELETE_WHILE_PROCESSING'" class="notice" role="alert">
        <p>{{ error.message }}</p>
        <p>진행 중인 분석이 끝난 뒤 다시 시도해 주세요. 계정은 그대로 유지됩니다.</p>
        <router-link :to="{ name: 'recordingList' }" @click="close">기록 확인하기</router-link>
      </div>
      <p v-else-if="error && !passwordError" class="error" role="alert">{{ error.message }}</p>
      <div class="dialog__actions">
        <button type="button" class="secondary" :disabled="submitting" @click="close">취소</button>
        <button type="submit" class="danger" :disabled="submitting">
          {{ submitting ? '탈퇴 처리 중…' : '탈퇴하기' }}
        </button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.dialog { width: min(calc(100% - 32px), 460px); padding: 0; color: var(--color-text); border: 0; border-radius: var(--radius-2); box-shadow: 0 24px 80px rgba(20, 28, 48, 0.28); }
.dialog::backdrop { background: rgba(24, 32, 51, 0.55); }
.dialog__body { display: grid; gap: var(--space-4); padding: var(--space-6); }
.dialog h2 { margin: var(--space-1) 0; }
.dialog p { margin: 0; color: var(--color-text-muted); }
.dialog__eyebrow { color: var(--color-danger) !important; font-size: 0.78rem; font-weight: 800; }
label { display: grid; gap: var(--space-2); font-weight: 700; }
input { padding: 11px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-1); }
.error { color: var(--color-danger) !important; }
.notice { display: grid; gap: var(--space-2); padding: var(--space-3); background: var(--color-warning-weak); border-radius: var(--radius-1); }
.dialog__actions { display: flex; justify-content: end; gap: var(--space-2); }
.dialog__actions button { padding: 10px 14px; font-weight: 800; border-radius: var(--radius-1); }
.secondary { color: var(--color-text); background: white; border: 1px solid var(--color-border); }
.danger { color: white; background: var(--color-danger); border: 1px solid var(--color-danger); }
</style>
