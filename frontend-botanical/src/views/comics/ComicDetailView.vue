<template>
  <div class="comic-detail">
    <div class="detail-header">
      <button @click="goBack" class="back-btn">
        <span class="back-icon">←</span>
        <span>返回漫画库</span>
      </button>
      <h1>{{ comic.title }}</h1>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error">
      <p>{{ error }}</p>
      <button @click="fetchComicDetail" class="retry-btn">
        重试
      </button>
    </div>

    <div v-else class="detail-content">
      <div class="comic-info">
        <div class="cover-section">
          <div class="cover-wrapper">
            <img :src="getImageUrl(comic.cover_image)" :alt="comic.title" class="cover-image" />
            <div class="cover-decoration"></div>
          </div>
          <div class="info-box">
            <h2 class="comic-title">{{ comic.title }}</h2>
            <div class="comic-categories">
              <span
                v-for="category in comic.category_names"
                :key="category"
                class="category-tag"
                @click="goToCategory(category)"
              >
                {{ category }}
              </span>
            </div>
            <p class="author">
              <span class="author-icon">✍️</span>
              作者: {{ comic.author }}
            </p>
            <p class="description">{{ comic.description }}</p>
            <button
              v-if="comic.chapters && comic.chapters.length > 0"
              @click="startReading"
              class="start-read-btn"
            >
              <span class="btn-icon">📖</span>
              {{ isMobile ? '开始阅读' : '开始阅读' }}
            </button>
          </div>
        </div>
      </div>

      <div class="chapters-section">
        <h3 class="section-title">
          <span class="section-icon">📚</span>
          章节列表
        </h3>
        <div class="chapters-list">
          <div
            v-for="chapter in comic.chapters"
            :key="chapter.id"
            class="chapter-item"
            @click="readChapter(chapter)"
          >
            <span class="chapter-title">{{ chapter.title }}</span>
            <span class="chapter-pages">
              <span class="page-icon">📄</span>
              {{ chapter.images?.length || 0 }} 页
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '../../api/client'

const router = useRouter()
const route = useRoute()
const comic = ref({})
const categories = ref([])
const loading = ref(true)
const error = ref('')
const windowWidth = ref(window.innerWidth)

const isMobile = computed(() => windowWidth.value < 768)

const fetchComicDetail = async () => {
  try {
    loading.value = true
    error.value = ''
    const response = await apiClient.get(`/comics/${route.params.slug}/`)
    comic.value = response.data
  } catch (e) {
    error.value = e.response?.data?.detail || '获取漫画详情失败'
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
  return `/media/${path}`
}

const startReading = () => {
  if (comic.value.chapters && comic.value.chapters.length > 0) {
    const firstChapter = comic.value.chapters[0]
    router.push(`/comics/${route.params.slug}/read/${firstChapter.id}`)
  }
}

const readChapter = (chapter) => {
  router.push(`/comics/${route.params.slug}/read/${chapter.id}`)
}

const goBack = () => {
  router.push('/comics')
}

const goToCategory = (categoryName) => {
  const category = categories.value.find(cat => cat.name === categoryName)
  if (category) {
    router.push(`/categories/${category.slug}`)
  }
}

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

onMounted(async () => {
  await fetchCategories()
  await fetchComicDetail()
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.comic-detail {
  padding: 20px 0;
  min-height: calc(100vh - 100px);
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
  animation: fade-up 0.5s ease-out;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--flora-base);
  border: 2px solid var(--flora-base-dark);
  padding: 10px 20px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--flora-stem);
  transition: all var(--transition-fast);
  margin-right: 20px;
}

.back-btn:hover {
  background: var(--flora-base-dark);
  transform: translateX(-5px);
}

.back-icon {
  font-size: 1.1rem;
}

.detail-header h1 {
  font-family: var(--font-heading);
  font-size: 1.75rem;
  color: var(--flora-stem);
  margin: 0;
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
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.retry-btn:hover {
  background: var(--flora-leaf-dark);
}

.detail-content {
  animation: bloom-in 0.6s ease-out;
}

.cover-section {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
  background: var(--flora-base-light);
  padding: 24px;
  border-radius: var(--radius-xl);
  box-shadow: 0 8px 30px var(--flora-shadow);
}

.cover-wrapper {
  position: relative;
  flex-shrink: 0;
}

.cover-image {
  width: 200px;
  height: 280px;
  object-fit: cover;
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 25px var(--flora-shadow-dark);
  transition: transform 0.3s ease;
}

.cover-wrapper:hover .cover-image {
  transform: scale(1.02);
}

.cover-decoration {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 60px;
  height: 60px;
  background: radial-gradient(circle, var(--flora-bloom-light) 0%, transparent 70%);
  border-radius: 50%;
  z-index: -1;
}

.info-box {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comic-title {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  color: var(--flora-stem);
  margin-bottom: 12px;
}

.comic-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.category-tag {
  padding: 6px 14px;
  background: var(--flora-base);
  color: var(--flora-stem);
  border-radius: 20px;
  font-family: var(--font-body);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid var(--flora-base-dark);
}

.category-tag:hover {
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border-color: transparent;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--flora-stem-light);
  margin-bottom: 15px;
}

.author-icon {
  font-size: 1rem;
}

.description {
  font-family: var(--font-body);
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--flora-stem);
  margin: 0 0 20px 0;
  flex: 1;
}

.start-read-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 36px;
  background: linear-gradient(135deg, var(--flora-leaf) 0%, var(--flora-leaf-dark) 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 4px 15px rgba(123, 169, 56, 0.4);
  align-self: flex-start;
}

.start-read-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(123, 169, 56, 0.5);
}

.btn-icon {
  font-size: 1.2rem;
}

.chapters-section {
  margin-bottom: 30px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-heading);
  font-size: 1.25rem;
  color: var(--flora-stem);
  margin-bottom: 20px;
}

.section-icon {
  font-size: 1.3rem;
}

.chapters-list {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 16px;
  box-shadow: 0 4px 20px var(--flora-shadow);
}

.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: 8px;
  background: var(--flora-base);
}

.chapter-item:last-child {
  margin-bottom: 0;
}

.chapter-item:hover {
  background: var(--flora-base-dark);
  transform: translateX(5px);
}

.chapter-title {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 500;
  color: var(--flora-stem);
}

.chapter-pages {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
}

.page-icon {
  font-size: 0.9rem;
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
    transform: scale(0.98);
    filter: blur(5px);
  }
  100% {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
}

@media (max-width: 768px) {
  .cover-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 20px;
  }

  .cover-image {
    width: 160px;
    height: 220px;
  }

  .info-box {
    align-items: center;
  }

  .comic-categories {
    justify-content: center;
  }

  .start-read-btn {
    align-self: center;
  }

  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .back-btn {
    margin-right: 0;
  }
}
</style>