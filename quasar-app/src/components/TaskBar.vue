<template>
  <div
    class="taskbar"
    :class="[`position-${position}`, `style-${style}`, { 'auto-hide': autoHide, 'is-hidden': autoHide && isHidden }]"
    style="pointer-events: none;"
  >
    <div
      class="taskbar-content"
      :class="[`corner-${cornerType}`, `corner-${cornerType}-${position}`, `align-${alignment}`, { 'hidden': autoHide && isHidden }]"
      :style="{ ...getTaskbarStyle(), pointerEvents: 'auto' }"
      @mouseenter="handleMouseEnterTaskbar"
      @mouseleave="handleMouseLeaveTaskbar"
      @contextmenu="handleContextMenu"
      @click="handleTaskbarClick"
    >
      <!-- macOS style - top position -->
      <div v-if="position === 'top' && style === 'macos'" class="taskbar-macos">
        <div class="macos-left">
          <div class="macos-dots">
            <span class="dot dot-red"></span>
            <span class="dot dot-yellow"></span>
            <span class="dot dot-green"></span>
          </div>
          <div class="macos-apple">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.28-.79-1.51 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C3.25 17 2.94 12.45 5.05 9.39c.77-1.2 1.98-2.08 3.51-2.11 1.44-.03 2.8.86 3.51.86.71 0 2.26-1.04 3.72-.91 1.61.15 2.89 1.28 3.66 2.53.64 1.11.86 2.37.66 3.53-.26 1.45-.91 2.8-1.91 3.89-.89.9-2.01 1.61-3.18 1.58-1.04-.02-1.99-.72-2.79-.72-.93 0-1.87.83-2.91.83-1.13 0-2.22-.92-2.93-.92-.63 0-1.29.42-1.84 1.03-.13.13-.24.28-.36.43-.07.08-.14.17-.19.26-.06.1-.08.21-.06.32-.01.13.03.26.11.37.12.19.3.37.51.5.02.01.03.03.05.04.01.01.03.02.04.03.01.01.01.03.02.04.01.01.01.02.02.03.58.43 1.28.67 2.05.62 1.21-.08 2.27-.71 3.02-1.61.82-1.04.83-2.43.02-3.51-.83-1.11-2.18-1.75-3.65-1.66-1.43.09-2.89.82-3.7 2.01-.74 1.11-.85 2.57-.31 3.86.24.55.59 1.06 1.09 1.43.94.68 2.08.89 3.2.83 1.09-.06 2.16-.62 3.02-1.36.81-.7 1.46-1.6 1.91-2.61.13-.32.24-.66.34-1 .1-.34.2-.69.31-1.04.11-.35.24-.71.4-.99.16-.28.38-.5.63-.66.25-.16.53-.26.83-.29 1.03-.11 2.1.37 2.92 1.21.87.9 1.39 2.11 1.29 3.41-.1 1.33-.83 2.52-1.91 3.34"/>
            </svg>
          </div>
        </div>
        
        <div class="macos-center">
          <div class="dock-items">
            <!-- 系统固定应用 -->
            <button
              v-for="app in pinnedApps"
              :key="'macos-pinned-' + app.id"
              class="dock-item"
              :class="{ active: isAppActive(app) }"
              @click="launchApp(app.path)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <div class="dock-icon">
                <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="macos-icon-img" />
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <!-- 活动窗口数量指示器 -->
              <div v-if="getAppWindowCount(app) > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
            
            <!-- 活动应用（不在固定列表中的运行应用） -->
            <button
              v-for="app in activeApps"
              :key="'macos-active-' + app.id"
              class="dock-item active-app"
              :class="{ active: true }"
              @click="activateWindow(app.windows[0]?.handle)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <div class="dock-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <!-- 活动窗口数量指示器 -->
              <div v-if="app.windows.length > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
            </div>
        </div>
        
        <div class="macos-right">
          <div class="status-icons">
            <!-- 系统托盘 -->
            <span class="status-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
                <path d="M8 12l3 3 5-5"/>
              </svg>
            </span>
            <!-- 输入法 -->
            <span class="status-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </span>
            <span v-if="showTime" class="status-text">{{ currentTime }}</span>
          </div>
        </div>
      </div>

      <!-- User style - bottom position -->
      <div v-else-if="position === 'bottom' && style === 'user'" class="taskbar-user">
        <div class="user-dock">
          <!-- 系统固定应用 -->
          <button
            v-for="app in pinnedApps"
            :key="'user-pinned-' + app.id"
            class="user-dock-item"
            :class="{ active: isAppActive(app) }"
            @click="launchApp(app.path)"
            @mouseenter="handleAppHover($event, app)"
            @mouseleave="handleAppLeave"
            :title="app.name"
          >
            <div class="app-icon">
              <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="app-icon-img" />
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
              </svg>
            </div>
            <span v-if="app.name" class="app-label">{{ app.name }}</span>
            <!-- 活动窗口数量指示器 -->
            <div v-if="getAppWindowCount(app) > 0" class="window-indicator">
              <span class="indicator-dot"></span>
            </div>
          </button>
          
          <!-- 活动应用（不在固定列表中的运行应用） -->
          <button
            v-for="app in activeApps"
            :key="'user-active-' + app.id"
            class="user-dock-item active-app"
            :class="{ active: hoveredApp?.id === app.id }"
            @click="activateWindow(app.windows[0]?.handle)"
            @mouseenter="handleAppHover($event, app)"
            @mouseleave="handleAppLeave"
            :title="app.name"
          >
            <div class="app-icon">
              <img v-if="app.icon" :src="getAppIconUrl(app.icon)" :alt="app.name" />
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
              </svg>
            </div>
            <span class="app-label">{{ app.name }}</span>
            <!-- 活动窗口数量指示器 -->
            <div v-if="app.windows.length > 0" class="window-indicator">
              <span class="indicator-dot"></span>
            </div>
          </button>
        </div>
      </div>

      <!-- Linux style - left/right position -->
      <div v-else-if="(position === 'left' || position === 'right') && style === 'linux'" class="taskbar-linux">
        <div class="linux-launcher">
          <button class="launcher-btn" @click="handleLauncherClick">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="12" x2="21" y2="12"/>
              <line x1="3" y1="6" x2="21" y2="6"/>
              <line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
        </div>
        
        <div class="linux-dock">
          <!-- 系统固定应用 -->
          <button
            v-for="app in pinnedApps"
            :key="'linux-pinned-' + app.id"
            class="linux-dock-item"
            :class="{ active: isAppActive(app) }"
            @click="launchApp(app.path)"
            @mouseenter="handleAppHover($event, app)"
            @mouseleave="handleAppLeave"
            :title="app.name"
          >
            <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="linux-icon-img" />
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
              <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
            </svg>
            <!-- 活动窗口数量指示器 -->
            <div v-if="getAppWindowCount(app) > 0" class="window-indicator">
              <span class="indicator-dot"></span>
            </div>
          </button>
          
          <!-- 活动应用（不在固定列表中的运行应用） -->
          <button
            v-for="app in activeApps"
            :key="'linux-active-' + app.id"
            class="linux-dock-item active-app"
            :class="{ active: true }"
            @click="activateWindow(app.windows[0]?.handle)"
            @mouseenter="handleAppHover($event, app)"
            @mouseleave="handleAppLeave"
            :title="app.name"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
              <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
            </svg>
            <!-- 活动窗口数量指示器 -->
            <div v-if="app.windows.length > 0" class="window-indicator">
              <span class="indicator-dot"></span>
            </div>
          </button>
        </div>
        
        <div class="linux-status">
          <div class="status-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
            </svg>
          </div>
        </div>
      </div>

      <!-- Native style - traditional Windows-like -->
      <div v-else-if="(position === 'bottom' || position === 'top') && style === 'native'" class="taskbar-native">
        <div class="classic-left">
          <button class="classic-start" @click="handleLauncherClick">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 12l1.41 4.84L12 14.89l6.59 1.95L20 12l-6-6-6 6z"/>
            </svg>
          </button>
          <div class="classic-dock">
            <!-- 系统固定应用 -->
            <button 
              v-for="app in pinnedApps" 
              :key="'classic-pinned-' + app.id"
              class="classic-dock-item"
              :class="{ active: isAppActive(app) }"
              @click="launchApp(app.path)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="classic-icon-img" />
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
              </svg>
              <!-- 活动窗口数量指示器 -->
              <div v-if="getAppWindowCount(app) > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
            
            <!-- 活动应用（不在固定列表中的运行应用） -->
            <button 
              v-for="app in activeApps" 
              :key="'classic-active-' + app.id"
              class="classic-dock-item active-app"
              :class="{ active: true }"
              @click="activateWindow(app.windows[0]?.handle)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
              </svg>
              <!-- 活动窗口数量指示器 -->
              <div v-if="app.windows.length > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
          </div>
        </div>
        
        <div class="classic-right">
          <div class="classic-status">
            <!-- 系统托盘 -->
            <span class="status-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
                <path d="M8 12l3 3 5-5"/>
              </svg>
            </span>
            <!-- 输入法 -->
            <span class="status-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </span>
            <span v-if="showTime" class="status-text">{{ currentTime }}</span>
          </div>
        </div>
      </div>

      <!-- Fluent style - Windows 11 like -->
      <div v-else-if="(position === 'top' || position === 'bottom') && style === 'fluent'" class="taskbar-fluent">
        <div class="modern-left">
          <button class="modern-search">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
            <span class="search-placeholder">Search</span>
          </button>
        </div>
        
        <div class="modern-center">
          <div class="modern-dock">
            <!-- 系统固定应用 -->
            <button 
              v-for="app in pinnedApps" 
              :key="'modern-pinned-' + app.id"
              class="modern-dock-item"
              :class="{ active: isAppActive(app) }"
              @click="launchApp(app.path)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <div class="modern-icon">
                <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="modern-icon-img" />
                <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <!-- 活动窗口数量指示器 -->
              <div v-if="getAppWindowCount(app) > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
            
            <!-- 活动应用（不在固定列表中的运行应用） -->
            <button 
              v-for="app in activeApps" 
              :key="'modern-active-' + app.id"
              class="modern-dock-item active-app"
              :class="{ active: true }"
              @click="activateWindow(app.windows[0]?.handle)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <div class="modern-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <!-- 活动窗口数量指示器 -->
              <div v-if="app.windows.length > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
          </div>
        </div>
        
        <div class="modern-right">
          <div class="modern-status">
            <!-- 系统托盘 -->
            <span class="status-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
                <path d="M8 12l3 3 5-5"/>
              </svg>
            </span>
            <!-- 输入法 -->
            <span class="status-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </span>
            <span v-if="showTime" class="status-text">{{ currentTime }}</span>
          </div>
        </div>
      </div>

      <!-- Rounded style - left and right bars with background, center with circular icons -->
      <div v-else-if="(position === 'top' || position === 'bottom') && style === 'rounded'" class="taskbar-rounded">
        <div class="rounded-left" :class="`corner-${cornerType}`" :style="{ gap: `${roundedLeftGap}px` }">
          <button class="rounded-start" @click="handleLauncherClick">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 12l1.41 4.84L12 14.89l6.59 1.95L20 12l-6-6-6 6z"/>
            </svg>
          </button>
          <button class="rounded-search">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
            <span class="search-placeholder">Search</span>
          </button>
        </div>
        
        <div class="rounded-center" :style="{ margin: `0 ${roundedCenterGap}px` }">
          <div class="rounded-dock" :style="{ gap: `${roundedDockGap}px` }">
            <!-- 系统固定应用 -->
            <button 
              v-for="app in pinnedApps" 
              :key="'rounded-pinned-' + app.id"
              class="rounded-dock-item"
              :class="{ active: isAppActive(app) }"
              @click="launchApp(app.path)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <div class="rounded-icon">
                <img v-if="app.icon" :src="getAppIconUrl(app.icon)" class="rounded-icon-img" />
                <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <!-- 活动窗口数量指示器 -->
              <div v-if="getAppWindowCount(app) > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
            
            <!-- 活动应用（不在固定列表中的运行应用） -->
            <button 
              v-for="app in activeApps" 
              :key="'rounded-active-' + app.id"
              class="rounded-dock-item active-app"
              :class="{ active: true }"
              @click="activateWindow(app.windows[0]?.handle)"
              @mouseenter="handleAppHover($event, app)"
              @mouseleave="handleAppLeave"
              :title="app.name"
            >
              <div class="rounded-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="3" y="3" width="18" height="20" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M9 3v20M6 10h6M6 14h6" fill="none" stroke="currentColor" stroke-width="1.5"/>
                </svg>
              </div>
              <!-- 活动窗口数量指示器 -->
              <div v-if="app.windows.length > 0" class="window-indicator">
                <span class="indicator-dot"></span>
              </div>
            </button>
          </div>
        </div>
        
        <div class="rounded-right" :class="`corner-${cornerType}`">
          <div class="rounded-status" :style="{ gap: `${roundedRightGap}px` }">
            <!-- 系统托盘 -->
            <div 
              class="status-item tray-trigger" 
              @click="handleSystemTrayClick"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/>
                <path d="M8 12l3 3 5-5"/>
              </svg>
            </div>
            
            <!-- 输入法 -->
            <div class="status-item">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>

            <!-- 快捷设置 -->
            <div 
              class="status-item"
              @click="handleQuickSettingsClick"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 6V4m0 2a2 2 0 1 0 0 4m0-4a2 2 0 1 1 0 4m-6 8a2 2 0 1 0 0-4m0 4a2 2 0 1 1 0-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 1 0 0-4m0 4a2 2 0 1 1 0-4m0 4v2m0-6V4"/>
              </svg>
            </div>
            <!-- 时钟 - 点击打开通知中心 -->
            <div 
              v-if="showTime" 
              class="status-time"
              @click="handleNotificationCenterClick"
            >{{ currentTime }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右键菜单 -->
    <TaskbarContextMenu 
      :is-visible="showContextMenu"
      :position="contextMenuPosition"
      @close="handleCloseContextMenu"
      @open-settings="handleOpenSettings"
    />
  </div>
</template>

<script setup>import { ref, onMounted, onUnmounted, watch, markRaw, computed, defineEmits } from 'vue';
import TaskbarContextMenu from './TaskbarContextMenu.vue';
const handleMouseEnterTaskbar = () => {
 window.electronAPI?.taskbar?.sendMouseEnter();
};
const handleMouseLeaveTaskbar = () => {
  window.electronAPI?.taskbar?.sendMouseLeave();
  // 鼠标离开任务栏区域时，清除预览和悬停状态
  showPreview.value = false;
  hoveredApp.value = null;
  if (hoverTimer) {
    clearTimeout(hoverTimer);
    hoverTimer = null;
  }
};

const props = defineProps({
  position: {
    type: String,
    default: 'bottom',
    validator: (value) => ['top', 'bottom', 'left', 'right'].includes(value)
  },
  style: {
    type: String,
    default: 'user',
    validator: (value) => ['macos', 'user', 'linux', 'native', 'fluent', 'rounded'].includes(value)
  },
  showTime: {
    type: Boolean,
    default: true
  },
  autoHide: {
    type: Boolean,
    default: false
  },
  showFromEdge: {
    type: Boolean,
    default: false
  },
  blurRadius: {
    type: Number,
    default: 20
  },
  glassStrength: {
    type: Number,
    default: 0.8
  },
  iconColor: {
    type: String,
    default: '#ffffff'
  },
  barColor: {
    type: String,
    default: 'rgba(0, 0, 0, 0.8)'
  },
  cornerType: {
    type: String,
    default: 'rounded'
  },
  iconOpacity: {
    type: Number,
    default: 1
  },
  alignment: {
    type: String,
    default: 'right',
    validator: (value) => ['left', 'center', 'right'].includes(value)
  },
  // Rounded style spacing
  roundedDockGap: {
    type: Number,
    default: 6
  },
  roundedCenterGap: {
    type: Number,
    default: 12
  },
  roundedLeftGap: {
    type: Number,
    default: 8
  },
  roundedRightGap: {
    type: Number,
    default: 8
  }
})

const emit = defineEmits(['launcher-click'])

const currentTime = ref('')

const isHidden = ref(false)
let hideTimer = null

// 右键菜单状态
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })

