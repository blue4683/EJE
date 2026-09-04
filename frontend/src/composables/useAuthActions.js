import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logIn, logOut, signUp } from '@/api/auth'
import { useSessionStore } from '@/stores/session'

function safeRedirect(value) {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
    ? value
    : null
}

export function useAuthActions() {
  const router = useRouter()
  const route = useRoute()
  const session = useSessionStore()
  const submitting = ref(false)
  const error = ref(null)

  const goAfterAuth = () => {
    const redirect = safeRedirect(route.query.redirect)
    return router.replace(redirect ?? { name: 'home' })
  }

  const run = async (request) => {
    submitting.value = true
    error.value = null
    try {
      const data = await request()
      session.setSession({ accessToken: data.accessToken, user: data.user })
      await goAfterAuth()
      return data
    } catch (caught) {
      error.value = caught
      return null
    } finally {
      submitting.value = false
    }
  }

  return {
    submitting,
    error,
    login: (payload) => run(() => logIn(payload)),
    signup: (payload) => run(() => signUp(payload)),
    logout: async () => {
      try {
        await logOut()
      } catch {
        // 서버 응답과 무관하게 브라우저의 세션은 종료한다.
      } finally {
        session.clear()
        await router.replace({ name: 'login' })
      }
    },
  }
}
