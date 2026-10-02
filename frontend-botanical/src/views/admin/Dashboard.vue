<template>
  <div class="dashboard">
    <div class="dashboard-overview">
      <h2>系统概览</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon comics">📚</div>
          <div class="stat-content">
            <div class="stat-value">{{ comicCount }}</div>
            <div class="stat-label">漫画数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon videos">🎥</div>
          <div class="stat-content">
            <div class="stat-value">{{ videoCount }}</div>
            <div class="stat-label">视频数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon categories">📁</div>
          <div class="stat-content">
            <div class="stat-value">{{ categoryCount }}</div>
            <div class="stat-label">分类数量</div>
          </div>
        </div>
      </div>
    </div>
    <div class="dashboard-recent">
      <h2>最近更新</h2>
      <div class="recent-items">
        <div v-if="recentComics.length === 0 && recentVideos.length === 0" class="empty-state">
          <span class="empty-icon">📭</span>
          <p>暂无更新内容</p>
        </div>
        <div v-for="comic in recentComics" :key="comic.id" class="recent-item comic">
          <div class="item-icon">📚</div>
          <div class="item-info">
            <h3>{{ comic.title }}</h3>
            <p>{{ formatDate(comic.created_at) }}</p>
          </div>
          <router-link to="/admin/comics/edit/${comic.slug}" class="item-action">
            编辑
          </router-link>
        </div>
        <div v-for="video in recentVideos" :key="video.id" class="recent-item video">
          <div class="item-icon">🎥</div>
          <div class="item-info">
            <h3>{{ video.title }}</h3>
            <p>{{ formatDate(video.created_at) }}</p>
          </div>
          <router-link :to="`/admin/videos/edit/${video.slug}`" class="item-action">
            编辑
          </router-link>
        </div>
      </div>
    </div>
    <div class="dashboard-user-management">
      <h2>用户管理</h2>
      <div class="user-management-content">
        <div v-if="loadingUsers" class="loading-state">
          <span class="loading-spinner"></span>
          <p>加载中...</p>
        </div>
        <div v-else-if="users.length === 0" class="empty-state">
          <span class="empty-icon">👥</span>
          <p>暂无用户</p>
        </div>
        <div v-else class="users-list">
          <div v-for="user in users" :key="user.id" class="user-item">
            <div class="user-info">
              <div class="user-avatar">{{ user.username.charAt(0).toUpperCase() }}</div>
              <div class="user-details">
                <div class="user-name">{{ user.username }}</div>
                <div class="user-email">{{ user.email || '无邮箱' }}</div>
                <div class="user-type" :class="user.user_type">
                  {{ user.user_type === 'admin' ? '管理员' : '普通用户' }}
                </div>
              </div>
            </div>
            <div class="user-actions">
              <button @click="showResetPasswordDialog(user)" class="reset-password-btn">
                重置密码
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="showDialog" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog">
        <div class="dialog-header">
          <h3>重置密码</h3>
          <button @click="closeDialog" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <p class="dialog-info">
            为用户 <strong>{{ selectedUser?.username }}</strong> 设置新密码
          </p>
          <div class="form-group">
            <label for="new-password">新密码</label>
            <input
              id="new-password"
              v-model="newPassword"
              type="password"
              placeholder="请输入新密码（至少6个字符）"
              minlength="6"
            />
          </div>
          <div class="form-group">
            <label for="confirm-password">确认密码</label>
            <input
              id="confirm-password"
              v-model="confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              minlength="6"
            />
          </div>
          <div v-if="dialogError" class="dialog-error">
            {{ dialogError }}
          </div>
          <div v-if="dialogSuccess" class="dialog-success">
            {{ dialogSuccess }}
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="closeDialog" class="cancel-btn">取消</button>
          <button @click="resetPassword" class="submit-btn" :disabled="resetting">
            {{ resetting ? '重置中...' : '确认重置' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '../../api/client'

const comicCount = ref(0)
const videoCount = ref(0)
const categoryCount = ref(0)
const recentComics = ref([])
const recentVideos = ref([])
const users = ref([])
const loadingUsers = ref(false)
const showDialog = ref(false)
const selectedUser = ref(null)
const newPassword = ref('')
const confirmPassword = ref('')
const dialogError = ref('')
const dialogSuccess = ref('')
const resetting = ref(false)

const fetchStats = async () => {
  try {
    const [comicsRes, videosRes, categoriesRes] = await Promise.all([
      apiClient.get('/comics/'),
      apiClient.get('/videos/'),
      apiClient.get('/categories/')
    ])

    comicCount.value = comicsRes.data.length
    videoCount.value = videosRes.data.length
    categoryCount.value = categoriesRes.data.length

    recentComics.value = comicsRes.data
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
      .slice(0, 3)

    recentVideos.value = videosRes.data
      .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
      .slice(0, 3)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchUsers = async () => {
  loadingUsers.value = true
  try {
    const response = await apiClient.get('/users/')
    users.value = response.data
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loadingUsers.value = false
  }
}

const showResetPasswordDialog = (user) => {
  selectedUser.value = user
  newPassword.value = ''
  confirmPassword.value = ''
  dialogError.value = ''
  dialogSuccess.value = ''
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
  selectedUser.value = null
  newPassword.value = ''
  confirmPassword.value = ''
  dialogError.value = ''
  dialogSuccess.value = ''
}

const resetPassword = async () => {
  dialogError.value = ''
  dialogSuccess.value = ''

  if (!newPassword.value) {
    dialogError.value = '请输入新密码'
    return
  }

  if (newPassword.value.length < 6) {
    dialogError.value = '密码长度不能少于6个字符'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    dialogError.value = '两次输入的密码不一致'
    return
  }

  resetting.value = true
  try {
    await apiClient.post(`/admin/users/${selectedUser.value.id}/reset-password/`, {
      user_id: selectedUser.value.id,
      new_password: newPassword.value
    })
    dialogSuccess.value = `用户${selectedUser.value.username}的密码已成功重置`
    setTimeout(() => {
      closeDialog()
    }, 1500)
  } catch (error) {
    dialogError.value = error.response?.data?.error || '重置密码失败，请稍后重试'
  } finally {
    resetting.value = false
  }
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchStats()
  fetchUsers()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.dashboard-overview h2,
.dashboard-recent h2 {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 20px;
  box-shadow: 0 4px 15px var(--flora-shadow);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px var(--flora-shadow);
}

.stat-icon {
  font-size: 32px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.comics {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
}

.stat-icon.videos {
  background: linear-gradient(135deg, var(--flora-bloom) 0%, var(--flora-bloom-dark) 100%);
  color: white;
}

.stat-icon.categories {
  background: linear-gradient(135deg, var(--flora-stem) 0%, var(--flora-stem-light) 100%);
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--flora-stem);
}

.stat-label {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
  margin-top: 5px;
}

.recent-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.recent-item {
  background: var(--flora-base-light);
  border-radius: var(--radius-lg);
  padding: 15px;
  box-shadow: 0 2px 10px var(--flora-shadow);
  display: flex;
  align-items: center;
  gap: 15px;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.recent-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px var(--flora-shadow);
}

.recent-item .item-icon {
  font-size: 24px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recent-item.comic .item-icon {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
}

.recent-item.video .item-icon {
  background: linear-gradient(135deg, var(--flora-bloom) 0%, var(--flora-bloom-dark) 100%);
  color: white;
}

.item-info {
  flex: 1;
}

.item-info h3 {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0 0 5px 0;
}

.item-info p {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
  margin: 0;
}

.item-action {
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  text-decoration: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.item-action:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  border: 2px dashed var(--flora-base-dark);
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
}

.empty-state p {
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--flora-stem);
  margin: 0;
}

.dashboard-user-management {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 20px;
  box-shadow: 0 4px 15px var(--flora-shadow);
}

.dashboard-user-management h2 {
  font-family: var(--font-heading);
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin-bottom: 20px;
}

.user-management-content {
  min-height: 200px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid var(--flora-base-dark);
  border-top-color: var(--flora-leaf);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  background: var(--flora-base);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.user-item:hover {
  background: var(--flora-base-dark);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 1.25rem;
  font-weight: bold;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: bold;
  color: var(--flora-stem);
}

.user-email {
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: var(--flora-stem-light);
}

.user-type {
  font-family: var(--font-body);
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  display: inline-block;
  width: fit-content;
}

.user-type.admin {
  background: var(--flora-base);
  color: var(--flora-leaf);
}

.user-type.user {
  background: var(--flora-base);
  color: var(--flora-stem);
}

.user-actions {
  display: flex;
  gap: 10px;
}

.reset-password-btn {
  padding: 8px 16px;
  background: var(--flora-stem);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.reset-password-btn:hover {
  background: var(--flora-stem-light);
  transform: translateY(-2px);
}

.dialog-overlay {
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
}

.dialog {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 420px;
  box-shadow: 0 20px 60px var(--flora-shadow);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 25px;
  border-bottom: 1px solid var(--flora-base-dark);
}

.dialog-header h3 {
  font-family: var(--font-heading);
  font-size: 1.125rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.75rem;
  color: var(--flora-stem-light);
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: var(--flora-stem);
}

.dialog-body {
  padding: 25px;
}

.dialog-info {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem);
  margin-bottom: 20px;
}

.dialog-info strong {
  color: var(--flora-stem);
}

.dialog-body .form-group {
  margin-bottom: 15px;
}

.dialog-body .form-group label {
  display: block;
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--flora-stem);
  margin-bottom: 8px;
}

.dialog-body .form-group input {
  width: 100%;
  padding: 12px 14px;
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
  background: var(--flora-base);
  color: var(--flora-stem);
}

.dialog-body .form-group input:focus {
  outline: none;
  border-color: var(--flora-leaf);
}

.dialog-error {
  padding: 12px;
  background: var(--flora-base);
  border-radius: var(--radius-md);
  color: var(--flora-bloom-dark);
  font-family: var(--font-body);
  font-size: 0.875rem;
  margin-top: 15px;
}

.dialog-success {
  padding: 12px;
  background: var(--flora-base);
  border-radius: var(--radius-md);
  color: var(--flora-leaf);
  font-family: var(--font-body);
  font-size: 0.875rem;
  margin-top: 15px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px 25px;
  border-top: 1px solid var(--flora-base-dark);
}

.cancel-btn {
  padding: 10px 20px;
  background: var(--flora-base);
  color: var(--flora-stem);
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.cancel-btn:hover {
  background: var(--flora-base-dark);
}

.submit-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px var(--flora-shadow);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-card {
    flex-direction: column;
    text-align: center;
  }

  .recent-item {
    flex-direction: column;
    text-align: center;
  }
}
</style>