import { spawn } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';
import os from 'os';
import fs from 'fs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// DLL文件路径
const projectRoot = path.resolve(__dirname, '..', '..');
const DLL_PATHS = [
  path.join(projectRoot, 'src-native', 'build', 'Release', 'NativeServiceDLL.dll'),
  path.join(projectRoot, 'src-native', 'out', 'build', 'x64-Debug', 'NativeServiceDLL.dll'),
  path.join(projectRoot, 'src-native', 'NativeServiceDLL.dll'),
  path.join(__dirname, '..', 'src-native', 'build', 'Release', 'NativeServiceDLL.dll'),
  path.join(__dirname, '..', 'src-native', 'out', 'build', 'x64-Debug', 'NativeServiceDLL.dll'),
];

let DLL_PATH = null;
for (const dllPath of DLL_PATHS) {
  if (fs.existsSync(dllPath)) {
    DLL_PATH = dllPath;
    console.log('[NativeService] Found DLL at:', DLL_PATH);
    break;
  }
}

export default class NativeServiceClient {
  constructor() {
    // win32-api 相关变量
    this.win32 = null;
    this.dllInstance = null;
    this.isDLLLoaded = false;
    
    // 媒体轮询相关
    this.mediaPollInterval = null;
    this.eventListeners = {};
  }

  // 初始化DLL
  async loadDLL() {
    if (this.isDLLLoaded) {
      console.log('[NativeService] DLL already loaded');
      return true;
    }

    if (!DLL_PATH) {
      console.warn('[NativeService] No NativeServiceDLL.dll found, using fallback implementations');
      return false;
    }

    console.log('[NativeService] Starting to load DLL from:', DLL_PATH);
    
    try {
      console.log('[NativeService] Importing win32-api...');
      let dll;
      try {
        const win32 = await import('win32-api');
        console.log('[NativeService] win32-api imported successfully');
        console.log('[NativeService] win32 keys:', Object.keys(win32));
        
        // 保存 win32 到实例变量
        this.win32 = win32;
        
        // 检查 ffi 是否可用
        console.log('[NativeService] win32.ffi exists:', !!win32.ffi);
        
        if (!win32.ffi) {
          console.error('[NativeService] win32.ffi is not available');
          return false;
        }
        
        // 使用 ffi 加载 DLL
        console.log('[NativeService] ffi keys:', Object.keys(win32.ffi));
        
        // 使用 ffi.load 加载 DLL
        console.log('[NativeService] Using ffi.load to load DLL...');
        
        // ffi.load(DLL_PATH) 应该返回一个库对象
        try {
          this.dllInstance = win32.ffi.load(DLL_PATH);
          console.log('[NativeService] DLL loaded, instance:', this.dllInstance);
          console.log('[NativeService] DLL instance type:', typeof this.dllInstance);
          console.log('[NativeService] DLL instance keys:', Object.keys(this.dllInstance || {}).slice(0, 10));
          
          if (this.dllInstance) {
            this.isDLLLoaded = true;
            console.log('[NativeService] DLL loaded successfully');
            return true;
          }
        } catch (libError) {
          console.error('[NativeService] ffi.load error:', libError.message);
          console.error('[NativeService] ffi.load stack:', libError.stack);
        }
        
        console.error('[NativeService] Failed to load DLL using ffi');
        return false;
        
      } catch (importError) {
        console.error('[NativeService] Failed to import win32-api:', importError.message);
        console.error('[NativeService] Import stack:', importError.stack);
        return false;
      }
    } catch (error) {
      console.error('[NativeService] Error loading DLL:', error);
      console.error('[NativeService] Error stack:', error.stack);
      return false;
    }
  }

