<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <header class="page-header">
    <h1>{{ title }}</h1>
    <p v-if="description">{{ description }}</p>
    <slot name="actions" />
  </header>
</template>

<style scoped>
.page-header { margin-bottom: var(--space-6); }
.page-header h1 { margin: 0 0 var(--space-2); font-size: 1.5rem; }
.page-header p { margin: 0; color: var(--color-text-muted); }
</style>
