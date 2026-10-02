<template>
  <div class="settings-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('settings.taskbar') }}</h1>
      <p class="page-description">{{ t('settings.taskbarDesc') }}</p>
    </div>

    <div class="section feature-toggle">
      <div class="option-item main-toggle">
        <div class="option-info">
          <span class="option-label">{{ t('settings.taskbarEnabled') }}</span>
          <span class="option-description">{{ t('settings.taskbarEnabledDesc') }}</span>
        </div>
        <button 
          class="toggle-btn large" 
          :class="{ active: isTaskbarEnabled }"
          @click="toggleTaskbarEnabled"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.taskbarPosition') }}</h2>
      <div class="position-options">
        <button
          v-for="pos in positions"
          :key="pos.value"
          :class="['position-btn', { active: currentPosition === pos.value }]"
          @click="selectPosition(pos.value)"
        >
          <component :is="pos.icon" :size="20" />
          <span>{{ t(pos.label) }}</span>
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.taskbarStyle') }}</h2>
      <div class="style-options">
        <button
          v-for="style in styles"
          :key="style.value"
          :class="['style-card', { active: currentStyle === style.value, disabled: !isStyleEnabled(style.value) }]"
          :disabled="!isStyleEnabled(style.value)"
          @click="isStyleEnabled(style.value) && selectStyle(style.value)"
        >
          <div class="style-preview" :class="style.value">
            <div class="preview-taskbar"></div>
          </div>
          <span class="style-name">{{ t(style.label) }}</span>
        </button>
      </div>
    </div>

    <!-- Alignment Settings (only for horizontal styles - not linux) -->
    <div class="section" v-if="isAlignmentAvailable()">
      <h2 class="section-title">对齐方式</h2>
      <div class="alignment-options">
        <button
          v-for="align in alignments"
          :key="align.value"
          :class="['alignment-btn', { active: alignment === align.value }]"
          @click="selectAlignment(align.value)"
        >
          <component :is="align.icon" :size="20" />
          <span>{{ align.label }}</span>
        </button>
      </div>
    </div>

    <!-- Auto Hide Settings -->
    <div class="section auto-hide-section">
      <h2 class="section-title">任务栏设置</h2>
      <!-- 自动隐藏开关 -->
      <div class="option-item main-toggle">
        <div class="option-info">
          <span class="option-label">{{ t('settings.autoHide') }}</span>
          <span class="option-description">{{ t('settings.autoHideDesc') }}</span>
        </div>
        <button
          class="toggle-btn large"
          :class="{ active: isAutoHide }"
          @click="toggleAutoHide"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>
      <!-- 自动隐藏延时 -->
       <div class="option-item" v-if="isAutoHide">
        <div class="option-info">
          <span class="option-label">{{ t('settings.autoHideDelay') }}</span>
          <span class="option-description">{{ t('settings.autoHideDelayDesc') }}</span>
        </div>
        <div class="input-with-controls">
          <button 
            class="control-btn" 
            @click="adjustAutoHideDelay(-1)"
            :disabled="autoHideDelay <= 1 || isHideOnLeave"
          >
            -
          </button>
          <input
            type="number"
            v-model.number="autoHideDelay"
            @change="updateAutoHideDelay"
            min="1"
            max="10"
            step="1"
            :disabled="isHideOnLeave"
          />
          <button 
            class="control-btn" 
            @click="adjustAutoHideDelay(1)"
            :disabled="autoHideDelay >= 10 || isHideOnLeave"
          >
            +
          </button>
          <span class="input-unit">秒</span>
        </div>
      </div>
      <!-- 离开任务栏立即隐藏 -->
      <div class="option-item" v-if="isAutoHide">
        <div class="option-info">
          <span class="option-label">{{ t('settings.hideOnLeave') }}</span>
          <span class="option-description">{{ t('settings.hideOnLeaveDesc') }}</span>
        </div>
        <button
          class="toggle-btn large"
          :class="{ active: isHideOnLeave }"
          @click="toggleHideOnLeave"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>
    </div>

    <!-- Corner Type Selection (only for native, fluent, user, rounded styles) -->
    <div class="section" v-if="isCornerTypeAvailable">
      <h2 class="section-title">圆角类型</h2>
      <div class="corner-type-options">
        <button
          v-for="type in cornerTypes"
          :key="type.value"
          :class="['corner-type-btn', { active: cornerType === type.value }]"
          @click="selectCornerType(type.value)"
        >
          <div class="corner-preview" :class="type.value"></div>
          <span class="corner-label">{{ type.label }}</span>
        </button>
      </div>
    </div>

    <!-- Rounded Style Spacing Settings (only for rounded style) -->
    <div class="section" v-if="currentStyle === 'rounded'">
      <h2 class="section-title">间距设置</h2>
      
      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">应用图标间距</span>
          <span class="slider-value">{{ roundedDockGap }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          min="0" 
          max="20" 
          :value="roundedDockGap"
          @input="updateRoundedDockGap($event.target.value)"
        />
        <div class="slider-hints">
          <span>0px</span>
          <span>20px</span>
        </div>
      </div>

      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">中间区域与两边间距</span>
          <span class="slider-value">{{ roundedCenterGap }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          min="0" 
          max="30" 
          :value="roundedCenterGap"
          @input="updateRoundedCenterGap($event.target.value)"
        />
        <div class="slider-hints">
          <span>0px</span>
          <span>30px</span>
        </div>
      </div>

      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">左区域元素间隔</span>
          <span class="slider-value">{{ roundedLeftGap }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          min="0" 
          max="20" 
          :value="roundedLeftGap"
          @input="updateRoundedLeftGap($event.target.value)"
        />
        <div class="slider-hints">
          <span>0px</span>
          <span>20px</span>
        </div>
      </div>

      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">右区域元素间隔</span>
          <span class="slider-value">{{ roundedRightGap }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          min="0" 
          max="20" 
          :value="roundedRightGap"
          @input="updateRoundedRightGap($event.target.value)"
        />
        <div class="slider-hints">
          <span>0px</span>
          <span>20px</span>
        </div>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.taskbarSize') || '尺寸调整' }}</h2>
      
      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">{{ t('settings.taskbarWidth') || '宽度' }}</span>
          <span class="slider-value">{{ taskbarWidth }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          :min="minWidth" 
          :max="maxWidth" 
          :value="taskbarWidth"
          @input="updateWidth($event.target.value)"
        />
        <div class="slider-hints">
          <span>{{ minWidth }}px</span>
          <span>{{ maxWidth }}px</span>
        </div>
      </div>

      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">{{ t('settings.taskbarHeight') || '高度' }}</span>
          <span class="slider-value">{{ taskbarHeight }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          :min="minHeight" 
          :max="maxHeight" 
          :value="taskbarHeight"
          @input="updateHeight($event.target.value)"
        />
        <div class="slider-hints">
          <span>{{ minHeight }}px</span>
          <span>{{ maxHeight }}px</span>
        </div>
      </div>

      <div class="slider-item">
        <div class="slider-info">
          <span class="slider-label">{{ t('settings.taskbarMargin') || '边距' }}</span>
          <span class="slider-value">{{ taskbarMargin }}px</span>
        </div>
        <input 
          type="range" 
          class="slider" 
          min="0" 
          max="100" 
          :value="taskbarMargin"
          @input="updateMargin($event.target.value)"
        />
        <div class="slider-hints">
          <span>0px</span>
          <span>100px</span>
        </div>
      </div>

      <div class="slider-item">
          <div class="slider-info">
            <span class="slider-label">{{ t('settings.taskbarOpacity') || '透明度' }}</span>
            <span class="slider-value">{{ Math.round(taskbarOpacity * 100) }}%</span>
          </div>
          <input 
            type="range" 
            class="slider" 
            min="0.1" 
            max="1" 
            step="0.1"
            :value="taskbarOpacity"
            @input="updateOpacity($event.target.value)"
          />
          <div class="slider-hints">
            <span>10%</span>
            <span>100%</span>
          </div>
        </div>

        <div class="slider-item">
          <div class="slider-info">
            <span class="slider-label">图标透明度</span>
            <span class="slider-value">{{ Math.round(iconOpacity * 100) }}%</span>
          </div>
          <input 
            type="range" 
            class="slider" 
            min="0.1" 
            max="1" 
            step="0.1"
            :value="iconOpacity"
            @input="updateIconOpacity($event.target.value)"
          />
          <div class="slider-hints">
            <span>10%</span>
            <span>100%</span>
          </div>
        </div>

        <div class="slider-item">
          <div class="slider-info">
            <span class="slider-label">毛玻璃模糊</span>
            <span class="slider-value">{{ blurRadius }}px</span>
          </div>
          <input 
            type="range" 
            class="slider" 
            min="0" 
            max="40" 
            :value="blurRadius"
            @input="updateBlurRadius($event.target.value)"
          />
          <div class="slider-hints">
            <span>0px</span>
            <span>40px</span>
          </div>
        </div>

        <div class="slider-item">
          <div class="slider-info">
            <span class="slider-label">磨砂强度</span>
            <span class="slider-value">{{ Math.round(glassStrength * 100) }}%</span>
          </div>
          <input 
            type="range" 
            class="slider" 
            min="0.1" 
            max="1" 
            step="0.1"
            :value="glassStrength"
            @input="updateGlassStrength($event.target.value)"
          />
          <div class="slider-hints">
            <span>10%</span>
            <span>100%</span>
          </div>
        </div>
      </div>

      <!-- Color Settings -->
      <div class="section">
        <h2 class="section-title">配色设置</h2>
        
        <div class="option-item">
          <div class="option-info">
            <span class="option-label">图标颜色</span>
            <span class="option-description">设置任务栏图标的颜色</span>
          </div>
          <div class="preset-colors">
            <button
              v-for="color in presetIconColors"
              :key="color"
              :class="['preset-color', { active: iconColor === color }]"
              :style="{ background: color }"
              @click="setIconColor(color)"
            />
          </div>
        </div>

        <div class="option-item">
          <div class="option-info">
            <span class="option-label">底栏颜色</span>
            <span class="option-description">设置任务栏背景颜色</span>
          </div>
          <div class="preset-colors">
            <button
              v-for="color in presetBarColors"
              :key="color"
              :class="['preset-color', { active: barColor === color }]"
              :style="{ background: color }"
              @click="setBarColor(color)"
            />
          </div>
        </div>
      </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.taskbarOptions') }}</h2>

      <div class="option-item">
        <div class="option-info">
          <span class="option-label">{{ t('settings.showTime') }}</span>
          <span class="option-description">{{ t('settings.showTimeDesc') }}</span>
        </div>
        <button 
          class="toggle-btn" 
          :class="{ active: isShowTime }"
          @click="toggleShowTime"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.devTools') }}</h2>
      
      <div class="option-item">
        <div class="option-info">
          <span class="option-label">{{ t('settings.taskbarDevTools') }}</span>
          <span class="option-description">{{ t('settings.taskbarDevToolsDesc') }}</span>
        </div>
        <button
          class="toggle-btn"
          :class="{ active: devToolsEnabled }"
          @click="toggleTaskbarDevTools"
        >
          <span class="toggle-track">
            <span class="toggle-thumb"></span>
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const savedValue = (key, defaultValue) => {
  const saved = localStorage.getItem(key)
  if (saved === null) return defaultValue
  try {
    return JSON.parse(saved)
  } catch {
    return saved
  }
}

const savedNumber = (key, defaultValue) => {
  const saved = localStorage.getItem(key)
  return saved ? parseFloat(saved) : defaultValue
}

const currentPosition = ref(localStorage.getItem('taskbarPosition') || 'bottom')
const currentStyle = ref(localStorage.getItem('taskbarStyle') || 'user')
const isTaskbarEnabled = ref(savedValue('taskbarEnabled', true))
const isAutoHide = ref(savedValue('taskbarAutoHide', false))
const autoHideDelay = ref(savedNumber('taskbarAutoHideDelay', 5))
const isHideOnLeave = ref(savedValue('taskbarHideOnLeave', true))
const isShowTime = ref(savedValue('taskbarShowTime', true))
const devToolsEnabled = ref(savedValue('taskbarDevToolsEnabled', false))

const taskbarWidth = ref(900)
const taskbarHeight = ref(60)
const taskbarMargin = ref(20)
const taskbarOpacity = ref(0.8)
const iconOpacity = ref(1)  // 图标透明度
const blurRadius = ref(20)
const glassStrength = ref(0.8)

// Color settings
const iconColor = ref('#ffffff')
const barColor = ref('rgba(0, 0, 0, 0.8)')

// 预设颜色
const presetIconColors = [
  '#ffffff', '#000000', '#ff5252', '#ff7043', '#ffca28',
  '#69f0ae', '#40c4ff', '#536dfe', '#e040fb', '#ff4081'
]

const presetBarColors = [
  'rgba(0, 0, 0, 0.8)', 'rgba(255, 255, 255, 0.8)',
  'rgba(33, 33, 33, 0.9)', 'rgba(66, 66, 66, 0.9)',
  'rgba(0, 122, 255, 0.8)', 'rgba(52, 199, 89, 0.8)',
  'rgba(255, 149, 0, 0.8)', 'rgba(255, 59, 48, 0.8)'
]

// Corner type settings
const cornerType = ref('rounded')

const cornerTypes = [
  { value: 'rounded', label: '圆角', icon: 'rounded' },
  { value: 'pill', label: '胶囊', icon: 'pill' },
  { value: 'square', label: '矩形', icon: 'square' },
]

// 判断圆角类型设置是否可用（原生、流畅、自定义、圆润样式可用）
const isCornerTypeAvailable = () => {
  return ['native', 'fluent', 'user', 'rounded'].includes(currentStyle.value)
}

// 对齐方式图标定义
const LeftAlignIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M15 19l-7-7 7-7M5 12h14"/></svg>',
  props: ['size']
}

const CenterAlignIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M18 19l-6-6-6 6M5 12h14"/></svg>',
  props: ['size']
}

const RightAlignIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M9 19l7-7-7-7M19 12H5"/></svg>',
  props: ['size']
}

// 对齐方式设置
const alignment = ref('center')

const alignments = [
  { value: 'left', label: '左对齐', icon: LeftAlignIcon },
  { value: 'center', label: '居中', icon: CenterAlignIcon },
  { value: 'right', label: '右对齐', icon: RightAlignIcon },
]

// 判断对齐设置是否可用（横向样式可用，linux样式不可用）
const isAlignmentAvailable = () => {
  return currentStyle.value !== 'linux' &&
         (currentPosition.value === 'top' || currentPosition.value === 'bottom')
}

// 圆润样式间距设置
const roundedDockGap = ref(6)        // 中间圆形应用图标间距
const roundedCenterGap = ref(12)     // 中间区域与两边区域的间距
const roundedLeftGap = ref(8)        // 左区域中元素间隔
const roundedRightGap = ref(8)       // 右区域中元素间隔

const minWidth = ref(400)
const maxWidth = ref(1400)
const minHeight = ref(30)
const maxHeight = ref(120)

const TopIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M12 19V5M5 12l7-7 7 7"/></svg>',
  props: ['size']
}

const BottomIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M12 5v14M5 12l7 7 7-7"/></svg>',
  props: ['size']
}

const LeftIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M19 12H5M12 5l7 7-7 7"/></svg>',
  props: ['size']
}

