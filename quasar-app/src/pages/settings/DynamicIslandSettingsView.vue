<template>
  <div class="dynamic-island-settings">
    <div class="settings-section">
      <h2 class="section-title">{{ t('settings.dynamicIsland') }}</h2>
      <p class="section-desc">{{ t('settings.dynamicIslandDesc') }}</p>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.enableDynamicIsland') }}</span>
          <span class="setting-hint">{{ t('settings.enableDynamicIslandHint') }}</span>
        </div>
        <button
          class="toggle-btn"
          :class="{ active: isEnabled }"
          @click="toggleDynamicIsland"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.islandPosition') }}</span>
          <span class="setting-hint">{{ t('settings.islandPositionHint') }}</span>
        </div>
        <select v-model="position" class="select-input" @change="updatePosition">
          <option value="top">{{ t('settings.positionTop') }}</option>
          <option value="bottom">{{ t('settings.positionBottom') }}</option>
          <option value="left">{{ t('settings.positionLeft') }}</option>
          <option value="right">{{ t('settings.positionRight') }}</option>
        </select>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.islandStyle') }}</span>
          <span class="setting-hint">{{ t('settings.islandStyleHint') }}</span>
        </div>
        <div class="style-options">
          <button
            v-for="style in styleOptions"
            :key="style.value"
            class="style-option"
            :class="{ active: style.value === selectedStyle }"
            @click="selectStyle(style.value)"
          >
            <span class="style-preview" :class="style.value"></span>
            <span class="style-name">{{ style.label }}</span>
          </button>
        </div>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.showOnStartup') }}</span>
          <span class="setting-hint">{{ t('settings.showOnStartupHint') }}</span>
        </div>
        <button
          class="toggle-btn"
          :class="{ active: showOnStartup }"
          @click="toggleShowOnStartup"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.animationEnabled') }}</span>
          <span class="setting-hint">{{ t('settings.animationEnabledHint') }}</span>
        </div>
        <button
          class="toggle-btn"
          :class="{ active: animationsEnabled }"
          @click="toggleAnimations"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <span class="setting-label">{{ t('settings.devToolsEnabled') }}</span>
          <span class="setting-hint">{{ t('settings.devToolsEnabledHint') }}</span>
        </div>
        <button
          class="toggle-btn"
          :class="{ active: devToolsEnabled }"
          @click="toggleDevTools"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>

      </div>

    <div class="settings-section">
      <h3 class="section-subtitle">{{ t('settings.preview') }}</h3>
      <div class="preview-container">
        <div class="preview-window">
          <div class="preview-header">
            <span class="preview-dot red"></span>
            <span class="preview-dot yellow"></span>
            <span class="preview-dot green"></span>
          </div>
          <div class="preview-content" :class="position">
            <div
              class="preview-island"
              :class="[selectedStyle, { expanded: isPreviewExpanded, minimal: isPreviewMinimal }]"
              :style="{ opacity: isEnabled ? 1 : 0.3 }"
              @click="togglePreviewExpand"
            >
              <div class="preview-icon" :class="previewIconClass"></div>
              <div class="preview-text" v-if="!isPreviewMinimal">
                <span class="preview-title">{{ previewTitle }}</span>
                <span class="preview-subtitle" v-if="previewSubtitle">{{ previewSubtitle }}</span>
              </div>
              <div class="preview-progress" v-if="previewProgress !== undefined">
                <div class="preview-progress-ring">
                  <svg viewBox="0 0 24 24">
                    <circle class="progress-bg" cx="12" cy="12" r="10" />
                    <circle
                      class="progress-value"
                      cx="12" cy="12" r="10"
                      :stroke-dasharray="62.83"
                      :stroke-dashoffset="62.83 - (previewProgress / 100 * 62.83)"
                    />
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-subtitle">{{ t('settings.testIsland') }}</h3>
      <div class="test-buttons">
        <button class="test-btn" @click="testExpand">
          <span class="btn-icon">↕</span>
          {{ t('settings.testExpand') }}
        </button>
        <button class="test-btn" @click="testCollapse">
          <span class="btn-icon">↔</span>
          {{ t('settings.testCollapse') }}
        </button>
        <button class="test-btn" @click="testMinimal">
          <span class="btn-icon">○</span>
          {{ t('settings.testMinimal') }}
        </button>
        <button class="test-btn" @click="testMusic">
          <span class="btn-icon">♪</span>
          {{ t('settings.testMusic') }}
        </button>
        <button class="test-btn" @click="testDownload">
          <span class="btn-icon">↓</span>
          {{ t('settings.testDownload') }}
        </button>
        <button class="test-btn" @click="testTimer">
          <span class="btn-icon">⏱</span>
          {{ t('settings.testTimer') }}
        </button>
      </div>
    </div>

    <div class="settings-section">
      <h3 class="section-subtitle">{{ t('settings.quickActions') }}</h3>
      <div class="quick-actions-list">
        <div class="quick-action-item" v-for="action in quickActions" :key="action.id">
          <div class="action-icon">{{ action.icon }}</div>
          <div class="action-info">
            <span class="action-name">{{ action.name }}</span>
            <span class="action-desc">{{ action.description }}</span>
          </div>
          <button
            class="action-toggle"
            :class="{ active: action.enabled }"
            @click="toggleQuickAction(action.id)"
          >
            <span class="toggle-track">
              <span class="toggle-thumb"></span>
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const storeDynamicIslandSettings = (settings) => {
  if (window.electronAPI?.dynamicIsland) {
    // 将响应式对象转换为普通对象，避免克隆错误
    const plainSettings = JSON.parse(JSON.stringify(settings))
    window.electronAPI.dynamicIsland.setSettings(plainSettings)
  }
}

