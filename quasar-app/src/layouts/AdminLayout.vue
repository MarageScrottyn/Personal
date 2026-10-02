<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <SettingsIcon class="header-icon" />
        <span class="header-title">{{ t('menu.admin') }}</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          to="/"
          :class="['nav-item', { active: route.path === '/' }]"
        >
          <HomeIcon class="nav-icon" />
          <span>{{ t('common.backHome') }}</span>
        </router-link>

        <router-link
          to="/admin"
          :class="['nav-item', { active: route.path === '/admin' }]"
        >
          <DashboardIcon class="nav-icon" />
          <span>{{ t('admin.dashboard') }}</span>
        </router-link>

        <div class="nav-divider"></div>

        <router-link
          to="/admin/comics"
          :class="['nav-item', { active: route.path.startsWith('/admin/comics') }]"
        >
          <BookIcon class="nav-icon" />
          <span>{{ t('admin.comics') }}</span>
        </router-link>

        <router-link
          to="/admin/videos"
          :class="['nav-item', { active: route.path.startsWith('/admin/videos') }]"
        >
          <VideoIcon class="nav-icon" />
          <span>{{ t('admin.videos') }}</span>
        </router-link>

        <router-link
          to="/admin/categories"
          :class="['nav-item', { active: route.path.startsWith('/admin/categories') }]"
        >
          <FolderIcon class="nav-icon" />
          <span>{{ t('admin.categories') }}</span>
        </router-link>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <h1>{{ pageTitle }}</h1>
      </header>
      <div class="admin-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const { t } = useI18n()

const SettingsIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '12', cy: '12', r: '3' }),
      h('path', { d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z' })
    ])
  }
}

const HomeIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z' }),
      h('polyline', { points: '9 22 9 12 15 12 15 22' })
    ])
  }
}

const DashboardIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('rect', { x: '3', y: '3', width: '7', height: '9' }),
      h('rect', { x: '14', y: '3', width: '7', height: '5' }),
      h('rect', { x: '14', y: '12', width: '7', height: '9' }),
      h('rect', { x: '3', y: '16', width: '7', height: '5' })
    ])
  }
}

const BookIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' }),
      h('path', { d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z' })
    ])
  }
}

const VideoIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('rect', { x: '2', y: '4', width: '20', height: '16', rx: '2' }),
      h('path', { d: 'm10 9 5 3-5 3z' })
    ])
  }
}

const FolderIcon = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z' })
    ])
  }
}

const pageTitle = computed(() => {
  const path = route.path
  if (path === '/admin') return t('admin.dashboard')
  if (path.startsWith('/admin/comics')) return t('admin.comics')
  if (path.startsWith('/admin/videos')) return t('admin.videos')
  if (path.startsWith('/admin/categories')) return t('admin.categories')
  return t('menu.admin')
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-primary);
}

.admin-sidebar {
  width: var(--sidebar-width);
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 50;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.header-icon {
  width: 22px;
  height: 22px;
  color: var(--accent-primary);
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--radius-md);
  margin-bottom: 4px;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--accent-primary);
  color: white;
}

.nav-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.nav-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 16px 0;
}

.admin-main {
  flex: 1;
  margin-left: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.admin-header {
  padding: 24px 32px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-secondary);
}

.admin-header h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.admin-content {
  flex: 1;
  padding: 24px 32px;
}
</style>