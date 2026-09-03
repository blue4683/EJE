<script setup>
import { computed } from 'vue'
import { STATUS_LABEL } from '@/utils/format'

const props = defineProps({ status: { type: String, required: true } })
const label = computed(() => STATUS_LABEL[props.status] ?? props.status)
</script>

<template>
  <span :class="['chip', `chip--${status.toLowerCase()}`]">{{ label }}</span>
</template>

<style scoped>
.chip { display: inline-flex; padding: 4px 9px; color: var(--color-text-muted); font-size: 0.76rem; font-weight: 800; background: var(--color-surface-soft); border: 1px solid var(--color-border); border-radius: 999px; }
.chip--completed { color: var(--color-success); background: var(--color-success-weak); border-color: #bfe6d3; }
.chip--pending, .chip--processing { color: var(--color-primary-strong); background: var(--color-primary-weak); border-color: #cbd8ff; }
.chip--failed { color: var(--color-danger); background: var(--color-danger-weak); border-color: #efcaca; }
</style>
