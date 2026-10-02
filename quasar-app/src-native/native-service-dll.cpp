#define NATIVE_SERVICE_DLL_EXPORTS
#include "native-service-dll.h"

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>
#include <iostream>
#include <string>
#include <memory>
#include <psapi.h>
#include <powrprof.h>
#include <endpointvolume.h>
#include <mmdeviceapi.h>

#undef GetObject
#undef IUnknown
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Media.Control.h>

#include "media-monitor/media-monitor.h"
#include "json_helper.h"
#include "taskbar/taskbar-utils.h"

#pragma comment(lib, "psapi.lib")
#pragma comment(lib, "powrprof.lib")
#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "oleaut32.lib")

using namespace winrt;
using namespace winrt::Windows::Media::Control;
using namespace winrt::Windows::Foundation;

// 全局实例
static std::unique_ptr<MediaMonitor> g_mediaMonitor = nullptr;
static bool g_initialized = false;

// 字符串转换辅助函数
std::string WStringToString(const std::wstring& wstr) {
    if (wstr.empty()) return std::string();
    int sizeNeeded = WideCharToMultiByte(CP_UTF8, 0, wstr.c_str(), (int)wstr.length(), 
                                          nullptr, 0, nullptr, nullptr);
    std::string result(sizeNeeded, 0);
    WideCharToMultiByte(CP_UTF8, 0, wstr.c_str(), (int)wstr.length(), 
                       &result[0], sizeNeeded, nullptr, nullptr);
    return result;
}

std::wstring StringToWString(const std::string& str) {
    if (str.empty()) return std::wstring();
    int sizeNeeded = MultiByteToWideChar(CP_UTF8, 0, str.c_str(), (int)str.length(), 
                                         nullptr, 0);
    std::wstring result(sizeNeeded, 0);
    MultiByteToWideChar(CP_UTF8, 0, str.c_str(), (int)str.length(), 
                       &result[0], sizeNeeded);
    return result;
}

// CPU信息获取
float GetCPUUsage() {
    FILETIME idleTime, kernelTime, userTime;
    if (!GetSystemTimes(&idleTime, &kernelTime, &userTime)) {
        return 0.0f;
    }

    ULONGLONG idle = (ULONGLONG)idleTime.dwHighDateTime << 32 | idleTime.dwLowDateTime;
    ULONGLONG kernel = (ULONGLONG)kernelTime.dwHighDateTime << 32 | kernelTime.dwLowDateTime;
    ULONGLONG user = (ULONGLONG)userTime.dwHighDateTime << 32 | userTime.dwLowDateTime;
    ULONGLONG total = kernel + user;

    static ULONGLONG prevIdle = 0, prevTotal = 0;
    ULONGLONG diffIdle = idle - prevIdle;
    ULONGLONG diffTotal = total - prevTotal;

    prevIdle = idle;
    prevTotal = total;

    double usage = diffTotal > 0 ? (1.0 - (double)diffIdle / diffTotal) * 100 : 0;
    return (float)usage;
}

// 内存信息获取
void GetMemoryInfo(
    unsigned long long* total,
    unsigned long long* available,
    unsigned long long* used,
    float* usage
) {
    MEMORYSTATUSEX memInfo;
    memInfo.dwLength = sizeof(MEMORYSTATUSEX);
    
    if (!GlobalMemoryStatusEx(&memInfo)) {
        *total = 0;
        *available = 0;
        *used = 0;
        *usage = 0.0f;
        return;
    }

    *total = memInfo.ullTotalPhys;
    *available = memInfo.ullAvailPhys;
    *used = memInfo.ullTotalPhys - memInfo.ullAvailPhys;
    *usage = (float)((1.0 - (double)memInfo.ullAvailPhys / memInfo.ullTotalPhys) * 100);
}

