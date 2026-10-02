import { BrowserWindow, screen, ipcMain, app, shell, Tray, nativeImage } from 'electron'
import { join, dirname } from 'path'
import { readdir, readFile } from 'fs/promises'
import Store from 'electron-store'
import { fileURLToPath } from 'url'
import NativeServiceClient from './native-service-client.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const isDev = !app.isPackaged

// 菜单窗口与触发按钮之间的间距（统一配置）
const MENU_TRIGGER_GAP = 20

function getPopupPosition({
  taskbarWin,
  taskbarPosition,
  triggerX,
  triggerY,
  winWidth,
  winHeight,
  offsetX = 0,
  offsetY = 0,
  alignRight = false
}) {
  const [tx, ty] = taskbarWin.getPosition()
  const [, th] = taskbarWin.getSize()

  let x = tx
  let y = ty

  switch (taskbarPosition) {
    case 'bottom':
      x = alignRight 
        ? tx + triggerX - winWidth + offsetX 
        : tx + triggerX - winWidth / 2 + offsetX
      y = ty - winHeight + offsetY
      break

    case 'top':
      x = alignRight 
        ? tx + triggerX - winWidth + offsetX 
        : tx + triggerX - winWidth / 2 + offsetX
      y = ty + th + offsetY
      break

    case 'left':
      x = tx + th + offsetX
      y = alignRight 
        ? ty + triggerY - winHeight + offsetY 
        : ty + triggerY - winHeight / 2 + offsetY
      break

    case 'right':
      x = tx - winWidth + offsetX
      y = alignRight 
        ? ty + triggerY - winHeight + offsetY 
        : ty + triggerY - winHeight / 2 + offsetY
      break
  }

  return { x, y }
}

const nativeClient = new NativeServiceClient()

const store = new Store({
  defaults: {
    dynamicIsland: {
      enabled: false,
      position: 'top',
      style: 'rounded',
      showOnStartup: true,
      animationsEnabled: true,
      devToolsEnabled: false,
      windowX: null,
      windowY: null,
      musicControlEnabled: false,
      quickActions: []
    },
    taskbar: {
      enabled: false,
      position: 'bottom',
      style: 'user',
      autoHide: false,
      autoHideDelay: 5,
      hideOnLeave: true,
      showTime: true,
      blurRadius: 20,
      glassStrength: 0.8,
      iconColor: '#ffffff',
      barColor: 'rgba(0, 0, 0, 0.8)',
      cornerType: 'rounded',
      iconOpacity: 1,
      alignment: 'center',
      devToolsEnabled: false,
      opacity: 0.8,
      width: 900,
      height: 60,
      margin: 20,
      roundedDockGap: 6,
      roundedCenterGap: 12,
      roundedLeftGap: 8,
      roundedRightGap: 8
    }
  }
})

// 安全保存设置到 store，避免文件权限错误
const safeStoreSet = (key, value) => {
  try {
    store.set(key, value)
    return true
  } catch (error) {
    console.error('[WindowManager] Failed to save to store:', error.message)
    return false
  }
}

let preloadPath, rendererDist

if (isDev) {
  preloadPath = join(__dirname, 'preload', 'electron-preload.cjs')
  rendererDist = join(__dirname, '..', 'dist', 'spa')
} else {
  const appPath = app.getAppPath()
  const appDir = dirname(appPath)
  preloadPath = join(appDir, 'app.asar.unpacked', 'src-electron', 'electron-preload.js')
  rendererDist = join(appDir, 'app.asar', 'dist', 'spa')
}

function getDevServerUrl() {
  const args = process.argv.slice(2)
  let specifiedPort = null

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--port' && args[i + 1]) {
      specifiedPort = parseInt(args[i + 1])
      break
    } else if (args[i].startsWith('--port=')) {
      specifiedPort = parseInt(args[i].split('=')[1])
      break
    }
  }

  const possiblePorts = specifiedPort ? [specifiedPort] : [9528, 9527, 9090, 9000, 8080, 3000]

  const devServerUrl = process.env.APP_URL ||
                      process.env.VITE_DEV_SERVER_URL ||
                      process.env.DEV_SERVER_URL ||
                      process.env.LOCAL_DEV_SERVER_URL ||
                      process.env.QUASAR_DEV_SERVER_URL

  if (devServerUrl) {
    console.log('[WindowManager] Using dev server URL:', devServerUrl)
    return devServerUrl
  }

  console.log('[WindowManager] No dev server URL env var, trying ports:', possiblePorts)
  return `http://localhost:${possiblePorts[0]}`
}

class WindowManager {
  constructor() {
    this.windows = new Map()
    this.isQuitting = false
    this.islandSettings = store.get('dynamicIsland')
    this.pendingWindowPreview = null

    this.WINDOW_TYPES = {
      MAIN: 'main',
      SUSPENSION: 'suspension',
      DYNAMIC_ISLAND: 'dynamicIsland',
      TASKBAR: 'taskbar',
      SETTINGS: 'settings',
      NOTIFICATION: 'notification',
      START_MENU: 'startMenu',
      QUICK_SETTINGS: 'quickSettings',
      NOTIFICATION_CENTER: 'notificationCenter',
      WINDOW_PREVIEW: 'windowPreview'
    }
    
    const storedTaskbar = store.get('taskbar') || {}
    const autoHideDefault = storedTaskbar.autoHide === true || storedTaskbar.autoHide === false ? storedTaskbar.autoHide : false
    const autoHideDelayDefault = storedTaskbar.autoHideDelay != null ? storedTaskbar.autoHideDelay : 5
    const hideOnLeaveDefault = storedTaskbar.hideOnLeave === true || storedTaskbar.hideOnLeave === false ? storedTaskbar.hideOnLeave : true
    this.taskbarSettings = { 
      ...storedTaskbar,
      autoHide: autoHideDefault,
      autoHideDelay: autoHideDelayDefault,
      hideOnLeave: hideOnLeaveDefault,
      isHidden: false,
    }

    this.edgeWatchTimer = null     // 边缘检测定时器
    this.hideTimer = null          // 自动隐藏计时器
    this.isMouseInTaskbar = false  // 鼠标是否在任务栏内
    this.justHidden = false        // 刚刚隐藏任务栏标记
    this.taskbarTray = null        // 任务栏 Tray 实例
    this.trayBounds = null         // 托盘图标位置信息

    // 立即注册 taskbar:getSettings，确保在窗口创建前就可用
    ipcMain.handle('taskbar:getSettings', () => {
      console.log('[WindowManager] taskbar:getSettings called, returning:', this.taskbarSettings)
      return { ...this.taskbarSettings }
    })

    // 注意：window:isMaximized 已在 electron-main.js 中注册，避免重复
  }

