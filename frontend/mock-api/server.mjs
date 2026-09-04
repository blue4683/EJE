import http from 'node:http'

const PORT = Number(process.env.MOCK_PORT ?? 18080)
const ORIGIN = 'http://localhost:5173'
const accounts = {
  'demo@example.com': { password: 'Demo1234!', plan: 'FREE', name: '데모 사용자' },
  'pro@example.com': { password: 'Pro1234!', plan: 'PRO', name: 'PRO 사용자' },
}
const statusCalls = new Map()

const envelope = (data) => ({ success: true, data, error: null })
const userOf = (email) => ({ id: email.startsWith('pro') ? '2' : '1', email, name: accounts[email].name, plan: accounts[email].plan, profileImageUrl: null, createdAt: '2026-09-03T00:00:00Z' })

function send(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'Access-Control-Allow-Origin': ORIGIN, 'Access-Control-Allow-Credentials': 'true', 'Access-Control-Allow-Headers': 'Authorization, Content-Type, Idempotency-Key', 'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS', Vary: 'Origin' })
  res.end(body == null ? undefined : JSON.stringify(body))
}

async function json(req) {
  const chunks = []
  for await (const chunk of req) chunks.push(chunk)
  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString()) : null
}

async function drain(req) { for await (const _chunk of req) { /* Mock은 업로드 파일을 저장하지 않습니다. */ } }

const recordings = [
  { recordingId: '101', submittedAt: '2026-09-02T05:20:00Z', durationMs: 53000, status: 'COMPLETED', fillerTotalCount: 2 },
  { recordingId: '102', submittedAt: '2026-09-01T05:20:00Z', durationMs: 57000, status: 'COMPLETED', fillerTotalCount: 5 },
  { recordingId: '103', submittedAt: '2026-08-30T05:20:00Z', durationMs: 51000, status: 'PROCESSING', fillerTotalCount: null },
]

function statusOf(analysisId, recordingId = '101') {
  const call = (statusCalls.get(analysisId) ?? 0) + 1
  statusCalls.set(analysisId, call)
  return { analysisId, recordingId, status: call === 1 ? 'PENDING' : call === 2 ? 'PROCESSING' : 'COMPLETED', attemptNo: 1, autoRetryCount: 0, failureCode: null, retryable: false, retryRequiresAudio: false, startedAt: call > 1 ? '2026-09-03T05:00:00Z' : null, finishedAt: call > 2 ? '2026-09-03T05:00:03Z' : null }
}