const RightIcon = {
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :width="size" :height="size"><path d="M5 12h14M12 5l-7 7 7 7"/></svg>',
  props: ['size']
}

const positions = [
  { value: 'top', label: 'settings.top', icon: TopIcon },
  { value: 'bottom', label: 'settings.bottom', icon: BottomIcon },
  { value: 'left', label: 'settings.left', icon: LeftIcon },
  { value: 'right', label: 'settings.right', icon: RightIcon },
]

const styles = [
  { value: 'macos', label: 'settings.macosStyle' },
  { value: 'user', label: 'settings.userStyle' },
  { value: 'linux', label: 'settings.linuxStyle' },
  { value: 'native', label: 'settings.nativeStyle' },
  { value: 'fluent', label: 'settings.fluentStyle' },
  { value: 'rounded', label: 'settings.roundedStyle' },
]

// 判断样式是否可用
const isStyleEnabled = (style) => {
  const pos = currentPosition.value
  
  // 左右侧位置：仅linux样式可用
  if (pos === 'left' || pos === 'right') {
    return style === 'linux'
  }
  
  // 顶部位置：linux和user样式不可用，macos可用
  if (pos === 'top') {
    return style !== 'linux' && style !== 'user'
  }
  
  // 底部位置：linux和macos样式不可用
  if (pos === 'bottom') {
    return style !== 'linux' && style !== 'macos'
  }
  
  return true
}

