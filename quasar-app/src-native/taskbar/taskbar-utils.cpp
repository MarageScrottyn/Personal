#include "taskbar-utils.h"
#include <iostream>
#include <fstream>
#include <filesystem>
#include <unordered_set>
#include <comdef.h>
#include <gdiplus.h>
#include <codecvt>
#include <locale>
#include <shlobj.h>
#include <shobjidl.h>
#include <shellapi.h>
#include <psapi.h>

#pragma comment(lib, "gdiplus.lib")
#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "oleaut32.lib")

using namespace Gdiplus;
namespace fs = std::filesystem;

static ULONG_PTR gdiplusToken = 0;
static bool gdiplusInitialized = false;

static void InitGdiplus() {
    if (!gdiplusInitialized) {
        GdiplusStartupInput gdiplusStartupInput;
        GdiplusStartup(&gdiplusToken, &gdiplusStartupInput, NULL);
        gdiplusInitialized = true;
    }
}

static void ShutdownGdiplus() {
    if (gdiplusInitialized) {
        GdiplusShutdown(gdiplusToken);
        gdiplusInitialized = false;
    }
}

static std::string WideStringToUTF8(const std::wstring& wstr) {
    if (wstr.empty()) return std::string();
    int size_needed = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
    std::string strTo(size_needed, 0);
    WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], size_needed, NULL, NULL);
    return strTo;
}

// 检测字符串是否包含乱码字符
static bool ContainsGarbledChars(const std::wstring& wstr) {
    for (wchar_t c : wstr) {
        if (c == 0xFFFD || 
            (c >= 0xDC00 && c <= 0xDFFF) ||
            (c >= 0xE000 && c <= 0xF8FF)) {
            return true;
        }
    }
    return false;
}

// 检测字符串是否更可能是GBK乱码而不是正常中文
static bool IsMoreLikelyGBKGarbled(const std::wstring& original, const std::wstring& fixed) {
    if (fixed.empty()) return false;
    if (fixed == original) return false;
    
    int origChineseCount = 0;
    int fixedChineseCount = 0;
    
    for (wchar_t c : original) {
        if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
            origChineseCount++;
        }
    }
    
    for (wchar_t c : fixed) {
        if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
            fixedChineseCount++;
        }
    }
    
    // 如果修复后中文字符增多，说明修复有效
    return fixedChineseCount > origChineseCount;
}

// 尝试修复GBK编码被错误解码为UTF-8的问题
static std::wstring FixGBKEncoding(const std::wstring& wstr) {
    // 直接尝试修复，不依赖复杂的检测
    int gbkSize = WideCharToMultiByte(CP_ACP, 0, wstr.c_str(), (int)wstr.length(), NULL, 0, NULL, NULL);
    if (gbkSize <= 0) {
        return wstr;
    }
    
    std::string gbkBytes(gbkSize, 0);
    WideCharToMultiByte(CP_ACP, 0, wstr.c_str(), (int)wstr.length(), &gbkBytes[0], gbkSize, NULL, NULL);
    
    int unicodeSize = MultiByteToWideChar(CP_ACP, 0, gbkBytes.c_str(), (int)gbkBytes.length(), NULL, 0);
    if (unicodeSize <= 0) {
        return wstr;
    }
    
    std::wstring fixedStr(unicodeSize, 0);
    MultiByteToWideChar(CP_ACP, 0, gbkBytes.c_str(), (int)gbkBytes.length(), &fixedStr[0], unicodeSize);
    
    // 如果修复后中文字符更多，或者原字符串有乱码但修复后没有，则使用修复后的
    if (IsMoreLikelyGBKGarbled(wstr, fixedStr) || (ContainsGarbledChars(wstr) && !ContainsGarbledChars(fixedStr))) {
        return fixedStr;
    }
    
    return wstr;
}

static std::wstring UTF8ToWideString(const std::string& str) {
    if (str.empty()) return std::wstring();
    int size_needed = MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), NULL, 0);
    std::wstring wstrTo(size_needed, 0);
    MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), &wstrTo[0], size_needed);
    return wstrTo;
}

