<template>
  <div class="taskbar-view">
    <TaskBar 
      :position="position" 
      :style="style"
      :show-time="showTime"
      :auto-hide="autoHide"
      :blur-radius="blurRadius"
      :glass-strength="glassStrength"
      :icon-color="iconColor"
      :bar-color="barColor"
      :corner-type="cornerType"
      :icon-opacity="iconOpacity"
      :alignment="alignment"
      :rounded-dock-gap="roundedDockGap"
      :rounded-center-gap="roundedCenterGap"
      :rounded-left-gap="roundedLeftGap"
      :rounded-right-gap="roundedRightGap"
      :show-from-edge="showFromEdge"
      @item-click="handleItemClick"
      @launcher-click="handleLauncherClick"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import TaskBar from 'components/TaskBar.vue'

const position = ref('bottom')
const style = ref('user')
const showTime = ref(true)
const autoHide = ref(false)
const blurRadius = ref(20)
const glassStrength = ref(0.8)
const iconColor = ref('#ffffff')
const barColor = ref('rgba(0, 0, 0, 0.8)')
const cornerType = ref('rounded')
const iconOpacity = ref(1)
const alignment = ref('right')
const showFromEdge = ref(false)
const roundedDockGap = ref(6)
const roundedCenterGap = ref(12)
const roundedLeftGap = ref(8)
const roundedRightGap = ref(8)

const handleItemClick = (item) => {
  window.electronAPI?.taskbar?.launchApp(item.id)
}

const handleLauncherClick = () => {
  window.electronAPI?.taskbar?.launchApp('main')
}

const updateSettings = (newSettings) => {
  if (newSettings.position !== undefined) position.value = newSettings.position
  if (newSettings.style !== undefined) style.value = newSettings.style
  if (newSettings.showTime !== undefined) showTime.value = newSettings.showTime
  if (newSettings.autoHide !== undefined) autoHide.value = newSettings.autoHide
  if (newSettings.blurRadius !== undefined) blurRadius.value = newSettings.blurRadius
  if (newSettings.glassStrength !== undefined) glassStrength.value = newSettings.glassStrength
  if (newSettings.iconColor !== undefined) iconColor.value = newSettings.iconColor
  if (newSettings.barColor !== undefined) barColor.value = newSettings.barColor
  if (newSettings.cornerType !== undefined) cornerType.value = newSettings.cornerType
  if (newSettings.iconOpacity !== undefined) iconOpacity.value = newSettings.iconOpacity
  if (newSettings.alignment !== undefined) alignment.value = newSettings.alignment
  if (newSettings.roundedDockGap !== undefined) roundedDockGap.value = newSettings.roundedDockGap
  if (newSettings.roundedCenterGap !== undefined) roundedCenterGap.value = newSettings.roundedCenterGap
  if (newSettings.roundedLeftGap !== undefined) roundedLeftGap.value = newSettings.roundedLeftGap
  if (newSettings.roundedRightGap !== undefined) roundedRightGap.value = newSettings.roundedRightGap
}

onMounted(() => {
  window.electronAPI?.taskbar?.getSettings().then(settings => {
    if (settings) {
      updateSettings(settings)
    }
  })

  window.electronAPI?.taskbar?.onPositionUpdated((pos) => {
    position.value = pos
  })

  window.electronAPI?.taskbar?.onStyleUpdated((styl) => {
    style.value = styl
  })

  window.electronAPI?.taskbar?.onShowTimeUpdated((show) => {
    showTime.value = show
  })

  window.electronAPI?.taskbar?.onAutoHideUpdated((hide) => {
    autoHide.value = hide
  })

  window.electronAPI?.taskbar?.onBlurRadiusUpdated((radius) => {
    blurRadius.value = radius
  })

  window.electronAPI?.taskbar?.onGlassStrengthUpdated((strength) => {
    glassStrength.value = strength
  })

  window.electronAPI?.taskbar?.onIconColorUpdated((color) => {
    iconColor.value = color
  })

  window.electronAPI?.taskbar?.onBarColorUpdated((color) => {
    barColor.value = color
  })

  window.electronAPI?.taskbar?.onCornerTypeUpdated((type) => {
    cornerType.value = type
  })

  window.electronAPI?.taskbar?.onIconOpacityUpdated((opacity) => {
    iconOpacity.value = opacity
  })

  window.electronAPI?.taskbar?.onAlignmentUpdated((align) => {
    alignment.value = align
  })

  window.electronAPI?.taskbar?.onShowFromEdge(() => {
    showFromEdge.value = true
    setTimeout(() => {
      showFromEdge.value = false
    }, 100)
  })

  window.electronAPI?.taskbar?.onRoundedDockGapUpdated((gap) => {
    roundedDockGap.value = gap
  })

  window.electronAPI?.taskbar?.onRoundedCenterGapUpdated((gap) => {
    roundedCenterGap.value = gap
  })

  window.electronAPI?.taskbar?.onRoundedLeftGapUpdated((gap) => {
    roundedLeftGap.value = gap
  })

  window.electronAPI?.taskbar?.onRoundedRightGapUpdated((gap) => {
    roundedRightGap.value = gap
  })
})

onUnmounted(() => {
  window.electronAPI?.taskbar?.removeAllListeners?.()
})
</script>

<style scoped>
.taskbar-view {
  width: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
  position: relative;
  background: transparent;
}
</style>