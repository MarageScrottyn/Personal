<template>
  <div class="video-list">
    <div class="page-header">
      <h1>🎥 视频库</h1>
      <p>观看精彩视频内容</p>
    </div>

    <div class="category-filter">
      <div class="filter-tabs-wrapper">
        <div class="filter-tabs">
          <button
            :class="['filter-tab', { active: selectedCategory === null }]"
            @click="selectCategory(null)"
          >
            全部
          </button>
          <button
            v-for="category in videoCategories"
            :key="category.id"
            :class="['filter-tab', { active: selectedCategory === category.id }]"
            @click="selectCategory(category.id)"
          >
            {{ category.name }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="filteredVideos.length === 0 && selectedCategory" class="empty">
      <span class="empty-icon">📭</span>
      <p>该分类下暂无视频</p>
    </div>
    <div v-else class="video-grid">
      <div
        v-for="video in filteredVideos"
        :key="video.id"
        class="video-card"
        @click="goToDetail(video.slug)"
      >
        <div class="video-thumbnail">
          <img :src="getImageUrl(video.thumbnail)" :alt="video.title" />
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
        </div>
      </div>
    </div>
    <div v-if="!loading && filteredVideos.length === 0" class="empty">
      <span class="empty-icon">📭</span>
      <p>暂无视频</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '../../api/client'

const router = useRouter()
const videos = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const selectedCategory = ref(null)

const videoCategories = computed(() => {
  const videoCategoryIds = new Set()
  videos.value.forEach(video => {
    if (video.categories && Array.isArray(video.categories)) {
      video.categories.forEach(catId => videoCategoryIds.add(catId))
    }
  })
  return categories.value.filter(cat => videoCategoryIds.has(cat.id))
})

const filteredVideos = computed(() => {
  if (!selectedCategory.value) {
    return videos.value
  }
  return videos.value.filter(video =>
    video.categories.includes(selectedCategory.value)
  )
})

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

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
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

const goToDetail = (slug) => {
  router.push(`/videos/${slug}`)
}

const goToCategory = (categoryName) => {
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
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 8px;
}

.page-header p {
  color: #7f8c8d;
  font-size: 14px;
}

.category-filter {
  margin-bottom: 25px;
  padding: 0 15px;
  overflow: hidden;
}

.filter-tabs-wrapper {
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding-bottom: 10px;
}

.filter-tabs-wrapper::-webkit-scrollbar {
  display: none;
}

.filter-tabs {
  display: flex;
  gap: 10px;
  justify-content: flex-start;
  min-width: max-content;
}

.filter-tab {
  padding: 8px 20px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.filter-tab:hover {
  background: #e8e8e8;
}

.filter-tab.active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
  border-color: transparent;
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
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  padding: 0 15px;
}

.video-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.video-thumbnail {
  position: relative;
  height: 150px;
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
  width: 40px;
  height: 40px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #11998e;
  padding-left: 3px;
}

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 15px;
  color: #2c3e50;
  margin-bottom: 8px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-tag {
  padding: 2px 10px;
  background: #f0f0f0;
  color: #666;
  border-radius: 12px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tag:hover {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
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
    font-size: 22px;
  }

  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 0 10px;
  }

  .video-thumbnail {
    height: 120px;
  }

  .video-info {
    padding: 10px;
  }

  .video-title {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .category-tag {
    font-size: 10px;
    padding: 2px 8px;
  }

  .filter-tabs {
    gap: 8px;
  }

  .filter-tab {
    padding: 6px 14px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .video-thumbnail {
    height: 110px;
  }

  .video-info {
    padding: 8px;
  }

  .video-title {
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .page-header {
    display: none;
  }
}
</style>