<script setup>
import { formatCount } from '@/utils/format'

defineProps({ items: { type: Array, default: () => [] } })
</script>

<template>
  <div class="breakdown">
    <div v-for="item in items" :key="item.word" class="breakdown__row">
      <span>“{{ item.word }}”</span>
      <strong>{{ formatCount(item.count) }}</strong>
    </div>
  </div>
</template>

<style scoped>
.breakdown { display: grid; border-top: 1px solid var(--color-border); }
.breakdown__row { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); padding: var(--space-3) 0; border-bottom: 1px solid var(--color-border); }
</style>