// 电池信息获取
void GetBatteryInfo(
    bool* acOnline,
    bool* batteryPresent,
    unsigned char* batteryLifePercent,
    unsigned long* batteryLifeTime,
    unsigned long* batteryFullLifeTime
) {
    SYSTEM_POWER_STATUS powerStatus;
    if (!GetSystemPowerStatus(&powerStatus)) {
        *acOnline = false;
        *batteryPresent = false;
        *batteryLifePercent = 0;
        *batteryLifeTime = 0;
        *batteryFullLifeTime = 0;
        return;
    }

    *acOnline = (powerStatus.ACLineStatus == 1);
    *batteryPresent = (powerStatus.BatteryFlag != 0);
    *batteryLifePercent = (unsigned char)powerStatus.BatteryLifePercent;
    *batteryLifeTime = powerStatus.BatteryLifeTime;
    *batteryFullLifeTime = powerStatus.BatteryFullLifeTime;
}

// 音量信息获取
void GetVolumeInfo(
    float* level,
    bool* muted
) {
    *level = 0.0f;
    *muted = false;

    try {
        HRESULT hr = CoInitialize(NULL);
        if (FAILED(hr)) {
            return;
        }

        IMMDeviceEnumerator* pEnumerator = NULL;
        hr = CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL, 
                            CLSCTX_ALL, __uuidof(IMMDeviceEnumerator), 
                            (void**)&pEnumerator);
        
        if (FAILED(hr)) {
            CoUninitialize();
            return;
        }

        IMMDevice* pDevice = NULL;
        hr = pEnumerator->GetDefaultAudioEndpoint(eRender, eConsole, &pDevice);
        pEnumerator->Release();

        if (FAILED(hr)) {
            CoUninitialize();
            return;
        }

        IAudioEndpointVolume* pVolume = NULL;
        hr = pDevice->Activate(__uuidof(IAudioEndpointVolume), CLSCTX_ALL, 
                            NULL, (void**)&pVolume);
        pDevice->Release();

        if (FAILED(hr)) {
            CoUninitialize();
            return;
        }

        float volLevel;
        hr = pVolume->GetMasterVolumeLevelScalar(&volLevel);
        
        BOOL isMuted;
        pVolume->GetMute(&isMuted);

        pVolume->Release();
        CoUninitialize();

        *level = volLevel * 100;
        *muted = (isMuted == TRUE);
    } catch (...) {
        return;
    }
}

// DLL导出函数实现

NATIVE_SERVICE_API bool __stdcall NativeService_Init() {
    if (g_initialized) {
        return true;
    }

    try {
        // 初始化WinRT
        winrt::init_apartment(winrt::apartment_type::single_threaded);
        
        // 创建媒体监控器
        g_mediaMonitor = std::make_unique<MediaMonitor>();
        g_mediaMonitor->Start();
        
        g_initialized = true;
        return true;
    } catch (const winrt::hresult_error& e) {
        return false;
    } catch (...) {
        return false;
    }
}

NATIVE_SERVICE_API void __stdcall NativeService_Shutdown() {
    if (g_mediaMonitor) {
        g_mediaMonitor->Stop();
        g_mediaMonitor.reset();
    }
    
    if (g_initialized) {
        winrt::uninit_apartment();
        g_initialized = false;
    }
}

NATIVE_SERVICE_API float __stdcall NativeService_GetCPUUsage() {
    return GetCPUUsage();
}

NATIVE_SERVICE_API void __stdcall NativeService_GetMemoryInfo(
    unsigned long long* total,
    unsigned long long* available,
    unsigned long long* used,
    float* usage
) {
    GetMemoryInfo(total, available, used, usage);
}

NATIVE_SERVICE_API void __stdcall NativeService_GetBatteryInfo(
    bool* acOnline,
    bool* batteryPresent,
    unsigned char* batteryLifePercent,
    unsigned long* batteryLifeTime,
    unsigned long* batteryFullLifeTime
) {
    GetBatteryInfo(acOnline, batteryPresent, batteryLifePercent, batteryLifeTime, batteryFullLifeTime);
}

NATIVE_SERVICE_API void __stdcall NativeService_GetVolumeInfo(
    float* level,
    bool* muted
) {
    GetVolumeInfo(level, muted);
}