// 将Bitmap转换为PNG格式的字节数组
static std::vector<uint8_t> BitmapToPng(Bitmap* bitmap) {
    std::vector<uint8_t> pngData;
    
    if (!bitmap || bitmap->GetLastStatus() != Ok) {
        return pngData;
    }
    
    IStream* pStream = NULL;
    HRESULT hr = CreateStreamOnHGlobal(NULL, TRUE, &pStream);
    if (FAILED(hr)) {
        return pngData;
    }
    
    CLSID pngClsid;
    hr = CLSIDFromString(L"{557cf406-1a04-11d3-9a73-0000f81ef32e}", &pngClsid);
    if (FAILED(hr)) {
        pStream->Release();
        return pngData;
    }
    
    Status status = bitmap->Save(pStream, &pngClsid, NULL);
    if (status == Ok) {
        STATSTG statstg;
        hr = pStream->Stat(&statstg, STATFLAG_NONAME);
        if (SUCCEEDED(hr)) {
            LARGE_INTEGER liZero = {0};
            pStream->Seek(liZero, STREAM_SEEK_SET, NULL);
            
            pngData.resize((size_t)statstg.cbSize.QuadPart);
            ULONG bytesRead = 0;
            pStream->Read(pngData.data(), (ULONG)pngData.size(), &bytesRead);
            pngData.resize(bytesRead);
        }
    }
    
    pStream->Release();
    return pngData;
}

IconData TaskbarUtils::ExtractIconFromExe(const std::wstring& filePath, int iconSize) {
    IconData result = { {}, 0, 0, false };
    
    if (filePath.empty()) {
        return result;
    }
    
    InitGdiplus();
    
    // 方法1: 使用 SHGetFileInfo 获取图标（Windows系统原生方法）
    SHFILEINFOW shfi = {0};
    UINT flags = SHGFI_ICON | SHGFI_USEFILEATTRIBUTES;
    
    if (iconSize >= 32) {
        flags |= SHGFI_LARGEICON;
    } else {
        flags |= SHGFI_SMALLICON;
    }
    
    HICON hIcon = NULL;
    
    // 首先尝试使用 SHGetFileInfo
    if (SHGetFileInfoW(filePath.c_str(), FILE_ATTRIBUTE_NORMAL, &shfi, sizeof(SHFILEINFOW), flags)) {
        hIcon = shfi.hIcon;
    }
    
    // 如果 SHGetFileInfo 失败，尝试 ExtractIconEx
    if (!hIcon) {
        UINT iconCount = ExtractIconExW(filePath.c_str(), 0, NULL, NULL, 0);
        if (iconCount > 0) {
            HICON hIconLarge = NULL;
            HICON hIconSmall = NULL;
            
            if (ExtractIconExW(filePath.c_str(), 0, &hIconLarge, &hIconSmall, 1) > 0) {
                hIcon = (iconSize >= 32) ? hIconLarge : hIconSmall;
                if (!hIcon) {
                    hIcon = hIconLarge ? hIconLarge : hIconSmall;
                }
                // 只释放未使用的图标
                if (hIcon == hIconLarge && hIconSmall) {
                    DestroyIcon(hIconSmall);
                } else if (hIcon == hIconSmall && hIconLarge) {
                    DestroyIcon(hIconLarge);
                }
            }
        }
    }
    
    if (!hIcon) {
        return result;
    }
    
    Bitmap* bitmap = Bitmap::FromHICON(hIcon);
    if (!bitmap || bitmap->GetLastStatus() != Ok) {
        if (bitmap) delete bitmap;
        DestroyIcon(hIcon);
        return result;
    }
    
    result.width = bitmap->GetWidth();
    result.height = bitmap->GetHeight();
    
    // 将位图转换为PNG格式
    result.pixels = BitmapToPng(bitmap);
    
    if (!result.pixels.empty()) {
        result.valid = true;
    }
    
    delete bitmap;
    DestroyIcon(hIcon);
    
    return result;
}

