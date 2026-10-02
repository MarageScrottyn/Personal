<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from './stores/theme'

const router = useRouter()
const themeStore = useThemeStore()

let navigateHandler = null

onMounted(() => {
  // 初始化主题
  themeStore.initTheme()
  
  if (window.electronAPI) {
    navigateHandler = window.electronAPI.on.navigate((path) => {
      router.push(path)
    })
  }
})

onUnmounted(() => {
  if (navigateHandler) {
    navigateHandler()
  }
})
</script>