  createMainWindow() {
    if (this.getWindow('main')) {
      return this.getWindow('main')
    }

    const mainWindow = new BrowserWindow({
      width: 1280,
      height: 800,
      minWidth: 900,
      minHeight: 600,
      autoHideMenuBar: true,
      frame: false,
      titleBarStyle: 'hidden',
      titleBarOverlay: false,
      backgroundColor: '#1a1a1a',
      show: false,
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    if (isDev) {
      const devServerUrl = getDevServerUrl()
      console.log('[WindowManager] Loading main window from:', devServerUrl)
      mainWindow.loadURL(devServerUrl)
      mainWindow.webContents.openDevTools({ mode: 'detach' })
    } else {
      mainWindow.loadFile(join(rendererDist, 'index.html'))
    }

    mainWindow.webContents.on('did-finish-load', () => {
      mainWindow.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-main')
      `)
    })

    mainWindow.once('ready-to-show', () => {
      mainWindow.show()
    })

    mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
      console.error('Failed to load:', errorCode, errorDescription)
    })

    mainWindow.webContents.on('crashed', () => {
      console.error('Renderer process crashed')
    })

    mainWindow.on('maximize', () => {
      mainWindow?.webContents.send('window:maximized')
    })

    mainWindow.on('unmaximize', () => {
      mainWindow?.webContents.send('window:unmaximized')
    })

    this.setupWindowEvents(mainWindow, 'main')
    this.windows.set('main', mainWindow)

    return mainWindow
  }

  createSuspensionWindow() {
    if (this.getWindow('suspension')) {
      return this.getWindow('suspension')
    }

    const primaryDisplay = screen.getPrimaryDisplay()
    const { width, height } = primaryDisplay.workAreaSize

    const suspensionWindow = new BrowserWindow({
      width: 180,
      height: 60,
      minWidth: 160,
      minHeight: 50,
      autoHideMenuBar: true,
      frame: false,
      transparent: true,
      alwaysOnTop: true,
      x: width - 180,
      y: height - 60,
      skipTaskbar: true,
      resizable: false,
      titleBarStyle: 'hidden',
      movable: true,
      fullscreenable: false,
      closable: false,
      hasShadow: false,
      ...(process.platform !== 'darwin' ? { titleBarOverlay: true } : {}),
      backgroundColor: '#faf8f5',
      show: false,
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    suspensionWindow.loadFile(join(__dirname, '../dist/suspension.html'))
    suspensionWindow.setIgnoreMouseEvents(false)

    this.setupWindowEvents(suspensionWindow, 'suspension')
    this.windows.set('suspension', suspensionWindow)

    return suspensionWindow
  }

  createDynamicIslandWindow() {
    if (this.getWindow('dynamicIsland')) {
      return this.getWindow('dynamicIsland')
    }
    // 灵动岛跟随鼠标
    const cursorPoint = screen.getCursorScreenPoint()
    const currentDisplay = screen.getDisplayNearestPoint(cursorPoint)
    // 获取显示器可用工作区（包含准确的位置信息）
    const { x: workX, y: workY, width: workWidth, height: workHeight } = currentDisplay.workArea
    // 按比例动态计算窗口大小
    let stageWidth = Math.floor(workWidth * 0.12)
    let stageHeight = Math.floor(workHeight * 0.18)
    stageWidth = Math.max(stageWidth, 300)
    stageHeight = Math.max(stageHeight, 200)

    let idealX = Math.floor(workX + (workWidth - stageWidth) / 2)
    let idealY = workY + 20

    // 2. 读取保存位置
    const savedX = this.islandSettings.windowX
    const savedY = this.islandSettings.windowY

    let finalX = idealX
    let finalY = idealY

    // 只有当 savedX 是数字，且在屏幕范围内，并且不是贴边（防止 0,0 这种错误值）时才使用
    // 我们允许 20px 的误差范围，如果 savedX 离屏幕左边缘太近（小于 20px），说明可能是 bug 导致的，直接丢弃
    const isValidX = typeof savedX === 'number' && 
                     savedX >= workX && 
                     savedX < (workX + workWidth) &&
                     savedX > (workX + 20); // 关键：强制要求 X 坐标必须大于屏幕起点 20px

    if (isValidX) {
        finalX = savedX
        if (typeof savedY === 'number') {
            finalY = savedY
        }
    }

    console.log('最终位置:', finalX, finalY)

    const dynamicIslandWindow = new BrowserWindow({
      width: stageWidth,
      height: stageHeight,
      autoHideMenuBar: true,
      frame: false,
      transparent: true,
      alwaysOnTop: true,
      x: finalX,
      y: finalY,
      skipTaskbar: true,
      resizable: false,
      titleBarStyle: 'hidden',
      movable: true,
      fullscreenable: false,
      closable: false,
      hasShadow: false,
      backgroundColor: '#00000000',
      show: false,
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    const indexPath = isDev
      ? getDevServerUrl() + '/#/dynamic-island?window=dynamicIsland'
      : join(rendererDist, 'index.html')

    console.log('[WindowManager] Loading dynamic island from:', indexPath)
    
    if (isDev) {
      dynamicIslandWindow.loadURL(indexPath)
    } else {
      dynamicIslandWindow.loadFile(indexPath, { hash: '/dynamic-island?window=dynamicIsland' })
    }

    dynamicIslandWindow.webContents.on('did-finish-load', () => {
      dynamicIslandWindow.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-dynamic-island', 'window-menu')
      `)
    })

    dynamicIslandWindow.setIgnoreMouseEvents(true, { forward: true })

    this.setupDynamicIslandEvents(dynamicIslandWindow)
    this.setupWindowEvents(dynamicIslandWindow, 'dynamicIsland')
    this.windows.set('dynamicIsland', dynamicIslandWindow)

    // 只有在启用时才显示窗口
    if (this.islandSettings.enabled) {
      dynamicIslandWindow.show()
      console.log('[WindowManager] Dynamic island window shown')
    } else {
      console.log('[WindowManager] Dynamic island is disabled, not showing window')
    }

    return dynamicIslandWindow
  }

  // 边缘检测
  startEdgeWatcher() {
    if (this.edgeWatchTimer) return

    this.edgeWatchTimer = setInterval(() => {
      if (!this.taskbarSettings.autoHide) return

      const win = this.getWindow('taskbar')
      if (!win || win.isDestroyed()) return

      // 如果刚刚隐藏了任务栏，暂时不做边缘检测显示
      if (this.justHidden) {
        return
      }

      const { x, y } = screen.getCursorScreenPoint()
      const { width, height } = screen.getPrimaryDisplay().workAreaSize

      const THRESHOLD = 5 // 离边缘 5px 就唤醒
      const pos = this.taskbarSettings.position

      let shouldShow = false

      switch (pos) {
        case 'top':
          shouldShow = y <= THRESHOLD
          break
        case 'bottom':
          shouldShow = y >= height - THRESHOLD
          break
        case 'left':
          shouldShow = x <= THRESHOLD
          break
        case 'right':
          shouldShow = x >= width - THRESHOLD
          break
      }

      if (shouldShow && this.taskbarSettings.isHidden) {
        this.showTaskbar()
      }
    }, 200)
  }

  // 任务栏比例计算
  getTaskbarSize(position, style, workArea) {
    const { width, height } = workArea

    // 横向（top / bottom）
    if (position === 'top' || position === 'bottom') {
      let ratio = 0.05 // 默认 4.5%

      if (style === 'macos') ratio = 0.035
      if (style === 'user') ratio = 0.055
      if (style === 'linux') ratio = 0.04

      const calculated = Math.round(height * ratio)

      return {
        width: width,
        height: Math.max(36, Math.min(72, calculated))
      }
    }

    // 纵向（left / right）
    if (position === 'left' || position === 'right') {
      let ratio = 0.05 // 默认 5%

      if (style === 'linux') ratio = 0.04
      if (style === 'user') ratio = 0.06

      const calculated = Math.round(width * ratio)

      return {
        width: Math.max(48, Math.min(80, calculated)),
        height: height
      }
    }

    return { width, height }
  }

  createTaskbarWindow() {
    if (this.getWindow('taskbar')) {
      return this.getWindow('taskbar')
    }
    
    console.log('[WindowManager] Taskbar settings:', JSON.stringify(this.taskbarSettings))

    const primaryDisplay = screen.getPrimaryDisplay()
    const workArea = primaryDisplay.workAreaSize

    const position = this.taskbarSettings.position
    const style = this.taskbarSettings.style

    const { width, height } = this.getTaskbarSize(position, style, workArea)

    let xPos = 0
    let yPos = 0

    switch (position) {
      case 'top':
        xPos = 0
        yPos = 0
        break
      case 'bottom':
        xPos = 0
        yPos = workArea.height - height
        break
      case 'left':
        xPos = 0
        yPos = 0
        break
      case 'right':
        xPos = workArea.width - width
        yPos = 0
        break
    }

    console.log('[WindowManager] Creating BrowserWindow for taskbar...')
    const taskbarWindow = new BrowserWindow({
      width: width,
      height: height,
      autoHideMenuBar: true,
      frame: false,
      transparent: true,
      alwaysOnTop: true,
      x: xPos,
      y: yPos,
      skipTaskbar: true,
      resizable: false,
      titleBarStyle: 'hidden',
      movable: false,
      fullscreenable: false,
      closable: false,
      hasShadow: false,
      backgroundColor: '#00000000',
      show: false,
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })
    console.log('[WindowManager] BrowserWindow created, id:', taskbarWindow.id)

    const indexPath = isDev
      ? getDevServerUrl() + '/#/taskbar?window=taskbar'
      : join(rendererDist, 'index.html')

    console.log('[WindowManager] Loading taskbar from:', indexPath)
    
    if (isDev) {
      taskbarWindow.loadURL(indexPath)
    } else {
      taskbarWindow.loadFile(indexPath, { hash: '/taskbar?window=taskbar' })
    }
    console.log('[WindowManager] Taskbar URL loaded')

    taskbarWindow.webContents.on('did-finish-load', () => {
      taskbarWindow.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-taskbar')
      `)
    })

    taskbarWindow.setIgnoreMouseEvents(false)
    console.log('[WindowManager] Mouse events configured - CSS handles click-through')

    try {
      console.log('[WindowManager] About to call setupTaskbarEvents')
      this.setupTaskbarEvents(taskbarWindow)
      console.log('[WindowManager] Taskbar events setup complete')
    } catch (error) {
      console.error('[WindowManager] Error setting up taskbar events:', error)
      console.error('[WindowManager] Error stack:', error.stack)
    }
    
    this.setupWindowEvents(taskbarWindow, 'taskbar')
    console.log('[WindowManager] Window events setup complete')
    
    this.windows.set('taskbar', taskbarWindow)
    console.log('[WindowManager] Taskbar window stored in windows map, current windows:', Array.from(this.windows.keys()))

    // 只有在启用时才显示窗口
    if (this.taskbarSettings.enabled) {
      taskbarWindow.show()
      console.log('[WindowManager] Taskbar window shown')
      // 如果启用了自动隐藏，启动隐藏计时器
      if (this.taskbarSettings.autoHide && !this.isMouseInTaskbar) {
        this.startHideTimer()
      }
    }

    // 边缘检测
    this.startEdgeWatcher()

    return taskbarWindow
  }

  setupTaskbarEvents(win) {
    try {
      ipcMain.on('taskbar:setPosition', (event, position) => {
      this.taskbarSettings.position = position
      
      const primaryDisplay = screen.getPrimaryDisplay()
      const { width, height } = primaryDisplay.workAreaSize

      let taskbarWidth, taskbarHeight, xPos, yPos

      const style = this.taskbarSettings.style

      if (position === 'top') {
        taskbarWidth = width
        taskbarHeight = style === 'macos' ? 28 : 40
        xPos = 0
        yPos = 0
      } else if (position === 'bottom') {
        taskbarWidth = width
        taskbarHeight = style === 'user' ? 60 : 40
        xPos = 0
        yPos = height - taskbarHeight
      } else if (position === 'left') {
        taskbarWidth = style === 'linux' ? 48 : 60
        taskbarHeight = height
        xPos = 0
        yPos = 0
      } else { // right
        taskbarWidth = style === 'linux' ? 48 : 60
        taskbarHeight = height
        xPos = width - taskbarWidth
        yPos = 0
      }

      win.setSize(taskbarWidth, taskbarHeight)
      win.setPosition(xPos, yPos)
      win.webContents.send('taskbar:positionUpdated', position)
    })

    ipcMain.on('taskbar:setStyle', (event, style) => {
      this.taskbarSettings.style = style
      win.webContents.send('taskbar:styleUpdated', style)

      const primaryDisplay = screen.getPrimaryDisplay()
      const { width, height } = primaryDisplay.workAreaSize

      let taskbarWidth, taskbarHeight, xPos, yPos
      const position = this.taskbarSettings.position

      if (position === 'top') {
        taskbarWidth = width
        taskbarHeight = style === 'macos' ? 28 : 40
        xPos = 0
        yPos = 0
      } else if (position === 'bottom') {
        taskbarWidth = width
        taskbarHeight = style === 'user' ? 60 : 40
        xPos = 0
        yPos = height - taskbarHeight
      } else if (position === 'left') {
        taskbarWidth = style === 'linux' ? 48 : 60
        taskbarHeight = height
        xPos = 0
        yPos = 0
      } else { // right
        taskbarWidth = style === 'linux' ? 48 : 60
        taskbarHeight = height
        xPos = width - taskbarWidth
        yPos = 0
      }

      win.setSize(taskbarWidth, taskbarHeight)
      win.setPosition(xPos, yPos)
    })

    ipcMain.on('taskbar:setEnabled', (event, enabled) => {
      console.log('[WindowManager] taskbar:setEnabled called with:', enabled)
      this.taskbarSettings.enabled = enabled
      safeStoreSet('taskbar', this.taskbarSettings)
      if (enabled) {
        win.show()
      } else {
        win.hide()
      }
      // 更新托盘菜单
      ipcMain.emit('tray:updateTaskbarEnabled', null, enabled)
    })

    ipcMain.on('taskbar:launchApp', (event, appPath) => {
      console.log('[WindowManager] taskbar:launchApp called with:', appPath)
      try {
        // 使用 shell.openPath 启动应用
        shell.openPath(appPath).then(() => {
          console.log('[WindowManager] Application launched successfully:', appPath)
        }).catch((error) => {
          console.error('[WindowManager] Failed to launch application:', error)
        })
      } catch (error) {
        console.error('[WindowManager] Error launching application:', error)
      }
    })

    // 动态切换鼠标事件穿透
    ipcMain.on('taskbar:setIgnoreMouseEvents', (event, ignore) => {
      const taskbarWin = this.getWindow('taskbar')
      if (taskbarWin && !taskbarWin.isDestroyed()) {
        if (ignore) {
          taskbarWin.setIgnoreMouseEvents(true, { forward: true })
        } else {
          taskbarWin.setIgnoreMouseEvents(false)
        }
      }
    })

    ipcMain.handle('taskbar:getPinnedApps', async () => {
      console.log('[WindowManager] taskbar:getPinnedApps called')
      try {
        const pinnedApps = await this.getPinnedApplications()
        console.log('[WindowManager] Found pinned apps:', pinnedApps)
        return pinnedApps
      } catch (error) {
        console.error('[WindowManager] Error getting pinned apps:', error)
        return []
      }
    })

    // 获取活动窗口列表
    ipcMain.handle('taskbar:getActiveWindows', async () => {
      console.log('[WindowManager] taskbar:getActiveWindows called')
      try {
        const activeWindows = await nativeClient.getActiveWindows()
        console.log('[WindowManager] Found active windows:', activeWindows.length)
        return activeWindows
      } catch (error) {
        console.error('[WindowManager] Error getting active windows:', error)
        return []
      }
    })

    // 激活窗口
    ipcMain.handle('taskbar:activateWindow', async (event, hwnd) => {
      console.log('[WindowManager] taskbar:activateWindow called with handle:', hwnd)
      try {
        const result = await nativeClient.activateWindow(hwnd)
        console.log('[WindowManager] Window activation result:', result)
        return result
      } catch (error) {
        console.error('[WindowManager] Error activating window:', error)
        return false
      }
    })

    // 关闭窗口
    ipcMain.handle('taskbar:closeWindow', async (event, hwnd) => {
      console.log('[WindowManager] taskbar:closeWindow called with handle:', hwnd)
      try {
        const result = await nativeClient.closeWindow(hwnd)
        console.log('[WindowManager] Window close result:', result)
        return result
      } catch (error) {
        console.error('[WindowManager] Error closing window:', error)
        return false
      }
    })

    ipcMain.handle('taskbar:setDevToolsEnabled', (event, enabled) => {
      console.log('[WindowManager] taskbar:setDevToolsEnabled called with:', enabled)
      console.log('[WindowManager] win:', win?.id)
      console.log('[WindowManager] win.webContents:', win?.webContents)
      
      this.taskbarSettings.devToolsEnabled = enabled
      safeStoreSet('taskbar', this.taskbarSettings)
      
      try {
        if (enabled) {
          console.log('[WindowManager] Opening dev tools...')
          win.webContents.openDevTools({ mode: 'detach' })
          console.log('[WindowManager] Dev tools opened successfully')
        } else {
          console.log('[WindowManager] Closing dev tools...')
          win.webContents.closeDevTools()
          console.log('[WindowManager] Dev tools closed successfully')
        }
      } catch (error) {
        console.error('[WindowManager] Error opening/closing dev tools:', error)
      }
      
      return true
    })

    ipcMain.on('taskbar:openDevTools', () => {
      if (win && !win.isDestroyed()) {
        win.webContents.openDevTools({ mode: 'detach' })
      }
    })

    ipcMain.on('taskbar:updateSize', (event, size) => {
      if (size.width !== undefined) {
        this.taskbarSettings.width = size.width
      }
      if (size.height !== undefined) {
        this.taskbarSettings.height = size.height
      }
      if (size.margin !== undefined) {
        this.taskbarSettings.margin = size.margin
      }
      safeStoreSet('taskbar', this.taskbarSettings)
      win.setSize(this.taskbarSettings.width, this.taskbarSettings.height)
      win.webContents.send('taskbar:sizeUpdated', this.taskbarSettings)
    })

    ipcMain.on('taskbar:setOpacity', (event, opacity) => {
      this.taskbarSettings.opacity = opacity
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:opacityUpdated', opacity)
    })

    ipcMain.on('taskbar:setIconOpacity', (event, opacity) => {
      this.taskbarSettings.iconOpacity = opacity
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:iconOpacityUpdated', opacity)
    })

    ipcMain.on('taskbar:setBlurRadius', (event, radius) => {
      this.taskbarSettings.blurRadius = radius
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:blurRadiusUpdated', radius)
    })

    ipcMain.on('taskbar:setGlassStrength', (event, strength) => {
      this.taskbarSettings.glassStrength = strength
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:glassStrengthUpdated', strength)
    })

    ipcMain.on('taskbar:setIconColor', (event, color) => {
      this.taskbarSettings.iconColor = color
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:iconColorUpdated', color)
    })

    ipcMain.on('taskbar:setBarColor', (event, color) => {
      this.taskbarSettings.barColor = color
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:barColorUpdated', color)
    })

    ipcMain.on('taskbar:setCornerType', (event, type) => {
      this.taskbarSettings.cornerType = type
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:cornerTypeUpdated', type)
    })

    ipcMain.on('taskbar:setRoundedDockGap', (event, gap) => {
      this.taskbarSettings.roundedDockGap = gap
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:roundedDockGapUpdated', gap)
    })

    ipcMain.on('taskbar:setRoundedCenterGap', (event, gap) => {
      this.taskbarSettings.roundedCenterGap = gap
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:roundedCenterGapUpdated', gap)
    })

    ipcMain.on('taskbar:setRoundedLeftGap', (event, gap) => {
      this.taskbarSettings.roundedLeftGap = gap
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:roundedLeftGapUpdated', gap)
    })

    ipcMain.on('taskbar:setRoundedRightGap', (event, gap) => {
      this.taskbarSettings.roundedRightGap = gap
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:roundedRightGapUpdated', gap)
    })

    ipcMain.on('taskbar:setIgnoreMouseEvents', (event, ignore) => {
      win.setIgnoreMouseEvents(ignore)
    })

    ipcMain.on('taskbar:setAlignment', (event, alignment) => {
      this.taskbarSettings.alignment = alignment
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:alignmentUpdated', alignment)
    })

    ipcMain.on('taskbar:mouseEnter', () => {
      this.isMouseInTaskbar = true
      this.justHidden = false // 鼠标进入任务栏，清除刚刚隐藏的标记
      if (this.hideTimer) {
        clearTimeout(this.hideTimer)
        this.hideTimer = null
      }
      if (this.taskbarSettings.autoHide && this.taskbarSettings.isHidden) {
        this.showTaskbar()
      }
    })

    ipcMain.on('taskbar:mouseLeave', () => {
      this.isMouseInTaskbar = false
      if (this.taskbarSettings.autoHide) {
        if (this.taskbarSettings.hideOnLeave) {
          // 清除边缘检测定时器，防止在动画完成前重新显示
          if (this.edgeWatchTimer) {
            clearInterval(this.edgeWatchTimer)
            this.edgeWatchTimer = null
          }
          this.hideTaskbar()
          // 重新启动边缘检测
          this.startEdgeWatcher()
        } else {
          this.startHideTimer()
        }
      }
    })

    ipcMain.on('taskbar:setAutoHide', (event, autoHide) => {
      this.taskbarSettings.autoHide = autoHide
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:autoHideUpdated', autoHide)
      ipcMain.emit('taskbar:updateSettings', null, { autoHide })
      if (!autoHide && this.taskbarSettings.isHidden) {
        this.showTaskbar()
      }
      if (autoHide && !this.isMouseInTaskbar) {
        this.startHideTimer()
      }
    })

    ipcMain.on('taskbar:setAutoHideDelay', (event, delay) => {
      this.taskbarSettings.autoHideDelay = delay
      safeStoreSet('taskbar', this.taskbarSettings)
      if (this.hideTimer && this.taskbarSettings.autoHide && !this.taskbarSettings.hideOnLeave) {
        this.startHideTimer()
      }
    })

    ipcMain.on('taskbar:setHideOnLeave', (event, hideOnLeave) => {
      this.taskbarSettings.hideOnLeave = hideOnLeave
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:hideOnLeaveUpdated', hideOnLeave)
      if (hideOnLeave && !this.isMouseInTaskbar && this.taskbarSettings.autoHide) {
        this.hideTaskbar()
      } else if (!hideOnLeave && this.taskbarSettings.autoHide && !this.isMouseInTaskbar) {
        this.startHideTimer()
      }
    })

    ipcMain.on('taskbar:setShowTime', (event, showTime) => {
      this.taskbarSettings.showTime = showTime
      safeStoreSet('taskbar', this.taskbarSettings)
      win.webContents.send('taskbar:showTimeUpdated', showTime)
    })
    } catch (error) {
      console.error('[WindowManager] Error registering taskbar events:', error)
      console.error('[WindowManager] Error stack:', error.stack)
    }

    // 开始菜单相关 IPC
    ipcMain.on('taskbar:toggleStartMenu', () => {
      this.toggleStartMenu()
    })

    ipcMain.on('taskbar:closeStartMenu', () => {
      this.closeStartMenu()
    })

    ipcMain.on('taskbar:showStartMenuAt', (event, x, y) => {
      this.showStartMenuAt(x, y)
    })

    // 快捷设置相关 IPC
    ipcMain.on('taskbar:toggleQuickSettings', () => {
      this.toggleQuickSettings()
    })

    ipcMain.on('taskbar:closeQuickSettings', () => {
      this.closeQuickSettings()
    })

    ipcMain.on('taskbar:showQuickSettingsAt', (event, x, y) => {
      this.showQuickSettingsAt(x, y)
    })

    // 通知中心相关 IPC
    ipcMain.on('taskbar:toggleNotificationCenter', () => {
      this.toggleNotificationCenter()
    })

    ipcMain.on('taskbar:closeNotificationCenter', () => {
      this.closeNotificationCenter()
    })

    ipcMain.on('taskbar:showNotificationCenterAt', (event, x, y) => {
      this.showNotificationCenterAt(x, y)
    })

    // 系统托盘相关 IPC
    ipcMain.on('taskbar:showSystemTrayAt', (event, x, y) => {
      console.log('[WindowManager] IPC received taskbar:showSystemTrayAt - x:', x, 'y:', y, 'typeof x:', typeof x, 'typeof y:', typeof y)
      this.showSystemTrayAt(x, y)
    })

    ipcMain.on('taskbar:closeSystemTray', () => {
      this.closeSystemTray()
    })

    // 窗口预览相关 IPC
    ipcMain.on('taskbar:showWindowPreviewAt', (event, x, y, taskbarPosition, app) => {
      this.showWindowPreviewAt(x, y, taskbarPosition, app)
    })

    ipcMain.on('taskbar:closeWindowPreview', () => {
      this.closeWindowPreview()
    })

    ipcMain.on('taskbar:refreshWindowPreview', () => {
      this.refreshWindowPreview()
    })

    // 开发者工具设置相关 IPC
    ipcMain.on('settings:toggleMainWindowDevTools', (event, enabled) => {
      const mainWindow = this.getWindow('main')
      if (mainWindow && !mainWindow.isDestroyed()) {
        if (enabled) {
          mainWindow.webContents.openDevTools({ mode: 'detach' })
        } else {
          mainWindow.webContents.closeDevTools()
        }
      }
    })

    ipcMain.on('settings:toggleTaskbarDevTools', (event, enabled) => {
      const taskbarWin = this.getWindow('taskbar')
      if (taskbarWin && !taskbarWin.isDestroyed()) {
        if (enabled) {
          taskbarWin.webContents.openDevTools({ mode: 'detach' })
        } else {
          taskbarWin.webContents.closeDevTools()
        }
      }
    })

    ipcMain.on('settings:toggleIslandDevTools', (event, enabled) => {
      const islandWin = this.getWindow('island')
      if (islandWin && !islandWin.isDestroyed()) {
        if (enabled) {
          islandWin.webContents.openDevTools({ mode: 'detach' })
        } else {
          islandWin.webContents.closeDevTools()
        }
      }
    })

    ipcMain.on('settings:toggleStartMenuDevTools', (event, enabled) => {
      const startMenuWin = this.getWindow('startMenu')
      if (startMenuWin && !startMenuWin.isDestroyed()) {
        if (enabled) {
          startMenuWin.webContents.openDevTools({ mode: 'detach' })
        } else {
          startMenuWin.webContents.closeDevTools()
        }
      }
    })

    ipcMain.on('settings:toggleQuickSettingsDevTools', (event, enabled) => {
      const quickSettingsWin = this.getWindow('quickSettings')
      if (quickSettingsWin && !quickSettingsWin.isDestroyed()) {
        if (enabled) {
          quickSettingsWin.webContents.openDevTools({ mode: 'detach' })
        } else {
          quickSettingsWin.webContents.closeDevTools()
        }
      }
    })

    ipcMain.on('settings:toggleNotificationCenterDevTools', (event, enabled) => {
      const notificationCenterWin = this.getWindow('notificationCenter')
      if (notificationCenterWin && !notificationCenterWin.isDestroyed()) {
        if (enabled) {
          notificationCenterWin.webContents.openDevTools({ mode: 'detach' })
        } else {
          notificationCenterWin.webContents.closeDevTools()
        }
      }
    })

    ipcMain.on('settings:toggleSystemTrayDevTools', (event, enabled) => {
      const systemTrayWin = this.getWindow('systemTray')
      if (systemTrayWin && !systemTrayWin.isDestroyed()) {
        if (enabled) {
          systemTrayWin.webContents.openDevTools({ mode: 'detach' })
        } else {
          systemTrayWin.webContents.closeDevTools()
        }
      }
    })
  }

  // 创建系统托盘窗口
  createSystemTrayWindow() {
    if (this.getWindow('systemTray')) {
      return this.getWindow('systemTray')
    }

    const systemTrayWin = new BrowserWindow({
      width: 280,
      height: 200,
      frame: false,
      transparent: true,
      skipTaskbar: true,
      resizable: false,
      movable: false,
      show: false,
      focusable: true,
      backgroundColor: '#00000000',
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    const indexPath = isDev
      ? getDevServerUrl() + '/#/system-tray'
      : join(rendererDist, 'index.html')

    if (isDev) {
      systemTrayWin.loadURL(indexPath)
    } else {
      systemTrayWin.loadFile(indexPath, { hash: '/system-tray' })
    }

    systemTrayWin.webContents.on('did-finish-load', () => {
      systemTrayWin.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-system-tray', 'window-menu')
      `)
    })

    systemTrayWin.on('blur', () => {
      this.closeSystemTray()
    })

    this.windows.set('systemTray', systemTrayWin)
    return systemTrayWin
  }

