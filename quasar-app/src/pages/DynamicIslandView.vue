<template>
  <div class="dynamic-island-wrapper">
    <div class="dynamic-island" :class="[currentStyle, { expanded: isExpanded, collapsed: !isExpanded, 'no-animations': !animationsEnabled }]" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
      <div class="island-morph" @click="handleClick">
      <div class="island-core" v-if="!isExpanded">
        <div class="core-leading" @mousedown.stop="startDrag" v-if="!isExpanded">
          <div class="core-icon" :class="currentTrack.title && currentTrack.isPlaying ? 'pulse-animation' : iconAnimationClass">
            <div class="album-cover-container" v-if="currentTrack.thumbnail">
              <img 
                :src="currentTrack.thumbnail" 
                alt="Album cover"
                class="album-cover"
              />
            </div>
            <div class="default-icon-container" v-else>
              <svg v-if="currentTrack.title" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
              <svg v-else-if="currentIconType === 'music'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
              <svg v-else-if="currentIconType === 'download'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              <svg v-else-if="currentIconType === 'timer'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <svg v-else-if="currentIconType === 'sync'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/></svg>
              <svg v-else-if="currentIconType === 'notification'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0"/></svg>
              <svg v-else-if="currentIconType === 'message'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              <svg v-else-if="currentIconType === 'call'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M20 15.5a1.5 1.5 0 0 0-1.142-1.424l-1.757-.586a.5.5 0 0 0-.658.217l-.34 1.36a1.5 1.5 0 0 0 .217 1.516l1.598 1.065a1.5 1.5 0 0 0 1.342-.118l1.757-.586a1.5 1.5 0 0 0 1.142-1.424Z"/><path d="M15.5 20a1.5 1.5 0 0 0 1.143-1.424l1.757-.586a.5.5 0 0 0 .217-.658l-.34-1.36a1.5 1.5 0 0 0-1.516-.217l-1.598 1.065a1.5 1.5 0 0 0-.118 1.342l.586 1.757A1.5 1.5 0 0 0 15.5 20Z"/><path d="M16 10.5a1.5 1.5 0 0 0-1.142-1.424l-1.757-.586a.5.5 0 0 0-.658.217l-.34 1.36a1.5 1.5 0 0 0 .217 1.516l1.598 1.065a1.5 1.5 0 0 0 1.342-.118l1.757-.586A1.5 1.5 0 0 0 16 10.5Z"/><path d="M11.5 16a1.5 1.5 0 0 0 1.143-1.424l1.757-.586a.5.5 0 0 0 .217-.658l-.34-1.36a1.5 1.5 0 0 0-1.516-.217l-1.598 1.065a1.5 1.5 0 0 0-.118 1.342l.586 1.757A1.5 1.5 0 0 0 11.5 16Z"/><path d="M20 4a2 2 0 0 1 2 2v9a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V6a2 2 0 0 1 2-2h16Z"/></svg>
              <svg v-else-if="currentIconType === 'weather'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v2"/><path d="M12 20v2"/><path d="M4.93 4.93l1.41 1.41"/><path d="M17.66 17.66l1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="M6.34 17.66l-1.41 1.41"/><path d="M19.07 4.93l-1.41 1.41"/><circle cx="12" cy="12" r="3"/></svg>
              <svg v-else-if="currentIconType === 'battery'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M16 2H8a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2"/><path d="M22 11.67a1.34 1.34 0 0 0 0-2.67 1.34 1.34 0 0 0 0 2.67"/></svg>
              <svg v-else-if="currentIconType === 'bluetooth'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M7.5 3.21V20.8c0 .45.54.67.85.35l4.86-4.86a.5.5 0 0 1 .35-.15h6.87a.5.5 0 0 0 .35-.85l-2.38-3.57a.5.5 0 0 1 .15-.65l2.79-2.79a.5.5 0 0 0-.35-.85H14.5a.5.5 0 0 1-.35-.15l-5.86-5.86a.5.5 0 0 0-.85.36Z"/></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
            </div>
          </div>
          <div class="icon-badge" v-if="status.badge">{{ status.badge }}</div>
        </div>

        <div class="core-content" v-if="!isExpanded && !currentTrack.title">
          <span class="core-title">{{ status.title || '灵动岛' }}</span>
          <span class="core-subtitle" v-if="status.subtitle">{{ status.subtitle }}</span>
        </div>

        <div class="core-content music-content" v-if="!isExpanded && currentTrack.title">
          <span class="core-title">{{ currentTrack.title }}</span>
          <span class="core-subtitle">{{ currentTrack.artist }}{{ currentTrack.album ? ' - ' + currentTrack.album : '' }}</span>
        </div>

        <div class="core-trailing" v-if="!isExpanded && !currentTrack.title">
          <div class="core-progress" v-if="status.progress !== undefined">
            <div class="progress-ring">
              <svg viewBox="0 0 24 24">
                <circle class="progress-bg" cx="12" cy="12" r="10" />
                <circle
                  class="progress-value"
                  cx="12" cy="12" r="10"
                  :stroke-dasharray="circumference"
                  :stroke-dashoffset="progressOffset"
                />
              </svg>
              <span class="progress-text">{{ animatedProgress }}%</span>
            </div>
          </div>
          <div class="core-time" v-else-if="status.time">
            <span class="time-value" :class="{ 'flip-animation': isTimeAnimating }">{{ status.time }}</span>
          </div>
          <div class="core-indicator" v-else-if="status.statusIndicator">
            <span class="indicator-dot" :class="status.statusIndicator"></span>
          </div>
        </div>
      </div>

      <Transition name="expand" mode="out-in">
        <div class="island-expanded" v-if="isExpanded" key="expanded" @click="handleExpandedClick">
          <div class="expanded-body" v-if="!currentTrack.title">
            <div class="expanded-icon-wrap">
              <div class="expanded-icon" :class="iconAnimationClass">
                <svg v-if="currentIconType === 'music'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
                <svg v-else-if="currentIconType === 'download'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                <svg v-else-if="currentIconType === 'timer'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <svg v-else-if="currentIconType === 'sync'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/></svg>
                <svg v-else-if="currentIconType === 'notification'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                <svg v-else-if="currentIconType === 'message'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                <svg v-else-if="currentIconType === 'weather'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v2"/><path d="M12 20v2"/><path d="M4.93 4.93l1.41 1.41"/><path d="M17.66 17.66l1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="M6.34 17.66l-1.41 1.41"/><path d="M19.07 4.93l-1.41 1.41"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else-if="currentIconType === 'battery'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M16 2H8a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2"/><path d="M22 11.67a1.34 1.34 0 0 0 0-2.67 1.34 1.34 0 0 0 0 2.67"/></svg>
                <svg v-else-if="currentIconType === 'bluetooth'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M7.5 3.21V20.8c0 .45.54.67.85.35l4.86-4.86a.5.5 0 0 1 .35-.15h6.87a.5.5 0 0 0 .35-.85l-2.38-3.57a.5.5 0 0 1 .15-.65l2.79-2.79a.5.5 0 0 0-.35-.85H14.5a.5.5 0 0 1-.35-.15l-5.86-5.86a.5.5 0 0 0-.85.36Z"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
              </div>
            </div>

            <div class="expanded-info">
              <span class="expanded-title">{{ status.title || '灵动岛' }}</span>
              <span class="expanded-subtitle" v-if="status.subtitle">{{ status.subtitle }}</span>
              <div class="expanded-meta" v-if="status.meta">
                <span v-for="(meta, index) in status.meta" :key="index" class="meta-item">{{ meta }}</span>
              </div>
            </div>
          </div>

          <div class="expanded-progress" v-if="!currentTrack.title && status.progress !== undefined">
            <div class="progress-bar-expanded">
              <div class="progress-fill-expanded" :style="{ width: animatedProgress + '%' }"></div>
            </div>
            <div class="progress-info">
              <span>{{ status.progressText || `${animatedProgress}%` }}</span>
              <span v-if="status.speed">{{ status.speed }}</span>
            </div>
          </div>

          <div class="expanded-timer" v-if="!currentTrack.title && status.time">
            <span class="timer-display" :class="{ 'flip-animation': isTimeAnimating }">{{ status.time }}</span>
            <span class="timer-label" v-if="status.timeLabel">{{ status.timeLabel }}</span>
          </div>

          <div class="expanded-controls" v-if="!currentTrack.title && status.actions && status.actions.length > 0">
            <button
              v-for="(action, index) in status.actions"
              :key="index"
              class="expanded-btn"
              :class="{ primary: action.primary }"
              @click.stop="handleActionClick(action)"
            >
              {{ action.label }}
            </button>
          </div>

          <div class="expanded-content" v-if="status.content">
            <div v-html="status.content"></div>
          </div>

          <div class="expanded-music" v-if="currentTrack.title">
            <div class="music-header">
            <div class="music-cover">
              <div class="album-cover-container" v-if="currentTrack.thumbnail">
                <img 
                  :src="currentTrack.thumbnail" 
                  alt="Album cover"
                  class="cover-image"
                />
              </div>
              <div class="default-icon-container" v-else>
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="url(#musicGradient)">
                  <path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/>
                  <defs>
                    <linearGradient id="musicGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" style="stop-color:#4299f4"/>
                      <stop offset="100%" style="stop-color:#3184e0"/>
                    </linearGradient>
                  </defs>
                </svg>
              </div>
            </div>
            <div class="music-info">
              <span class="music-title">{{ currentTrack.title }}</span>
              <span class="music-artist">{{ currentTrack.artist }}</span>
              <span class="music-album" v-if="currentTrack.album">{{ currentTrack.album }}</span>
            </div>
          </div>
            
            <div class="music-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: mediaProgress + '%' }"></div>
              </div>
              <div class="progress-time">
                <span>{{ formatTime(currentTrack.position) }}</span>
                <span>{{ formatTime(currentTrack.duration) }}</span>
              </div>
            </div>
            
            <div class="music-controls">
              <button class="music-btn music-play-next-btn" @click.stop="prevTrack">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="15 4 5 12 15 20"/>
                  <polygon points="23 4 13 12 23 20"/>
                </svg>
              </button>
              <button class="music-btn play-btn" @click.stop="playPause">
                <svg v-if="!currentTrack.isPlaying" xmlns="http://www.w3.org/2000/svg" width="26" height="26" viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="5 3 19 12 5 21"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="26" height="26" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="4" width="4" height="16"/>
                  <rect x="14" y="4" width="4" height="16"/>
                </svg>
              </button>
              <button class="music-btn music-play-next-btn" @click.stop="nextTrack">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="9 4 19 12 9 20"/>
                  <polygon points="1 4 11 12 1 20"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <div class="task-indicators" v-if="taskQueue.length > 0 && !isExpanded">
      <div 
        v-for="(task, index) in taskQueue.slice(0, 3)" 
        :key="index" 
        class="task-indicator"
        :style="{ animationDelay: index * 0.1 + 's' }"
        @click="switchTask(index)"
      ></div>
      <div class="task-indicator more" v-if="taskQueue.length > 3">+{{ taskQueue.length - 3 }}</div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isExpanded = ref(false)
