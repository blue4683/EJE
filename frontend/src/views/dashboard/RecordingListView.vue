<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchRecordingPage } from '@/api/history'
import { usePagination } from '@/composables/usePagination'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import Pagination from '@/components/history/Pagination.vue'
import RecordingCard from '@/components/history/RecordingCard.vue'

const route = useRoute()
const router = useRouter()
const pagination = usePagination(fetchRecordingPage)

async function changePage(next) {
  await router.replace({ query: next > 0 ? { page: String(next) } : {} })
  await pagination.load(next)
}

onMounted(() => {
  const requestedPage = Number(route.query.page)
  pagination.load(Number.isFinite(requestedPage) ? requestedPage : 0)
})
</script>

<template>
  <PageHeader title="연습 기록" description="제출한 순서대로 모든 연습을 확인할 수 있습니다.">
    <template #actions><router-link :to="{ name: 'record' }" class="primary-link">새 연습</router-link></template>
  </PageHeader>
  <StateBlock :state="pagination.state.value" :message="pagination.error.value?.message" @retry="pagination.load()">
    <template #empty-action><router-link :to="{ name: 'record' }">첫 연습 시작하기</router-link></template>
    <div class="recording-grid">
      <RecordingCard v-for="item in pagination.items.value" :key="item.recordingId" :recording="item" />
    </div>
    <Pagination
      :page="pagination.page.value"
      :total-pages="pagination.totalPages.value"
      :total-elements="pagination.totalElements.value"
      :can-prev="pagination.canPrev.value"
      :can-next="pagination.canNext.value"
      @change="changePage"
    />
  </StateBlock>
</template>

<style scoped>
.primary-link { display: inline-flex; padding: 10px 14px; color: white; font-weight: 800; text-decoration: none; background: var(--color-primary); border-radius: var(--radius-1); }
.recording-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-4); }
@media (max-width: 700px) { .recording-grid { grid-template-columns: 1fr; } }
</style>