  // 获取函数
  async getFunction(name) {
    if (!this.dllInstance || !this.isDLLLoaded) {
      console.log('[NativeService] getFunction: DLL not loaded');
      return null;
    }
    
    try {
      // this.dllInstance 是 win32.ffi.load() 返回的对象
      // 它有 func 和 symbol 方法
      
      console.log('[NativeService] getFunction: trying to get symbol', name);
      
      // 使用 symbol 方法获取函数地址
      let funcPtr;
      try {
        // 尝试使用 symbol 方法
        // symbol(name, type) - 需要指定类型
        // 对于函数指针，使用 'void' 类型
        funcPtr = this.dllInstance.symbol(name, 'void');
        console.log('[NativeService] getFunction: symbol returned:', funcPtr);
      } catch (symbolError) {
        console.log('[NativeService] getFunction: symbol error:', symbolError.message);
        // 如果 symbol 方法失败，尝试使用 ref 库直接读取
        return null;
      }
      
      if (!funcPtr) {
        console.log('[NativeService] getFunction: funcPtr is null');
        return null;
      }
      
      // 使用 ffi.func 创建一个函数包装器
      // 需要指定函数签名
      // 从 win32.ffi 获取 types
      const ffiTypes = this.win32.ffi.types;
      console.log('[NativeService] getFunction: ffiTypes:', ffiTypes);
      
      if (!ffiTypes) {
        console.error('[NativeService] getFunction: ffiTypes is undefined');
        return null;
      }
      
      // 创建一个函数签名
      // int NativeService_GetPinnedApps(char* buffer, int bufferSize)
      // 使用 func 方法创建函数包装器
      const { func } = this.dllInstance;
      console.log('[NativeService] getFunction: func method:', typeof func);
      
      // 尝试使用 func 创建函数包装器
      // func(returnType, [argTypes...])(address)
      try {
        // 使用 ffiTypes 中的类型对象
        const returnType = ffiTypes.int;
        // 检查 pointer 类型
        console.log('[NativeService] getFunction: ffiTypes.pointer:', ffiTypes.pointer);
        console.log('[NativeService] getFunction: ffiTypes.uintptr_t:', ffiTypes.uintptr_t);
        
        // 使用 uintptr_t 作为指针类型
        const pointerType = ffiTypes.uintptr_t || ffiTypes.ulong;
        const argTypes = [pointerType, ffiTypes.int];
        console.log('[NativeService] getFunction: returnType:', returnType, 'argTypes:', argTypes);
        
        // 创建函数
        console.log('[NativeService] getFunction: about to call func(returnType, argTypes)');
        let funcImpl;
        try {
          funcImpl = func(returnType, argTypes);
          console.log('[NativeService] getFunction: funcImpl type:', typeof funcImpl);
          console.log('[NativeService] getFunction: funcImpl:', funcImpl);
        } catch (createError) {
          console.error('[NativeService] getFunction: create error:', createError.message);
          console.error('[NativeService] getFunction: create stack:', createError.stack);
          return null;
        }
        
        // 调用函数并传入地址
        console.log('[NativeService] getFunction: calling funcImpl with funcPtr:', funcPtr);
        try {
          const wrappedFunc = funcImpl(funcPtr);
          console.log('[NativeService] getFunction: wrappedFunc type:', typeof wrappedFunc);
          console.log('[NativeService] getFunction: wrappedFunc:', wrappedFunc);
          return wrappedFunc;
        } catch (callError) {
          console.error('[NativeService] getFunction: call error:', callError.message);
          console.error('[NativeService] getFunction: call stack:', callError.stack);
          return null;
        }
      } catch (funcError) {
        console.error('[NativeService] getFunction: func error:', funcError.message);
        console.error('[NativeService] getFunction: func stack:', funcError.stack);
        return null;
      }
    } catch (error) {
      console.error('[NativeService] Error getting function:', name, error);
      console.error('[NativeService] Error stack:', error.stack);
      return null;
    }
  }

  // 启动媒体轮询
  startMediaPolling(interval = 2000) {
    if (this.mediaPollInterval) {
      return;
    }

    console.log('[NativeService] Starting media polling...');

    this.mediaPollInterval = setInterval(async () => {
      if (this.isDLLLoaded) {
        try {
          const mediaInfo = await this.getMediaInfo();
          if (mediaInfo) {
            this.emit('mediaUpdate', mediaInfo);
          }
        } catch (err) {
          console.debug('[NativeService] Media polling error:', err);
        }
      }
    }, interval);
  }

