<template>
  <div class="start-menu-window">
    <div class="start-menu">
      <!-- 搜索框 -->
      <div class="search-box">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <path d="M21 21l-4.35-4.35"/>
        </svg>
        <input type="text" class="search-input" placeholder="搜索应用、设置和文件..." />
        <div class="search-shortcuts">
          <span class="shortcut">Win</span>
          <span class="shortcut">S</span>
        </div>
      </div>

      <!-- 应用列表区域 -->
      <div class="apps-section">
        <div class="section-header">
          <span class="section-title">固定</span>
          <button class="section-toggle">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 9l-7 7-7-7"/>
            </svg>
          </button>
        </div>
        
        <!-- 分页应用列表 -->
        <div 
          class="pinned-apps-container"
          @wheel="handleWheel"
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
        >
          <div 
            class="pinned-apps"
            :style="{ transform: `translateY(-${currentPage * pageHeight}px)` }"
          >
            <button 
              v-for="app in pinnedApps" 
              :key="app.id" 
              class="app-item" 
              @click="launchApp(app)"
            >
              <div class="app-icon-wrapper">
                <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="app-icon" :alt="app.name"/>
                <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <span class="app-name">{{ app.name }}</span>
            </button>
          </div>
        </div>
        
        <!-- 分页指示器 -->
        <div v-if="totalPages > 1" class="pagination-indicator">
          <button
            v-for="page in totalPages"
            :key="page"
            class="pagination-dot"
            :class="{ active: currentPage === page - 1 }"
            @click="goToPage(page - 1)"
          ></button>
        </div>
      </div>

      <!-- 推荐区域 -->
      <div class="recommended-section">
        <div class="section-header">
          <span class="section-title">推荐</span>
          <button class="section-toggle">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 9l-7 7-7-7"/>
            </svg>
          </button>
        </div>
        
        <div class="recommended-items">
          <div class="recommended-item">
            <div class="recommended-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
            </div>
            <div class="recommended-info">
              <span class="recommended-name">最近没有使用文件</span>
              <span class="recommended-type">开始使用以查看推荐</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部区域 -->
      <div class="start-footer">
        <div class="user-section">
          <div class="user-avatar">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <span class="user-name">用户</span>
        </div>
        
        <div class="footer-buttons">
          <button class="footer-btn" title="下载">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </button>
          <button class="footer-btn" title="文档">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </button>
          <button class="footer-btn" title="设置">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
              <path d="M12 16v-4"/>
              <path d="M12 8h.01"/>
            </svg>
          </button>
          <button class="footer-btn power-btn" title="电源">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22a10 10 0 0 0 10-10V6a10 10 0 0 0-20 0v6a10 10 0 0 0 10 10z"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const pinnedApps = ref([])
const currentPage = ref(0)
const appsPerPage = 18 // 每页显示18个应用（6列 × 3行）
const pageHeight = 180 // 每页高度（包含间距）
const touchStartY = ref(0)

const totalPages = computed(() => {
  return Math.ceil(pinnedApps.value.length / appsPerPage)
})

const goToPage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
  }
}

// 处理鼠标滚轮事件
const handleWheel = (event) => {
  event.preventDefault()
  
  if (event.deltaY < 0) {
    // 向上滚动，切换到上一页
    goToPage(currentPage.value - 1)
  } else {
    // 向下滚动，切换到下一页
    goToPage(currentPage.value + 1)
  }
}

// 处理触摸开始
const handleTouchStart = (event) => {
  touchStartY.value = event.touches[0].clientY
}

// 处理触摸移动
const handleTouchMove = (event) => {
  event.preventDefault()
  const touchCurrentY = event.touches[0].clientY
  const deltaY = touchStartY.value - touchCurrentY
  
  if (Math.abs(deltaY) > 50) {
    if (deltaY > 0) {
      goToPage(currentPage.value + 1)
    } else {
      goToPage(currentPage.value - 1)
    }
    touchStartY.value = touchCurrentY
  }
}

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

const fetchPinnedApps = async () => {
  try {
    const apps = await window.electronAPI?.taskbar?.getPinnedApps()
    if (apps) {
      pinnedApps.value = apps
      currentPage.value = 0
    }
  } catch (error) {
    console.error('获取固定应用失败:', error)
  }
}

const launchApp = (app) => {
  if (app.path) {
    window.electronAPI?.taskbar?.launchApp(app.path)
  }
  window.electronAPI?.taskbar?.closeStartMenu()
}

const handleKeydown = (event) => {
  if (event.key === 'Escape') {
    window.electronAPI?.taskbar?.closeStartMenu()
  }
}

onMounted(() => {
  fetchPinnedApps()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.start-menu-window {
  width: 480px;
  height: 640px;
  background: linear-gradient(180deg, #1c1c1c 0%, #1a1a2e 100%);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.start-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
  color: #e5e7eb;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 12px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #e5e7eb;
  font-size: 14px;
}

.search-input::placeholder {
  color: #9ca3af;
}

.search-shortcuts {
  display: flex;
  gap: 4px;
}

.shortcut {
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  font-size: 11px;
  color: #9ca3af;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-toggle {
  background: transparent;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.15s;
}

.section-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
}

.apps-section {
  flex: 1;
  overflow: hidden;
}

.pinned-apps-container {
  height: 100%;
  overflow: hidden;
}

.pinned-apps {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  transition: transform 0.3s ease;
}

.pagination-indicator {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 8px 0;
}

.pagination-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-dot:hover {
  background: rgba(255, 255, 255, 0.5);
}

.pagination-dot.active {
  background: rgba(255, 255, 255, 0.8);
  width: 12px;
  border-radius: 3px;
}

.app-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.app-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.app-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.app-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.app-name {
  font-size: 11px;
  color: #e5e7eb;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommended-section {
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.recommended-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recommended-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.recommended-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.recommended-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

.recommended-info {
  display: flex;
  flex-direction: column;
}

.recommended-name {
  font-size: 13px;
  color: #e5e7eb;
}

.recommended-type {
  font-size: 11px;
  color: #9ca3af;
}

.start-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: auto;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.user-section:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: #e5e7eb;
}

.footer-buttons {
  display: flex;
  gap: 4px;
}

.footer-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s;
}

.footer-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

.power-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.apps-section::-webkit-scrollbar {
  width: 6px;
}

.apps-section::-webkit-scrollbar-track {
  background: transparent;
}

.apps-section::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
}

.apps-section::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}
</style>