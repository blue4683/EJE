<script setup>
import { useSessionStore } from '@/stores/session'
import { useAuthActions } from '@/composables/useAuthActions'
import PlanBadge from './PlanBadge.vue'

const session = useSessionStore()
const { logout, submitting } = useAuthActions()
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <router-link :to="{ name: 'home' }" class="brand">말습관</router-link>
      <nav class="nav" aria-label="주요 메뉴">
        <router-link :to="{ name: 'record' }">연습하기</router-link>
        <router-link :to="{ name: 'recordingList' }">기록</router-link>
        <router-link :to="{ name: 'trends' }">추이</router-link>
        <router-link :to="{ name: 'weeklyReport' }">
          주간 리포트 <span v-if="!session.isPro" class="lock" aria-label="PRO 전용">PRO</span>
        </router-link>
      </nav>
      <div class="me">
        <PlanBadge :plan="session.user?.plan" />
        <router-link :to="{ name: 'me' }" class="me__name">{{ session.user?.name }}</router-link>
        <button type="button" :disabled="submitting" @click="logout">로그아웃</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header { position: sticky; z-index: 10; top: 0; background: rgba(255, 255, 255, 0.94); border-bottom: 1px solid var(--color-border); backdrop-filter: blur(12px); }
.header__inner { display: flex; width: min(100% - 32px, 1080px); min-height: 64px; align-items: center; justify-content: space-between; gap: var(--space-5); margin: 0 auto; }
.brand { color: var(--color-text); font-size: 1.2rem; font-weight: 900; text-decoration: none; letter-spacing: -0.04em; }
.nav, .me { display: flex; align-items: center; gap: var(--space-4); }
.nav a, .me__name { color: var(--color-text-muted); font-size: 0.9rem; font-weight: 700; text-decoration: none; white-space: nowrap; }
.nav a.router-link-active, .me__name.router-link-active { color: var(--color-primary); }
.lock { margin-left: 2px; padding: 2px 5px; color: var(--color-primary-strong); font-size: 0.62rem; background: var(--color-primary-weak); border-radius: 999px; }
.me button { padding: 7px 10px; color: var(--color-text-muted); background: transparent; border: 1px solid var(--color-border); border-radius: var(--radius-1); }
@media (max-width: 820px) { .header__inner { flex-wrap: wrap; padding: var(--space-3) 0; } .nav { order: 3; width: 100%; justify-content: space-between; overflow-x: auto; } }
@media (max-width: 480px) { .header__inner { width: min(100% - 24px, 1080px); } .me__name { display: none; } }
</style>