const isMorphing = ref(false)
const isCollapsing = ref(false)
const isMinimal = ref(false)
const currentStyle = ref('rounded')
const currentPosition = ref('top')
const animationsEnabled = ref(true)
const animatedProgress = ref(0)
const isTimeAnimating = ref(false)

const status = ref({
  type: 'activity',
  title: '聚合空间',
  subtitle: '运行中',
  progress: undefined,
  time: undefined,
  statusIndicator: 'active',
  badge: null
})

const taskQueue = ref([])
const currentTaskIndex = ref(0)

const currentIconType = computed(() => status.value.type)

const currentTrack = ref({
  title: '',
  artist: '',
  album: '',
  duration: 0,
  position: 0,
  isPlaying: false,
  thumbnail: null
})

const formatTime = (seconds) => {
  if (!seconds || seconds <= 0 || seconds > 10000) {
    return '--:--'
  }
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const mediaProgress = computed(() => {
  if (!currentTrack.value.duration || currentTrack.value.duration <= 0) return 0
  return Math.min(100, Math.round((currentTrack.value.position / currentTrack.value.duration) * 100))
})

const playPause = async () => {
  if (currentTrack.value.isPlaying) {
    await window.electronAPI?.media?.pause()
  } else {
    await window.electronAPI?.media?.play()
  }
}

const nextTrack = async () => {
  await window.electronAPI?.media?.next()
}

const prevTrack = async () => {
  await window.electronAPI?.media?.previous()
}
const circumference = 2 * Math.PI * 10

const progressOffset = computed(() => {
  return circumference - (animatedProgress.value / 100) * circumference
})

const iconAnimationClass = computed(() => {
  switch (status.value.type) {
    case 'music': return 'pulse-animation'
    case 'sync': return 'spin-animation'
    case 'download': return 'bounce-animation'
    default: return ''
  }
})

let removeExpandedListener = null
let removeUpdateListener = null
let removeStyleListener = null
let removePositionListener = null
let removeSettingsListener = null
let progressAnimationFrame = null

const animateProgress = (from, to, duration = 500) => {
  const startTime = performance.now()
  
  const animate = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animatedProgress.value = Math.round(from + (to - from) * eased)
    
    if (progress < 1) {
      progressAnimationFrame = requestAnimationFrame(animate)
    }
  }
  
  if (progressAnimationFrame) {
    cancelAnimationFrame(progressAnimationFrame)
  }
  progressAnimationFrame = requestAnimationFrame(animate)
}