LnkInfo TaskbarUtils::ParseLnkFile(const std::wstring& lnkPath) {
    LnkInfo result = { L"", L"", L"", L"", L"", 0, false };
    
    HRESULT hr = CoInitialize(NULL);
    if (FAILED(hr)) {
        return result;
    }
    
    IShellLinkW* pShellLink = NULL;
    hr = CoCreateInstance(CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, IID_IShellLinkW, (void**)&pShellLink);
    
    if (FAILED(hr)) {
        CoUninitialize();
        return result;
    }
    
    IPersistFile* pPersistFile = NULL;
    hr = pShellLink->QueryInterface(IID_IPersistFile, (void**)&pPersistFile);
    
    if (FAILED(hr)) {
        pShellLink->Release();
        CoUninitialize();
        return result;
    }
    
    hr = pPersistFile->Load(lnkPath.c_str(), STGM_READ);
    if (FAILED(hr)) {
        pPersistFile->Release();
        pShellLink->Release();
        CoUninitialize();
        return result;
    }
    
    wchar_t targetPath[MAX_PATH] = {0};
    hr = pShellLink->GetPath(targetPath, MAX_PATH, NULL, SLGP_RAWPATH);
    if (SUCCEEDED(hr)) {
        result.targetPath = targetPath;
    }
    
    wchar_t arguments[1024] = {0};
    hr = pShellLink->GetArguments(arguments, 1024);
    if (SUCCEEDED(hr)) {
        result.arguments = arguments;
    }
    
    wchar_t workingDir[MAX_PATH] = {0};
    hr = pShellLink->GetWorkingDirectory(workingDir, MAX_PATH);
    if (SUCCEEDED(hr)) {
        result.workingDirectory = workingDir;
    }
    
    wchar_t description[1024] = {0};
    hr = pShellLink->GetDescription(description, 1024);
    if (SUCCEEDED(hr)) {
        result.description = description;
    }
    
    wchar_t iconPath[MAX_PATH] = {0};
    int iconIndex = 0;
    hr = pShellLink->GetIconLocation(iconPath, MAX_PATH, &iconIndex);
    if (SUCCEEDED(hr)) {
        result.iconPath = iconPath;
        result.iconIndex = iconIndex;
    }
    
    result.valid = !result.targetPath.empty();
    
    pPersistFile->Release();
    pShellLink->Release();
    CoUninitialize();
    
    return result;
}

std::wstring TaskbarUtils::GetTaskbarPinnedPath() {
    wchar_t* appDataPath = NULL;
    if (SUCCEEDED(SHGetKnownFolderPath(FOLDERID_RoamingAppData, 0, NULL, &appDataPath))) {
        std::wstring basePath = appDataPath;
        CoTaskMemFree(appDataPath);
        
        // 尝试多个可能的路径
        std::vector<std::wstring> possiblePaths = {
            basePath + L"\\Microsoft\\Internet Explorer\\Quick Launch\\User Pinned\\TaskBar",
            basePath + L"\\Microsoft\\Windows\\Start Menu\\Programs",
            basePath + L"\\Microsoft\\Windows\\TaskBar"
        };
        
        for (const auto& path : possiblePaths) {
            if (fs::exists(path)) {
                return path;
            }
        }
        
        // 默认返回传统路径
        return basePath + L"\\Microsoft\\Internet Explorer\\Quick Launch\\User Pinned\\TaskBar";
    }
    return L"";
}

std::wstring TaskbarUtils::GetAppDisplayName(const std::wstring& lnkPath, const LnkInfo& lnkInfo) {
    if (!lnkInfo.description.empty()) {
        // 尝试修复可能的GBK编码问题
        std::wstring fixedDescription = FixGBKEncoding(lnkInfo.description);
        
        bool hasGarbled = false;
        for (wchar_t c : fixedDescription) {
            if (c == 0xFFFD) {
                hasGarbled = true;
                break;
            }
        }
        if (!hasGarbled && fixedDescription != lnkInfo.targetPath) {
            return fixedDescription;
        }
    }
    
    fs::path p(lnkPath);
    std::wstring fileName = p.stem().wstring();
    // 同样修复文件名的编码问题
    std::wstring fixedFileName = FixGBKEncoding(fileName);
    if (!fixedFileName.empty()) {
        return fixedFileName;
    }
    
    if (!lnkInfo.targetPath.empty()) {
        fs::path target(lnkInfo.targetPath);
        std::wstring targetName = target.stem().wstring();
        return FixGBKEncoding(targetName);
    }
    
    return L"Unknown";
}

