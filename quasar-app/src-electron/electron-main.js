import { app, BrowserWindow, ipcMain, dialog, Tray, Menu, nativeImage, screen, session } from 'electron';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import Store from 'electron-store';
import NativeServiceClient from './native-service-client.js';
import windowManager from './window-manager.js';
import MediaController from './media-controller.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const isDev = !app.isPackaged;

// 配置存储
const store = new Store({
  defaults: {
    dynamicIsland: {
      enabled: false,
      position: 'top',
      style: 'rounded',
      showOnStartup: true,
      animationsEnabled: true,
      devToolsEnabled: false,
      windowX: 0,
      windowY: 0
    },
    taskbar: {
      enabled: false,
      position: 'bottom',
      style: 'user',
      autoHide: false,
      showTime: true,
      blurRadius: 20,
      glassStrength: 0.8,
      iconColor: '#ffffff',
      barColor: 'rgba(0, 0, 0, 0.8)',
      cornerType: 'rounded',
      iconOpacity: 1,
      alignment: 'center',
      roundedDockGap: 6,
      roundedCenterGap: 12,
      roundedLeftGap: 8,
      roundedRightGap: 8
    }
  }
});

// 全局变量
let tray = null;
let trayContextMenu = null;
let mainWindow = null;
let isQuitting = false;
let nativeService = null;

// 从存储加载设置
const taskbarSettings = { ...store.get('taskbar') };
const islandSettings = store.get('dynamicIsland');
let isDynamicIslandEnabled = islandSettings.enabled;

// 预加载和渲染器路径
let preloadPath, rendererDist;

if (isDev) {
  preloadPath = join(__dirname, 'preload', 'electron-preload.cjs');
  rendererDist = join(__dirname, '..', 'dist', 'spa');
} else {
  const appPath = app.getAppPath();
  const appDir = dirname(appPath);
  preloadPath = join(appDir, 'app.asar.unpacked', 'src-electron', 'electron-preload.js');
  rendererDist = join(appDir, 'app.asar', 'dist', 'spa');
}

/**
 * 获取开发服务器 URL
 */
function getDevServerUrl() {
  const args = process.argv.slice(2);
  let specifiedPort = null;

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--port' && args[i + 1]) {
      specifiedPort = parseInt(args[i + 1]);
      break;
    } else if (args[i].startsWith('--port=')) {
      specifiedPort = parseInt(args[i].split('=')[1]);
      break;
    }
  }

  const possiblePorts = specifiedPort ? [specifiedPort] : [9528, 9527, 9090, 9000, 8080, 3000];

  const devServerUrl = process.env.APP_URL ||
                      process.env.VITE_DEV_SERVER_URL ||
                      process.env.DEV_SERVER_URL ||
                      process.env.LOCAL_DEV_SERVER_URL ||
                      process.env.QUASAR_DEV_SERVER_URL;

  if (devServerUrl) {
    console.log('[Main] Using dev server URL:', devServerUrl);
    return devServerUrl;
  }

  console.log('[Main] No dev server URL env var, trying ports:', possiblePorts);
  return `http://localhost:${possiblePorts[0]}`;
}

/**
 * 创建系统托盘
 */
