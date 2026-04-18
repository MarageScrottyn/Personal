<template>
  <div class="main-layout">
    <nav class="navbar" :class="{ 'header-hidden': isComicReadPage }">
      <div class="nav-content">
        <div class="nav-brand">
          <span class="brand-icon">🎬</span>
          <span class="brand-text">聚合空间</span>
        </div>
        <div class="nav-links">
          <router-link to="/comics" :class="['nav-link', { active: $route.path.startsWith('/comics') }]">
            <span class="link-icon">📚</span>
            <span class="link-text">漫画</span>
          </router-link>
          <router-link to="/videos" :class="['nav-link', { active: $route.path.startsWith('/videos') }]">
            <span class="link-icon">🎥</span>
            <span class="link-text">视频</span>
          </router-link>
          <router-link to="/notes" :class="['nav-link', { active: $route.path.startsWith('/notes') }]">
            <span class="link-icon">📝</span>
            <span class="link-text">笔记</span>
          </router-link>
          <router-link to="/cloud" :class="['nav-link', { active: $route.path.startsWith('/cloud') }]">
            <span class="link-icon">☁️</span>
            <span class="link-text">云盘</span>
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin" :class="['nav-link', { active: $route.path.startsWith('/admin') }]">
            <span class="link-icon">⚙️</span>
            <span class="link-text">管理</span>
          </router-link>
        </div>
        <div class="nav-user">
          <button v-if="authStore.isAdmin" @click="syncMedia" class="sync-btn">
            <span>同步媒体</span>
          </button>
          <div class="user-info" @click="openUserProfile">
            <span class="user-avatar">👤</span>
            <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
            <span class="user-arrow">▼</span>
          </div>
          <button @click="handleLogout" class="logout-btn">
            <span>退出</span>
          </button>
        </div>
        <button class="mobile-menu-btn" @click="toggleMobileMenu">
          <span class="menu-icon">{{ mobileMenuOpen ? '✕' : '☰' }}</span>
        </button>
      </div>
    </nav>
    <div :class="['mobile-menu', { open: mobileMenuOpen }]">
      <div class="mobile-nav-links">
        <router-link to="/comics" class="mobile-nav-link" @click="mobileMenuOpen = false">
          <span class="link-icon">📚</span>
          <span>漫画</span>
        </router-link>
        <router-link to="/videos" class="mobile-nav-link" @click="mobileMenuOpen = false">
          <span class="link-icon">🎥</span>
          <span>视频</span>
        </router-link>
        <router-link to="/notes" class="mobile-nav-link" @click="mobileMenuOpen = false">
          <span class="link-icon">📝</span>
          <span>笔记</span>
        </router-link>
        <router-link to="/cloud" class="mobile-nav-link" @click="mobileMenuOpen = false">
          <span class="link-icon">☁️</span>
          <span>云盘</span>
        </router-link>
        <router-link v-if="authStore.isAdmin" to="/admin" class="mobile-nav-link" @click="mobileMenuOpen = false">
          <span class="link-icon">⚙️</span>
          <span>管理</span>
        </router-link>
      </div>
      <div class="mobile-user">
        <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
        <button @click="handleLogout" class="mobile-logout-btn">退出登录</button>
      </div>
    </div>
    <main class="main-content">
      <router-view />
    </main>
  </div>
  
  <!-- 个人信息弹窗 -->
  <div v-if="showUserProfile" class="user-profile-modal" @click="closeUserProfile">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>个人信息</h3>
        <button @click="closeUserProfile" class="close-btn">✕</button>
      </div>
      <div class="modal-body">
        <div class="user-info-section">
          <div class="info-item">
            <label>用户名:</label>
            <span>{{ authStore.user?.username || '未知' }}</span>
          </div>
        </div>
        
        <div class="password-section">
          <h4>修改密码</h4>
          <form @submit.prevent="changePassword">
            <div class="form-group">
              <label for="currentPassword">当前密码:</label>
              <input type="password" id="currentPassword" v-model="passwordForm.currentPassword" required>
            </div>
            <div class="form-group">
              <label for="newPassword">新密码:</label>
              <input type="password" id="newPassword" v-model="passwordForm.newPassword" required>
            </div>
            <div class="form-group">
              <label for="confirmPassword">确认新密码:</label>
              <input type="password" id="confirmPassword" v-model="passwordForm.confirmPassword" required>
            </div>
            <button type="submit" class="submit-btn">修改密码</button>
          </form>
        </div>
        
        <div v-if="authStore.isAdmin" class="permissions-section">
          <h4>账户权限管理</h4>
          <div class="permissions-content">
            <div class="user-list">
              <div v-for="user in users" :key="user.id" class="user-item">
                <span class="user-name">{{ user.username }}</span>
                <select v-model="user.permission" @change="updateUserPermission(user)">
                  <option value="admin">管理员</option>
                  <option value="user">普通用户</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import apiClient from '../api/client'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)
