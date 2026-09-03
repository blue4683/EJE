import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { onSessionExpired } from '@/api/client'
import { restoreSession } from '@/api/auth'
import './styles/tokens.css'

const app = createApp(App)
app.use(createPinia())

onSessionExpired(() => {
  const currentPath = router.currentRoute.value.fullPath
  if (router.currentRoute.value.name !== 'login') {
    router.replace({ name: 'login', query: { redirect: currentPath } })
  }
})

await restoreSession()

app.use(router)
app.mount('#app')