// 位置变更时自动选择可用的样式
const selectPosition = (position) => {
  currentPosition.value = position
  localStorage.setItem('taskbarPosition', position)
  window.electronAPI?.taskbar?.setPosition(position)
  
  // 如果当前样式在新位置不可用，选择第一个可用样式
  if (!isStyleEnabled(currentStyle.value)) {
    const availableStyle = styles.find(s => isStyleEnabled(s.value))
    if (availableStyle) {
      currentStyle.value = availableStyle.value
      localStorage.setItem('taskbarStyle', availableStyle.value)
      window.electronAPI?.taskbar?.setStyle(availableStyle.value)
    }
  }
}

const selectStyle = (style) => {
  currentStyle.value = style
  localStorage.setItem('taskbarStyle', style)
  window.electronAPI?.taskbar?.setStyle(style)
}

const selectCornerType = (type) => {
  cornerType.value = type
  localStorage.setItem('cornerType', type)
  window.electronAPI?.taskbar?.setCornerType(type)
}

const selectAlignment = (align) => {
  alignment.value = align
  localStorage.setItem('taskbarAlignment', align)
  window.electronAPI?.taskbar?.setAlignment(align)
}

const toggleTaskbarEnabled = () => {
  isTaskbarEnabled.value = !isTaskbarEnabled.value
  localStorage.setItem('taskbarEnabled', JSON.stringify(isTaskbarEnabled.value))
  window.electronAPI?.taskbar?.setEnabled(isTaskbarEnabled.value)
}

