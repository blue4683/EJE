<script setup>
import { useSessionStore } from '@/stores/session'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'

const session = useSessionStore()
const route = useRoute()
</script>

<template>
  <AppHeader />
  <main class="app-main" :class="{ 'app-main--page': route.name !== 'home' }">
    <router-view />
  </main>
</template>

<style scoped>
.app-main {
  width: min(100% - 96px, 1200px);
  margin: 0 auto;
  padding: var(--space-6) 0 var(--space-12);
}

.app-main--page {
  min-height: 100vh;
}

@media (max-width: 640px) {
  .app-main {
    width: min(100% - 48px, 1200px);
    padding-top: var(--space-6);
  }
}

@media (max-width: 640px) {
  .app-main--page {
    min-height: 100vh;
  }
}
</style>
