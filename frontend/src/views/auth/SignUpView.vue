<script setup>
import { computed, reactive } from 'vue'
import AuthField from '@/components/auth/AuthField.vue'
import { useAuthActions } from '@/composables/useAuthActions'
import { normalizeEmail, validateEmail, validateName, validatePassword } from '@/utils/validators'

const { signup, submitting, error } = useAuthActions()
const form = reactive({ name: '', email: '', password: '' })
const touched = reactive({ name: false, email: false, password: false })

const nameError = computed(() => (touched.name ? validateName(form.name) : null))
const emailError = computed(() => {
  if (error.value?.code === 'EMAIL_ALREADY_EXISTS') return error.value.message
  return touched.email ? validateEmail(form.email) : null
})
const passwordError = computed(() => (touched.password ? validatePassword(form.password) : null))
const serverError = computed(() =>
  error.value && error.value.code !== 'EMAIL_ALREADY_EXISTS' ? error.value.message : '',
)
const canSubmit = computed(
  () =>
    !submitting.value &&
    !validateName(form.name) &&
    !validateEmail(form.email) &&
    !validatePassword(form.password),
)

const onSubmit = () => {
  Object.keys(touched).forEach((key) => { touched[key] = true })
  if (!canSubmit.value) return
  signup({
    name: form.name.trim(),
    email: normalizeEmail(form.email),
    password: form.password,
  })
}
</script>

<template>
  <section class="auth-page">
    <form class="auth-card" @submit.prevent="onSubmit">
      <div>
        <p class="eyebrow">무료로 시작하기</p>
        <h1>회원가입</h1>
        <p class="description">연습 기록과 분석 결과를 안전하게 모아보세요.</p>
      </div>
      <AuthField v-model="form.name" label="이름" autocomplete="name" :error="nameError" @blur="touched.name = true" />
      <AuthField v-model="form.email" label="이메일" type="email" autocomplete="email" placeholder="name@example.com" :error="emailError" @blur="touched.email = true" />
      <AuthField v-model="form.password" label="비밀번호" type="password" autocomplete="new-password" :error="passwordError" @blur="touched.password = true" />
      <p class="hint">8~64자, UTF-8 기준 최대 72바이트</p>
      <p v-if="serverError" class="server-error" role="alert">{{ serverError }}</p>
      <button type="submit" class="primary-button" :disabled="!canSubmit">
        {{ submitting ? '가입하는 중…' : '무료로 가입하기' }}
      </button>
      <p class="auth-link">이미 계정이 있나요? <router-link :to="{ name: 'login' }">로그인</router-link></p>
    </form>
  </section>
</template>

<style scoped>
.auth-page { display: grid; min-height: calc(100vh - 96px); place-items: center; }
.auth-card { display: grid; width: min(100%, 480px); gap: var(--space-5); padding: var(--space-8); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-3); box-shadow: var(--shadow-card); }
.auth-card h1 { margin: var(--space-1) 0 0; font-size: 2rem; }
.eyebrow { margin: 0; color: var(--color-primary); font-size: 0.78rem; font-weight: 800; letter-spacing: 0.12em; }
.description, .hint, .auth-link { margin: 0; color: var(--color-text-muted); }
.hint { margin-top: calc(var(--space-3) * -1); font-size: 0.82rem; }
.auth-link { text-align: center; }
.primary-button { padding: 13px 18px; color: white; font-weight: 800; background: var(--color-primary); border: 0; border-radius: var(--radius-1); }
.server-error { margin: 0; padding: var(--space-3); color: var(--color-danger); background: var(--color-danger-weak); border-radius: var(--radius-1); }
</style>
