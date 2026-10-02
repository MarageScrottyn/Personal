# 部署和配置指南

## 概述

本文档介绍了如何构建、部署和配置本 Electron 应用。

## 环境要求

### 开发环境

- Node.js >= 18.0.0
- npm >= 8.0.0
- Windows 10/11（推荐）

### 构建环境（C++ 原生服务）

- MSBuild（Visual Studio 2022 或更高版本）
- 或 GCC（MinGW-w64）

## 项目结构

```
├── src/                    # Vue 渲染进程代码
│   ├── components/         # Vue 组件
│   ├── pages/              # 页面组件
│   ├── layouts/            # 布局组件
│   └── css/                # 全局样式
├── src-electron/           # Electron 主进程代码
│   ├── electron-main.js    # 主进程入口
│   ├── electron-preload.js # 预加载脚本
│   └── native-service-client.js # 原生服务客户端
├── src-native/             # C++ 原生服务代码
│   ├── native-service.cpp  # 服务主文件
│   ├── native-service.h    # 头文件
│   └── nlohmann/           # JSON 库
├── scripts/                # 构建脚本
│   └── build-native.js     # 原生服务构建脚本
├── docs/                   # 文档
│   ├── IPC_PROTOCOL.md     # IPC 协议文档
│   └── DEPLOYMENT.md       # 部署指南
├── package.json            # 项目配置
└── quasar.config.js        # Quasar 配置
```

## 安装依赖

```bash
npm install
```

## 构建流程

### 1. 构建 C++ 原生服务

```bash
npm run build:native
```

**说明：**
- 此命令会编译 `src-native/native-service.cpp`
- 生成的可执行文件位于 `src-native/build/native-service.exe`
- 需要 MSBuild 或 GCC 编译器

### 2. 开发模式运行

```bash
npm run dev
```

**说明：**
- 启动开发服务器和 Electron 主进程
- 自动加载热更新
- 打开开发者工具

### 3. 生产构建

```bash
npm run build
```

**说明：**
- 构建 Vue 应用
- 打包 Electron 应用
- 生成安装程序

## 配置说明

### 配置文件

应用配置存储在用户数据目录的配置文件中：

- **Windows**: `%APPDATA%/<app-name>/config.json`

### 配置项

#### 灵动岛配置

```json
{
  "dynamicIsland": {
    "enabled": true,
    "position": "top",
    "style": "rounded",
    "showOnStartup": true,
    "animationsEnabled": true,
    "devToolsEnabled": false,
    "windowX": 0,
    "windowY": 10
  }
}
```

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| enabled | boolean | true | 是否启用灵动岛 |
| position | string | "top" | 位置（top/bottom） |
| style | string | "rounded" | 样式 |
| showOnStartup | boolean | true | 启动时显示 |
| animationsEnabled | boolean | true | 启用动画 |
| devToolsEnabled | boolean | false | 启用开发者工具 |
| windowX | number | 0 | X 坐标位置 |
| windowY | number | 10 | Y 坐标位置 |

#### 任务栏配置

```json
{
  "taskbar": {
    "enabled": true,
    "position": "bottom",
    "style": "user",
    "autoHide": false,
    "showTime": true,
    "blurRadius": 20,
    "glassStrength": 0.8,
    "iconColor": "#ffffff",
    "barColor": "rgba(0, 0, 0, 0.8)",
    "cornerType": "rounded",
    "iconOpacity": 1,
    "alignment": "center",
    "roundedDockGap": 6,
    "roundedCenterGap": 12,
    "roundedLeftGap": 8,
    "roundedRightGap": 8
  }
}
```

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| enabled | boolean | true | 是否启用任务栏 |
| position | string | "bottom" | 位置（top/bottom/left/right） |
| style | string | "user" | 样式类型 |
| autoHide | boolean | false | 自动隐藏 |
| showTime | boolean | true | 显示时间 |
| blurRadius | number | 20 | 模糊半径 |
| glassStrength | number | 0.8 | 毛玻璃强度 |
| iconColor | string | "#ffffff" | 图标颜色 |
| barColor | string | "rgba(0,0,0,0.8)" | 任务栏背景色 |
| cornerType | string | "rounded" | 圆角类型 |
| iconOpacity | number | 1 | 图标透明度 |
| alignment | string | "center" | 对齐方式（left/center/right） |
| roundedDockGap | number | 6 | 中间图标间距 |
| roundedCenterGap | number | 12 | 中间与两边间距 |
| roundedLeftGap | number | 8 | 左侧元素间距 |
| roundedRightGap | number | 8 | 右侧元素间距 |

## 启动选项

### 命令行参数

| 参数 | 说明 | 示例 |
|------|------|------|
| --port | 指定开发服务器端口 | `npm run dev -- --port 8080` |
| --no-native | 不启动原生服务 | `npm run dev -- --no-native` |

## 调试指南

### 启用开发者工具

1. **主窗口**：自动开启（开发模式）
2. **灵动岛**：在设置中启用 `devToolsEnabled`
3. **任务栏**：不支持开发者工具

### 日志查看

日志输出到控制台和开发工具的 Console 面板。

#### 日志级别

- `[Main]` - 主进程日志
- `[DynamicIsland]` - 灵动岛相关日志
- `[Taskbar]` - 任务栏相关日志
- `[NativeService]` - 原生服务相关日志

### 常见问题

#### 1. 原生服务无法启动

**原因**：缺少编译器或编译失败

**解决方案**：
- 安装 Visual Studio 2022
- 或者安装 MinGW-w64
- 运行 `npm run build:native` 检查错误

#### 2. 灵动岛不显示

**原因**：
- 灵动岛被禁用
- 窗口位置超出屏幕

**解决方案**：
- 在托盘菜单中启用灵动岛
- 检查配置文件中的 windowX/windowY 值

#### 3. 任务栏点击无效

**原因**：任务栏窗口设置为不接受焦点

**解决方案**：
- 检查任务栏窗口配置
- 确保 `focusable: false`（正常行为）

#### 4. 设置不生效

**原因**：配置文件未正确保存

**解决方案**：
- 检查配置文件路径
- 确保应用有写入权限

## 打包说明

### 生成安装程序

```bash
npm run pack
```

**输出**：
- Windows Installer: `dist/electron/Packaged/<app-name>-Setup.exe`
- Portable: `dist/electron/Packaged/win-unpacked/`

### 打包配置

在 `electron-builder.json` 中配置打包选项：

```json
{
  "appId": "com.example.desktop-assistant",
  "productName": "Desktop Assistant",
  "directories": {
    "output": "dist/electron/Packaged"
  },
  "win": {
    "target": ["nsis", "portable"],
    "icon": "src-electron/assets/icon.ico"
  },
  "nsis": {
    "oneClick": false,
    "allowToChangeInstallationDirectory": true
  }
}
```

## 卸载说明

### Windows

1. 通过「控制面板」→「程序和功能」卸载
2. 或直接删除安装目录
3. 配置文件位于 `%APPDATA%/<app-name>/`

## 技术支持

如有问题，请查看：
- [IPC 协议文档](./IPC_PROTOCOL.md)
- 项目 GitHub Issues
- 联系开发团队