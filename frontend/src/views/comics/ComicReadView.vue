<template>
  <div class="comic-read" @scroll="handleScroll" ref="scrollContainer">
    <div class="reader-header" :class="{ 'header-hidden': !headerVisible }">
      <button class="back-btn" @click="goBack">
        <span>←</span> 返回
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
      <!-- 电脑端：单页显示 -->
      <div v-if="!isMobile" class="image-container">
        <img 
          :src="currentImage" 
          :alt="`第${currentPage + 1}页`" 
          @click="nextPage"
          @error="handleImageError"
        />
      </div>
      
      <!-- 移动端：上下滑动查看所有图片 -->
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
    
    <!-- 回到顶部按钮 -->
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
  
  // 隐藏/显示头部
  if (scrollDirection && scrollTop > scrollThreshold) {
    headerVisible.value = false
  } else if (!scrollDirection && scrollTop < lastScrollTop.value - 20) {
    headerVisible.value = true
  }
  
  // 显示/隐藏回到顶部按钮
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
    
    // 找到对应的章节
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
  
  // 初始化滚动容器
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
  background: #000;
  color: white;
  overflow-y: auto;
  position: relative;
  scroll-behavior: smooth;
}

.reader-header {
  position: sticky;
  top: 0;
  background: rgba(0, 0, 0, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #222;
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
  background: #333;
  color: white;
  border: 1px solid #444;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #444;
}

.reader-info {
  flex: 1;
}

.comic-title {
  font-size: 16px;
  font-weight: bold;
  margin: 0 0 4px 0;
  color: #667eea;
}

.chapter-title {
  font-size: 14px;
  margin: 0;
  color: #ccc;
}

.reader-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.control-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 12px;
  background: #333;
  color: white;
  border: 1px solid #444;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
}

.control-btn:hover:not(:disabled) {
  background: #667eea;
  border-color: #667eea;
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #ccc;
  min-width: 60px;
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
  border-radius: 8px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.5);
}

.reader-footer {
  background: rgba(26, 26, 26, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid #333;
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
  width: 32px;
  height: 32px;
  border: 1px solid #444;
  background: #333;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.page-btn:hover {
  background: #444;
  border-color: #667eea;
}

.page-btn.active {
  background: #667eea;
  border-color: #667eea;
  color: white;
}

/* 移动端样式 */
.mobile-images-container {
  width: 100%;
  padding: 0;
  background: #000;
}

.mobile-image-item {
  position: relative;
  margin-bottom: 0;
  width: 100%;
  overflow: hidden;
}

.mobile-image-item img {
  width: 100%;
  height: auto;
  object-fit: contain;
  border-radius: 0;
  box-shadow: none;
  display: block;
  background: #000;
}

/* 优化移动端滚动体验 */
@media (max-width: 768px) {
  .comic-read {
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }
  
  .mobile-images-container {
    padding: 0;
  }
  
  .mobile-image-item {
    margin-bottom: 0;
  }
  
  .mobile-image-item img {
    width: 100%;
    height: auto;
    object-fit: contain;
    display: block;
  }
  
  /* 隐藏滚动条 */
  .comic-read::-webkit-scrollbar {
    display: none;
  }
  
  .comic-read {
    -ms-overflow-style: none;
    scrollbar-width: none;
  }
}

/* 隐藏头部样式 */
.header-hidden {
  transform: translateY(-100%);
  opacity: 0;
  pointer-events: none;
}

/* 回到顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 50px;
  height: 50px;
  border: 2px solid #667eea;
  background: rgba(102, 126, 234, 0.9);
  color: white;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99;
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(20px);
  pointer-events: none;
}

.back-to-top.visible {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.back-to-top:hover {
  background: #667eea;
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

@media (max-width: 768px) {
  .reader-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 10px 15px;
  }
  
  .reader-controls {
    width: 100%;
    justify-content: space-between;
  }
  
  .reader-content {
    padding: 0;
  }
  
  .image-container img {
    max-height: 70vh;
  }
  
  .page-nav {
    gap: 5px;
  }
  
  .page-btn {
    width: 28px;
    height: 28px;
    font-size: 11px;
  }
  
  .mobile-images-container {
    padding: 0;
  }
  
  .mobile-image-item {
    margin-bottom: 0;
  }
  
  .mobile-image-item img {
    border-radius: 0;
  }
  
  .back-to-top {
    width: 45px;
    height: 45px;
    font-size: 20px;
    bottom: 15px;
    right: 15px;
  }
}
</style>
