import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const API_TARGET = 'http://localhost:8123'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
        // SSE 长连接
        timeout: 600000,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Connection', 'keep-alive')
          })
        },
      },
    },
  },
})