const toggleAutoHide = () => {
  isAutoHide.value = !isAutoHide.value
  localStorage.setItem('taskbarAutoHide', JSON.stringify(isAutoHide.value))
  window.electronAPI?.taskbar?.setAutoHide(isAutoHide.value)
}

const toggleHideOnLeave = () => {
  isHideOnLeave.value = !isHideOnLeave.value
  localStorage.setItem('taskbarHideOnLeave', JSON.stringify(isHideOnLeave.value))
  window.electronAPI?.taskbar?.setHideOnLeave(isHideOnLeave.value)
}

const updateAutoHideDelay = () => {
  localStorage.setItem('taskbarAutoHideDelay', autoHideDelay.value.toString())
  window.electronAPI?.taskbar?.setAutoHideDelay(autoHideDelay.value)
}

const adjustAutoHideDelay = (delta) => {
  autoHideDelay.value = Math.max(1, Math.min(10, autoHideDelay.value + delta))
  updateAutoHideDelay()
}

const toggleShowTime = () => {
  isShowTime.value = !isShowTime.value
  localStorage.setItem('taskbarShowTime', JSON.stringify(isShowTime.value))
  window.electronAPI?.taskbar?.setShowTime(isShowTime.value)
}

const updateWidth = (value) => {
  taskbarWidth.value = parseInt(value)
  localStorage.setItem('taskbarWidth', value)
  window.electronAPI?.taskbar?.updateSize({
    width: taskbarWidth.value,
    height: taskbarHeight.value,
    margin: taskbarMargin.value
  })
}