// 系统固定应用列表
const pinnedApps = ref([])

// 活动窗口列表
const activeWindows = ref([])

// 当前悬停的应用（用于显示窗口预览）
const hoveredApp = ref(null)
const hoverPosition = ref({ x: 0, y: 0 })
const showPreview = ref(false)

// 判断应用是否有活动窗口
const isAppActive = (app) => {
  if (!app.path || activeWindows.value.length === 0) return false
  const appExe = app.path.toLowerCase().split('\\').pop().replace('.exe', '')
  
  const found = activeWindows.value.some(w => {
    if (!w.processPath) return false
    return w.processPath.toLowerCase().includes(appExe)
  })
  
  return found
}

// 获取应用的活动窗口数量
const getAppWindowCount = (app) => {
  if (!app.path || activeWindows.value.length === 0) return 0
  const appExe = app.path.toLowerCase().split('\\').pop().replace('.exe', '')
  return activeWindows.value.filter(w => {
    if (!w.processPath) return false
    return w.processPath.toLowerCase().includes(appExe)
  }).length
}

// 获取应用的活动窗口列表
const getAppWindows = (app) => {
  if (!app.path || activeWindows.value.length === 0) return []
  const appExe = app.path.toLowerCase().split('\\').pop().replace('.exe', '')
  return activeWindows.value.filter(w => {
    if (!w.processPath) return false
    return w.processPath.toLowerCase().includes(appExe)
  })
}

