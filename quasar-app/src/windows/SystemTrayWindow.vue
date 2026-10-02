<template>
  <div class="system-tray-window">
    <div class="tray-header">
      <span>通知区域图标</span>
    </div>
    <div class="tray-content">
      <div class="tray-icons">
        <div 
          v-for="(icon, index) in trayIcons" 
          :key="index" 
          class="tray-icon"
          :title="icon.name"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path :d="icon.path"/>
          </svg>
        </div>
      </div>
    </div>
    <div class="tray-footer">
      <button class="tray-button" @click="handleCustomize">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 20h9"/>
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7.5 19.5a2.121 2.121 0 0 1-3-3z"/>
        </svg>
        <span>自定义</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const trayIcons = ref([
  { name: '音量', path: 'M11 5L6 9H2v6h4l5 4V5z' },
  { name: '网络', path: 'M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z' },
  { name: '蓝牙', path: 'M17.71 7.71L12 2H2v10l10 10 5.71-5.71-7.71-7.71z' },
  { name: '电池', path: 'M22 11.08V12a10 10 0 1 1-5.93-9.14' },
  { name: '电源', path: 'M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M12 6v6l4 2' },
])

const handleCustomize = () => {
  window.electronAPI?.taskbar?.closeSystemTray()
}

const closeWindow = () => {
  window.electronAPI?.taskbar?.closeSystemTray()
}
</script>

<style scoped>
.system-tray-window {
  width: 100%;
  height: 100%;
  min-height: 180px;
  background: #1f1f1f;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.tray-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
  font-size: 13px;
  font-weight: 500;
}

.tray-content {
  flex: 1;
  padding: 12px;
}

.tray-icons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tray-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tray-icon:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

.tray-footer {
  padding: 8px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.tray-button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.tray-button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}
</style>
