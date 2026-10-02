import { ipcMain, BrowserWindow } from 'electron'

export default function setupIPC(WindowManager) {
  // 悬浮窗拖拽
  ipcMain.on('set-window-position', (event, { x, y }) => {
    const win = BrowserWindow.fromWebContents(event.sender)
    if (win) {
      win.setPosition(x, y)
    }
  })
  
  ipcMain.on('get-window-position', (event) => {
    const win = BrowserWindow.fromWebContents(event.sender)
    if (win) {
      const [x, y] = win.getPosition()
      event.returnValue = { x, y }
    } else {
      event.returnValue = { x: 0, y: 0 }
    }
  })
  
  // 显示/隐藏主窗口
  ipcMain.on('suspension:toggle-main', () => {
    const mainWin = WindowManager.getWindow('main')
    if (mainWin) {
      if (mainWin.isVisible()) {
        mainWin.hide()
      } else {
        mainWin.show()
        mainWin.focus()
      }
    }
  })
  
  // 显示设置窗口
  ipcMain.on('show-settings-window', () => {
    const mainWin = WindowManager.getWindow('main')
    if (mainWin) {
      mainWin.show()
      mainWin.focus()
      // 可以触发主窗口内的设置页面路由
      mainWin.webContents.send('navigate-to', '/settings')
    }
  })
}