// 获取不在固定列表中的活动应用
const activeApps = computed(() => {
  const activeProcesses = new Map()
  
  // 系统进程列表（不显示在任务栏）
  const systemProcesses = [
    'applicationframehost.exe',
    'svchost.exe',
    'csrss.exe',
    'wininit.exe',
    'services.exe',
    'lsass.exe',
    'smss.exe',
    'explorer.exe',
    'taskmgr.exe',
    'conhost.exe',
    'dwm.exe',
    'runtimebroker.exe',
    'backgroundtaskhost.exe',
    'win32calc.exe',
    'systemsettings.exe',
    'searchui.exe',
    'textinputhost.exe',
    'sihost.exe',
    'wlanext.exe',
    'audiodg.exe',
    'spoolsv.exe',
    'wuauclt.exe',
    'msmpeng.exe',
    'mpssvc.exe',
    'nvvsvc.exe',
    'nvidia*',
    'amd*',
    'intel*',
    'taskbar.exe',
    'quasar.exe',
    'electron.exe'
  ]
  
  console.log('[TaskBar] activeApps computed - activeWindows length:', activeWindows.value?.length || 0)
  console.log('[TaskBar] activeApps computed - pinnedApps length:', pinnedApps.value?.length || 0)
  
  if (!activeWindows.value || activeWindows.value.length === 0) {
    console.log('[TaskBar] No active windows to process')
    return []
  }
  
  activeWindows.value.forEach(w => {
    console.log('[TaskBar] Processing window:', w.title, 'processPath:', w.processPath)
    
    if (!w.processPath) {
      console.log('[TaskBar] Skipping window without processPath:', w.title)
      return
    }
    
    const processPath = w.processPath.toLowerCase()
    const exeName = processPath.split('\\').pop()
    
    // 检查是否是系统进程
    const isSystemProcess = systemProcesses.some(sysProc => {
      if (sysProc.endsWith('*')) {
        const prefix = sysProc.slice(0, -1)
        return exeName.startsWith(prefix)
      }
      return exeName === sysProc
    })
    
    if (isSystemProcess) {
      console.log('[TaskBar] Skipping system process:', exeName)
      return
    }
    
    // 检查是否已经在固定应用中（暂时注释掉，测试活动应用显示）
    // const isPinned = pinnedApps.value.some(pinned => {
    //   if (!pinned.path) return false
    //   const pinnedExe = pinned.path.toLowerCase().split('\\').pop().replace('.exe', '')
    //   const currentExe = exeName.replace('.exe', '')
    //   const matches = pinned.path.toLowerCase().includes(currentExe)
    //   if (matches) {
    //     console.log('[TaskBar] Found pinned match:', exeName, 'matches pinned:', pinned.path)
    //   }
    //   return matches
    // })
    // 
    // if (isPinned) {
    //   console.log('[TaskBar] Skipping pinned app:', exeName)
    //   return
    // }
    
    if (!activeProcesses.has(exeName)) {
      // 尝试从活动窗口中获取图标
      let appIcon = null
      const windowWithIcon = activeWindows.value.find(win => {
        const winPath = win.processPath?.toLowerCase() || ''
        return winPath.includes(exeName.replace('.exe', '')) && win.icon
      })
      if (windowWithIcon && windowWithIcon.icon) {
        appIcon = windowWithIcon.icon
      }
      
      activeProcesses.set(exeName, {
        id: `active-${exeName}`,
        name: exeName.replace('.exe', ''),
        path: w.processPath,
        icon: appIcon,
        windows: []
      })
      console.log('[TaskBar] Adding active app:', exeName, 'with icon:', !!appIcon)
    }
    activeProcesses.get(exeName).windows.push(w)
  })
  
  const result = Array.from(activeProcesses.values())
  console.log('[TaskBar] activeApps count:', result.length)
  return result
})

