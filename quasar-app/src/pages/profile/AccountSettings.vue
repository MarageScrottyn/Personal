<template>
  <div class="settings-container">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <ChevronLeftIcon :size="18" />
        <span>{{ t('app.back') }}</span>
      </button>
      <h1 class="page-title">{{ t('profile.accountSettings') }}</h1>
      <div class="placeholder"></div>
    </div>

    <div class="settings-content">
      <div class="section">
        <h2 class="section-title">{{ t('profile.basicInfo') }}</h2>
        <div class="form-group">
          <label>{{ t('profile.username') }}</label>
          <input
            type="text"
            v-model="form.username"
            :placeholder="t('profile.usernamePlaceholder')"
            class="settings-input"
          />
        </div>
        <div class="form-group">
          <label>{{ t('profile.email') }}</label>
          <input
            type="email"
            v-model="form.email"
            :placeholder="t('profile.emailPlaceholder')"
            class="settings-input"
          />
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.displayName') }}</h2>
        <div class="form-group">
          <label>{{ t('profile.nickname') }}</label>
          <input
            type="text"
            v-model="form.nickname"
            :placeholder="t('profile.nicknamePlaceholder')"
            class="settings-input"
          />
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.language') }}</h2>
        <div class="form-group">
          <label>{{ t('profile.language') }}</label>
          <select v-model="form.language" class="settings-select">
            <option value="zh-CN">{{ t('profile.chinese') }}</option>
            <option value="en-US">{{ t('profile.english') }}</option>
          </select>
        </div>
      </div>

      <div class="action-bar">
        <button class="btn btn-secondary" @click="resetForm">{{ t('app.reset') }}</button>
        <button class="btn btn-primary" @click="saveSettings">{{ t('app.save') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../../stores/auth'
import apiClient from '../../api/client'

const router = useRouter()
const { t, locale } = useI18n()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  email: '',
  nickname: '',
  language: locale.value
})

const ChevronLeftIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M15 19l-7-7 7-7' })
    ])
  }
}

const goBack = () => {
  router.push('/profile')
}

const resetForm = () => {
  form.username = authStore.user?.username || ''
  form.email = authStore.user?.email || ''
  form.nickname = authStore.user?.nickname || ''
  form.language = locale.value
}

const saveSettings = async () => {
  try {
    await apiClient.patch('/users/me/', {
      username: form.username,
      email: form.email,
      nickname: form.nickname
    })
    
    if (authStore.user) {
      authStore.updateUser({
        ...authStore.user,
        username: form.username,
        email: form.email,
        nickname: form.nickname
      })
    }
    
    locale.value = form.language
    localStorage.setItem('locale', form.language)
    
    router.push('/profile')
  } catch (error) {
    console.error('保存设置失败:', error)
  }
}

onMounted(() => {
  resetForm()
})
</script>

<style scoped>
.settings-container {
  min-height: 100%;
  background: var(--bg-primary);
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--bg-secondary) 0%, rgba(var(--accent-primary-rgb), 0.05) 100%);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  color: var(--text-primary);
  background: var(--border-color);
  transform: translateX(-2px);
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.5px;
}

.placeholder {
  width: 100px;
}

.settings-content {
  padding: 24px;
}

.section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.form-group {
  margin-bottom: 16px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.settings-input,
.settings-select {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--bg-primary);
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--transition-fast);
  box-sizing: border-box;
}

.settings-input:focus,
.settings-select:focus {
  border-color: var(--accent-primary);
}

.settings-input::placeholder {
  color: var(--text-tertiary);
}

.settings-select {
  cursor: pointer;
}

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 16px;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-primary {
  background: var(--accent-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--accent-primary-dark);
}

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.btn-secondary:hover {
  background: var(--border-color);
}
</style>