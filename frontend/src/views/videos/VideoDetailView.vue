<template>
  <div class="video-detail">
    <button class="back-btn" @click="goBack">
      <span>←</span> 返回
    </button>
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="video" class="video-content">
      <div class="video-player">
        <video
          v-if="getVideoUrl(video.video_file)"
          :src="getVideoUrl(video.video_file)"
          controls
          autoplay
        ></video>
        <div v-else class="no-video">
          <span class="empty-icon">🎬</span>
          <p>暂无视频</p>
        </div>
      </div>
      <div class="video-info">
        <h1>{{ video.title }}</h1>
        <div class="meta-info">
          <div class="categories-list">
            <span class="meta-icon">📂</span>
            <span 
              v-for="category in video.category_names" 
              :key="category"
              class="category-tag"
              @click="goToCategory(category)"
            >
              {{ category }}
            </span>
          </div>
          <span class="meta-item">
            <span class="meta-icon">⏱️</span>
            {{ formatDuration(video.duration) }}
          </span>
        </div>
        <p class="video-desc">{{ video.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '../../api/client'

const route = useRoute()
const router = useRouter()
const video = ref(null)
const categories = ref([])
const loading = ref(true)
const error = ref('')

const fetchVideo = async () => {
  try {
    const response = await apiClient.get(`/videos/${route.params.slug}/`)
    video.value = response.data
  } catch (e) {
    error.value = e.response?.data?.detail || '获取视频详情失败'
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/categories/')
    categories.value = response.data
  } catch (e) {
    console.error('获取分类列表失败:', e)
  }
}

const getVideoUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) {
    // 处理完整URL中的重复/media前缀
    return path.replace('/media/media/', '/media/')
  }
  // 处理相对路径
  let cleanPath = path
  cleanPath = cleanPath.replace(/^\/media\/*/g, '')
  return `/media/${cleanPath}`
}

const formatDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const goBack = () => {
  router.push('/videos')
}

const goToCategory = (categoryName) => {
  // 查找分类的slug
  const category = categories.value.find(cat => cat.name === categoryName)
  if (category) {
    router.push(`/categories/${category.slug}`)
  }
}

onMounted(async () => {
  await fetchCategories()
  await fetchVideo()
})
</script>

<style scoped>
.video-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: white;
  color: #333;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.back-btn:hover {
  background: #11998e;
  color: white;
  border-color: #11998e;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 15px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #11998e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error {
  text-align: center;
  padding: 40px;
  color: #e74c3c;
  font-size: 16px;
  background: white;
  border-radius: 16px;
}

.video-content {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.video-player {
  width: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.video-player video {
  width: 100%;
  max-height: 70vh;
  outline: none;
}

.no-video {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  color: #999;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.no-video p {
  font-size: 18px;
}

.video-info {
  padding: 30px;
}

.video-info h1 {
  font-size: 26px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.categories-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.category-tag {
  padding: 4px 12px;
  background: #f0f0f0;
  color: #333;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tag:hover {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.meta-icon {
  font-size: 14px;
}

.video-desc {
  color: #555;
  line-height: 1.8;
  font-size: 15px;
}

@media (max-width: 768px) {
  .video-player {
    min-height: 250px;
  }

  .video-info {
    padding: 20px;
  }

  .video-info h1 {
    font-size: 20px;
  }
}
</style>