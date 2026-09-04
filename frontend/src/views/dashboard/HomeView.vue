<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchRecentAnalyses } from '@/api/history'
import { fetchProAnalysis } from '@/api/proAnalysis'
import { formatDateTime, formatMs } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const items = ref([])
const state = ref('loading')
const error = ref(null)
const isGuest = computed(() => !session.isAuthenticated)
const recentRows = computed(() => [items.value[0] ?? null, items.value[1] ?? null])
const latestFillerCount = computed(() => items.value[0]?.fillerTotalCount ?? null)
const proMetricLabels = ['말하기 속도', '침묵 구간', '맞춤 코칭']
const proMetrics = ref(null)
const proMetricCards = computed(() => [
  { label: '최근 분석의 추임새', value: latestFillerCount.value, unit: '회' },
  {
    label: '말하기 속도',
    value: proMetrics.value?.speechRate?.wordsPerMinute ?? null,
    unit: 'WPM',
  },
  {
    label: '침묵 시간',
    value: proMetrics.value ? formatMs(proMetrics.value.silenceDurationMs) : null,
    unit: '',
  },
  {
    label: '맞춤 코칭',
    value: proMetrics.value?.coaching?.actionItems?.length ?? null,
    unit: '개',
  },
])

async function load() {
  state.value = 'loading'
  error.value = null
  proMetrics.value = null
  try {
    const recent = await fetchRecentAnalyses()
    items.value = recent.items
    if (session.isPro && recent.items[0]) {
      proMetrics.value = (await fetchProAnalysis(recent.items[0].recordingId)).metrics
    }
    state.value = recent.items.length ? 'ready' : 'empty'
  } catch (caught) {
    error.value = caught
    state.value = 'error'
  }
}

onMounted(() => {
  if (session.isAuthenticated) load()
  else state.value = 'empty'
})
</script>

<template>
  <section class="intro">
    <div>
      <h1>{{ isGuest ? '나의 말하기를 기록하고 개선해보세요' : `${session.user?.name}님, 한 번 더 말해볼까요?` }}</h1>
      <p>{{ isGuest ? '1분 자기소개부터 시작해 나도 몰랐던 말하기 습관을 확인해보세요.' : '짧게라도 꾸준히 기록하면 변화가 선명해집니다.' }}</p>
    </div>
  </section>

  <section class="practice-card" aria-labelledby="practice-title">
    <div>
      <p class="eyebrow">INLINE RECORDING</p>
      <h2 id="practice-title"><span aria-hidden="true">🎙️</span> 1분 자기소개</h2>
      <p>1분 동안 자유롭게 자신을 소개해보세요.</p>
      <router-link :to="{ name: 'record' }" class="record-button"><span aria-hidden="true">●</span> 녹음 시작</router-link>
    </div>
  </section>

  <section class="dashboard" aria-labelledby="dashboard-title">
    <div class="section-heading"><h2 id="dashboard-title">나의 말하기 Dashboard</h2><span v-if="isGuest">로그인 후 제공</span></div>
    <div v-if="isGuest" class="guest-lock">
      <div class="placeholder-chart" aria-hidden="true"></div>
      <div class="guest-lock__content"><h3>나의 말하기 변화를 기록해보세요</h3><p>로그인하면 연습 횟수와 최근 분석 기록을 확인할 수 있습니다.</p><router-link :to="{ name: 'login' }">로그인 →</router-link></div>
    </div>
    <div v-if="session.isAuthenticated" class="metric-grid" aria-label="말하기 지표">
      <template v-if="session.isPro">
        <article v-for="card in proMetricCards" :key="card.label" class="metric-card metric-card--basic">
          <span>{{ card.label }}</span>
          <strong>{{ card.value ?? '—' }} <small v-if="card.value != null && card.unit">{{ card.unit }}</small></strong>
        </article>
      </template>
      <template v-else>
        <article class="metric-card metric-card--basic">
          <span>최근 분석의 추임새</span>
          <strong>{{ latestFillerCount ?? '—' }} <small v-if="latestFillerCount != null">회</small></strong>
        </article>
        <router-link
          v-for="label in proMetricLabels"
          :key="label"
          :to="{ name: 'upgrade' }"
          class="metric-card metric-card--locked"
        >
          <span>{{ label }}</span>
          <span class="metric-card__lock" aria-label="PRO 전용">🔒</span>
          <small>PRO 상세 분석</small>
        </router-link>
      </template>
    </div>
      <template v-if="session.isAuthenticated">
      <div class="recent-heading">
        <h2>최근 분석</h2>
        <router-link :to="{ name: 'recordingList' }">전체 기록 보기</router-link>
      </div>
      <StateBlock v-if="state === 'loading'" state="loading" />
      <StateBlock v-else-if="state === 'error'" :message="error?.message" state="error" @retry="load" />
      <div v-else class="recent-list" aria-label="최근 분석 2건">
        <template v-for="(item, index) in recentRows" :key="item?.recordingId ?? `empty-${index}`">
          <router-link v-if="item" :to="{ name: 'recordingDetail', params: { recordingId: item.recordingId } }" class="recent-row">
            <span>{{ formatDateTime(item.submittedAt) }}</span><strong>1분 자기소개</strong><span>{{ formatMs(item.durationMs) }}</span><span>{{ item.status }}</span><span class="recent-row__link">상세보기 →</span>
          </router-link>
          <div v-else class="recent-row recent-row--empty" aria-label="아직 분석 기록이 없습니다"><span></span><span></span><span></span></div>
        </template>
      </div>
    </template>
    <div v-if="isGuest" class="guest-recent"><h2>최근 분석</h2><p><span aria-hidden="true">🔒</span> 로그인 후 기록을 확인할 수 있어요 <router-link :to="{ name: 'login' }">로그인</router-link></p></div>
  </section>