// 处理应用图标路径，支持 file:// 协议和相对路径
const getAppIconUrl = (iconPath) => {
  if (!iconPath) return ''
  // 如果已经是 data: 或 http/https URL，直接返回
  if (iconPath.startsWith('data:') || iconPath.startsWith('http://') || iconPath.startsWith('https://')) {
    return iconPath
  }
  // 如果是 file:// 协议，进行 URL 编码处理
  if (iconPath.startsWith('file://')) {
    // 提取路径部分并编码
    const pathPart = iconPath.substring(7)
    return `file://${encodeURI(pathPart)}`
  }
  // 如果是本地文件路径，转换为 file:// URL
  if (iconPath.startsWith('/') || iconPath.match(/^[A-Za-z]:/)) {
    // Windows 路径处理
    let filePath = iconPath.replace(/\\/g, '/')
    // 确保 Windows 路径格式正确
    if (filePath.match(/^[A-Za-z]:/)) {
      filePath = `/${filePath}`
    }
    // 对路径进行 URL 编码以处理空格和特殊字符
    return `file://${encodeURI(filePath)}`
  }
  return iconPath
}

// 获取系统固定应用
const fetchPinnedApps = async () => {
  console.log('[TaskBar] fetchPinnedApps called')
  try {
    console.log('[TaskBar] Checking electronAPI:', !!window.electronAPI)
    console.log('[TaskBar] Checking electronAPI.taskbar:', !!window.electronAPI?.taskbar)
    console.log('[TaskBar] Checking electronAPI.taskbar.getPinnedApps:', typeof window.electronAPI?.taskbar?.getPinnedApps)
    
    const apps = await window.electronAPI?.taskbar?.getPinnedApps()
    console.log('[TaskBar] getPinnedApps returned:', apps)
    
    if (apps && apps.length > 0) {
      pinnedApps.value = apps
      console.log('[TaskBar] Loaded pinned apps:', pinnedApps.value)
    } else {
      console.log('[TaskBar] No pinned apps found or empty result')
    }
  } catch (error) {
    console.error('[TaskBar] Error fetching pinned apps:', error)
    console.error('[TaskBar] Error stack:', error.stack)
  }
}

