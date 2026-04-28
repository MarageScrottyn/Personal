<template>
  <div class="comic-list">
    <div class="page-header">
      <h1>📚 漫画库</h1>
      <p>发现精彩的漫画世界</p>
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
            v-for="category in comicCategories"
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
    <div v-else-if="filteredComics.length === 0 && selectedCategory" class="empty">
      <span class="empty-icon">📭</span>
      <p>该分类下暂无漫画</p>
    </div>
    <div v-else class="comic-grid">
      <div
        v-for="comic in filteredComics"
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
        </div>
      </div>
    </div>
    <div v-if="!loading && filteredComics.length === 0 && !selectedCategory" class="empty">
      <span class="empty-icon">📭</span>
      <p>暂无漫画</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '../../api/client'

const router = useRouter()
const comics = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const selectedCategory = ref(null)

const comicCategories = computed(() => {
  const comicCategoryIds = new Set()
  comics.value.forEach(comic => {
    if (comic.categories && Array.isArray(comic.categories)) {
      comic.categories.forEach(catId => comicCategoryIds.add(catId))
    }
  })
  return categories.value.filter(cat => comicCategoryIds.has(cat.id))
})

const filteredComics = computed(() => {
  if (!selectedCategory.value) {
    return comics.value
  }
  return comics.value.filter(comic =>
    comic.categories.includes(selectedCategory.value)
  )
})

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
  router.push(`/comics/${slug}`)
}

const goToCategory = (categoryName) => {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
  padding: 0 15px;
}

.comic-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.comic-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.comic-cover {
  position: relative;
  height: 220px;
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
  padding: 8px 16px;
  background: white;
  color: #333;
  border-radius: 20px;
  font-weight: 600;
  font-size: 13px;
}

.comic-info {
  padding: 12px;
}

.comic-title {
  font-size: 14px;
  color: #2c3e50;
  margin-bottom: 8px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.comic-categories {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

  .comic-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 0 10px;
  }

  .comic-cover {
    height: 160px;
  }

  .comic-info {
    padding: 10px;
  }

  .comic-title {
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
  .comic-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .comic-cover {
    height: 150px;
  }

  .comic-info {
    padding: 8px;
  }

  .comic-title {
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .page-header {
    display: none;
  }
}
</style>