watch(() => status.value.progress, (newVal, oldVal) => {
  if (newVal !== undefined) {
    animateProgress(oldVal || 0, newVal)
  }
})

watch(() => status.value.time, () => {
  isTimeAnimating.value = true
  setTimeout(() => {
    isTimeAnimating.value = false
  }, 300)
})

const handleClick = (event) => {
  event.stopPropagation()
  if (!isExpanded.value && !isCollapsing.value) {
    expand()
  }
}

const handleExpandedClick = (event) => {
  event.stopPropagation()
  collapse()
}

const handleDocumentClick = (event) => {
  if (isExpanded.value) {
    const island = document.querySelector('.dynamic-island')
    if (island && !island.contains(event.target)) {
      collapse()
    }
  }
}

const expand = () => {
  isExpanded.value = true
  isMinimal.value = false
  window.electronAPI?.dynamicIsland?.expand()
}

const collapse = () => {
  if (isCollapsing.value) return
  isCollapsing.value = true
  
  isExpanded.value = false
  window.electronAPI?.dynamicIsland?.collapse()
  
  setTimeout(() => {
    isCollapsing.value = false
  }, 500)
}

const handleMouseEnter = () => {
  window.electronAPI?.dynamicIsland?.mouseEnter()
}

const handleMouseLeave = () => {
  window.electronAPI?.dynamicIsland?.mouseLeave()
}

