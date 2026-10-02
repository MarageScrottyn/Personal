<template>
  <div class="main-layout">
    <nav class="navbar" :class="{ 'header-hidden': isComicReadPage }">
      <div class="nav-content">
        <div class="nav-brand">
          <span class="brand-icon">🌿</span>
          <span class="brand-text" v-if="!isMobile">芳草空间</span>
          <span class="page-title" v-else>{{ currentPageTitle }}</span>
        </div>
        <div v-if="isMobile" class="mobile-user-btn" @click="openMobileUserSheet">
          <span class="user-avatar">🌸</span>
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
            <span class="user-avatar">🌸</span>
            <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
            <span class="user-arrow">▼</span>
          </div>
          <button @click="handleLogout" class="logout-btn">
            <span>退出</span>
          </button>
        </div>
      </div>
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

  <div v-if="showMobileUserSheet" class="mobile-user-sheet" @click="closeMobileUserSheet">
    <div class="sheet-content" @click.stop>
      <div class="sheet-header">
        <h3>用户设置</h3>
        <button @click="closeMobileUserSheet" class="sheet-close-btn">✕</button>
      </div>
      <div class="sheet-body">
        <div class="user-info-card">
          <div class="user-avatar-large">🌸</div>
          <div class="user-details">
            <span class="username">{{ authStore.user?.username || '未知' }}</span>
            <span class="user-role">{{ authStore.isAdmin ? '管理员' : '普通用户' }}</span>
          </div>
        </div>

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
    return '芳草空间'
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

    if (user.id === authStore.user?.id) {
      const updatedUser = { ...authStore.user, user_type: user.permission }
      authStore.updateUser(updatedUser)
    }

    fetchUsers()
  } catch (error) {
    console.error('权限更新失败:', error)
    console.error('错误响应:', error.response?.data)
    alert('权限更新失败：' + (error.response?.data?.error || error.response?.data || '未知错误'))
    fetchUsers()
  }
}

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
.main-layout {
  min-height: 100vh;
  background: var(--flora-base);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: clamp(1.5rem, 4vw, 2.5rem);
}

.navbar {
  background: var(--flora-base-light);
  box-shadow: 0 4px 20px var(--flora-shadow);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid var(--flora-base-dark);
}

.nav-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0.875rem 1.5rem;
  gap: 2rem;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.brand-icon {
  font-size: 1.75rem;
  animation: wind-sway 5s ease-in-out infinite;
}

.brand-text {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  color: var(--flora-stem);
  letter-spacing: 0.05em;
}

.page-title {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  color: var(--flora-stem);
}

.mobile-user-btn {
  display: none;
  padding: 0.5rem;
  cursor: pointer;
}

.user-avatar {
  font-size: 1.25rem;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex: 1;
  justify-content: center;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--radius-md);
  color: var(--flora-stem);
  font-family: var(--font-body);
  font-size: 0.95rem;
  transition: all var(--transition-fast);
  position: relative;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--flora-leaf), var(--flora-bloom));
  transition: all var(--transition-normal);
  transform: translateX(-50%);
}

.nav-link:hover {
  color: var(--flora-leaf);
  background: var(--flora-glow);
}

.nav-link:hover::before {
  width: 80%;
}

.nav-link.active {
  color: var(--flora-leaf);
  background: var(--flora-glow);
}

.nav-link.active::before {
  width: 80%;
}

.link-icon {
  font-size: 1.1rem;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.sync-btn {
  background: var(--flora-leaf);
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.sync-btn:hover {
  background: var(--flora-leaf-dark);
  transform: translateY(-1px);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--flora-glow);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-info:hover {
  background: var(--flora-glow-strong);
}

.user-name {
  font-family: var(--font-body);
  font-size: 0.9rem;
  color: var(--flora-stem);
}

.user-arrow {
  font-size: 0.625rem;
  color: var(--flora-stem-light);
}

.logout-btn {
  background: transparent;
  color: var(--flora-stem);
  border: 1px solid var(--flora-stem-light);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.logout-btn:hover {
  background: var(--flora-bloom-light);
  border-color: var(--flora-bloom);
  color: var(--flora-bloom-dark);
}

.mobile-nav-container {
  display: none;
  overflow-x: auto;
  padding: 0.75rem 1rem;
  background: var(--flora-base);
  scrollbar-width: none;
}

.mobile-nav-container::-webkit-scrollbar {
  display: none;
}

.mobile-nav-scroll {
  display: flex;
  gap: 0.5rem;
  min-width: max-content;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  background: var(--flora-base-light);
  color: var(--flora-stem);
  font-family: var(--font-body);
  font-size: 0.875rem;
  white-space: nowrap;
  transition: all var(--transition-fast);
}

.mobile-nav-item.active {
  background: var(--flora-leaf);
  color: white;
}

.mobile-user-sheet {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.sheet-content {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  width: 100%;
  max-width: 500px;
  max-height: 85vh;
  overflow-y: auto;
  animation: fade-up 0.3s ease-out;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--flora-base-dark);
  position: sticky;
  top: 0;
  background: var(--flora-base-light);
}

.sheet-header h3 {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  color: var(--flora-stem);
}

.sheet-close-btn {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: var(--flora-stem);
  cursor: pointer;
  padding: 0.25rem;
}

.sheet-body {
  padding: 1.5rem;
}

.user-info-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--flora-glow);
  border-radius: var(--radius-lg);
  margin-bottom: 1.5rem;
}

.user-avatar-large {
  font-size: 2.5rem;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.username {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  color: var(--flora-stem);
}

.user-role {
  font-size: 0.875rem;
  color: var(--flora-stem-light);
}

.sheet-section {
  margin-bottom: 1rem;
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.section-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background: var(--flora-base);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.section-toggle:hover {
  background: var(--flora-base-dark);
}

.section-toggle h4 {
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--flora-stem);
}

.toggle-icon {
  font-size: 0.75rem;
  color: var(--flora-stem-light);
}

.section-content {
  padding: 1.25rem;
  background: var(--flora-base-light);
}

.form-group {
  margin-bottom: 1rem;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.95rem;
  background: white;
  color: #4a4543;
  transition: all var(--transition-fast);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--flora-leaf);
  box-shadow: 0 0 0 4px rgba(123, 169, 56, 0.1);
}

.form-group input::placeholder {
  color: var(--flora-stem-light);
}

.sheet-btn {
  width: 100%;
  padding: 0.875rem 1.5rem;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.95rem;
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-top: 0.5rem;
}

.sheet-btn.primary {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
}

.sheet-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(123, 169, 56, 0.3);
}

