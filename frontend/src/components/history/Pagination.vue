<script setup>
defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalElements: { type: Number, required: true },
  canPrev: { type: Boolean, required: true },
  canNext: { type: Boolean, required: true },
})

defineEmits(['change'])
</script>

<template>
  <nav v-if="totalPages > 0" class="pagination" aria-label="기록 페이지">
    <button type="button" :disabled="!canPrev" @click="$emit('change', page - 1)">이전</button>
    <span><strong>{{ page + 1 }}</strong> / {{ totalPages }} 페이지 · 총 {{ totalElements }}건</span>
    <button type="button" :disabled="!canNext" @click="$emit('change', page + 1)">다음</button>
  </nav>
</template>

<style scoped>
.pagination { display: flex; align-items: center; justify-content: center; gap: var(--space-4); margin-top: var(--space-6); color: var(--color-text-muted); }
.pagination button { padding: 8px 13px; color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-1); }
@media (max-width: 460px) { .pagination { gap: var(--space-2); font-size: 0.84rem; } }
</style>
