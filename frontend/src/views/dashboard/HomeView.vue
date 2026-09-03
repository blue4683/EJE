<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchRecentAnalyses, fetchRecordingPage } from '@/api/history'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import RecordingCard from '@/components/history/RecordingCard.vue'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const items = ref([])
const state = ref('loading')
const error = ref(null)
const isGuest = computed(() => !session.isAuthenticated)
const previewLayout = import.meta.env.DEV && import.meta.env.VITE_WIREFRAME_PREVIEW !== 'false'
const totalExercises = ref(null)

async function load() {
  state.value = 'loading'
  error.value = null
  try {
    const [recent, page] = await Promise.all([
      fetchRecentAnalyses(),
      fetchRecordingPage(0, 1),
    ])
    items.value = recent.items
    totalExercises.value = page.totalElements
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
    <div v-if="isGuest && previewLayout" class="metric-grid metric-grid--preview" aria-label="대시보드 레이아웃 미리보기">
      <article><span>전체 연습 횟수</span><strong>0 <small>회</small></strong></article>
      <article><span>최근 분석</span><strong>0 <small>건</small></strong></article>
      <article><span>‘음’ 빈도</span><strong>0 <small>회</small></strong></article>
      <article><span>‘어’ 빈도</span><strong>0 <small>회</small></strong></article>
    </div>
    <div v-else class="metric-grid">
      <article><span>전체 연습 횟수</span><strong>{{ totalExercises }} <small>회</small></strong></article>
    </div>
      <template v-if="session.isAuthenticated">
      <PageHeader title="최근 분석" description="완료된 분석 중 최근 3건을 보여드려요.">
        <template #actions><router-link :to="{ name: 'recordingList' }">전체 기록 보기</router-link></template>
      </PageHeader>
      <StateBlock :state="state" :message="error?.message" @retry="load">
        <template #empty-action><router-link :to="{ name: 'record' }">첫 연습 시작하기</router-link></template>
        <div class="recording-grid"><RecordingCard v-for="item in items" :key="item.recordingId" :recording="item" /></div>
      </StateBlock>
    </template>
    <div v-if="isGuest" class="guest-recent"><h2>최근 분석</h2><p><span aria-hidden="true">🔒</span> 로그인 후 기록을 확인할 수 있어요 <router-link :to="{ name: 'login' }">로그인</router-link></p></div>
  </section>
</template>

<style scoped>
.intro { padding: 0 0 var(--space-8); border-bottom: 1px solid var(--color-border); }
.intro h1 { margin: 0 0 var(--space-2); font-size: clamp(1.7rem, 3vw, 2.25rem); line-height: 1.25; letter-spacing: -0.04em; }
.intro p { margin: 0; color: var(--color-text-muted); }
.practice-card { display: flex; align-items: flex-start; justify-content: space-between; min-height: 250px; gap: var(--space-6); margin: var(--space-6) 0 var(--space-8); padding: 36px 28px; border: 3px solid #3b3b3b; border-radius: var(--radius-1); background: var(--color-surface); }
.eyebrow { display: inline-block; margin: 0 0 var(--space-3); padding: 5px 12px; color: #555; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.76rem; font-weight: 800; letter-spacing: 0.08em; background: #e8e8e8; border-radius: 6px; }
.practice-card h2 { margin: 0; font-size: 1.4rem; letter-spacing: -0.03em; }.practice-card h2 span { margin-right: 6px; }.practice-card > div > p:not(.eyebrow) { margin: var(--space-3) 0 var(--space-5); color: var(--color-text-muted); }
.record-button { display: inline-flex; align-items: center; gap: 8px; padding: 13px 22px; color: white; font-weight: 800; text-decoration: none; background: #222; border-radius: 7px; }.record-button span { color: white; font-size: 0.85rem; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }.section-heading h2, .guest-recent h2 { margin: 0; font-size: 1.1rem; letter-spacing: -0.03em; }.section-heading span { padding: 5px 12px; color: #666; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.72rem; font-weight: 700; background: #e8e8e8; border-radius: 6px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-3); margin: var(--space-4) 0 var(--space-5); }.metric-grid article { display: grid; gap: var(--space-2); min-height: 116px; padding: var(--space-5); border: 1px solid #cfd3da; border-radius: 7px; background: var(--color-surface); }.metric-grid span { color: var(--color-text-muted); font-size: 0.82rem; }.metric-grid strong { font-size: 2rem; line-height: 1; }.metric-grid small { color: var(--color-text-muted); font-size: 0.8rem; font-weight: 600; }
.guest-lock { position: relative; display: grid; min-height: 210px; place-items: center; overflow: hidden; border: 1px dashed #b9bdc4; border-radius: var(--radius-1); background: var(--color-surface); }.placeholder-chart { position: absolute; inset: 28px 22px; opacity: 0.18; background: linear-gradient(168deg, transparent 48%, #d5d8de 49% 51%, transparent 52%) 0 30% / 28% 60% repeat-x, linear-gradient(#e1e3e7 1px, transparent 1px) 0 0 / 100% 35%; }.guest-lock__content { position: relative; padding: 20px 28px; text-align: center; background: rgba(255,255,255,.88); }.guest-lock__content h3 { margin: 0; font-size: 1.1rem; }.guest-lock__content p { margin: var(--space-2) 0 var(--space-4); color: var(--color-text-muted); }.guest-lock__content a { display: inline-block; padding: 10px 18px; color: white; font-size: .9rem; font-weight: 800; text-decoration: none; background: #222; border-radius: 6px; }
.guest-recent { margin-top: var(--space-8); }.guest-recent p { margin: var(--space-3) 0 0; padding: 28px; color: var(--color-text-muted); text-align: center; border: 1px solid var(--color-border); border-radius: var(--radius-1); background: var(--color-surface); }.guest-recent a { display: inline-block; margin-left: var(--space-3); padding: 7px 14px; color: var(--color-text); font-weight: 700; text-decoration: none; border: 1px solid #bbb; border-radius: 6px; }
.recording-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); }
@media (max-width: 760px) { .practice-card { min-height: 0; padding: var(--space-5); }.metric-grid { grid-template-columns: repeat(2, 1fr); }.recording-grid { grid-template-columns: 1fr; } }
@media (max-width: 480px) { .practice-card { flex-direction: column; }.section-heading { align-items: flex-start; flex-direction: column; }.metric-grid { grid-template-columns: 1fr; }.guest-lock__content { padding: 20px 14px; } }
</style>
