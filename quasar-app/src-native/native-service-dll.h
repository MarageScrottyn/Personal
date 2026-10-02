#ifndef NATIVE_SERVICE_DLL_H
#define NATIVE_SERVICE_DLL_H

#ifdef NATIVE_SERVICE_DLL_EXPORTS
#define NATIVE_SERVICE_API __declspec(dllexport)
#else
#define NATIVE_SERVICE_API __declspec(dllimport)
#endif

#include <windows.h>

#ifdef __cplusplus
extern "C" {
#endif

// 初始化DLL，必须在调用其他函数前调用
NATIVE_SERVICE_API bool __stdcall NativeService_Init();

// 释放DLL资源
NATIVE_SERVICE_API void __stdcall NativeService_Shutdown();

// 获取CPU使用率
// 返回: 使用率百分比 (0-100)
NATIVE_SERVICE_API float __stdcall NativeService_GetCPUUsage();

// 获取内存信息
// 参数:
//   total - 总内存字节数
//   available - 可用内存字节数
//   used - 已用内存字节数
//   usage - 使用率百分比
NATIVE_SERVICE_API void __stdcall NativeService_GetMemoryInfo(
    unsigned long long* total,
    unsigned long long* available,
    unsigned long long* used,
    float* usage
);

// 获取电池状态
// 参数:
//   acOnline - 是否交流电源
//   batteryPresent - 是否有电池
//   batteryLifePercent - 电池电量百分比
//   batteryLifeTime - 剩余时间秒数
//   batteryFullLifeTime - 满电时间秒数
NATIVE_SERVICE_API void __stdcall NativeService_GetBatteryInfo(
    bool* acOnline,
    bool* batteryPresent,
    unsigned char* batteryLifePercent,
    unsigned long* batteryLifeTime,
    unsigned long* batteryFullLifeTime
);

// 获取音量信息
// 参数:
//   level - 音量级别 (0-100)
//   muted - 是否静音
NATIVE_SERVICE_API void __stdcall NativeService_GetVolumeInfo(
    float* level,
    bool* muted
);

// 获取媒体信息
// 参数:
//   buffer - 输出缓冲区，用于存储JSON字符串
//   bufferSize - 缓冲区大小
// 返回: 实际写入的字节数，0表示失败
NATIVE_SERVICE_API int __stdcall NativeService_GetMediaInfo(
    char* buffer,
    int bufferSize
);

// 隐藏系统任务栏
NATIVE_SERVICE_API bool __stdcall NativeService_HideSystemTaskbar();

// 显示系统任务栏
NATIVE_SERVICE_API bool __stdcall NativeService_ShowSystemTaskbar();

// 获取固定应用列表
// 参数:
//   buffer - 输出缓冲区，用于存储JSON字符串
//   bufferSize - 缓冲区大小
// 返回: 实际写入的字节数，0表示失败
NATIVE_SERVICE_API int __stdcall NativeService_GetPinnedApps(
    char* buffer,
    int bufferSize
);

// 从可执行文件提取图标
// 参数:
//   filePath - 文件路径
//   iconSize - 图标大小
//   buffer - 输出缓冲区，用于存储JSON字符串（包含base64编码的图标数据）
//   bufferSize - 缓冲区大小
// 返回: 实际写入的字节数，0表示失败
NATIVE_SERVICE_API int __stdcall NativeService_ExtractIcon(
    const char* filePath,
    int iconSize,
    char* buffer,
    int bufferSize
);

// 解析快捷方式文件
// 参数:
//   lnkPath - 快捷方式路径
//   buffer - 输出缓冲区，用于存储JSON字符串
//   bufferSize - 缓冲区大小
// 返回: 实际写入的字节数，0表示失败
NATIVE_SERVICE_API int __stdcall NativeService_ParseLnk(
    const char* lnkPath,
    char* buffer,
    int bufferSize
);

// 获取活动窗口列表
// 参数:
//   buffer - 输出缓冲区，用于存储JSON字符串
//   bufferSize - 缓冲区大小
// 返回: 实际写入的字节数，0表示失败
NATIVE_SERVICE_API int __stdcall NativeService_GetActiveWindows(
    char* buffer,
    int bufferSize
);

// 激活窗口
// 参数:
//   hWnd - 窗口句柄
// 返回: 是否成功
NATIVE_SERVICE_API bool __stdcall NativeService_ActivateWindow(
    unsigned long long hWnd
);

// 关闭窗口
// 参数:
//   hWnd - 窗口句柄
// 返回: 是否成功
NATIVE_SERVICE_API bool __stdcall NativeService_CloseWindow(
    unsigned long long hWnd
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_SERVICE_DLL_H