<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchBasicResult, fetchRecordingDetail } from '@/api/history'
import { fetchProAnalysis } from '@/api/proAnalysis'
import { formatBytes, formatDateTime, formatMs } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import StateBlock from '@/components/common/StateBlock.vue'
import StatusChip from '@/components/history/StatusChip.vue'
import BasicResultCard from '@/components/history/BasicResultCard.vue'
import DeleteRecordingDialog from '@/components/history/DeleteRecordingDialog.vue'
import ProAnalysisSummary from '@/components/practice/ProAnalysisSummary.vue'

const props = defineProps({ recordingId: { type: String, required: true } })
const router = useRouter()
const detail = ref(null)
const state = ref('loading')
const error = ref(null)
const refreshError = ref(null)
const refreshing = ref(false)
const deleteOpen = ref(false)
const proResult = ref(null)
const proError = ref(null)

const isPending = computed(() => detail.value && !detail.value.basic)

async function load() {
  state.value = 'loading'
  error.value = null
  proResult.value = null
  proError.value = null
  try {
    detail.value = await fetchRecordingDetail(props.recordingId)
    if (detail.value.pro?.available) {
      try {
        proResult.value = await fetchProAnalysis(props.recordingId)
      } catch (caught) {
        proError.value = caught
      }
    }
    state.value = 'ready'
  } catch (caught) {
    error.value = caught
    state.value = 'error'
  }
}

async function refreshResult() {
  refreshing.value = true
  refreshError.value = null
  try {
    detail.value.basic = await fetchBasicResult(props.recordingId)
  } catch (caught) {
    refreshError.value = caught
  } finally {
    refreshing.value = false
  }
}

async function onDeleted() {
  await router.replace({ name: 'recordingList' })
}

onMounted(load)
</script>

<template>
  <PageHeader title="1분 자기소개 분석 Report" description="녹음 결과와 발견된 말하기 습관을 확인합니다.">
    <template #actions><router-link :to="{ name: 'home' }">← Dashboard</router-link></template>
  </PageHeader>
  <StateBlock :state="state" :message="error?.message" @retry="load">
    <template v-if="detail">
      <section class="meta-card">
        <div class="meta-card__title"><h2>{{ formatDateTime(detail.submittedAt) }}</h2><StatusChip :status="detail.analysis.status" /></div>
        <dl>
          <div><dt>길이</dt><dd>{{ formatMs(detail.durationMs) }}</dd></div>
          <div><dt>형식</dt><dd>{{ detail.mimeType }}</dd></div>
          <div><dt>파일 크기</dt><dd>{{ formatBytes(detail.fileSizeBytes) }}</dd></div>
          <div><dt>분석 버전</dt><dd>{{ detail.algorithmVersion || '—' }}</dd></div>
        </dl>
      </section>

      <section class="result-section">
        <div class="section-heading">
          <div><p>기본 분석</p><h2>추임새 결과</h2></div>
          <button v-if="detail.basic" type="button" :disabled="refreshing" @click="refreshResult">{{ refreshing ? '불러오는 중…' : '결과 다시 불러오기' }}</button>
        </div>
        <p v-if="refreshError" class="inline-error" role="alert">{{ refreshError.message }}</p>
        <BasicResultCard v-if="detail.basic" :basic="detail.basic" />
        <StateBlock v-else-if="isPending" state="pending">
          <template #default></template>
        </StateBlock>
        <router-link v-if="isPending" :to="{ name: 'analysisProgress', params: { analysisId: detail.analysis.analysisId } }" class="progress-link">분석 진행 상황 보기</router-link>
      </section>

      <section v-if="detail.pro" class="pro-result-section">
        <div class="section-heading">
          <div><p>PRO ANALYSIS</p><h2>상세 분석 데이터</h2></div>
          <router-link v-if="detail.pro.available" :to="{ name: 'proAnalysis', params: { recordingId } }">전체 화면으로 보기</router-link>
          <router-link v-else :to="{ name: 'upgrade' }">PRO 기능 알아보기</router-link>
        </div>
        <p v-if="proError" class="inline-error" role="alert">{{ proError.message }}</p>
        <ProAnalysisSummary v-else-if="proResult?.metrics" :metrics="proResult.metrics" />
        <ProAnalysisSummary v-else :locked="true" />
      </section>
      <div class="footer-actions">
        <button type="button" @click="deleteOpen = true">기록 삭제</button>
      </div>
    </template>
  </StateBlock>
  <DeleteRecordingDialog :open="deleteOpen" :recording-id="recordingId" @close="deleteOpen = false" @deleted="onDeleted" />
</template>

<style scoped>
.meta-card { padding: var(--space-5); background: var(--color-surface); border: 0; }
.meta-card__title { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.meta-card h2 { margin: 0; font-size: 1.15rem; }
dl { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--space-3); margin: var(--space-5) 0 0; }
dl div { display: grid; min-height: 120px; place-content: center; gap: var(--space-2); text-align: center; border: 1px solid var(--color-border); border-radius: 7px; }
dt { color: var(--color-text-muted); font-size: 0.82rem; }
dd { margin: 0; font-size: 1.35rem; font-weight: 800; }
.result-section { margin-top: var(--space-6); padding: var(--space-5); border: 1px solid var(--color-border); border-radius: 7px; background: var(--color-surface); }
.pro-result-section { display: grid; gap: var(--space-4); margin-top: var(--space-6); padding: var(--space-5); border: 1px solid var(--color-border); border-radius: 7px; background: var(--color-surface); }
.section-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-4); }
.section-heading p, .section-heading h2 { margin: 0; }
.section-heading p { color: var(--color-text-muted); font-size: 0.76rem; font-weight: 900; }
.section-heading button { padding: 8px 11px; color: var(--color-text); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-1); }
.section-heading a { color: var(--color-text); font-size: .9rem; font-weight: 800; }
.inline-error { padding: var(--space-3); color: var(--color-danger); background: var(--color-danger-weak); border-radius: var(--radius-1); }
.progress-link { display: inline-flex; margin-top: var(--space-3); font-weight: 800; }
.footer-actions { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin-top: var(--space-8); padding-top: var(--space-5); border-top: 1px solid var(--color-border); }
.footer-actions button { padding: 8px 11px; color: var(--color-danger); background: transparent; border: 1px solid #efcaca; border-radius: var(--radius-1); }
@media (max-width: 650px) { dl { grid-template-columns: repeat(2, 1fr); } }
</style>