  // 在指定位置显示系统托盘
  showSystemTrayAt(triggerX, triggerY) {
    console.log('[WindowManager] showSystemTrayAt called - triggerX:', triggerX, 'triggerY:', triggerY)
    
    // 验证参数
    if (isNaN(triggerX) || isNaN(triggerY)) {
      console.error('[WindowManager] Invalid parameters for showSystemTrayAt:', triggerX, triggerY)
      return
    }
    
    const systemTrayWin = this.createSystemTrayWindow()
    const [winWidth, winHeight] = systemTrayWin.getSize()
    const taskbarPosition = this.taskbarSettings.position

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay

    // 获取任务栏窗口的屏幕位置
    const taskbarWin = this.getWindow('taskbar')
    
    let x, y
    if (taskbarWin && !taskbarWin.isDestroyed()) {
      // 系统托盘按钮宽度为36，居中位置需要调整
      const adjustedTriggerX = triggerX - 18
      const pos = getPopupPosition({
        taskbarWin,
        taskbarPosition,
        adjustedTriggerX,
        triggerY,
        winWidth,
        winHeight
      })
      x = pos.x
      y = pos.y
    } else {
      x = workArea.x + 20
      y = workArea.y + 20
    }

    // 应用屏幕边界限制
    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    systemTrayWin.setPosition(Math.round(x), Math.round(y))
    systemTrayWin.show()
    systemTrayWin.focus()

    this.closeStartMenu()
    this.closeQuickSettings()
    this.closeNotificationCenter()
  }

