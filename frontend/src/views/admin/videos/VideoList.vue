<template>
  <div class="video-list">
    <div class="list-header">
      <h2>视频管理</h2>
      <router-link to="/admin/videos/create" class="create-btn">
        <span class="btn-icon">➕</span>
        <span>新增视频</span>
      </router-link>
    </div>
    <div class="list-content">
      <div v-if="loading" class="loading">
        <span class="loading-icon">⏳</span>
        <p>加载中...</p>
      </div>
      <div v-else-if="videos.length === 0" class="empty-state">
        <span class="empty-icon">🎥</span>
        <p>暂无视频</p>
        <router-link to="/admin/videos/create" class="create-btn">
          新增视频
        </router-link>
      </div>
      <div v-else class="video-grid">
        <div v-for="video in videos" :key="video.id" class="video-card">
          <div class="video-thumbnail">
            <img :src="getImageUrl(video.thumbnail)" :alt="video.title" />
            <div class="video-duration">
              {{ formatDuration(video.duration) }}
            </div>
          </div>
          <div class="video-info">
            <h3>{{ video.title }}</h3>
            <p class="video-category">{{ video.category_name }}</p>
            <p class="video-duration-text">{{ formatDuration(video.duration) }}</p>
          </div>
          <div class="video-actions">
            <router-link :to="`/admin/videos/edit/${video.slug}`" class="action-btn edit">
              <span>编辑</span>
            </router-link>
            <button @click="confirmDelete(video)" class="action-btn delete">
              <span>删除</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- 确认删除对话框 -->
    <div v-if="showDeleteConfirm" class="delete-confirm">
      <div class="confirm-dialog">
        <h3>确认删除</h3>
        <p>确定要删除视频 <strong>{{ videoToDelete?.title }}</strong> 吗？</p>
        <div class="confirm-actions">
          <button @click="cancelDelete" class="btn cancel">取消</button>
          <button @click="deleteVideo" class="btn delete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '../../../api/client'

const getImageUrl = (path) => {
  if (!path) return '/placeholder.png'
  if (path.startsWith('http')) {
    return path
  }
  if (path.startsWith('/media/')) {
    return path
  }
  if (path.startsWith('media/')) {
    return '/' + path
  }
  return '/media/' + path
}

const videos = ref([])
const loading = ref(true)
const showDeleteConfirm = ref(false)
const videoToDelete = ref(null)

const fetchVideos = async () => {
  try {
    loading.value = true
    const response = await apiClient.get('/videos/')
    videos.value = response.data
  } catch (error) {
    console.error('获取视频列表失败:', error)
  } finally {
    loading.value = false
  }
}

const confirmDelete = (video) => {
  videoToDelete.value = video
  showDeleteConfirm.value = true
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  videoToDelete.value = null
}

const deleteVideo = async () => {
  if (!videoToDelete.value) return
  
  try {
    await apiClient.delete(`/admin/videos/${videoToDelete.value.slug}/delete/`)
    showDeleteConfirm.value = false
    // 重新获取视频列表
    await fetchVideos()
  } catch (error) {
    console.error('删除视频失败:', error)
  }
}

const formatDuration = (seconds) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

onMounted(() => {
  fetchVideos()
})
</script>

<style scoped>
.video-list {
  position: relative;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.list-header h2 {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.create-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 18px;
}

.loading {
  text-align: center;
  padding: 60px 20px;
}

.loading-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #f9f9f9;
  border-radius: 12px;
  border: 2px dashed #ddd;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 15px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
  margin: 0 0 20px 0;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.video-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
}

.video-thumbnail {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: #f0f0f0;
}

.video-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.video-card:hover .video-thumbnail img {
  transform: scale(1.05);
}

.video-duration {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.video-info {
  padding: 15px;
}

.video-info h3 {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 10px 0;
  line-height: 1.4;
}

.video-category,
.video-duration-text {
  font-size: 14px;
  color: #666;
  margin: 0 0 5px 0;
}

.video-actions {
  display: flex;
  gap: 10px;
  padding: 15px;
  border-top: 1px solid #eee;
}

.action-btn {
  flex: 1;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  text-decoration: none;
  transition: all 0.3s ease;
  cursor: pointer;
  border: none;
}

.action-btn.edit {
  background: #e3f2fd;
  color: #1976d2;
}

.action-btn.edit:hover {
  background: #bbdefb;
}

.action-btn.delete {
  background: #ffebee;
  color: #e53935;
}

.action-btn.delete:hover {
  background: #ffcdd2;
}

.delete-confirm {
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

.confirm-dialog {
  background: white;
  border-radius: 12px;
  padding: 30px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
}

.confirm-dialog h3 {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0 0 15px 0;
}

.confirm-dialog p {
  font-size: 16px;
  color: #666;
  margin: 0 0 25px 0;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.3s ease;
}

.btn.cancel {
  background: #f5f5f5;
  color: #333;
}

.btn.cancel:hover {
  background: #e0e0e0;
}

.btn.delete {
  background: #e53935;
  color: white;
}

.btn.delete:hover {
  background: #c62828;
}

@media (max-width: 768px) {
  .video-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
    padding: 0 15px;
  }
  
  .list-content {
    padding: 0 15px;
  }
  
  .video-card {
    border-radius: 8px;
  }
  
  .video-thumbnail {
    height: 160px;
  }
  
  .video-info {
    padding: 12px;
  }
  
  .video-info h3 {
    font-size: 15px;
  }
  
  .video-actions {
    padding: 12px;
    gap: 8px;
  }
  
  .action-btn {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .confirm-dialog {
    padding: 20px;
    width: 95%;
  }
  
  .confirm-dialog h3 {
    font-size: 16px;
  }
  
  .confirm-dialog p {
    font-size: 14px;
    margin-bottom: 20px;
  }
  
  .confirm-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
    padding: 10px;
  }
}
</style>