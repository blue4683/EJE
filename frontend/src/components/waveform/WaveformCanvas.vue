<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps({
  points: { type: Array, default: () => [] },
  height: { type: Number, default: 120 },
  cursorMs: { type: Number, default: null },
})

const canvas = ref(null)
let resizeObserver = null

function draw() {
  const element = canvas.value
  if (!element) return

  const devicePixelRatio = window.devicePixelRatio || 1
  const width = element.clientWidth
  const height = props.height
  element.width = Math.floor(width * devicePixelRatio)
  element.height = Math.floor(height * devicePixelRatio)

  const context = element.getContext('2d')
  context.setTransform(devicePixelRatio, 0, 0, devicePixelRatio, 0, 0)
  context.clearRect(0, 0, width, height)

  const pointCount = props.points.length
  if (!pointCount) return

  const styles = getComputedStyle(document.documentElement)
  const speechColor = styles.getPropertyValue('--color-speech').trim() || '#2f6df6'
  const silenceColor = styles.getPropertyValue('--color-silence').trim() || '#c9ced6'
  const textColor = styles.getPropertyValue('--color-text').trim() || '#1b1d21'
  const barWidth = width / pointCount
  const middle = height / 2

  props.points.forEach((point, index) => {
    context.fillStyle = point.type === 'SPEECH' ? speechColor : silenceColor
    const barHeight = Math.max(1, point.amplitude * (height - 4))
    context.fillRect(
      index * barWidth,
      middle - barHeight / 2,
      Math.max(1, barWidth - 0.5),
      barHeight,
    )
  })

  if (props.cursorMs != null && pointCount > 1) {
    const totalDuration = props.points[pointCount - 1].timeMs + 100
    const cursorPosition = (props.cursorMs / totalDuration) * width
    context.fillStyle = textColor
    context.fillRect(cursorPosition, 0, 1, height)
  }
}

onMounted(() => {
  draw()
  resizeObserver = new ResizeObserver(draw)
  resizeObserver.observe(canvas.value)
})
onUnmounted(() => resizeObserver?.disconnect())
watch(() => [props.points, props.cursorMs], draw, { deep: true })
</script>

<template>
  <canvas
    ref="canvas"
    class="waveform"
    :style="{ height: `${height}px` }"
    role="img"
    :aria-label="`음성 파형, ${points.length}개 구간`"
  />
</template>

<style scoped>
.waveform {
  display: block;
  width: 100%;
  background: var(--color-surface, var(--social-bg));
  border-radius: var(--radius-1, 6px);
}
</style>