// 获取活动窗口
const fetchActiveWindows = async () => {
  console.log('[TaskBar] fetchActiveWindows called')
  console.log('[TaskBar] window.electronAPI exists:', !!window.electronAPI)
  console.log('[TaskBar] window.electronAPI.taskbar exists:', !!window.electronAPI?.taskbar)
  console.log('[TaskBar] window.electronAPI.taskbar.getActiveWindows exists:', typeof window.electronAPI?.taskbar?.getActiveWindows)
  
  try {
    const windows = await window.electronAPI?.taskbar?.getActiveWindows()
    console.log('[TaskBar] getActiveWindows returned:', windows?.length)
    
    if (windows && windows.length > 0) {
      activeWindows.value = windows
      console.log('[TaskBar] Loaded active windows:', activeWindows.value)
    } else {
      console.log('[TaskBar] No active windows found')
    }
  } catch (error) {
    console.error('[TaskBar] Error fetching active windows:', error)
  }
}

// 激活窗口
const activateWindow = async (hwnd) => {
  try {
    await window.electronAPI?.taskbar?.activateWindow(hwnd)
    showPreview.value = false
    hoveredApp.value = null
  } catch (error) {
    console.error('[TaskBar] Error activating window:', error)
  }
}

// 关闭窗口
const closeWindow = async (hwnd) => {
  try {
    await window.electronAPI?.taskbar?.closeWindow(hwnd)
    // 刷新活动窗口列表
    setTimeout(fetchActiveWindows, 100)
  } catch (error) {
    console.error('[TaskBar] Error closing window:', error)
  }
}

// 处理应用悬停（显示窗口预览）
const handleAppHover = (event, app) => {
  const rect = event.currentTarget.getBoundingClientRect()
  console.log('[TaskBar] handleAppHover called for:', app.name, 'at position:', rect.left, rect.top)
  
  // 获取该应用的所有窗口
  let appWindows = []
  if (app.windows && app.windows.length > 0) {
    // 活动应用直接使用已有的 windows
    appWindows = app.windows
  } else if (app.path) {
    // 固定应用需要过滤
    const appExe = app.path.toLowerCase().split('\\').pop().replace('.exe', '')
    appWindows = activeWindows.value.filter(w => {
      if (!w.processPath) return false
      return w.processPath.toLowerCase().includes(appExe)
    })
  }
  
  console.log('[TaskBar] Found', appWindows.length, 'windows for app:', app.name)
  
  // 清除之前的延迟定时器
  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }
  
  // 立即设置悬停的应用（选中状态立即显示）
  hoveredApp.value = { ...app, windows: appWindows }
  
  // 延迟1秒后显示预览（调用主进程创建独立窗口）
    if (appWindows.length > 0) {
      hoverTimer = setTimeout(() => {
        console.log('[TaskBar] Showing window preview at:', rect.left + rect.width / 2, rect.top, 'position:', props.position)
        // 创建可序列化的简单对象，避免IPC克隆错误
        const previewApp = {
          id: app.id,
          name: app.name,
          path: app.path,
          icon: app.icon,
          windows: appWindows.map(w => ({
            handle: w.handle,
            title: w.title,
            processPath: w.processPath,
            isForeground: w.isForeground
          }))
        }
        console.log('[TaskBar] Sending previewApp:', JSON.stringify(previewApp))
        if (window.electronAPI && window.electronAPI.taskbar && window.electronAPI.taskbar.showWindowPreviewAt) {
          window.electronAPI.taskbar.showWindowPreviewAt(
            rect.left + rect.width / 2,
            rect.top,
            props.position,
            previewApp
          )
        } else {
          console.error('[TaskBar] electronAPI.taskbar.showWindowPreviewAt is not available')
        }
      }, 1000)
    } else {
      console.log('[TaskBar] No windows found, not showing preview')
    }
}

