<template>
  <div 
    class="notification-center" 
    :class="{ visible: isVisible }"
    @click.self="$emit('close')"
  >
    <div class="notification-center-content">
      <!-- 通知区域 -->
      <div class="notifications-section">
        <div class="section-header">
          <h3 class="section-title">通知</h3>
          <button class="clear-all-btn" @click="clearAllNotifications">
            <span>全部清除</span>
          </button>
        </div>

        <div class="notifications-list">
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
                  <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                </svg>
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                </svg>
              </div>
              <div class="notification-content">
                <span class="notification-title">{{ notification.title }}</span>
                <p class="notification-message">{{ notification.message }}</p>
              </div>
              <span class="notification-time">{{ notification.time }}</span>
            </div>
            
            <div v-if="expandedId === notification.id" class="notification-actions">
              <button class="action-btn" @click="handleNotificationAction(notification, 'close')">
                关闭
              </button>
            </div>
          </div>
        </div>

        <div v-if="notifications.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0 1 18 14.158V11a6.002 6.002 0 0 0-4-5.659V5a2 2 0 1 0-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 1 1-6 0v-1m6 0H9"/>
          </svg>
          <p>暂无通知</p>
        </div>
      </div>

      <!-- 日历区域 -->
      <div class="calendar-section">
        <div class="calendar-header">
          <div class="calendar-title">
            <span>{{ currentDateDisplay }}</span>
            <span class="lunar-date">{{ lunarDate }}</span>
          </div>
          <button class="collapse-btn" @click="$emit('collapse')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 15l7-7 7 7"/>
            </svg>
          </button>
        </div>

        <div class="calendar-nav">
          <button class="nav-btn" @click="prevMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <span class="month-year">{{ currentMonthYear }}</span>
          <button class="nav-btn" @click="nextMonth">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>

        <div class="calendar-weekdays">
          <span v-for="day in weekdays" :key="day" class="weekday">{{ day }}</span>
        </div>

        <div class="calendar-days">
          <div 
            v-for="(day, index) in calendarDays" 
            :key="index"
            class="calendar-day"
            :class="{ 
              'other-month': !day.currentMonth,
              'today': day.isToday,
              'selected': day.date === selectedDate,
              'weekend': day.isWeekend
            }"
            @click="selectDate(day)"
          >
            {{ day.day }}
          </div>
        </div>

        <!-- 焦点时间 -->
        <div class="focus-section">
          <div class="focus-header">
            <span class="focus-label">焦点时间</span>
            <div class="focus-controls">
              <button class="focus-btn" @click="decreaseFocusTime">-</button>
              <span class="focus-time">30 分钟</span>
              <button class="focus-btn" @click="increaseFocusTime">+</button>
            </div>
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