const updateHeight = (value) => {
  taskbarHeight.value = parseInt(value)
  localStorage.setItem('taskbarHeight', value)
  window.electronAPI?.taskbar?.updateSize({
    width: taskbarWidth.value,
    height: taskbarHeight.value,
    margin: taskbarMargin.value
  })
}

const updateMargin = (value) => {
  taskbarMargin.value = parseInt(value)
  localStorage.setItem('taskbarMargin', value)
  window.electronAPI?.taskbar?.updateSize({
    width: taskbarWidth.value,
    height: taskbarHeight.value,
    margin: taskbarMargin.value
  })
}

const updateOpacity = (value) => {
  taskbarOpacity.value = parseFloat(value)
  localStorage.setItem('taskbarOpacity', taskbarOpacity.value)
  window.electronAPI?.taskbar?.setOpacity(taskbarOpacity.value)
}

const updateIconOpacity = (value) => {
  iconOpacity.value = parseFloat(value)
  localStorage.setItem('iconOpacity', iconOpacity.value)
  window.electronAPI?.taskbar?.setIconOpacity(iconOpacity.value)
}

const updateBlurRadius = (value) => {
  blurRadius.value = parseInt(value)
  localStorage.setItem('blurRadius', value)
  window.electronAPI?.taskbar?.setBlurRadius(blurRadius.value)
}

