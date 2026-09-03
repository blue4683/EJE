<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <header class="page-header">
    <div>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.page-header h1 {
  margin: 0;
  font-size: clamp(1.45rem, 3vw, 2rem);
  line-height: 1.2;
  letter-spacing: -0.035em;
}

.page-header p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-muted);
}

.page-header__actions {
  flex: none;
}

@media (max-width: 640px) {
  .page-header {
    align-items: start;
    flex-direction: column;
  }
}
</style>
