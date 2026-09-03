<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="not-ready">
    <h1>준비 중인 화면입니다.</h1>
    <p>이 경로(<code>{{ route.path }}</code>)의 화면은 아직 만들어지지 않았습니다.</p>
    <router-link :to="{ name: 'home' }">홈으로 가기</router-link>
  </div>
</template>

<style scoped>
.not-ready { padding: var(--space-8); color: var(--color-text-muted); text-align: center; }
</style>