.sheet-btn.danger {
  background: var(--flora-bloom-light);
  color: var(--flora-bloom-dark);
}

.sheet-btn.danger:hover {
  background: var(--flora-bloom);
}

.sheet-actions {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--flora-base-dark);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.user-list-mobile {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.user-item-mobile {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: white;
  border-radius: var(--radius-md);
}

.item-username {
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--flora-stem);
}

.user-item-mobile select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.875rem;
  background: white;
  cursor: pointer;
}

.error-text {
  color: var(--flora-bloom-dark);
  font-size: 0.875rem;
  margin-top: 0.5rem;
}

.success-text {
  color: var(--flora-leaf);
  font-size: 0.875rem;
  margin-top: 0.5rem;
}

.user-profile-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-content {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: bloom-in 0.4s ease-out;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--flora-base-dark);
  position: sticky;
  top: 0;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

.modal-header h3 {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  color: var(--flora-stem);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--flora-stem);
  cursor: pointer;
  padding: 0.25rem;
  line-height: 1;
}

.modal-body {
  padding: 1.5rem;
}

.user-info-section {
  padding: 1rem;
  background: var(--flora-glow);
  border-radius: var(--radius-lg);
  margin-bottom: 1.5rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.info-item label {
  font-family: var(--font-body);
  font-size: 0.9rem;
  color: var(--flora-stem-light);
}

.info-item span {
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--flora-stem);
}

.settings-section {
  margin-bottom: 1rem;
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.settings-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background: var(--flora-base);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.settings-toggle:hover {
  background: var(--flora-base-dark);
}

.settings-toggle h4 {
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--flora-stem);
}

.toggle-arrow {
  font-size: 0.75rem;
  color: var(--flora-stem-light);
}

.settings-content {
  padding: 1.25rem;
  background: var(--flora-base-light);
}

.settings-content .form-group {
  margin-bottom: 1rem;
}

.settings-content .form-group label {
  display: block;
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem);
  margin-bottom: 0.5rem;
}

.settings-content .form-group input,
.settings-content .form-group select {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.95rem;
  background: white;
  color: #4a4543;
  transition: all var(--transition-fast);
}

.settings-content .form-group input:focus,
.settings-content .form-group select:focus {
  outline: none;
  border-color: var(--flora-leaf);
  box-shadow: 0 0 0 4px rgba(123, 169, 56, 0.1);
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.875rem 1rem;
  background: white;
  border-radius: var(--radius-md);
}

.user-item .user-name {
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--flora-stem);
}

.user-item select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-sm);
  font-family: var(--font-body);
  font-size: 0.875rem;
  background: white;
  cursor: pointer;
}

.submit-btn {
  width: 100%;
  padding: 0.875rem 1.5rem;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.95rem;
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-top: 0.5rem;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(123, 169, 56, 0.3);
}

.reset-btn {
  background: linear-gradient(135deg, var(--flora-bloom) 0%, var(--flora-bloom-dark) 100%);
}

.reset-btn:hover {
  box-shadow: 0 4px 15px rgba(233, 169, 169, 0.3);
}

.error-message {
  background: linear-gradient(135deg, var(--flora-bloom-light), #fff);
  border: 1px solid var(--flora-bloom);
  border-radius: var(--radius-md);
  padding: 0.875rem 1rem;
  color: var(--flora-bloom-dark);
  margin: 1rem 0;
  font-size: 0.875rem;
}

.success-message {
  background: linear-gradient(135deg, rgba(123, 169, 56, 0.1), #fff);
  border: 1px solid var(--flora-leaf);
  border-radius: var(--radius-md);
  padding: 0.875rem 1rem;
  color: var(--flora-leaf-dark);
  margin: 1rem 0;
  font-size: 0.875rem;
}

.header-hidden .navbar {
  transform: translateY(-100%);
  transition: transform 0.3s ease;
}

@media (max-width: 768px) {
  .nav-content {
    padding: 0.75rem 1rem;
    gap: 1rem;
  }

  .nav-links {
    display: none;
  }

  .mobile-user-btn {
    display: flex;
  }

  .nav-user {
    display: none;
  }

  .mobile-nav-container {
    display: block;
  }

  .nav-brand {
    flex: 1;
  }

  .brand-icon {
    font-size: 1.5rem;
  }

  .brand-text {
    font-size: 1.25rem;
  }
}

@keyframes wind-sway {
  0%, 100% {
    transform: translateX(0) rotate(0deg);
  }
  25% {
    transform: translateX(2px) rotate(1deg);
  }
  75% {
    transform: translateX(-2px) rotate(-1deg);
  }
}

@keyframes fade-up {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bloom-in {
  0% {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
    filter: blur(5px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
    filter: blur(0);
  }
}
</style>