const isEnabled = ref(true)
const position = ref('top')
const selectedStyle = ref('rounded')
const showOnStartup = ref(true)
const animationsEnabled = ref(true)
const devToolsEnabled = ref(false)

const isPreviewExpanded = ref(false)
const isPreviewMinimal = ref(false)
const previewType = ref('activity')
const previewTitle = ref('聚合空间')
const previewSubtitle = ref('运行中')
const previewProgress = ref(undefined)

const styleOptions = [
  { value: 'rounded', label: t('settings.styleRounded') },
  { value: 'pill', label: t('settings.stylePill') },
  { value: 'square', label: t('settings.styleSquare') },
]

const quickActions = ref([
  { id: 'music', name: t('settings.actionMusic'), description: t('settings.actionMusicDesc'), icon: '♪', enabled: true },
  { id: 'download', name: t('settings.actionDownload'), description: t('settings.actionDownloadDesc'), icon: '↓', enabled: true },
  { id: 'timer', name: t('settings.actionTimer'), description: t('settings.actionTimerDesc'), icon: '⏱', enabled: true },
  { id: 'sync', name: t('settings.actionSync'), description: t('settings.actionSyncDesc'), icon: '⟳', enabled: true },
  { id: 'notifications', name: t('settings.actionNotifications'), description: t('settings.actionNotificationsDesc'), icon: '🔔', enabled: true },
])

const previewIconClass = computed(() => {
  switch (previewType.value) {
    case 'music': return 'pulse-animation'
    case 'sync': return 'spin-animation'
    case 'download': return 'bounce-animation'
    default: return ''
  }
})

onMounted(async () => {
  if (window.electronAPI?.dynamicIsland) {
    isEnabled.value = await window.electronAPI.dynamicIsland.isEnabled()
    const settings = await window.electronAPI.dynamicIsland.getSettings()
    if (settings) {
      position.value = settings.position || 'top'
      selectedStyle.value = settings.style || 'rounded'
      showOnStartup.value = settings.showOnStartup !== undefined ? settings.showOnStartup : true
      devToolsEnabled.value = settings.devToolsEnabled !== undefined ? settings.devToolsEnabled : false
      animationsEnabled.value = settings.animationsEnabled !== undefined ? settings.animationsEnabled : true
    }
  }
})