// 处理应用离开
const handleAppLeave = () => {
  // 不清除悬停状态和定时器，保持选中状态
  // 等待鼠标移动到预览窗口或离开任务栏区域
}

// 处理任务栏点击（关闭预览）
const handleTaskbarClick = () => {
  hoveredApp.value = null
  window.electronAPI?.taskbar?.closeWindowPreview()
}

// 启动应用
const launchApp = async (appPath) => {
  try {
    await window.electronAPI?.taskbar?.launchApp(appPath)
  } catch (error) {
    console.error('[TaskBar] Error launching app:', error)
  }
}

// Icon components are rendered directly in template

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

let timer = null
let activeWindowTimer = null
let hoverTimer = null

const handleGlobalClick = (event) => {
  const target = event.target
  if (!target.closest('.taskbar') && !target.closest('.context-menu')) {
    showContextMenu.value = false
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  // 获取系统固定应用
  fetchPinnedApps()
  // 获取活动窗口
  fetchActiveWindows()
  // 设置活动窗口定时更新（每2秒刷新一次）
  activeWindowTimer = setInterval(fetchActiveWindows, 2000)
  // 添加全局点击事件监听
  document.addEventListener('click', handleGlobalClick)
})

watch(() => props.showFromEdge, (newVal) => {
  if (newVal && props.autoHide) {
    isHidden.value = false
    if (hideTimer) {
      clearTimeout(hideTimer)
      hideTimer = null
    }
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (hideTimer) clearTimeout(hideTimer)
  if (activeWindowTimer) clearInterval(activeWindowTimer)
  if (hoverTimer) clearTimeout(hoverTimer)
  // 移除全局点击事件监听
  document.removeEventListener('click', handleGlobalClick)
})

const handleLauncherClick = (event) => {
  emit('launcher-click')
  const rect = event.currentTarget.getBoundingClientRect()
  window.electronAPI?.taskbar?.showStartMenuAt(Math.round(rect.x + rect.width / 2), Math.round(rect.y))
}

const handleContextMenu = (event) => {
  event.preventDefault()
  event.stopPropagation()
  
  showContextMenu.value = true
  contextMenuPosition.value = {
    x: event.clientX,
    y: event.clientY
  }
}

const handleCloseContextMenu = () => {
  showContextMenu.value = false
}

const handleOpenSettings = () => {
  showContextMenu.value = false
}

const handleQuickSettingsClick = (event) => {
  const rect = event.currentTarget.getBoundingClientRect()
  window.electronAPI?.taskbar?.showQuickSettingsAt(Math.round(rect.x + rect.width / 2), Math.round(rect.y))
}

const handleSystemTrayClick = (event) => {
  const rect = event.currentTarget.getBoundingClientRect()
  window.electronAPI?.taskbar?.showSystemTrayAt(Math.round(rect.x + rect.width / 2), Math.round(rect.y))
}

const handleNotificationCenterClick = (event) => {
  const rect = event.currentTarget.getBoundingClientRect()
  window.electronAPI?.taskbar?.showNotificationCenterAt(Math.round(rect.x + rect.width / 2), Math.round(rect.y))
}

const handleMouseEnter = () => {
  if (props.autoHide && isHidden.value) {
    isHidden.value = false
  }
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  window.electronAPI?.taskbar?.setIgnoreMouseEvents(false)
}

const handleMouseLeave = () => {
  if (props.autoHide) {
    hideTimer = setTimeout(() => {
      isHidden.value = true
    }, 2000)
  }
  window.electronAPI?.taskbar?.setIgnoreMouseEvents(true)
}

const getTaskbarPosition = () => {
  const margin = 20
  const positions = {
    bottom: { bottom: margin },
    top: { top: margin },
    left: { left: margin },
    right: { right: margin }
  }
  return positions[props.position] || positions.bottom
}

const getTaskbarStyle = () => {
  const baseStyle = {
    backdropFilter: `blur(${props.blurRadius}px)`,
    background: props.barColor,
    color: props.iconColor
  }

  const margin = 20

  const getAlignStyles = () => {
    const alignStyles = {}
    if (props.position === 'bottom' || props.position === 'top') {
      if (props.alignment === 'left') {
        alignStyles.left = margin
        alignStyles.right = 'auto'
        alignStyles.transform = 'translateX(0)'
      } else if (props.alignment === 'center') {
        alignStyles.left = '50%'
        alignStyles.right = 'auto'
        alignStyles.transform = 'translateX(-50%)'
      } else {
        alignStyles.right = margin
        alignStyles.left = 'auto'
        alignStyles.transform = 'translateX(0)'
      }
    } else {
      alignStyles.top = '50%'
      alignStyles.transform = 'translateY(-50%)'
    }
    return alignStyles
  }

  if (!props.autoHide || !isHidden.value) {
    return {
      ...baseStyle,
      ...getTaskbarPosition(),
      ...getAlignStyles(),
      transitionTimingFunction: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)'
    }
  }

  const hiddenOffset = 150

  const hidePositions = {
    bottom: { bottom: -hiddenOffset },
    top: { top: -hiddenOffset },
    left: { left: -hiddenOffset },
    right: { right: -hiddenOffset }
  }

  return {
    ...baseStyle,
    ...hidePositions[props.position],
    ...getAlignStyles(),
    transitionTimingFunction: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)'
  }
}
</script>