NATIVE_SERVICE_API int __stdcall NativeService_GetMediaInfo(
    char* buffer,
    int bufferSize
) {
    if (!g_mediaMonitor || !buffer || bufferSize <= 0) {
        return 0;
    }

    try {
        auto mediaData = g_mediaMonitor->getMediaInfo();
        std::string jsonStr = mediaData.dump();
        
        if (jsonStr.length() >= (size_t)bufferSize) {
            return 0; // 缓冲区不足
        }

        strcpy_s(buffer, bufferSize, jsonStr.c_str());
        return (int)jsonStr.length();
    } catch (...) {
        return 0;
    }
}

NATIVE_SERVICE_API bool __stdcall NativeService_HideSystemTaskbar() {
    TaskbarUtils::HideSystemTaskbar();
    return true;
}

NATIVE_SERVICE_API bool __stdcall NativeService_ShowSystemTaskbar() {
    TaskbarUtils::ShowSystemTaskbar();
    return true;
}

NATIVE_SERVICE_API int __stdcall NativeService_GetPinnedApps(
    char* buffer,
    int bufferSize
) {
    if (!buffer || bufferSize <= 0) {
        return 0;
    }

    try {
        auto apps = TaskbarUtils::GetPinnedApps();
        json result;

        for (const auto& app : apps) {
            json appJson;
            json_set(appJson, "id", WStringToString(app.id));
            json_set(appJson, "name", WStringToString(app.name));
            json_set(appJson, "path", WStringToString(app.path));
            json_set(appJson, "iconWidth", app.icon.width);
            json_set(appJson, "iconHeight", app.icon.height);

            if (app.icon.valid && !app.icon.pixels.empty()) {
                static const char* base64Chars =
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
                const unsigned char* data = app.icon.pixels.data();
                size_t len = app.icon.pixels.size();
                std::string iconBase64;

                for (size_t i = 0; i < len; i += 3) {
                    int a = data[i];
                    int b = (i + 1 < len) ? data[i + 1] : 0;
                    int c = (i + 2 < len) ? data[i + 2] : 0;

                    iconBase64.push_back(base64Chars[(a >> 2) & 0x3F]);
                    iconBase64.push_back(base64Chars[((a << 4) | (b >> 4)) & 0x3F]);
                    iconBase64.push_back(base64Chars[((b << 2) | (c >> 6)) & 0x3F]);
                    iconBase64.push_back(base64Chars[c & 0x3F]);
                }

                size_t padding = (len % 3);
                if (padding > 0) {
                    for (size_t p = 0; p < 3 - padding; ++p) {
                        iconBase64[iconBase64.size() - 1 - p] = '=';
                    }
                }

                json_set(appJson, "iconData", iconBase64);
            } else {
                json_set(appJson, "iconData", "");
            }

            result.push_back(appJson);
        }

        std::string jsonStr = result.dump();
        
        if (jsonStr.length() >= (size_t)bufferSize) {
            return 0; // 缓冲区不足
        }

        strcpy_s(buffer, bufferSize, jsonStr.c_str());
        return (int)jsonStr.length();
    } catch (...) {
        return 0;
    }
}

