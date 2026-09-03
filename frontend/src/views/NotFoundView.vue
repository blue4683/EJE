<template>
  <div class="not-found">
    <h1>페이지를 찾을 수 없습니다.</h1>
    <router-link :to="{ name: 'home' }">홈으로 가기</router-link>
  </div>
</template>

<style scoped>
.not-found { padding: var(--space-8); text-align: center; }
</style>
