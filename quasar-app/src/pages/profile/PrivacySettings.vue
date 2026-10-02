<template>
  <div class="settings-container">
    <div class="settings-header">
      <button class="back-btn" @click="goBack">
        <ChevronLeftIcon :size="18" />
        <span>{{ t('app.back') }}</span>
      </button>
      <h1 class="page-title">{{ t('profile.privacySettings') }}</h1>
      <div class="placeholder"></div>
    </div>

    <div class="settings-content">
      <div class="section">
        <h2 class="section-title">{{ t('profile.profileVisibility') }}</h2>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.publicProfile') }}</h3>
            <p>{{ t('profile.publicProfileDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.publicProfile }]"
            @click="settings.publicProfile = !settings.publicProfile"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.showOnlineStatus') }}</h3>
            <p>{{ t('profile.showOnlineStatusDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.showOnlineStatus }]"
            @click="settings.showOnlineStatus = !settings.showOnlineStatus"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.showActivity') }}</h3>
            <p>{{ t('profile.showActivityDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.showActivity }]"
            @click="settings.showActivity = !settings.showActivity"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.dataSharing') }}</h2>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.shareUsageData') }}</h3>
            <p>{{ t('profile.shareUsageDataDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.shareUsageData }]"
            @click="settings.shareUsageData = !settings.shareUsageData"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.receiveRecommendations') }}</h3>
            <p>{{ t('profile.receiveRecommendationsDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.receiveRecommendations }]"
            @click="settings.receiveRecommendations = !settings.receiveRecommendations"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.notifications') }}</h2>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.emailNotifications') }}</h3>
            <p>{{ t('profile.emailNotificationsDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.emailNotifications }]"
            @click="settings.emailNotifications = !settings.emailNotifications"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('profile.pushNotifications') }}</h3>
            <p>{{ t('profile.pushNotificationsDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.pushNotifications }]"
            @click="settings.pushNotifications = !settings.pushNotifications"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">{{ t('profile.dataManagement') }}</h2>
        <div class="data-actions">
          <button class="data-action-btn" @click="exportData">
            <DownloadIcon :size="18" />
            <span>{{ t('profile.exportData') }}</span>
          </button>
          <button class="data-action-btn danger" @click="deleteAccount">
            <TrashIcon :size="18" />
            <span>{{ t('profile.deleteAccount') }}</span>
          </button>
        </div>
      </div>

      <div class="action-bar">
        <button class="btn btn-primary" @click="saveSettings">{{ t('app.save') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import apiClient from '../../api/client'

const router = useRouter()
const { t } = useI18n()

const settings = reactive({
  publicProfile: true,
  showOnlineStatus: true,
  showActivity: true,
  shareUsageData: false,
  receiveRecommendations: true,
  emailNotifications: true,
  pushNotifications: true
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

const DownloadIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4' }),
      h('polyline', { points: '7 10 12 15 17 10' }),
      h('line', { x1: '12', y1: '15', x2: '12', y2: '3' })
    ])
  }
}

const TrashIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M3 6h18' }),
      h('path', { d: 'M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6' }),
      h('path', { d: 'M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2' })
    ])
  }
}

const goBack = () => {
  router.push('/profile')
}

const saveSettings = async () => {
  try {
    await apiClient.patch('/users/me/privacy/', settings)
    router.push('/profile')
  } catch (error) {
    console.error('保存隐私设置失败:', error)
  }
}

const exportData = () => {
  console.log('Exporting data...')
}

const deleteAccount = () => {
  if (confirm(t('profile.deleteAccountConfirm'))) {
    console.log('Deleting account...')
  }
}
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

.data-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.data-action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  background: var(--bg-tertiary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-primary);
  font-size: 14px;
  transition: background var(--transition-fast);
  text-align: left;
}

.data-action-btn:hover {
  background: var(--border-color);
}

.data-action-btn.danger {
  color: var(--accent-danger);
  background: rgba(239, 68, 68, 0.1);
}

.data-action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.2);
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
</style>