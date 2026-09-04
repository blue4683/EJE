<script setup>
import { computed, reactive } from 'vue'
import AuthField from '@/components/auth/AuthField.vue'
import { useAuthActions } from '@/composables/useAuthActions'
import { normalizeEmail, validateEmail, validatePassword } from '@/utils/validators'

const { login, submitting, error } = useAuthActions()
const form = reactive({ email: '', password: '' })
const touched = reactive({ email: false, password: false })

const emailError = computed(() => (touched.email ? validateEmail(form.email) : null))
const passwordError = computed(() => (touched.password ? validatePassword(form.password) : null))
const serverError = computed(() => error.value?.message ?? '')
const canSubmit = computed(
  () => !submitting.value && !validateEmail(form.email) && !validatePassword(form.password),
)

const onSubmit = () => {
  touched.email = true
  touched.password = true
  if (!canSubmit.value) return
  login({ email: normalizeEmail(form.email), password: form.password })
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-copy">
      <p class="eyebrow">SPEECH HABITS</p>
      <h1>말하기 습관을<br />기록하고 개선하세요.</h1>
      <p>한 번의 연습을 데이터로 남기고, 달라지는 흐름을 확인할 수 있습니다.</p>
    </div>

    <form class="auth-card" @submit.prevent="onSubmit">
      <div>
        <p class="eyebrow">다시 만나 반가워요</p>
        <h2>로그인</h2>
      </div>
      <AuthField
        v-model="form.email"
        label="이메일"
        type="email"
        autocomplete="email"
        placeholder="name@example.com"
        :error="emailError"
        @blur="touched.email = true"
      />
      <AuthField
        v-model="form.password"
        label="비밀번호"
        type="password"
        autocomplete="current-password"
        :error="passwordError"
        @blur="touched.password = true"
      />
      <p v-if="serverError" class="server-error" role="alert">{{ serverError }}</p>
      <button type="submit" class="primary-button" :disabled="!canSubmit">
        {{ submitting ? '로그인 중…' : '로그인' }}
      </button>
      <p class="auth-link">처음이신가요? <router-link :to="{ name: 'signup' }">회원가입</router-link></p>
    </form>
  </section>
</template>

<style scoped>
.auth-page { display: grid; min-height: calc(100vh - 96px); align-items: center; grid-template-columns: 1fr 440px; gap: var(--space-12); }
.auth-copy h1 { margin: var(--space-3) 0; font-size: clamp(2.3rem, 5vw, 4rem); line-height: 1.08; letter-spacing: -0.055em; }
.auth-copy > p:last-child { max-width: 480px; color: var(--color-text-muted); font-size: 1.05rem; }
.eyebrow { margin: 0; color: var(--color-primary); font-size: 0.78rem; font-weight: 800; letter-spacing: 0.12em; }
.auth-card { display: grid; gap: var(--space-5); padding: var(--space-8); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-3); box-shadow: var(--shadow-card); }
.auth-card h2 { margin: var(--space-1) 0 0; font-size: 1.8rem; }
.primary-button { width: 100%; padding: 13px 18px; color: white; font-weight: 800; background: var(--color-primary); border: 0; border-radius: var(--radius-1); }
.primary-button:hover:not(:disabled) { background: var(--color-primary-strong); }
.server-error { margin: 0; padding: var(--space-3); color: var(--color-danger); background: var(--color-danger-weak); border-radius: var(--radius-1); }
.auth-link { margin: 0; color: var(--color-text-muted); text-align: center; }
@media (max-width: 800px) { .auth-page { grid-template-columns: 1fr; gap: var(--space-8); } .auth-copy { display: none; } }
</style>
