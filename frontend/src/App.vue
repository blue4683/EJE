<script setup>
import { useSessionStore } from '@/stores/session'
import AppHeader from '@/components/layout/AppHeader.vue'

const session = useSessionStore()
</script>

<template>
  <AppHeader v-if="session.isAuthenticated" />
  <main class="app-main">
    <router-view />
  </main>
</template>

<style scoped>
.app-main {
  width: min(100% - 32px, 1080px);
  margin: 0 auto;
  padding: var(--space-8) 0 var(--space-12);
}

@media (max-width: 640px) {
  .app-main {
    width: min(100% - 24px, 1080px);
    padding-top: var(--space-6);
  }
}
</style>
