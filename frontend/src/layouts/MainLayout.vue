<template>
  <div class="main-layout">
    <nav class="navbar" :class="{ 'header-hidden': isComicReadPage }">
      <div class="nav-content">
        <div class="nav-brand">
          <span class="brand-icon">🎬</span>
          <span class="brand-text" v-if="!isMobile">聚合空间</span>
          <span class="page-title" v-else>{{ currentPageTitle }}</span>
        </div>
        <!-- 移动端用户按钮 -->
        <div v-if="isMobile" class="mobile-user-btn" @click="openMobileUserSheet">
          <span class="user-avatar">👤</span>
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
      </div>
      <!-- 移动端水平滚动导航 -->
      <div class="mobile-nav-container">
        <div class="mobile-nav-scroll">
          <router-link to="/comics" :class="['mobile-nav-item', { active: $route.path.startsWith('/comics') }]">
            <span class="link-icon">📚</span>
            <span class="link-text">漫画</span>
          </router-link>
          <router-link to="/videos" :class="['mobile-nav-item', { active: $route.path.startsWith('/videos') }]">
            <span class="link-icon">🎥</span>
            <span class="link-text">视频</span>
          </router-link>
          <router-link to="/notes" :class="['mobile-nav-item', { active: $route.path.startsWith('/notes') }]">
            <span class="link-icon">📝</span>
            <span class="link-text">笔记</span>
          </router-link>
          <router-link to="/cloud" :class="['mobile-nav-item', { active: $route.path.startsWith('/cloud') }]">
            <span class="link-icon">☁️</span>
            <span class="link-text">云盘</span>
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin" :class="['mobile-nav-item', { active: $route.path.startsWith('/admin') }]">
            <span class="link-icon">⚙️</span>
            <span class="link-text">管理</span>
          </router-link>
        </div>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
  
  <!-- 移动端用户设置底部弹窗 -->
  <div v-if="showMobileUserSheet" class="mobile-user-sheet" @click="closeMobileUserSheet">
    <div class="sheet-content" @click.stop>
      <div class="sheet-header">
        <h3>用户设置</h3>
        <button @click="closeMobileUserSheet" class="sheet-close-btn">✕</button>
      </div>
      <div class="sheet-body">
        <div class="user-info-card">
          <div class="user-avatar-large">👤</div>
          <div class="user-details">
            <span class="username">{{ authStore.user?.username || '未知' }}</span>
            <span class="user-role">{{ authStore.isAdmin ? '管理员' : '普通用户' }}</span>
          </div>
        </div>
        
        <!-- 修改密码 -->
        <div class="sheet-section">
          <div class="section-toggle" @click="mobileShowPassword = !mobileShowPassword">
            <h4>修改密码</h4>
            <span class="toggle-icon">{{ mobileShowPassword ? '▲' : '▼' }}</span>
          </div>
          <div v-if="mobileShowPassword" class="section-content">
            <form @submit.prevent="changePassword">
              <div class="form-group">
                <input type="password" v-model="passwordForm.currentPassword" placeholder="当前密码" required>
              </div>
              <div class="form-group">
                <input type="password" v-model="passwordForm.newPassword" placeholder="新密码" required>
              </div>
              <div class="form-group">
                <input type="password" v-model="passwordForm.confirmPassword" placeholder="确认新密码" required>
              </div>
              <button type="submit" class="sheet-btn primary">修改密码</button>
            </form>
          </div>
        </div>

        <!-- 账户权限管理 -->
        <div v-if="authStore.isAdmin" class="sheet-section">
          <div class="section-toggle" @click="mobileShowPermissions = !mobileShowPermissions">
            <h4>账户权限管理</h4>
            <span class="toggle-icon">{{ mobileShowPermissions ? '▲' : '▼' }}</span>
          </div>
          <div v-if="mobileShowPermissions" class="section-content">
            <div class="user-list-mobile">
              <div v-for="user in users" :key="user.id" class="user-item-mobile">
                <span class="item-username">{{ user.username }}</span>
                <select v-model="user.permission" @change="updateUserPermission(user)">
                  <option value="admin">管理员</option>
                  <option value="user">普通用户</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- 重置用户密码 -->
        <div v-if="authStore.isAdmin" class="sheet-section">
          <div class="section-toggle" @click="mobileShowResetPassword = !mobileShowResetPassword">
            <h4>重置用户密码</h4>
            <span class="toggle-icon">{{ mobileShowResetPassword ? '▲' : '▼' }}</span>
          </div>
          <div v-if="mobileShowResetPassword" class="section-content">
            <form @submit.prevent="resetUserPassword">
              <div class="form-group">
                <select v-model="resetPasswordForm.userId" required>
                  <option value="">选择用户</option>
                  <option v-for="user in users" :key="user.id" :value="user.id">
                    {{ user.username }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <input type="password" v-model="resetPasswordForm.newPassword" placeholder="新密码" required minlength="6">
              </div>
              <div class="form-group">
                <input type="password" v-model="resetPasswordForm.confirmPassword" placeholder="确认新密码" required minlength="6">
              </div>
              <div v-if="resetPasswordError" class="error-text">{{ resetPasswordError }}</div>
              <div v-if="resetPasswordSuccess" class="success-text">{{ resetPasswordSuccess }}</div>
              <button type="submit" class="sheet-btn primary">重置密码</button>
            </form>
          </div>
        </div>

        <div class="sheet-actions">
          <button v-if="authStore.isAdmin" @click="syncMedia" class="sheet-btn">
            <span>🔄</span> 同步媒体
          </button>
          <button @click="handleLogout" class="sheet-btn danger">
            <span>🚪</span> 退出登录
          </button>
        </div>
      </div>
    </div>
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
        
        <!-- 修改密码 -->
        <div class="settings-section">
          <div class="settings-toggle" @click="showPasswordSection = !showPasswordSection">
            <h4>修改密码</h4>
            <span class="toggle-arrow">{{ showPasswordSection ? '▲' : '▼' }}</span>
          </div>
          <div v-if="showPasswordSection" class="settings-content">
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
        </div>
        
        <!-- 账户权限管理 -->
        <div v-if="authStore.isAdmin" class="settings-section">
          <div class="settings-toggle" @click="showPermissionsSection = !showPermissionsSection">
            <h4>账户权限管理</h4>
            <span class="toggle-arrow">{{ showPermissionsSection ? '▲' : '▼' }}</span>
          </div>
          <div v-if="showPermissionsSection" class="settings-content">
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

        <!-- 重置用户密码 -->
        <div v-if="authStore.isAdmin" class="settings-section">
          <div class="settings-toggle" @click="showResetSection = !showResetSection">
            <h4>重置用户密码</h4>
            <span class="toggle-arrow">{{ showResetSection ? '▲' : '▼' }}</span>
          </div>
          <div v-if="showResetSection" class="settings-content">
            <form @submit.prevent="resetUserPassword">
              <div class="form-group">
                <label for="resetUserSelect">选择用户:</label>
                <select id="resetUserSelect" v-model="resetPasswordForm.userId" required>
                  <option value="">请选择用户</option>
                  <option v-for="user in users" :key="user.id" :value="user.id">
                    {{ user.username }} ({{ user.permission === 'admin' ? '管理员' : '普通用户' }})
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label for="resetNewPassword">新密码:</label>
                <input type="password" id="resetNewPassword" v-model="resetPasswordForm.newPassword" placeholder="请输入新密码（至少6个字符）" required minlength="6">
              </div>
              <div class="form-group">
                <label for="resetConfirmPassword">确认新密码:</label>
                <input type="password" id="resetConfirmPassword" v-model="resetPasswordForm.confirmPassword" placeholder="请再次输入新密码" required minlength="6">
              </div>
              <div v-if="resetPasswordError" class="error-message">{{ resetPasswordError }}</div>
              <div v-if="resetPasswordSuccess" class="success-message">{{ resetPasswordSuccess }}</div>
              <button type="submit" class="submit-btn reset-btn">重置密码</button>
            </form>
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
const showUserProfile = ref(false)
const showMobileUserSheet = ref(false)
const showPasswordSection = ref(false)
const showPermissionsSection = ref(false)
const showResetSection = ref(false)
const mobileShowPassword = ref(false)
const mobileShowPermissions = ref(false)
const mobileShowResetPassword = ref(false)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const users = ref([])
const resetPasswordForm = ref({
  userId: '',
  newPassword: '',
  confirmPassword: ''
})
const resetPasswordError = ref('')
const resetPasswordSuccess = ref('')

const isComicReadPage = computed(() => {
  return route.path.includes('/read/')
})

const isMobile = computed(() => {
  return window.innerWidth <= 768
})

const currentPageTitle = computed(() => {
  const path = route.path
  if (path.startsWith('/comics')) {
    return '漫画'
  } else if (path.startsWith('/videos')) {
    return '视频'
  } else if (path.startsWith('/notes')) {
    return '笔记'
  } else if (path.startsWith('/cloud')) {
    return '云盘'
  } else if (path.startsWith('/admin')) {
    return '管理'
  } else if (path.startsWith('/login')) {
    return '登录'
  } else {
    return '聚合空间'
  }
})

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
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

const openMobileUserSheet = () => {
  showMobileUserSheet.value = true
  document.body.style.overflow = 'hidden'
  if (authStore.isAdmin) {
    fetchUsers()
  }
}

const closeMobileUserSheet = () => {
  showMobileUserSheet.value = false
  document.body.style.overflow = ''
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

const resetUserPassword = async () => {
  resetPasswordError.value = ''
  resetPasswordSuccess.value = ''

  if (!resetPasswordForm.value.userId) {
    resetPasswordError.value = '请选择要重置密码的用户'
    return
  }

  if (!resetPasswordForm.value.newPassword) {
    resetPasswordError.value = '请输入新密码'
    return
  }

  if (resetPasswordForm.value.newPassword.length < 6) {
    resetPasswordError.value = '密码长度不能少于6个字符'
    return
  }

  if (resetPasswordForm.value.newPassword !== resetPasswordForm.value.confirmPassword) {
    resetPasswordError.value = '两次输入的密码不一致'
    return
  }

  try {
    await apiClient.post(`/admin/users/${resetPasswordForm.value.userId}/reset-password/`, {
      new_password: resetPasswordForm.value.newPassword
    })
    resetPasswordSuccess.value = '密码重置成功！'
    resetPasswordForm.value = {
      userId: '',
      newPassword: '',
      confirmPassword: ''
    }
    setTimeout(() => {
      resetPasswordSuccess.value = ''
    }, 3000)
  } catch (error) {
    console.error('重置密码失败:', error)
    console.error('错误响应:', error.response?.data)
    resetPasswordError.value = error.response?.data?.error || '重置密码失败，请稍后重试'
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
  padding: 0 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-icon {
  font-size: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-text {
  font-size: 18px;
  font-weight: bold;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  color: #666;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
  font-size: 14px;
}

.link-icon {
  font-size: 16px;
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

/* 移动端水平滚动导航 */
.mobile-nav-container {
  display: none;
  width: 100%;
  border-top: 1px solid #eee;
  background: white;
}

.mobile-nav-scroll {
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  padding: 8px 12px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.mobile-nav-scroll::-webkit-scrollbar {
  display: none;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  margin-right: 8px;
  color: #666;
  text-decoration: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  background: #f5f5f5;
  white-space: nowrap;
}

.mobile-nav-item:hover {
  background: #e0e0e0;
}

.mobile-nav-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.mobile-nav-item .link-icon {
  font-size: 16px;
}

.mobile-nav-item .link-text {
  font-size: 13px;
}

@media (max-width: 768px) {
  .nav-links,
  .nav-user {
    display: none;
  }

  .mobile-nav-container {
    display: block;
  }

  .nav-content {
    height: 45px;
    padding: 0 10px;
  }

  .main-content {
    padding: 10px;
  }

  .brand-icon {
    font-size: 20px;
  }

  .page-title {
    font-size: 14px;
    max-width: 100px;
  }

  .mobile-nav-container {
    border-top: 1px solid #eee;
  }

  .mobile-nav-scroll {
    padding: 6px 10px;
  }

  .mobile-nav-item {
    padding: 7px 14px;
    margin-right: 6px;
    font-size: 13px;
  }

  .mobile-nav-item .link-icon {
    font-size: 15px;
  }

  .mobile-nav-item .link-text {
    font-size: 12px;
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

.settings-section {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.settings-section:last-child {
  border-bottom: none;
}

.settings-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  padding: 8px 0;
  user-select: none;
}

.settings-toggle h4 {
  margin: 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.toggle-arrow {
  font-size: 12px;
  color: #999;
  transition: transform 0.2s ease;
}

.settings-content {
  padding-top: 15px;
  animation: fadeIn 0.2s ease;
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

.reset-password-section {
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.reset-btn {
  background: #f57c00;
  margin-top: 15px;
}

.reset-btn:hover {
  background: #ef6c00;
}

.error-message {
  padding: 10px;
  background: #fee;
  border-radius: 6px;
  color: #e74c3c;
  font-size: 13px;
  margin-top: 10px;
}

.success-message {
  padding: 10px;
  background: #e8f5e9;
  border-radius: 6px;
  color: #2e7d32;
  font-size: 13px;
  margin-top: 10px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from { transform: translateY(-20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.mobile-user-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  cursor: pointer;
  transition: all 0.3s ease;
}

.mobile-user-btn .user-avatar {
  font-size: 18px;
  filter: grayscale(100%) brightness(200%);
}

.mobile-user-btn:active {
  transform: scale(0.95);
}

/* 移动端用户设置底部弹窗 */
.mobile-user-sheet {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  animation: fadeIn 0.2s ease;
}

.sheet-content {
  background: white;
  border-radius: 20px 20px 0 0;
  max-height: 85vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  background: white;
  border-radius: 20px 20px 0 0;
}

.sheet-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.sheet-close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f5f5f5;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.sheet-close-btn:active {
  background: #e0e0e0;
  transform: scale(0.95);
}

.sheet-body {
  padding: 20px;
}

.user-info-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  margin-bottom: 20px;
}

.user-avatar-large {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-details .username {
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.user-details .user-role {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.sheet-section {
  margin-bottom: 16px;
  background: #f8f8f8;
  border-radius: 12px;
  overflow: hidden;
}

.section-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  user-select: none;
}

.section-toggle h4 {
  font-size: 15px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.toggle-icon {
  font-size: 11px;
  color: #999;
  transition: transform 0.2s ease;
}

.section-content {
  padding: 0 16px 14px 16px;
  animation: fadeIn 0.2s ease;
}

.sheet-section .form-group {
  margin-bottom: 12px;
}

.sheet-section input,
.sheet-section select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
  background: white;
}

.sheet-section input:focus,
.sheet-section select:focus {
  border-color: #667eea;
}

.error-text {
  color: #e74c3c;
  font-size: 13px;
  margin-bottom: 10px;
}

.success-text {
  color: #27ae60;
  font-size: 13px;
  margin-bottom: 10px;
}

.user-list-mobile {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
}

.user-item-mobile {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8f8f8;
  border-radius: 10px;
}

.item-username {
  font-size: 14px;
  color: #333;
}

.user-item-mobile select {
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  background: white;
}

.sheet-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.sheet-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f5f5f5;
  color: #333;
}

.sheet-btn:active {
  transform: scale(0.98);
}

.sheet-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.sheet-btn.danger {
  background: #ffe5e5;
  color: #e74c3c;
}

.sheet-btn span {
  font-size: 16px;
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
  
  .nav-content {
    height: 45px;
    padding: 0 10px;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .brand-icon {
    font-size: 20px;
  }
  
  .page-title {
    font-size: 14px;
    max-width: 100px;
  }
  
  .mobile-user-btn {
    width: 32px;
    height: 32px;
  }
  
  .mobile-user-btn .user-avatar {
    font-size: 16px;
  }
  
  .mobile-nav-container {
    border-top: 1px solid #eee;
  }

  .mobile-nav-scroll {
    padding: 6px 10px;
  }

  .mobile-nav-item {
    padding: 7px 14px;
    margin-right: 6px;
    font-size: 13px;
  }

  .mobile-nav-item .link-icon {
    font-size: 15px;
  }

  .mobile-nav-item .link-text {
    font-size: 12px;
  }
}
</style>