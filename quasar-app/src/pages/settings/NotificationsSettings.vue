<template>
  <div class="settings-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('settings.notifications') }}</h1>
      <p class="page-description">{{ t('settings.notificationsDesc') }}</p>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.emailNotifications') }}</h2>
      <div class="toggle-item">
        <div class="toggle-info">
          <h3>{{ t('settings.newContent') }}</h3>
          <p>{{ t('settings.newContentDesc') }}</p>
        </div>
        <button
          :class="['toggle-btn', { active: settings.newContent }]"
          @click="settings.newContent = !settings.newContent"
        >
          <span class="toggle-thumb"></span>
        </button>
      </div>
      <div class="toggle-item">
        <div class="toggle-info">
          <h3>{{ t('settings.commentsReplies') }}</h3>
          <p>{{ t('settings.commentsRepliesDesc') }}</p>
        </div>
        <button
          :class="['toggle-btn', { active: settings.commentsReplies }]"
          @click="settings.commentsReplies = !settings.commentsReplies"
        >
          <span class="toggle-thumb"></span>
        </button>
      </div>
      <div class="toggle-item">
        <div class="toggle-info">
          <h3>{{ t('settings.systemUpdates') }}</h3>
          <p>{{ t('settings.systemUpdatesDesc') }}</p>
        </div>
        <button
          :class="['toggle-btn', { active: settings.systemUpdates }]"
          @click="settings.systemUpdates = !settings.systemUpdates"
        >
          <span class="toggle-thumb"></span>
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.pushNotifications') }}</h2>
      <div class="toggle-item">
        <div class="toggle-info">
          <h3>{{ t('settings.enablePush') }}</h3>
          <p>{{ t('settings.enablePushDesc') }}</p>
        </div>
        <button
          :class="['toggle-btn', { active: settings.enablePush }]"
          @click="settings.enablePush = !settings.enablePush"
        >
          <span class="toggle-thumb"></span>
        </button>
      </div>
      <div v-if="settings.enablePush" class="nested-section">
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('settings.pushNewContent') }}</h3>
            <p>{{ t('settings.pushNewContentDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.pushNewContent }]"
            @click="settings.pushNewContent = !settings.pushNewContent"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
        <div class="toggle-item">
          <div class="toggle-info">
            <h3>{{ t('settings.pushMentions') }}</h3>
            <p>{{ t('settings.pushMentionsDesc') }}</p>
          </div>
          <button
            :class="['toggle-btn', { active: settings.pushMentions }]"
            @click="settings.pushMentions = !settings.pushMentions"
          >
            <span class="toggle-thumb"></span>
          </button>
        </div>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.browserNotifications') }}</h2>
      <div class="toggle-item">
        <div class="toggle-info">
          <h3>{{ t('settings.enableBrowser') }}</h3>
          <p>{{ t('settings.enableBrowserDesc') }}</p>
        </div>
        <button
          :class="['toggle-btn', { active: settings.enableBrowser }]"
          @click="toggleBrowserNotifications"
        >
          <span class="toggle-thumb"></span>
        </button>
      </div>
    </div>

    <div class="action-bar">
      <button class="btn btn-secondary" @click="resetToDefault">{{ t('settings.resetToDefault') }}</button>
      <button class="btn btn-primary" @click="saveSettings">{{ t('app.save') }}</button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const settings = reactive({
  newContent: true,
  commentsReplies: true,
  systemUpdates: true,
  enablePush: false,
  pushNewContent: true,
  pushMentions: true,
  enableBrowser: true,
})

const toggleBrowserNotifications = () => {
  if (!settings.enableBrowser && 'Notification' in window) {
    Notification.requestPermission().then((permission) => {
      settings.enableBrowser = permission === 'granted'
    })
  } else {
    settings.enableBrowser = !settings.enableBrowser
  }
}

const resetToDefault = () => {
  settings.newContent = true
  settings.commentsReplies = true
  settings.systemUpdates = true
  settings.enablePush = false
  settings.pushNewContent = true
  settings.pushMentions = true
  settings.enableBrowser = true
}

const saveSettings = () => {
  localStorage.setItem('notifications', JSON.stringify(settings))
}
</script>

<style scoped>
.settings-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
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

.nested-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
}

.nested-section .toggle-item {
  padding-left: 16px;
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