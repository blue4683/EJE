<script setup>
import { useSessionStore } from '@/stores/session'
import AppHeader from '@/components/layout/AppHeader.vue'

const session = useSessionStore()
</script>

<template>
  <AppHeader />
  <main class="app-main">
    <router-view />
  </main>
</template>

<style scoped>
.app-main {
  width: min(100% - 48px, 1280px);
  margin: 0 auto;
  padding: var(--space-6) 0 var(--space-12);
}

@media (max-width: 640px) {
  .app-main {
    width: min(100% - 24px, 1280px);
    padding-top: var(--space-6);
  }
}
</style>