const updateGlassStrength = (value) => {
  glassStrength.value = parseFloat(value)
  localStorage.setItem('glassStrength', value)
  window.electronAPI?.taskbar?.setGlassStrength(glassStrength.value)
}

const updateIconColor = () => {
  localStorage.setItem('iconColor', iconColor.value)
  window.electronAPI?.taskbar?.setIconColor(iconColor.value)
}

const setIconColor = (color) => {
  iconColor.value = color
  updateIconColor()
}

const updateBarColor = () => {
  localStorage.setItem('barColor', barColor.value)
  window.electronAPI?.taskbar?.setBarColor(barColor.value)
}

const setBarColor = (color) => {
  barColor.value = color
  updateBarColor()
}

// 更新圆润样式间距设置
const updateRoundedDockGap = (value) => {
  roundedDockGap.value = parseInt(value)
  localStorage.setItem('roundedDockGap', value)
  window.electronAPI?.taskbar?.setRoundedDockGap(roundedDockGap.value)
}

const updateRoundedCenterGap = (value) => {
  roundedCenterGap.value = parseInt(value)
  localStorage.setItem('roundedCenterGap', value)
  window.electronAPI?.taskbar?.setRoundedCenterGap(roundedCenterGap.value)
}

const updateRoundedLeftGap = (value) => {
  roundedLeftGap.value = parseInt(value)
  localStorage.setItem('roundedLeftGap', value)
  window.electronAPI?.taskbar?.setRoundedLeftGap(roundedLeftGap.value)
}

const updateRoundedRightGap = (value) => {
  roundedRightGap.value = parseInt(value)
  localStorage.setItem('roundedRightGap', value)
  window.electronAPI?.taskbar?.setRoundedRightGap(roundedRightGap.value)
}

const toggleTaskbarDevTools = async () => {
  devToolsEnabled.value = !devToolsEnabled.value
  if (window.electronAPI?.taskbar) {
    await window.electronAPI.taskbar.setDevToolsEnabled(devToolsEnabled.value)
  }
}