const handleActionClick = (action) => {
  if (action.handler) {
    action.handler()
  }
  if (action.navigate) {
    router.push(action.navigate)
    window.electronAPI?.dynamicIsland?.collapse()
    window.electronAPI?.dynamicIsland?.hide()
  }
  collapse()
}

const switchTask = (index) => {
  if (taskQueue.value[index]) {
    currentTaskIndex.value = index
    status.value = { ...taskQueue.value[index] }
    taskQueue.value.splice(index, 1)
    taskQueue.value.push({ ...status.value })
  }
}

const addTask = (task) => {
  const exists = taskQueue.value.find(t => t.id === task.id)
  if (!exists) {
    taskQueue.value.push(task)
  }
}

const removeTask = (taskId) => {
  const index = taskQueue.value.findIndex(t => t.id === taskId)
  if (index !== -1) {
    taskQueue.value.splice(index, 1)
  }
}

const startDrag = (e) => {
  if (isExpanded.value) return

  const startX = e.screenX
  const startY = e.screenY

  const handleMouseMove = (moveEvent) => {
    const deltaX = moveEvent.screenX - startX
    const deltaY = moveEvent.screenY - startY
    if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
      window.electronAPI?.dynamicIsland?.update({ dragging: true })
    }
  }

  const handleMouseUp = (upEvent) => {
    const deltaX = upEvent.screenX - startX
    const deltaY = upEvent.screenY - startY
    window.electronAPI?.dynamicIsland?.dragEnd?.({ x: deltaX, y: deltaY })
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

onMounted(async () => {
  animatedProgress.value = status.value.progress || 0
  document.addEventListener('click', handleDocumentClick)
  
  document.body.classList.add('dynamic-island-body')
  
  console.log('[DynamicIslandView] Component mounted')
  
  if (window.electronAPI?.dynamicIsland) {
    const settings = await window.electronAPI.dynamicIsland.getSettings()
    if (settings) {
      currentStyle.value = settings.style || 'rounded'
      currentPosition.value = settings.position || 'top'
      animationsEnabled.value = settings.animationsEnabled !== undefined ? settings.animationsEnabled : true
    }

    removeExpandedListener = window.electronAPI.dynamicIsland.onExpanded((expanded) => {
      isExpanded.value = expanded
    })

    removeUpdateListener = window.electronAPI.dynamicIsland.onUpdate((data) => {
      if (data) {
        if (data.task) {
          addTask(data.task)
        } else if (data.removeTask) {
          removeTask(data.removeTask)
        } else if (data.minimal !== undefined) {
          isMinimal.value = data.minimal
        } else {
          status.value = { ...status.value, ...data }
        }
      }
    })

    removeStyleListener = window.electronAPI.dynamicIsland.onStyleUpdated((style) => {
      currentStyle.value = style
    })

    removePositionListener = window.electronAPI.dynamicIsland.onPositionUpdated((position) => {
      currentPosition.value = position
    })

    removeSettingsListener = window.electronAPI.dynamicIsland.onSettingsUpdated((settings) => {
      console.log('[DynamicIsland] settingsUpdated received:', settings)
      if (settings) {
        if (settings.style !== undefined) {
          console.log('[DynamicIsland] Updating style from', currentStyle.value, 'to', settings.style)
          currentStyle.value = settings.style
        }
        if (settings.position !== undefined) {
          console.log('[DynamicIsland] Updating position from', currentPosition.value, 'to', settings.position)
          currentPosition.value = settings.position
        }
        if (settings.animationsEnabled !== undefined) {
          console.log('[DynamicIsland] Updating animationsEnabled from', animationsEnabled.value, 'to', settings.animationsEnabled)
          animationsEnabled.value = settings.animationsEnabled
        }
        if (settings.devToolsEnabled !== undefined) {
          console.log('[DynamicIsland] devToolsEnabled changed to:', settings.devToolsEnabled)
          if (settings.devToolsEnabled) {
            window.electronAPI.dynamicIsland.openDevTools()
          } else {
            window.electronAPI.dynamicIsland.closeDevTools()
          }
        }
      }
    })

    window.electronAPI.dynamicIsland.show()
  }
  
  console.log('[DynamicIslandView] Checking electronAPI.media')
  
  if (window.electronAPI?.media) {
    console.log('[DynamicIslandView] electronAPI.media available')
    
    window.electronAPI.media.getCurrentTrack().then(track => {
      if (track) {
        console.log('[DynamicIslandView] getCurrentTrack returned:', track)
        let thumbnailBase64 = `data:image/png;base64,${track.thumbnail}`;
        track.thumbnail = thumbnailBase64;
        currentTrack.value = track
      }
    })
    
    window.electronAPI.media.onUpdate((track) => {
      if (track) {
        console.log('[DynamicIslandView] Received media update:', track)
        let thumbnailBase64 = `data:image/png;base64,${track.thumbnail}`;
        track.thumbnail = thumbnailBase64;
        currentTrack.value = track
      }
    })
  } else {
    console.log('[DynamicIslandView] electronAPI.media NOT available')
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
  if (removeExpandedListener) removeExpandedListener()
  if (removeUpdateListener) removeUpdateListener()
  if (removeStyleListener) removeStyleListener()
  if (removePositionListener) removePositionListener()
  if (removeSettingsListener) removeSettingsListener()
  if (progressAnimationFrame) cancelAnimationFrame(progressAnimationFrame)
  
  document.body.classList.remove('dynamic-island-body')
})
</script>

<style>
body.dynamic-island-body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  background-color: transparent !important;
  overflow: hidden !important;
}
</style>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 1. 容器：固定舞台，不参与动画 */
.dynamic-island-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 250px;
  pointer-events: none;
}

