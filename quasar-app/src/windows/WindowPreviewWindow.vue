<template>
  <div
    v-if="app && app.windows"
    class="window-preview"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="preview-header">
      <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="preview-app-icon" />
      <span class="preview-app-name">{{ app.name?.replace('.exe', '') || 'Application' }}</span>
      <span class="preview-window-count">{{ app.windows.length }} window{{ app.windows.length > 1 ? 's' : '' }}</span>
    </div>
    
    <div class="preview-content">
      <div
        v-for="window in app.windows"
        :key="window.handle"
        class="preview-window-item"
        :class="{ active: window.isForeground }"
        @click="handleWindowClick(window.handle)"
        @contextmenu.prevent="handleWindowClose(window.handle)"
      >
        <div class="window-preview-thumbnail">
          <svg width="100%" height="100%" viewBox="0 0 200 120" preserveAspectRatio="xMidYMid meet">
            <rect x="2" y="2" width="196" height="116" rx="6" fill="rgba(30, 30, 30, 0.9)" stroke="rgba(255,255,255,0.1)" />
            <rect x="4" y="4" width="192" height="24" rx="4" fill="rgba(60, 60, 60, 0.8)" />
            <circle cx="12" cy="16" r="6" fill="#ff5f57" />
            <circle cx="28" cy="16" r="6" fill="#ffbd2e" />
            <circle cx="44" cy="16" r="6" fill="#27ca40" />
            <rect x="8" y="32" width="184" height="80" rx="2" fill="rgba(40, 40, 40, 0.8)" />
            <text x="100" y="76" text-anchor="middle" fill="rgba(255,255,255,0.3)" font-size="12">{{ window.title?.substring(0, 20) }}...</text>
          </svg>
        </div>
        <div class="window-preview-title">{{ window.title }}</div>
        <button class="window-close-btn" @click.stop="handleClose(window.handle)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const app = ref(null)

const getAppIconUrl = (iconPath) => {
  if (!iconPath) return ''
  if (iconPath.startsWith('data:') || iconPath.startsWith('http://') || iconPath.startsWith('https://')) {
    return iconPath
  }
  if (iconPath.startsWith('file://')) {
    const pathPart = iconPath.substring(7)
    return `file://${encodeURI(pathPart)}`
  }
  if (iconPath.startsWith('/') || iconPath.match(/^[A-Za-z]:/)) {
    let filePath = iconPath.replace(/\\/g, '/')
    if (filePath.match(/^[A-Za-z]:/)) {
      filePath = `/${filePath}`
    }
    return `file://${encodeURI(filePath)}`
  }
  return iconPath
}

const handleWindowClick = async (handle) => {
  await window.electronAPI.taskbar.activateWindow(handle)
  await window.electronAPI.taskbar.closeWindowPreview()
}

const handleClose = async (handle) => {
  await window.electronAPI.taskbar.closeWindow(handle)
  // 更新窗口列表
  window.electronAPI.taskbar.refreshWindowPreview()
}

const handleWindowClose = async (handle) => {
  await window.electronAPI.taskbar.closeWindow(handle)
  window.electronAPI.taskbar.refreshWindowPreview()
}

const handleMouseEnter = () => {
  // 保持预览显示
}

const handleMouseLeave = () => {
  window.electronAPI.taskbar.closeWindowPreview()
}

const handlePreviewUpdate = (event, data) => {
  console.log('[WindowPreviewWindow] Received preview update:', data)
  app.value = data.app
}

const handleClosePreview = () => {
  app.value = null
}

onMounted(() => {
  window.electronAPI.taskbar.onWindowPreviewUpdate(handlePreviewUpdate)
  window.electronAPI.taskbar.onCloseWindowPreview(handleClosePreview)
})

onUnmounted(() => {
  window.electronAPI.taskbar.removeWindowPreviewUpdateListener(handlePreviewUpdate)
  window.electronAPI.taskbar.removeCloseWindowPreviewListener(handleClosePreview)
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: transparent;
}

.window-preview {
  width: 300px;
  background: rgba(20, 20, 20, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  animation: previewFadeIn 0.15s ease-out;
}

@keyframes previewFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

.preview-app-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  border-radius: 4px;
}

.preview-app-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.preview-window-count {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 10px;
}

.preview-content {
  max-height: 280px;
  overflow-y: auto;
  padding: 4px;
}

.preview-content::-webkit-scrollbar {
  width: 6px;
}

.preview-content::-webkit-scrollbar-track {
  background: transparent;
}

.preview-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.preview-window-item {
  position: relative;
  margin: 4px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  background: rgba(255, 255, 255, 0.02);
}

.preview-window-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.preview-window-item.active {
  background: rgba(100, 149, 237, 0.2);
  border: 1px solid rgba(100, 149, 237, 0.3);
}

.window-preview-thumbnail {
  width: 100%;
  height: 68px;
  border-radius: 6px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.3);
  margin-bottom: 8px;
}

.window-preview-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 24px;
}

.window-close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  opacity: 0;
  transition: all 0.15s ease;
}

.preview-window-item:hover .window-close-btn {
  opacity: 1;
}

.window-close-btn:hover {
  background: rgba(255, 95, 87, 0.8);
  color: white;
}
</style>