const showUserProfile = ref(false)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const users = ref([])

const isComicReadPage = computed(() => {
  return route.path.includes('/read/')
})

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const syncMedia = async () => {
  try {
    const response = await apiClient.post('/sync-media/')
    alert('媒体文件同步成功！')
  } catch (error) {
    alert('同步失败：' + (error.response?.data?.error || '未知错误'))
  }
}

const closeUserProfile = () => {
  showUserProfile.value = false
}

const changePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('新密码和确认密码不一致！')
    return
  }
  
  try {
    const response = await apiClient.post('/change-password/', {
      current_password: passwordForm.value.currentPassword,
      new_password: passwordForm.value.newPassword
    })
    alert('密码修改成功！')
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error) {
    alert('密码修改失败：' + (error.response?.data?.error || '未知错误'))
  }
}

const fetchUsers = async () => {
  if (authStore.isAdmin) {
    try {
      const response = await apiClient.get('/users/')
      users.value = response.data.map(user => ({
        id: user.id,
        username: user.username,
        permission: user.profile?.user_type || 'user'
      }))
    } catch (error) {
      console.error('获取用户列表失败:', error)
    }
  }
}

const updateUserPermission = async (user) => {
  try {
    console.log('更新用户权限:', user)
    const response = await apiClient.put(`/users/${user.id}/`, {
      user_type: user.permission
    })
    console.log('权限更新成功:', response.data)
    alert('用户权限更新成功！')

    // 如果更新的是当前用户，刷新本地用户状态
    if (user.id === authStore.user?.id) {
      const updatedUser = { ...authStore.user, user_type: user.permission }
      authStore.updateUser(updatedUser)
    }

    // 刷新用户列表
    fetchUsers()
  } catch (error) {
    console.error('权限更新失败:', error)
    console.error('错误响应:', error.response?.data)
    alert('权限更新失败：' + (error.response?.data?.error || error.response?.data || '未知错误'))
    // 恢复原始权限
    fetchUsers()
  }
}

// 当弹窗打开且是管理员时，获取用户列表
const openUserProfile = () => {
  showUserProfile.value = true
  if (authStore.isAdmin) {
    fetchUsers()
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.main-layout {
  min-height: 100vh;
  background: #f5f6fa;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.navbar {
  background: white;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.navbar.header-hidden {
  transform: translateY(-100%);
  opacity: 0;
  pointer-events: none;
}

.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-icon {
  font-size: 28px;
}

.brand-text {
  font-size: 20px;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-links {
  display: flex;
  gap: 10px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  color: #666;
  text-decoration: none;
  border-radius: 10px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.nav-link:hover {
  background: #f0f0f0;
  color: #333;
}

.nav-link.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.link-icon {
  font-size: 18px;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 15px;
}

.sync-btn,
.logout-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.sync-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.sync-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.logout-btn {
  background: #f0f0f0;
  color: #666;
}

.logout-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: #f0f0f0;
}

.user-arrow {
  font-size: 12px;
  color: #666;
  transition: transform 0.3s ease;
}

.user-info:hover .user-arrow {
  transform: rotate(180deg);
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.mobile-menu-btn:hover {
  background: #f0f0f0;
}

/* 移动端菜单 */
.mobile-menu {
  display: none;
  position: fixed;
  top: 70px;
  left: 0;
  right: 0;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 99;
  padding: 20px;
  transform: translateY(-100%);
  opacity: 0;
  transition: all 0.3s ease;
}

.mobile-menu.open {
  display: block;
  transform: translateY(0);
  opacity: 1;
}

.mobile-nav-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: #666;
  text-decoration: none;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.mobile-nav-link:hover,
.mobile-nav-link.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.mobile-user {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.mobile-logout-btn {
  padding: 8px 16px;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.mobile-logout-btn:hover {
  background: #e0e0e0;
  color: #333;
}

@media (max-width: 768px) {
  .nav-links,
  .nav-user {
    display: none;
  }

  .mobile-menu-btn {
    display: block;
  }

  .nav-content {
    height: 70px;
  }

  .main-content {
    padding: 15px;
  }
}

/* 个人信息弹窗样式 */
.user-profile-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  animation: slideIn 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 20px;
}

.user-info-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.info-item label {
  font-weight: 500;
  color: #666;
}

.info-item span {
  color: #333;
  font-weight: 500;
}

.password-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.password-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #666;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.permissions-section {
  margin-top: 20px;
}

.permissions-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.user-item:hover {
  background: #f0f0f0;
}

.user-item .user-name {
  font-weight: 500;
  color: #333;
}

.user-item select {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-item select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from { transform: translateY(-20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  
  .modal-header,
  .modal-body {
    padding: 15px;
  }
  
  .user-info {
    display: none;
  }
}
</style>