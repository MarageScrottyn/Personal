<template>
  <div class="notification-center-window">
    <div class="notification-center">
      <!-- 顶部标题栏 -->
      <div class="notification-header">
        <div class="header-tabs">
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'notifications' }"
            @click="activeTab = 'notifications'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            <span>通知</span>
            <span v-if="notifications.length > 0" class="notification-count">{{ notifications.length }}</span>
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: activeTab === 'calendar' }"
            @click="activeTab = 'calendar'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            <span>日历</span>
          </button>
        </div>
        <button class="header-close" @click="closeWindow">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- 通知列表 -->
      <div v-if="activeTab === 'notifications'" class="notifications-section">
        <div v-if="notifications.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span class="empty-title">没有通知</span>
          <span class="empty-desc">当有新通知时，它们会显示在这里</span>
        </div>

        <div v-else class="notifications-list">
          <div 
            v-for="notification in notifications" 
            :key="notification.id" 
            class="notification-item"
            :class="{ expanded: expandedId === notification.id }"
          >
            <div class="notification-header" @click="toggleExpand(notification.id)">
              <div class="notification-icon" :class="notification.type">
                <svg v-if="notification.type === 'info'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
                <svg v-else-if="notification.type === 'warning'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 9v2m0 4h.01"/>
                  <path d="M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z"/>
                </svg>
                <svg v-else-if="notification.type === 'error'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18"/>
                  <path d="M6 6l12 12"/>
                </svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <div class="notification-content">
                <span class="notification-title">{{ notification.title }}</span>
                <p class="notification-message">{{ notification.message }}</p>
              </div>
              <span class="notification-time">{{ notification.time }}</span>
            </div>
            <div v-if="expandedId === notification.id" class="notification-actions">
              <button class="action-btn">操作1</button>
              <button class="action-btn">操作2</button>
            </div>
          </div>
        </div>

        <button v-if="notifications.length > 0" class="clear-all-btn" @click="clearAll">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18"/>
            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
          </svg>
          <span>全部清除</span>
        </button>
      </div>

      <!-- 日历视图 -->
      <div v-if="activeTab === 'calendar'" class="calendar-section">
        <div class="calendar-header">
          <button class="nav-btn" @click="prevMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <span class="current-month">{{ currentMonthYear }}</span>
          <button class="nav-btn" @click="nextMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>

        <div class="calendar-weekdays">
          <span v-for="day in weekdays" :key="day" class="weekday">{{ day }}</span>
        </div>

        <div class="calendar-grid">
          <div 
            v-for="(day, index) in calendarDays" 
            :key="index" 
            class="calendar-day"
            :class="{ 
              'other-month': !day.currentMonth,
              'today': day.isToday,
              'selected': day.date === selectedDate
            }"
            @click="selectDate(day)"
          >
            <span class="day-number">{{ day.day }}</span>
            <span v-if="day.lunar" class="day-lunar">{{ day.lunar }}</span>
          </div>
        </div>

        <!-- 焦点时间 -->
        <div class="focus-section">
          <div class="focus-header">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
            </svg>
            <span class="focus-title">焦点时间</span>
          </div>
          <div class="focus-timer">
            <button class="timer-btn" @click="adjustFocusTime(-15)" :disabled="focusTime <= 15">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
            <span class="focus-duration">{{ focusTime }}分钟</span>
            <button class="timer-btn" @click="adjustFocusTime(15)" :disabled="focusTime >= 180">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </button>
          </div>
          <button class="start-focus-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"/>
            </svg>
            <span>开始专注</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const activeTab = ref('notifications')
const expandedId = ref(null)
const focusTime = ref(45)
const currentDate = new Date()
const selectedDate = ref(currentDate.toDateString())

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const notifications = ref([
  {
    id: 1,
    type: 'info',
    title: '系统更新',
    message: '您的设备已更新到最新版本',
    time: '刚刚'
  },
  {
    id: 2,
    type: 'warning',
    title: '存储空间不足',
    message: '您的C盘存储空间不足，请释放空间',
    time: '10分钟前'
  },
  {
    id: 3,
    type: 'success',
    title: '文件已保存',
    message: '文档 "工作计划.docx" 已自动保存',
    time: '1小时前'
  }
])

const currentMonthYear = computed(() => {
  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()
  const months = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
  return `${months[month]} ${year}`
})