const toggleDynamicIsland = async () => {
  isEnabled.value = !isEnabled.value
  console.log('[IslandSettings] toggleDynamicIsland called, electronAPI:', window.electronAPI)
  if (window.electronAPI?.dynamicIsland) {
    try {
      await window.electronAPI.dynamicIsland.setEnabled(isEnabled.value)
      console.log('[IslandSettings] setEnabled success:', isEnabled.value)
    } catch (error) {
      console.error('[IslandSettings] setEnabled error:', error)
    }
  } else {
    console.error('[IslandSettings] electronAPI or dynamicIsland is undefined')
  }
}

const toggleShowOnStartup = async () => {
    showOnStartup.value = !showOnStartup.value
    if (window.electronAPI?.dynamicIsland) {
      const settings = await window.electronAPI.dynamicIsland.getSettings()
      if (settings) {
        settings.showOnStartup = showOnStartup.value
        storeDynamicIslandSettings(settings)
      }
    }
  }

const toggleAnimations = async () => {
  animationsEnabled.value = !animationsEnabled.value
  if (window.electronAPI?.dynamicIsland) {
    const settings = await window.electronAPI.dynamicIsland.getSettings()
    if (settings) {
      settings.animationsEnabled = animationsEnabled.value
      storeDynamicIslandSettings(settings)
    }
  }
}

const toggleDevTools = async () => {
  devToolsEnabled.value = !devToolsEnabled.value
  console.log('[IslandSettings] toggleDevTools called, new value:', devToolsEnabled.value)
  
  if (window.electronAPI?.dynamicIsland) {
    try {
      // 直接调用 setDevToolsEnabled，这是更简单的方式
      await window.electronAPI.dynamicIsland.setDevToolsEnabled(devToolsEnabled.value)
      console.log('[IslandSettings] setDevToolsEnabled called successfully')
    } catch (error) {
      console.error('[IslandSettings] toggleDevTools error:', error)
    }
  } else {
    console.error('[IslandSettings] electronAPI.dynamicIsland is NOT available')
  }
}

const updatePosition = async () => {
  if (window.electronAPI?.dynamicIsland) {
    await window.electronAPI.dynamicIsland.updatePosition(position.value)
  }
}

const selectStyle = async (style) => {
  selectedStyle.value = style
  if (window.electronAPI?.dynamicIsland) {
    await window.electronAPI.dynamicIsland.updateStyle(style)
  }
}

const togglePreviewExpand = () => {
  if (isPreviewMinimal.value) {
    isPreviewMinimal.value = false
  } else {
    isPreviewExpanded.value = !isPreviewExpanded.value
  }
}

const testExpand = () => {
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.expand()
  }
}

const testCollapse = () => {
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.collapse()
  }
}

const testMinimal = () => {
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.minimize()
  }
}

const testMusic = () => {
  previewType.value = 'music'
  previewTitle.value = '测试音乐'
  previewSubtitle.value = '艺术家 - 歌曲名'
  previewProgress.value = undefined
  
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.update({
      type: 'music',
      title: '测试音乐',
      subtitle: '艺术家 - 歌曲名'
    })
  }
}

const testDownload = () => {
  previewType.value = 'download'
  previewTitle.value = '正在下载'
  previewSubtitle.value = '文件.zip'
  previewProgress.value = 45
  
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.update({
      type: 'download',
      title: '正在下载',
      subtitle: '文件.zip',
      progress: 45,
      progressText: '45%',
      speed: '2.5 MB/s'
    })
  }
}

const testTimer = () => {
  previewType.value = 'timer'
  previewTitle.value = '倒计时'
  previewSubtitle.value = '05:30'
  previewProgress.value = undefined
  
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.update({
      type: 'timer',
      title: '倒计时',
      time: '05:30',
      timeLabel: '剩余时间'
    })
  }
}

const toggleQuickAction = async (actionId) => {
  const action = quickActions.value.find(a => a.id === actionId)
  if (action) {
    action.enabled = !action.enabled
    if (window.electronAPI?.dynamicIsland) {
      const settings = await window.electronAPI.dynamicIsland.getSettings()
      if (settings) {
        settings.quickActions = quickActions.value
        storeDynamicIslandSettings(settings)
      }
    }
  }
}

watch(position, (newPosition) => {
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.updatePosition(newPosition)
  }
})