std::vector<PinnedApp> TaskbarUtils::GetPinnedApps() {
    std::vector<PinnedApp> apps;
    std::unordered_set<std::wstring> addedPaths; // 避免重复
    
    wchar_t* appDataPath = NULL;
    wchar_t* publicPath = NULL;
    wchar_t* userProfilePath = NULL;
    
    if (FAILED(SHGetKnownFolderPath(FOLDERID_RoamingAppData, 0, NULL, &appDataPath))) {
        OutputDebugStringW(L"[TaskbarUtils] Failed to get RoamingAppData path\n");
        return apps;
    }
    
    SHGetKnownFolderPath(FOLDERID_PublicDesktop, 0, NULL, &publicPath);
    SHGetKnownFolderPath(FOLDERID_Profile, 0, NULL, &userProfilePath);
    
    std::wstring baseAppData = appDataPath;
    CoTaskMemFree(appDataPath);
    
    // 尝试多个可能的路径（参考JavaScript端实现）
    std::vector<std::wstring> possiblePaths = {
        // 标准任务栏固定路径
        baseAppData + L"\\Microsoft\\Internet Explorer\\Quick Launch\\User Pinned\\TaskBar",
        // 备用路径
        baseAppData + L"\\Microsoft\\Windows\\TaskBar",
        // 开始菜单程序目录
        baseAppData + L"\\Microsoft\\Windows\\Start Menu\\Programs"
    };
    
    // 添加公共桌面路径（如果可用）
    if (publicPath) {
        possiblePaths.push_back(std::wstring(publicPath));
        CoTaskMemFree(publicPath);
    }
    
    // 添加用户桌面路径（如果可用）
    if (userProfilePath) {
        possiblePaths.push_back(std::wstring(userProfilePath) + L"\\Desktop");
        CoTaskMemFree(userProfilePath);
    }
    
    // 遍历所有路径
    for (const auto& taskbarPath : possiblePaths) {
        if (!fs::exists(taskbarPath)) {
            std::wstring msg = L"[TaskbarUtils] Path does not exist: " + taskbarPath + L"\n";
            OutputDebugStringW(msg.c_str());
            continue;
        }
        
        std::wstring scanMsg = L"[TaskbarUtils] Scanning path: " + taskbarPath + L"\n";
        OutputDebugStringW(scanMsg.c_str());
        
        try {
            bool foundLnk = false;
            for (const auto& entry : fs::directory_iterator(taskbarPath)) {
                if (entry.path().extension() == L".lnk") {
                    foundLnk = true;
                    std::wstring lnkPath = entry.path().wstring();
                    std::wstring foundMsg = L"[TaskbarUtils] Found .lnk file: " + lnkPath + L"\n";
                    OutputDebugStringW(foundMsg.c_str());
                    
                    LnkInfo lnkInfo = ParseLnkFile(lnkPath);
                    if (!lnkInfo.valid || lnkInfo.targetPath.empty()) {
                        std::wstring failMsg = L"[TaskbarUtils] Failed to parse or empty target: " + lnkPath + L"\n";
                        OutputDebugStringW(failMsg.c_str());
                        continue;
                    }
                    
                    // 避免重复
                    if (addedPaths.find(lnkInfo.targetPath) != addedPaths.end()) {
                        continue;
                    }
                    addedPaths.insert(lnkInfo.targetPath);
                    
                    PinnedApp app;
                    app.id = lnkPath;
                    app.path = lnkInfo.targetPath;
                    app.name = GetAppDisplayName(lnkPath, lnkInfo);
                    
                    std::wstring addMsg = L"[TaskbarUtils] Adding app: " + app.name + L" -> " + app.path + L"\n";
                    OutputDebugStringW(addMsg.c_str());
                    
                    std::wstring iconSource = !lnkInfo.iconPath.empty() ? lnkInfo.iconPath : lnkInfo.targetPath;
                    app.icon = ExtractIconFromExe(iconSource, 48);
                    
                    std::wstring validStr = app.icon.valid ? L"true" : L"false";
                    std::wstring iconMsg = L"[TaskbarUtils] Icon extracted: valid=" + validStr + L", size=" + 
                        std::to_wstring(app.icon.pixels.size()) + L"\n";
                    OutputDebugStringW(iconMsg.c_str());
                    
                    apps.push_back(app);
                }
            }
            if (!foundLnk) {
                std::wstring noLnkMsg = L"[TaskbarUtils] No .lnk files found in: " + taskbarPath + L"\n";
                OutputDebugStringW(noLnkMsg.c_str());
            }
        } catch (...) {
            std::wstring errMsg = L"[TaskbarUtils] Error scanning path: " + taskbarPath + L"\n";
            OutputDebugStringW(errMsg.c_str());
        }
    }
    
    std::wstring totalMsg = L"[TaskbarUtils] Total apps found: " + std::to_wstring(apps.size()) + L"\n";
    OutputDebugStringW(totalMsg.c_str());
    
    return apps;
}

