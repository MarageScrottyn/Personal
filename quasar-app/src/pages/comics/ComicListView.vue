<template>
  <div class="comic-list">
    <div class="page-header">
      <div class="header-content">
        <h1>{{ t('menu.comics') }}</h1>
        <span class="count">{{ filteredComics.length }} {{ t('common.items') }}</span>
      </div>
      <div class="header-actions">
        <div class="search-box">
          <SearchIcon class="search-icon" />
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="t('common.search')"
          />
        </div>
      </div>
    </div>

    <div class="category-filter">
      <button
        :class="['filter-tab', { active: selectedCategory === null }]"
        @click="selectCategory(null)"
      >
        {{ t('common.all') }}
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

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <div v-else-if="error" class="error-state">
      <AlertCircleIcon :size="48" />
      <p>{{ error }}</p>
      <button @click="fetchComics" class="retry-btn">{{ t('common.retry') }}</button>
    </div>

    <div v-else-if="filteredComics.length === 0" class="empty-state">
      <FolderIcon :size="64" />
      <h3>{{ t('common.noData') }}</h3>
      <p>{{ t('common.noDataHint') }}</p>
    </div>

    <div v-else class="comic-grid">
      <div
        v-for="comic in filteredComics"
        :key="comic.id"
        class="comic-card"
        @click="goToDetail(comic.slug)"
      >
        <div class="card-cover">
          <img :src="getImageUrl(comic.cover_image)" :alt="comic.title" />
          <div class="card-overlay">
            <span class="view-text">{{ t('common.view') }}</span>
          </div>
        </div>
        <div class="card-info">
          <h3 class="card-title">{{ comic.title }}</h3>
          <div class="card-meta">
            <span
              v-for="cat in comic.category_names?.slice(0, 2)"
              :key="cat"
              class="meta-tag"
            >
              {{ cat }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import apiClient from '../../api/client'

const router = useRouter()
const { t } = useI18n()

const comics = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const selectedCategory = ref(null)
const searchQuery = ref('')

const SearchIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '11', cy: '11', r: '8' }),
      h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
    ])
  }
}

const AlertCircleIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('line', { x1: '12', y1: '8', x2: '12', y2: '12' }),
      h('line', { x1: '12', y1: '16', x2: '12.01', y2: '16' })
    ])
  }
}

const FolderIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 24
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z' })
    ])
  }
}

const comicCategories = computed(() => {
  if (!Array.isArray(comics.value) || !Array.isArray(categories.value)) {
    return []
  }
  const comicCategoryIds = new Set()
  comics.value.forEach(comic => {
    if (comic.categories && Array.isArray(comic.categories)) {
      comic.categories.forEach(catId => comicCategoryIds.add(catId))
    }
  })
  return categories.value.filter(cat => comicCategoryIds.has(cat.id))
})

const filteredComics = computed(() => {
  if (!Array.isArray(comics.value)) {
    return []
  }
  let result = comics.value

  if (selectedCategory.value) {
    result = result.filter(comic =>
      comic.categories && comic.categories.includes(selectedCategory.value)
    )
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(comic =>
      comic.title.toLowerCase().includes(query) ||
      comic.category_names?.some(cat => cat.toLowerCase().includes(query))
    )
  }

  return result
})

const fetchComics = async () => {
  try {
    loading.value = true
    error.value = ''
    const response = await apiClient.get('/comics/')
    const data = response.data.results || response.data
    comics.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.response?.data?.detail || t('common.fetchError')
    comics.value = []
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/categories/')
    const data = response.data.results || response.data
    categories.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Failed to fetch categories:', e)
    categories.value = []
  }
}

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
}

const getImageUrl = (path) => {
  if (!path) return '/placeholder.png'
  if (path.startsWith('http')) return path
  if (path.startsWith('/media/')) return path
  if (path.startsWith('media/')) return '/' + path
  return '/media/' + path
}

const goToDetail = (slug) => {
  router.push(`/comics/${slug}`)
}

onMounted(async () => {
  await Promise.all([fetchCategories(), fetchComics()])
})
</script>

<style scoped>
.comic-list {
  padding: 24px 32px;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.header-content {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.header-content h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.count {
  font-size: 14px;
  color: var(--text-tertiary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  width: 18px;
  height: 18px;
  color: var(--text-tertiary);
  pointer-events: none;
}

.search-box input {
  width: 280px;
  padding: 10px 12px 10px 40px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.search-box input::placeholder {
  color: var(--text-tertiary);
}

.search-box input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.category-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.filter-tab:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}

.filter-tab.active {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  color: white;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-default);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--text-tertiary);
  text-align: center;
}

.error-state svg,
.empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.error-state h3,
.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.error-state p,
.empty-state p {
  font-size: 14px;
  margin-bottom: 20px;
}

.retry-btn {
  padding: 10px 20px;
  background: var(--accent-primary);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: white;
  transition: background var(--transition-fast);
}

.retry-btn:hover {
  background: var(--accent-hover);
}

.comic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
}

.comic-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.comic-card:hover {
  border-color: var(--accent-primary);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.card-cover {
  position: relative;
  height: 280px;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.comic-card:hover .card-cover img {
  transform: scale(1.05);
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.comic-card:hover .card-overlay {
  opacity: 1;
}

.view-text {
  padding: 10px 20px;
  background: var(--accent-primary);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.card-info {
  padding: 16px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.meta-tag {
  padding: 4px 10px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  font-size: 11px;
  color: var(--text-secondary);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>