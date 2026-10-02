<template>
  <div class="video-detail">
    <button class="back-btn" @click="goBack">
      <span class="back-icon">←</span>
      <span>返回</span>
    </button>
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="video" class="video-content">
      <div class="video-main">
        <div class="video-player-wrapper">
          <div class="video-player">
            <video
              ref="videoPlayer"
              v-if="getVideoUrl(video.video_file)"
              :src="getVideoUrl(video.video_file)"
              autoplay
              preload="auto"
              @error="handleVideoError"
              @timeupdate="handleTimeUpdate"
              @loadedmetadata="handleLoadedMetadata"
              @loadeddata="handleLoadedData"
              @ended="handleVideoEnded"
              @pause="isPlaying = false"
              @play="isPlaying = true"
              @seeked="handleSeeked"
              @seeking="handleSeeking"
            ></video>
            <div v-else class="no-video">
              <span class="empty-icon">🎬</span>
              <p>暂无视频</p>
            </div>

            <div class="custom-controls" :class="{ hidden: hideControls && isPlaying }">
              <button class="control-btn play-btn" @click="togglePlay">
                <span>{{ isPlaying ? '⏸' : '▶' }}</span>
              </button>

              <div class="progress-area">
                <div class="progress-bar" @click="handleProgressClick">
                  <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
                  <div class="progress-thumb" :style="{ left: progressPercent + '%' }"></div>
                </div>
              </div>

              <div class="time-display">
                <span>{{ formatTime(currentTime) }}</span>
                <span>/</span>
                <span>{{ formatTime(duration) }}</span>
              </div>

              <div class="volume-container">
                <button class="control-btn volume-btn" @click="toggleMute">
                  <span>{{ isMuted ? '🔇' : '🔊' }}</span>
                </button>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  v-model="volume"
                  class="volume-slider"
                  @input="updateVolume"
                />
              </div>

              <button class="control-btn fullscreen-btn" @click="toggleFullscreen">
                <span>⛶</span>
              </button>
            </div>

            <div class="play-overlay" v-if="!isPlaying && currentTime === 0" @click="togglePlay">
              <span class="big-play-icon">▶</span>
            </div>
          </div>
        </div>
        <div class="video-info">
          <h1 class="video-title">{{ video.title }}</h1>
          <div class="meta-info">
            <div class="categories-list">
              <span class="meta-icon">📂</span>
              <span
                v-for="category in video.category_names"
                :key="category"
                class="category-tag"
                @click="goToCategory(category)"
              >
                {{ category }}
              </span>
            </div>
            
          </div>
          <p class="video-desc">{{ video.description }}</p>
        </div>
      </div>
      <div v-if="relatedVideos.length > 0" class="recommendations-sidebar">
        <div class="recommendations-header">
          <h3>推荐</h3>
          <span class="recommendation-count">共{{ relatedVideos.length }}个视频</span>
        </div>
        <div class="recommendations-grid">
          <div
            v-for="vid in relatedVideos"
            :key="vid.id"
            :class="['recommendation-card', { active: vid.id === video.id }]"
            @click="switchVideo(vid)"
          >
            <div class="recommendation-thumbnail">
              <img :src="getThumbnailUrl(vid.thumbnail)" :alt="vid.title" />
              <div class="play-overlay">
                <span class="play-icon">▶</span>
              </div>
            </div>
            <div class="recommendation-info">
              <h4 class="recommendation-title">{{ vid.title }}</h4>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '../../api/client'

const route = useRoute()
const router = useRouter()
const video = ref(null)
const categories = ref([])
const relatedVideos = ref([])
const loading = ref(true)
const error = ref('')
const videoPlayer = ref(null)
let hlsInstance = null

const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(1)
const isMuted = ref(false)
const isFullscreen = ref(false)
const hideControls = ref(false)
const isSeeking = ref(false)
const isVideoReady = ref(false)
let hideControlsTimer = null

const isM3U8 = (url) => {
  return url && (url.endsWith('.m3u8') || url.includes('.m3u8?'))
}

const fetchVideo = async () => {
  try {
    const response = await apiClient.get(`/videos/${route.params.slug}/`)
    video.value = response.data
    if (video.value.category_names && video.value.category_names.length > 0) {
      fetchRelatedVideos(video.value.category_names[0])
    }
  } catch (e) {
    error.value = e.response?.data?.detail || '获取视频详情失败'
  } finally {
    loading.value = false
  }
}

