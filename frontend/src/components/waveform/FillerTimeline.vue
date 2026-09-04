<script setup>
const props = defineProps({
  events: { type: Array, default: () => [] },
  durationMs: { type: Number, required: true },
})

const left = (milliseconds) => {
  if (props.durationMs <= 0) return '0%'
  return `${(milliseconds / props.durationMs) * 100}%`
}
</script>

<template>
  <div class="timeline" role="img" :aria-label="`추임새 ${events.length}회`">
    <span
      v-for="event in events"
      :key="event.eventIndex"
      class="marker"
      :style="{ left: left(event.timeMs) }"
      :title="`${event.word} · ${(event.timeMs / 1000).toFixed(1)}초`"
    >
      <span class="dot" aria-hidden="true" />
      <span class="word">{{ event.word }}</span>
    </span>
  </div>
</template>

<style scoped>
.timeline { position: relative; height: 52px; border-bottom: 2px solid var(--color-border); }
.marker { position: absolute; bottom: -7px; transform: translateX(-50%); }
.dot { display: block; width: 12px; height: 12px; margin: 0 auto;
  background: var(--color-warning); border-radius: 50%; }
.word { display: none; position: absolute; bottom: 18px; left: 50%; padding: var(--space-1) var(--space-2);
  color: var(--color-text); background: var(--color-surface); border-radius: var(--radius-1);
  white-space: nowrap; transform: translateX(-50%); }
.marker:hover .word { display: block; }
</style>