  // 关闭系统托盘
  closeSystemTray() {
    const win = this.getWindow('systemTray')
    if (win && !win.isDestroyed()) {
      win.hide()
    }
  }

  // 创建开始菜单窗口
  createStartMenuWindow() {
    if (this.getWindow('startMenu')) {
      return this.getWindow('startMenu')
    }

    const startMenuWin = new BrowserWindow({
      width: 480,
      height: 640,
      frame: false,
      transparent: true,
      skipTaskbar: true,
      resizable: false,
      snap: false,
      movable: false,
      show: false,
      focusable: true,
      backgroundColor: '#00000000',
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    const indexPath = isDev
      ? getDevServerUrl() + '/#/start-menu'
      : join(rendererDist, 'index.html')

    if (isDev) {
      startMenuWin.loadURL(indexPath)
    } else {
      startMenuWin.loadFile(indexPath, { hash: '/start-menu' })
    }

    startMenuWin.webContents.on('did-finish-load', () => {
      startMenuWin.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-start-menu', 'window-menu')
      `)
    })

    startMenuWin.on('blur', () => {
      this.closeStartMenu()
    })

    this.windows.set('startMenu', startMenuWin)
    return startMenuWin
  }

  // 显示开始菜单
  showStartMenu() {
    const startMenuWin = this.createStartMenuWindow()
    const taskbarWin = this.getWindow('taskbar')

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay
    const [winWidth, winHeight] = startMenuWin.getSize()

    if (!taskbarWin || taskbarWin.isDestroyed()) {
      startMenuWin.setPosition(workArea.x + 20, workArea.y + 20)
      startMenuWin.show()
      startMenuWin.focus()
      return
    }

    const [tx, ty] = taskbarWin.getPosition()
    const [, th] = taskbarWin.getSize()

    const position = this.taskbarSettings.position
    let x = tx
    let y = ty

    switch (position) {
      case 'bottom':
        y = ty - winHeight
        break
      case 'top':
        y = ty + th
        break
      case 'left':
        x = tx + th
        break
      case 'right':
        x = tx - winWidth
        break
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    startMenuWin.setPosition(Math.round(x), Math.round(y))
    startMenuWin.show()
    startMenuWin.focus()

    this.closeQuickSettings()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  // 关闭开始菜单
  closeStartMenu() {
    const win = this.getWindow('startMenu')
    if (win && !win.isDestroyed()) {
      win.hide()
    }
  }

  // 切换开始菜单
  toggleStartMenu() {
    const win = this.getWindow('startMenu')
    if (win && !win.isDestroyed() && win.isVisible()) {
      this.closeStartMenu()
    } else {
      this.showStartMenu()
    }
  }

  // 在指定位置显示开始菜单（触发按钮上方）
  showStartMenuAt(triggerX, triggerY) {
    const startMenuWin = this.createStartMenuWindow()
    const [winWidth, winHeight] = startMenuWin.getSize()
    const taskbarPosition = this.taskbarSettings.position

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay

    // 获取任务栏窗口的屏幕位置
    const taskbarWin = this.getWindow('taskbar')
    // let screenX = triggerX
    // let screenY = triggerY
    
    // if (taskbarWin && !taskbarWin.isDestroyed()) {
    //   const [tx, ty] = taskbarWin.getPosition()
    //   // 将任务栏窗口内的相对坐标转换为屏幕坐标
    //   screenX = tx + triggerX
    //   screenY = ty + triggerY
    // }

    // let x = screenX - winWidth / 2 + 12
    // let y = screenY

    // // 菜单与触发按钮之间的间距
    // const gap = MENU_TRIGGER_GAP

    // switch (position) {
    //   case 'bottom':
    //     y = screenY - winHeight - gap
    //     break
    //   case 'top':
    //     y = screenY + 40 + gap
    //     break
    //   case 'left':
    //     x = screenX + 40 + gap
    //     y = screenY - winHeight / 2 + 12
    //     break
    //   case 'right':
    //     x = screenX - winWidth - 40 - gap
    //     y = screenY - winHeight / 2 + 12
    //     break
    //   default:
    //     y = screenY - winHeight - gap
    // }

    // x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    // y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))
    const { x, y } = getPopupPosition({
      taskbarWin,
      taskbarPosition,
      triggerX,
      triggerY,
      winWidth,
      winHeight,
      offsetX: 0,
      offsetY: 0
    })
    
    startMenuWin.setPosition(Math.round(x), Math.round(y))
    startMenuWin.show()
    startMenuWin.focus()

    this.closeQuickSettings()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  // 创建快捷设置窗口
  createQuickSettingsWindow() {
    if (this.getWindow('quickSettings')) {
      return this.getWindow('quickSettings')
    }

    const quickSettingsWin = new BrowserWindow({
      width: 360,
      height: 520,
      frame: false,
      transparent: true,
      skipTaskbar: true,
      resizable: false,
      movable: false,
      show: false,
      focusable: true,
      backgroundColor: '#00000000',
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    const indexPath = isDev
      ? getDevServerUrl() + '/#/quick-settings'
      : join(rendererDist, 'index.html')

    if (isDev) {
      quickSettingsWin.loadURL(indexPath)
    } else {
      quickSettingsWin.loadFile(indexPath, { hash: '/quick-settings' })
    }

    quickSettingsWin.webContents.on('did-finish-load', () => {
      document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
      document.body.classList.add('window-quick-settings', 'window-menu')
    })

    quickSettingsWin.on('blur', () => {
      this.closeQuickSettings()
    })

    this.windows.set('quickSettings', quickSettingsWin)
    return quickSettingsWin
  }

  // 显示快捷设置
  showQuickSettings() {
    const quickSettingsWin = this.createQuickSettingsWindow()
    const taskbarWin = this.getWindow('taskbar')

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay
    const [winWidth, winHeight] = quickSettingsWin.getSize()

    let x = workArea.x + workArea.width - winWidth - 20
    let y = workArea.y + workArea.height - winHeight - 20

    if (taskbarWin && !taskbarWin.isDestroyed()) {
      const [tx, ty] = taskbarWin.getPosition()
      const [, th] = taskbarWin.getSize()

      const position = this.taskbarSettings.position
      if (position === 'bottom') {
        y = ty - winHeight - 10
      } else if (position === 'top') {
        y = ty + th + 10
      }
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    quickSettingsWin.setPosition(Math.round(x), Math.round(y))
    quickSettingsWin.show()
    quickSettingsWin.focus()

    this.closeStartMenu()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  // 关闭快捷设置
  closeQuickSettings() {
    const win = this.getWindow('quickSettings')
    if (win && !win.isDestroyed()) {
      win.hide()
    }
  }

  // 切换快捷设置
  toggleQuickSettings() {
    const win = this.getWindow('quickSettings')
    if (win && !win.isDestroyed() && win.isVisible()) {
      this.closeQuickSettings()
    } else {
      this.showQuickSettings()
    }
  }

  // 在指定位置显示快捷设置（触发按钮上方）
  showQuickSettingsAt(triggerX, triggerY) {
    const quickSettingsWin = this.createQuickSettingsWindow()
    const [winWidth, winHeight] = quickSettingsWin.getSize()
    const taskbarPosition = this.taskbarSettings.position

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay

    // 获取任务栏窗口的屏幕位置
    const taskbarWin = this.getWindow('taskbar')
    
    let x, y
    if (taskbarWin && !taskbarWin.isDestroyed()) {
      const pos = getPopupPosition({
        taskbarWin,
        taskbarPosition,
        triggerX,
        triggerY,
        winWidth,
        winHeight,
        offsetX: 15
      })
      x = pos.x
      y = pos.y
    } else {
      x = workArea.x + 20
      y = workArea.y + 20
    }

    // 应用屏幕边界限制
    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    quickSettingsWin.setPosition(Math.round(x), Math.round(y))
    quickSettingsWin.show()
    quickSettingsWin.focus()

    this.closeStartMenu()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  // 创建通知中心窗口
  createNotificationCenterWindow() {
    if (this.getWindow('notificationCenter')) {
      return this.getWindow('notificationCenter')
    }

    const notificationCenterWin = new BrowserWindow({
      width: 360,
      height: 520,
      frame: false,
      transparent: true,
      skipTaskbar: true,
      resizable: false,
      movable: false,
      show: false,
      focusable: true,
      backgroundColor: '#00000000',
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    const indexPath = isDev
      ? getDevServerUrl() + '/#/notification-center'
      : join(rendererDist, 'index.html')

    if (isDev) {
      notificationCenterWin.loadURL(indexPath)
    } else {
      notificationCenterWin.loadFile(indexPath, { hash: '/notification-center' })
    }

    notificationCenterWin.webContents.on('did-finish-load', () => {
      notificationCenterWin.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-notification-center', 'window-menu')
      `)
    })

    notificationCenterWin.on('blur', () => {
      this.closeNotificationCenter()
    })

    this.windows.set('notificationCenter', notificationCenterWin)
    return notificationCenterWin
  }

  // 显示通知中心
  showNotificationCenter() {
    const notificationCenterWin = this.createNotificationCenterWindow()
    const taskbarWin = this.getWindow('taskbar')

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay
    const [winWidth, winHeight] = notificationCenterWin.getSize()

    let x = workArea.x + workArea.width - winWidth - 20
    let y = workArea.y + workArea.height - winHeight - 20

    if (taskbarWin && !taskbarWin.isDestroyed()) {
      const [tx, ty] = taskbarWin.getPosition()
      const [, th] = taskbarWin.getSize()

      const position = this.taskbarSettings.position
      if (position === 'bottom') {
        y = ty - winHeight - 10
      } else if (position === 'top') {
        y = ty + th + 10
      }
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    notificationCenterWin.setPosition(Math.round(x), Math.round(y))
    notificationCenterWin.show()
    notificationCenterWin.focus()

    this.closeStartMenu()
    this.closeQuickSettings()
    this.closeSystemTray()
  }

  // 关闭通知中心
  closeNotificationCenter() {
    const win = this.getWindow('notificationCenter')
    if (win && !win.isDestroyed()) {
      win.hide()
    }
  }

  // 创建窗口预览窗口
  createWindowPreviewWindow() {
    if (this.getWindow('windowPreview')) {
      return this.getWindow('windowPreview')
    }

    console.log('[WindowManager] Creating new windowPreview window')
    const windowPreviewWin = new BrowserWindow({
      width: 300,
      height: 400,
      frame: false,
      transparent: true,
      alwaysOnTop: true,
      skipTaskbar: true,
      resizable: false,
      show: false,
      focusable: false,
      backgroundColor: '#00000000',
      webPreferences: {
        preload: preloadPath,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false,
        webSecurity: false,
        devTools: true,
      },
    })

    const indexPath = isDev
      ? getDevServerUrl() + '/#/window-preview'
      : join(rendererDist, 'index.html')

    console.log('[WindowManager] Loading windowPreview from:', indexPath)

    if (isDev) {
      windowPreviewWin.loadURL(indexPath)
    } else {
      windowPreviewWin.loadFile(indexPath, { hash: '/window-preview' })
    }

    windowPreviewWin.webContents.on('did-finish-load', () => {
      console.log('[WindowManager] windowPreview did-finish-load')
      windowPreviewWin.webContents.executeJavaScript(`
        document.body.classList.remove('window-main', 'window-taskbar', 'window-start-menu', 'window-quick-settings', 'window-notification-center', 'window-system-tray', 'window-dynamic-island', 'window-menu')
        document.body.classList.add('window-menu')
      `)
      if (this.pendingWindowPreview) {
        console.log('[WindowManager] Sending pending window preview data')
        windowPreviewWin.webContents.send('window-preview:update', this.pendingWindowPreview)
        this.pendingWindowPreview = null
      }
    })

    windowPreviewWin.on('blur', () => {
      console.log('[WindowManager] windowPreview blur event')
      this.closeWindowPreview()
    })

    this.windows.set('windowPreview', windowPreviewWin)
    return windowPreviewWin
  }

  // 在指定位置显示窗口预览
  showWindowPreviewAt(triggerX, triggerY, taskbarPosition, app) {
    console.log('[WindowManager] showWindowPreviewAt called - triggerX:', triggerX, 'triggerY:', triggerY, 'taskbarPosition:', taskbarPosition, 'app:', app?.name)

    const previewWin = this.createWindowPreviewWindow()
    const [winWidth, winHeight] = previewWin.getSize()
    console.log('[WindowManager] previewWin size:', winWidth, 'x', winHeight)

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay
    console.log('[WindowManager] workArea:', workArea)
    console.log('[WindowManager] workArea dimensions:', workArea.x, workArea.y, workArea.width, workArea.height)

    // 获取任务栏窗口的屏幕位置
    const taskbarWin = this.getWindow('taskbar')
    let x, y

    if (!taskbarWin || taskbarWin.isDestroyed()) {
      // 如果任务栏窗口不存在，使用默认位置
      x = workArea.x + 20
      y = workArea.y + 20
    } else {
      // 使用公共方法计算弹出窗口位置
      const pos = getPopupPosition({
        taskbarWin,
        taskbarPosition,
        triggerX,
        triggerY,
        winWidth,
        winHeight
      })
      x = pos.x
      y = pos.y
      console.log('[WindowManager] Calculated position using getPopupPosition - x:', x, 'y:', y)
    }

    console.log('[WindowManager] Final position:', Math.round(x), Math.round(y))

    previewWin.setPosition(Math.round(x), Math.round(y))

    // 存储待发送的数据，在窗口ready后再发送
    this.pendingWindowPreview = { app }
    console.log('[WindowManager] Stored pending window preview data for app:', app?.name)

    // 检查窗口是否已经ready（loaded）
    if (previewWin.webContents.isLoading()) {
      console.log('[WindowManager] Window is still loading, will send data when ready')
    } else {
      // 窗口已经ready，直接发送数据
      console.log('[WindowManager] Window is ready, sending data now')
      previewWin.webContents.send('window-preview:update', { app })
      this.pendingWindowPreview = null
    }

    previewWin.show()
    console.log('[WindowManager] windowPreview shown')

    // 关闭其他菜单
    this.closeStartMenu()
    this.closeQuickSettings()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  // 关闭窗口预览
  closeWindowPreview() {
    const win = this.getWindow('windowPreview')
    if (win && !win.isDestroyed()) {
      win.hide()
    }
  }

  // 刷新窗口预览
  refreshWindowPreview() {
    const win = this.getWindow('windowPreview')
    if (win && !win.isDestroyed() && win.isVisible()) {
      win.webContents.send('window-preview:refresh')
    }
  }

  // 切换通知中心
  toggleNotificationCenter() {
    const win = this.getWindow('notificationCenter')
    if (win && !win.isDestroyed() && win.isVisible()) {
      this.closeNotificationCenter()
    } else {
      this.showNotificationCenter()
    }
  }

  // 在指定位置显示通知中心（触发按钮上方）
  showNotificationCenterAt(triggerX, triggerY) {
    const notificationCenterWin = this.createNotificationCenterWindow()
    const [winWidth, winHeight] = notificationCenterWin.getSize()
    const taskbarPosition = this.taskbarSettings.position

    const primaryDisplay = screen.getPrimaryDisplay()
    const { workArea } = primaryDisplay

    // 获取任务栏窗口的屏幕位置
    const taskbarWin = this.getWindow('taskbar')
    
    let x, y
    if (taskbarWin && !taskbarWin.isDestroyed()) {
      const pos = getPopupPosition({
        taskbarWin,
        taskbarPosition,
        triggerX,
        triggerY,
        winWidth,
        winHeight,
        offsetX: 60,
        alignRight: true
      })
      x = pos.x
      y = pos.y
    } else {
      x = workArea.x + 20
      y = workArea.y + 20
    }

    // 应用屏幕边界限制
    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    notificationCenterWin.setPosition(Math.round(x), Math.round(y))
    notificationCenterWin.show()
    notificationCenterWin.focus()

    this.closeStartMenu()
    this.closeQuickSettings()
    this.closeSystemTray()
  }

  animateTo(win, targetX, targetY, duration = 150) {
    if (!win || win.isDestroyed()) return

    const [startX, startY] = win.getPosition()
    const steps = 10
    const dx = (targetX - startX) / steps
    const dy = (targetY - startY) / steps
    let step = 0

    const timer = setInterval(() => {
      if (step >= steps) {
        win.setPosition(targetX, targetY)
        clearInterval(timer)
        return
      }
      win.setPosition(
        Math.round(startX + dx * step),
        Math.round(startY + dy * step)
      )
      step++
    }, duration / steps)
  }

  startHideTimer() {
    if (this.hideTimer) clearTimeout(this.hideTimer)
    
    this.hideTimer = setTimeout(() => {
      if (!this.isMouseInTaskbar && this.taskbarSettings.autoHide) {
        this.hideTaskbar()
      }
      this.hideTimer = null
    }, (this.taskbarSettings.autoHideDelay || 5) * 1000)
  }

  showTaskbar() {
    const win = this.getWindow('taskbar')
    if (!win || win.isDestroyed()) {
      return
    }

    const position = this.taskbarSettings.position
    const primaryDisplay = screen.getPrimaryDisplay()
    const { width, height } = primaryDisplay.workAreaSize
    const [w, h] = win.getSize()
    
    let targetY = 0
    let targetX = 0
    
    switch (position) {
      case 'top':
        targetY = 0
        break
      case 'bottom':
        targetY = height - h
        break
      case 'left':
        targetX = 0
        break
      case 'right':
        targetX = width - w
        break
    }
      
    win.show()
    this.animateTo(win, targetX, targetY)
    this.taskbarSettings.isHidden = false
  }

  hideTaskbar() {
    const win = this.getWindow('taskbar')
    if (!win || win.isDestroyed()) {
      return
    }

    const position = this.taskbarSettings.position
    const primaryDisplay = screen.getPrimaryDisplay()
    const { width, height } = primaryDisplay.workAreaSize
    const [w, h] = win.getSize()
    
    let targetY = 0
    let targetX = 0
    
    switch (position) {
      case 'top':
        targetY = -h
        break
      case 'bottom':
        targetY = height
        break
      case 'left':
        targetX = -w
        break
      case 'right':
        targetX = width
        break
    }
    
    this.animateTo(win, targetX, targetY)
    this.taskbarSettings.isHidden = true
    
    // 如果是通过 hideOnLeave 触发的隐藏，设置标记阻止边缘检测在短时间内重新显示
    if (this.taskbarSettings.hideOnLeave) {
      this.justHidden = true
      setTimeout(() => {
        this.justHidden = false
      }, 500) // 500ms 内不重新显示
    }
  }

  setupDynamicIslandEvents(win) {
    let isExpanded = false
    let isMinimal = false

    ipcMain.on('dynamicIsland:mouse-enter', () => {
      if (win && !win.isDestroyed()) {
        win.setIgnoreMouseEvents(false)
      }
    })

    ipcMain.on('dynamicIsland:mouse-leave', () => {
      if (win && !win.isDestroyed()) {
        win.setIgnoreMouseEvents(true, { forward: true })
      }
    })

    ipcMain.on('dynamicIsland:expand', () => {
      if (isExpanded) return
      isExpanded = true
      isMinimal = false
      win.webContents.send('dynamicIsland:expanded', true)
    })

    ipcMain.on('dynamicIsland:collapse', () => {
      if (!isExpanded) return
      isExpanded = false
      win.webContents.send('dynamicIsland:expanded', false)
    })

    ipcMain.on('dynamicIsland:toggle', () => {
      if (isExpanded) {
        win.webContents.send('dynamicIsland:collapse')
        isExpanded = false
        isMinimal = false
      } else {
        win.webContents.send('dynamicIsland:expand')
        isExpanded = true
        isMinimal = false
      }
    })

    ipcMain.on('dynamicIsland:minimize', () => {
      isMinimal = true
      win.webContents.send('dynamicIsland:update', { minimal: true })
    })

    ipcMain.on('dynamicIsland:restore', () => {
      isMinimal = false
      win.webContents.send('dynamicIsland:update', { minimal: false })
    })

    ipcMain.on('dynamicIsland:update', (event, data) => {
      if (win && !win.isDestroyed()) {
        win.webContents.send('dynamicIsland:update', data)
      }
    })

    ipcMain.on('dynamicIsland:show', () => {
      if (win && !win.isDestroyed()) {
        win.show()
      }
    })

    ipcMain.on('dynamicIsland:hide', () => {
      if (win && !win.isDestroyed()) {
        win.hide()
      }
    })

    ipcMain.on('dynamicIsland:openDevTools', () => {
      if (win && !win.isDestroyed()) {
        console.log('[WindowManager] Opening DevTools for dynamic island')
        win.webContents.openDevTools({ mode: 'detach' })
      }
    })

    ipcMain.on('dynamicIsland:closeDevTools', () => {
      if (win && !win.isDestroyed()) {
        console.log('[WindowManager] Closing DevTools for dynamic island')
        win.webContents.closeDevTools()
      }
    })

    ipcMain.on('dynamicIsland:drag-end', (event, { x, y }) => {
      const [currentX, currentY] = win.getPosition()
      const newX = currentX + x
      const newY = currentY + y
      win.setPosition(newX, newY)
      
      this.islandSettings.windowX = newX
      this.islandSettings.windowY = newY
      store.set('dynamicIsland', this.islandSettings)
    })

    ipcMain.on('dynamicIsland:setPosition', (event, position) => {
      this.islandSettings.position = position
      store.set('dynamicIsland', this.islandSettings)
      win.webContents.send('dynamicIsland:positionUpdated', position)
      
      const primaryDisplay = screen.getPrimaryDisplay()
      const { width, height } = primaryDisplay.workAreaSize
      const [currentWidth, currentHeight] = win.getSize()
      
      let x = 0
      let y = 0
      
      switch (position) {
        case 'top':
          x = Math.floor((width - currentWidth) / 2)
          y = 10
          break
        case 'bottom':
          x = Math.floor((width - currentWidth) / 2)
          y = height - currentHeight - 10
          break
        case 'left':
          x = 10
          y = Math.floor((height - currentHeight) / 2)
          break
        case 'right':
          x = width - currentWidth - 10
          y = Math.floor((height - currentHeight) / 2)
          break
        default:
          x = Math.floor((width - currentWidth) / 2)
          y = 10
      }
      
      this.islandSettings.windowX = x
      this.islandSettings.windowY = y
      store.set('dynamicIsland', this.islandSettings)
      
      win.setPosition(x, y)
    })

    ipcMain.on('dynamicIsland:setStyle', (event, style) => {
      this.islandSettings.style = style
      store.set('dynamicIsland', this.islandSettings)
      win.webContents.send('dynamicIsland:styleUpdated', style)
    })

    // 注意：以下处理器已在 electron-main.js 中实现，避免重复注册
    // dynamicIsland:setEnabled, dynamicIsland:isEnabled, dynamicIsland:getSettings
    // dynamicIsland:setSettings, dynamicIsland:setMusicControlEnabled, dynamicIsland:setDevToolsEnabled

    ipcMain.on('dynamicIsland:updateStyle', (event, style) => {
      this.islandSettings.style = style
      store.set('dynamicIsland', this.islandSettings)
      win.webContents.send('dynamicIsland:styleUpdated', style)
    })

    ipcMain.on('dynamicIsland:updatePosition', (event, position) => {
      this.islandSettings.position = position
      store.set('dynamicIsland', this.islandSettings)
      win.webContents.send('dynamicIsland:positionUpdated', position)
    })

    ipcMain.on('dynamicIsland:addTask', (event, task) => {
      win.webContents.send('dynamicIsland:update', { task })
    })

    ipcMain.on('dynamicIsland:removeTask', (event, taskId) => {
      win.webContents.send('dynamicIsland:update', { removeTask: taskId })
    })

    ipcMain.on('suspension:toggle-main', () => {
      const mainWin = this.getWindow('main')
      if (mainWin) {
        if (mainWin.isVisible()) {
          mainWin.hide()
        } else {
          mainWin.show()
          mainWin.focus()
        }
      }
    })

    ipcMain.on('suspension:drag-end', (event, position) => {
      win.setPosition(position.x, position.y)
    })
  }

  createNotificationWindow(title, message) {
    const win = new BrowserWindow({
      width: 320,
      height: 100,
      frame: false,
      transparent: true,
      alwaysOnTop: true,
      skipTaskbar: true,
      resizable: false,
      show: false,
      webPreferences: {
        nodeIntegration: true,
        contextIsolation: false
      }
    })

    win.loadFile('notification.html', {
      query: { title, message }
    })

    const primaryDisplay = screen.getPrimaryDisplay()
    const { width } = primaryDisplay.workAreaSize

    win.setPosition(width - 340, 20)

    setTimeout(() => {
      if (win && !win.isDestroyed()) {
        win.close()
      }
    }, 3000)

    const id = `notification-${Date.now()}`
    this.windows.set(id, win)

    return win
  }

  setupSuspensionWindowEvents(win) {
    let isMouseOver = false
    let hideTimeout

    win.on('show', () => {
      win.webContents.executeJavaScript(`
        document.addEventListener('mouseenter', () => {
          window.suspensionMouseOver = true
        })
        document.addEventListener('mouseleave', () => {
          window.suspensionMouseOver = false
        })
      `)
    })

    ipcMain.on('suspension:toggle-main', () => {
      const mainWin = this.getWindow('main')
      if (mainWin) {
        if (mainWin.isVisible()) {
          mainWin.hide()
        } else {
          mainWin.show()
          mainWin.focus()
        }
      }
    })

    ipcMain.on('suspension:drag-end', (event, position) => {
      win.setPosition(position.x, position.y)
    })
  }

  setupWindowEvents(win, type) {
    win.on('closed', () => {
      this.windows.delete(type)
    })

    if (type === 'main') {
      win.on('close', (event) => {
        if (!this.isQuitting) {
          event.preventDefault()
          win.hide()
          this.createNotificationWindow('应用已最小化', '程序仍在后台运行')
        }
      })
    }
  }

  getWindow(id) {
    return this.windows.get(id)
  }

  showAll() {
    for (const win of this.windows.values()) {
      if (win && !win.isDestroyed()) {
        win.show()
      }
    }
  }

  hideAllExceptSuspension() {
    for (const [id, win] of this.windows) {
      if (id !== 'suspension' && id !== 'dynamicIsland' && win && !win.isDestroyed()) {
        win.hide()
      }
    }
  }

  closeAll() {
    for (const [id, win] of this.windows) {
      if (win && !win.isDestroyed()) {
        win.close()
      }
    }
    this.windows.clear()
  }

  setQuitting(value) {
    this.isQuitting = value
  }

  // 获取系统任务栏固定的应用
  async getPinnedApplications() {
    console.log('[WindowManager] getPinnedApplications called');
    try {
      // 优先使用C++层获取固定应用（包含图标）
      try {
        console.log('[WindowManager] Attempting to get pinned apps from native service');
        console.log('[WindowManager] nativeClient exists:', !!nativeClient);
        console.log('[WindowManager] nativeClient.getPinnedApps exists:', typeof nativeClient?.getPinnedApps);
        const nativeApps = await nativeClient.getPinnedApps()
        console.log('[WindowManager] Native service returned:', nativeApps);
        console.log('[WindowManager] Native service returned type:', Array.isArray(nativeApps) ? 'array' : typeof nativeApps);
        if (nativeApps && nativeApps.length > 0) {
          console.log('[WindowManager] Got pinned apps from native service:', nativeApps.length)
          console.log('[WindowManager] First app:', nativeApps[0]);
          return nativeApps.map(app => ({
            id: app.id,
            name: app.name,
            path: app.path,
            icon: app.icon ? `data:image/png;base64,${app.icon}` : null
          }))
        } else {
          console.log('[WindowManager] Native service returned empty or null');
        }
      } catch (nativeError) {
        console.log('[WindowManager] Native service not available, falling back to PowerShell:', nativeError.message)
        console.log('[WindowManager] Native error stack:', nativeError.stack);
      }
      
      // 回退到PowerShell实现
      const pinnedApps = []
      
      // Windows 10/11 任务栏固定应用位置 - 多个可能的路径
      const possiblePaths = [
        // 标准任务栏固定路径
        join(process.env.APPDATA, 'Microsoft', 'Internet Explorer', 'Quick Launch', 'User Pinned', 'TaskBar'),
        // 备用路径
        join(process.env.APPDATA, 'Microsoft', 'Windows', 'TaskBar'),
        // 开始菜单程序目录
        join(process.env.APPDATA, 'Microsoft', 'Windows', 'Start Menu', 'Programs'),
        // 公共开始菜单
        join(process.env.PUBLIC, 'Desktop'),
        // 用户桌面
        join(process.env.USERPROFILE, 'Desktop')
      ]
      
      // 尝试从所有可能的路径读取
      for (const path of possiblePaths) {
        try {
          const files = await readdir(path)
          for (const file of files) {
            if (file.endsWith('.lnk')) {
              const fullPath = join(path, file)
              const appInfo = await this.parseLnkFile(fullPath)
              if (appInfo) {
                // 避免重复
                if (!pinnedApps.find(a => a.path === appInfo.path)) {
                  pinnedApps.push(appInfo)
                }
              }
            }
          }
        } catch (error) {
          // 路径不存在或无法访问，继续尝试下一个
        }
      }
      
      // 如果还是没有，返回默认应用列表
      if (pinnedApps.length === 0) {
        return this.getDefaultPinnedApps()
      }
      
      return pinnedApps.slice(0, 15) // 最多返回15个
    } catch (error) {
      console.error('[WindowManager] Error getting pinned applications:', error)
      return this.getDefaultPinnedApps()
    }
  }
  
  // 解析 .lnk 快捷方式文件（使用 PowerShell）
  async parseLnkFile(lnkPath) {
    try {
      // 使用 PowerShell 解析快捷方式目标路径和显示名称
      const { execFile } = await import('child_process')
      
      return new Promise((resolve) => {
        const command = `
          $shortcut = (New-Object -ComObject WScript.Shell).CreateShortcut('${lnkPath.replace(/\\/g, '\\\\')}')
          $shortcut.TargetPath + '|' + $shortcut.Description + '|' + $shortcut.FullName
        `
        
        execFile('powershell.exe', ['-Command', command], (error, stdout, stderr) => {
          if (error) {
            console.log('[WindowManager] PowerShell error parsing lnk:', lnkPath, error.message)
            resolve(null)
            return
          }
          
          const output = stdout.trim()
          if (!output || output.length === 0) {
            resolve(null)
            return
          }
          
          // 解析输出：targetPath|description|fullName
          const parts = output.split('|')
          const targetPath = parts[0]?.trim() || ''
          const description = parts[1]?.trim() || ''
          const fullName = parts[2]?.trim() || ''
          
          if (!targetPath) {
            resolve(null)
            return
          }
          
          // 确定应用名称的优先级：description > lnk文件名 > 目标文件名
          let appName = ''
          
          // 检测是否包含乱码字符（Unicode替换字符 U+FFFD）
          const hasGarbledChars = (str) => /\uFFFD/.test(str)
          
          // 尝试从 description 获取名称（排除乱码情况）
          if (description && description.length > 0 && description !== targetPath && !hasGarbledChars(description)) {
            appName = description
          } else {
            // 从 lnk 文件名获取（去掉 .lnk 后缀）
            const lnkFileName = lnkPath.split('\\').pop().split('/').pop().replace(/\.lnk$/i, '')
            if (lnkFileName && lnkFileName.length > 0) {
              appName = lnkFileName
            } else {
              // 从目标路径获取
              const fileName = targetPath.split('\\').pop() || targetPath.split('/').pop()
              appName = fileName.replace(/\.(exe|lnk|url|msi)$/i, '').trim()
            }
          }
          
          // 如果路径包含特殊协议（如 ms-settings:），直接使用
          if (targetPath.includes(':') && !targetPath.includes('\\')) {
            resolve({
              id: lnkPath,
              name: appName || this.getProtocolName(targetPath),
              path: targetPath,
              icon: null
            })
            return
          }
          
          if (appName && (targetPath.includes('.exe') || targetPath.includes('.lnk') || targetPath.includes('://'))) {
            resolve({
              id: lnkPath,
              name: appName,
              path: targetPath,
              icon: null
            })
          } else {
            resolve(null)
          }
        })
      })
    } catch (error) {
      console.log('[WindowManager] Error parsing lnk file:', lnkPath, error.message)
      return null
    }
  }
  
  // 获取协议名称
  getProtocolName(protocol) {
    const protocolNames = {
      'ms-settings:': '设置',
      'microsoft-edge:': 'Edge',
      'mailto:': '邮件',
      'tel:': '电话'
    }
    return protocolNames[protocol] || protocol.split(':')[0]
  }
  
  // 获取默认固定应用列表（当系统没有固定应用时使用）
  getDefaultPinnedApps() {
    return [
      { id: 'explorer', name: '文件资源管理器', path: 'explorer.exe', icon: null },
      { id: 'cmd', name: '命令提示符', path: 'cmd.exe', icon: null },
      { id: 'notepad', name: '记事本', path: 'notepad.exe', icon: null },
      { id: 'settings', name: '设置', path: 'ms-settings:', icon: null },
      { id: 'edge', name: 'Edge', path: 'microsoft-edge:', icon: null },
    ]
  }

  shouldShowIslandOnStartup() {
    return this.islandSettings.showOnStartup && this.islandSettings.enabled
  }

  createTaskbarTray() {
    if (this.taskbarTray) {
      return this.taskbarTray
    }

    const iconPath = join(__dirname, 'assets', 'tray-icon.png')
    let trayIcon = nativeImage.createFromPath(iconPath)

    if (trayIcon.isEmpty()) {
      trayIcon = nativeImage.createEmpty()
    }

    this.taskbarTray = new Tray(trayIcon)
    this.taskbarTray.setToolTip('TaskBar')

    this.taskbarTray.on('click', (event, bounds) => {
      this.trayBounds = bounds
      this.showStartMenuNearTray(bounds)
    })

    this.taskbarTray.on('right-click', (event, bounds) => {
      this.trayBounds = bounds
    })

    return this.taskbarTray
  }

  getTrayBounds() {
    if (this.taskbarTray) {
      return this.taskbarTray.getBounds()
    }
    return this.trayBounds
  }

  showMenuNearTray(menuWin, options = {}) {
    const {
      anchor = 'bottom',
      offsetX = 0,
      offsetY = 0
    } = options

    const trayBounds = this.getTrayBounds()
    if (!trayBounds) {
      menuWin.setPosition(20, 20)
      menuWin.show()
      return
    }

    const display = screen.getDisplayMatching(trayBounds)
    const { workArea } = display
    const [winWidth, winHeight] = menuWin.getSize()

    let x = trayBounds.x + offsetX
    let y = trayBounds.y + offsetY

    const isTop = trayBounds.y <= workArea.y + 10
    const isBottom = trayBounds.y + trayBounds.height >= workArea.y + workArea.height - 10
    const isLeft = trayBounds.x <= workArea.x + 10
    const isRight = trayBounds.x + trayBounds.width >= workArea.x + workArea.width - 10

    if (anchor === 'bottom') {
      if (isBottom) {
        y = trayBounds.y - winHeight
      } else {
        y = trayBounds.y + trayBounds.height
      }
    } else if (anchor === 'top') {
      if (isTop) {
        y = trayBounds.y + trayBounds.height
      } else {
        y = trayBounds.y - winHeight
      }
    } else if (anchor === 'left') {
      if (isLeft) {
        x = trayBounds.x + trayBounds.width
      } else {
        x = trayBounds.x - winWidth
      }
      y = trayBounds.y
    } else if (anchor === 'right') {
      if (isRight) {
        x = trayBounds.x - winWidth
      } else {
        x = trayBounds.x + trayBounds.width
      }
      y = trayBounds.y
    }

    if (isRight) {
      x = trayBounds.x + trayBounds.width - winWidth
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    menuWin.setPosition(Math.round(x), Math.round(y), false)
    menuWin.show()
    menuWin.focus()
  }

  showStartMenuNearTray(trayBounds) {
    const startMenuWin = this.createStartMenuWindow()
    const [winWidth, winHeight] = startMenuWin.getSize()
    const position = this.taskbarSettings.position

    const display = screen.getDisplayMatching(trayBounds)
    const { workArea } = display

    let x = trayBounds.x
    let y = trayBounds.y

    switch (position) {
      case 'bottom':
        y = trayBounds.y - winHeight
        break
      case 'top':
        y = trayBounds.y + trayBounds.height
        break
      case 'left':
        x = trayBounds.x + trayBounds.width
        break
      case 'right':
        x = trayBounds.x - winWidth
        break
      default:
        if (trayBounds.y + trayBounds.height + winHeight > workArea.y + workArea.height) {
          y = trayBounds.y - winHeight
        } else {
          y = trayBounds.y + trayBounds.height
        }
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    startMenuWin.setPosition(Math.round(x), Math.round(y), false)
    startMenuWin.show()
    startMenuWin.focus()

    this.closeQuickSettings()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  showQuickSettingsNearTray(trayBounds) {
    const quickSettingsWin = this.createQuickSettingsWindow()
    const [winWidth, winHeight] = quickSettingsWin.getSize()
    const position = this.taskbarSettings.position

    const display = screen.getDisplayMatching(trayBounds)
    const { workArea } = display

    let x = trayBounds.x + trayBounds.width - winWidth
    let y = trayBounds.y

    switch (position) {
      case 'bottom':
        y = trayBounds.y - winHeight
        break
      case 'top':
        y = trayBounds.y + trayBounds.height
        break
      case 'left':
        x = trayBounds.x + trayBounds.width
        break
      case 'right':
        x = trayBounds.x - winWidth
        break
      default:
        if (trayBounds.y + trayBounds.height + winHeight > workArea.y + workArea.height) {
          y = trayBounds.y - winHeight
        } else {
          y = trayBounds.y + trayBounds.height
        }
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    quickSettingsWin.setPosition(Math.round(x), Math.round(y), false)
    quickSettingsWin.show()
    quickSettingsWin.focus()

    this.closeStartMenu()
    this.closeNotificationCenter()
    this.closeSystemTray()
  }

  showNotificationCenterNearTray(trayBounds) {
    const notificationCenterWin = this.createNotificationCenterWindow()
    const [winWidth, winHeight] = notificationCenterWin.getSize()
    const position = this.taskbarSettings.position

    const display = screen.getDisplayMatching(trayBounds)
    const { workArea } = display

    let x = trayBounds.x + trayBounds.width - winWidth
    let y = trayBounds.y

    switch (position) {
      case 'bottom':
        y = trayBounds.y - winHeight
        break
      case 'top':
        y = trayBounds.y + trayBounds.height
        break
      case 'left':
        x = trayBounds.x + trayBounds.width
        break
      case 'right':
        x = trayBounds.x - winWidth
        break
      default:
        if (trayBounds.y + trayBounds.height + winHeight > workArea.y + workArea.height) {
          y = trayBounds.y - winHeight
        } else {
          y = trayBounds.y + trayBounds.height
        }
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    notificationCenterWin.setPosition(Math.round(x), Math.round(y), false)
    notificationCenterWin.show()
    notificationCenterWin.focus()

    this.closeStartMenu()
    this.closeQuickSettings()
    this.closeSystemTray()
  }

  showSystemTrayNearTray(trayBounds) {
    const systemTrayWin = this.createSystemTrayWindow()
    const [winWidth, winHeight] = systemTrayWin.getSize()
    const position = this.taskbarSettings.position

    const display = screen.getDisplayMatching(trayBounds)
    const { workArea } = display

    let x = trayBounds.x
    let y = trayBounds.y

    switch (position) {
      case 'bottom':
        y = trayBounds.y - winHeight
        break
      case 'top':
        y = trayBounds.y + trayBounds.height
        break
      case 'left':
        x = trayBounds.x + trayBounds.width
        break
      case 'right':
        x = trayBounds.x - winWidth
        break
      default:
        if (trayBounds.y + trayBounds.height + winHeight > workArea.y + workArea.height) {
          y = trayBounds.y - winHeight
        } else {
          y = trayBounds.y + trayBounds.height
        }
    }

    x = Math.max(workArea.x, Math.min(x, workArea.x + workArea.width - winWidth))
    y = Math.max(workArea.y, Math.min(y, workArea.y + workArea.height - winHeight))

    systemTrayWin.setPosition(Math.round(x), Math.round(y), false)
    systemTrayWin.show()
    systemTrayWin.focus()

    this.closeStartMenu()
    this.closeQuickSettings()
    this.closeNotificationCenter()
  }
}

export default new WindowManager()