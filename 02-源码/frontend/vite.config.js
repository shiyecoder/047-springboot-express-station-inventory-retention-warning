import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 通道 2 专用前端端口 5171；代理到本通道后端 8081
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5171,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8081', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8081', changeOrigin: true }
    }
  }
})
