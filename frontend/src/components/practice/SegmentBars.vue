<script setup>
import { computed } from 'vue'

const props = defineProps({
  segments: { type: Array, default: () => [] },
})

const labels = { INITIAL: '초반', MIDDLE: '중반', FINAL: '후반' }
const maxCount = computed(() => Math.max(1, ...props.segments.map((item) => item.fillerCount)))
const width = (count) => `${(count / maxCount.value) * 100}%`
</script>

<template>
  <div class="segments">
    <div v-for="segment in segments" :key="segment.segment" class="segment">
      <span>{{ labels[segment.segment] || segment.segment }}</span>
      <div class="track"><span class="bar" :style="{ width: width(segment.fillerCount) }" /></div>
      <strong>{{ segment.fillerCount }}회</strong>
    </div>
  </div>
</template>

<style scoped>
.segments { display: grid; gap: var(--space-3); }
.segment { display: grid; grid-template-columns: 48px 1fr 48px; align-items: center; gap: var(--space-3); }
.track { height: 12px; overflow: hidden; background: var(--color-border); border-radius: 999px; }
.bar { display: block; height: 100%; background: var(--color-primary); border-radius: inherit; }
.segment strong { text-align: right; }
</style>
