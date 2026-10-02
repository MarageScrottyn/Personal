<template>
  <div class="settings-layout">
    <CustomTitleBar />

    <div class="settings-body">
      <aside class="settings-sidebar">
        <div class="sidebar-header">
          <SettingsIcon :size="20" />
          <span>{{ t('app.settings') }}</span>
        </div>
        <nav class="sidebar-nav">
          <button
            v-for="item in menuItems"
            :key="item.path"
            :class="['nav-item', { active: currentPath === item.path }]"
            @click="navigate(item.path)"
          >
            <component :is="item.icon" :size="16" />
            <span>{{ t(item.label) }}</span>
          </button>
        </nav>
      </aside>

      <main class="settings-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import CustomTitleBar from '../components/CustomTitleBar.vue'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const currentPath = computed(() => route.path)

const SettingsIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 20
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('circle', { cx: '12', cy: '12', r: '3' }),
      h('path', { d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z' })
    ])
  }
}

const UserIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: '12', cy: '7', r: '4' })
    ])
  }
}

const LockIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('rect', { x: '3', y: '11', width: '18', height: '11', rx: '2', ry: '2' }),
      h('path', { d: 'M7 11V7a5 5 0 0 1 10 0v4' })
    ])
  }
}

const EyeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z' }),
      h('circle', { cx: '12', cy: '12', r: '3' })
    ])
  }
}

const MonitorIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('rect', { x: '2', y: '3', width: '20', height: '14', rx: '2', ry: '2' }),
      h('line', { x1: '8', y1: '21', x2: '16', y2: '21' }),
      h('line', { x1: '12', y1: '17', x2: '12', y2: '21' })
    ])
  }
}

const BellIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9' }),
      h('path', { d: 'M13.73 21a2 2 0 0 1-3.46 0' })
    ])
  }
}

const GlobeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('line', { x1: '2', y1: '12', x2: '22', y2: '12' }),
      h('path', { d: 'M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z' })
    ])
  }
}

const DiamondIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M2.7 10.3a2.41 2.41 0 0 0 0 3.41l7.59 7.59a2.41 2.41 0 0 0 3.41 0l7.59-7.59a2.41 2.41 0 0 0 0-3.41l-7.59-7.59a2.41 2.41 0 0 0-3.41 0Z' })
    ])
  }
}

const TaskbarIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('rect', { x: '3', y: '10', width: '18', height: '10', rx: '2' }),
      h('line', { x1: '7', y1: '15', x2: '7', y2: '15' }),
      h('line', { x1: '12', y1: '15', x2: '12', y2: '15' }),
      h('line', { x1: '17', y1: '15', x2: '17', y2: '15' })
    ])
  }
}

const ToolIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('path', { d: 'M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z' })
    ])
  }
}

const CodeIcon = {
  props: ['size'],
  render() {
    const iconSize = this.size || 16
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', width: iconSize, height: iconSize }, [
      h('polyline', { points: '16 18 22 12 16 6' }),
      h('polyline', { points: '8 6 2 12 8 18' })
    ])
  }
}

const menuItems = [
  { path: '/settings/account', icon: UserIcon, label: 'profile.accountSettings' },
  { path: '/settings/security', icon: LockIcon, label: 'profile.securitySettings' },
  { path: '/settings/privacy', icon: EyeIcon, label: 'profile.privacySettings' },
  { path: '/settings/general', icon: SettingsIcon, label: 'settings.general' },
  { path: '/settings/appearance', icon: MonitorIcon, label: 'settings.appearance' },
  { path: '/settings/dynamicIsland', icon: DiamondIcon, label: 'settings.dynamicIsland' },
  { path: '/settings/taskbar', icon: TaskbarIcon, label: 'settings.taskbar' },
  { path: '/settings/notifications', icon: BellIcon, label: 'settings.notifications' },
  { path: '/settings/language', icon: GlobeIcon, label: 'settings.language' },
  { path: '/settings/devTools', icon: CodeIcon, label: 'settings.devTools' },
]

const navigate = (path) => {
  router.push(path)
}
</script>

<style scoped>
.settings-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-primary);
  overflow: hidden;
}

.settings-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.settings-sidebar {
  width: 220px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.sidebar-nav {
  flex: 1;
  padding: 12px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  background: transparent;
  border: none;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
  text-align: left;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.nav-item.active {
  color: var(--accent-primary);
  background: rgba(var(--accent-primary-rgb), 0.1);
}

.settings-content {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-primary);
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.settings-content > * {
  width: 100%;
  max-width: 720px;
}
</style>
