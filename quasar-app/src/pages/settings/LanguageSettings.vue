<template>
  <div class="settings-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('settings.language') }}</h1>
      <p class="page-description">{{ t('settings.languageDesc') }}</p>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.appLanguage') }}</h2>
      <div class="language-options">
        <button
          v-for="lang in languages"
          :key="lang.code"
          :class="['language-card', { active: currentLanguage === lang.code }]"
          @click="selectLanguage(lang.code)"
        >
          <div class="language-flag">{{ lang.flag }}</div>
          <div class="language-info">
            <span class="language-name">{{ lang.name }}</span>
            <span class="language-native">{{ lang.native }}</span>
          </div>
          <CheckIcon v-if="currentLanguage === lang.code" :size="18" />
        </button>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">{{ t('settings.region') }}</h2>
      <div class="form-group">
        <label>{{ t('settings.timezone') }}</label>
        <select v-model="selectedTimezone" class="settings-select">
          <option v-for="tz in timezones" :key="tz.value" :value="tz.value">
            {{ tz.label }}
          </option>
        </select>
      </div>
      <div class="form-group">
        <label>{{ t('settings.dateFormat') }}</label>
        <select v-model="dateFormat" class="settings-select">
          <option value="YYYY-MM-DD">{{ t('settings.dateFormat1') }}</option>
          <option value="DD/MM/YYYY">{{ t('settings.dateFormat2') }}</option>
          <option value="MM/DD/YYYY">{{ t('settings.dateFormat3') }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>{{ t('settings.timeFormat') }}</label>
        <select v-model="timeFormat" class="settings-select">
          <option value="24h">{{ t('settings.timeFormat24') }}</option>
          <option value="12h">{{ t('settings.timeFormat12') }}</option>
        </select>
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

const { t, locale } = useI18n()

const currentLanguage = ref(locale.value)
const selectedTimezone = ref('Asia/Shanghai')
const dateFormat = ref('YYYY-MM-DD')
const timeFormat = ref('24h')

const languages = [
  { code: 'zh-CN', name: '简体中文', native: '中文', flag: '🇨🇳' },
  { code: 'en-US', name: 'English', native: 'English', flag: '🇺🇸' },
  { code: 'ja-JP', name: '日本語', native: '日本語', flag: '🇯🇵' },
  { code: 'ko-KR', name: '한국어', native: '한국어', flag: '🇰🇷' },
]

const timezones = [
  { value: 'Asia/Shanghai', label: '中国标准时间 (UTC+8)' },
  { value: 'Asia/Tokyo', label: '日本标准时间 (UTC+9)' },
  { value: 'Asia/Seoul', label: '韩国标准时间 (UTC+9)' },
  { value: 'America/New_York', label: '美国东部时间 (UTC-5/UTC-4)' },
  { value: 'Europe/London', label: '英国夏令时间 (UTC+0/UTC+1)' },
  { value: 'Europe/Paris', label: '中欧时间 (UTC+1/UTC+2)' },
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

const selectLanguage = (code) => {
  currentLanguage.value = code
}

const resetToDefault = () => {
  currentLanguage.value = 'zh-CN'
  selectedTimezone.value = 'Asia/Shanghai'
  dateFormat.value = 'YYYY-MM-DD'
  timeFormat.value = '24h'
}

const saveSettings = () => {
  locale.value = currentLanguage.value
  localStorage.setItem('locale', currentLanguage.value)
  localStorage.setItem('timezone', selectedTimezone.value)
  localStorage.setItem('dateFormat', dateFormat.value)
  localStorage.setItem('timeFormat', timeFormat.value)
}

onMounted(() => {
  const savedTimezone = localStorage.getItem('timezone')
  const savedDateFormat = localStorage.getItem('dateFormat')
  const savedTimeFormat = localStorage.getItem('timeFormat')
  
  if (savedTimezone) selectedTimezone.value = savedTimezone
  if (savedDateFormat) dateFormat.value = savedDateFormat
  if (savedTimeFormat) timeFormat.value = savedTimeFormat
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

.language-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.language-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.language-card:hover {
  background: var(--border-color);
}

.language-card.active {
  border-color: var(--accent-primary);
  background: rgba(var(--accent-primary-rgb), 0.1);
}

.language-flag {
  font-size: 24px;
  width: 32px;
  text-align: center;
}

.language-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.language-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.language-native {
  font-size: 13px;
  color: var(--text-secondary);
}

.form-group {
  margin-bottom: 16px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.settings-select {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--bg-primary);
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--transition-fast);
  cursor: pointer;
}

.settings-select:focus {
  border-color: var(--accent-primary);
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