<template>
  <div class="settings-container">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <ChevronLeftIcon :size="18" />
        <span>{{ t('app.back') }}</span>
      </button>
      <h1 class="page-title">{{ t('profile.securitySettings') }}</h1>
      <div class="placeholder"></div>
    </div>

    <div class="settings-content">
      <div class="section">
        <h2 class="section-title">{{ t('profile.changePassword') }}</h2>
        <div class="form-group">
          <label>{{ t('profile.currentPassword') }}</label>
          <input
            type="password"
            v-model="form.currentPassword"
            :placeholder="t('profile.currentPasswordPlaceholder')"
            class="settings-input"
          />
        </div>
        <div class="form-group">
          <label>{{ t('profile.newPassword') }}</label>
          <input
            type="password"
            v-model="form.newPassword"
            :placeholder="t('profile.newPasswordPlaceholder')"
            class="settings-input"
          />
        </div>
        <div class="form-group">
          <label>{{ t('profile.confirmPassword') }}</label>
          <input
            type="password"
            v-model="form.confirmPassword"
            :placeholder="t('profile.confirmPasswordPlaceholder')"
            class="settings-input"
          />
        </div>
        <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        <button class="btn btn-primary" @click="changePassword" :disabled="isSubmitting">
          {{ isSubmitting ? t('app.loading') : t('profile.changePassword') }}
        </button>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.twoFactor') }}</h2>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.twoFactorAuth') }}</h3>
            <p>{{ t('profile.twoFactorDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: twoFactorEnabled }]"
            @click="toggleTwoFactor"
            :disabled="isSubmitting"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.sessions') }}</h2>
        <div v-if="sessions.length === 0" class="empty-state">
          <p>{{ t('profile.noSessions') }}</p>
        </div>
        <div v-else class="session-list">
          <div
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
          >
            <div class="session-info">
              <span class="session-device">{{ session.device }}</span>
              <span class="session-location">{{ session.location }}</span>
            </div>
            <div class="session-meta">
              <span class="session-time">{{ session.time }}</span>
              <button
                v-if="!session.isCurrent"
                class="btn btn-sm btn-secondary"
                @click="revokeSession(session.id)"
              >
                {{ t('profile.revoke') }}
              </button>
              <span v-else class="current-badge">{{ t('profile.current') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import apiClient from '../../api/client'

const router = useRouter()
const { t } = useI18n()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordError = ref('')
const isSubmitting = ref(false)
const twoFactorEnabled = ref(false)
const sessions = ref([])

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

const changePassword = async () => {
  passwordError.value = ''
  
  if (!form.currentPassword) {
    passwordError.value = t('profile.emptyCurrentPassword')
    return
  }
  
  if (!form.newPassword) {
    passwordError.value = t('profile.emptyNewPassword')
    return
  }
  
  if (form.newPassword !== form.confirmPassword) {
    passwordError.value = t('profile.passwordMismatch')
    return
  }
  
  isSubmitting.value = true
  
  try {
    await apiClient.post('/users/me/change-password/', {
      current_password: form.currentPassword,
      new_password: form.newPassword
    })
    
    form.currentPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    
    router.push('/profile')
  } catch (error) {
    passwordError.value = error.response?.data?.error || t('profile.changePasswordFailed')
  } finally {
    isSubmitting.value = false
  }
}

const toggleTwoFactor = () => {
  isSubmitting.value = true
  twoFactorEnabled.value = !twoFactorEnabled.value
  setTimeout(() => {
    isSubmitting.value = false
  }, 500)
}

const revokeSession = (sessionId) => {
  sessions.value = sessions.value.filter(s => s.id !== sessionId)
}

const fetchSessions = async () => {
  try {
    const response = await apiClient.get('/users/me/sessions/')
    sessions.value = response.data
  } catch (error) {
    console.error('获取会话列表失败:', error)
    sessions.value = [
      { id: 1, device: 'Desktop', location: '本地', time: '刚刚', isCurrent: true },
      { id: 2, device: 'Mobile', location: '未知', time: '2小时前', isCurrent: false }
    ]
  }
}

onMounted(() => {
  fetchSessions()
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

.form-group label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.settings-input {
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

.settings-input:focus {
  border-color: var(--accent-primary);
}

.settings-input::placeholder {
  color: var(--text-tertiary);
}

.error-message {
  color: var(--error-color);
  font-size: 13px;
  margin-bottom: 16px;
}

.toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.toggle-item:last-child {
  border-bottom: none;
}

.toggle-info h3 {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.toggle-info p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.toggle-btn {
  position: relative;
  width: 50px;
  height: 28px;
  background: var(--bg-tertiary);
  border: none;
  border-radius: 14px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.toggle-btn.active {
  background: var(--accent-primary);
}

.toggle-thumb {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform var(--transition-fast);
  box-shadow: var(--shadow-sm);
}

.toggle-btn.active .toggle-thumb {
  transform: translateX(22px);
}

.empty-state {
  padding: 32px;
  text-align: center;
}

.empty-state p {
  color: var(--text-secondary);
  margin: 0;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-device {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.session-location {
  font-size: 13px;
  color: var(--text-secondary);
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.session-time {
  font-size: 13px;
  color: var(--text-secondary);
}

.current-badge {
  font-size: 12px;
  color: var(--accent-primary);
  padding: 4px 8px;
  background: rgba(var(--accent-primary-rgb), 0.1);
  border-radius: var(--radius-sm);
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-primary {
  background: var(--accent-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--accent-primary-dark);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background: var(--border-color);
}
</style>