NATIVE_SERVICE_API int __stdcall NativeService_ExtractIcon(
    const char* filePath,
    int iconSize,
    char* buffer,
    int bufferSize
) {
    if (!filePath || !buffer || bufferSize <= 0) {
        return 0;
    }

    try {
        std::wstring wFilePath = StringToWString(filePath);
        auto iconData = TaskbarUtils::ExtractIconFromExe(wFilePath, iconSize);

        json result;
        json_set(result, "success", iconData.valid);
        json_set(result, "width", iconData.width);
        json_set(result, "height", iconData.height);

        if (iconData.valid && !iconData.pixels.empty()) {
            static const char* base64Chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
            const unsigned char* data = iconData.pixels.data();
            size_t len = iconData.pixels.size();
            std::string iconBase64;

            for (size_t i = 0; i < len; i += 3) {
                int a = data[i];
                int b = (i + 1 < len) ? data[i + 1] : 0;
                int c = (i + 2 < len) ? data[i + 2] : 0;

                iconBase64.push_back(base64Chars[(a >> 2) & 0x3F]);
                iconBase64.push_back(base64Chars[((a << 4) | (b >> 4)) & 0x3F]);
                iconBase64.push_back(base64Chars[((b << 2) | (c >> 6)) & 0x3F]);
                iconBase64.push_back(base64Chars[c & 0x3F]);
            }

            size_t padding = (len % 3);
            if (padding > 0) {
                for (size_t p = 0; p < 3 - padding; ++p) {
                    iconBase64[iconBase64.size() - 1 - p] = '=';
                }
            }

            json_set(result, "iconData", iconBase64);
        } else {
            json_set(result, "iconData", "");
        }

        std::string jsonStr = result.dump();
        
        if (jsonStr.length() >= (size_t)bufferSize) {
            return 0; // 缓冲区不足
        }

        strcpy_s(buffer, bufferSize, jsonStr.c_str());
        return (int)jsonStr.length();
    } catch (...) {
        return 0;
    }
}

NATIVE_SERVICE_API int __stdcall NativeService_ParseLnk(
    const char* lnkPath,
    char* buffer,
    int bufferSize
) {
    if (!lnkPath || !buffer || bufferSize <= 0) {
        return 0;
    }

    try {
        std::wstring wLnkPath = StringToWString(lnkPath);
        auto lnkInfo = TaskbarUtils::ParseLnkFile(wLnkPath);

        json result;
        json_set(result, "success", lnkInfo.valid);
        json_set(result, "targetPath", WStringToString(lnkInfo.targetPath));
        json_set(result, "arguments", WStringToString(lnkInfo.arguments));
        json_set(result, "workingDirectory", WStringToString(lnkInfo.workingDirectory));
        json_set(result, "description", WStringToString(lnkInfo.description));
        json_set(result, "iconPath", WStringToString(lnkInfo.iconPath));
        json_set(result, "iconIndex", lnkInfo.iconIndex);

        std::string jsonStr = result.dump();
        
        if (jsonStr.length() >= (size_t)bufferSize) {
            return 0; // 缓冲区不足
        }

        strcpy_s(buffer, bufferSize, jsonStr.c_str());
        return (int)jsonStr.length();
    } catch (...) {
        return 0;
    }
}

NATIVE_SERVICE_API int __stdcall NativeService_GetActiveWindows(
    char* buffer,
    int bufferSize
) {
    if (!buffer || bufferSize <= 0) {
        return 0;
    }

    try {
        auto windows = TaskbarUtils::GetActiveWindows();
        json result;

        for (const auto& window : windows) {
            json windowJson;
            json_set(windowJson, "handle", (unsigned long long)window.handle);
            json_set(windowJson, "title", WStringToString(window.title));
            json_set(windowJson, "processPath", WStringToString(window.processPath));
            json_set(windowJson, "pid", (unsigned int)window.processId);
            json_set(windowJson, "isForeground", window.isForeground);

            result.push_back(windowJson);
        }

        std::string jsonStr = result.dump();
        
        if (jsonStr.length() >= (size_t)bufferSize) {
            return 0; // 缓冲区不足
        }

        strcpy_s(buffer, bufferSize, jsonStr.c_str());
        return (int)jsonStr.length();
    } catch (...) {
        return 0;
    }
}

NATIVE_SERVICE_API bool __stdcall NativeService_ActivateWindow(
    unsigned long long hWnd
) {
    try {
        return TaskbarUtils::ActivateWindow((HWND)hWnd);
    } catch (...) {
        return false;
    }
}

NATIVE_SERVICE_API bool __stdcall NativeService_CloseWindow(
    unsigned long long hWnd
) {
    try {
        return TaskbarUtils::CloseWindow((HWND)hWnd);
    } catch (...) {
        return false;
    }
}

// DLL入口点
BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            break;
        case DLL_THREAD_ATTACH:
            break;
        case DLL_THREAD_DETACH:
            break;
        case DLL_PROCESS_DETACH:
            NativeService_Shutdown();
            break;
    }
    return TRUE;
}