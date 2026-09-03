<script setup>
defineProps({
  level: { type: Number, default: 0 },
  active: { type: Boolean, default: false },
})
</script>

<template>
  <div class="meter">
    <span class="meter__label">입력 음량</span>
    <progress :value="active ? level : 0" max="1">{{ Math.round(level * 100) }}%</progress>
  </div>
</template>

<style scoped>
.meter { display: grid; gap: 8px; width: min(100%, 360px); }
.meter__label { color: var(--color-text-muted, var(--text)); font-size: 0.875rem; }
progress { width: 100%; height: 12px; accent-color: var(--color-primary, var(--accent)); }
</style>
