<script setup>
import { computed, ref } from 'vue'
import { analyzeMockTranscript, analyzeMockWaveform } from '@/api/mock'
import PageHeader from '@/components/common/PageHeader.vue'
import WaveformCanvas from '@/components/waveform/WaveformCanvas.vue'

const waveformExample = {
  durationMs: 3000,
  waveform: Array.from({ length: 30 }, (_, index) => ({
    timeMs: index * 100,
    amplitude: index >= 5 && index < 25 ? 0.65 : 0.02,
  })),
}

const transcriptExample = {
  durationMs: 3000,
  tokens: [
    { text: '음', startMs: 100, endMs: 200 },
    { text: '오늘', startMs: 300, endMs: 600 },
    { text: '오늘', startMs: 1200, endMs: 1500 },
    { text: '어', startMs: 2600, endMs: 2700 },
  ],
}

const activeTab = ref('waveform')
const input = ref('')
const result = ref(null)
const error = ref(null)
const loading = ref(false)

const isWaveform = computed(() => activeTab.value === 'waveform')

function fillExample() {
  const example = isWaveform.value ? waveformExample : transcriptExample
  input.value = JSON.stringify(example, null, 2)
  result.value = null
  error.value = null
}

function selectTab(tab) {
  activeTab.value = tab
  input.value = ''
  result.value = null
  error.value = null
  fillExample()
}

async function analyze() {
  error.value = null
  result.value = null

  let payload
  try {
    payload = JSON.parse(input.value)
  } catch {
    error.value = { code: 'INVALID_JSON', message: 'JSON 형식을 먼저 확인해 주세요.' }
    return
  }

  loading.value = true
  try {
    result.value = isWaveform.value
      ? await analyzeMockWaveform(payload)
      : await analyzeMockTranscript(payload)
  } catch (caught) {
    error.value = caught
  } finally {
    loading.value = false
  }
}

fillExample()
</script>

<template>
  <section class="mock-page">
    <PageHeader
      title="Mock 분석 콘솔"
      description="개발·검증 전용 화면입니다. 백엔드 local/test 프로파일과 로그인이 필요합니다."
    />

    <div class="tabs" role="tablist" aria-label="Mock API 선택">
      <button
        type="button"
        role="tab"
        :aria-selected="isWaveform"
        @click="selectTab('waveform')"
      >
        파형 분석
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="!isWaveform"
        @click="selectTab('transcript')"
      >
        토큰 분석
      </button>
    </div>

    <label>
      요청 JSON
      <textarea v-model="input" rows="16" spellcheck="false" />
    </label>
    <div class="actions">
      <button type="button" class="secondary" @click="fillExample">명세 예시 채우기</button>
      <button type="button" :disabled="loading" @click="analyze">
        {{ loading ? '분석 중…' : '분석 요청' }}
      </button>
    </div>

    <p v-if="error" class="error" role="alert">
      {{ error.code }} · {{ error.message }}
    </p>

    <template v-if="result">
      <section v-if="isWaveform" class="result">
        <h2>파형 결과</h2>
        <WaveformCanvas :points="result.waveform" />
        <p>
          발화 {{ result.speechDurationMs }}ms + 침묵 {{ result.silenceDurationMs }}ms
          = 전체 {{ result.durationMs }}ms
        </p>
      </section>
      <section v-else class="result">
        <h2>토큰 결과</h2>
        <p>전체 단어 {{ result.totalWordCount }}개</p>
        <p>반복 표현 {{ result.repeatedExpressionCount }}회</p>
        <p>추임새 {{ result.basic?.fillerTotalCount }}회</p>
      </section>

      <section class="result">
        <h2>응답 JSON</h2>
        <pre>{{ JSON.stringify(result, null, 2) }}</pre>
      </section>
    </template>
  </section>
</template>

<style scoped>
.mock-page { display: grid; gap: var(--space-4); }
.tabs,
.actions { display: flex; gap: var(--space-2); }
label { display: grid; gap: var(--space-2); }
textarea,
pre { overflow: auto; padding: var(--space-3); border: 1px solid var(--color-border);
  border-radius: var(--radius-1); background: var(--color-surface); font-family: monospace; }
button { padding: var(--space-2) var(--space-4); border: 0; border-radius: var(--radius-1);
  color: white; background: var(--color-primary); }
button[aria-selected='true'] { background: var(--color-success); }
.secondary { color: var(--color-text); background: var(--color-border); }
.error { color: var(--color-danger); }
.result { display: grid; gap: var(--space-3); }
.result h2 { margin-bottom: 0; }
</style>
