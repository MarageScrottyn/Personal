import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const API_BASE = '/api'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  
  const user = ref(null)
  try {
    const userData = localStorage.getItem('user')
    if (userData) {
      user.value = JSON.parse(userData)
    }
  } catch (e) {
    console.error('解析用户数据失败:', e)
    localStorage.removeItem('user')
  }

  const isAuthenticated = computed(() => !!accessToken.value)

  const isAdmin = computed(() => {
    return user.value && user.value.user_type === 'admin'
  })

  function setAuth(tokens, userData) {
    accessToken.value = tokens.access
    refreshToken.value = tokens.refresh
    user.value = userData
    localStorage.setItem('accessToken', tokens.access)
    localStorage.setItem('refreshToken', tokens.refresh)
    localStorage.setItem('user', JSON.stringify(userData))
    axios.defaults.headers.common['Authorization'] = `Bearer ${tokens.access}`
  }

  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    delete axios.defaults.headers.common['Authorization']
  }

  function updateUser(userData) {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  async function login(username, password) {
    try {
      const response = await axios.post(
        `${API_BASE}/auth/login/`,
        { username, password },
        { headers: { 'Content-Type': 'application/json' } }
      )
      setAuth(response.data, response.data.user)
      return response.data
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  async function register(username, email, password) {
    try {
      const response = await axios.post(
        `${API_BASE}/auth/register/`,
        { username, email, password },
        { headers: { 'Content-Type': 'application/json' } }
      )
      return response.data
    } catch (error) {
      console.error('Register error:', error)
      throw error
    }
  }

  async function logout() {
    clearAuth()
  }

  if (accessToken.value) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken.value}`
  }

  return {
    accessToken,
    refreshToken,
    user,
    isAuthenticated,
    isAdmin,
    login,
    register,
    logout,
    setAuth,
    clearAuth,
    updateUser
  }
})