<script setup>import { ref, computed, onMounted } from 'vue';
const props = defineProps({
 isVisible: {
 type: Boolean,
 default: false
 }
});
defineEmits(['close', 'collapse']);
const notifications = ref([
 {
 id: 1,
 type: 'info',
 title: 'TRAE CN',
 message: '等待操作\n任务正在等待您的回复，请返回 TRAE 进行下一',
 time: '2分钟前',
 expanded: false
 },
 {
 id: 2,
 type: 'warning',
 title: 'Windows 安全中心',
 message: '启用 Windows 防火墙\nWindows 防火墙已关闭。点击或单击以启用。',
 time: '10分钟前',
 expanded: false
 }
]);
const expandedId = ref(null);
const currentDate = ref(new Date());
const selectedDate = ref(new Date().toISOString().split('T')[0]);
const focusMinutes = ref(30);
const weekdays = ['一', '二', '三', '四', '五', '六', '日'];
const currentDateDisplay = computed(() => {
 const month = currentDate.value.getMonth() + 1;
 const day = currentDate.value.getDate();
 const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
 const weekDay = weekDays[currentDate.value.getDay()];
 return `${month}月${day}日, ${weekDay}`;
});
const lunarDate = computed(() => {
 return '四月初八';
});
const currentMonthYear = computed(() => {
 const year = currentDate.value.getFullYear();
 const month = currentDate.value.getMonth() + 1;
 return `${year}年${month}月`;
});
const calendarDays = computed(() => {
 const year = currentDate.value.getFullYear();
 const month = currentDate.value.getMonth();
 const firstDay = new Date(year, month, 1);
 const lastDay = new Date(year, month + 1, 0);
 const days = [];
 const startDay = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
 const prevMonthLastDay = new Date(year, month, 0).getDate();
 for (let i = startDay - 1; i >= 0; i--) {
 days.push({
 day: prevMonthLastDay - i,
 date: new Date(year, month - 1, prevMonthLastDay - i).toISOString().split('T')[0],
 currentMonth: false,
 isToday: false,
 isWeekend: false
 });
 }
 for (let i = 1; i <= lastDay.getDate(); i++) {
 const date = new Date(year, month, i);
 const isToday = date.toDateString() === new Date().toDateString();
 const dayOfWeek = date.getDay();
 days.push({
 day: i,
 date: date.toISOString().split('T')[0],
 currentMonth: true,
 isToday,
 isWeekend: dayOfWeek === 0 || dayOfWeek === 6
 });
 }
 const remainingCells = 42 - days.length;
 for (let i = 1; i <= remainingCells; i++) {
 days.push({
 day: i,
 date: new Date(year, month + 1, i).toISOString().split('T')[0],
 currentMonth: false,
 isToday: false,
 isWeekend: false
 });
 }
 return days;
});
const toggleExpand = (id) => {
 expandedId.value = expandedId.value === id ? null : id;
};
const clearAllNotifications = () => {
 notifications.value = [];
};
const handleNotificationAction = (notification, action) => {
 console.log('Notification action:', action, notification);
};
const prevMonth = () => {
 const newDate = new Date(currentDate.value);
 newDate.setMonth(newDate.getMonth() - 1);
 currentDate.value = newDate;
};
const nextMonth = () => {
 const newDate = new Date(currentDate.value);
 newDate.setMonth(newDate.getMonth() + 1);
 currentDate.value = newDate;
};
const selectDate = (day) => {
 selectedDate.value = day.date;
};
const decreaseFocusTime = () => {
 if (focusMinutes.value > 5) {
 focusMinutes.value -= 5;
 }
};
const increaseFocusTime = () => {
 if (focusMinutes.value < 120) {
 focusMinutes.value += 5;
 }
};
onMounted(() => {
 selectedDate.value = new Date().toISOString().split('T')[0];
});
</script>

<style scoped>
.notification-center {
  position: fixed;
  bottom: 48px;
  right: 8px;
  width: 360px;
  max-height: 85vh;
  background: linear-gradient(180deg, #2d2d2d 0%, #1a1a1a 100%);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  opacity: 0;
  visibility: hidden;
  transform: translateY(20px);
  transition: all 0.2s ease;
  z-index: 1000;
}

.notification-center.visible {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.notification-center-content {
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  overflow-y: auto;
}

.notifications-section {
  padding: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.clear-all-btn {
  background: transparent;
  border: none;
  color: #3b82f6;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.15s ease;
}

.clear-all-btn:hover {
  background: rgba(59, 130, 246, 0.1);
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s ease;
}

.notification-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.notification-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  cursor: pointer;
}

.notification-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
}

.notification-icon.info {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.notification-icon.warning {
  background: rgba(234, 179, 8, 0.15);
  color: #eab308;
}

.notification-icon.default {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 2px;
  display: block;
}

.notification-message {
  font-size: 12px;
  color: #9ca3af;
  margin: 0;
  white-space: pre-wrap;
}

.notification-time {
  font-size: 11px;
  color: #6b7280;
  flex-shrink: 0;
}

.notification-actions {
  padding: 0 10px 10px;
}

.action-btn {
  width: 100%;
  padding: 8px;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 6px;
  color: #e5e7eb;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: #6b7280;
}

.empty-state svg {
  margin-bottom: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 12px;
}

.calendar-section {
  padding: 12px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.calendar-title {
  display: flex;
  flex-direction: column;
}

.calendar-title span:first-child {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.lunar-date {
  font-size: 11px;
  color: #9ca3af;
}

.collapse-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.calendar-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.nav-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s ease;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #e5e7eb;
}

.month-year {
  font-size: 13px;
  font-weight: 500;
  color: #e5e7eb;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 11px;
  color: #6b7280;
  padding: 4px 0;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 16px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
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
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.calendar-day.weekend {
  color: #9ca3af;
}

.focus-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
}

.focus-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.focus-label {
  font-size: 12px;
  color: #9ca3af;
}

.focus-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.focus-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: none;
  border-radius: 4px;
  color: #e5e7eb;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.focus-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.focus-time {
  font-size: 12px;
  color: #e5e7eb;
  min-width: 60px;
  text-align: center;
}

.start-focus-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.start-focus-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.notification-center-content::-webkit-scrollbar {
  width: 6px;
}

.notification-center-content::-webkit-scrollbar-track {
  background: transparent;
}

.notification-center-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}
</style>