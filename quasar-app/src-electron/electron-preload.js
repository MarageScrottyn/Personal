import { contextBridge, ipcRenderer } from 'electron'

console.log('[Preload] electron-preload.js loaded')

const electronAPI = {
  platform: process.platform,

  window: {
    minimize: () => ipcRenderer.invoke('window:minimize'),
    maximize: () => ipcRenderer.invoke('window:maximize'),
    close: () => ipcRenderer.invoke('window:close'),
    isMaximized: () => ipcRenderer.invoke('window:isMaximized'),
  },

  app: {
    getVersion: () => ipcRenderer.invoke('app:getVersion'),
    getSettings: () => ipcRenderer.invoke('app:getSettings'),
    setMainDevToolsEnabled: (enabled) => ipcRenderer.invoke('app:setMainDevToolsEnabled', enabled),
  },

  dialog: {
    confirm: (options) => ipcRenderer.invoke('dialog:confirm', options),
  },

  dynamicIsland: {
    expand: () => ipcRenderer.send('dynamicIsland:expand'),
    collapse: () => ipcRenderer.send('dynamicIsland:collapse'),
    toggle: () => ipcRenderer.send('dynamicIsland:toggle'),
    minimize: () => ipcRenderer.send('dynamicIsland:minimize'),
    restore: () => ipcRenderer.send('dynamicIsland:restore'),
    update: (data) => ipcRenderer.send('dynamicIsland:update', data),
    show: () => ipcRenderer.send('dynamicIsland:show'),
    hide: () => ipcRenderer.send('dynamicIsland:hide'),
    mouseEnter: () => ipcRenderer.send('dynamicIsland:mouse-enter'),
    mouseLeave: () => ipcRenderer.send('dynamicIsland:mouse-leave'),
    isEnabled: () => ipcRenderer.invoke('dynamicIsland:isEnabled'),
    setEnabled: (enabled) => ipcRenderer.invoke('dynamicIsland:setEnabled', enabled),
    setMusicControlEnabled: (enabled) => ipcRenderer.invoke('dynamicIsland:setMusicControlEnabled', enabled),
    setDevToolsEnabled: (enabled) => ipcRenderer.invoke('dynamicIsland:setDevToolsEnabled', enabled),
    getSettings: () => ipcRenderer.invoke('dynamicIsland:getSettings'),
    openDevTools: () => ipcRenderer.send('dynamicIsland:openDevTools'),
    closeDevTools: () => ipcRenderer.send('dynamicIsland:closeDevTools'),
    setSettings: (settings) => ipcRenderer.invoke('dynamicIsland:setSettings', settings),
    updateStyle: (style) => ipcRenderer.send('dynamicIsland:updateStyle', style),
    updatePosition: (position) => ipcRenderer.send('dynamicIsland:updatePosition', position),
    addTask: (task) => ipcRenderer.send('dynamicIsland:addTask', task),
    removeTask: (taskId) => ipcRenderer.send('dynamicIsland:removeTask', taskId),
    dragEnd: (position) => ipcRenderer.send('dynamicIsland:drag-end', position),
    onExpanded: (callback) => {
      const handler = (event, expanded) => callback(expanded)
      ipcRenderer.on('dynamicIsland:expanded', handler)
      return () => ipcRenderer.removeListener('dynamicIsland:expanded', handler)
    },
    onUpdate: (callback) => {
      const handler = (event, data) => callback(data)
      ipcRenderer.on('dynamicIsland:update', handler)
      return () => ipcRenderer.removeListener('dynamicIsland:update', handler)
    },
    onStyleUpdated: (callback) => {
      const handler = (event, style) => callback(style)
      ipcRenderer.on('dynamicIsland:styleUpdated', handler)
      return () => ipcRenderer.removeListener('dynamicIsland:styleUpdated', handler)
    },
    onPositionUpdated: (callback) => {
      const handler = (event, position) => callback(position)
      ipcRenderer.on('dynamicIsland:positionUpdated', handler)
      return () => ipcRenderer.removeListener('dynamicIsland:positionUpdated', handler)
    },
    onSettingsUpdated: (callback) => {
      const handler = (event, settings) => callback(settings)
      ipcRenderer.on('dynamicIsland:settingsUpdated', handler)
      return () => ipcRenderer.removeListener('dynamicIsland:settingsUpdated', handler)
    },
    onNavigate: (callback) => {
      const handler = (event, path) => callback(path)
      ipcRenderer.on('navigate', handler)
      return () => ipcRenderer.removeListener('navigate', handler)
    },
  },

  media: {
    getCurrentTrack: () => ipcRenderer.invoke('media:getCurrentTrack'),
    play: () => ipcRenderer.invoke('media:play'),
    pause: () => ipcRenderer.invoke('media:pause'),
    toggle: () => ipcRenderer.invoke('media:toggle'),
    next: () => ipcRenderer.invoke('media:next'),
    previous: () => ipcRenderer.invoke('media:previous'),
    seek: (position) => ipcRenderer.invoke('media:seek', position),
    onUpdate: (callback) => {
      const handler = (event, data) => callback(data)
      ipcRenderer.on('media-update', handler)
      return () => ipcRenderer.removeListener('media-update', handler)
    },
  },

  nativeService: {
    getCPUUsage: () => ipcRenderer.invoke('nativeService:getCPUUsage'),
    getMemoryUsage: () => ipcRenderer.invoke('nativeService:getMemoryUsage'),
    getBatteryStatus: () => ipcRenderer.invoke('nativeService:getBatteryStatus'),
    getVolumeLevel: () => ipcRenderer.invoke('nativeService:getVolumeLevel'),
    getAllStats: () => ipcRenderer.invoke('nativeService:getAllStats'),
    start: () => ipcRenderer.invoke('nativeService:start'),
    stop: () => ipcRenderer.invoke('nativeService:stop'),
    startMediaPolling: (interval) => ipcRenderer.invoke('nativeService:startMediaPolling', interval),
    stopMediaPolling: () => ipcRenderer.invoke('nativeService:stopMediaPolling'),
    onBroadcast: (callback) => {
      const handler = (event, data) => callback(data)
      ipcRenderer.on('nativeService:broadcast', handler)
      return () => ipcRenderer.removeListener('nativeService:broadcast', handler)
    },
  },

  taskbar: {
    getSettings: () => ipcRenderer.invoke('taskbar:getSettings'),
    setPosition: (position) => ipcRenderer.send('taskbar:setPosition', position),
    setStyle: (style) => ipcRenderer.send('taskbar:setStyle', style),
    setEnabled: (enabled) => ipcRenderer.send('taskbar:setEnabled', enabled),
    setAutoHide: (autoHide) => ipcRenderer.send('taskbar:setAutoHide', autoHide),
    setAutoHideDelay: (delay) => ipcRenderer.send('taskbar:setAutoHideDelay', delay),
    setHideOnLeave: (hideOnLeave) => ipcRenderer.send('taskbar:setHideOnLeave', hideOnLeave),
    setShowTime: (showTime) => ipcRenderer.send('taskbar:setShowTime', showTime),
    launchApp: (appPath) => ipcRenderer.send('taskbar:launchApp', appPath),
    getPinnedApps: () => ipcRenderer.invoke('taskbar:getPinnedApps'),
    openDevTools: () => ipcRenderer.send('taskbar:openDevTools'),
    setDevToolsEnabled: (enabled) => ipcRenderer.invoke('taskbar:setDevToolsEnabled', enabled),
    updateSize: (size) => ipcRenderer.send('taskbar:updateSize', size),
    setOpacity: (opacity) => ipcRenderer.send('taskbar:setOpacity', opacity),
    setBlurRadius: (radius) => ipcRenderer.send('taskbar:setBlurRadius', radius),
    setGlassStrength: (strength) => ipcRenderer.send('taskbar:setGlassStrength', strength),
    setIconColor: (color) => ipcRenderer.send('taskbar:setIconColor', color),
    setBarColor: (color) => ipcRenderer.send('taskbar:setBarColor', color),
    setCornerType: (type) => ipcRenderer.send('taskbar:setCornerType', type),
    setIgnoreMouseEvents: (ignore) => ipcRenderer.send('taskbar:setIgnoreMouseEvents', ignore),
    setIconOpacity: (opacity) => ipcRenderer.send('taskbar:setIconOpacity', opacity),
    setRoundedDockGap: (gap) => ipcRenderer.send('taskbar:setRoundedDockGap', gap),
    setRoundedCenterGap: (gap) => ipcRenderer.send('taskbar:setRoundedCenterGap', gap),
    setRoundedLeftGap: (gap) => ipcRenderer.send('taskbar:setRoundedLeftGap', gap),
    setRoundedRightGap: (gap) => ipcRenderer.send('taskbar:setRoundedRightGap', gap),
    setAlignment: (alignment) => ipcRenderer.send('taskbar:setAlignment', alignment),
    sendMouseEnter: () => ipcRenderer.send('taskbar:mouseEnter'),
    sendMouseLeave: () => ipcRenderer.send('taskbar:mouseLeave'),
    toggleStartMenu: () => ipcRenderer.send('taskbar:toggleStartMenu'),
    closeStartMenu: () => ipcRenderer.send('taskbar:closeStartMenu'),
    showStartMenuAt: (x, y) => ipcRenderer.send('taskbar:showStartMenuAt', x, y),
    toggleQuickSettings: () => ipcRenderer.send('taskbar:toggleQuickSettings'),
    closeQuickSettings: () => ipcRenderer.send('taskbar:closeQuickSettings'),
    showQuickSettingsAt: (x, y) => ipcRenderer.send('taskbar:showQuickSettingsAt', x, y),
    toggleNotificationCenter: () => ipcRenderer.send('taskbar:toggleNotificationCenter'),
    closeNotificationCenter: () => ipcRenderer.send('taskbar:closeNotificationCenter'),
    showNotificationCenterAt: (x, y) => ipcRenderer.send('taskbar:showNotificationCenterAt', x, y),
    showSystemTrayAt: (x, y) => ipcRenderer.send('taskbar:showSystemTrayAt', x, y),
    closeSystemTray: () => ipcRenderer.send('taskbar:closeSystemTray'),
    onPositionUpdated: (callback) => {
      const handler = (event, position) => callback(position)
      ipcRenderer.on('taskbar:positionUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:positionUpdated', handler)
    },
    onStyleUpdated: (callback) => {
      const handler = (event, style) => callback(style)
      ipcRenderer.on('taskbar:styleUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:styleUpdated', handler)
    },
    onShowTimeUpdated: (callback) => {
      const handler = (event, showTime) => callback(showTime)
      ipcRenderer.on('taskbar:showTimeUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:showTimeUpdated', handler)
    },
    onAutoHideUpdated: (callback) => {
      const handler = (event, autoHide) => callback(autoHide)
      ipcRenderer.on('taskbar:autoHideUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:autoHideUpdated', handler)
    },
    onBlurRadiusUpdated: (callback) => {
      const handler = (event, radius) => callback(radius)
      ipcRenderer.on('taskbar:blurRadiusUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:blurRadiusUpdated', handler)
    },
    onGlassStrengthUpdated: (callback) => {
      const handler = (event, strength) => callback(strength)
      ipcRenderer.on('taskbar:glassStrengthUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:glassStrengthUpdated', handler)
    },
    onIconColorUpdated: (callback) => {
      const handler = (event, color) => callback(color)
      ipcRenderer.on('taskbar:iconColorUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:iconColorUpdated', handler)
    },
    onBarColorUpdated: (callback) => {
      const handler = (event, color) => callback(color)
      ipcRenderer.on('taskbar:barColorUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:barColorUpdated', handler)
    },
    onCornerTypeUpdated: (callback) => {
      const handler = (event, type) => callback(type)
      ipcRenderer.on('taskbar:cornerTypeUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:cornerTypeUpdated', handler)
    },
    onAlignmentUpdated: (callback) => {
      const handler = (event, alignment) => callback(alignment)
      ipcRenderer.on('taskbar:alignmentUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:alignmentUpdated', handler)
    },
    onShowFromEdge: (callback) => {
      const handler = (event) => callback()
      ipcRenderer.on('taskbar:showFromEdge', handler)
      return () => ipcRenderer.removeListener('taskbar:showFromEdge', handler)
    },
    onIconOpacityUpdated: (callback) => {
      const handler = (event, opacity) => callback(opacity)
      ipcRenderer.on('taskbar:iconOpacityUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:iconOpacityUpdated', handler)
    },
    onRoundedDockGapUpdated: (callback) => {
      const handler = (event, gap) => callback(gap)
      ipcRenderer.on('taskbar:roundedDockGapUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:roundedDockGapUpdated', handler)
    },
    onRoundedCenterGapUpdated: (callback) => {
      const handler = (event, gap) => callback(gap)
      ipcRenderer.on('taskbar:roundedCenterGapUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:roundedCenterGapUpdated', handler)
    },
    onRoundedLeftGapUpdated: (callback) => {
      const handler = (event, gap) => callback(gap)
      ipcRenderer.on('taskbar:roundedLeftGapUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:roundedLeftGapUpdated', handler)
    },
    onRoundedRightGapUpdated: (callback) => {
      const handler = (event, gap) => callback(gap)
      ipcRenderer.on('taskbar:roundedRightGapUpdated', handler)
      return () => ipcRenderer.removeListener('taskbar:roundedRightGapUpdated', handler)
    },
    onLaunch: (callback) => {
      const handler = (event, appId) => callback(appId)
      ipcRenderer.on('taskbar:launch', handler)
      return () => ipcRenderer.removeListener('taskbar:launch', handler)
    },
    getActiveWindows: () => ipcRenderer.invoke('taskbar:getActiveWindows'),
    activateWindow: (hwnd) => ipcRenderer.invoke('taskbar:activateWindow', hwnd),
    closeWindow: (hwnd) => ipcRenderer.invoke('taskbar:closeWindow', hwnd),
    showWindowPreviewAt: (x, y, taskbarPosition, app) => ipcRenderer.send('taskbar:showWindowPreviewAt', x, y, taskbarPosition, app),
    closeWindowPreview: () => ipcRenderer.send('taskbar:closeWindowPreview'),
    refreshWindowPreview: () => ipcRenderer.send('taskbar:refreshWindowPreview'),
    onWindowPreviewUpdate: (callback) => {
      const handler = (event, data) => callback(event, data)
      ipcRenderer.on('window-preview:update', handler)
      return () => ipcRenderer.removeListener('window-preview:update', handler)
    },
    onCloseWindowPreview: (callback) => {
      const handler = () => callback()
      ipcRenderer.on('window-preview:close', handler)
      return () => ipcRenderer.removeListener('window-preview:close', handler)
    },
    removeWindowPreviewUpdateListener: (callback) => {
      ipcRenderer.removeListener('window-preview:update', callback)
    },
    removeCloseWindowPreviewListener: (callback) => {
      ipcRenderer.removeListener('window-preview:close', callback)
    },
  },

  on: {
    windowMaximized: (callback) => {
      const handler = () => callback()
      ipcRenderer.on('window:maximized', handler)
      return () => ipcRenderer.removeListener('window:maximized', handler)
    },
    windowUnmaximized: (callback) => {
      const handler = () => callback()
      ipcRenderer.on('window:unmaximized', handler)
      return () => ipcRenderer.removeListener('window:unmaximized', handler)
    },
    navigate: (callback) => {
      const handler = (event, path) => callback(path)
      ipcRenderer.on('navigate', handler)
      return () => ipcRenderer.removeListener('navigate', handler)
    },
  },

  settings: {
    toggleMainWindowDevTools: (enabled) => ipcRenderer.send('settings:toggleMainWindowDevTools', enabled),
    toggleTaskbarDevTools: (enabled) => ipcRenderer.send('settings:toggleTaskbarDevTools', enabled),
    toggleIslandDevTools: (enabled) => ipcRenderer.send('settings:toggleIslandDevTools', enabled),
    toggleStartMenuDevTools: (enabled) => ipcRenderer.send('settings:toggleStartMenuDevTools', enabled),
    toggleQuickSettingsDevTools: (enabled) => ipcRenderer.send('settings:toggleQuickSettingsDevTools', enabled),
    toggleNotificationCenterDevTools: (enabled) => ipcRenderer.send('settings:toggleNotificationCenterDevTools', enabled),
    toggleSystemTrayDevTools: (enabled) => ipcRenderer.send('settings:toggleSystemTrayDevTools', enabled),
  },
}

contextBridge.exposeInMainWorld('electronAPI', electronAPI)