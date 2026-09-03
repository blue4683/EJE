<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="message-page">
    <p class="message-page__eyebrow">준비 중</p>
    <h1>아직 준비 중인 화면입니다.</h1>
    <p><code>{{ route.path }}</code> 화면을 구현하고 있습니다.</p>
    <router-link :to="{ name: 'home' }">홈으로 가기</router-link>
  </section>
</template>

<style scoped>
.message-page {
  padding: var(--space-12) var(--space-6);
  color: var(--color-text-muted);
  text-align: center;
  background: var(--color-surface);
  border-radius: var(--radius-3);
}
.message-page__eyebrow { color: var(--color-primary); font-weight: 800; }
.message-page h1 { color: var(--color-text); }
</style>
