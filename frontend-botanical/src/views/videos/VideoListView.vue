<template>
  <div class="video-list">
    <div class="page-header">
      <h1 class="section-title">🎥 视频库</h1>
      <p class="page-subtitle">观看精彩视频内容</p>
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
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="filteredVideos.length === 0 && selectedCategory" class="empty-state">
      <span class="empty-icon">🌸</span>
      <p>该分类下暂无视频</p>
    </div>
    <div v-else class="video-grid">
      <div
        v-for="(video, index) in filteredVideos"
        :key="video.id"
        class="video-card"
        :style="{ animationDelay: `${index * 0.05}s` }"
        @click="goToDetail(video.slug)"
      >
        <div class="video-thumbnail">
          <img :src="getImageUrl(video.thumbnail)" :alt="video.title" />
          <div class="play-overlay">
            <span class="play-icon">▶</span>
          </div>
          <div class="card-decoration"></div>
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
    <div v-if="!loading && filteredVideos.length === 0" class="empty-state">
      <span class="empty-icon">🌿</span>
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
  padding: 1.5rem 0;
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
}

.section-title {
  font-family: var(--font-heading);
  font-size: clamp(1.8rem, 4vw, 2.5rem);
  color: var(--flora-stem);
  margin-bottom: 0.5rem;
  position: relative;
  display: inline-block;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 60%;
  height: 3px;
  background: linear-gradient(90deg, var(--flora-leaf), transparent);
  border-radius: 2px;
}

.page-subtitle {
  font-family: var(--font-body);
  color: var(--flora-stem-light);
  font-size: 1rem;
  margin-top: 0.75rem;
}

.category-filter {
  margin-bottom: 2rem;
  padding: 0 0.5rem;
  overflow: hidden;
}

.filter-tabs-wrapper {
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding-bottom: 0.75rem;
}

.filter-tabs-wrapper::-webkit-scrollbar {
  display: none;
}

.filter-tabs {
  display: flex;
  gap: 0.625rem;
  justify-content: flex-start;
  min-width: max-content;
}

.filter-tab {
  padding: 0.5rem 1.25rem;
  background: var(--flora-base-light);
  border: 1px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.9rem;
  color: var(--flora-stem);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.filter-tab:hover {
  background: var(--flora-glow);
  border-color: var(--flora-bloom);
}

.filter-tab.active {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border-color: transparent;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 1.5rem;
  gap: 1rem;
}

.loading p {
  font-family: var(--font-body);
  color: var(--flora-stem);
  font-size: 1rem;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1.5rem;
  padding: 0 0.5rem;
}

.video-card {
  background: var(--flora-base-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 20px var(--flora-shadow);
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: bloom-in 0.5s ease-out forwards;
  opacity: 0;
}

.video-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 35px var(--flora-shadow-dark);
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
  transition: transform 0.4s ease;
}

.video-card:hover .video-thumbnail img {
  transform: scale(1.08);
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    to bottom,
    rgba(123, 169, 56, 0.1) 0%,
    rgba(0, 0, 0, 0.5) 100%
  );
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
  width: 44px;
  height: 44px;
  background: var(--flora-base-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--flora-leaf);
  padding-left: 3px;
  transition: all var(--transition-fast);
}

.video-card:hover .play-icon {
  transform: scale(1.1);
  background: var(--flora-leaf);
  color: white;
}

.card-decoration {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: linear-gradient(to top, var(--flora-glow), transparent);
  pointer-events: none;
}

.video-info {
  padding: 1rem;
}

.video-title {
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--flora-stem);
  margin-bottom: 0.625rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 0.375rem;
}

.category-tag {
  padding: 0.25rem 0.75rem;
  background: var(--flora-glow);
  color: var(--flora-stem);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.category-tag:hover {
  background: var(--flora-leaf);
  color: white;
}

@media (max-width: 768px) {
  .page-header {
    display: none;
  }

  .section-title {
    font-size: 1.5rem;
  }

  .section-title::after {
    width: 40%;
  }

  .page-subtitle {
    display: none;
  }

  .video-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
    padding: 0 0.5rem;
  }

  .video-thumbnail {
    height: 120px;
  }

  .video-info {
    padding: 0.75rem;
  }

  .video-title {
    font-size: 0.875rem;
    margin-bottom: 0.5rem;
  }

  .category-tag {
    font-size: 0.7rem;
    padding: 0.2rem 0.5rem;
  }

  .filter-tabs {
    gap: 0.5rem;
  }

  .filter-tab {
    padding: 0.4rem 1rem;
    font-size: 0.85rem;
  }
}

@keyframes bloom-in {
  0% {
    opacity: 0;
    transform: scale(0.95) translateY(15px);
    filter: blur(5px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
    filter: blur(0);
  }
}
</style>