<template>
  <div 
    class="quick-settings" 
    :class="{ visible: isVisible }"
    @click.self="$emit('close')"
  >
    <div class="quick-settings-content">
      <!-- 媒体控制 -->
      <div class="media-section">
        <div class="media-info">
          <div class="media-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
            </svg>
          </div>
          <div class="media-details">
            <h4 class="media-title">酷狗音乐</h4>
            <p class="media-subtitle">我很好，那么你呢？</p>
            <p class="media-album">《心电心》</p>
          </div>
          <div class="media-cover">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="currentColor">
              <circle cx="12" cy="12" r="10"/>
            </svg>
          </div>
        </div>
        
        <div class="media-controls">
          <button class="control-btn" @click="handlePrev">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="19 20 9 12 19 4 19 20"/>
              <polygon points="5 20 15 12 5 4 5 20"/>
            </svg>
          </button>
          <button class="control-btn play-btn" @click="handlePlayPause">
            <svg v-if="!isPlaying" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
            <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="6" y="4" width="4" height="16"/>
              <rect x="14" y="4" width="4" height="16"/>
            </svg>
          </button>
          <button class="control-btn" @click="handleNext">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 4 15 12 5 20 5 4"/>
              <polygon points="19 4 9 12 19 20 19 4"/>
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
          <svg class="setting-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>

        <button class="setting-card" :class="{ active: bluetoothEnabled }" @click="toggleBluetooth">
          <div class="setting-icon" :class="{ active: bluetoothEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17.7 7.7a2.5 2.5 0 1 1 1.8 4.3H2a2.5 2.5 0 1 1 1.8-4.3h13.9zM15 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm4.7-1.8a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"/>
            </svg>
          </div>
          <span class="setting-name">蓝牙</span>
          <span class="setting-status">未连接</span>
          <svg class="setting-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>

        <button class="setting-card" :class="{ active: flightMode }" @click="toggleFlightMode">
          <div class="setting-icon" :class="{ active: flightMode }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17.8 19.2 16 11l3-3-1.8-1.8L12 8 7.8 3.8 6 6l3 8-1.8 1.8L12 14l5.8 5.2z"/>
            </svg>
          </div>
          <span class="setting-name">飞行模式</span>
          <span class="setting-status">关闭</span>
        </button>

        <button class="setting-card" :class="{ active: nightMode }" @click="toggleNightMode">
          <div class="setting-icon" :class="{ active: nightMode }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </div>
          <span class="setting-name">夜间模式</span>
          <span class="setting-status">关闭</span>
        </button>

        <button class="setting-card" :class="{ active: hotspotEnabled }" @click="toggleHotspot">
          <div class="setting-icon" :class="{ active: hotspotEnabled }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </div>
          <span class="setting-name">移动热点</span>
          <span class="setting-status">关闭</span>
        </button>

        <button class="setting-card" @click="handleAccessibility">
          <div class="setting-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
          </div>
          <span class="setting-name">辅助功能</span>
          <span class="setting-status">更多</span>
          <svg class="setting-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>
      </div>

      <!-- 亮度滑块 -->
      <div class="slider-section">
        <div class="slider-header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          <span class="slider-label">亮度</span>
        </div>
        <input 
          type="range" 
          class="brightness-slider" 
          min="0" 
          max="100" 
          v-model="brightness"
        />
      </div>

      <!-- 底部按钮 -->
      <div class="bottom-section">
        <button class="expand-btn" @click="$emit('expand')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/>
          </svg>
          <span>展开</span>
        </button>
        <button class="settings-btn" @click="$emit('settings')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
          <span>设置</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  isVisible: {
    type: Boolean,
    default: false
  }
})

defineEmits(['close', 'expand', 'settings'])

const isPlaying = ref(false)
const networkEnabled = ref(true)
const bluetoothEnabled = ref(false)
const flightMode = ref(false)
const nightMode = ref(false)
const hotspotEnabled = ref(false)
const brightness = ref(50)

const handlePrev = () => {
  console.log('Previous track')
}

const handlePlayPause = () => {
  isPlaying.value = !isPlaying.value
}

const handleNext = () => {
  console.log('Next track')
}

const toggleNetwork = () => {
  networkEnabled.value = !networkEnabled.value
}

const toggleBluetooth = () => {
  bluetoothEnabled.value = !bluetoothEnabled.value
}

const toggleFlightMode = () => {
  flightMode.value = !flightMode.value
}

const toggleNightMode = () => {
  nightMode.value = !nightMode.value
}

const toggleHotspot = () => {
  hotspotEnabled.value = !hotspotEnabled.value
}

const handleAccessibility = () => {
  console.log('Accessibility settings')
}
</script>

<style scoped>
.quick-settings {
  position: fixed;
  bottom: 48px;
  right: 8px;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #2d2d2d 0%, #1a1a1a 100%);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  opacity: 0;
  visibility: hidden;
  transform: translateY(20px);
  transition: all 0.2s ease;
  z-index: 1000;
}

.quick-settings.visible {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.quick-settings-content {
  padding: 12px;
}

.media-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.media-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.media-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.media-details {
  flex: 1;
}

.media-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.media-subtitle {
  margin: 0 0 2px 0;
  font-size: 13px;
  color: #e5e7eb;
}

.media-album {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.media-cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff6b6b 0%, #feca57 100%);
  color: rgba(255, 255, 255, 0.8);
}

.media-controls {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.control-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 50%;
  color: #e5e7eb;
  cursor: pointer;
  transition: all 0.15s ease;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.play-btn {
  width: 48px;
  height: 48px;
  background: #ffffff;
  color: #1a1a1a;
}

.play-btn:hover {
  background: #f3f4f6;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.setting-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  gap: 4px;
}

.setting-card:hover {
  background: rgba(255, 255, 255, 0.1);
}

.setting-card.active {
  background: rgba(59, 130, 246, 0.15);
}

.setting-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: #9ca3af;
  transition: all 0.15s ease;
}

.setting-icon.active {
  background: #3b82f6;
  color: white;
}

.setting-name {
  font-size: 11px;
  color: #e5e7eb;
  text-align: center;
}

.setting-status {
  font-size: 10px;
  color: #6b7280;
}

.setting-arrow {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #6b7280;
}

.slider-section {
  padding: 8px;
  margin-bottom: 12px;
}

.slider-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #9ca3af;
}

.slider-label {
  font-size: 12px;
}

.brightness-slider {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  appearance: none;
  cursor: pointer;
}

.brightness-slider::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.bottom-section {
  display: flex;
  gap: 8px;
}

.expand-btn,
.settings-btn {
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
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.expand-btn:hover,
.settings-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}
</style>