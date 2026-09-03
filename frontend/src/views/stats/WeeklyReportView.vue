<script setup>
import PageHeader from '@/components/common/PageHeader.vue'
</script>

<template>
  <section class="coming-soon">
    <PageHeader title="AI Coach" description="추후 확장 예정입니다." />
    <div class="coming-soon__panel">
      <span class="coming-soon__icon" aria-hidden="true">🔒</span>
      <h2>추후 확장 예정입니다</h2>
      <p>말하기 습관을 바탕으로 한 AI 코칭 기능을 준비하고 있습니다.</p>
      <router-link :to="{ name: 'home' }">Dashboard로 돌아가기</router-link>
    </div>
  </section>
</template>

<style scoped>
.coming-soon { display: grid; gap: var(--space-3); }.coming-soon__panel { display: grid; min-height: 360px; place-content: center; justify-items: center; gap: var(--space-3); padding: var(--space-8); text-align: center; border: 1px solid var(--color-border); border-radius: 7px; background: var(--color-surface); }.coming-soon__icon { font-size: 2.4rem; filter: grayscale(1); }.coming-soon__panel h2 { margin: 0; font-size: 1.25rem; }.coming-soon__panel p { margin: 0 0 var(--space-3); color: var(--color-text-muted); }.coming-soon__panel a { padding: 10px 16px; color: #fff; font-weight: 700; text-decoration: none; border-radius: 6px; background: #222; }
</style>
