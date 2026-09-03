<script setup>
import { onMounted, ref } from 'vue'
import { fetchRecentAnalyses } from '@/api/history'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import RecordingCard from '@/components/history/RecordingCard.vue'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const items = ref([])
const state = ref('loading')
const error = ref(null)

async function load() {
  state.value = 'loading'
  error.value = null
  try {
    const data = await fetchRecentAnalyses()
    items.value = data.items
    state.value = data.items.length ? 'ready' : 'empty'
  } catch (caught) {
    error.value = caught
    state.value = 'error'
  }
}

onMounted(load)
</script>

<template>
  <section class="welcome">
    <div>
      <p>오늘의 말하기 연습</p>
      <h1>{{ session.user?.name }}님, 한 번 더 말해볼까요?</h1>
      <span>짧게라도 꾸준히 기록하면 변화가 선명해집니다.</span>
    </div>
    <router-link :to="{ name: 'record' }">연습 시작하기</router-link>
  </section>

  <PageHeader title="최근 연습" description="완료된 분석 중 최근 3건을 보여드려요.">
    <template #actions>
      <router-link :to="{ name: 'recordingList' }">전체 기록 보기</router-link>
    </template>
  </PageHeader>

  <StateBlock :state="state" :message="error?.message" @retry="load">
    <template #empty-action>
      <router-link :to="{ name: 'record' }">첫 연습 시작하기</router-link>
    </template>
    <div class="recording-grid">
      <RecordingCard v-for="item in items" :key="item.recordingId" :recording="item" />
    </div>
  </StateBlock>
</template>

<style scoped>
.welcome { display: flex; align-items: center; justify-content: space-between; gap: var(--space-6); margin-bottom: var(--space-10); padding: clamp(24px, 5vw, 48px); color: white; background: linear-gradient(135deg, #1d3578, #406ee5 70%, #668bee); border-radius: var(--radius-3); box-shadow: var(--shadow-card); }
.welcome p { margin: 0; font-size: 0.82rem; font-weight: 800; opacity: 0.75; }
.welcome h1 { margin: var(--space-2) 0; font-size: clamp(1.75rem, 4vw, 2.8rem); line-height: 1.15; letter-spacing: -0.04em; }
.welcome span { opacity: 0.82; }
.welcome a { flex: none; padding: 12px 17px; color: var(--color-primary-strong); font-weight: 800; text-decoration: none; background: white; border-radius: var(--radius-1); }
.recording-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-4); }
@media (max-width: 760px) { .welcome { align-items: stretch; flex-direction: column; } .welcome a { align-self: start; } .recording-grid { grid-template-columns: 1fr; } }
</style>