const calendarDays = computed(() => {
  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const days = []
  
  const startPadding = firstDay.getDay()
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  
  for (let i = startPadding - 1; i >= 0; i--) {
    days.push({
      day: prevMonthLastDay - i,
      currentMonth: false,
      isToday: false,
      lunar: ''
    })
  }
  
  const today = new Date()
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const isToday = today.getDate() === i && 
                    today.getMonth() === month && 
                    today.getFullYear() === year
    days.push({
      day: i,
      currentMonth: true,
      isToday: isToday,
      date: new Date(year, month, i).toDateString(),
      lunar: getLunarDate(i)
    })
  }
  
  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    days.push({
      day: i,
      currentMonth: false,
      isToday: false,
      lunar: ''
    })
  }
  
  return days
})

const getLunarDate = (day) => {
  const lunarDates = ['初一', '初二', '初三', '初四', '初五', '初六', '初七', '初八', '初九', '初十',
                      '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十',
                      '廿一', '廿二', '廿三', '廿四', '廿五', '廿六', '廿七', '廿八', '廿九', '三十']
  return lunarDates[day - 1] || ''
}

const prevMonth = () => {
  currentDate.setMonth(currentDate.getMonth() - 1)
}

const nextMonth = () => {
  currentDate.setMonth(currentDate.getMonth() + 1)
}

const selectDate = (day) => {
  if (day.date) {
    selectedDate.value = day.date
  }
}

const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

const clearAll = () => {
  notifications.value = []
}

const adjustFocusTime = (delta) => {
  focusTime.value += delta
}

const closeWindow = () => {
  window.electronAPI?.taskbar?.closeNotificationCenter()
}

const handleKeydown = (event) => {
  if (event.key === 'Escape') {
    closeWindow()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.notification-center-window {
  width: 360px;
  height: 520px;
  background: linear-gradient(180deg, #1c1c1c 0%, #1a1a2e 100%);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.notification-center {
  display: flex;
  flex-direction: column;
  height: 100%;
  color: #e5e7eb;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.08);
}

.tab-btn.active {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

.notification-count {
  padding: 2px 6px;
  background: #ef4444;
  border-radius: 10px;
  font-size: 11px;
  color: white;
}

.header-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s;
}

.header-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

.notifications-section {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6b7280;
}

.empty-title {
  margin-top: 12px;
  font-size: 14px;
  font-weight: 500;
}

.empty-desc {
  margin-top: 4px;
  font-size: 12px;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  overflow: hidden;
}

.notification-header {
  display: flex;
  gap: 10px;
  padding: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.notification-header:hover {
  background: rgba(255, 255, 255, 0.05);
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-icon.info {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.notification-icon.warning {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.notification-icon.error {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.notification-icon.success {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 13px;
  font-weight: 500;
  color: #e5e7eb;
}

.notification-message {
  margin: 2px 0 0;
  font-size: 12px;
  color: #9ca3af;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 11px;
  color: #6b7280;
  flex-shrink: 0;
}

.notification-actions {
  display: flex;
  gap: 8px;
  padding: 0 10px 10px;
}

.action-btn {
  flex: 1;
  padding: 8px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #e5e7eb;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.clear-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  margin-top: auto;
}

.clear-all-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #e5e7eb;
}

.calendar-section {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.nav-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 6px;
  color: #e5e7eb;
  cursor: pointer;
  transition: background 0.15s;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}

.current-month {
  font-size: 14px;
  font-weight: 600;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  color: #6b7280;
  padding: 4px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.calendar-day:hover {
  background: rgba(255, 255, 255, 0.1);
}

.calendar-day.other-month {
  color: #4b5563;
}

.calendar-day.today {
  background: #3b82f6;
  color: white;
}

.calendar-day.selected {
  background: rgba(59, 130, 246, 0.3);
}

.day-number {
  font-size: 13px;
}

.day-lunar {
  font-size: 9px;
  opacity: 0.7;
}

.focus-section {
  margin-top: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.focus-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.focus-title {
  font-size: 13px;
  font-weight: 500;
}

.focus-timer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 12px;
}

.timer-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #e5e7eb;
  cursor: pointer;
  transition: all 0.15s;
}

.timer-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.15);
}

.timer-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.focus-duration {
  font-size: 18px;
  font-weight: 600;
}

.start-focus-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: #3b82f6;
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.start-focus-btn:hover {
  background: #2563eb;
}

.notifications-section::-webkit-scrollbar,
.calendar-section::-webkit-scrollbar {
  width: 6px;
}

.notifications-section::-webkit-scrollbar-track,
.calendar-section::-webkit-scrollbar-track {
  background: transparent;
}

.notifications-section::-webkit-scrollbar-thumb,
.calendar-section::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
}

.notifications-section::-webkit-scrollbar-thumb:hover,
.calendar-section::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}
</style>