onMounted(async () => {
  const savedPosition = localStorage.getItem('taskbarPosition')
  const savedStyle = localStorage.getItem('taskbarStyle')
  const savedEnabled = localStorage.getItem('taskbarEnabled')
  const savedAutoHide = localStorage.getItem('taskbarAutoHide')
  const savedAutoHideDelay = localStorage.getItem('taskbarAutoHideDelay')
  const savedHideOnLeave = localStorage.getItem('taskbarHideOnLeave')
  const savedShowTime = localStorage.getItem('taskbarShowTime')
  const savedWidth = localStorage.getItem('taskbarWidth')
  const savedHeight = localStorage.getItem('taskbarHeight')
  const savedMargin = localStorage.getItem('taskbarMargin')
  const savedOpacity = localStorage.getItem('taskbarOpacity')
  const savedAlignment = localStorage.getItem('taskbarAlignment')
  
  if (savedPosition) currentPosition.value = savedPosition
  if (savedStyle) currentStyle.value = savedStyle
  if (savedEnabled) isTaskbarEnabled.value = JSON.parse(savedEnabled)
  if (savedAutoHide) isAutoHide.value = JSON.parse(savedAutoHide)
  if (savedAutoHideDelay) autoHideDelay.value = parseInt(savedAutoHideDelay)
  if (savedHideOnLeave) isHideOnLeave.value = JSON.parse(savedHideOnLeave)
  if (savedShowTime) isShowTime.value = JSON.parse(savedShowTime)
  if (savedWidth) taskbarWidth.value = parseInt(savedWidth)
  if (savedHeight) taskbarHeight.value = parseInt(savedHeight)
  if (savedMargin) taskbarMargin.value = parseInt(savedMargin)
  if (savedOpacity) taskbarOpacity.value = parseFloat(savedOpacity)
  if (savedAlignment) alignment.value = savedAlignment
  
  if (window.electronAPI?.taskbar) {
    const settings = await window.electronAPI.taskbar.getSettings()
    if (settings) {
      devToolsEnabled.value = settings.devToolsEnabled !== undefined ? settings.devToolsEnabled : false
    }
  }
})
</script>

<style scoped>
.settings-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.section {
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.position-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.position-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: var(--bg-tertiary);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.position-btn:hover {
  background: var(--border-color);
}

.position-btn.active {
  border-color: var(--accent-primary);
  background: rgba(var(--accent-primary-rgb), 0.1);
}

.style-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.style-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: var(--bg-tertiary);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.style-card:hover {
  background: var(--border-color);
}

.style-card.active {
  border-color: var(--accent-primary);
  background: rgba(var(--accent-primary-rgb), 0.1);
}

.style-card.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

.style-preview {
  width: 80px;
  height: 40px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  position: relative;
}

.style-preview .preview-taskbar {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

/* macOS样式 - 顶部细横线 */
.style-preview.macos .preview-taskbar {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.8);
}

/* 用户自定义样式 - 圆角矩形 */
.style-preview.user .preview-taskbar {
  width: 100%;
  height: 24px;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0.1) 100%);
}

/* Linux样式 - 左侧竖线 */
.style-preview.linux .preview-taskbar {
  width: 6px;
  height: 100%;
  border-radius: 0;
  background: rgba(255, 255, 255, 0.5);
}

/* 原生样式 - Windows风格 */
.style-preview.native .preview-taskbar {
  width: 100%;
  height: 28px;
  border-radius: 4px;
  background: rgba(45, 45, 45, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 流畅样式 - 毛玻璃效果 */
.style-preview.fluent .preview-taskbar {
  width: 100%;
  height: 32px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* 圆润样式 - 左右分开 */
.style-preview.rounded {
  display: flex;
  justify-content: space-between;
  padding: 4px;
}
.style-preview.rounded .preview-taskbar {
  width: 45%;
  height: 100%;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.6);
}

.style-name {
  font-size: 13px;
  color: var(--text-primary);
}

.corner-type-options {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.corner-type-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}

.corner-type-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.corner-type-btn.active {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  box-shadow: 0 4px 20px rgba(var(--accent-primary-rgb), 0.4);
}

/* 圆角预览图标 */
.corner-preview {
  width: 40px;
  height: 24px;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.3);
}

.corner-preview.rounded {
  border-radius: 6px;
}

.corner-preview.pill {
  border-radius: 12px;
}

.corner-preview.square {
  border-radius: 0;
}

.corner-type-btn.active .corner-preview {
  background: rgba(255, 255, 255, 0.9);
}

.corner-label {
  font-size: 13px;
  color: var(--text-primary);
}

.corner-type-btn.active .corner-label {
  color: white;
}

/* 对齐方式选项 */
.alignment-options {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.alignment-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-primary);
  transition: all 0.2s ease;
}

