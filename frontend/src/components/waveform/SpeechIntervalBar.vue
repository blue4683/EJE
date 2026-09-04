<script setup>
const props = defineProps({
  intervals: { type: Array, default: () => [] },
  durationMs: { type: Number, required: true },
})

const percent = (value) => {
  if (props.durationMs <= 0) return '0%'
  return `${(value / props.durationMs) * 100}%`
}
</script>

<template>
  <div class="track" role="img" aria-label="발화 구간">
    <span
      v-for="(interval, index) in intervals"
      :key="index"
      class="segment"
      :style="{
        left: percent(interval.startMs),
        width: percent(interval.endMs - interval.startMs),
      }"
    />
  </div>
</template>

<style scoped>
.track {
  position: relative;
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-silence, #c9ced6);
}
.segment {
  position: absolute;
  inset-block: 0;
  background: var(--color-speech, #2f6df6);
}
</style>
