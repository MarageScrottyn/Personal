# NativeService C++ 程序编译指南

## 📍 文件位置

- **可执行文件**: `src-native\build\Release\NativeService.exe`
- **源代码**: `src-native\native-service.cpp`
- **CMake 配置**: `src-native\CMakeLists.txt`
- **FlatBuffers Schema**: `src-native\schema\service.fbs`

---

## 🚀 快速重新编译

### 方法一：使用命令行（推荐）

```powershell
# 1. 进入构建目录
cd "g:\Personal\quasar-app\src-native\build"

# 2. 配置 CMake（如果 CMakeLists.txt 有更改）
cmake ..

# 3. 编译
cmake --build . --config Release
```

### 方法二：使用 Visual Studio

1. 打开 `g:\Personal\quasar-app\src-native\build\NativeService.sln`
2. 选择 **Release** 配置
3. 按 **Ctrl+Shift+B** 或点击 **生成 → 重新生成解决方案**

---

## ⚙️ 编译配置说明

### CMakeLists.txt 关键配置

- **编译器**: MSVC C++17
- **Windows SDK**: 10.0.26100.0
- **依赖库**:
  - FlatBuffers v24.3.25 (通过 FetchContent 从 Gitee 镜像获取)
  - nlohmann/json.hpp (本地版本)
  - Windows Media Control APIs

### 生成的文件

- `NativeService.exe` - 主程序
- `flatc.exe` - FlatBuffers 编译器
- `service_generated.h` - FlatBuffers 生成的 C++ 头文件

---

## 🔧 常见问题

### 问题 1: CMake 配置失败 - 无法访问 GitHub

**原因**: 网络问题，无法从 GitHub 克隆 flatbuffers

**解决方案**: 
```cmake
# CMakeLists.txt 中已修改为使用 Gitee 镜像
GIT_REPOSITORY https://gitee.com/mirrors/flatbuffers.git
```

### 问题 2: 编译错误 - Offset 类型不匹配

**原因**: FlatBuffers 的 CreateResponse 函数需要直接的 Offset 值，不是指针

**错误代码**:
```cpp
// ❌ 错误
CreateResponse(builder, id, status, 0, &cpuOffset, &memoryOffset, ...)

// ✅ 正确
CreateResponse(builder, id, status, 0, cpuOffset, memoryOffset, ...)
```

### 问题 3: 找不到 nlohmann/json

**解决方案**: 
```cmake
# 确保包含本地 nlohmann 目录
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/nlohmann)
```

---

## 📋 编译步骤详解

### 1. 清理构建目录（可选）

```powershell
# 完整清理
Remove-Item -Recurse -Force "g:\Personal\quasar-app\src-native\build\*"

# 重新创建 build 目录
New-Item -ItemType Directory -Path "g:\Personal\quasar-app\src-native\build"
```

### 2. 配置 CMake

```powershell
cd "g:\Personal\quasar-app\src-native\build"
cmake ..
```

**预期输出**:
```
-- Building for: Visual Studio 17 2022
-- Proceeding with version: 24.3.25.0
-- Configuring done
-- Generating done
-- Build files have been written to: G:/Personal/quasar-app/src-native/build
```

### 3. 编译项目

```powershell
cmake --build . --config Release
```

**预期输出**:
```
flatc.vcxproj -> G:\Personal\quasar-app\src-native\build\_deps\flatbuffers-build\Release\flatc.exe
flatbuffers.vcxproj -> G:\Personal\quasar-app\src-native\build\_deps\flatbuffers-build\Release\flatbuffers.lib
NativeService.vcxproj -> G:\Personal\quasar-app\src-native\build\Release\NativeService.exe
```

### 4. 验证生成的可执行文件

```powershell
Get-ChildItem "g:\Personal\quasar-app\src-native\build\Release\NativeService.exe"
```

**预期结果**:
```
Name              Length LastWriteTime     
----              ------ -------------
NativeService.exe 128512 2026/5/24 11:46:30
```

---

## 🔍 调试技巧

### 查看编译日志

```powershell
cmake --build . --config Release --verbose 2>&1 | Tee-Object -FilePath build.log
```

### 使用 Visual Studio 调试

1. 在 Visual Studio 中打开解决方案
2. 设置 `NativeService.exe` 为启动项目
3. 设置断点
4. 按 F5 开始调试

### 检查依赖项

```powershell
# 使用 dumpbin 查看依赖
dumpbin /DEPENDENTS "g:\Personal\quasar-app\src-native\build\Release\NativeService.exe"
```

---

## 📦 构建输出结构

```
src-native\build\
├── Release\
│   ├── NativeService.exe          # 主程序
│   └── *.pdb                      # 调试符号
├── _deps\
│   ├── flatbuffers-src\           # FlatBuffers 源码
│   │   ├── include\
│   │   │   └── flatbuffers\       # FlatBuffers 头文件
│   │   └── src\
│   ├── flatbuffers-build\         # FlatBuffers 构建
│   │   └── Release\
│   │       ├── flatc.exe          # FlatBuffers 编译器
│   │       └── flatbuffers.lib    # FlatBuffers 库
│   └── flatbuffers-subbuild\      # FetchContent 子构建
├── generated\                      # FlatBuffers 生成的代码
│   └── service_generated.h
├── CMakeCache.txt
├── CMakeFiles\
├── NativeService.sln
└── NativeService.vcxproj
```

---

## 🎯 功能验证

编译完成后，可以运行以下命令测试 NativeService：

```powershell
# 进入构建目录
cd "g:\Personal\quasar-app\src-native\build\Release"

# 运行测试（需要先启动 Electron 应用）
.\NativeService.exe
```

**预期输出**:
```
[NativeService] Starting IPC service...
[NativeService] Named pipe created: \\.\pipe\NativeServicePipe
[NativeService] Waiting for connections...
```

---

## 📚 相关文档

- **FlatBuffers 文档**: https://google.github.io/flatbuffers/
- **CMake 教程**: https://cmake.org/cmake/help/latest/
- **Windows Media Control**: https://docs.microsoft.com/en-us/windows/win32/api/mediaendpoint/

---

## 🔄 版本信息

- **最后编译时间**: 2026-05-24
- **FlatBuffers 版本**: v24.3.25
- **C++ 标准**: C++17
- **Windows SDK**: 10.0.26100.0
- **编译器**: MSVC 19.44.35211.0

---

## 💡 提示

1. **编译前清理**: 如果遇到奇怪的编译错误，先清理构建目录
2. **检查 CMakeLists.txt**: 修改源代码后，确保 CMakeLists.txt 中的源文件列表是最新的
3. **FlatBuffers Schema**: 修改 `service.fbs` 后会自动重新生成 `service_generated.h`
4. **网络问题**: 如果 FetchContent 失败，可以手动克隆 flatbuffers 到 `_deps` 目录

---

## 🆘 获取帮助

如果遇到编译问题：

1. 检查错误信息中的行号
2. 查看 CMake 配置日志
3. 确保所有依赖都已正确安装
4. 检查 Windows SDK 是否正确安装

---

**Happy Coding!** 🎉