watch(selectedStyle, (newStyle) => {
  if (window.electronAPI?.dynamicIsland) {
    window.electronAPI.dynamicIsland.updateStyle(newStyle)
  }
})

watch(animationsEnabled, async (enabled) => {
  if (window.electronAPI?.dynamicIsland) {
    const settings = await window.electronAPI.dynamicIsland.getSettings()
    if (settings) {
      settings.animationsEnabled = enabled
      storeDynamicIslandSettings(settings)
    }
  }
})
</script>

<style scoped>
.dynamic-island-settings {
  max-width: 600px;
}

.settings-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.section-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.section-subtitle {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
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

.select-input {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  min-width: 120px;
}

.select-input:hover {
  border-color: var(--accent-primary);
}

.style-options {
  display: flex;
  gap: 8px;
}

.style-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.style-option:hover {
  border-color: var(--accent-primary);
}

.style-option.active {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
}

.style-preview {
  width: 32px;
  height: 20px;
  background: rgba(255, 255, 255, 0.2);
}

.style-option.active .style-preview {
  background: rgba(255, 255, 255, 0.3);
}

.style-preview.rounded {
  border-radius: 10px;
}

.style-preview.pill {
  border-radius: 10px;
}

.style-preview.square {
  border-radius: 4px;
}

.style-name {
  font-size: 12px;
  color: var(--text-primary);
}

.style-option.active .style-name {
  color: white;
}

.preview-container {
  padding: 20px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.preview-window {
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.preview-header {
  display: flex;
  gap: 6px;
  padding: 8px 12px;
  background: #2d2d2d;
}

.preview-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.preview-dot.red {
  background: #ff5f56;
}

.preview-dot.yellow {
  background: #ffbd2e;
}

.preview-dot.green {
  background: #27ca40;
}

.preview-content {
  padding: 40px 20px;
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  position: relative;
}

.preview-content.top {
  align-items: flex-start;
}

.preview-content.bottom {
  align-items: flex-end;
}

.preview-content.left {
  justify-content: flex-start;
}

.preview-content.right {
  justify-content: flex-end;
}

.preview-island {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  min-width: 180px;
}

.preview-island:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

.preview-island.rounded {
  border-radius: 20px;
}

.preview-island.pill {
  border-radius: 50px;
}

.preview-island.square {
  border-radius: 8px;
}

.preview-island.expanded {
  min-width: 280px;
  flex-direction: column;
  padding: 16px;
  gap: 12px;
}

.preview-island.minimal {
  padding: 0;
  min-width: 40px;
  height: 40px;
  border-radius: 50%;
}

.preview-icon {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #4299f4 0%, #3184e0 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.preview-icon.pulse-animation {
  animation: pulse 1.5s ease-in-out infinite;
}

.preview-icon.spin-animation {
  animation: spin 2s linear infinite;
}

.preview-icon.bounce-animation {
  animation: bounce 0.6s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.preview-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.preview-title {
  font-size: 13px;
  font-weight: 600;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-subtitle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-progress {
  flex-shrink: 0;
}

.preview-progress-ring {
  width: 24px;
  height: 24px;
  position: relative;
}

.preview-progress-ring svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.preview-progress-ring circle {
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
}

.progress-bg {
  stroke: rgba(255, 255, 255, 0.2);
}

.progress-value {
  stroke: #4299f4;
  transition: stroke-dashoffset 0.3s ease;
}

.test-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.test-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.test-btn:hover {
  background: var(--bg-tertiary);
  border-color: var(--accent-primary);
}

.btn-icon {
  font-size: 16px;
}

.quick-actions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.action-icon {
  width: 36px;
  height: 36px;
  background: var(--accent-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: white;
  flex-shrink: 0;
}

.action-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.action-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.action-toggle {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  position: relative;
  width: 44px;
  height: 24px;
}

.action-toggle .toggle-track {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--border-color);
  border-radius: 12px;
  transition: background var(--transition-fast);
}

.action-toggle.active .toggle-track {
  background: var(--accent-primary);
}

.action-toggle .toggle-thumb {
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

.action-toggle.active .toggle-thumb {
  transform: translateX(20px);
}
</style>