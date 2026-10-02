<template>
  <div class="comic-read" @scroll="handleScroll" ref="scrollContainer">
    <div class="reader-header" :class="{ 'header-hidden': !headerVisible }">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
        <span>返回</span>
      </button>
      <div class="reader-info">
        <h1 class="comic-title">{{ comic?.title }}</h1>
        <h2 class="chapter-title">{{ chapter?.title }}</h2>
      </div>
      <div class="reader-controls" v-if="!isMobile">
        <button class="control-btn" @click="prevPage" :disabled="currentPage === 0">
          <span>←</span> 上一页
        </button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="control-btn" @click="nextPage" :disabled="currentPage >= totalPages - 1">
          下一页 <span>→</span>
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="reader-content">
      <div v-if="!isMobile" class="image-container">
        <img
          :src="currentImage"
          :alt="`第${currentPage + 1}页`"
          @click="nextPage"
          @error="handleImageError"
        />
      </div>

      <div v-else class="mobile-images-container">
        <div
          v-for="(image, index) in images"
          :key="index"
          class="mobile-image-item"
        >
          <img
            :src="getImageUrl(image)"
            :alt="`第${index + 1}页`"
            @error="handleImageError"
          />
        </div>
      </div>
    </div>

    <div class="reader-footer" v-if="!isMobile">
      <div class="page-nav">
        <button
          v-for="(_, index) in totalPages"
          :key="index"
          class="page-btn"
          :class="{ active: currentPage === index }"
          @click="goToPage(index)"
        >
          {{ index + 1 }}
        </button>
      </div>
    </div>

    <button
      class="back-to-top"
      :class="{ 'visible': showBackToTop }"
      @click="scrollToTop"
    >
      ↑
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '../../api/client'

const route = useRoute()
const router = useRouter()
const comic = ref(null)
const chapter = ref(null)
const loading = ref(true)
const error = ref('')
const currentPage = ref(0)
const images = ref([])
const windowWidth = ref(window.innerWidth)
const scrollContainer = ref(null)
const headerVisible = ref(true)
const showBackToTop = ref(false)
const lastScrollTop = ref(0)
const scrollThreshold = 50

const isMobile = computed(() => windowWidth.value < 768)

const totalPages = computed(() => images.value.length)

const currentImage = computed(() => {
  if (images.value[currentPage.value]) {
    return getImageUrl(images.value[currentPage.value])
  }
  return '/placeholder.png'
})

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

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

const handleScroll = () => {
  if (!isMobile.value || !scrollContainer.value) return

  const scrollTop = scrollContainer.value.scrollTop
  const scrollDirection = scrollTop > lastScrollTop.value

  if (scrollDirection && scrollTop > scrollThreshold) {
    headerVisible.value = false
  } else if (!scrollDirection && scrollTop < lastScrollTop.value - 20) {
    headerVisible.value = true
  }

  showBackToTop.value = scrollTop > 300

  lastScrollTop.value = scrollTop
}

const scrollToTop = () => {
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }
}

const fetchComic = async () => {
  try {
    const { comicSlug, chapterId } = route.params
    const response = await apiClient.get(`/comics/${comicSlug}/`)
    comic.value = response.data

    const foundChapter = comic.value.chapters.find(ch => ch.id === parseInt(chapterId))
    if (!foundChapter) {
      error.value = '章节不存在'
      return
    }

    chapter.value = foundChapter
    images.value = chapter.value.images || []
  } catch (e) {
    error.value = e.response?.data?.detail || '获取漫画失败'
  } finally {
    loading.value = false
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
  }
}

const prevPage = () => {
  if (currentPage.value > 0) {
    currentPage.value--
  }
}

const goToPage = (index) => {
  currentPage.value = index
}

const goBack = () => {
  router.push(`/comics/${route.params.comicSlug}`)
}

const handleImageError = (event) => {
  event.target.src = '/placeholder.png'
}

onMounted(() => {
  fetchComic()
  window.addEventListener('resize', handleResize)

  nextTick(() => {
    if (scrollContainer.value) {
      lastScrollTop.value = scrollContainer.value.scrollTop
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.comic-read {
  min-height: 100vh;
  background: #1a1a1a;
  color: white;
  overflow-y: auto;
  position: relative;
  scroll-behavior: smooth;
}

.reader-header {
  position: sticky;
  top: 0;
  background: rgba(26, 26, 26, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--flora-base-dark);
  padding: 15px 20px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 20px;
  transition: transform 0.3s ease, opacity 0.3s ease;
  will-change: transform, opacity;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--flora-stem);
  color: white;
  border: 1px solid var(--flora-stem-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.875rem;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: var(--flora-leaf);
  border-color: var(--flora-leaf);
}

.back-icon {
  font-size: 1rem;
}

.reader-info {
  flex: 1;
}

.comic-title {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--flora-bloom);
}

.chapter-title {
  font-family: var(--font-body);
  font-size: 0.875rem;
  margin: 0;
  color: #aaa;
}

.reader-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.control-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px;
  background: var(--flora-stem);
  color: white;
  border: 1px solid var(--flora-stem-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.8rem;
  transition: all var(--transition-fast);
}

.control-btn:hover:not(:disabled) {
  background: var(--flora-leaf);
  border-color: var(--flora-leaf);
}

.control-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: #aaa;
  min-width: 70px;
  text-align: center;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 15px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #333;
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
  color: var(--flora-bloom);
  font-family: var(--font-body);
  font-size: 1rem;
}

.reader-content {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  min-height: 70vh;
}

@media (max-width: 768px) {
  .reader-content {
    padding: 0;
    align-items: flex-start;
  }
}

.image-container {
  max-width: 100%;
  max-height: 80vh;
  cursor: pointer;
  transition: all 0.3s ease;
}

.image-container:hover {
  transform: scale(1.01);
}

.image-container img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
}

.reader-footer {
  background: rgba(26, 26, 26, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid var(--flora-base-dark);
  padding: 15px 20px;
  position: sticky;
  bottom: 0;
  z-index: 100;
}

.page-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 800px;
  margin: 0 auto;
}

.page-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2a2a2a;
  color: #aaa;
  border: 1px solid #444;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.8rem;
  transition: all var(--transition-fast);
}

.page-btn:hover {
  background: var(--flora-stem);
  color: white;
  border-color: var(--flora-stem);
}

.page-btn.active {
  background: var(--flora-leaf);
  color: white;
  border-color: var(--flora-leaf);
}

.back-to-top {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 44px;
  height: 44px;
  background: var(--flora-stem);
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  visibility: hidden;
  transition: all var(--transition-fast);
  box-shadow: 0 4px 15px rgba(163, 139, 125, 0.4);
  z-index: 99;
}

.back-to-top.visible {
  opacity: 1;
  visibility: visible;
}

.back-to-top:hover {
  background: var(--flora-leaf);
  transform: translateY(-3px);
}

@media (max-width: 768px) {
  .reader-header {
    padding: 12px 15px;
    gap: 12px;
  }

  .back-btn {
    padding: 6px 12px;
    font-size: 0.8rem;
  }

  .comic-title {
    font-size: 0.875rem;
  }

  .chapter-title {
    font-size: 0.75rem;
  }

  .back-to-top {
    bottom: 20px;
    right: 20px;
    width: 40px;
    height: 40px;
  }

  .page-btn {
    width: 32px;
    height: 32px;
    font-size: 0.75rem;
  }
}
</style>