const fetchRelatedVideos = async (categoryName) => {
  try {
    const categoryResponse = await apiClient.get('/categories/')
    const category = categoryResponse.data.find(cat => cat.name === categoryName)
    if (category) {
      const response = await apiClient.get(`/videos/?category=${category.slug}`)
      relatedVideos.value = response.data
    }
  } catch (e) {
    console.error('获取相关视频失败:', e)
  }
}

const switchVideo = (newVideo) => {
  if (newVideo.id === video.value.id) return
  video.value = newVideo
  router.push({ name: 'video-detail', params: { slug: newVideo.slug } })
}

const getVideoUrl = (path) => {
  if (!path) {
    console.warn('视频路径为空')
    return ''
  }
  if (path.startsWith('http')) {
    return path
  }
  let cleanPath = path
  cleanPath = cleanPath.replace(/^\/media\/*/g, '')
  return `/media/${cleanPath}`
}

const getThumbnailUrl = (path) => {
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

const handleVideoError = (event) => {
  const videoError = event.target.error
  if (videoError?.code === 4) {
    error.value = '视频格式不支持或视频文件损坏'
  } else if (videoError?.code === 2) {
    error.value = '视频网络加载失败，请检查网络连接'
  } else if (videoError?.code === 1) {
    error.value = '视频加载被中断'
  } else {
    error.value = '视频源格式错误，请重试'
  }
}

const handleVideoClick = (event) => {
  const videoElement = event.target
  if (videoElement.duration) {
    const rect = videoElement.getBoundingClientRect()
    const percent = (event.clientX - rect.left) / rect.width
    videoElement.currentTime = percent * videoElement.duration
  }
}

const progressPercent = ref(0)

const togglePlay = () => {
  if (videoPlayer.value) {
    if (isPlaying.value) {
      videoPlayer.value.pause()
    } else {
      videoPlayer.value.play()
    }
  }
}

const handleProgressClick = (event) => {
  if (!videoPlayer.value || duration.value <= 0 || !isVideoReady.value) {
    return
  }

  if (!videoPlayer.value.src || videoPlayer.value.src === '') {
    return
  }

  const progressBar = event.currentTarget
  const rect = progressBar.getBoundingClientRect()
  const clickX = event.clientX || (event.touches && event.touches[0]?.clientX) || 0
  const percent = Math.max(0, Math.min(1, (clickX - rect.left) / rect.width))
  const newTime = percent * duration.value

  isSeeking.value = true
  const wasPlaying = !videoPlayer.value.paused

  videoPlayer.value.pause()
  videoPlayer.value.currentTime = newTime

  const seekPromise = new Promise((resolve, reject) => {
    let timeout = setTimeout(() => {
      videoPlayer.value.removeEventListener('seeked', onSeeked)
      videoPlayer.value.removeEventListener('error', onError)
      reject(new Error('seek超时'))
    }, 3000)

    const onSeeked = () => {
      clearTimeout(timeout)
      videoPlayer.value.removeEventListener('seeked', onSeeked)
      videoPlayer.value.removeEventListener('error', onError)
      resolve(videoPlayer.value.currentTime)
    }

    const onError = (e) => {
      clearTimeout(timeout)
      videoPlayer.value.removeEventListener('seeked', onSeeked)
      videoPlayer.value.removeEventListener('error', onError)
      reject(e)
    }

    videoPlayer.value.addEventListener('seeked', onSeeked)
    videoPlayer.value.addEventListener('error', onError)
  })

  seekPromise.then((resultTime) => {
    if (resultTime > 0) {
      currentTime.value = resultTime
      progressPercent.value = (resultTime / duration.value) * 100
    }
    if (wasPlaying) {
      videoPlayer.value.play().catch(err => {
        console.error('恢复播放失败:', err)
      })
    }
    setTimeout(() => {
      isSeeking.value = false
    }, 300)
  }).catch(() => {
    isSeeking.value = false
  })
}

const checkVideoSeekSupport = (url, targetTime) => {
  return new Promise((resolve) => {
    const xhr = new XMLHttpRequest()
    xhr.open('HEAD', url, true)
    xhr.onload = function() {
      const acceptRanges = xhr.getResponseHeader('Accept-Ranges')
      if (acceptRanges === 'bytes') {
        resolve(true)
      } else {
        resolve(false)
      }
    }
    xhr.onerror = function() {
      resolve(false)
    }
    xhr.send()
  })
}

const retrySeek = (newTime, wasPlaying = false) => {
  videoPlayer.value.currentTime = newTime
  setTimeout(() => {
    if (videoPlayer.value.currentTime > 0) {
      currentTime.value = videoPlayer.value.currentTime
      progressPercent.value = (currentTime.value / duration.value) * 100
      if (wasPlaying) {
        videoPlayer.value.play().catch(err => {
          console.error('恢复播放失败:', err)
        })
      }
    }
    setTimeout(() => {
      isSeeking.value = false
    }, 300)
  }, 100)
}

const handleTimeUpdate = () => {
  if (isSeeking.value) return

  if (videoPlayer.value) {
    currentTime.value = videoPlayer.value.currentTime
    if (duration.value > 0) {
      progressPercent.value = (currentTime.value / duration.value) * 100
    }
  }
}

const handleSeeking = () => {
  isSeeking.value = true
}

const handleSeeked = () => {
  isSeeking.value = false
  if (videoPlayer.value) {
    currentTime.value = videoPlayer.value.currentTime
    if (duration.value > 0) {
      progressPercent.value = (currentTime.value / duration.value) * 100
    }
  }
}

const handleLoadedMetadata = () => {
  if (videoPlayer.value) {
    duration.value = videoPlayer.value.duration
    videoPlayer.value.volume = volume.value
    videoPlayer.value.muted = isMuted.value
    isVideoReady.value = true
  }
}

const handleLoadedData = () => {
  if (videoPlayer.value) {
    isVideoReady.value = true
    duration.value = videoPlayer.value.duration || duration.value
  }
}

const handleVideoEnded = () => {
  isPlaying.value = false
}

const toggleMute = () => {
  if (videoPlayer.value) {
    isMuted.value = !isMuted.value
    videoPlayer.value.muted = isMuted.value
  }
}

const updateVolume = () => {
  if (videoPlayer.value) {
    videoPlayer.value.volume = volume.value
    if (volume.value > 0) {
      isMuted.value = false
      videoPlayer.value.muted = false
    }
  }
}

const toggleFullscreen = () => {
  const playerWrapper = document.querySelector('.video-player-wrapper')
  if (!playerWrapper) return

  if (!document.fullscreenElement) {
    playerWrapper.requestFullscreen().then(() => {
      isFullscreen.value = true
    }).catch(err => {
      console.error('全屏失败:', err)
    })
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
    }).catch(err => {
      console.error('退出全屏失败:', err)
    })
  }
}

const formatTime = (seconds) => {
  if (isNaN(seconds) || seconds < 0) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const initHlsPlayer = async () => {
  if (!videoPlayer.value || !video.value) return

  const videoUrl = getVideoUrl(video.value.video_file)

  if (isM3U8(videoUrl)) {
    try {
      const Hls = await import('hls.js')

      if (Hls.default.isSupported()) {
        hlsInstance = new Hls.default({
          enableWorker: true,
          lowLatencyMode: true
        })

        hlsInstance.loadSource(videoUrl)
        hlsInstance.attachMedia(videoPlayer.value)

        hlsInstance.on(Hls.default.Events.MANIFEST_PARSED, () => {
          isVideoReady.value = true
        })

        hlsInstance.on(Hls.default.Events.ERROR, (event, data) => {
          if (data.fatal) {
            switch (data.type) {
              case Hls.default.ErrorTypes.NETWORK_ERROR:
                error.value = '网络加载失败，请检查网络连接'
                break
              case Hls.default.ErrorTypes.MEDIA_ERROR:
                error.value = '视频解码失败'
                break
              default:
                error.value = '视频加载失败，请重试'
                break
            }
          }
        })
      } else {
        videoPlayer.value.src = videoUrl
      }
    } catch (e) {
      console.error('Failed to load hls.js:', e)
      videoPlayer.value.src = videoUrl
    }
  } else {
    videoPlayer.value.src = videoUrl
  }
}

const destroyHls = () => {
  if (hlsInstance) {
    hlsInstance.destroy()
    hlsInstance = null
  }
}

const formatDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const goBack = () => {
  router.push('/videos')
}

const goToCategory = (categoryName) => {
  const category = categories.value.find(cat => cat.name === categoryName)
  if (category) {
    router.push(`/categories/${category.slug}`)
  }
}

const onVideoReady = () => {
  initHlsPlayer()
}

watch(video, (newVideo) => {
  if (newVideo) {
    destroyHls()
    setTimeout(() => {
      initHlsPlayer()
    }, 100)
  }
})

onMounted(async () => {
  await fetchCategories()
  await fetchVideo()
})

onUnmounted(() => {
  destroyHls()
})

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/categories/')
    categories.value = response.data
  } catch (e) {
    console.error('获取分类列表失败:', e)
  }
}
</script>

<style scoped>
.video-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--flora-base-light);
  color: var(--flora-stem);
  border: 2px solid var(--flora-base-dark);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-family: var(--font-body);
  font-weight: 500;
  transition: all var(--transition-fast);
  margin-bottom: 20px;
}

.back-btn:hover {
  background: var(--flora-leaf);
  color: white;
  border-color: var(--flora-leaf);
}

.back-icon {
  font-size: 1.1rem;
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
  font-family: var(--font-body);
  font-size: 1rem;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
}

.video-content {
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: 0 8px 30px var(--flora-shadow);
  display: flex;
  gap: 20px;
}

.video-main {
  flex: 1;
  min-width: 0;
}

.video-player-wrapper {
  width: 100%;
  background: #000;
}

.video-player {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  position: relative;
}

.video-player video {
  width: 100%;
  max-height: 70vh;
  outline: none;
}

.no-video {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  color: #999;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.no-video p {
  font-family: var(--font-body);
  font-size: 1.125rem;
}

.custom-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.9));
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: opacity 0.3s ease;
}

.custom-controls.hidden {
  opacity: 0;
  pointer-events: none;
}

.control-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.control-btn:hover {
  background: var(--flora-leaf);
}

.progress-area {
  flex: 1;
  padding: 0 10px;
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  cursor: pointer;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: var(--flora-leaf);
  border-radius: 3px;
  transition: width 0.1s linear;
}

.progress-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: var(--flora-leaf);
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.time-display {
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: white;
  min-width: 90px;
  text-align: center;
}

.volume-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.volume-slider {
  width: 80px;
  height: 4px;
  -webkit-appearance: none;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  cursor: pointer;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  background: var(--flora-leaf);
  border-radius: 50%;
  cursor: pointer;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.big-play-icon {
  width: 70px;
  height: 70px;
  background: var(--flora-leaf);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  color: white;
  padding-left: 6px;
  transition: transform 0.3s ease;
}

.play-overlay:hover .big-play-icon {
  transform: scale(1.1);
}

.video-info {
  padding: 30px;
}

.video-title {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  color: var(--flora-stem);
  margin-bottom: 15px;
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.categories-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--flora-base);
  border-radius: 20px;
  font-family: var(--font-body);
  font-size: 0.875rem;
  color: var(--flora-stem-light);
}

.meta-icon {
  font-size: 0.9rem;
}

.video-desc {
  font-family: var(--font-body);
  color: var(--flora-stem);
  line-height: 1.8;
  font-size: 0.95rem;
}

.recommendations-sidebar {
  width: 320px;
  background: var(--flora-base-light);
  border-radius: var(--radius-xl);
  padding: 20px;
  box-shadow: 0 4px 20px var(--flora-shadow);
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.recommendations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--flora-base-dark);
}

.recommendations-header h3 {
  font-family: var(--font-heading);
  font-size: 1rem;
  color: var(--flora-stem);
  margin: 0;
}

.recommendation-count {
  font-family: var(--font-body);
  font-size: 0.75rem;
  color: var(--flora-stem-light);
  background: var(--flora-base);
  padding: 4px 10px;
  border-radius: 12px;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.recommendation-card {
  background: var(--flora-base);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.recommendation-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px var(--flora-shadow);
}

.recommendation-card.active {
  box-shadow: 0 0 0 2px var(--flora-leaf);
}

.recommendation-thumbnail {
  position: relative;
  height: 90px;
  overflow: hidden;
}

.recommendation-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.recommendation-card:hover .recommendation-thumbnail img {
  transform: scale(1.05);
}

.recommendation-card .play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.recommendation-card:hover .play-overlay {
  opacity: 1;
}

.recommendation-card .play-icon {
  width: 32px;
  height: 32px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: var(--flora-leaf);
  padding-left: 2px;
}

.recommendation-info {
  padding: 8px;
}

.recommendation-title {
  font-family: var(--font-body);
  font-size: 0.75rem;
  color: var(--flora-stem);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.recommendation-card.active .recommendation-title {
  color: var(--flora-leaf);
}

@media (max-width: 768px) {
  .video-player {
    min-height: 250px;
  }

  .video-info {
    padding: 20px;
  }

  .video-title {
    font-size: 1.25rem;
  }

  .video-content {
    flex-direction: column;
  }

  .recommendations-sidebar {
    width: 100%;
    max-height: none;
  }

  .recommendations-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .recommendation-thumbnail {
    height: 80px;
  }

  .recommendation-title {
    font-size: 0.7rem;
  }
}

@media (max-width: 480px) {
  .recommendations-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .recommendation-thumbnail {
    height: 70px;
  }

  .recommendation-info {
    padding: 6px;
  }

  .recommendation-title {
    font-size: 0.7rem;
  }
}
</style>