function createTray() {
  const iconPath = join(__dirname, 'assets', 'tray-icon.png');
  let trayIcon = nativeImage.createFromPath(iconPath);
  
  if (trayIcon.isEmpty()) {
    console.warn('[Tray] Tray icon not found, using default icon');
    trayIcon = nativeImage.createEmpty();
  }
  
  tray = new Tray(trayIcon);
  
  trayContextMenu = Menu.buildFromTemplate([
    {
      label: '显示主窗口',
      click: () => {
        if (mainWindow) {
          mainWindow.show();
          mainWindow.focus();
        }
      }
    },
    {
      label: '显示灵动岛',
      type: 'checkbox',
      checked: isDynamicIslandEnabled,
      click: (menuItem) => {
        isDynamicIslandEnabled = menuItem.checked
        const dynamicIslandWindow = windowManager.getWindow('dynamicIsland')
        if (dynamicIslandWindow) {
          if (menuItem.checked) {
            dynamicIslandWindow.show()
          } else {
            dynamicIslandWindow.hide()
          }
        }
        store.set('dynamicIsland.enabled', menuItem.checked)
      }
    },
    {
      label: '显示任务栏',
      type: 'checkbox',
      checked: taskbarSettings.enabled,
      click: (menuItem) => {
        taskbarSettings.enabled = menuItem.checked
        const taskbarWindow = windowManager.getWindow('taskbar')
        if (taskbarWindow) {
          if (menuItem.checked) {
            taskbarWindow.show()
          } else {
            taskbarWindow.hide()
          }
        }
        store.set('taskbar.enabled', menuItem.checked)
      }
    },
    { type: 'separator' },
    {
      label: '退出',
      click: () => {
        isQuitting = true;
        app.quit();
      }
    }
  ]);

  tray.setContextMenu(trayContextMenu);
  tray.setToolTip('Desktop Assistant');
  
  tray.on('click', () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

/**
 * 设置 IPC 处理器
 */
function setupIPCHandlers() {
  // 窗口控制
  ipcMain.handle('window:minimize', () => {
    mainWindow?.minimize();
    return true;
  });

  ipcMain.handle('window:maximize', () => {
    if (mainWindow?.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow?.maximize();
    }
    return true;
  });

  ipcMain.handle('window:close', () => {
    mainWindow?.hide();
    return true;
  });

  ipcMain.handle('window:isMaximized', () => {
    return mainWindow?.isMaximized() || false;
  });

  ipcMain.on('window:quit', () => {
    isQuitting = true;
    app.quit();
  });

  // 监听任务栏启用状态变化，更新托盘菜单
  ipcMain.on('tray:updateTaskbarEnabled', (event, enabled) => {
    if (tray && trayContextMenu && trayContextMenu.items && trayContextMenu.items[2]) {
      trayContextMenu.items[2].checked = enabled;
      tray.setContextMenu(trayContextMenu);
    }
  });

  // 灵动岛 IPC（在 windowManager 窗口创建之后注册）
  ipcMain.handle('dynamicIsland:isEnabled', () => {
    console.log('[DynamicIsland] isEnabled:', isDynamicIslandEnabled);
    return isDynamicIslandEnabled;
  });

  ipcMain.handle('dynamicIsland:setEnabled', (event, enabled) => {
    console.log('[DynamicIsland] setEnabled:', enabled);
    isDynamicIslandEnabled = enabled;
    islandSettings.enabled = enabled;
    try {
      store.set('dynamicIsland', islandSettings);
    } catch (error) {
      console.error('[DynamicIsland] Failed to save settings:', error.message);
    }

    // 同步设置到 windowManager
    if (windowManager.islandSettings) {
      windowManager.islandSettings.enabled = enabled;
    }

    const dynamicIslandWindow = windowManager.getWindow('dynamicIsland');
    if (dynamicIslandWindow && !dynamicIslandWindow.isDestroyed()) {
      if (enabled) {
        dynamicIslandWindow.show();
      } else {
        dynamicIslandWindow.hide();
      }
    }

    // 更新托盘菜单
    if (tray && trayContextMenu && trayContextMenu.items && trayContextMenu.items[1]) {
      trayContextMenu.items[1].checked = enabled;
      tray.setContextMenu(trayContextMenu);
    }

    return true;
  });

  ipcMain.handle('dynamicIsland:getSettings', () => {
    console.log('[DynamicIsland] getSettings:', islandSettings);
    return { ...islandSettings };
  });

  ipcMain.handle('dynamicIsland:setSettings', (event, settings) => {
    console.log('[DynamicIsland] setSettings:', settings);
    Object.assign(islandSettings, settings);
    try {
      store.set('dynamicIsland', islandSettings);
    } catch (error) {
      console.error('[DynamicIsland] Failed to save settings:', error.message);
    }

    // 同步设置到 windowManager
    if (windowManager.islandSettings) {
      Object.assign(windowManager.islandSettings, settings);
    }

    const dynamicIslandWindow = windowManager.getWindow('dynamicIsland');
    if (dynamicIslandWindow && !dynamicIslandWindow.isDestroyed()) {
      dynamicIslandWindow.webContents.send('dynamicIsland:settingsUpdated', islandSettings);
    }

    return true;
  });

  ipcMain.handle('dynamicIsland:setMusicControlEnabled', (event, enabled) => {
    console.log('[DynamicIsland] setMusicControlEnabled:', enabled);
    islandSettings.musicControlEnabled = enabled;
    try {
      store.set('dynamicIsland', islandSettings);
    } catch (error) {
      console.error('[DynamicIsland] Failed to save settings:', error.message);
    }

    // 同步设置到 windowManager
    if (windowManager.islandSettings) {
      windowManager.islandSettings.musicControlEnabled = enabled;
    }

    return true;
  });

  ipcMain.handle('dynamicIsland:setDevToolsEnabled', (event, enabled) => {
    console.log('[DynamicIsland] setDevToolsEnabled:', enabled);
    islandSettings.devToolsEnabled = enabled;
    try {
      store.set('dynamicIsland', islandSettings);
    } catch (error) {
      console.error('[DynamicIsland] Failed to save settings:', error.message);
    }

    // 同步设置到 windowManager
    if (windowManager.islandSettings) {
      windowManager.islandSettings.devToolsEnabled = enabled;
    }

    const dynamicIslandWindow = windowManager.getWindow('dynamicIsland');
    if (dynamicIslandWindow && !dynamicIslandWindow.isDestroyed()) {
      // 确保窗口可见后再打开开发者工具
      if (enabled && !dynamicIslandWindow.isVisible()) {
        dynamicIslandWindow.show();
        console.log('[DynamicIsland] Window shown for DevTools');
      }
      
      if (enabled) {
        dynamicIslandWindow.webContents.openDevTools({ mode: 'detach' });
        console.log('[DynamicIsland] DevTools opened');
      } else {
        dynamicIslandWindow.webContents.closeDevTools();
        console.log('[DynamicIsland] DevTools closed');
      }
    } else {
      console.warn('[DynamicIsland] Dynamic island window not available');
    }

    return true;
  });

  // 原生服务 IPC
  ipcMain.handle('nativeService:getCPUUsage', async () => {
    try {
      return await nativeService.getCPUUsage();
    } catch (error) {
      console.error('[NativeService] Failed to get CPU usage:', error);
      return { type: 'cpu', error: error.message };
    }
  });

  ipcMain.handle('nativeService:getMemoryUsage', async () => {
    try {
      return await nativeService.getMemoryUsage();
    } catch (error) {
      console.error('[NativeService] Failed to get memory usage:', error);
      return { type: 'memory', error: error.message };
    }
  });

  ipcMain.handle('nativeService:getBatteryStatus', async () => {
    try {
      return await nativeService.getBatteryStatus();
    } catch (error) {
      console.error('[NativeService] Failed to get battery status:', error);
      return { type: 'battery', error: error.message };
    }
  });

  ipcMain.handle('nativeService:getVolumeLevel', async () => {
    try {
      return await nativeService.getVolumeLevel();
    } catch (error) {
      console.error('[NativeService] Failed to get volume:', error);
      return { type: 'volume', error: error.message };
    }
  });

  ipcMain.handle('nativeService:getAllStats', async () => {
    try {
      return await nativeService.getAllStats();
    } catch (error) {
      console.error('[NativeService] Failed to get all stats:', error);
      return { error: error.message };
    }
  });

  // 媒体轮询控制
  ipcMain.handle('nativeService:startMediaPolling', (event, interval) => {
    try {
      nativeService.startMediaPolling(interval);
      return true;
    } catch (error) {
      console.error('[NativeService] Failed to start media polling:', error);
      return false;
    }
  });

  ipcMain.handle('nativeService:stopMediaPolling', () => {
    try {
      nativeService.stopMediaPolling();
      return true;
    } catch (error) {
      console.error('[NativeService] Failed to stop media polling:', error);
      return false;
    }
  });

  // 手动启动/停止服务
  ipcMain.handle('nativeService:start', async () => {
    try {
      return await nativeService.ensureServiceStarted();
    } catch (error) {
      console.error('[NativeService] Failed to start service:', error);
      return false;
    }
  });

  ipcMain.handle('nativeService:stop', async () => {
    try {
      await nativeService.stopService();
      return true;
    } catch (error) {
      console.error('[NativeService] Failed to stop service:', error);
      return false;
    }
  });
}

/**
 * 初始化原生服务（按需启动模式）
 */
async function initNativeService() {
  nativeService = new NativeServiceClient();
  
  console.log('[NativeService] Service client initialized (on-demand mode)');
  console.log('[NativeService] Service will start when first request is made');
  
  // 设置事件监听器
  nativeService.on('broadcast', (data) => {
    console.log('[NativeService] Broadcast received:', data);
    mainWindow?.webContents.send('nativeService:broadcast', data);
    const dynamicIslandWindow = windowManager?.getWindow('dynamicIsland');
    dynamicIslandWindow?.webContents.send('nativeService:broadcast', data);
  });
  
  nativeService.on('mediaUpdate', (data) => {
    console.log('[NativeService] Media update received:', data);
    
    ipcMain.emit('media:fromNativeService', null, data);
    
    const dynamicIslandWindow = windowManager?.getWindow('dynamicIsland');
    if (dynamicIslandWindow) {
      dynamicIslandWindow?.webContents.send('media-update', {
        ...data,
        isPlaying: data.playing || false,
        position: data.position || 0,
        duration: data.duration || 0
      });
    }
  });
  
  nativeService.on('connect', () => {
    console.log('[NativeService] Connected');
    ipcMain.emit('nativeService:connected');
  });
  
  nativeService.on('disconnect', () => {
    console.log('[NativeService] Disconnected');
    ipcMain.emit('nativeService:disconnected');
  });
  
  // 不再自动启动服务，改为按需启动
  // 服务将在第一次调用 getCPUUsage/getMemoryInfo 等方法时自动启动
}

/**
 * 应用启动
 */
app.whenReady().then(async () => {
  console.log('[Main] App ready, initializing...');
  console.log('[Main] windowManager initialized:', windowManager ? 'yes' : 'no');
  console.log('[Main] windowManager.windows size:', windowManager?.windows?.size || 0);
  
  // 初始化原生服务
  await initNativeService();
  
  // ⚠️ 重要：先设置 IPC 处理器，再创建窗口
  // 防止渲染进程加载时调用 API 但处理器还未注册
  setupIPCHandlers();
  
  // 创建窗口（通过窗口管理器）
  mainWindow = windowManager.createMainWindow();
  const dynamicIslandWindow = windowManager.createDynamicIslandWindow();
  console.log('[Main] Before creating taskbar window');
  try {
    const taskbarWindow = windowManager.createTaskbarWindow();
    console.log('[Main] Taskbar window created:', taskbarWindow ? 'success' : 'failed');
    console.log('[Main] Available windows after creation:', Array.from(windowManager.windows.keys()));
  } catch (error) {
    console.error('[Main] Failed to create taskbar window:', error);
  }
  
  // 初始化媒体控制器
  const mediaController = new MediaController(mainWindow, dynamicIslandWindow)
  mediaController.setEnabled(isDynamicIslandEnabled)
  mediaController.setMusicControlEnabled(islandSettings.musicControlEnabled)
  
  createTray();
});

/**
 * 应用退出
 */
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    mainWindow = windowManager.createMainWindow();
  }
});

app.on('before-quit', async () => {
  isQuitting = true;
  
  // 停止原生服务
  if (nativeService) {
    try {
      await nativeService.stopService();
    } catch (error) {
      console.error('[NativeService] Error stopping service:', error);
    }
  }
  
  // 关闭所有窗口（通过窗口管理器）
  windowManager?.closeAll();
});