<template>
  <div class="settings-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('settings.general') }}</h1>
      <p class="page-description">{{ t('settings.generalDesc') }}</p>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.devTools') }}</h2>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.mainDevTools') }}</span>
          <span class="setting-hint">{{ t('settings.mainDevToolsHint') }}</span>
        </div>
        <button
          class="toggle-btn"
          :class="{ active: mainDevToolsEnabled }"
          @click="toggleMainDevTools"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const mainDevToolsEnabled = ref(false)

onMounted(() => {
  if (window.electronAPI?.app) {
    window.electronAPI.app.getSettings().then(settings => {
      if (settings) {
        mainDevToolsEnabled.value = settings.mainDevToolsEnabled !== undefined ? settings.mainDevToolsEnabled : false
      }
    })
  }
})

const toggleMainDevTools = async () => {
  mainDevToolsEnabled.value = !mainDevToolsEnabled.value
  if (window.electronAPI?.app) {
    await window.electronAPI.app.setMainDevToolsEnabled(mainDevToolsEnabled.value)
  }
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

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.setting-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.setting-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.toggle-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
}

.toggle-track {
  display: block;
  width: 44px;
  height: 24px;
  background: var(--bg-tertiary);
  border-radius: 12px;
  position: relative;
  transition: background var(--transition-fast);
}

.toggle-btn.active .toggle-track {
  background: var(--accent-primary);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform var(--transition-fast);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.toggle-btn.active .toggle-thumb {
  transform: translateX(20px);
}
</style>