  // 停止媒体轮询
  stopMediaPolling() {
    if (this.mediaPollInterval) {
      console.log('[NativeService] Stopping media polling...');
      clearInterval(this.mediaPollInterval);
      this.mediaPollInterval = null;
    }
  }

  // 事件监听
  on(event, callback) {
    if (!this.eventListeners[event]) {
      this.eventListeners[event] = [];
    }
    this.eventListeners[event].push(callback);
  }

  // 触发事件
  emit(event, data) {
    if (this.eventListeners[event]) {
      this.eventListeners[event].forEach(callback => callback(data));
    }
  }

  // 获取CPU使用率
  async getCPUUsage() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return this.fallbackCPUUsage();
    }

    try {
      const func = this.getFunction('NativeService_GetCPUUsage');
      if (!func) {
        return this.fallbackCPUUsage();
      }
      
      const usage = func();
      return { type: 'cpu', usage: parseFloat(usage.toFixed(1)) };
    } catch (error) {
      console.warn('[NativeService] Failed to get CPU usage:', error);
      return this.fallbackCPUUsage();
    }
  }

  // 获取内存信息
  async getMemoryUsage() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return this.fallbackMemoryUsage();
    }

    try {
      const func = this.getFunction('NativeService_GetMemoryInfo');
      if (!func) {
        return this.fallbackMemoryUsage();
      }

      const { struct, DTypes } = await import('win32-api');
      
      const result = struct({
        total: DTypes.uint64,
        available: DTypes.uint64,
        used: DTypes.uint64,
        usage: DTypes.float
      });

      func(result.ref());

      return {
        type: 'memory',
        total: result.total,
        available: result.available,
        used: result.used,
        usage: parseFloat(result.usage.toFixed(1))
      };
    } catch (error) {
      console.warn('[NativeService] Failed to get memory usage:', error);
      return this.fallbackMemoryUsage();
    }
  }

  // 获取电池状态
  async getBatteryStatus() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return { type: 'battery', error: 'Service unavailable' };
    }

    try {
      const func = this.getFunction('NativeService_GetBatteryInfo');
      if (!func) {
        return { type: 'battery', error: 'Service unavailable' };
      }

      const { struct, DTypes } = await import('win32-api');
      
      const result = struct({
        acOnline: DTypes.bool,
        batteryPresent: DTypes.bool,
        batteryLifePercent: DTypes.uchar,
        batteryLifeTime: DTypes.uint32,
        batteryFullLifeTime: DTypes.uint32
      });

      func(result.ref());

      return {
        type: 'battery',
        acOnline: result.acOnline,
        batteryPresent: result.batteryPresent,
        batteryLifePercent: result.batteryLifePercent,
        batteryLifeTime: result.batteryLifeTime,
        batteryFullLifeTime: result.batteryFullLifeTime
      };
    } catch (error) {
      console.warn('[NativeService] Failed to get battery status:', error);
      return { type: 'battery', error: 'Service unavailable' };
    }
  }

  // 获取音量信息
  async getVolumeLevel() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return { type: 'volume', error: 'Service unavailable' };
    }

    try {
      const func = this.getFunction('NativeService_GetVolumeInfo');
      if (!func) {
        return { type: 'volume', error: 'Service unavailable' };
      }

      const { struct, DTypes } = await import('win32-api');
      
      const result = struct({
        level: DTypes.float,
        muted: DTypes.bool
      });

      func(result.ref());

      return {
        type: 'volume',
        level: parseFloat(result.level.toFixed(1)),
        muted: result.muted
      };
    } catch (error) {
      console.warn('[NativeService] Failed to get volume:', error);
      return { type: 'volume', error: 'Service unavailable' };
    }
  }

  // 获取媒体信息
  async getMediaInfo() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return null;
    }

    try {
      const func = this.getFunction('NativeService_GetMediaInfo');
      if (!func) {
        return null;
      }

      const bufferSize = 65536;
      const buffer = Buffer.alloc(bufferSize);
      
      const result = func(buffer, bufferSize);
      
      if (result > 0 && result < bufferSize) {
        const jsonStr = buffer.toString('utf-8', 0, result);
        return JSON.parse(jsonStr);
      }
      return null;
    } catch (error) {
      console.error('[NativeService] getMediaInfo error:', error);
      return null;
    }
  }

  // 获取所有状态
  async getAllStats() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return {
        cpu: this.fallbackCPUUsage(),
        memory: this.fallbackMemoryUsage(),
        battery: { type: 'battery', error: 'Service unavailable' },
        volume: { type: 'volume', error: 'Service unavailable' },
        media: null
      };
    }

    try {
      const [cpu, memory, battery, volume, media] = await Promise.all([
        this.getCPUUsage(),
        this.getMemoryUsage(),
        this.getBatteryStatus(),
        this.getVolumeLevel(),
        this.getMediaInfo()
      ]);

      return { cpu, memory, battery, volume, media };
    } catch (error) {
      console.warn('[NativeService] Failed to get all stats:', error);
      return {
        cpu: this.fallbackCPUUsage(),
        memory: this.fallbackMemoryUsage(),
        battery: { type: 'battery', error: 'Service unavailable' },
        volume: { type: 'volume', error: 'Service unavailable' },
        media: null
      };
    }
  }

  // 获取固定应用列表
  async getPinnedApps() {
    console.log('[NativeService] getPinnedApps called');
    
    // 优先使用DLL调用
    try {
      await this.loadDLL();
      
      if (this.isDLLLoaded && this.dllInstance) {
        console.log('[NativeService] Using DLL to get pinned apps');
        
        const { ref, types } = await import('win32-api');
        const { UINT32, LPSTR } = types;
        
        // 分配缓冲区
        const bufferSize = 1024 * 1024; // 1MB
        const buffer = Buffer.alloc(bufferSize);
        
        try {
          // 调用 DLL 函数
          const result = this.dllInstance.NativeService_GetPinnedApps(buffer, bufferSize);
          console.log('[NativeService] DLL returned:', result);
          
          if (result > 0 && result < bufferSize) {
            const jsonStr = buffer.toString('utf8', 0, result);
            console.log('[NativeService] JSON length:', jsonStr.length);
            
            const apps = JSON.parse(jsonStr);
            console.log('[NativeService] Parsed apps:', apps.length);
            
            return apps.map(app => ({
              id: app.id || '',
              name: app.name || '',
              path: app.path || '',
              icon: app.iconData || null,
              iconWidth: app.iconWidth || 0,
              iconHeight: app.iconHeight || 0
            }));
          }
        } catch (dllError) {
          console.log('[NativeService] DLL call failed, falling back to PowerShell:', dllError.message);
        }
      }
    } catch (dllLoadError) {
      console.log('[NativeService] DLL not available, using PowerShell fallback:', dllLoadError.message);
    }
    
    // 回退到 PowerShell 实现
    console.log('[NativeService] Falling back to PowerShell for pinned apps');
    try {
      const { exec } = await import('child_process');
      
      const script = [
        '$ErrorActionPreference = "SilentlyContinue"',
        'Add-Type -AssemblyName System.Drawing',
        '$taskbarPath = [Environment]::GetFolderPath("ApplicationData") + "\\Microsoft\\Internet Explorer\\Quick Launch\\User Pinned\\TaskBar"',
        '$result = @()',
        'if (Test-Path $taskbarPath) {',
        '  Get-ChildItem $taskbarPath -Filter "*.lnk" | ForEach-Object {',
        '    $shell = New-Object -ComObject WScript.Shell',
        '    $shortcut = $shell.CreateShortcut($_.FullName)',
        '    if ($shortcut.TargetPath -and $shortcut.TargetPath.EndsWith(".exe")) {',
        '      try {',
        '        $icon = [System.Drawing.Icon]::ExtractAssociatedIcon($shortcut.TargetPath)',
        '        if ($icon) {',
        '          $ms = New-Object System.IO.MemoryStream',
        '          $icon.ToBitmap().Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)',
        '          $bytes = $ms.ToArray()',
        '          $b64 = [Convert]::ToBase64String($bytes)',
        '          $ms.Close()',
        '          $icon.Dispose()',
        '          $name = Split-Path $shortcut.TargetPath -Leaf',
        '          $result += [PSCustomObject]@{name=$name;path=$shortcut.TargetPath;icon=$b64}',
        '        }',
        '      } catch { }',
        '    }',
        '  }',
        '}',
        '$result | ConvertTo-Json -Compress'
      ].join('; ');
      
      return new Promise((resolve) => {
        const encodedScript = Buffer.from(script, 'utf16le').toString('base64');
        exec('powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand ' + encodedScript, { maxBuffer: 1024 * 1024 * 10, timeout: 30000 }, (error, stdout, stderr) => {
          if (error) {
            console.error('[NativeService] PowerShell error:', error.message);
            resolve([]);
            return;
          }
          
          try {
            const trimmed = (stdout || '').trim();
            if (!trimmed || trimmed === 'null' || trimmed === '' || trimmed === '[]') {
              console.log('[NativeService] No pinned apps found via PowerShell');
              resolve([]);
              return;
            }
            
            let apps = JSON.parse(trimmed);
            if (!Array.isArray(apps)) {
              apps = [apps];
            }
            
            console.log('[NativeService] PowerShell returned apps:', apps.length);
            resolve(apps.map(app => ({
              id: app.path || '',
              name: app.name || '',
              path: app.path || '',
              icon: app.icon || null,
              iconWidth: app.icon ? 48 : 0,
              iconHeight: app.icon ? 48 : 0
            })));
          } catch (parseError) {
            console.error('[NativeService] Parse error:', parseError.message);
            resolve([]);
          }
        });
      });
    } catch (error) {
      console.error('[NativeService] getPinnedApps error:', error);
      return Promise.resolve([]);
    }
  }

  // 提取图标
  async extractIcon(filePath, iconSize = 48) {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return null;
    }

    try {
      const func = this.getFunction('NativeService_ExtractIcon');
      if (!func) {
        return null;
      }

      const bufferSize = 65536;
      const buffer = Buffer.alloc(bufferSize);
      
      const result = func(filePath, iconSize, buffer, bufferSize);
      
      if (result > 0 && result < bufferSize) {
        const jsonStr = buffer.toString('utf-8', 0, result);
        const data = JSON.parse(jsonStr);
        if (data.success) {
          return {
            iconData: data.iconData,
            width: data.width,
            height: data.height
          };
        }
      }
      return null;
    } catch (error) {
      console.warn('[NativeService] Failed to extract icon:', error);
      return null;
    }
  }

  // 解析快捷方式
  async parseLnk(lnkPath) {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return null;
    }

    try {
      const func = this.getFunction('NativeService_ParseLnk');
      if (!func) {
        return null;
      }

      const bufferSize = 65536;
      const buffer = Buffer.alloc(bufferSize);
      
      const result = func(lnkPath, buffer, bufferSize);
      
      if (result > 0 && result < bufferSize) {
        const jsonStr = buffer.toString('utf-8', 0, result);
        const data = JSON.parse(jsonStr);
        if (data.success) {
          return {
            targetPath: data.targetPath,
            arguments: data.arguments,
            workingDirectory: data.workingDirectory,
            description: data.description,
            iconPath: data.iconPath,
            iconIndex: data.iconIndex
          };
        }
      }
      return null;
    } catch (error) {
      console.warn('[NativeService] Failed to parse lnk:', error);
      return null;
    }
  }

  // 隐藏系统任务栏
  async hideSystemTaskbar() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return false;
    }

    try {
      const func = this.getFunction('NativeService_HideSystemTaskbar');
      if (!func) {
        return false;
      }
      
      return func();
    } catch (error) {
      console.warn('[NativeService] Failed to hide system taskbar:', error);
      return false;
    }
  }

  // 显示系统任务栏
  async showSystemTaskbar() {
    await this.loadDLL();
    
    if (!this.isDLLLoaded) {
      return false;
    }

    try {
      const func = this.getFunction('NativeService_ShowSystemTaskbar');
      if (!func) {
        return false;
      }
      
      return func();
    } catch (error) {
      console.warn('[NativeService] Failed to show system taskbar:', error);
      return false;
    }
  }

  // 停止服务
  stopService() {
    this.stopMediaPolling();
    
    if (this.isDLLLoaded && this.dllInstance) {
      try {
        const func = this.getFunction('NativeService_Shutdown');
        if (func) {
          func();
        }
        this.isDLLLoaded = false;
        console.log('[NativeService] DLL shutdown successfully');
      } catch (error) {
        console.error('[NativeService] Error shutting down DLL:', error);
      }
    }
  }

  // 降级实现 - CPU
  fallbackCPUUsage() {
    const cpus = os.cpus();
    const total = cpus.reduce((acc, cpu) => {
      return acc + cpu.times.user + cpu.times.nice + cpu.times.sys + cpu.times.idle;
    }, 0);
    const idle = cpus.reduce((acc, cpu) => acc + cpu.times.idle, 0);
    const usage = ((total - idle) / total * 100).toFixed(1);

    return { type: 'cpu', usage: parseFloat(usage) };
  }

  // 降级实现 - 内存
  fallbackMemoryUsage() {
    const total = os.totalmem();
    const available = os.freemem();

    return {
      type: 'memory',
      total,
      available,
      used: total - available,
      usage: ((total - available) / total * 100).toFixed(1)
    };
  }

  // 获取活动窗口列表
  async getActiveWindows() {
    console.log('[NativeService] getActiveWindows called');
    
    // 优先使用DLL调用
    try {
      await this.loadDLL();
      
      if (this.isDLLLoaded && this.dllInstance) {
        console.log('[NativeService] Using DLL to get active windows');
        
        // 分配缓冲区
        const bufferSize = 1024 * 1024; // 1MB
        const buffer = Buffer.alloc(bufferSize);
        
        try {
          // 调用 DLL 函数
          const result = this.dllInstance.NativeService_GetActiveWindows(buffer, bufferSize);
          console.log('[NativeService] DLL NativeService_GetActiveWindows returned:', result);
          
          if (result > 0 && result < bufferSize) {
            const jsonStr = buffer.toString('utf8', 0, result);
            console.log('[NativeService] Active windows JSON length:', jsonStr.length);
            
            const windows = JSON.parse(jsonStr);
            console.log('[NativeService] Found active windows:', windows.length);
            if (windows.length > 0) {
              console.log('[NativeService] First window:', JSON.stringify(windows[0]));
            }
            
            return windows.map(w => ({
              handle: w.handle,
              title: w.title || '',
              pid: w.pid || 0,
              processPath: w.processPath || '',
              isForeground: w.isForeground || false
            }));
          }
        } catch (dllError) {
          console.log('[NativeService] DLL call failed, falling back to PowerShell:', dllError.message);
        }
      }
    } catch (dllLoadError) {
      console.log('[NativeService] DLL not available, using PowerShell fallback:', dllLoadError.message);
    }
    
    // 回退到 PowerShell 实现
    console.log('[NativeService] Falling back to PowerShell for active windows');
    try {
      const { exec } = await import('child_process');
      
      const command = `powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Process | Where-Object { $_.MainWindowTitle -ne '' } | Select-Object Id, Name, MainWindowTitle, MainWindowHandle | ConvertTo-Json"`;
      
      return new Promise((resolve) => {
        exec(command, { maxBuffer: 1024 * 1024 * 10, timeout: 30000 }, (error, stdout, stderr) => {
          if (error) {
            console.error('[NativeService] PowerShell error:', error.message);
            resolve([]);
            return;
          }
          
          try {
            const trimmed = (stdout || '').trim();
            if (!trimmed || trimmed === 'null' || trimmed === '' || trimmed === '[]') {
              console.log('[NativeService] No active windows found');
              resolve([]);
              return;
            }
            
            let processes = JSON.parse(trimmed);
            if (!Array.isArray(processes)) {
              processes = [processes];
            }
            
            const windows = processes.map(p => ({
              handle: p.MainWindowHandle,
              title: p.MainWindowTitle,
              pid: p.Id,
              processPath: p.Name + '.exe',
              isForeground: false
            }));
            
            console.log('[NativeService] Found active windows:', windows.length);
            resolve(windows);
          } catch (parseError) {
            console.error('[NativeService] Parse error:', parseError.message);
            resolve([]);
          }
        });
      });
    } catch (error) {
      console.error('[NativeService] getActiveWindows error:', error);
      return Promise.resolve([]);
    }
  }

  // 激活窗口
  async activateWindow(hwnd) {
    console.log('[NativeService] activateWindow called with handle:', hwnd);
    
    // 优先使用DLL调用
    try {
      await this.loadDLL();
      
      if (this.isDLLLoaded && this.dllInstance) {
        console.log('[NativeService] Using DLL to activate window');
        
        try {
          const result = this.dllInstance.NativeService_ActivateWindow(hwnd);
          console.log('[NativeService] DLL NativeService_ActivateWindow returned:', result);
          return result;
        } catch (dllError) {
          console.log('[NativeService] DLL call failed, falling back to PowerShell:', dllError.message);
        }
      }
    } catch (dllLoadError) {
      console.log('[NativeService] DLL not available, using PowerShell fallback:', dllLoadError.message);
    }
    
    // 回退到 PowerShell 实现
    try {
      const { exec } = await import('child_process');
      
      const script = `
        Add-Type @"
        using System;
        using System.Runtime.InteropServices;
        public class User32 {
          [DllImport("user32.dll")]
          public static extern bool SetForegroundWindow(IntPtr hWnd);
          [DllImport("user32.dll")]
          public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
        }
        "@
        $hwnd = [IntPtr]::new(${hwnd})
        [User32]::ShowWindow($hwnd, 5)
        [User32]::SetForegroundWindow($hwnd)
      `;
      
      return new Promise((resolve) => {
        const encodedScript = Buffer.from(script, 'utf16le').toString('base64');
        exec('powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand ' + encodedScript, { timeout: 5000 }, (error, stdout, stderr) => {
          if (error) {
            console.error('[NativeService] activateWindow error:', error.message);
            resolve(false);
            return;
          }
          resolve(true);
        });
      });
    } catch (error) {
      console.error('[NativeService] activateWindow error:', error);
      return Promise.resolve(false);
    }
  }

  // 关闭窗口
  async closeWindow(hwnd) {
    console.log('[NativeService] closeWindow called with handle:', hwnd);
    
    // 优先使用DLL调用
    try {
      await this.loadDLL();
      
      if (this.isDLLLoaded && this.dllInstance) {
        console.log('[NativeService] Using DLL to close window');
        
        try {
          const result = this.dllInstance.NativeService_CloseWindow(hwnd);
          console.log('[NativeService] DLL NativeService_CloseWindow returned:', result);
          return result;
        } catch (dllError) {
          console.log('[NativeService] DLL call failed, falling back to PowerShell:', dllError.message);
        }
      }
    } catch (dllLoadError) {
      console.log('[NativeService] DLL not available, using PowerShell fallback:', dllLoadError.message);
    }
    
    // 回退到 PowerShell 实现
    try {
      const { exec } = await import('child_process');
      
      const script = `
        Add-Type @"
        using System;
        using System.Runtime.InteropServices;
        public class User32 {
          [DllImport("user32.dll")]
          public static extern bool PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);
        }
        "@
        $hwnd = [IntPtr]::new(${hwnd})
        [User32]::PostMessage($hwnd, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
      `;
      
      return new Promise((resolve) => {
        const encodedScript = Buffer.from(script, 'utf16le').toString('base64');
        exec('powershell -NoProfile -ExecutionPolicy Bypass -EncodedCommand ' + encodedScript, { timeout: 5000 }, (error, stdout, stderr) => {
          if (error) {
            console.error('[NativeService] closeWindow error:', error.message);
            resolve(false);
            return;
          }
          resolve(true);
        });
      });
    } catch (error) {
      console.error('[NativeService] closeWindow error:', error);
      return Promise.resolve(false);
    }
  }

  // 保持向后兼容的方法
  async ensureServiceStarted() {
    return await this.loadDLL();
  }
}