<template>
  <div class="category-browse-page">
    <div class="page-header">
      <h1>{{ categoryName || '分类浏览' }}</h1>
      <p v-if="categoryDescription" class="category-description">{{ categoryDescription }}</p>
    </div>

    <div class="category-nav">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error">
      <p>{{ error }}</p>
      <button @click="fetchContent" class="retry-btn">重试</button>
    </div>

    <div v-else>
      <div v-if="activeTab === 'comics'" class="content-section">
        <div v-if="comics.length === 0" class="empty-state">
          <div class="empty-icon">📚</div>
          <p>该分类暂无漫画</p>
        </div>
        <div v-else class="comics-grid">
          <div
            v-for="comic in comics"
            :key="comic.id"
            class="comic-card"
            @click="navigateToComic(comic.slug)"
          >
            <div class="comic-cover">
              <img :src="getImageUrl(comic.cover_image)" :alt="comic.title" />
            </div>
            <div class="comic-info">
              <h3 class="comic-title">{{ comic.title }}</h3>
              <p class="comic-author">{{ comic.author }}</p>
              <div class="comic-categories">
                <span
                  v-for="category in comic.category_names"
                  :key="category"
                  class="category-tag"
                  @click.stop="navigateToCategory(category)"
                >
                  {{ category }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'videos'" class="content-section">
        <div v-if="videos.length === 0" class="empty-state">
          <div class="empty-icon">🎬</div>
          <p>该分类暂无视频</p>
        </div>
        <div v-else class="videos-grid">
          <div
            v-for="video in videos"
            :key="video.id"
            class="video-card"
            @click="navigateToVideo(video.slug)"
          >
            <div class="video-thumbnail">
              <img :src="getImageUrl(video.thumbnail)" :alt="video.title" />
            </div>
            <div class="video-info">
              <h3 class="video-title">{{ video.title }}</h3>
              <div class="video-categories">
                <span
                  v-for="category in video.category_names"
                  :key="category"
                  class="category-tag"
                  @click.stop="navigateToCategory(category)"
                >
                  {{ category }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '../../api/client'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const error = ref('')
const activeTab = ref('comics')
const comics = ref([])
const videos = ref([])
const categories = ref([])

const categorySlug = computed(() => route.params.slug)

const categoryName = computed(() => {
  const category = categories.value.find(cat => cat.slug === categorySlug.value)
  return category ? category.name : ''
})

const categoryDescription = computed(() => {
  return ''
})

const tabs = [
  { label: '漫画', value: 'comics' },
  { label: '视频', value: 'videos' }
]

const fetchContent = async () => {
  try {
    loading.value = true
    error.value = ''

    const categoriesResponse = await apiClient.get('/categories/')
    categories.value = categoriesResponse.data

    const comicsResponse = await apiClient.get(`/comics/?category=${categorySlug.value}`)
    comics.value = comicsResponse.data

    const videosResponse = await apiClient.get(`/videos/?category=${categorySlug.value}`)
    videos.value = videosResponse.data

  } catch (e) {
    error.value = e.response?.data?.detail || '加载失败'
  } finally {
    loading.value = false
  }
}

const getImageUrl = (path) => {
  if (!path) return ''
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

const _formatDuration = (seconds) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

const navigateToComic = (slug) => {
  router.push(`/comics/${slug}`)
}

const navigateToVideo = (slug) => {
  router.push(`/videos/${slug}`)
}

const navigateToCategory = (categoryName) => {
  const category = categories.value.find(cat => cat.name === categoryName)
  if (category) {
    router.push(`/categories/${category.slug}`)
  }
}

onMounted(fetchContent)
</script>

<style scoped>
.category-browse-page {
  padding: 20px 0;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
}

.page-header h1 {
  font-family: var(--font-heading);
  font-size: 2rem;
  color: var(--flora-stem);
  margin-bottom: 10px;
}

.category-description {
  font-family: var(--font-body);
  color: var(--flora-stem-light);
  font-size: 1rem;
  max-width: 800px;
  margin: 0 auto;
}

.category-nav {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.tab-btn {
  padding: 12px 24px;
  background: var(--flora-base);
  border: 2px solid var(--flora-base-dark);
  border-radius: 25px;
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  color: var(--flora-stem);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-btn:hover {
  background: var(--flora-base-dark);
}

.tab-btn.active {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(123, 169, 56, 0.4);
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
  border: 3px solid var(--flora-base-dark);
  border-top-color: var(--flora-leaf);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error {
  text-align: center;
  padding: 40px;
  color: var(--flora-bloom-dark);
}

.retry-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: var(--flora-leaf);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.retry-btn:hover {
  background: var(--flora-leaf-dark);
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: var(--flora-base);
  border-radius: var(--radius-xl);
  border: 2px dashed var(--flora-base-dark);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.empty-state p {
  font-family: var(--font-body);
  font-size: 1.125rem;
  color: var(--flora-stem-light);
  margin: 0;
}

.content-section {
  margin-top: 20px;
}

.comics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.comic-card {
  background: var(--flora-base-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 20px var(--flora-shadow);
  cursor: pointer;
  transition: all var(--transition-fast);
  animation: fade-up 0.5s ease-out forwards;
}

.comic-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px var(--flora-shadow-dark);
}

.comic-cover {
  width: 100%;
  height: 280px;
  overflow: hidden;
}

.comic-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.comic-card:hover .comic-cover img {
  transform: scale(1.05);
}

.comic-info {
  padding: 15px;
}

.comic-title {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 600;
  color: var(--flora-stem);
  margin: 0 0 5px 0;
  line-height: 1.4;
}

.comic-author {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
  margin: 0 0 10px 0;
}

.comic-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 10px;
}

.category-tag {
  padding: 4px 10px;
  background: var(--flora-base);
  color: var(--flora-stem);
  border-radius: 12px;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.category-tag:hover {
  background: var(--flora-leaf);
  color: white;
}

.videos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.video-card {
  background: var(--flora-base-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 20px var(--flora-shadow);
  cursor: pointer;
  transition: all var(--transition-fast);
  animation: fade-up 0.5s ease-out forwards;
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px var(--flora-shadow-dark);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  height: 160px;
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

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
}

.video-info {
  padding: 15px;
}

.video-title {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 600;
  color: var(--flora-stem);
  margin: 0 0 10px 0;
  line-height: 1.4;
}

.video-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
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

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 1.25rem;
  }

  .category-nav {
    flex-direction: row;
    justify-content: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  .tab-btn {
    flex: 1;
    min-width: calc(50% - 10px);
    max-width: calc(50% - 10px);
    padding: 10px 16px;
    font-size: 0.875rem;
  }

  .comics-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .comic-cover {
    height: 160px;
  }

  .comic-info {
    padding: 10px;
  }

  .comic-title {
    font-size: 0.875rem;
  }

  .comic-author {
    font-size: 0.75rem;
  }

  .videos-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .video-thumbnail {
    height: 100px;
  }

  .video-info {
    padding: 10px;
  }

  .video-title {
    font-size: 0.875rem;
  }
}
</style>