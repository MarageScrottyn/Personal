<template>
  <div class="devtools-settings">
    <h2 class="settings-title">{{ t('settings.devTools') }}</h2>
    <p class="settings-description">{{ t('settings.devToolsDescription') }}</p>

    <div class="settings-section">
      <div class="section-title">{{ t('settings.mainWindow') }}</div>
      
      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsMain') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsMainHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: mainWindowDevTools }"
            @click="toggleMainWindowDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsAutoOpen') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsAutoOpenHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: autoOpenMainDevTools }"
            @click="toggleAutoOpenMainDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>
    </div>

    <div class="settings-section">
      <div class="section-title">{{ t('settings.taskbar') }}</div>
      
      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsTaskbar') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsTaskbarHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: taskbarDevTools }"
            @click="toggleTaskbarDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsAutoOpenTaskbar') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsAutoOpenTaskbarHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: autoOpenTaskbarDevTools }"
            @click="toggleAutoOpenTaskbarDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>
    </div>

    <div class="settings-section">
      <div class="section-title">{{ t('settings.dynamicIsland') }}</div>
      
      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsIsland') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsIslandHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: islandDevTools }"
            @click="toggleIslandDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsAutoOpenIsland') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsAutoOpenIslandHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: autoOpenIslandDevTools }"
            @click="toggleAutoOpenIslandDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>
    </div>

    <div class="settings-section">
      <div class="section-title">{{ t('settings.systemWindows') }}</div>
      
      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsStartMenu') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsStartMenuHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: startMenuDevTools }"
            @click="toggleStartMenuDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsQuickSettings') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsQuickSettingsHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: quickSettingsDevTools }"
            @click="toggleQuickSettingsDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsNotificationCenter') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsNotificationCenterHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: notificationCenterDevTools }"
            @click="toggleNotificationCenterDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">{{ t('settings.devToolsSystemTray') }}</div>
          <div class="setting-hint">{{ t('settings.devToolsSystemTrayHint') }}</div>
        </div>
        <div class="setting-control">
          <button 
            class="toggle-btn"
            :class="{ active: systemTrayDevTools }"
            @click="toggleSystemTrayDevTools"
          >
            <div class="toggle-track">
              <div class="toggle-thumb"></div>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 主程序窗口
const mainWindowDevTools = ref(false)
const autoOpenMainDevTools = ref(false)

// 任务栏
const taskbarDevTools = ref(false)
const autoOpenTaskbarDevTools = ref(false)

// 灵动岛
const islandDevTools = ref(false)
const autoOpenIslandDevTools = ref(false)

// 系统窗口
const startMenuDevTools = ref(false)
const quickSettingsDevTools = ref(false)
const notificationCenterDevTools = ref(false)
const systemTrayDevTools = ref(false)

const toggleMainWindowDevTools = () => {
  mainWindowDevTools.value = !mainWindowDevTools.value
  window.electronAPI?.settings?.toggleMainWindowDevTools(mainWindowDevTools.value)
}

const toggleAutoOpenMainDevTools = () => {
  autoOpenMainDevTools.value = !autoOpenMainDevTools.value
}

const toggleTaskbarDevTools = () => {
  taskbarDevTools.value = !taskbarDevTools.value
  window.electronAPI?.settings?.toggleTaskbarDevTools(taskbarDevTools.value)
}

const toggleAutoOpenTaskbarDevTools = () => {
  autoOpenTaskbarDevTools.value = !autoOpenTaskbarDevTools.value
}

const toggleIslandDevTools = () => {
  islandDevTools.value = !islandDevTools.value
  window.electronAPI?.settings?.toggleIslandDevTools(islandDevTools.value)
}

const toggleAutoOpenIslandDevTools = () => {
  autoOpenIslandDevTools.value = !autoOpenIslandDevTools.value
}

const toggleStartMenuDevTools = () => {
  startMenuDevTools.value = !startMenuDevTools.value
  window.electronAPI?.settings?.toggleStartMenuDevTools(startMenuDevTools.value)
}

const toggleQuickSettingsDevTools = () => {
  quickSettingsDevTools.value = !quickSettingsDevTools.value
  window.electronAPI?.settings?.toggleQuickSettingsDevTools(quickSettingsDevTools.value)
}

const toggleNotificationCenterDevTools = () => {
  notificationCenterDevTools.value = !notificationCenterDevTools.value
  window.electronAPI?.settings?.toggleNotificationCenterDevTools(notificationCenterDevTools.value)
}

const toggleSystemTrayDevTools = () => {
  systemTrayDevTools.value = !systemTrayDevTools.value
  window.electronAPI?.settings?.toggleSystemTrayDevTools(systemTrayDevTools.value)
}
</script>

<style scoped>
.devtools-settings {
  width: 100%;
  max-width: 600px;
}

.settings-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.settings-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.settings-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info {
  flex: 1;
}

.setting-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.setting-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.setting-control {
  margin-left: 16px;
}

.toggle-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
}

.toggle-track {
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
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform var(--transition-fast);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.toggle-btn.active .toggle-thumb {
  transform: translateX(20px);
}

.settings-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.action-btn {
  flex: 1;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.apply-btn {
  background: var(--accent-primary);
  color: white;
}

.apply-btn:hover {
  background: var(--accent-primary-dark);
}

.reset-btn {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.reset-btn:hover {
  background: var(--border-color);
}
</style>
