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
        target: 'https://marage.ccwu.cc',
        changeOrigin: true,
        secure: false
      },
      '/media': {
        target: 'https://marage.ccwu.cc',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
