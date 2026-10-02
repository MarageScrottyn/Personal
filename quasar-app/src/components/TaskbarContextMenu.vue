<template>
  <div 
    class="context-menu" 
    :class="{ visible: isVisible }"
    :style="menuStyle"
    @click.self="$emit('close')"
  >
    <div class="menu-items">
      <button class="menu-item" @click="handleTaskManager">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
        </svg>
        <span>任务管理器</span>
      </button>
      <button class="menu-item" @click="handleTaskbarSettings">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"/>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
        </svg>
        <span>任务栏设置</span>
      </button>
      <hr class="menu-divider"/>
      <button class="menu-item" @click="handleTaskbarSettings2">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 19V5"/>
          <path d="M5 12l7-7 7 7"/>
        </svg>
        <span>任务栏设置</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  isVisible: {
    type: Boolean,
    default: false
  },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 })
  }
})

const emit = defineEmits(['close'])

const menuStyle = computed(() => {
  const menuWidth = 180
  const menuHeight = 140
  
  let left = props.position.x
  let top = props.position.y + 10
  
  if (left + menuWidth > window.innerWidth) {
    left = window.innerWidth - menuWidth - 10
  }
  if (top + menuHeight > window.innerHeight) {
    top = props.position.y - menuHeight - 10
  }
  
  return {
    left: `${Math.max(10, left)}px`,
    top: `${Math.max(10, top)}px`
  }
})

const handleTaskManager = () => {
  console.log('打开任务管理器')
  emit('close')
}

const handleTaskbarSettings = () => {
  console.log('打开任务栏设置')
  emit('close')
  emit('open-settings')
}

const handleTaskbarSettings2 = () => {
  console.log('打开任务栏设置')
  emit('close')
  emit('open-settings')
}
</script>

<style scoped>
.context-menu {
  position: fixed;
  min-width: 180px;
  background: #2d2d2d;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-8px);
  transition: all 0.15s ease;
  z-index: 2000;
  overflow: hidden;
}

.context-menu.visible {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.menu-items {
  padding: 4px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 12px;
  background: transparent;
  border: none;
  color: #e5e7eb;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease;
  text-align: left;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.menu-divider {
  height: 1px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  margin: 4px 0;
}
</style>