import { defineStore } from 'pinia'

function safeJsonParse(str, fallback = null) {
  try {
    return str ? JSON.parse(str) : fallback
  } catch {
    return fallback
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    user: safeJsonParse(localStorage.getItem('user'))
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    isAdmin: (state) => state.user?.is_staff || false
  },

  actions: {
    login(accessToken, refreshToken, user) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      this.user = user
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      localStorage.setItem('user', JSON.stringify(user))
    },

    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    },

    updateUser(user) {
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    }
  }
})
