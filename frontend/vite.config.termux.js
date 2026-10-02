import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Termux 部署专用配置
// - API/媒体请求代理到本机 Django(8000)
// - 通过 Cloudflare 域名(HTTPS)访问时，HMR 热重载走 WSS
export default defineConfig({
  plugins: [
    vue(),
    {
      // 仅对 Vite 注入的 HMR 客户端强制禁用缓存：
      // 开发服务器重启后 wsToken 会变化，no-store 保证浏览器每次都拿到含最新 token 的客户端，
      // 避免因 304 缓存沿用旧 token 导致 HMR WebSocket 握手 400 失败。
      // 只影响 /@vite/client 这一个小文件，其他模块与预构建依赖仍走 Vite 默认缓存（不拖慢首屏）。
      name: 'force-no-store-hmr-client',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const path = (req.url || '').split('?')[0]
          if (path === '/@vite/client') {
            // 拦截对 Cache-Control 的写入，统一改写为 no-store（确保覆盖 Vite 默认的 no-cache）
            const originalSetHeader = res.setHeader.bind(res)
            res.setHeader = (name, value) => {
              if (typeof name === 'string' && name.toLowerCase() === 'cache-control') {
                value = 'no-store'
              }
              return originalSetHeader(name, value)
            }
          }
          next()
        })
      }
    }
  ],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: false,
    // 允许通过任意域名（含 Cloudflare 隧道域名）访问开发服务器
    allowedHosts: true,
    // HMR 热重载：经 Cloudflare HTTPS 访问时使用 WSS 协议回连域名
    hmr: {
      protocol: 'wss',
      host: 'marage.ccwu.cc',
      clientPort: 443
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        secure: false
      },
      '/media': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
