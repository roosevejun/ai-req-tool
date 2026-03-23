import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// WSL 环境下，`localhost` 指向的是 WSL 自己，而不是 Windows 主机。
// 通过环境变量覆盖后端地址即可，例如：
//   export VITE_BACKEND_URL="http://<windows-host-ip>:8080"
const backendUrl = process.env.VITE_BACKEND_URL || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: backendUrl,
        changeOrigin: true
      }
    }
  }
})

