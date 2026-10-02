<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <h1>{{ t('app.name') }}</h1>
        <p>{{ t('auth.loginSubtitle') }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">{{ t('auth.username') }}</label>
          <div class="input-wrapper">
            <UserIcon class="input-icon" />
            <input
              id="username"
              v-model="username"
              type="text"
              :placeholder="t('auth.usernamePlaceholder')"
              required
              autocomplete="username"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="password">{{ t('auth.password') }}</label>
          <div class="input-wrapper">
            <LockIcon class="input-icon" />
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              :placeholder="t('auth.passwordPlaceholder')"
              required
              autocomplete="current-password"
            />
            <button type="button" class="toggle-password" @click="showPassword = !showPassword">
              <EyeOffIcon v-if="showPassword" :size="18" />
              <EyeIcon v-else :size="18" />
            </button>
          </div>
        </div>

        <div v-if="error" class="error-message">
          <AlertCircleIcon :size="16" />
          <span>{{ error }}</span>
        </div>

        <button type="submit" class="login-btn" :disabled="loading">
          <LoaderIcon v-if="loading" :size="18" class="spin" />
          <span v-else>{{ t('auth.login') }}</span>
        </button>
      </form>

      <div class="login-footer">
        <span>{{ t('auth.noAccount') }}</span>
        <router-link to="/register" class="register-link">{{ t('auth.register') }}</router-link>
      </div>
    </div>

    <div class="language-switcher">
      <button
        v-for="lang in languages"
        :key="lang.code"
        :class="['lang-btn', { active: currentLocale === lang.code }]"
        @click="changeLanguage(lang.code)"
      >
        {{ lang.name }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const { t, locale } = useI18n()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const showPassword = ref(false)

const currentLocale = computed(() => locale.value)

const languages = [
  { code: 'zh-CN', name: '中文' },
  { code: 'en-US', name: 'EN' },
]

const UserIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: '12', cy: '7', r: '4' })
    ])
  }
}

const LockIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('rect', { x: '3', y: '11', width: '18', height: '11', rx: '2' }),
      h('path', { d: 'M7 11V7a5 5 0 0 1 10 0v4' })
    ])
  }
}

const EyeIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' }),
      h('circle', { cx: '12', cy: '12', r: '3' })
    ])
  }
}

const EyeOffIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94' }),
      h('path', { d: 'M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19' }),
      h('line', { x1: '1', y1: '1', x2: '23', y2: '23' })
    ])
  }
}

const AlertCircleIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('line', { x1: '12', y1: '8', x2: '12', y2: '12' }),
      h('line', { x1: '12', y1: '16', x2: '12.01', y2: '16' })
    ])
  }
}

const LoaderIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('line', { x1: '12', y1: '2', x2: '12', y2: '6' }),
      h('line', { x1: '12', y1: '18', x2: '12', y2: '22' }),
      h('line', { x1: '4.93', y1: '4.93', x2: '7.76', y2: '7.76' }),
      h('line', { x1: '16.24', y1: '16.24', x2: '19.07', y2: '19.07' }),
      h('line', { x1: '2', y1: '12', x2: '6', y2: '12' }),
      h('line', { x1: '18', y1: '12', x2: '22', y2: '12' }),
      h('line', { x1: '4.93', y1: '19.07', x2: '7.76', y2: '16.24' }),
      h('line', { x1: '16.24', y1: '7.76', x2: '19.07', y2: '4.93' })
    ])
  }
}

import apiClient from '../api/client'

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  try {
    const response = await apiClient.post('/auth/login/', {
      username: username.value,
      password: password.value
    })
    const { access, refresh, user } = response.data
    authStore.login(access, refresh, user)
    router.push('/comics')
  } catch (e) {
    const errorData = e.response?.data
    if (errorData?.detail) {
      error.value = errorData.detail
    } else if (errorData?.error) {
      error.value = errorData.error
      if (errorData.detail) {
        error.value += ': ' + errorData.detail
      }
    } else {
      error.value = t('auth.loginFailed')
    }
  } finally {
    loading.value = false
  }
}

const changeLanguage = (code) => {
  locale.value = code
  localStorage.setItem('locale', code)
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--bg-primary) 0%, var(--bg-secondary) 100%);
  padding: 20px;
  position: relative;
}

.login-container {
  width: 100%;
  max-width: 400px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-lg);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent-primary) 0%, #60a5fa 100%);
  border-radius: 16px;
  color: white;
}

.logo svg {
  width: 32px;
  height: 32px;
}

.login-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  width: 18px;
  height: 18px;
  color: var(--text-tertiary);
  pointer-events: none;
}

.form-group input {
  width: 100%;
  padding: 12px 14px 12px 44px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.form-group input::placeholder {
  color: var(--text-tertiary);
}

.form-group input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.toggle-password {
  position: absolute;
  right: 12px;
  padding: 4px;
  color: var(--text-tertiary);
  transition: color var(--transition-fast);
}

.toggle-password:hover {
  color: var(--text-secondary);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: var(--radius-md);
  color: var(--accent-danger);
  font-size: 13px;
}

.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: var(--accent-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: white;
  transition: all var(--transition-fast);
  margin-top: 8px;
}

.login-btn:hover:not(:disabled) {
  background: var(--accent-hover);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.register-link {
  color: var(--accent-primary);
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}

.language-switcher {
  position: absolute;
  bottom: 24px;
  right: 24px;
  display: flex;
  gap: 8px;
  padding: 4px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.lang-btn {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.lang-btn:hover {
  color: var(--text-primary);
}

.lang-btn.active {
  background: var(--accent-primary);
  color: white;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>