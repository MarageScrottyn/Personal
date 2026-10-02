import { defineRouter } from '#q-app/wrappers'
import {
  createRouter,
  createMemoryHistory,
  createWebHistory,
  createWebHashHistory,
} from 'vue-router'
import { useAuthStore } from '../stores/auth'
import routes from './routes'

export default defineRouter((/* { store, ssrContext } */) => {
  const createHistory = process.env.SERVER
    ? createMemoryHistory
    : process.env.VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  })

  Router.beforeEach((to) => {
    const authStore = useAuthStore()
    const publicPaths = ['/login', '/register']

    if (!publicPaths.includes(to.path) && !authStore.accessToken) {
      console.log('Unauthorized, redirecting to login')
      return '/login'
    }

    if ((to.path === '/login' || to.path === '/register') && authStore.accessToken) {
      console.log('Already logged in, redirecting to comics')
      return '/comics'
    }

    console.log('Navigating to:', to.path)
  })

  return Router
})
