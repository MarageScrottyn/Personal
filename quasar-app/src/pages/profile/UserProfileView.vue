<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="avatar-section">
        <div class="avatar">
          <UserIcon :size="80" />
        </div>
        <h2 class="username">{{ currentUser?.username || '用户名' }}</h2>
        <p class="email">{{ currentUser?.email || '未绑定邮箱' }}</p>
        <p class="user-type">{{ currentUser?.user_type === 'admin' || currentUser?.is_staff ? t('profile.admin') : t('profile.user') }}</p>
      </div>
    </div>

    <div class="profile-content">
      <div class="section">
        <h3 class="section-title">{{ t('profile.basicInfo') }}</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">{{ t('profile.userId') }}</span>
            <span class="value">{{ currentUser?.id || currentUser?.pk || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">{{ t('profile.joinDate') }}</span>
            <span class="value">{{ formatDate(currentUser?.created_at) || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">{{ t('profile.lastLogin') }}</span>
            <span class="value">{{ formatDate(currentUser?.last_login) || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">{{ t('profile.stats') }}</h3>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">
              <BookIcon :size="24" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.comics_count || 0 }}</div>
              <div class="stat-label">{{ t('profile.comics') }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <VideoIcon :size="24" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.videos_count || 0 }}</div>
              <div class="stat-label">{{ t('profile.videos') }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <FileTextIcon :size="24" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.notes_count || 0 }}</div>
              <div class="stat-label">{{ t('profile.notes') }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">{{ t('profile.settings') }}</h3>
        <div class="settings-list">
          <button class="setting-item" @click="goToSettings">
            <SettingsIcon :size="18" />
            <span>{{ t('profile.accountSettings') }}</span>
            <ChevronRightIcon :size="18" />
          </button>
          <button class="setting-item" @click="goToSecurity">
            <LockIcon :size="18" />
            <span>{{ t('profile.securitySettings') }}</span>
            <ChevronRightIcon :size="18" />
          </button>
          <button class="setting-item" @click="goToPrivacy">
            <EyeIcon :size="18" />
            <span>{{ t('profile.privacySettings') }}</span>
            <ChevronRightIcon :size="18" />
          </button>
          <button class="setting-item version-item" @click="handleVersionClick">
            <PackageIcon :size="18" />
            <span>{{ t('profile.version') }} {{ appVersion }}</span>
            <span v-if="clickCount > 0" class="click-hint">{{ 5 - clickCount }} {{ t('profile.moreClicks') }}</span>
            <ChevronRightIcon :size="18" />
          </button>
        </div>
      </div>
    </div>

    <!-- 权限提升弹窗 -->
    <div v-if="showElevationModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{ t('profile.elevationTitle') }}</h3>
          <button class="modal-close" @click="closeModal">
            <XIcon :size="18" />
          </button>
        </div>
        <div class="modal-body">
          <div v-if="isAdmin" class="admin-message">
            <CheckCircleIcon :size="48" />
            <p>{{ t('profile.alreadyAdmin') }}</p>
          </div>
          <div v-else>
            <p>{{ t('profile.elevationDesc') }}</p>
            <div class="form-group">
              <label>{{ t('profile.adminCode') }}</label>
              <input
                type="text"
                v-model="adminCode"
                :placeholder="t('profile.codePlaceholder')"
                class="elevation-input"
              />
            </div>
            <div v-if="elevationError" class="error-message">
              {{ elevationError }}
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button v-if="!isAdmin" class="btn btn-primary" @click="submitElevation" :disabled="isSubmitting">
            {{ isSubmitting ? t('app.loading') : t('app.confirm') }}
          </button>
          <button class="btn btn-secondary" @click="closeModal">
            {{ t('app.cancel') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 成功提示弹窗 -->
    <div v-if="showSuccessModal" class="modal-overlay" @click.self="closeSuccessModal">
      <div class="modal-content success-modal">
        <div class="success-icon">
          <CheckCircleIcon :size="64" />
        </div>
        <h3 class="modal-title">{{ t('app.success') }}</h3>
        <p>{{ t('profile.elevationSuccess') }}</p>
        <button class="btn btn-primary" @click="closeSuccessModal">
          {{ t('app.confirm') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import apiClient from '../../api/client'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const user = ref(null)
const stats = ref({})
const loading = ref(true)

const currentUser = computed(() => user.value || authStore.user)

// 版本号和权限提升相关
const appVersion = '1.0.0'
const clickCount = ref(0)
const clickTimer = ref(null)
const showElevationModal = ref(false)
const showSuccessModal = ref(false)
const adminCode = ref('')
const elevationError = ref('')
const isSubmitting = ref(false)
const isAdmin = ref(false)

const UserIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 24
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: '12', cy: '7', r: '4' })
    ])
  }
}

const BookIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z' }),
      h('path', { d: 'M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z' })
    ])
  }
}

const VideoIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('polygon', { points: '5 3 19 12 5 21 5 3' }),
      h('line', { x1: '19', y1: '12', x2: '23', y2: '12' })
    ])
  }
}

const FileTextIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' })
    ])
  }
}

const SettingsIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('circle', { cx: '12', cy: '12', r: '3' }),
      h('path', { d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z' })
    ])
  }
}

const LockIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('rect', { x: '3', y: '11', width: '18', height: '11', rx: '2', ry: '2' }),
      h('path', { d: 'M7 11V7a5 5 0 0 1 10 0v4' })
    ])
  }
}

const EyeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' }),
      h('circle', { cx: '12', cy: '12', r: '3' })
    ])
  }
}

const ChevronRightIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M9 18l6-6-6-6' })
    ])
  }
}

const PackageIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M12.8 2.5a2.5 2.5 0 0 0-1.6 0l-8 5A2.5 2.5 0 0 0 2 9.6v8.8a2 2 0 0 0 1.17 1.78l8 4a2 2 0 0 0 1.66 0l8-4a2 2 0 0 0 1.17-1.78V9.6a2.5 2.5 0 0 0-.6-1.1l-8-5z' }),
      h('polyline', { points: '2.29 7.54 12 12.05 21.71 7.54' }),
      h('line', { x1: '12', y1: '22.05', x2: '12', y2: '12.05' })
    ])
  }
}

const XIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M18 6L6 18' }),
      h('path', { d: 'M6 6l12 12' })
    ])
  }
}

const CheckCircleIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('polyline', { points: '16 10 10 16 8 14' })
    ])
  }
}

const formatDate = (dateString) => {
  if (!dateString) return null
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const fetchUserProfile = async () => {
  try {
    const response = await apiClient.get('/users/me/')
    user.value = response.data
    isAdmin.value = user.value.user_type === 'admin' || user.value.is_staff
    
    const statsResponse = await apiClient.get('/users/me/stats/')
    stats.value = statsResponse.data
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 如果API请求失败，使用authStore中的用户信息作为后备
    if (authStore.user) {
      isAdmin.value = authStore.user.user_type === 'admin' || authStore.user.is_staff
    }
  } finally {
    loading.value = false
  }
}

const goToSettings = () => {
  router.push('/profile/settings')
}

const goToSecurity = () => {
  router.push('/profile/security')
}

const goToPrivacy = () => {
  router.push('/profile/privacy')
}

const handleVersionClick = () => {
  clickCount.value++
  
  if (clickTimer.value) {
    clearTimeout(clickTimer.value)
  }
  
  if (clickCount.value >= 5) {
    showElevationModal.value = true
    clickCount.value = 0
  } else {
    clickTimer.value = setTimeout(() => {
      clickCount.value = 0
    }, 2000)
  }
}

const closeModal = () => {
  showElevationModal.value = false
  adminCode.value = ''
  elevationError.value = ''
}

const closeSuccessModal = () => {
  showSuccessModal.value = false
  // 刷新用户信息
  fetchUserProfile()
}

const submitElevation = async () => {
  if (!adminCode.value.trim()) {
    elevationError.value = t('profile.emptyCode')
    return
  }
  
  isSubmitting.value = true
  elevationError.value = ''
  
  try {
    const response = await apiClient.post('/users/me/elevate/', {
      admin_code: adminCode.value
    })
    
    if (response.data.success) {
      showElevationModal.value = false
      showSuccessModal.value = true
      adminCode.value = ''
    }
  } catch (error) {
    elevationError.value = error.response?.data?.error || error.response?.data?.detail || t('profile.elevationFailed')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  fetchUserProfile()
})
</script>

<style scoped>
.profile-container {
  min-height: 100%;
  padding: 24px;
  background: var(--bg-primary);
}

.profile-header {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 32px;
  margin-bottom: 24px;
  text-align: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.username {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.email {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item .label {
  font-size: 13px;
  color: var(--text-secondary);
}

.info-item .value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--accent-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-primary);
  font-size: 14px;
  transition: all var(--transition-fast);
  text-align: left;
  width: 100%;
}

.setting-item:hover {
  background: var(--bg-tertiary);
}

.setting-item svg:first-child {
  color: var(--text-secondary);
}

.setting-item svg:last-child {
  margin-left: auto;
  color: var(--text-secondary);
}

.version-item {
  color: var(--text-secondary);
}

.version-item .click-hint {
  font-size: 12px;
  color: var(--accent-primary);
  margin-left: auto;
}

.user-type {
  font-size: 13px;
  color: var(--accent-primary);
  margin: 4px 0 0 0;
  padding: 4px 12px;
  background: rgba(var(--accent-primary-rgb), 0.1);
  border-radius: var(--radius-full);
  display: inline-block;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 420px;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.modal-close:hover {
  background: var(--bg-tertiary);
}

.modal-body {
  padding: 20px;
}

.modal-body p {
  color: var(--text-secondary);
  margin: 0 0 16px 0;
  line-height: 1.6;
}

.admin-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  color: var(--accent-primary);
}

.admin-message p {
  margin-top: 12px;
  color: var(--text-primary);
  font-weight: 500;
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

.elevation-input {
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

.elevation-input:focus {
  border-color: var(--accent-primary);
}

.elevation-input::placeholder {
  color: var(--text-tertiary);
}

.error-message {
  color: var(--error-color);
  font-size: 13px;
  margin-top: 8px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  justify-content: flex-end;
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
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.btn-secondary:hover {
  background: var(--border-color);
}

/* 成功弹窗 */
.success-modal {
  text-align: center;
}

.success-icon {
  color: var(--success-color);
  margin-bottom: 16px;
}

.success-modal p {
  margin-bottom: 24px;
}
</style>