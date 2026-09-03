import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
  state: () => ({
    accessToken: null,
    user: null,
    bootstrapped: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.user),
    isPro: (state) => state.user?.plan === 'PRO',
  },
  actions: {
    setSession({ accessToken, user }) {
      this.accessToken = accessToken
      this.user = user
    },
    setAccessToken(accessToken) {
      this.accessToken = accessToken
    },
    setUser(user) {
      this.user = user
    },
    clear() {
      this.accessToken = null
      this.user = null
    },
    markBootstrapped() {
      this.bootstrapped = true
    },
  },
})
