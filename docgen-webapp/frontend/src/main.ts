import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import { buildLoginRedirectPath, getToken, logout } from './auth'

axios.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axios.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const status = error?.response?.status
    const code = error?.response?.data?.code
    if (status === 401 || code === 40101) {
      logout()
      const current = router.currentRoute.value.fullPath || '/docgen'
      if (current !== '/login') {
        const loginPath = buildLoginRedirectPath(current)
        void router.replace(loginPath).catch(() => {
          window.location.replace(loginPath)
        })
      }
    }
    return Promise.reject(error)
  }
)

createApp(App).use(router).mount('#app')

