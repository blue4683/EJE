import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const views = import.meta.glob(['/src/views/**/*.vue', '!/src/views/dev/**'])
const view = (path) =>
  views[`/src/views/${path}`] ?? (() => import('@/views/NotReadyView.vue'))

const routes = [
  { path: '/login', name: 'login', meta: { public: true }, component: view('auth/LoginView.vue') },
  { path: '/signup', name: 'signup', meta: { public: true }, component: view('auth/SignUpView.vue') },
  { path: '/', name: 'home', meta: { public: true }, component: view('dashboard/HomeView.vue') },
  { path: '/recordings', name: 'recordingList', component: view('dashboard/RecordingListView.vue') },
  {
    path: '/recordings/:recordingId',
    name: 'recordingDetail',
    props: true,
    meta: { public: true },
    component: view('dashboard/RecordingDetailView.vue'),
  },
  { path: '/trends', name: 'trends', component: view('stats/TrendsView.vue') },
  {
    path: '/weekly-report',
    name: 'weeklyReport',
    meta: { public: true },
    component: view('stats/WeeklyReportView.vue'),
  },
  { path: '/me', name: 'me', component: view('account/MyPageView.vue') },
  { path: '/upgrade', name: 'upgrade', component: view('account/UpgradeView.vue') },
  { path: '/record', name: 'record', meta: { public: true }, component: view('practice/RecordView.vue') },
  {
    path: '/analyses/:analysisId',
    name: 'analysisProgress',
    props: true,
    meta: { public: true },
    component: view('practice/AnalysisProgressView.vue'),
  },
  {
    path: '/recordings/:recordingId/pro',
    name: 'proAnalysis',
    props: true,
    meta: { proFeature: true },
    component: view('practice/ProAnalysisView.vue'),
  },
]

if (import.meta.env.DEV) {
  const devViews = import.meta.glob('/src/views/dev/**/*.vue')
  routes.push({
    path: '/dev/mock',
    name: 'devMock',
    component:
      devViews['/src/views/dev/MockConsoleView.vue'] ??
      (() => import('@/views/NotReadyView.vue')),
  })
}

routes.push({
  path: '/:pathMatch(.*)*',
  name: 'notFound',
  meta: { public: true },
  component: view('NotFoundView.vue'),
})

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  if (to.meta.public) return true

  const session = useSessionStore()
  if (!session.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