bool TaskbarUtils::HideSystemTaskbar() {
    HWND hTaskbar = FindWindowW(L"Shell_TrayWnd", NULL);
    if (hTaskbar) {
        ShowWindow(hTaskbar, SW_HIDE);
        return true;
    }
    return false;
}

bool TaskbarUtils::ShowSystemTaskbar() {
    HWND hTaskbar = FindWindowW(L"Shell_TrayWnd", NULL);
    if (hTaskbar) {
        ShowWindow(hTaskbar, SW_SHOW);
        return true;
    }
    return false;
}

// 用于 EnumWindows 的回调数据
struct EnumWindowsData {
    std::vector<ActiveWindow>& windows;
    HWND foregroundWindow;
};

// EnumWindows 回调函数
static BOOL CALLBACK EnumWindowsProc(HWND hWnd, LPARAM lParam) {
    EnumWindowsData* data = reinterpret_cast<EnumWindowsData*>(lParam);
    
    // 跳过不可见窗口
    if (!IsWindowVisible(hWnd)) {
        return TRUE;
    }
    
    // 获取窗口标题
    wchar_t title[2048] = {0};
    GetWindowTextW(hWnd, title, sizeof(title) / sizeof(wchar_t));
    
    // 跳过标题为空的窗口（通常是一些系统窗口）
    if (wcslen(title) == 0) {
        return TRUE;
    }
    
    // 获取进程ID
    DWORD processId = 0;
    GetWindowThreadProcessId(hWnd, &processId);
    
    // 获取进程路径
    std::wstring processPath;
    HANDLE hProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, processId);
    if (hProcess) {
        wchar_t path[MAX_PATH] = {0};
        if (GetModuleFileNameExW(hProcess, NULL, path, MAX_PATH)) {
            processPath = path;
        }
        CloseHandle(hProcess);
    }
    
    // 判断是否是前台窗口
    bool isForeground = (hWnd == data->foregroundWindow);
    
    // 添加到列表
    ActiveWindow window;
    window.handle = hWnd;
    window.title = title;
    window.processPath = processPath;
    window.processId = processId;
    window.isForeground = isForeground;
    
    data->windows.push_back(window);
    
    return TRUE;
}

std::vector<ActiveWindow> TaskbarUtils::GetActiveWindows() {
    std::vector<ActiveWindow> windows;
    
    // 获取前台窗口
    HWND foregroundWindow = GetForegroundWindow();
    
    // 设置回调数据
    EnumWindowsData data = { windows, foregroundWindow };
    
    // 枚举所有窗口
    EnumWindows(EnumWindowsProc, reinterpret_cast<LPARAM>(&data));
    
    return data.windows;
}

bool TaskbarUtils::ActivateWindow(HWND hWnd) {
    if (!IsWindow(hWnd)) {
        return false;
    }
    
    // 显示窗口（如果最小化）
    ShowWindow(hWnd, SW_RESTORE);
    
    // 设置为前台窗口
    return SetForegroundWindow(hWnd) != FALSE;
}

bool TaskbarUtils::CloseWindow(HWND hWnd) {
    if (!IsWindow(hWnd)) {
        return false;
    }
    
    // 发送关闭消息
    return SendMessage(hWnd, WM_CLOSE, 0, 0) != 0;
}
