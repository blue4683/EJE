<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchComparison } from '@/api/stats'
import { formatCount, formatDateTime, formatMs, formatNumber } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import DeltaBadge from '@/components/stats/DeltaBadge.vue'

const props = defineProps({ recordingId: { type: String, required: true } })
const router = useRouter()
const data = ref(null)
const state = ref('loading')
const error = ref(null)

async function load() {
  state.value = 'loading'
  error.value = null
  try {
    data.value = await fetchComparison(props.recordingId)
    state.value = 'ready'
  } catch (caught) {
    error.value = caught
    if (caught.code === 'COMPARISON_TARGET_NOT_FOUND') state.value = 'empty'
    else if (caught.code === 'PRO_REQUIRED') await router.replace({ name: 'upgrade' })
    else state.value = 'error'
  }
}

onMounted(load)
</script>

<template>
  <PageHeader title="이전 기록과 비교" description="현재 기록에서 실제로 달라진 값을 그대로 보여드립니다.">
    <template #actions><router-link :to="{ name: 'recordingDetail', params: { recordingId } }">상세로 돌아가기</router-link></template>
  </PageHeader>
  <StateBlock :state="state" :message="error?.message" @retry="load">
    <template #empty-action><router-link :to="{ name: 'record' }">다음 비교를 위한 연습 시작하기</router-link></template>
    <template v-if="data">
      <div class="comparison-head"><span>{{ formatDateTime(data.target.submittedAt) }}</span><strong>→</strong><span>{{ formatDateTime(data.current.submittedAt) }}</span></div>
      <section class="comparison-grid">
        <article><span>추임새</span><div><small>{{ formatCount(data.target.fillerTotalCount) }}</small><strong>{{ formatCount(data.current.fillerTotalCount) }}</strong></div><DeltaBadge :value="data.delta.fillerCountChange" unit="회" /></article>
        <article><span>침묵 시간</span><div><small>{{ formatMs(data.target.silenceDurationMs) }}</small><strong>{{ formatMs(data.current.silenceDurationMs) }}</strong></div><DeltaBadge :value="data.delta.silenceDurationMsChange" unit="ms" /></article>
        <article><span>분당 단어 수</span><div><small>{{ formatNumber(data.target.wordsPerMinute) }}</small><strong>{{ formatNumber(data.current.wordsPerMinute) }}</strong></div><DeltaBadge :value="data.delta.wordsPerMinuteChange" unit="WPM" neutral /></article>
      </section>
      <p class="duration-note">연습 길이: 이전 {{ formatMs(data.target.durationMs) }} · 현재 {{ formatMs(data.current.durationMs) }}. 길이가 다르면 절대 변화량에 영향을 줄 수 있습니다.</p>
    </template>
  </StateBlock>
</template>

<style scoped>
.comparison-head { display: grid; grid-template-columns: 1fr auto 1fr; gap: var(--space-4); margin-bottom: var(--space-4); color: var(--color-text-muted); text-align: center; }
.comparison-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); }
.comparison-grid article { display: grid; gap: var(--space-4); padding: var(--space-5); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-2); }
.comparison-grid article > span { color: var(--color-text-muted); font-size: 0.84rem; }
.comparison-grid div { display: flex; align-items: baseline; gap: var(--space-3); }
.comparison-grid small { color: var(--color-text-muted); text-decoration: line-through; }
.comparison-grid strong { font-size: 1.4rem; }
.duration-note { padding: var(--space-4); color: var(--color-text-muted); background: var(--color-warning-weak); border-radius: var(--radius-1); }
@media (max-width: 720px) { .comparison-grid { grid-template-columns: 1fr; } }
</style>
