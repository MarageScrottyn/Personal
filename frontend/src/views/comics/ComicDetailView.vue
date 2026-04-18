<template>
  <div class="comic-detail">
    <div class="detail-header">
      <button @click="goBack" class="back-btn">
        ← 返回漫画库
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
          <img :src="getImageUrl(comic.cover_image)" :alt="comic.title" class="cover-image" />
          <div class="info-box">
            <h2>{{ comic.title }}</h2>
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
            <p class="author">作者: {{ comic.author }}</p>
            <p class="description">{{ comic.description }}</p>
            <button
              v-if="comic.chapters && comic.chapters.length > 0"
              @click="startReading"
              class="start-read-btn"
            >
              {{ isMobile ? '开始阅读' : '开始阅读' }}
            </button>
          </div>
        </div>
      </div>

      <div class="chapters-section">
        <h3>章节列表</h3>
        <div class="chapters-list">
          <div
            v-for="chapter in comic.chapters"
            :key="chapter.id"
            class="chapter-item"
            @click="readChapter(chapter)"
          >
            <span class="chapter-title">{{ chapter.title }}</span>
            <span class="chapter-pages">{{ chapter.images?.length || 0 }} 页</span>
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
  // 查找分类的slug
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
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.back-btn {
  background: #f5f5f5;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  margin-right: 20px;
  font-size: 14px;
  transition: background 0.3s ease;
}

.back-btn:hover {
  background: #e0e0e0;
}

.detail-header h1 {
  font-size: 28px;
  color: #2c3e50;
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
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
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
}

.retry-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s ease;
}

.retry-btn:hover {
  background: #5a6fea;
}

.cover-section {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.cover-image {
  width: 200px;
  height: 280px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.info-box {
  flex: 1;
}

.info-box h2 {
  font-size: 24px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.comic-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.author {
  font-size: 14px;
  color: #7f8c8d;
  margin-bottom: 15px;
}

.description {
  font-size: 14px;
  line-height: 1.6;
  color: #34495e;
  margin: 0 0 20px 0;
}

.start-read-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.start-read-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.chapters-section {
  margin-bottom: 30px;
}

.chapters-section h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
}

.chapters-list {
  background: white;
  border-radius: 12px;
  padding: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 5px;
}

.chapter-item:hover {
  background: #f5f5f5;
}

.chapter-item.active {
  background: #667eea;
  color: white;
}

.chapter-title {
  font-size: 16px;
  font-weight: 500;
}

.chapter-pages {
  font-size: 14px;
  opacity: 0.8;
}

.chapter-content {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.chapter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
}

.chapter-header h3 {
  font-size: 18px;
  color: #2c3e50;
  margin: 0;
}

.page-info {
  font-size: 14px;
  color: #7f8c8d;
}

.reading-area {
  text-align: center;
}

.chapter-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 8px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.navigation {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.nav-btn {
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.nav-btn:hover:not(:disabled) {
  background: #5a6fea;
  transform: translateY(-2px);
}

.nav-btn:disabled {
  background: #e0e0e0;
  color: #bdc3c7;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .back-btn {
    margin-right: 0;
  }

  .cover-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .cover-image {
    width: 150px;
    height: 200px;
  }

  .info-box h2 {
    font-size: 20px;
  }

  .start-read-btn {
    width: 100%;
    padding: 12px 24px;
    font-size: 14px;
  }

  .chapter-item {
    padding: 14px 15px;
  }

  .chapter-title {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .detail-header h1 {
    font-size: 20px;
  }

  .cover-image {
    width: 120px;
    height: 160px;
  }

  .info-box h2 {
    font-size: 18px;
  }

  .start-read-btn {
    padding: 10px 20px;
    font-size: 13px;
  }

  .chapter-item {
    padding: 12px 15px;
  }

  .chapter-title {
    font-size: 14px;
  }
}
</style>