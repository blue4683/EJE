<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchRecordingPage } from '@/api/history'
import { usePagination } from '@/composables/usePagination'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import Pagination from '@/components/history/Pagination.vue'
import StatusChip from '@/components/history/StatusChip.vue'
import { formatCount, formatDateTime, formatMs } from '@/utils/format'

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
  <PageHeader title="My History" description="지금까지의 말하기 연습을 확인해보세요.">
    <template #actions><router-link :to="{ name: 'record' }" class="primary-link">새 연습</router-link></template>
  </PageHeader>
  <StateBlock :state="pagination.state.value" :message="pagination.error.value?.message" @retry="pagination.load()">
    <template #empty-action><router-link :to="{ name: 'record' }">첫 연습 시작하기</router-link></template>
    <div class="history-table-wrap">
      <table class="history-table">
        <thead><tr><th>날짜</th><th>항목</th><th>시간</th><th>추임새</th><th>상태</th><th><span class="sr-only">상세 보기</span></th></tr></thead>
        <tbody>
          <tr v-for="item in pagination.items.value" :key="item.recordingId">
            <td>{{ formatDateTime(item.submittedAt) }}</td><td>1분 자기소개</td><td>{{ formatMs(item.durationMs) }}</td><td>{{ formatCount(item.fillerTotalCount) }}</td><td><StatusChip :status="item.status" /></td>
            <td><router-link :to="{ name: 'recordingDetail', params: { recordingId: item.recordingId } }">상세보기 →</router-link></td>
          </tr>
        </tbody>
      </table>
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
.history-table-wrap { overflow-x: auto; border: 1px solid var(--color-border); border-radius: 7px; background: var(--color-surface); }
.history-table { width: 100%; min-width: 720px; border-collapse: collapse; text-align: left; }.history-table th, .history-table td { padding: 18px 20px; border-bottom: 1px solid #e4e4e4; }.history-table th { color: var(--color-text-muted); font-size: .78rem; font-weight: 700; }.history-table tr:last-child td { border-bottom: 0; }.history-table a { color: var(--color-text); font-weight: 700; }
</style>