.alignment-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.alignment-btn.active {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
  box-shadow: 0 4px 20px rgba(var(--accent-primary-rgb), 0.4);
}

.alignment-btn.active svg {
  color: white;
}

.alignment-btn.active span {
  color: white;
}

.alignment-btn svg {
  transition: color 0.2s ease;
}

.alignment-btn span {
  font-size: 13px;
  color: var(--text-primary);
  transition: color 0.2s ease;
}

.option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.option-item:last-child {
  border-bottom: none;
}

.option-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.option-description {
  font-size: 12px;
  color: var(--text-secondary);
}

.toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.toggle-track {
  display: inline-block;
  width: 44px;
  height: 24px;
  background: var(--bg-tertiary);
  border-radius: 12px;
  position: relative;
  transition: background var(--transition-fast);
}

.toggle-btn.active .toggle-track {
  background: var(--accent-primary);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform var(--transition-fast);
}

.toggle-btn.active .toggle-thumb {
  transform: translateX(20px);
}

/* 大型开关样式 */
.toggle-btn.large .toggle-track {
  width: 56px;
  height: 32px;
  border-radius: 16px;
}

.toggle-btn.large .toggle-thumb {
  top: 3px;
  left: 3px;
  width: 26px;
  height: 26px;
}

.toggle-btn.large.active .toggle-thumb {
  transform: translateX(24px);
}

/* 功能开关区域样式 */
.feature-toggle {
  margin-bottom: 24px;
}

.feature-toggle .main-toggle {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}

.feature-toggle .option-label {
  font-size: 16px;
  font-weight: 600;
}

.feature-toggle .option-description {
  font-size: 13px;
}

/* 自动隐藏设置区域样式 */
.auto-hide-section {
  margin-top: 16px;
}

.auto-hide-section .main-toggle {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
}

.auto-hide-section .option-label {
  font-size: 16px;
  font-weight: 600;
}

.auto-hide-section .option-description {
  font-size: 13px;
}

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 16px;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-primary {
  background: var(--accent-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--accent-primary-dark);
}

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.btn-secondary:hover {
  background: var(--border-color);
}

.slider-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.slider-item:last-child {
  border-bottom: none;
}

.slider-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 颜色选择器样式 */
.color-picker-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-picker {
  width: 40px;
  height: 40px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-sm);
  cursor: pointer;
  background: none;
  padding: 0;
  transition: border-color var(--transition-fast);
}

.color-picker:hover {
  border-color: var(--accent-primary);
}

.color-value {
  font-size: 13px;
  color: var(--text-secondary);
  font-family: monospace;
  min-width: 120px;
}

/* 预设颜色 */
.preset-colors {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.preset-color {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.preset-color:hover {
  transform: scale(1.1);
}

.preset-color.active {
  border-color: var(--accent-primary);
}

.slider-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.slider-value {
  font-size: 13px;
  color: var(--accent-primary);
  font-weight: 600;
  background: rgba(var(--accent-primary-rgb), 0.1);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
}

.slider {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--bg-tertiary);
  border-radius: 3px;
  outline: none;
  cursor: pointer;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  background: var(--accent-primary);
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
}

.slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  background: var(--accent-primary);
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.slider-hints {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11px;
  color: var(--text-secondary);
}

/* 带控制按钮的输入框组样式 */
.input-with-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.control-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.control-btn:hover:not(:disabled) {
  background: var(--border-color);
}

.control-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-with-controls input[type="number"] {
  width: 50px;
  padding: 6px 8px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 14px;
  text-align: center;
  transition: all var(--transition-fast);
}

.input-with-controls input[type="number"]:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px rgba(var(--accent-primary-rgb), 0.2);
}

/* 隐藏默认的上下调节按钮 */
.input-with-controls input[type="number"]::-webkit-inner-spin-button,
.input-with-controls input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.input-with-controls input[type="number"] {
  -moz-appearance: textfield;
}

.input-unit {
  font-size: 14px;
  color: var(--text-secondary);
  margin-left: 4px;
}
</style>
