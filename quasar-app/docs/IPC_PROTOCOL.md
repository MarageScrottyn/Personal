# IPC 通信协议文档

## 概述

本文档定义了 Electron 主进程与渲染进程之间、以及 Electron 主进程与 C++ 原生服务进程之间的通信协议。

## 协议格式

### 消息结构

所有 IPC 消息采用 JSON 格式，包含以下字段：

```json
{
  "id": "消息唯一标识（可选）",
  "action": "操作类型",
  "data": "请求数据（可选）",
  "error": "错误信息（仅响应）",
  "timestamp": "时间戳"
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string/number | 否 | 消息唯一标识，用于请求-响应模式 |
| action | string | 是 | 操作类型，如 `get_cpu`, `expand` 等 |
| data | any | 否 | 请求或响应的数据内容 |
| error | string | 否 | 错误信息，仅在出错时返回 |
| timestamp | number | 否 | 消息发送时间戳 |

## 通信模式

### 1. 请求-响应模式

用于需要获取结果的操作，渲染进程发送请求后等待响应。

**示例：**
```json
// 请求
{
  "id": 1,
  "action": "nativeService:getCPUUsage",
  "timestamp": 1699900000000
}

// 响应
{
  "id": 1,
  "action": "nativeService:getCPUUsage",
  "data": {
    "type": "cpu",
    "usage": 45.2
  },
  "timestamp": 1699900001000
}
```

### 2. 事件广播模式

用于单向通知，无需响应。

**示例：**
```json
{
  "action": "dynamicIsland:expanded",
  "data": true,
  "timestamp": 1699900000000
}
```

## 消息分类

### A. 窗口控制消息

| Action | 方向 | 说明 |
|--------|------|------|
| `window:minimize` | 渲染 → 主进程 | 最小化主窗口 |
| `window:maximize` | 渲染 → 主进程 | 最大化/还原主窗口 |
| `window:close` | 渲染 → 主进程 | 关闭主窗口 |
| `window:quit` | 渲染 → 主进程 | 退出应用 |
| `window:maximized` | 主进程 → 渲染 | 窗口已最大化通知 |
| `window:unmaximized` | 主进程 → 渲染 | 窗口已还原通知 |

### B. 灵动岛消息

#### 请求消息

| Action | 方向 | 说明 |
|--------|------|------|
| `dynamicIsland:expand` | 渲染 → 主进程 | 展开灵动岛 |
| `dynamicIsland:collapse` | 渲染 → 主进程 | 收起灵动岛 |
| `dynamicIsland:toggle` | 渲染 → 主进程 | 切换展开/收起状态 |
| `dynamicIsland:update` | 渲染 → 主进程 | 更新灵动岛内容 |
| `dynamicIsland:show` | 渲染 → 主进程 | 显示灵动岛 |
| `dynamicIsland:hide` | 渲染 → 主进程 | 隐藏灵动岛 |
| `dynamicIsland:setEnabled` | 渲染 → 主进程 | 设置灵动岛启用状态 |
| `dynamicIsland:getSettings` | 渲染 → 主进程 | 获取灵动岛设置 |
| `dynamicIsland:setSettings` | 渲染 → 主进程 | 设置灵动岛配置 |
| `dynamicIsland:updateStyle` | 渲染 → 主进程 | 更新灵动岛样式 |
| `dynamicIsland:updatePosition` | 渲染 → 主进程 | 更新灵动岛位置 |
| `dynamicIsland:drag-end` | 渲染 → 主进程 | 拖动结束，保存位置 |

#### 响应/广播消息

| Action | 方向 | 说明 |
|--------|------|------|
| `dynamicIsland:expanded` | 主进程 → 渲染 | 灵动岛展开状态变化 |
| `dynamicIsland:styleUpdated` | 主进程 → 渲染 | 样式更新通知 |
| `dynamicIsland:positionUpdated` | 主进程 → 渲染 | 位置更新通知 |

#### 数据结构

**展开请求数据：**
```json
{
  "action": "dynamicIsland:expand",
  "data": {
    "height": 200
  }
}
```

**设置数据：**
```json
{
  "enabled": true,
  "position": "top",
  "style": "rounded",
  "showOnStartup": true,
  "animationsEnabled": true,
  "devToolsEnabled": false,
  "windowX": 0,
  "windowY": 10
}
```

### C. 任务栏消息

#### 请求消息

| Action | 方向 | 说明 |
|--------|------|------|
| `taskbar:getSettings` | 渲染 → 主进程 | 获取任务栏设置 |
| `taskbar:setEnabled` | 渲染 → 主进程 | 设置任务栏启用状态 |
| `taskbar:updateSettings` | 渲染 → 主进程 | 更新任务栏设置 |
| `taskbar:setAlignment` | 渲染 → 主进程 | 设置任务栏对齐方式 |

#### 响应/广播消息

| Action | 方向 | 说明 |
|--------|------|------|
| `taskbar:settingsUpdated` | 主进程 → 渲染 | 设置更新通知 |
| `taskbar:alignmentUpdated` | 主进程 → 渲染 | 对齐方式更新通知 |

#### 数据结构

**任务栏设置：**
```json
{
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
```

### D. 原生服务消息

#### 请求消息

| Action | 方向 | 说明 |
|--------|------|------|
| `nativeService:getCPUUsage` | 渲染 → 主进程 | 获取 CPU 使用率 |
| `nativeService:getMemoryUsage` | 渲染 → 主进程 | 获取内存使用情况 |
| `nativeService:getBatteryStatus` | 渲染 → 主进程 | 获取电池状态 |
| `nativeService:getVolumeLevel` | 渲染 → 主进程 | 获取音量级别 |
| `nativeService:getAllStats` | 渲染 → 主进程 | 获取所有系统指标 |

#### 响应数据结构

**CPU 使用率：**
```json
{
  "type": "cpu",
  "usage": 45.2
}
```

**内存使用：**
```json
{
  "type": "memory",
  "total": 16777216000,
  "available": 8388608000,
  "used": 8388608000,
  "usage": 50.0
}
```

**电池状态：**
```json
{
  "type": "battery",
  "acOnline": true,
  "batteryPresent": true,
  "batteryLifePercent": 85,
  "batteryLifeTime": 3600,
  "batteryFullLifeTime": 4200
}
```

**音量级别：**
```json
{
  "type": "volume",
  "level": 75.0,
  "muted": false
}
```

### E. 媒体控制消息

#### 请求消息

| Action | 方向 | 说明 |
|--------|------|------|
| `media:getCurrentTrack` | 渲染 → 主进程 | 获取当前播放曲目 |
| `media:play` | 渲染 → 主进程 | 播放媒体 |
| `media:pause` | 渲染 → 主进程 | 暂停媒体 |
| `media:toggle` | 渲染 → 主进程 | 切换播放/暂停 |
| `media:next` | 渲染 → 主进程 | 下一曲 |
| `media:previous` | 渲染 → 主进程 | 上一曲 |
| `media:seek` | 渲染 → 主进程 | 跳转到指定位置 |

#### 响应/广播消息

| Action | 方向 | 说明 |
|--------|------|------|
| `media-update` | 主进程 → 渲染 | 媒体状态更新通知 |

## 错误码定义

| 错误码 | 含义 | 说明 |
|--------|------|------|
| 0 | SUCCESS | 操作成功 |
| 1001 | PIPE_CREATE_FAILED | 创建命名管道失败 |
| 1002 | PIPE_CONNECT_FAILED | 连接命名管道失败 |
| 1003 | READ_FAILED | 读取数据失败 |
| 1004 | WRITE_FAILED | 写入数据失败 |
| 2001 | JSON_PARSE_ERROR | JSON 解析错误 |
| 2002 | UNKNOWN_ACTION | 未知操作类型 |
| 3001 | COM_INIT_FAILED | COM 初始化失败 |
| 3002 | DEVICE_ENUM_CREATE_FAILED | 设备枚举创建失败 |
| 3003 | AUDIO_ENDPOINT_FAILED | 音频端点获取失败 |
| 4001 | WINDOW_NOT_FOUND | 窗口不存在 |
| 4002 | SERVICE_NOT_RUNNING | 原生服务未运行 |

## 进程间通信（Electron ↔ C++ 服务）

### 通信方式

使用 Windows 命名管道（Named Pipe）进行通信。

**管道名称：** `\\.\pipe\NativeServicePipe`

### 消息格式

```json
{
  "id": 1,
  "action": "get_cpu",
  "timestamp": 1699900000000
}
```

### 支持的操作

| Action | 说明 |
|--------|------|
| `get_cpu` | 获取 CPU 使用率 |
| `get_memory` | 获取内存使用情况 |
| `get_battery` | 获取电池状态 |
| `get_volume` | 获取音量级别 |
| `get_all` | 获取所有系统指标 |
| `ping` | 服务健康检查 |

## 安全注意事项

1. **输入验证**：所有来自渲染进程的消息必须进行验证
2. **权限控制**：敏感操作需要权限检查
3. **数据加密**：敏感数据传输应加密
4. **错误处理**：完善的错误处理和日志记录

## 兼容性

本协议设计为向后兼容，新增字段不会影响现有功能。