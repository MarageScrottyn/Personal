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
          </div>
          <div class="video-info">
            <h3>{{ video.title }}</h3>
            <p class="video-category">{{ video.category_name }}</p>
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
    videos.value = response.data.results || response.data
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
    await fetchVideos()
  } catch (error) {
    console.error('删除视频失败:', error)
  }
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
  font-family: var(--font-heading);
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--flora-bloom) 0%, var(--flora-bloom-dark) 100%);
  color: white;
  text-decoration: none;
  border-radius: var(--radius-lg);
  font-family: var(--font-body);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.create-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px var(--flora-shadow);
}

.btn-icon {
  font-size: 1.125rem;
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
  margin: 0 0 20px 0;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.video-card {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: 0 4px 15px var(--flora-shadow);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px var(--flora-shadow);
}

.video-thumbnail {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: var(--flora-base);
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

.video-info {
  padding: 15px;
}

.video-info h3 {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0 0 10px 0;
  line-height: 1.4;
}

.video-category {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
  margin: 0 0 5px 0;
}

.video-actions {
  display: flex;
  gap: 10px;
  padding: 15px;
  border-top: 1px solid var(--flora-base-dark);
}

.action-btn {
  flex: 1;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  text-align: center;
  text-decoration: none;
  transition: all var(--transition-fast);
  cursor: pointer;
  border: none;
}

.action-btn.edit {
  background: var(--flora-base);
  color: var(--flora-bloom);
}

.action-btn.edit:hover {
  background: var(--flora-base-dark);
}

.action-btn.delete {
  background: var(--flora-base);
  color: var(--flora-bloom-dark);
}

.action-btn.delete:hover {
  background: var(--flora-base-dark);
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
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 30px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 60px var(--flora-shadow);
}

.confirm-dialog h3 {
  font-family: var(--font-heading);
  font-size: 1.125rem;
  font-weight: bold;
  color: var(--flora-stem);
  margin: 0 0 15px 0;
}

.confirm-dialog p {
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--flora-stem);
  margin: 0 0 25px 0;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all var(--transition-fast);
}

.btn.cancel {
  background: var(--flora-base);
  color: var(--flora-stem);
}

.btn.cancel:hover {
  background: var(--flora-base-dark);
}

.btn.delete {
  background: var(--flora-bloom-dark);
  color: white;
}

.btn.delete:hover {
  background: var(--flora-bloom);
}

@media (max-width: 768px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 0 12px;
  }

  .list-content {
    padding: 0 12px;
  }

  .create-btn {
    padding: 10px 20px;
    font-size: 0.875rem;
  }

  .btn-icon {
    font-size: 1rem;
  }

  .video-card {
    border-radius: var(--radius-lg);
  }

  .video-thumbnail {
    height: 120px;
  }

  .video-info {
    padding: 10px;
  }

  .video-info h3 {
    font-size: 0.875rem;
    margin-bottom: 8px;
  }

  .video-category {
    font-size: 0.75rem;
  }

  .video-actions {
    padding: 10px;
    gap: 6px;
  }

  .action-btn {
    padding: 5px 10px;
    font-size: 0.75rem;
  }

  .confirm-dialog {
    padding: 20px;
    width: 95%;
  }

  .confirm-dialog h3 {
    font-size: 1rem;
  }

  .confirm-dialog p {
    font-size: 0.875rem;
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

@media (max-width: 480px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .video-thumbnail {
    height: 100px;
  }

  .list-header h2 {
    font-size: 1.125rem;
  }

  .video-info h3 {
    font-size: 0.8rem;
  }

  .video-category {
    font-size: 0.7rem;
  }
}
</style>