const waveform = Array.from({ length: 48 }, (_, i) => ({ timeMs: i * 1100, amplitude: i % 9 < 2 ? 0.04 : 0.62, type: i % 9 < 2 ? 'SILENCE' : 'SPEECH' }))
function proResult(recordingId) {
  return { recordingId, analysisId: '5001', status: 'COMPLETED', algorithmVersion: 'speech-habits-v1', engineType: 'MOCK', engineVersion: 'local-review', metrics: { durationMs: 53000, speechDurationMs: 48000, silenceDurationMs: 5000, longSilenceCount: 1, repeatedExpressionCount: 1, basic: { fillerTotalCount: 5, fillerBreakdown: [{ expression: '음', count: 3 }, { expression: '어', count: 2 }] }, speechIntervals: [{ startMs: 0, endMs: 21000 }, { startMs: 23000, endMs: 53000 }], waveform, speechRate: { wordsPerMinute: 120, totalWordCount: 106 }, fillerTimeline: [{ eventIndex: 0, expression: '음', timeMs: 5400, segment: 'INITIAL' }, { eventIndex: 1, expression: '어', timeMs: 16200, segment: 'INITIAL' }, { eventIndex: 2, expression: '음', timeMs: 27600, segment: 'MIDDLE' }, { eventIndex: 3, expression: '음', timeMs: 38100, segment: 'MIDDLE' }, { eventIndex: 4, expression: '어', timeMs: 47200, segment: 'FINAL' }], segmentAnalysis: [{ segment: 'INITIAL', fillerCount: 2 }, { segment: 'MIDDLE', fillerCount: 2 }, { segment: 'FINAL', fillerCount: 1 }], coaching: { summary: '문장 시작 전에 추임새가 반복됩니다.', practiceRecommendation: '첫 문장을 천천히 시작해 보세요.', actionItems: ['첫 문장을 천천히 시작하기', '추임새 대신 1초 쉬기'] } } }
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') return send(res, 204)
  const path = new URL(req.url, `http://${req.headers.host}`).pathname.replace(/^\/api\/v1/, '')
  try {
    if (req.method === 'POST' && path === '/auth/login') {
      const body = await json(req); const account = accounts[body?.email]
      if (!account || account.password !== body.password) return send(res, 401, { success: false, data: null, error: { code: 'INVALID_CREDENTIALS', message: '이메일 또는 비밀번호를 확인해 주세요.' } })
      return send(res, 200, envelope({ accessToken: `mock-${account.plan.toLowerCase()}-token`, user: userOf(body.email) }))
    }
    if (req.method === 'POST' && path === '/auth/reissue') return send(res, 200, envelope({ accessToken: 'mock-free-token' }))
    if (req.method === 'POST' && path === '/auth/logout') { await drain(req); return send(res, 204) }
    if (req.method === 'GET' && path === '/users/me') return send(res, 200, envelope(userOf('demo@example.com')))
    if (req.method === 'GET' && path === '/dashboard/recordings') return send(res, 200, envelope({ content: recordings, page: 0, size: 20, totalElements: recordings.length, totalPages: 1 }))
    if (req.method === 'GET' && path === '/dashboard/recent-analyses') return send(res, 200, envelope({ items: recordings.slice(0, 2) }))
    if (req.method === 'POST' && path === '/recordings') { await drain(req); statusCalls.set('5001', 0); return send(res, 202, envelope({ recordingId: '101', analysisId: '5001', status: 'PENDING', attemptNo: 1, autoRetryCount: 0 })) }
    const analysis = path.match(/^\/analyses\/([^/]+)\/status$/)
    if (req.method === 'GET' && analysis) return send(res, 200, envelope(statusOf(analysis[1])))
    const dashStatus = path.match(/^\/dashboard\/recordings\/([^/]+)\/status$/)
    if (req.method === 'GET' && dashStatus) return send(res, 200, envelope(statusOf('5001', dashStatus[1])))
    const pro = path.match(/^\/recordings\/([^/]+)\/pro-analysis$/)
    if (req.method === 'GET' && pro) return send(res, 200, envelope(proResult(pro[1])))
    const detail = path.match(/^\/recordings\/([^/]+)$/)
    if (req.method === 'GET' && detail) return send(res, 200, envelope({ recordingId: detail[1], submittedAt: recordings[0].submittedAt, durationMs: recordings[0].durationMs, mimeType: 'audio/webm', fileSizeBytes: 48200, algorithmVersion: 'speech-habits-v1', analysis: { analysisId: '5001', status: 'COMPLETED' }, basic: proResult(detail[1]).metrics.basic, pro: { available: true, locked: false, lockedFeatures: [] } }))
    if (req.method === 'POST' && path === '/mock/waveform-analysis') { const body = await json(req); return send(res, 200, envelope({ ...proResult('mock').metrics, waveform: body?.waveform ?? waveform })) }
    if (req.method === 'POST' && path === '/mock/transcript-analysis') return send(res, 200, envelope(proResult('mock').metrics.basic))
    return send(res, 404, { success: false, data: null, error: { code: 'RESOURCE_NOT_FOUND', message: '요청한 정보를 찾을 수 없습니다.' } })
  } catch (error) { console.error(error); return send(res, 500, { success: false, data: null, error: { code: 'INTERNAL_ERROR', message: 'Mock 요청 처리 중 오류가 발생했습니다.' } }) }
})

server.listen(PORT, '127.0.0.1', () => console.log(`EJE frontend Mock API: http://127.0.0.1:${PORT}/api/v1`))
