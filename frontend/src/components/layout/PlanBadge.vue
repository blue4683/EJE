<script setup>
import { computed } from 'vue'
import { PLAN_LABEL } from '@/utils/format'

const props = defineProps({ plan: { type: String, default: 'FREE' } })
const label = computed(() => PLAN_LABEL[props.plan] ?? props.plan)
</script>

<template>
  <span :class="['badge', { 'badge--pro': plan === 'PRO' }]">{{ label }}</span>
</template>

<style scoped>
.badge { display: inline-flex; align-items: center; padding: 3px 8px; color: var(--color-text-muted); font-size: 0.72rem; font-weight: 800; background: var(--color-surface-soft); border: 1px solid var(--color-border); border-radius: 999px; }
.badge--pro { color: var(--color-primary-strong); background: var(--color-primary-weak); border-color: #cbd8ff; }
</style>
