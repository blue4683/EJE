<template>
  <section class="message-page">
    <p class="message-page__code">404</p>
    <h1>페이지를 찾을 수 없습니다.</h1>
    <p>주소가 올바른지 확인해 주세요.</p>
    <router-link :to="{ name: 'home' }">홈으로 가기</router-link>
  </section>
</template>

<style scoped>
.message-page {
  padding: var(--space-12) var(--space-6);
  text-align: center;
  background: var(--color-surface);
  border-radius: var(--radius-3);
}
.message-page__code { margin: 0; color: var(--color-primary); font-weight: 800; }
.message-page h1 { margin: var(--space-2) 0; }
.message-page p:not(.message-page__code) { color: var(--color-text-muted); }
</style>
