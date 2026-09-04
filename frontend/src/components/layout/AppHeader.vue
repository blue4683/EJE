<script setup>
import { ref } from 'vue'
import { useSessionStore } from '@/stores/session'
import { useAuthActions } from '@/composables/useAuthActions'
import PlanBadge from './PlanBadge.vue'

const session = useSessionStore()
const { logout, submitting } = useAuthActions()
const menuOpen = ref(false)
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <router-link :to="{ name: 'home' }" class="brand"><span aria-hidden="true">🎙️</span> SpeechCoach</router-link>
      <nav id="main-navigation" class="nav" :class="{ 'nav--open': menuOpen }" aria-label="주요 메뉴">
        <router-link :to="{ name: 'home' }">Dashboard</router-link>
        <router-link :to="{ name: 'record' }">Practice</router-link>
        <router-link :to="{ name: 'recordingList' }">History</router-link>
        <router-link :to="{ name: 'weeklyReport' }">AI Coach</router-link>
      </nav>
      <button class="menu-toggle" type="button" :aria-expanded="menuOpen" aria-controls="main-navigation" aria-label="메뉴 열기" @click="menuOpen = !menuOpen">
        <span></span><span></span><span></span>
      </button>
      <div v-if="session.isAuthenticated" class="me">
        <PlanBadge :plan="session.user?.plan" />
        <router-link :to="{ name: 'me' }" class="me__name">{{ session.user?.name }}</router-link>
        <button type="button" :disabled="submitting" @click="logout">로그아웃</button>
      </div>
      <router-link v-else :to="{ name: 'login' }" class="login-link">Login</router-link>
    </div>
  </header>
</template>

<style scoped>
.header { position: sticky; z-index: 10; top: 0; background: rgba(255, 255, 255, 0.97); border-bottom: 1px solid var(--color-border); }
.header__inner { display: flex; width: min(100% - 96px, 1200px); min-height: 72px; align-items: center; justify-content: space-between; gap: var(--space-6); margin: 0 auto; }
.brand { color: var(--color-text); font-size: 1.2rem; font-weight: 900; text-decoration: none; letter-spacing: -0.04em; }
.brand span { margin-right: 5px; font-size: 1rem; }
.nav, .me { display: flex; align-items: center; gap: 36px; }
.me { gap: 18px; }
.nav a { color: #999; font-size: 1rem; font-weight: 600; text-decoration: none; white-space: nowrap; }
.me__name { color: #999; font-size: 1rem; font-weight: 600; text-decoration: none; white-space: nowrap; }
.nav a.router-link-active, .me__name.router-link-active { color: var(--color-text); }
.nav a.router-link-active { position: relative; }
.nav a.router-link-active::after { position: absolute; right: 0; bottom: -25px; left: 0; height: 3px; background: var(--color-text); content: ''; }
.login-link { padding: 7px 20px; color: var(--color-text); font-size: 0.9rem; font-weight: 700; text-decoration: none; border: 1px solid var(--color-border); border-radius: var(--radius-1); }
.lock { margin-left: 2px; padding: 2px 5px; color: var(--color-primary-strong); font-size: 0.62rem; background: var(--color-primary-weak); border-radius: 999px; }
.me button { padding: 7px 10px; color: var(--color-text-muted); background: transparent; border: 1px solid var(--color-border); border-radius: var(--radius-1); }
@media (max-width: 760px) { .header__inner { flex-wrap: wrap; padding: var(--space-3) 0; } .menu-toggle { display: inline-grid; order: 2; gap: 4px; width: 36px; height: 36px; place-content: center; padding: 7px; border: 1px solid var(--color-border); border-radius: 6px; background: white; }.menu-toggle span { display: block; width: 18px; height: 2px; background: var(--color-text); }.nav { display: none; order: 4; width: 100%; align-items: stretch; flex-direction: column; gap: 0; padding-top: var(--space-2); border-top: 1px solid var(--color-border); }.nav.nav--open { display: flex; }.nav a { padding: 10px 0; }.nav a.router-link-active::after { display: none; }.me { order: 5; }.nav:not(.nav--open) ~ .me { display: none; }.nav.nav--open ~ .me { display: flex; width: 100%; justify-content: space-between; padding-top: var(--space-3); border-top: 1px solid var(--color-border); } }
@media (min-width: 761px) { .menu-toggle { display: none; } }
@media (max-width: 480px) { .header__inner { width: min(100% - 48px, 1200px); } .me__name { display: none; } }
</style>