/* 2. 岛体：负责视觉和动画 */
.dynamic-island {
  pointer-events: auto;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  
  /* 核心动画配置 - 丝滑弹性效果 */
  transition: 
    height 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94),
    top 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  /* 固定宽度，避免布局重排 */
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 360px;
  height: 70px;
  padding: 4px;
}

/* 收起状态 */
.dynamic-island.collapsed {
  height: 70px;
}

/* 展开状态 */
.dynamic-island.expanded {
  height: 220px;
  top: 15px;
}

/* 收起状态下隐藏展开内容 */
.dynamic-island.collapsed .island-expanded {
  opacity: 0;
  pointer-events: none;
  max-height: 0;
  overflow: hidden;
  z-index: -1;
  position: absolute;
  visibility: hidden;
  transform: scale(0);
  width: 0;
  height: 0;
}

/* 展开状态下显示展开内容 */
.dynamic-island.expanded .island-expanded {
  opacity: 1;
  pointer-events: auto;
  max-height: 200px;
}

/* 样式变体 */
.dynamic-island.rounded .island-morph {
  border-radius: 24px;
}

.dynamic-island.rounded.expanded .island-morph {
  border-radius: 20px;
}

.dynamic-island.pill {
  width: 100%;
}

.dynamic-island.pill .island-morph {
  border-radius: 48px;
}