<style scoped>
.taskbar {
  position: relative;
  width: 100%;
  height: 100%;
  background: transparent;
  backdrop-filter: none;
  z-index: 1000;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.taskbar-content {
  height: auto;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(20px);
  padding: 4px 8px;
  position: fixed;
}

/* Auto hide animations */
.taskbar.auto-hide .taskbar-content {
  transition: bottom 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              top 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              left 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              right 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              transform 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: bottom, top, left, right, transform;
}

.taskbar.auto-hide .taskbar-content.hidden {
  pointer-events: none;
}

/* Bottom position */
.position-bottom .taskbar-content {
  /* 移除 bottom: 20px，让外层 flex 居中生效 */
}

.position-bottom .taskbar-content.align-left {
  left: 20px;
  transform: translateX(0);
}

.position-bottom .taskbar-content.align-center {
  left: 50%;
  transform: translateX(-50%);
}

.position-bottom .taskbar-content.align-right {
  right: 20px;
  transform: translateX(0);
}

/* Top position */
.position-top .taskbar-content {
  top: 20px;
}

.position-top .taskbar-content.align-left {
  left: 20px;
  transform: translateX(0);
}

.position-top .taskbar-content.align-center {
  left: 50%;
  transform: translateX(-50%);
}

.position-top .taskbar-content.align-right {
  right: 20px;
  transform: translateX(0);
}

/* Left position */
.position-left .taskbar-content {
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
}

/* Right position */
.position-right .taskbar-content {
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
}

/* Corner types for bottom position (default) */
.corner-rounded.corner-rounded-bottom {
  border-radius: 12px;
}

.corner-pill.corner-pill-bottom {
  border-radius: 24px;
}

.corner-square.corner-square-bottom {
  border-radius: 0;
}

/* Corner types for top position */
.corner-rounded.corner-rounded-top {
  border-radius: 12px;
}

.corner-pill.corner-pill-top {
  border-radius: 24px;
}

.corner-square.corner-square-top {
  border-radius: 0;
}

/* Corner types for left position */
.corner-rounded.corner-rounded-left {
  border-radius: 12px;
}

.corner-pill.corner-pill-left {
  border-radius: 24px;
}

.corner-square.corner-square-left {
  border-radius: 0;
}

/* Corner types for right position */
.corner-rounded.corner-rounded-right {
  border-radius: 12px;
}

.corner-pill.corner-pill-right {
  border-radius: 24px;
}

.corner-square.corner-square-right {
  border-radius: 0;
}

/* macOS style - top position */
.taskbar-macos {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 8px;
}

.macos-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.macos-dots {
  display: flex;
  gap: 8px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot-red { background: #ff5f57; }
.dot-yellow { background: #ffbd2e; }
.dot-green { background: #27ca40; }

.macos-apple {
  color: #c0c0c0;
}

.macos-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.dock-items {
  display: flex;
  gap: 4px;
}

.dock-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 2px 8px;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: #c0c0c0;
  transition: all 0.2s ease;
}

.dock-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.dock-item.active {
  color: white;
}

.dock-icon {
  font-size: 16px;
}

.macos-icon-img {
  width: 16px;
  height: 16px;
  object-fit: contain;
  border-radius: 2px;
}

.dock-label {
  font-size: 10px;
}

.dock-item.active-app {
  opacity: 0.8;
}

.macos-right {
  display: flex;
  align-items: center;
}

.status-icons {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.75);
  padding: 4px 8px;
}

.status-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s ease;
  cursor: pointer;
}

.status-icon:hover {
  background: rgba(255, 255, 255, 0.1);
}

.status-icon svg {
  width: 14px;
  height: 14px;
}

.status-text {
  font-size: 11px;
  font-weight: 500;
  margin-left: 8px;
  padding-left: 8px;
  border-left: 1px solid rgba(255, 255, 255, 0.15);
}

/* User style - bottom position */
.taskbar.position-bottom.style-user {
  padding: 8px 16px;
  border-radius: 20px;
}

.taskbar-user {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-dock {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.user-dock-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: rgba(255, 255, 255, 0.8);
}

.user-dock-item:hover,
.user-dock-item.active {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.user-dock-item.expanded {
  width: 56px;
  height: 56px;
}

.app-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.app-icon-img {
  width: 24px;
  height: 24px;
  object-fit: contain;
  border-radius: 4px;
}

.app-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 2px;
  text-align: center;
  max-width: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.add-button {
  background: rgba(0, 200, 255, 0.2);
  border: 1px dashed rgba(0, 200, 255, 0.5);
}

.add-button:hover {
  background: rgba(0, 200, 255, 0.3);
  border-color: rgba(0, 200, 255, 0.7);
}

.user-icon {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.user-dock-item.active-app {
  opacity: 0.8;
}

/* Linux style - left/right position */

.taskbar-linux {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px 0;
}

.linux-launcher {
  display: flex;
  justify-content: center;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.launcher-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: white;
  transition: all 0.2s ease;
}

.launcher-btn:hover {
  transform: scale(1.05);
}

.linux-dock {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  gap: 4px;
}

.linux-dock-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
}

.linux-dock-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.linux-dock-item.active {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.linux-icon-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
  border-radius: 4px;
}

.linux-dock-item.active-app {
  opacity: 0.8;
}

.linux-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
}

.status-item {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Classic style - traditional Windows-like */
.taskbar-classic {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 4px;
  border-top: 1px solid #3a3a3a;
}

.classic-left {
  display: flex;
  align-items: center;
  gap: 2px;
}

.classic-start {
  width: 40px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to bottom, #4a90d9 0%, #2d5aa0 100%);
  border: 1px solid #1e3a5f;
  border-radius: 2px;
  cursor: pointer;
  color: white;
  transition: all 0.2s ease;
}

.classic-start:hover {
  background: linear-gradient(to bottom, #5a9fe9 0%, #3d6abb 100%);
}

.classic-dock {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-left: 8px;
  border-left: 1px solid #3a3a3a;
}

.classic-dock-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
}

.classic-dock-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.classic-dock-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.classic-icon-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
  border-radius: 4px;
}

.classic-dock-item.active-app {
  opacity: 0.8;
}

.classic-right {
  display: flex;
  align-items: center;
}

.classic-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
  border-left: 1px solid #3a3a3a;
  color: rgba(255, 255, 255, 0.7);
}

/* Modern style - Windows 11 like */
.taskbar-modern {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 12px;
}

.position-top .taskbar-modern {
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
}

.position-bottom .taskbar-modern {
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
}

.modern-left {
  display: flex;
  align-items: center;
}

.modern-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
  min-width: 200px;
}

.modern-search:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
}

