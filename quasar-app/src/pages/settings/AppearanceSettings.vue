<template>
  <div class="settings-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('settings.appearance') }}</h1>
      <p class="page-description">{{ t('settings.appearanceDesc') }}</p>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.theme') }}</h2>
      <div class="theme-options">
        <button
          v-for="theme in themes"
          :key="theme.id"
          :class="['theme-card', { active: currentTheme === theme.id }]"
          @click="selectTheme(theme.id)"
        >
          <div class="theme-preview">
            <div class="preview-bar"></div>
            <div class="preview-content">
              <div class="preview-item"></div>
              <div class="preview-item"></div>
              <div class="preview-item"></div>
            </div>
          </div>
          <span class="theme-name">{{ t(theme.label) }}</span>
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.accentColor') }}</h2>
      <div class="color-options">
        <button
          v-for="color in accentColors"
          :key="color.value"
          :class="['color-btn', { active: currentAccentColor === color.value }]"
          :style="{ background: color.value }"
          @click="selectAccentColor(color.value)"
        >
          <CheckIcon v-if="currentAccentColor === color.value" :size="14" />
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.fontSize') }}</h2>
      <div class="font-size-options">
        <button
          v-for="size in fontSizes"
          :key="size.value"
          :class="['font-btn', { active: currentFontSize === size.value }]"
          @click="selectFontSize(size.value)"
        >
          {{ t(size.label) }}
        </button>
      </div>
    </div>

    <div class="action-bar">
      <button class="btn btn-secondary" @click="resetToDefault">{{ t('settings.resetToDefault') }}</button>
      <button class="btn btn-primary" @click="saveSettings">{{ t('app.save') }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '../../stores/theme'

const { t } = useI18n()
const themeStore = useThemeStore()

const currentTheme = ref('dark')
const currentAccentColor = ref('#6366f1')
const currentFontSize = ref('medium')

const themes = [
  { id: 'dark', label: 'settings.darkTheme' },
  { id: 'light', label: 'settings.lightTheme' },
  { id: 'system', label: 'settings.systemTheme' },
]

const accentColors = [
  { value: '#6366f1' },
  { value: '#8b5cf6' },
  { value: '#ec4899' },
  { value: '#f59e0b' },
  { value: '#10b981' },
  { value: '#3b82f6' },
  { value: '#ef4444' },
]

const fontSizes = [
  { value: 'small', label: 'settings.small' },
  { value: 'medium', label: 'settings.medium' },
  { value: 'large', label: 'settings.large' },
]

const CheckIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('polyline', { points: '20 6 9 17 4 12' })
    ])
  }
}

const selectTheme = (theme) => {
  currentTheme.value = theme
  // 实时应用主题
  if (theme !== 'system') {
    themeStore.setTheme(theme)
  }
}

const selectAccentColor = (color) => {
  currentAccentColor.value = color
}

const selectFontSize = (size) => {
  currentFontSize.value = size
}

const resetToDefault = () => {
  currentTheme.value = 'dark'
  currentAccentColor.value = '#6366f1'
  currentFontSize.value = 'medium'
  themeStore.setTheme('dark')
}

const saveSettings = () => {
  localStorage.setItem('theme', currentTheme.value)
  localStorage.setItem('accentColor', currentAccentColor.value)
  localStorage.setItem('fontSize', currentFontSize.value)
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'dark'
  const savedColor = localStorage.getItem('accentColor')
  const savedFontSize = localStorage.getItem('fontSize')
  
  currentTheme.value = savedTheme
  if (savedColor) currentAccentColor.value = savedColor
  if (savedFontSize) currentFontSize.value = savedFontSize
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

.theme-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.theme-card {
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

.theme-card:hover {
  background: var(--border-color);
}

.theme-card.active {
  border-color: var(--accent-primary);
  background: rgba(var(--accent-primary-rgb), 0.1);
}

.theme-preview {
  width: 80px;
  height: 60px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.theme-preview .preview-bar {
  height: 10px;
  background: var(--text-secondary);
}

.theme-preview .preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  background: var(--bg-primary);
}

.theme-preview .preview-item {
  height: 8px;
  border-radius: 4px;
  background: var(--text-secondary);
}

.theme-preview .preview-item:nth-child(2) {
  width: 70%;
}

.theme-preview .preview-item:nth-child(3) {
  width: 50%;
}

.theme-name {
  font-size: 13px;
  color: var(--text-primary);
}

.color-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.color-btn {
  width: 40px;
  height: 40px;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.color-btn:hover {
  transform: scale(1.1);
}

.color-btn.active {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.2);
}

.font-size-options {
  display: flex;
  gap: 12px;
}

.font-btn {
  flex: 1;
  padding: 12px;
  background: var(--bg-tertiary);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-primary);
  font-size: 14px;
  transition: all var(--transition-fast);
}

.font-btn:hover {
  background: var(--border-color);
}

.font-btn.active {
  border-color: var(--accent-primary);
  background: rgba(var(--accent-primary-rgb), 0.1);
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
</style>