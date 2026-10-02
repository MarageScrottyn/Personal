<template>
  <header class="titlebar" :class="{ 'is-maximized': isMaximized }">
    <div class="titlebar-left">
      <div class="app-brand">
        <svg class="app-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2L2 7l10 5 10-5-10-5z"/>
          <path d="M2 17l10 5 10-5"/>
          <path d="M2 12l10 5 10-5"/>
        </svg>
        <span class="app-name">{{ t('app.name') }}</span>
      </div>
    </div>

    <nav class="titlebar-nav" v-if="hasRouter">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        :class="['nav-item', { active: isActive(item.path) }]"
      >
        <component :is="item.icon" class="nav-icon" :size="14" />
        <span>{{ t(item.label) }}</span>
      </router-link>
    </nav>

    <div class="titlebar-right">
      <div class="user-section">
        <button
          v-if="authStore.isAdmin"
          @click="handleSync"
          class="icon-btn"
          :title="t('app.sync')"
        >
          <RefreshIcon :size="16" />
        </button>

        <button
          class="icon-btn language-btn"
          @click.stop="toggleLanguageMenu"
          :title="t('app.language')"
        >
          <GlobeIcon :size="16" />
          <div v-if="languageMenuOpen" class="dropdown-menu language-dropdown">
            <button
              v-for="lang in languages"
              :key="lang.code"
              :class="['dropdown-item', { active: currentLocale === lang.code }]"
              @click.stop="changeLanguage(lang.code)"
            >
              {{ lang.name }}
            </button>
          </div>
        </button>

        <button
          v-if="hasRouter"
          class="icon-btn settings-btn"
          @click="goToSettings"
          :title="t('app.settings')"
        >
          <SettingsIcon :size="16" />
        </button>

        <div class="user-menu" @click.stop="toggleUserMenu" v-if="hasRouter">
          <div class="user-avatar">
            <UserIcon :size="14" />
          </div>
          <span class="user-name">{{ authStore.user?.username || t('app.user') }}</span>
          <ChevronDownIcon :size="12" />

          <div v-if="userMenuOpen" class="dropdown-menu user-dropdown">
            <button class="dropdown-item" @click.stop="handleProfile">
              <UserIcon :size="14" />
              <span>{{ t('app.profile') }}</span>
            </button>
            <button class="dropdown-item" @click.stop="handleSettings">
              <SettingsIcon :size="14" />
              <span>{{ t('app.settings') }}</span>
            </button>
            <button class="dropdown-item danger" @click.stop="handleLogout">
              <LogOutIcon :size="14" />
              <span>{{ t('app.logout') }}</span>
            </button>
          </div>
        </div>
      </div>

      <div class="window-controls">
        <button class="control-btn" @click.stop="handleMinimize" :title="t('app.minimize')">
          <MinimizeIcon :size="14" />
        </button>
        <button class="control-btn" @click.stop="handleMaximize" :title="isMaximized ? t('app.restore') : t('app.maximize')">
          <component :is="isMaximized ? Minimize2Icon : MaximizeIcon" :size="14" />
        </button>
        <button class="control-btn close" @click.stop="handleClose" :title="t('app.close')">
          <XIcon :size="14" />
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()

const { t, locale } = useI18n()
const authStore = useAuthStore()

const isMaximized = ref(false)
const userMenuOpen = ref(false)
const languageMenuOpen = ref(false)

const currentLocale = computed(() => locale.value)
const hasRouter = computed(() => !!router)

const languages = [
  { code: 'zh-CN', name: '简体中文' },
  { code: 'en-US', name: 'English' },
]

const BookIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' }),
      h('path', { d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z' })
    ])
  }
}

const VideoIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('rect', { x: '2', y: '4', width: '20', height: '16', rx: '2' }),
      h('path', { d: 'm10 9 5 3-5 3z' })
    ])
  }
}

const FileTextIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
      h('line', { x1: '10', y1: '9', x2: '8', y2: '9' })
    ])
  }
}

const CloudIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z' })
    ])
  }
}

const LingTaiIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        width: iconSize,
        height: iconSize
      },
      [
        h('circle', { cx: '12', cy: '12', r: '5' }),
        h('path', { d: 'M12 2v4' }),
        h('path', { d: 'M12 18v4' }),
        h('path', { d: 'M4.93 4.93l2.83 2.83' }),
        h('path', { d: 'M16.24 16.24l2.83 2.83' }),
        h('path', { d: 'M2 12h4' }),
        h('path', { d: 'M18 12h4' }),
        h('path', { d: 'M4.93 19.07l2.83-2.83' }),
        h('path', { d: 'M16.24 7.76l2.83-2.83' })
      ]
    )
  }
}

const BrainIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h(
      'svg',
      {
        viewBox: '0 0 24 24',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        width: iconSize,
        height: iconSize
      },
      [
        h('path', { d: 'M12 4.5a2.5 2.5 0 0 0-4.96-.46 2.5 2.5 0 0 0-1.98 3 2.5 2.5 0 0 0 .46 4.96 2.5 2.5 0 0 0 3 1.98 2.5 2.5 0 0 0 4.96.46 2.5 2.5 0 0 0 1.98-3 2.5 2.5 0 0 0-.46-4.96 2.5 2.5 0 0 0-3-1.98z' }),
        h('path', { d: 'M12 14.5a2.5 2.5 0 0 0-4.96-.46 2.5 2.5 0 0 0-.46 4.96 2.5 2.5 0 0 0 3 1.98 2.5 2.5 0 0 0 4.96.46 2.5 2.5 0 0 0 1.98-3 2.5 2.5 0 0 0-.46-4.96 2.5 2.5 0 0 0-3-1.98z' }),
        h('circle', { cx: '12', cy: '12', r: '1' })
      ]
    )
  }
}

const SettingsIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('circle', { cx: '12', cy: '12', r: '3' }),
      h('path', { d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z' })
    ])
  }
}

const RefreshIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('polyline', { points: '23 4 23 10 17 10' }),
      h('polyline', { points: '1 20 1 14 7 14' }),
      h('path', { d: 'M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15' })
    ])
  }
}

const GlobeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('line', { x1: '2', y1: '12', x2: '22', y2: '12' }),
      h('path', { d: 'M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z' })
    ])
  }
}

const UserIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: '12', cy: '7', r: '4' })
    ])
  }
}

const LogOutIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('path', { d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' }),
      h('polyline', { points: '16 17 21 12 16 7' }),
      h('line', { x1: '21', y1: '12', x2: '9', y2: '12' })
    ])
  }
}

const ChevronDownIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { 
      viewBox: '0 0 24 24', 
      fill: 'none', 
      stroke: 'currentColor', 
      'stroke-width': '2',
      width: iconSize,
      height: iconSize
    }, [
      h('polyline', { points: '6 9 12 15 18 9' })
    ])
  }
}

const MinimizeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 14
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('line', { x1: '5', y1: '12', x2: '19', y2: '12' })
    ])
  }
}

const MaximizeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 14
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('rect', { x: '3', y: '3', width: '18', height: '18', rx: '2' })
    ])
  }
}

const Minimize2Icon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 14
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('rect', { x: '5', y: '11', width: '14', height: '10', rx: '2' }),
      h('rect', { x: '3', y: '5', width: '14', height: '10', rx: '2' })
    ])
  }
}

const XIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 14
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('line', { x1: '18', y1: '6', x2: '6', y2: '18' }),
      h('line', { x1: '6', y1: '6', x2: '18', y2: '18' })
    ])
  }
}

const navItems = computed(() => {
  const items = [
    { path: '/comics', icon: BookIcon, label: 'menu.comics' },
    { path: '/videos', icon: VideoIcon, label: 'menu.videos' },
    { path: '/notes', icon: FileTextIcon, label: 'menu.notes' },
    { path: '/cloud', icon: CloudIcon, label: 'menu.cloudDrive' },
    { path: '/heart', icon: LingTaiIcon, label: 'menu.heart'},
    { path: '/brain', icon: BrainIcon, label: 'menu.brain'}
  ]
  if (authStore.isAdmin) {
    items.push({ path: '/admin', icon: SettingsIcon, label: 'menu.admin' })
  }
  return items
})

const isActive = (path) => {
  if (!route) return false
  if (path === '/comics') return route.path.startsWith('/comics')
  if (path === '/videos') return route.path.startsWith('/videos')
  if (path === '/notes') return route.path.startsWith('/notes')
  if (path === '/cloud') return route.path.startsWith('/cloud')
  if (path === '/brain') return route.path.startsWith('/brain')
  if (path === '/admin') return route.path.startsWith('/admin')
  return route.path === path
}

const handleMinimize = async () => {
  console.log('[TitleBar] handleMinimize called, electronAPI:', window.electronAPI)
  if (window.electronAPI) {
    try {
      await window.electronAPI.window.minimize()
      console.log('[TitleBar] minimize success')
    } catch (error) {
      console.error('[TitleBar] minimize error:', error)
    }
  } else {
    console.error('[TitleBar] electronAPI is undefined')
  }
}

