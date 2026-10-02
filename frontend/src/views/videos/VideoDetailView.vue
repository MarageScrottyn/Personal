<template>
  <div class="video-detail">
    <button class="back-btn" @click="goBack">
      <span>←</span> 返回
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
              playsinline
              disablePictureInPicture
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
            
            <!-- 自定义播放器控件 -->
            <div class="custom-controls" :class="{ hidden: hideControls && isPlaying }">
              <!-- 播放/暂停按钮 -->
              <button class="control-btn play-btn" @click="togglePlay">
                <span :class="isPlaying ? 'pause-icon' : 'play-icon'"></span>
              </button>
              
              <!-- 进度条和时间 -->
              <div class="progress-area">
                <div class="progress-bar" @click="handleProgressClick">
                  <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
                  <div class="progress-thumb" :style="{ left: progressPercent + '%' }"></div>
                </div>
              </div>
              
              <!-- 时间显示 -->
              <div class="time-display">
                <span>{{ formatTime(currentTime) }}</span>
                <span>/</span>
                <span>{{ formatTime(duration) }}</span>
              </div>
              
              <!-- 音量控制 -->
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
              
              <!-- 全屏按钮 -->
              <button class="control-btn fullscreen-btn" @click="toggleFullscreen">
                <span :class="isFullscreen ? 'exit-fullscreen-icon' : 'fullscreen-icon'"></span>
              </button>
            </div>
            
            <!-- 播放/暂停遮罩 -->
            <div class="play-overlay" v-if="!isPlaying && currentTime === 0" @click="togglePlay">
              <span class="big-play-icon">▶</span>
            </div>
          </div>
        </div>
        <div class="video-info">
          <h1>{{ video.title }}</h1>
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

// 播放器状态
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(1)
const isMuted = ref(false)
const isFullscreen = ref(false)
const hideControls = ref(false)
const isSeeking = ref(false)
const isVideoReady = ref(false)  // 新增：视频是否准备好
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
  console.error('视频加载错误:', videoError)
  console.error('视频URL:', event.target.src)
  console.error('错误码:', videoError?.code)
  console.error('错误信息:', videoError?.message)
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

// 自定义播放器控制方法
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
  // 检查视频是否准备好
  if (!videoPlayer.value || duration.value <= 0 || !isVideoReady.value) {
    console.log('视频未准备好:', { videoPlayer: !!videoPlayer.value, duration: duration.value, isVideoReady: isVideoReady.value })
    return
  }
  
  // 检查视频src是否有效
  if (!videoPlayer.value.src || videoPlayer.value.src === '') {
    console.error('视频src为空，无法跳转')
    return
  }
  
  // 检查视频readyState
  console.log('视频readyState:', videoPlayer.value.readyState)
  console.log('视频networkState:', videoPlayer.value.networkState)
  
  // 直接获取进度条元素
  const progressBar = document.querySelector('.progress-bar')
  if (!progressBar) {
    console.error('进度条元素未找到')
    return
  }
  
  const rect = progressBar.getBoundingClientRect()
  const clickX = event.clientX || (event.touches && event.touches[0]?.clientX) || 0
  const percent = Math.max(0, Math.min(1, (clickX - rect.left) / rect.width))
  const newTime = percent * duration.value
  
  console.log('点击进度条:', { clickX, rectLeft: rect.left, rectWidth: rect.width, percent, newTime, duration: duration.value })
  console.log('videoPlayer:', videoPlayer.value)
  console.log('当前currentTime:', videoPlayer.value.currentTime)
  console.log('视频duration:', videoPlayer.value.duration)
  
  // 检查缓冲范围
  const buffered = videoPlayer.value.buffered
  console.log('视频buffered:', buffered)
  if (buffered.length > 0) {
    console.log('缓冲开始:', buffered.start(0), '缓冲结束:', buffered.end(0))
  }
  
  // 设置跳转状态
  isSeeking.value = true
  
  // 保存当前播放状态（在暂停前保存）
  const wasPlaying = !videoPlayer.value.paused
  
  console.log('保存的播放状态:', wasPlaying)
  
  // 暂停视频（用于更稳定的seek）
  videoPlayer.value.pause()
  
  // 尝试直接设置currentTime
  console.log('尝试设置currentTime:', newTime)
  videoPlayer.value.currentTime = newTime
  
  // 立即检查结果
  console.log('设置后立即检查currentTime:', videoPlayer.value.currentTime)
  
  // 使用Promise封装seek操作
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
    console.log('seek成功，currentTime:', resultTime)
    
    if (resultTime > 0) {
      currentTime.value = resultTime
      progressPercent.value = (resultTime / duration.value) * 100
    }
    
    // 根据保存的状态决定是否恢复播放
    if (wasPlaying) {
      console.log('恢复播放')
      videoPlayer.value.play().catch(err => {
        console.error('恢复播放失败:', err)
      })
    }
    
    setTimeout(() => {
      isSeeking.value = false
    }, 300)
  }).catch((error) => {
    console.error('seek失败:', error)
    console.log('尝试检查服务器是否支持字节范围请求...')
    
    // 检查视频是否支持seek
    checkVideoSeekSupport(videoPlayer.value.src, newTime).then((success) => {
      if (success) {
        console.log('服务器支持字节范围请求，重试seek')
        retrySeek(newTime, wasPlaying)
      } else {
        console.error('服务器不支持字节范围请求，无法seek')
        isSeeking.value = false
      }
    }).catch(() => {
      console.error('无法检查服务器支持')
      isSeeking.value = false
    })
  })
}

