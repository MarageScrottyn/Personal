<template>
  <div class="system-tray">
    <div class="tray-items">
      <button 
        v-for="item in trayItems" 
        :key="item.id"
        class="tray-item"
        :class="{ active: item.active, 'has-notification': item.notification }"
        :title="item.label"
        @click="handleTrayClick(item)"
      >
        <svg :width="item.size || 16" :height="item.size || 16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path :d="item.path"/>
        </svg>
      </button>
    </div>
    
    <div class="tray-divider"></div>
    
    <div class="tray-clock">
      <span class="clock-time">{{ currentTime }}</span>
      <span class="clock-date">{{ currentDate }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('')
const currentDate = ref('')
let timer = null

const trayItems = ref([
  { id: 1, label: '网络', path: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z', active: true },
  { id: 2, label: '音量', path: 'M9 18V5l12-2v13', active: true },
  { id: 3, label: '蓝牙', path: 'M17.7 7.7a2.5 2.5 0 1 1 1.8 4.3H2a2.5 2.5 0 1 1 1.8-4.3h13.9zM15 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm4.7-1.8a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z', active: false },
  { id: 4, label: '电池', path: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z', active: true },
])

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const handleTrayClick = (item) => {
  console.log('Tray item clicked:', item.label)
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.system-tray {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
}

.tray-items {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tray-item {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tray-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

.tray-item.active {
  color: #e5e7eb;
}

.tray-item.has-notification::after {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  background: #ef4444;
  border-radius: 50%;
  top: 4px;
  right: 4px;
}

.tray-divider {
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 4px;
}

.tray-clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 2px 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.tray-clock:hover {
  background: rgba(255, 255, 255, 0.08);
}

.clock-time {
  font-size: 12px;
  font-weight: 500;
  color: #e5e7eb;
  line-height: 1.2;
}

.clock-date {
  font-size: 10px;
  color: #9ca3af;
  line-height: 1.2;
}
</style>