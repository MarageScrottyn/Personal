<template>
  <div class="comic-list">
    <div class="page-header">
      <h1>📚 漫画库</h1>
      <p>发现精彩的漫画世界</p>
    </div>
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="comic-grid">
      <div
        v-for="comic in comics"
        :key="comic.id"
        class="comic-card"
        @click="goToDetail(comic.slug)"
      >
        <div class="comic-cover">
          <img :src="getImageUrl(comic.cover_image)" :alt="comic.title" />
          <div class="comic-overlay">
            <span class="view-btn">点击查看</span>
          </div>
        </div>
        <div class="comic-info">
          <h3 class="comic-title">{{ comic.title }}</h3>
          <div class="comic-categories">
            <span 
              v-for="category in comic.category_names" 
              :key="category"
              class="category-tag"
              @click.stop="goToCategory(category)"
            >
              {{ category }}
            </span>
          </div>
          <p class="comic-desc">{{ comic.description }}</p>
        </div>
      </div>
    </div>
    <div v-if="!loading && comics.length === 0" class="empty">
      <span class="empty-icon">📭</span>
      <p>暂无漫画</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '../../api/client'

const router = useRouter()
const comics = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')

const fetchComics = async () => {
  try {
    const response = await apiClient.get('/comics/')
    comics.value = response.data
  } catch (e) {
    error.value = e.response?.data?.detail || '获取漫画列表失败'
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

const goToDetail = (slug) => {
  router.push(`/comics/${slug}`)
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
  await fetchComics()
})
</script>

<style scoped>
.comic-list {
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
  font-size: 16px;
}

.comic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 25px;
}

.comic-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.comic-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
}

.comic-cover {
  position: relative;
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

.comic-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.comic-card:hover .comic-overlay {
  opacity: 1;
}

.view-btn {
  padding: 10px 20px;
  background: white;
  color: #333;
  border-radius: 25px;
  font-weight: 600;
  font-size: 14px;
}

.comic-info {
  padding: 18px;
}

.comic-title {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 8px;
  font-weight: 600;
}

.comic-categories {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.comic-desc {
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

  .comic-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 15px;
  }

  .comic-cover {
    height: 200px;
  }

  .comic-info {
    padding: 12px;
  }

  .comic-title {
    font-size: 15px;
  }

  .comic-desc {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .comic-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .comic-cover {
    height: 180px;
  }

  .comic-info {
    padding: 10px;
  }

  .comic-title {
    font-size: 14px;
  }
}
</style>