// 检查服务器是否支持字节范围请求
const checkVideoSeekSupport = (url, targetTime) => {
  return new Promise((resolve) => {
    const xhr = new XMLHttpRequest()
    xhr.open('HEAD', url, true)
    xhr.onload = function() {
      const acceptRanges = xhr.getResponseHeader('Accept-Ranges')
      console.log('Accept-Ranges:', acceptRanges)
      console.log('Content-Length:', xhr.getResponseHeader('Content-Length'))
      
      if (acceptRanges === 'bytes') {
        // 服务器支持字节范围请求
        // 尝试实际的字节范围请求
        const rangeXhr = new XMLHttpRequest()
        const startByte = Math.floor(targetTime * 1000) // 估算字节位置
        rangeXhr.open('GET', url, true)
        rangeXhr.setRequestHeader('Range', `bytes=${startByte}-${startByte + 1024}`)
        rangeXhr.onload = function() {
          console.log('Range请求状态:', rangeXhr.status)
          if (rangeXhr.status >= 200 && rangeXhr.status < 300) {
            resolve(true)
          } else {
            resolve(false)
          }
        }
        rangeXhr.onerror = function() {
          resolve(false)
        }
        rangeXhr.send()
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

// 重试seek
const retrySeek = (newTime, wasPlaying = false) => {
  console.log('重试seek到:', newTime, 'wasPlaying:', wasPlaying)
  
  videoPlayer.value.currentTime = newTime
  
  setTimeout(() => {
    console.log('重试后currentTime:', videoPlayer.value.currentTime)
    
    if (videoPlayer.value.currentTime > 0) {
      currentTime.value = videoPlayer.value.currentTime
      progressPercent.value = (currentTime.value / duration.value) * 100
      
      // 根据保存的状态决定是否恢复播放
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
  // 跳转期间不更新进度，避免被旧值覆盖
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
    isVideoReady.value = true  // 标记视频已准备好
    console.log('视频已加载完成，duration:', duration.value)
  }
}

const handleLoadedData = () => {
  console.log('视频数据已加载')
  if (videoPlayer.value) {
    isVideoReady.value = true
    duration.value = videoPlayer.value.duration || duration.value
    console.log('视频数据加载完成，duration:', duration.value)
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
          console.log('HLS manifest parsed')
          isVideoReady.value = true  // HLS视频准备好
        })
        
        hlsInstance.on(Hls.default.Events.ERROR, (event, data) => {
          console.error('HLS error:', data)
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
        // 浏览器不支持hls.js，尝试原生HLS（如Safari）
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
  background: white;
  color: #333;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.back-btn:hover {
  background: #11998e;
  color: white;
  border-color: #11998e;
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
  border-top-color: #11998e;
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
  background: white;
  border-radius: 16px;
}

.video-content {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.video-player {
  width: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
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
  font-size: 18px;
}

.video-info {
  padding: 30px;
}

.video-info h1 {
  font-size: 26px;
  color: #2c3e50;
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
  padding: 4px 12px;
  background: #f0f0f0;
  color: #333;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-tag:hover {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #f0f0f0;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.meta-icon {
  font-size: 14px;
}

.video-desc {
  color: #555;
  line-height: 1.8;
  font-size: 15px;
}

.video-content {
  display: flex;
  gap: 20px;
}

.video-main {
  flex: 1;
  min-width: 0;
}

.recommendations-sidebar {
  width: 320px;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.recommendations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.recommendations-header h3 {
  font-size: 16px;
  color: #2c3e50;
  margin: 0;
}

.recommendation-count {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 4px 10px;
  border-radius: 12px;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.recommendation-card {
  background: #f8f9fa;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.recommendation-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.recommendation-card.active {
  box-shadow: 0 0 0 2px #11998e;
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
  font-size: 12px;
  color: #11998e;
  padding-left: 2px;
}

.recommendation-info {
  padding: 8px;
}

.recommendation-title {
  font-size: 12px;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.recommendation-card.active .recommendation-title {
  color: #11998e;
}

@media (max-width: 768px) {
  .video-player {
    min-height: 250px;
  }

  .video-info {
    padding: 20px;
  }

  .video-info h1 {
    font-size: 20px;
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
    font-size: 11px;
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
    font-size: 11px;
  }
}

/* 自定义播放器控件样式 */
.video-player-wrapper {
  position: relative;
  width: 100%;
}

.video-player {
  position: relative;
  width: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.video-player video {
  width: 100%;
  max-height: 70vh;
  outline: none;
}

.video-player video::-webkit-media-controls {
  display: none !important;
}

.video-player video::-webkit-media-controls-enclosure {
  display: none !important;
}

.custom-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.9), transparent);
  padding: 15px;
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
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.4);
}

.play-btn {
  font-size: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-icon {
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 7px 0 7px 12px;
  border-color: transparent transparent transparent #fff;
  margin-left: 2px;
}

.pause-icon {
  display: flex;
  gap: 4px;
}

.pause-icon::before,
.pause-icon::after {
  content: '';
  width: 4px;
  height: 14px;
  background: #fff;
  border-radius: 1px;
}

.volume-btn {
  font-size: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.volume-icon {
  width: 14px;
  height: 14px;
  position: relative;
}

.volume-icon::before {
  content: '';
  position: absolute;
  left: 0;
  top: 2px;
  width: 6px;
  height: 10px;
  background: #fff;
  border-radius: 3px 0 0 3px;
}

.volume-icon::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 7px 0 7px 8px;
  border-color: transparent transparent transparent #fff;
}

.mute-icon {
  width: 14px;
  height: 14px;
  position: relative;
}

.mute-icon::before {
  content: '';
  position: absolute;
  left: 0;
  top: 2px;
  width: 6px;
  height: 10px;
  background: #fff;
  border-radius: 3px 0 0 3px;
}

.mute-icon::after {
  content: '';
  position: absolute;
  left: 2px;
  top: 50%;
  width: 12px;
  height: 2px;
  background: #fff;
  transform: rotate(-45deg) translateY(-50%);
}

.fullscreen-btn {
  font-size: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fullscreen-icon {
  width: 14px;
  height: 14px;
  position: relative;
}

.fullscreen-icon::before,
.fullscreen-icon::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  border: 2px solid #fff;
}

.fullscreen-icon::before {
  top: 0;
  left: 0;
  border-right: none;
  border-bottom: none;
}

.fullscreen-icon::after {
  bottom: 0;
  right: 0;
  border-left: none;
  border-top: none;
}

.exit-fullscreen-icon {
  width: 14px;
  height: 14px;
  position: relative;
}

.exit-fullscreen-icon::before,
.exit-fullscreen-icon::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  border: 2px solid #fff;
}

.exit-fullscreen-icon::before {
  bottom: 0;
  left: 0;
  border-right: none;
  border-top: none;
}

.exit-fullscreen-icon::after {
  top: 0;
  right: 0;
  border-left: none;
  border-bottom: none;
}

.progress-area {
  flex: 1;
  height: 6px;
}

.progress-bar {
  position: relative;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  cursor: pointer;
}

.progress-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, #11998e, #38ef7d);
  border-radius: 3px;
  transition: width 0.1s linear;
}

.progress-thumb {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  background: white;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.progress-bar:hover .progress-thumb {
  opacity: 1;
}

.time-display {
  display: flex;
  align-items: center;
  gap: 4px;
  color: white;
  font-size: 12px;
  min-width: 70px;
}

.volume-container {
  display: flex;
  align-items: center;
  gap: 5px;
}

.volume-slider {
  width: 60px;
  height: 4px;
  cursor: pointer;
}

.volume-slider::-webkit-slider-thumb {
  appearance: none;
  width: 12px;
  height: 12px;
  background: white;
  border-radius: 50%;
  cursor: pointer;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.big-play-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #11998e;
  transition: transform 0.3s ease;
}

.play-overlay:hover .big-play-icon {
  transform: scale(1.1);
}

/* 移动端播放器样式 */
@media (max-width: 768px) {
  .video-player {
    min-height: 250px;
  }

  .video-player video {
    max-height: 50vh;
  }

  .video-player video::-webkit-media-controls {
    display: none !important;
  }

  .video-player video::-webkit-media-controls-enclosure {
    display: none !important;
  }

  .custom-controls {
    padding: 15px 10px 10px;
  }

  .control-btn {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .volume-container {
    display: none;
  }

  .time-display {
    font-size: 11px;
  }

  .big-play-icon {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }
}
</style>