.dynamic-island.pill.expanded .island-morph {
  border-radius: 24px;
}

.dynamic-island.compact {
  width: 160px;
}

.dynamic-island.compact .island-morph {
  border-radius: 32px;
}

.dynamic-island.compact.expanded {
  width: 320px;
  height: 180px;
}

/* 禁用动画样式 */
.dynamic-island.no-animations {
  transition: none !important;
}

.dynamic-island.no-animations .island-morph {
  transition: none !important;
}

.dynamic-island.no-animations .island-core {
  transition: none !important;
}

.dynamic-island.no-animations .core-icon {
  transition: none !important;
  animation: none !important;
}

.dynamic-island.no-animations .expand-enter-active,
.dynamic-island.no-animations .expand-leave-active {
  animation: none !important;
}

.island-morph {
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.85);
  border-radius: 24px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-radius 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  cursor: pointer;
  position: relative;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.dynamic-island.expanded .island-morph {
  border-radius: 20px;
  flex-direction: column;
  justify-content: center;
}

.island-morph::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, transparent 50%);
  pointer-events: none;
}

.island-morph.morphing {
  transform: scale(1.02);
}

.dynamic-island:hover .island-morph {
  border-color: rgba(66, 153, 244, 0.25);
}

.island-core {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 6px 16px;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;
  width: 100%;
  box-sizing: border-box;
  justify-content: center;
}

.core-leading {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}

.core-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4299f4 0%, #3184e0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.album-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.album-cover-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.default-icon-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.core-icon :deep(svg) {
  width: 14px;
  height: 14px;
}

.core-icon.pulse-animation {
  animation: pulse 1.5s ease-in-out infinite;
}

.core-icon.spin-animation {
  animation: spin 2s linear infinite;
}

.core-icon.bounce-animation {
  animation: bounce 0.6s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.icon-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: linear-gradient(135deg, #ff3b5c 0%, #ff1744 100%);
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(255, 59, 92, 0.4);
}

.core-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  gap: 2px;
}

.core-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s ease;
}

.core-subtitle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.core-trailing {
  flex-shrink: 0;
}

.core-progress {
  display: flex;
  align-items: center;
}

.progress-ring {
  position: relative;
  width: 28px;
  height: 28px;
}

.progress-ring svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-ring circle {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
}

.progress-bg {
  stroke: rgba(255, 255, 255, 0.15);
}

.progress-value {
  stroke: #4299f4;
  transition: stroke-dashoffset 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-ring .progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 9px;
  font-weight: 600;
  color: #ffffff;
}

.core-time {
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}

.time-value.flip-animation {
  animation: flip 0.3s ease-out;
}

@keyframes flip {
  0% { transform: scale(1.2); opacity: 0; }
  50% { transform: scale(0.9); }
  100% { transform: scale(1); opacity: 1; }
}

.core-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.indicator-dot.active {
  background: #4ade80;
  box-shadow: 0 0 8px rgba(74, 222, 128, 0.6);
}

