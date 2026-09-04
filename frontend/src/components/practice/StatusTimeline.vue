<script setup>
const props = defineProps({
  status: { type: String, required: true },
})

const steps = [
  { status: 'PENDING', label: '대기 중' },
  { status: 'PROCESSING', label: '분석 중' },
  { status: 'COMPLETED', label: '완료' },
]

const order = { PENDING: 0, PROCESSING: 1, COMPLETED: 2, FAILED: 1 }
const isReached = (status) => order[props.status] >= order[status]
</script>

<template>
  <ol class="timeline" aria-label="분석 진행 단계">
    <li
      v-for="step in steps"
      :key="step.status"
      :class="{ reached: isReached(step.status), failed: status === 'FAILED' && step.status === 'PROCESSING' }"
    >
      <span class="dot" aria-hidden="true" />
      <span>{{ status === 'FAILED' && step.status === 'PROCESSING' ? '분석 실패' : step.label }}</span>
    </li>
  </ol>
</template>

<style scoped>
.timeline { display: grid; grid-template-columns: repeat(3, 1fr); padding: 0; list-style: none; }
.timeline li { display: grid; place-items: center; gap: var(--space-2); color: var(--color-text-muted); }
.dot { width: 14px; height: 14px; border: 2px solid var(--color-border); border-radius: 50%; }
.reached { color: var(--color-primary); }
.reached .dot { border-color: var(--color-primary); background: var(--color-primary); }
.failed { color: var(--color-danger); }
.failed .dot { border-color: var(--color-danger); background: var(--color-danger); }
</style>
