import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://192.168.0.21:8000',
        changeOrigin: true,
        secure: false
      },
      '/media': {
        target: 'http://192.168.0.21:8000',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