const handleMaximize = async () => {
  if (window.electronAPI) {
    isMaximized.value = await window.electronAPI.window.maximize()
  }
}

const handleClose = async () => {
  if (window.electronAPI) {
    await window.electronAPI.window.close()
  }
}

const toggleUserMenu = () => {
  userMenuOpen.value = !userMenuOpen.value
  languageMenuOpen.value = false
}

const toggleLanguageMenu = () => {
  languageMenuOpen.value = !languageMenuOpen.value
  userMenuOpen.value = false
}

const changeLanguage = (code) => {
  locale.value = code
  localStorage.setItem('locale', code)
  languageMenuOpen.value = false
}

const handleSync = () => {
  authStore.syncMedia()
}

const handleLogout = () => {
  userMenuOpen.value = false
  authStore.logout()
  if (router) {
    router.push('/login')
  }
}

const handleProfile = () => {
  userMenuOpen.value = false
  if (router) {
    router.push('/profile')
  }
}

const goToSettings = () => {
  if (router) {
    router.push('/settings')
  }
}

const handleSettings = () => {
  userMenuOpen.value = false
  if (router) {
    router.push('/settings')
  }
}

const handleWindowMaximized = () => {
  isMaximized.value = true
}

const handleWindowUnmaximized = () => {
  isMaximized.value = false
}

const closeDropdowns = () => {
  userMenuOpen.value = false
  languageMenuOpen.value = false
}

onMounted(async () => {
  if (window.electronAPI) {
    isMaximized.value = await window.electronAPI.window.isMaximized()
    window.electronAPI.on.windowMaximized(handleWindowMaximized)
    window.electronAPI.on.windowUnmaximized(handleWindowUnmaximized)
  }
  document.addEventListener('click', closeDropdowns)
})

onUnmounted(() => {
  if (window.electronAPI) {
    window.electronAPI.on.windowMaximized(() => {})
    window.electronAPI.on.windowUnmaximized(() => {})
  }
  document.removeEventListener('click', closeDropdowns)
})
</script>

<style scoped>
.titlebar {
  display: flex;
  align-items: center;
  height: var(--titlebar-height);
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-subtle);
  padding: 0 8px;
  -webkit-app-region: drag;
  user-select: none;
  position: relative;
  z-index: 100;
}

.titlebar-nav {
  -webkit-app-region: drag;
}

.titlebar-left {
  display: flex;
  align-items: center;
  width: calc(var(--sidebar-width) + 20px);
  padding-left: 12px;
  -webkit-app-region: drag;
}

.app-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-logo {
  width: 20px;
  height: 20px;
  color: var(--accent-primary);
}

.app-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.3px;
}

.titlebar-nav {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  -webkit-app-region: no-drag;
}

.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.nav-item.active {
  color: var(--text-primary);
  background: var(--accent-primary);
}

.nav-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.nav-item svg {
  width: 14px !important;
  height: 14px !important;
}

.titlebar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  -webkit-app-region: drag;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: 12px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  cursor: pointer;
  outline: none;
  box-shadow: none;
  -webkit-appearance: none;
  -webkit-app-region: no-drag;
}

.icon-btn:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.language-btn {
  position: relative;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  position: relative;
  transition: all var(--transition-fast);
  outline: none;
  box-shadow: none;
  -webkit-appearance: none;
  -webkit-app-region: no-drag;
}

.user-menu:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.user-name {
  font-size: 12px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--accent-primary);
  border-radius: 50%;
  color: white;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  min-width: 140px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 4px;
  box-shadow: var(--shadow-lg);
  z-index: 1000;
}

.user-dropdown {
  min-width: 160px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 12px;
  color: var(--text-primary);
  font-size: 13px;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
  text-align: left;
}

.dropdown-item:hover {
  background: var(--bg-tertiary);
}

.dropdown-item.active {
  color: var(--accent-primary);
}

.dropdown-item.danger:hover {
  background: rgba(239, 68, 68, 0.15);
  color: var(--accent-danger);
}

.window-controls {
  display: flex;
  align-items: center;
  margin-left: 4px;
  -webkit-app-region: no-drag;
}

.control-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 32px;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  cursor: pointer;
  outline: none;
  box-shadow: none;
  -webkit-appearance: none;
}

.control-btn:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.control-btn.close:hover {
  background: var(--accent-danger);
  color: white;
}

.control-btn svg {
  width: 14px;
  height: 14px;
}
</style>