.indicator-dot.warning {
  background: #fbbf24;
  box-shadow: 0 0 8px rgba(251, 191, 36, 0.6);
}

.indicator-dot.error {
  background: #ff6b6b;
  box-shadow: 0 0 8px rgba(255, 107, 107, 0.6);
}

.island-expanded {
  padding: 16px 20px;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  color: white;
}

.island-expanded::-webkit-scrollbar {
  width: 0;
  height: 0;
  background: transparent;
}

.expanded-header {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}

.expanded-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.expanded-close:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  transform: scale(1.1);
}

.expanded-close :deep(svg) {
  width: 12px;
  height: 12px;
}

.expanded-body {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 8px 0 16px;
  width: 100%;
}

.expanded-icon-wrap {
  flex-shrink: 0;
}

.expanded-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #4299f4 0%, #3184e0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.expanded-icon :deep(svg) {
  width: 28px;
  height: 28px;
}

.expanded-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.expanded-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.expanded-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.expanded-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.meta-item {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 10px;
}

.expanded-progress {
  padding: 12px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.progress-bar-expanded {
  width: 100%;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill-expanded {
  height: 100%;
  background: linear-gradient(90deg, #4299f4, #66b3ff);
  border-radius: 3px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  padding-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.expanded-timer {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
}

.timer-display {
  font-size: 36px;
  font-weight: 700;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
  letter-spacing: 2px;
}

.timer-display.flip-animation {
  animation: flip 0.3s ease-out;
}

.timer-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.expanded-controls {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 12px 0;
}

.expanded-btn {
  padding: 10px 24px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.07);
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.expanded-btn:hover {
  background: rgba(255, 255, 255, 0.13);
  border-color: rgba(255, 255, 255, 0.26);
  transform: scale(1.02);
}

.expanded-btn.primary {
  background: linear-gradient(135deg, #4299f4 0%, #3184e0 100%);
  border-color: transparent;
}

.expanded-btn.primary:hover {
  background: linear-gradient(135deg, #3b8ef5 0%, #2a7ae0 100%);
  transform: scale(1.02);
}

.expanded-content {
  padding-top: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.expanded-music {
  width: 100%;
}

.music-header {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 0 8px;
}

.music-cover {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(66, 153, 244, 0.3) 0%, rgba(49, 132, 224, 0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.music-cover .album-cover-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.music-cover .default-icon-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.music-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.music-title {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.music-artist {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.music-progress {
  padding: 12px 8px 0;
}

.music-progress .progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  overflow: hidden;
  position: relative;
}

.music-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4299f4 0%, #3184e0 100%);
  border-radius: 2px;
  transition: width 0.1s linear;
  position: relative;
}

.music-progress .progress-fill::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.5);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.music-progress .progress-fill:hover::after {
  opacity: 1;
}

.progress-time {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  font-variant-numeric: tabular-nums;
}

.music-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 48px;
  padding: 16px 8px 8px;
}

.music-btn {
  padding: 0;
  margin: 0;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  font-size: 0;
  position: relative;
}

.music-play-next-btn {
  width: 40px;
  height: 40px;
}

.music-btn svg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
}

.music-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  transform: scale(1.08);
}

.music-btn:active {
  transform: scale(0.95);
}

.music-btn.play-btn {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #4299f4 0%, #3184e0 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(66, 153, 244, 0.4);
}

.music-btn.play-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 8px 24px rgba(66, 153, 244, 0.5);
}

.task-indicators {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.task-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
  animation: fadeInUp 0.3s ease-out forwards;
  opacity: 0;
}

.task-indicator:hover {
  background: rgba(255, 255, 255, 0.6);
  transform: scale(1.2);
}

.task-indicator.more {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  font-size: 10px;
  color: rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.expand-enter-active {
  animation: morph-in 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
}

.expand-leave-active {
  animation: morph-out 0.25s cubic-bezier(0.4, 0, 1, 1);
  will-change: transform, opacity;
}

@keyframes morph-in {
  0% {
    opacity: 0;
    transform: translateY(-12px) scale(0.96);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes morph-out {
  0% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  100% {
    opacity: 0;
    transform: translateY(-16px) scale(0.97);
  }
}
</style>