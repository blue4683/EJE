<script setup>
defineProps({
  left: { type: Number, required: true },
  right: { type: Number, required: true },
  baseline: { type: Number, required: true },
})
</script>

<template>
  <g aria-hidden="true">
    <line :x1="left" :y1="baseline" :x2="right" :y2="baseline" class="axis" />
  </g>
</template>

<style scoped>
.axis { stroke: var(--color-border); stroke-width: 1; }
</style>
