<template>
  <div class="quick-settings-window">
    <div class="quick-settings">
      <!-- 顶部标题栏 -->
      <div class="settings-header">
        <span class="settings-title">快捷设置</span>
        <button class="settings-close" @click="closeWindow">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- 媒体控制 -->
      <div class="media-section">
        <div class="media-cover">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 20V6"/>
            <path d="M18 20V10"/>
            <path d="M6 20v-8"/>
          </svg>
        </div>
        <div class="media-info">
          <span class="media-title">没有播放媒体</span>
          <span class="media-artist">选择一个应用来播放</span>
        </div>
        <div class="media-controls">
          <button class="control-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12.01 21.45a8 8 0 0 0 7.99-8 8 8 0 0 0-8-7.99v16z"/>
              <path d="M4.01 21.45a8 8 0 0 0 7.99-8 8 8 0 0 0-8-7.99v16z"/>
            </svg>
          </button>
          <button class="control-btn play-btn">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
          </button>
          <button class="control-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 4h4v16H6z"/>
              <path d="M14 4h4v16h-4z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 快捷开关 -->
      <div class="settings-grid">
        <button class="setting-card" :class="{ active: networkEnabled }" @click="toggleNetwork">
          <div class="setting-icon" :class="{ active: networkEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <span class="setting-name">YBTN-5G 2</span>
          <span class="setting-status">已连接</span>
        </button>

        <button class="setting-card" :class="{ active: bluetoothEnabled }" @click="toggleBluetooth">
          <div class="setting-icon" :class="{ active: bluetoothEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 15a6 6 0 0 0 12 0"/>
              <path d="M8 9a6 6 0 0 1 12 0"/>
            </svg>
          </div>
          <span class="setting-name">蓝牙</span>
          <span class="setting-status">{{ bluetoothEnabled ? '已开启' : '已关闭' }}</span>
        </button>

        <button class="setting-card" :class="{ active: airplaneEnabled }" @click="toggleAirplane">
          <div class="setting-icon" :class="{ active: airplaneEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
              <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
              <line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
          </div>
          <span class="setting-name">飞行模式</span>
          <span class="setting-status">{{ airplaneEnabled ? '已开启' : '已关闭' }}</span>
        </button>

        <button class="setting-card" :class="{ active: nightModeEnabled }" @click="toggleNightMode">
          <div class="setting-icon" :class="{ active: nightModeEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </div>
          <span class="setting-name">夜间模式</span>
          <span class="setting-status">{{ nightModeEnabled ? '已开启' : '已关闭' }}</span>
        </button>

        <button class="setting-card" :class="{ active: hotspotEnabled }" @click="toggleHotspot">
          <div class="setting-icon" :class="{ active: hotspotEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 20a8 8 0 0 0 8-8V6a8 8 0 0 0-16 0v6a8 8 0 0 0 8 8z"/>
              <path d="M9.87 9.87a4 4 0 1 0 5.66-5.66"/>
            </svg>
          </div>
          <span class="setting-name">移动热点</span>
          <span class="setting-status">{{ hotspotEnabled ? '已开启' : '已关闭' }}</span>
        </button>

        <button class="setting-card" :class="{ active: accessibilityEnabled }" @click="toggleAccessibility">
          <div class="setting-icon" :class="{ active: accessibilityEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 4v4m0 8v4"/>
              <path d="M4 12h4m8 0h4"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </div>
          <span class="setting-name">辅助功能</span>
          <span class="setting-status">{{ accessibilityEnabled ? '已开启' : '已关闭' }}</span>
        </button>
      </div>

      <!-- 亮度调节 -->
      <div class="brightness-section">
        <div class="brightness-header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/>
            <line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <span class="brightness-label">亮度</span>
          <span class="brightness-value">{{ brightness }}%</span>
        </div>
        <input type="range" class="brightness-slider" min="0" max="100" v-model="brightness" />
      </div>

      <!-- 底部按钮 -->
      <div class="settings-footer">
        <button class="footer-action-btn" @click="expandToNotification">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 19V5"/>
            <path d="M5 12l7-7 7 7"/>
          </svg>
          <span>展开</span>
        </button>
        <button class="footer-action-btn" @click="openSettings">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
            <path d="M12 16v-4"/>
            <path d="M12 8h.01"/>
          </svg>
          <span>设置</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const networkEnabled = ref(true)
const bluetoothEnabled = ref(false)
const airplaneEnabled = ref(false)
const nightModeEnabled = ref(false)
const hotspotEnabled = ref(false)
const accessibilityEnabled = ref(false)
const brightness = ref(80)

const toggleNetwork = () => {
  networkEnabled.value = !networkEnabled.value
}

const toggleBluetooth = () => {
  bluetoothEnabled.value = !bluetoothEnabled.value
}

const toggleAirplane = () => {
  airplaneEnabled.value = !airplaneEnabled.value
}

const toggleNightMode = () => {
  nightModeEnabled.value = !nightModeEnabled.value
}

const toggleHotspot = () => {
  hotspotEnabled.value = !hotspotEnabled.value
}

const toggleAccessibility = () => {
  accessibilityEnabled.value = !accessibilityEnabled.value
}

const closeWindow = () => {
  window.electronAPI?.taskbar?.closeQuickSettings()
}

const expandToNotification = () => {
  window.electronAPI?.taskbar?.closeQuickSettings()
  window.electronAPI?.taskbar?.showNotificationCenter()
}

const openSettings = () => {
  window.electronAPI?.taskbar?.closeQuickSettings()
}

const handleKeydown = (event) => {
  if (event.key === 'Escape') {
    closeWindow()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.quick-settings-window {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #1c1c1c 0%, #1a1a2e 100%);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.quick-settings {
  display: flex;
  flex-direction: column;
  padding: 16px;
  color: #e5e7eb;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.settings-title {
  font-size: 15px;
  font-weight: 600;
}

.settings-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s;
}

.settings-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

.media-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  margin-bottom: 16px;
}

.media-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

.media-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.media-title {
  font-size: 13px;
  font-weight: 500;
  color: #e5e7eb;
}

.media-artist {
  font-size: 12px;
  color: #9ca3af;
}

.media-controls {
  display: flex;
  gap: 8px;
}

.control-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: #e5e7eb;
  cursor: pointer;
  transition: all 0.15s;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.play-btn {
  width: 36px;
  height: 36px;
  background: #3b82f6;
}

.play-btn:hover {
  background: #2563eb;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.setting-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.setting-card:hover {
  background: rgba(255, 255, 255, 0.08);
}

.setting-card.active {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.3);
}

.setting-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  transition: color 0.15s;
}

.setting-icon.active {
  color: #3b82f6;
}

.setting-name {
  font-size: 11px;
  color: #e5e7eb;
  text-align: center;
}

.setting-status {
  font-size: 10px;
  color: #9ca3af;
}

.brightness-section {
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  margin-bottom: 16px;
}

.brightness-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.brightness-label {
  flex: 1;
  font-size: 13px;
  color: #e5e7eb;
}

.brightness-value {
  font-size: 13px;
  color: #9ca3af;
}

.brightness-slider {
  width: 100%;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
  outline: none;
}

.brightness-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  background: #3b82f6;
  border-radius: 50%;
  cursor: pointer;
}

.brightness-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  background: #3b82f6;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.settings-footer {
  display: flex;
  gap: 8px;
}

.footer-action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 8px;
  color: #e5e7eb;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.footer-action-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}
</style>