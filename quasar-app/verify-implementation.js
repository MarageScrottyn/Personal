#!/usr/bin/env node

/**
 * 代码验证脚本：检查所有IPC处理器是否正确实现
 * 
 * 这个脚本会扫描代码文件，检查必要的 IPC 处理器是否已实现
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('=== 代码完整性检查 ===\n');

// 需要检查的 IPC 处理器
const requiredHandlers = {
  'window-manager.js': [
    // 灵动岛相关
    { pattern: /dynamicIsland:setEnabled/i, name: '灵动岛-启用/禁用' },
    { pattern: /dynamicIsland:isEnabled/i, name: '灵动岛-获取启用状态' },
    { pattern: /dynamicIsland:expand/i, name: '灵动岛-展开' },
    { pattern: /dynamicIsland:collapse/i, name: '灵动岛-收起' },
    { pattern: /dynamicIsland:toggle/i, name: '灵动岛-切换' },
    { pattern: /dynamicIsland:updateStyle/i, name: '灵动岛-更新样式' },
    { pattern: /dynamicIsland:updatePosition/i, name: '灵动岛-更新位置' },
    { pattern: /dynamicIsland:getSettings/i, name: '灵动岛-获取设置' },
    { pattern: /dynamicIsland:setSettings/i, name: '灵动岛-设置设置' },
    { pattern: /dynamicIsland:openDevTools/i, name: '灵动岛-打开开发者工具' },
    { pattern: /dynamicIsland:closeDevTools/i, name: '灵动岛-关闭开发者工具' },
    { pattern: /dynamicIsland:setMusicControlEnabled/i, name: '灵动岛-音乐控制开关' },
    
    // 任务栏相关
    { pattern: /taskbar:setEnabled/i, name: '任务栏-启用/禁用' },
    { pattern: /taskbar:getSettings/i, name: '任务栏-获取设置' },
    { pattern: /taskbar:setPosition/i, name: '任务栏-设置位置' },
    { pattern: /taskbar:setStyle/i, name: '任务栏-设置样式' },
    { pattern: /taskbar:setDevToolsEnabled/i, name: '任务栏-开发者工具开关' },
    { pattern: /taskbar:openDevTools/i, name: '任务栏-打开开发者工具' },
    { pattern: /taskbar:updateSize/i, name: '任务栏-更新尺寸' },
    { pattern: /taskbar:setOpacity/i, name: '任务栏-设置透明度' },
    { pattern: /taskbar:setIconOpacity/i, name: '任务栏-设置图标透明度' },
    { pattern: /taskbar:setBlurRadius/i, name: '任务栏-设置模糊半径' },
    { pattern: /taskbar:setGlassStrength/i, name: '任务栏-设置磨砂强度' },
    { pattern: /taskbar:setIconColor/i, name: '任务栏-设置图标颜色' },
    { pattern: /taskbar:setBarColor/i, name: '任务栏-设置底栏颜色' },
    { pattern: /taskbar:setCornerType/i, name: '任务栏-设置圆角类型' },
    { pattern: /taskbar:setRoundedDockGap/i, name: '任务栏-设置Dock间隙' },
    { pattern: /taskbar:setRoundedCenterGap/i, name: '任务栏-设置中心间隙' },
    { pattern: /taskbar:setRoundedLeftGap/i, name: '任务栏-设置左侧间隙' },
    { pattern: /taskbar:setRoundedRightGap/i, name: '任务栏-设置右侧间隙' },
    { pattern: /taskbar:setAlignment/i, name: '任务栏-设置对齐方式' },
    { pattern: /taskbar:setIgnoreMouseEvents/i, name: '任务栏-设置忽略鼠标事件' },
    { pattern: /taskbar:setAutoHide/i, name: '任务栏-设置自动隐藏' },
    { pattern: /taskbar:setShowTime/i, name: '任务栏-设置显示时间' },
  ],
  
  'electron-preload.js': [
    // Preload 中需要暴露的 API
    { pattern: /dynamicIsland:\s*\{[^}]*setEnabled/i, name: 'Preload-灵动岛setEnabled' },
    { pattern: /dynamicIsland:\s*\{[^}]*setDevToolsEnabled/i, name: 'Preload-灵动岛setDevToolsEnabled' },
    { pattern: /taskbar:\s*\{[^}]*setEnabled/i, name: 'Preload-任务栏setEnabled' },
    { pattern: /taskbar:\s*\{[^}]*setDevToolsEnabled/i, name: 'Preload-任务栏setDevToolsEnabled' },
    { pattern: /taskbar:\s*\{[^}]*setIconOpacity/i, name: 'Preload-任务栏setIconOpacity' },
    { pattern: /taskbar:\s*\{[^}]*setBlurRadius/i, name: 'Preload-任务栏setBlurRadius' },
    { pattern: /taskbar:\s*\{[^}]*setGlassStrength/i, name: 'Preload-任务栏setGlassStrength' },
    { pattern: /taskbar:\s*\{[^}]*setIconColor/i, name: 'Preload-任务栏setIconColor' },
    { pattern: /taskbar:\s*\{[^}]*setBarColor/i, name: 'Preload-任务栏setBarColor' },
    { pattern: /taskbar:\s*\{[^}]*setCornerType/i, name: 'Preload-任务栏setCornerType' },
    { pattern: /taskbar:\s*\{[^}]*setRoundedDockGap/i, name: 'Preload-任务栏setRoundedDockGap' },
    { pattern: /taskbar:\s*\{[^}]*setRoundedCenterGap/i, name: 'Preload-任务栏setRoundedCenterGap' },
    { pattern: /taskbar:\s*\{[^}]*setRoundedLeftGap/i, name: 'Preload-任务栏setRoundedLeftGap' },
    { pattern: /taskbar:\s*\{[^}]*setRoundedRightGap/i, name: 'Preload-任务栏setRoundedRightGap' },
    { pattern: /taskbar:\s*\{[^}]*setAlignment/i, name: 'Preload-任务栏setAlignment' },
  ],
};

// 检查文件
let totalChecks = 0;
let passedChecks = 0;
let failedChecks = [];

for (const [file, checks] of Object.entries(requiredHandlers)) {
  const filePath = path.join(__dirname, 'src-electron', file);
  
  if (!fs.existsSync(filePath)) {
    console.log(`⚠️  文件不存在: ${file}`);
    continue;
  }
  
  const content = fs.readFileSync(filePath, 'utf-8');
  
  console.log(`\n检查文件: ${file}`);
  console.log('─'.repeat(50));
  
  for (const check of checks) {
    totalChecks++;
    const found = check.pattern.test(content);
    
    if (found) {
      console.log(`  ✅ ${check.name}`);
      passedChecks++;
    } else {
      console.log(`  ❌ ${check.name} - 未找到`);
      failedChecks.push({ file, check: check.name });
    }
  }
}

console.log('\n' + '='.repeat(50));
console.log('检查摘要');
console.log('='.repeat(50));
console.log(`总检查项: ${totalChecks}`);
console.log(`通过: ${passedChecks}`);
console.log(`失败: ${failedChecks.length}`);

if (failedChecks.length > 0) {
  console.log('\n❌ 未实现的处理器:');
  failedChecks.forEach(({ file, check }) => {
    console.log(`  - ${file}: ${check}`);
  });
  process.exit(1);
} else {
  console.log('\n✅ 所有处理器都已正确实现！');
  process.exit(0);
}
