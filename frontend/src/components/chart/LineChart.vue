<script setup>
import { computed } from 'vue'
import ChartAxis from './ChartAxis.vue'

const props = defineProps({
  points: { type: Array, required: true },
  height: { type: Number, default: 240 },
  ariaLabel: { type: String, default: '추이 그래프' },
})

const WIDTH = 720
const PADDING = { top: 18, right: 18, bottom: 34, left: 38 }
const values = computed(() => props.points.map((point) => point.value).filter((value) => value != null))
const maxY = computed(() => Math.max(1, ...values.value))
const x = (index) => PADDING.left + (index * (WIDTH - PADDING.left - PADDING.right)) / Math.max(1, props.points.length - 1)
const y = (value) => props.height - PADDING.bottom - (value / maxY.value) * (props.height - PADDING.top - PADDING.bottom)

const segments = computed(() => {
  const output = []
  let current = []
  props.points.forEach((point, index) => {
    if (point.value == null) {
      if (current.length > 1) output.push(current)
      current = []
      return
    }
    current.push([x(index), y(point.value)])
  })
  if (current.length > 1) output.push(current)
  return output.map((segment) => segment.map(([px, py]) => `${px},${py}`).join(' '))
})

const dots = computed(() =>
  props.points
    .map((point, index) => point.value == null ? null : { ...point, cx: x(index), cy: y(point.value) })
    .filter(Boolean),
)

const tickStep = computed(() => Math.max(1, Math.ceil(props.points.length / 8)))
const ticks = computed(() => props.points
  .map((point, index) => ({ ...point, index, x: x(index) }))
  .filter((point) => point.index === 0 || point.index === props.points.length - 1 || point.index % tickStep.value === 0))
</script>

<template>
  <div class="chart-wrap">
    <svg class="chart" :viewBox="`0 0 ${WIDTH} ${height}`" role="img" :aria-label="`${ariaLabel}, ${points.length}일`">
      <ChartAxis :left="PADDING.left" :right="WIDTH - PADDING.right" :baseline="height - PADDING.bottom" />
      <text :x="PADDING.left - 7" :y="PADDING.top + 4" text-anchor="end" class="value-label">{{ maxY }}</text>
      <text :x="PADDING.left - 7" :y="height - PADDING.bottom + 4" text-anchor="end" class="value-label">0</text>
      <polyline v-for="(pointsString, index) in segments" :key="index" :points="pointsString" class="line" />
      <circle v-for="(dot, index) in dots" :key="`${dot.date}-${index}`" :cx="dot.cx" :cy="dot.cy" r="4" class="dot">
        <title>{{ dot.date }} · {{ dot.value }}</title>
      </circle>
      <text v-for="tick in ticks" :key="tick.date" :x="tick.x" :y="height - 10" text-anchor="middle" :class="['tick', { 'tick--future': tick.future }]">{{ tick.label }}</text>
    </svg>
  </div>
</template>

<style scoped>
.chart-wrap { overflow-x: auto; }
.chart { display: block; width: 100%; min-width: 560px; height: auto; }
.line { fill: none; stroke: var(--color-primary); stroke-linecap: round; stroke-linejoin: round; stroke-width: 3; }
.dot { fill: var(--color-surface); stroke: var(--color-primary); stroke-width: 3; }
.tick, .value-label { fill: var(--color-text-muted); font-size: 10px; }
.tick--future { opacity: 0.42; }
</style>
