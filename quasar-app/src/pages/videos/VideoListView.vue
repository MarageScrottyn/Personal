<template>
  <div class="video-list">
    <div class="page-header">
      <div class="header-content">
        <h1>{{ t('menu.videos') }}</h1>
        <span class="count">{{ filteredVideos.length }} {{ t('common.items') }}</span>
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
        v-for="category in videoCategories"
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
      <button @click="fetchVideos" class="retry-btn">{{ t('common.retry') }}</button>
    </div>

    <div v-else-if="filteredVideos.length === 0" class="empty-state">
      <VideoOffIcon :size="64" />
      <h3>{{ t('common.noData') }}</h3>
      <p>{{ t('common.noDataHint') }}</p>
    </div>

    <div v-else class="video-grid">
      <div
        v-for="video in filteredVideos"
        :key="video.id"
        class="video-card"
        @click="goToDetail(video.slug)"
      >
        <div class="card-cover">
          <img :src="getImageUrl(video.thumbnail)" :alt="video.title" />
          <div class="play-btn">
            <PlayIcon />
          </div>
        </div>
        <div class="card-info">
          <h3 class="card-title">{{ video.title }}</h3>
          <div class="card-meta">
            <span
              v-for="cat in video.category_names?.slice(0, 2)"
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

const videos = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const selectedCategory = ref(null)
const searchQuery = ref('')

const SearchIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 18
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('circle', { cx: '11', cy: '11', r: '8' }),
      h('line', { x1: '21', y1: '21', x2: '16.65', y2: '16.65' })
    ])
  }
}

const PlayIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'currentColor',
      width: iconSize,
      height: iconSize
    }, [
      h('polygon', { points: '5 3 19 12 5 21 5 3' })
    ])
  }
}

const AlertCircleIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 48
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('line', { x1: '12', y1: '8', x2: '12', y2: '12' }),
      h('line', { x1: '12', y1: '16', x2: '12.01', y2: '16' })
    ])
  }
}

const VideoOffIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 64
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M16 16v1a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h2m5.66 0H14a2 2 0 0 1 2 2v3.34l1 1L23 7v10' }),
      h('line', { x1: '1', y1: '1', x2: '23', y2: '23' })
    ])
  }
}

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
  let result = videos.value

  if (selectedCategory.value) {
    result = result.filter(video =>
      video.categories.includes(selectedCategory.value)
    )
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(video =>
      video.title.toLowerCase().includes(query) ||
      video.category_names?.some(cat => cat.toLowerCase().includes(query))
    )
  }

  return result
})

const fetchVideos = async () => {
  console.log('开始获取视频列表...')
  loading.value = true
  error.value = ''
  
  // 设置超时，确保loading状态能被关闭
  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => reject(new Error('请求超时')), 30000)
  })
  
  try {
    const response = await Promise.race([
      apiClient.get('/videos/'),
      timeoutPromise
    ])
    const data = response.data.results || response.data
    videos.value = Array.isArray(data) ? data : []
    console.log('视频列表获取成功:', videos.value.length)
  } catch (e) {
    console.error('获取视频列表失败:', e)
    error.value = e.response?.data?.detail || e.message || t('common.fetchError')
    videos.value = []
  } finally {
    loading.value = false
    console.log('视频加载状态:', loading.value)
  }
}

const fetchCategories = async () => {
  console.log('开始获取分类...')
  try {
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('分类请求超时')), 30000)
    })
    
    const response = await Promise.race([
      apiClient.get('/categories/'),
      timeoutPromise
    ])
    const data = response.data.results || response.data
    categories.value = Array.isArray(data) ? data : []
    console.log('分类获取成功:', categories.value.length)
  } catch (e) {
    console.error('获取分类失败:', e)
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
  router.push(`/videos/${slug}`)
}

onMounted(async () => {
  console.log('视频页面挂载，开始加载数据...')
  
  // 设置一个很短的超时，确保loading一定能被关闭
  setTimeout(() => {
    console.log('强制关闭loading')
    loading.value = false
  }, 5000)
  
  // 并行获取数据，但不等待完成就继续
  Promise.all([fetchCategories(), fetchVideos()]).then(() => {
    console.log('所有数据加载完成')
  }).catch((e) => {
    console.error('加载数据出错:', e)
  }).finally(() => {
    loading.value = false
    console.log('数据加载结束，loading状态:', loading.value)
  })
  
  // 立即设置loading为false，然后异步加载数据
  setTimeout(() => {
    loading.value = false
  }, 100)
})
</script>

<style scoped>
.video-list {
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

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.video-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.video-card:hover {
  border-color: var(--accent-primary);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.card-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.video-card:hover .card-cover img {
  transform: scale(1.05);
}

.play-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 56px;
  height: 56px;
  background: rgba(59, 130, 246, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all var(--transition-fast);
}

.play-btn svg {
  width: 20px;
  height: 20px;
  color: white;
  margin-left: 3px;
}

.video-card:hover .play-btn {
  opacity: 1;
}

.play-btn:hover {
  transform: translate(-50%, -50%) scale(1.1);
  background: var(--accent-primary);
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