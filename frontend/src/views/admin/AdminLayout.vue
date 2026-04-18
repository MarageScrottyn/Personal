<template>
  <div class="admin-layout">
    <!-- 移动端导航栏 -->
    <div class="mobile-header" v-if="isMobile">
      <button class="menu-toggle" @click="toggleMenu">
        <span class="menu-icon">{{ menuOpen ? '✕' : '☰' }}</span>
      </button>
      <h1 class="mobile-title">{{ pageTitle }}</h1>
    </div>
    
    <!-- 侧边栏 -->
    <div class="admin-sidebar" :class="{ 'sidebar-open': menuOpen && isMobile }">
      <div class="admin-sidebar-header">
        <span class="sidebar-icon">⚙️</span>
        <h2>管理中心</h2>
      </div>
      <div class="admin-sidebar-menu">
        <router-link to="/" :class="['sidebar-item', { active: $route.path === '/' }]" @click="closeMenuOnMobile">
          <span class="item-icon">🏠</span>
          <span>返回首页</span>
        </router-link>
        <router-link to="/admin" :class="['sidebar-item', { active: $route.path === '/admin' }]" @click="closeMenuOnMobile">
          <span class="item-icon">📊</span>
          <span>仪表盘</span>
        </router-link>
        <router-link to="/admin/comics" :class="['sidebar-item', { active: $route.path.startsWith('/admin/comics') }]" @click="closeMenuOnMobile">
          <span class="item-icon">📚</span>
          <span>漫画管理</span>
        </router-link>
        <router-link to="/admin/videos" :class="['sidebar-item', { active: $route.path.startsWith('/admin/videos') }]" @click="closeMenuOnMobile">
          <span class="item-icon">🎥</span>
          <span>视频管理</span>
        </router-link>
        <router-link to="/admin/categories" :class="['sidebar-item', { active: $route.path.startsWith('/admin/categories') }]" @click="closeMenuOnMobile">
          <span class="item-icon">📁</span>
          <span>分类管理</span>
        </router-link>
      </div>
    </div>
    
    <!-- 主内容区 -->
    <div class="admin-content" :class="{ 'content-shifted': menuOpen && isMobile }">
      <div class="admin-content-header" v-if="!isMobile">
        <h1>{{ pageTitle }}</h1>
      </div>
      <div class="admin-content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const menuOpen = ref(false)
const windowWidth = ref(window.innerWidth)

const isMobile = computed(() => windowWidth.value < 768)

const pageTitle = computed(() => {
  const path = route.path
  if (path === '/admin') return '仪表盘'
  if (path.startsWith('/admin/comics')) return '漫画管理'
  if (path.startsWith('/admin/videos')) return '视频管理'
  if (path.startsWith('/admin/categories')) return '分类管理'
  return '管理中心'
})

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value
}

const closeMenuOnMobile = () => {
  if (isMobile.value) {
    menuOpen.value = false
  }
}

const handleResize = () => {
  windowWidth.value = window.innerWidth
  if (!isMobile.value) {
    menuOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f6fa;
  position: relative;
}

/* 移动端导航栏 */
.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 15px 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  z-index: 1000;
}

.menu-toggle {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  color: #333;
}

.mobile-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0;
  flex: 1;
}

/* 侧边栏 */
.admin-sidebar {
  width: 250px;
  background: white;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  padding: 20px 0;
  position: fixed;
  height: 100vh;
  overflow-y: auto;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  z-index: 900;
}

/* 移动端侧边栏打开状态 */
.admin-sidebar.sidebar-open {
  transform: translateX(0);
  box-shadow: 2px 0 20px rgba(0, 0, 0, 0.2);
}

.admin-sidebar-header {
  padding: 0 20px 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar-icon {
  font-size: 24px;
}

.admin-sidebar-header h2 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.admin-sidebar-menu {
  padding: 0 10px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #666;
  text-decoration: none;
  border-radius: 8px;
  margin-bottom: 5px;
  transition: all 0.3s ease;
}

.sidebar-item:hover {
  background: #f0f0f0;
  color: #333;
}

.sidebar-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.item-icon {
  font-size: 18px;
}

/* 主内容区 */
.admin-content {
  flex: 1;
  margin-left: 250px;
  padding: 30px;
  transition: margin-left 0.3s ease, transform 0.3s ease;
}

/* 移动端内容偏移 */
.admin-content.content-shifted {
  transform: translateX(250px);
}

.admin-content-header {
  margin-bottom: 30px;
}

.admin-content-header h1 {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.admin-content-body {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-sidebar {
    width: 250px;
    transform: translateX(-100%);
  }
  
  .admin-content {
    margin-left: 0;
    padding: 80px 20px 20px;
  }
  
  .admin-content-body {
    padding: 20px;
  }
  
  .admin-content-header {
    margin-bottom: 20px;
  }
  
  .admin-content-header h1 {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .mobile-header {
    padding: 12px 15px;
  }
  
  .menu-toggle {
    font-size: 20px;
  }
  
  .mobile-title {
    font-size: 16px;
  }
  
  .admin-content {
    padding: 70px 15px 15px;
  }
  
  .admin-content-body {
    padding: 15px;
    border-radius: 8px;
  }
  
  .admin-sidebar {
    width: 220px;
  }
  
  .admin-content.content-shifted {
    transform: translateX(220px);
  }
  
  .sidebar-item {
    padding: 10px 15px;
    font-size: 14px;
  }
  
  .item-icon {
    font-size: 16px;
  }
}

/* 桌面端样式保持不变 */
@media (min-width: 769px) {
  .admin-sidebar {
    transform: translateX(0);
  }
  
  .mobile-header {
    display: none;
  }
  
  .admin-content {
    margin-left: 250px;
    padding: 30px;
  }
}
</style>