</template>

<style scoped>
.intro { padding: var(--space-6) 0; border-bottom: 1px solid var(--color-border); }
.intro h1 { margin: 0 0 var(--space-2); font-size: clamp(1.2rem, 1.8vw, 1.5rem); line-height: 1.3; letter-spacing: -0.04em; }
.intro p { margin: 0; color: var(--color-text-muted); font-size: .9rem; }
.practice-card { display: flex; align-items: flex-start; justify-content: space-between; min-height: 250px; gap: var(--space-6); margin: var(--space-6) 0; padding: 24px; border: 3px solid #3b3b3b; border-radius: var(--radius-1); background: var(--color-surface); }
.eyebrow { display: inline-block; margin: 0 0 var(--space-3); padding: 5px 12px; color: #555; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.76rem; font-weight: 800; letter-spacing: 0.08em; background: #e8e8e8; border-radius: 6px; }
.practice-card h2 { margin: 0; font-size: 1.4rem; letter-spacing: -0.03em; }.practice-card h2 span { margin-right: 6px; }.practice-card > div > p:not(.eyebrow) { margin: var(--space-3) 0 var(--space-5); color: var(--color-text-muted); }
.record-button { display: inline-flex; align-items: center; gap: 8px; padding: 13px 22px; color: white; font-weight: 800; text-decoration: none; background: #222; border-radius: 7px; }.record-button span { color: white; font-size: 0.85rem; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }.section-heading h2, .guest-recent h2 { margin: 0; font-size: 1.1rem; letter-spacing: -0.03em; }.section-heading span { padding: 5px 12px; color: #666; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.72rem; font-weight: 700; background: #e8e8e8; border-radius: 6px; }
.recent-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin: var(--space-3) 0; }.recent-heading h2 { margin: 0; font-size: 1.1rem; letter-spacing: -0.03em; }.recent-heading a { color: var(--color-text-muted); font-size: .86rem; font-weight: 700; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-3); margin: var(--space-4) 0 var(--space-5); }.metric-card { position: relative; display: grid; gap: var(--space-2); min-height: 116px; padding: var(--space-5); color: var(--color-text); text-decoration: none; border: 1px solid #cfd3da; border-radius: 7px; background: var(--color-surface); }.metric-card > span:first-child { color: var(--color-text-muted); font-size: 0.82rem; }.metric-card strong { font-size: 2rem; line-height: 1; }.metric-card small { color: var(--color-text-muted); font-size: 0.8rem; font-weight: 600; }.metric-card--locked > span:first-child, .metric-card--locked strong { filter: blur(5px); opacity: .38; user-select: none; }.metric-card--locked::after { position: absolute; inset: 0; border-radius: inherit; background: rgba(255,255,255,.2); content: ''; pointer-events: none; }.metric-card__lock { position: absolute; top: 50%; left: 50%; z-index: 1; color: #777; font-size: 1.45rem; line-height: 1; transform: translate(-50%, -50%); }
.guest-lock { position: relative; display: grid; min-height: 210px; place-items: center; overflow: hidden; border: 1px dashed #b9bdc4; border-radius: var(--radius-1); background: var(--color-surface); }.placeholder-chart { position: absolute; inset: 28px 22px; opacity: 0.18; background: linear-gradient(168deg, transparent 48%, #d5d8de 49% 51%, transparent 52%) 0 30% / 28% 60% repeat-x, linear-gradient(#e1e3e7 1px, transparent 1px) 0 0 / 100% 35%; }.guest-lock__content { position: relative; padding: 20px 28px; text-align: center; background: rgba(255,255,255,.88); }.guest-lock__content h3 { margin: 0; font-size: 1.1rem; }.guest-lock__content p { margin: var(--space-2) 0 var(--space-4); color: var(--color-text-muted); }.guest-lock__content a { display: inline-block; padding: 10px 18px; color: white; font-size: .9rem; font-weight: 800; text-decoration: none; background: #222; border-radius: 6px; }
.guest-recent { margin-top: var(--space-8); }.guest-recent p { margin: var(--space-3) 0 0; padding: 28px; color: var(--color-text-muted); text-align: center; border: 1px solid var(--color-border); border-radius: var(--radius-1); background: var(--color-surface); }.guest-recent a { display: inline-block; margin-left: var(--space-3); padding: 7px 14px; color: var(--color-text); font-weight: 700; text-decoration: none; border: 1px solid #bbb; border-radius: 6px; }
.recent-list { display: grid; overflow: hidden; border: 1px solid var(--color-border); border-radius: 7px; background: var(--color-surface); }.recent-row { display: grid; grid-template-columns: 110px 1fr 90px 120px 110px; align-items: center; gap: var(--space-4); min-height: 76px; padding: 0 var(--space-5); color: var(--color-text); text-decoration: none; border-bottom: 1px solid #e4e4e4; }.recent-row:last-child { border-bottom: 0; }.recent-row > span { color: var(--color-text-muted); font-size: .86rem; }.recent-row strong { font-size: .96rem; }.recent-row__link { text-align: right; text-decoration: underline; }.recent-row--empty { background: repeating-linear-gradient(135deg, #fff, #fff 10px, #fafafa 10px, #fafafa 20px); }.recent-row--empty span { display: block; height: 10px; border-radius: 3px; background: #f0f0f0; }
@media (max-width: 760px) { .practice-card { min-height: 0; padding: var(--space-5); }.metric-grid { grid-template-columns: repeat(2, 1fr); }.recent-row { grid-template-columns: 1fr auto; gap: var(--space-2); padding: var(--space-4); }.recent-row > span:nth-of-type(2), .recent-row > span:nth-of-type(3) { display: none; }.recent-row strong { grid-row: 1; }.recent-row__link { grid-row: 2; grid-column: 2; } }
@media (max-width: 480px) { .practice-card { flex-direction: column; }.section-heading { align-items: flex-start; flex-direction: column; }.metric-grid { grid-template-columns: 1fr; }.guest-lock__content { padding: 20px 14px; } }
</style>
