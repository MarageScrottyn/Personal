<template>
  <div class="video-list">
    <div class="page-header">
      <h1>🎥 视频库</h1>
      <p>观看精彩视频内容</p>
    </div>
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="video-grid">
      <div
        v-for="video in videos"
        :key="video.id"
        class="video-card"
        @click="goToDetail(video.slug)"
      >
        <div class="video-thumbnail">
          <img :src="getImageUrl(video.thumbnail)" :alt="video.title" />
          <span class="duration">{{ formatDuration(video.duration) }}</span>
          <div class="play-overlay">
            <span class="play-icon">▶</span>
          </div>
        </div>
        <div class="video-info">
          <h3 class="video-title">{{ video.title }}</h3>
          <div class="video-categories">
            <span 
              v-for="category in video.category_names" 
              :key="category"
              class="category-tag"
              @click.stop="goToCategory(category)"
            >
              {{ category }}
            </span>
          </div>
          <p class="video-desc">{{ video.description }}</p>
        </div>
      </div>
    </div>
    <div v-if="!loading && videos.length === 0" class="empty">
      <span class="empty-icon">📭</span>
      <p>暂无视频</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '../../api/client'

const router = useRouter()
const videos = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')

const fetchVideos = async () => {
  try {
    const response = await apiClient.get('/videos/')
    videos.value = response.data
  } catch (e) {
    error.value = e.response?.data?.detail || '获取视频列表失败'
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

const formatDuration = (seconds) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

const goToDetail = (slug) => {
  router.push(`/videos/${slug}`)
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
  await fetchVideos()
})
</script>

<style scoped>
.video-list {
  padding: 20px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.page-header p {
  color: #7f8c8d;
  font-size: 16px;
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
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 25px;
}

.video-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.video-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
}

.video-thumbnail {
  position: relative;
  height: 180px;
  overflow: hidden;
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

.duration {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 10px;
  border-radius: 5px;
  font-size: 13px;
  font-weight: 500;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.video-card:hover .play-overlay {
  opacity: 1;
}

.play-icon {
  width: 50px;
  height: 50px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #11998e;
  padding-left: 5px;
}

.video-info {
  padding: 18px;
}

.video-title {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 8px;
  font-weight: 600;
}

.video-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
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

.video-desc {
  color: #7f8c8d;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 60px;
  display: block;
  margin-bottom: 15px;
}

.empty p {
  color: #7f8c8d;
  font-size: 16px;
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 24px;
  }

  .video-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 15px;
  }

  .video-thumbnail {
    height: 150px;
  }

  .video-info {
    padding: 12px;
  }

  .video-title {
    font-size: 15px;
  }

  .video-desc {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .video-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .video-thumbnail {
    height: 180px;
  }
}
</style>