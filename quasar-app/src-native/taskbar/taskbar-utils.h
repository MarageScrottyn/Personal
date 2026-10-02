#pragma once

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <string>
#include <vector>
#include <memory>

struct IShellLinkW;
struct IPersistFile;

struct IconData {
    std::vector<uint8_t> pixels;
    int width;
    int height;
    bool valid;
};

struct LnkInfo {
    std::wstring targetPath;
    std::wstring arguments;
    std::wstring workingDirectory;
    std::wstring description;
    std::wstring iconPath;
    int iconIndex;
    bool valid;
};

struct PinnedApp {
    std::wstring id;
    std::wstring name;
    std::wstring path;
    IconData icon;
};

struct ActiveWindow {
    HWND handle;
    std::wstring title;
    std::wstring processPath;
    DWORD processId;
    bool isForeground;
};

class TaskbarUtils {
public:
    static IconData ExtractIconFromExe(const std::wstring& filePath, int iconSize = 48);
    static LnkInfo ParseLnkFile(const std::wstring& lnkPath);
    static std::vector<PinnedApp> GetPinnedApps();
    static bool HideSystemTaskbar();
    static bool ShowSystemTaskbar();
    static std::vector<ActiveWindow> GetActiveWindows();
    static bool ActivateWindow(HWND hWnd);
    static bool CloseWindow(HWND hWnd);
    
private:
    static std::wstring GetAppDisplayName(const std::wstring& lnkPath, const LnkInfo& lnkInfo);
    static std::vector<uint8_t> IconToPng(HICON hIcon, int width, int height);
    static std::wstring GetTaskbarPinnedPath();
};