.search-placeholder {
  font-size: 12px;
}

.modern-center {
  display: flex;
  justify-content: center;
}

.modern-dock {
  display: flex;
  align-items: center;
  gap: 4px;
}

.modern-dock-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modern-icon {
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
}

.modern-dock-item:hover .modern-icon {
  color: white;
}

.modern-dock-item.active {
  background: rgba(255, 255, 255, 0.1);
}

.modern-dock-item.active .modern-icon {
  color: white;
}

.modern-icon-img {
  width: 18px;
  height: 18px;
  object-fit: contain;
  border-radius: 4px;
}

.modern-dock-item.active-app {
  opacity: 0.8;
}

.modern-right {
  display: flex;
  align-items: center;
}

.modern-status {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.75);
  padding: 4px 12px;
}

.modern-status .status-icon {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.modern-status .status-icon svg {
  width: 15px;
  height: 15px;
}

.modern-status .status-icon:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

/* Rounded style - left and right bars with background, center with circular icons */
.taskbar-rounded {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  width: auto;
  min-width: fit-content;
  padding: 0;
}

.rounded-left {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(20px);
}

.rounded-left.corner-rounded {
  border-radius: 12px 6px 6px 12px;
}

.rounded-left.corner-pill {
  border-radius: 24px;
}

.rounded-left.corner-square {
  border-radius: 0;
}

.rounded-start {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: white;
  transition: all 0.2s ease;
}

.rounded-start:hover {
  background: rgba(255, 255, 255, 0.15);
}

.rounded-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
  min-width: 180px;
}

.rounded-search:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
}

.rounded-center {
  display: flex;
  justify-content: center;
  align-items: center;
  background: transparent;
}

.rounded-dock {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
}

.rounded-dock-item {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
}

.rounded-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
}

.rounded-dock-item:hover .rounded-icon {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: scale(1.1);
}

.rounded-dock-item.active .rounded-icon {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.rounded-icon-img {
  width: 22px;
  height: 22px;
  object-fit: contain;
  border-radius: 4px;
}

.rounded-dock-item.active-app {
  opacity: 0.8;
}

.rounded-right {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(20px);
}

.rounded-right.corner-rounded {
  border-radius: 6px 12px 12px 6px;
}

.rounded-right.corner-pill {
  border-radius: 24px;
}

.rounded-right.corner-square {
  border-radius: 0;
}

.rounded-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.8);
  padding: 4px 12px;
}

.rounded-status .status-item {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.rounded-status .status-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.rounded-status .status-item svg {
  width: 16px;
  height: 16px;
}

.rounded-status .status-time {
  font-size: 12px;
  font-weight: 500;
  padding-left: 12px;
  padding-right: 4px;
  border-left: 1px solid rgba(255, 255, 255, 0.15);
  min-width: 40px;
  text-align: center;
}

/* Native style (renamed from classic) */
.taskbar-native {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 4px;
  border-top: 1px solid #3a3a3a;
}

/* Fluent style (renamed from modern) */
.taskbar-fluent {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 12px;
}

/* Window indicator styles */
.window-indicator {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.indicator-dot {
  width: 6px;
  height: 6px;
  background: #0078d4;
  border-radius: 50%;
  box-shadow: 0 0 4px rgba(0, 120, 212, 0.5);
}

.dock-item,
.user-dock-item,
.linux-dock-item,
.classic-dock-item,
.modern-dock-item,
.rounded-dock-item {
  position: relative;
}

.dock-item.active,
.user-dock-item.active,
.linux-dock-item.active,
.classic-dock-item.active,
.modern-dock-item.active,
.rounded-dock-item.active {
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}
</style>
