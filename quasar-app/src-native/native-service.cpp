#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <iostream>
#include <string>
#include <thread>
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
#include "flatbuffers/flatbuffers.h"
#include "service_generated.h"
#include "taskbar/taskbar-utils.h"

#pragma comment(lib, "psapi.lib")
#pragma comment(lib, "powrprof.lib")
#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "oleaut32.lib")

const std::string PIPE_NAME = "\\\\.\\pipe\\NativeServicePipe";
const int BUFFER_SIZE = 4096;

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

using namespace winrt;
using namespace winrt::Windows::Media::Control;
using namespace winrt::Windows::Foundation;
using namespace NativeService;

class SystemMonitor {
public:
    MediaMonitor mediaMonitor;

    void fillCPUInfo(flatbuffers::FlatBufferBuilder& builder, CPUInfoBuilder& cpuBuilder) {
        FILETIME idleTime, kernelTime, userTime;
        if (!GetSystemTimes(&idleTime, &kernelTime, &userTime)) {
            return;
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
        cpuBuilder.add_usage((float)usage);
    }

    void fillMemoryInfo(flatbuffers::FlatBufferBuilder& builder, MemoryInfoBuilder& memBuilder) {
        MEMORYSTATUSEX memInfo;
        memInfo.dwLength = sizeof(MEMORYSTATUSEX);
        
        if (!GlobalMemoryStatusEx(&memInfo)) {
            return;
        }

        memBuilder.add_total(memInfo.ullTotalPhys);
        memBuilder.add_available(memInfo.ullAvailPhys);
        memBuilder.add_used(memInfo.ullTotalPhys - memInfo.ullAvailPhys);
        memBuilder.add_usage((float)((1.0 - (double)memInfo.ullAvailPhys / memInfo.ullTotalPhys) * 100));
    }

    void fillBatteryInfo(flatbuffers::FlatBufferBuilder& builder, BatteryInfoBuilder& batteryBuilder) {
        SYSTEM_POWER_STATUS powerStatus;
        if (!GetSystemPowerStatus(&powerStatus)) {
            return;
        }

        batteryBuilder.add_acOnline(powerStatus.ACLineStatus == 1);
        batteryBuilder.add_batteryPresent(powerStatus.BatteryFlag != 0);
        batteryBuilder.add_batteryLifePercent((uint8_t)powerStatus.BatteryLifePercent);
        batteryBuilder.add_batteryLifeTime(powerStatus.BatteryLifeTime);
        batteryBuilder.add_batteryFullLifeTime(powerStatus.BatteryFullLifeTime);
    }

    void fillVolumeInfo(flatbuffers::FlatBufferBuilder& builder, VolumeInfoBuilder& volBuilder) {
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

            float level;
            hr = pVolume->GetMasterVolumeLevelScalar(&level);
            
            BOOL isMuted;
            pVolume->GetMute(&isMuted);

            pVolume->Release();
            CoUninitialize();

            volBuilder.add_level(level * 100);
            volBuilder.add_muted(isMuted == TRUE);
        } catch (...) {
            return;
        }
    }

    void fillMediaInfo(flatbuffers::FlatBufferBuilder& builder, MediaInfoBuilder& mediaBuilder) {
        auto mediaData = mediaMonitor.getMediaInfo();
        
        std::string title = json_get(mediaData, "title", std::string(""));
        std::string artist = json_get(mediaData, "artist", std::string(""));
        std::string albumTitle = json_get(mediaData, "albumTitle", std::string(""));
        std::string albumArtist = json_get(mediaData, "albumArtist", std::string(""));
        bool playing = json_get(mediaData, "playing", false);
        double position = json_get(mediaData, "position", 0.0);
        double duration = json_get(mediaData, "duration", 0.0);

        mediaBuilder.add_title(builder.CreateString(title));
        mediaBuilder.add_artist(builder.CreateString(artist));
        mediaBuilder.add_albumTitle(builder.CreateString(albumTitle));
        mediaBuilder.add_albumArtist(builder.CreateString(albumArtist));
        mediaBuilder.add_playing(playing);
        mediaBuilder.add_position(position);
        mediaBuilder.add_duration(duration);
    }
};

class IPCService {
private:
    HANDLE hPipe;
    SystemMonitor monitor;
    bool running;

    bool readMessage(std::vector<uint8_t>& buffer) {
        buffer.resize(BUFFER_SIZE);
        DWORD bytesRead = 0;

        std::cout << "[NativeService] Waiting for data..." << std::endl;

        if (!ReadFile(hPipe, buffer.data(), BUFFER_SIZE, &bytesRead, NULL)) {
            DWORD error = GetLastError();
            std::cerr << "[NativeService] ReadFile failed, error: " << error << std::endl;
            return false;
        }

        buffer.resize(bytesRead);
        std::cout << "[NativeService] Read " << bytesRead << " bytes" << std::endl;
        return true;
    }

    bool sendMessage(const std::vector<uint8_t>& message) {
        DWORD bytesWritten = 0;
        
        // 先发送消息长度（4字节，little-endian）
        uint32_t messageLength = static_cast<uint32_t>(message.size());
        std::cout << "[IPCService] sendMessage: sending length " << messageLength << std::endl;
        
        if (!WriteFile(hPipe, &messageLength, sizeof(uint32_t), &bytesWritten, NULL)) {
            DWORD error = GetLastError();
            std::cerr << "[IPCService] sendMessage: WriteFile failed (length), error: " << error << std::endl;
            return false;
        }

        std::cout << "[IPCService] sendMessage: sending " << message.size() << " bytes" << std::endl;

        if (!WriteFile(hPipe, message.data(), (DWORD)message.size(), &bytesWritten, NULL)) {
            DWORD error = GetLastError();
            std::cerr << "[IPCService] sendMessage: WriteFile failed (data), error: " << error << std::endl;
            return false;
        }

        std::cout << "[IPCService] sendMessage: wrote " << bytesWritten << " bytes" << std::endl;

        if (!FlushFileBuffers(hPipe)) {
            DWORD error = GetLastError();
            std::cerr << "[IPCService] sendMessage: FlushFileBuffers failed, error: " << error << std::endl;
            return false;
        }

        std::cout << "[IPCService] sendMessage: flushed successfully" << std::endl;
        return true;
    }

    std::vector<uint8_t> processRequest(const std::vector<uint8_t>& requestBuffer) {
        flatbuffers::FlatBufferBuilder responseBuilder(1024);

        try {
            auto verifier = flatbuffers::Verifier(requestBuffer.data(), requestBuffer.size());
            if (!VerifyMessageWrapperBuffer(verifier)) {
                std::cerr << "[NativeService] Invalid FlatBuffers message" << std::endl;
                auto errorMsg = responseBuilder.CreateString("Invalid message format");
                auto resp = CreateResponse(responseBuilder, 0, ResponseStatus_STATUS_ERROR, errorMsg);
                auto wrapper = CreateMessageWrapper(responseBuilder, Message_Response, resp.Union());
                responseBuilder.Finish(wrapper);
                return std::vector<uint8_t>(responseBuilder.GetBufferPointer(), 
                                           responseBuilder.GetBufferPointer() + responseBuilder.GetSize());
            }

            auto messageWrapper = GetMessageWrapper(requestBuffer.data());
            auto messageType = messageWrapper->message_type_type();
            
            if (messageType != Message_Request) {
                std::cerr << "[NativeService] Expected Request message" << std::endl;
                auto errorMsg = responseBuilder.CreateString("Expected Request message");
                auto resp = CreateResponse(responseBuilder, 0, ResponseStatus_STATUS_ERROR, errorMsg);
                auto wrapper = CreateMessageWrapper(responseBuilder, Message_Response, resp.Union());
                responseBuilder.Finish(wrapper);
                return std::vector<uint8_t>(responseBuilder.GetBufferPointer(), 
                                           responseBuilder.GetBufferPointer() + responseBuilder.GetSize());
            }

            auto request = static_cast<const Request*>(messageWrapper->message_type());
            uint32_t requestId = request->id();
            ActionType action = request->action();

            std::cout << "[NativeService] Processing request: id=" << requestId << ", action=" << (int)action << std::endl;

            flatbuffers::Offset<CPUInfo> cpuOffset;
            flatbuffers::Offset<MemoryInfo> memoryOffset;
            flatbuffers::Offset<BatteryInfo> batteryOffset;
            flatbuffers::Offset<VolumeInfo> volumeOffset;
            flatbuffers::Offset<MediaInfo> mediaOffset;

            bool includeCPU = (action == ActionType_ACTION_GET_CPU || action == ActionType_ACTION_GET_ALL);
            bool includeMemory = (action == ActionType_ACTION_GET_MEMORY || action == ActionType_ACTION_GET_ALL);
            bool includeBattery = (action == ActionType_ACTION_GET_BATTERY || action == ActionType_ACTION_GET_ALL);
            bool includeVolume = (action == ActionType_ACTION_GET_VOLUME || action == ActionType_ACTION_GET_ALL);
            bool includeMedia = (action == ActionType_ACTION_GET_MEDIA || action == ActionType_ACTION_GET_ALL);

            if (includeCPU) {
                CPUInfoBuilder cpuBuilder(responseBuilder);
                monitor.fillCPUInfo(responseBuilder, cpuBuilder);
                cpuOffset = cpuBuilder.Finish();
            }

            if (includeMemory) {
                MemoryInfoBuilder memBuilder(responseBuilder);
                monitor.fillMemoryInfo(responseBuilder, memBuilder);
                memoryOffset = memBuilder.Finish();
            }

            if (includeBattery) {
                BatteryInfoBuilder batteryBuilder(responseBuilder);
                monitor.fillBatteryInfo(responseBuilder, batteryBuilder);
                batteryOffset = batteryBuilder.Finish();
            }

            if (includeVolume) {
                VolumeInfoBuilder volBuilder(responseBuilder);
                monitor.fillVolumeInfo(responseBuilder, volBuilder);
                volumeOffset = volBuilder.Finish();
            }

            if (includeMedia) {
                MediaInfoBuilder mediaBuilder(responseBuilder);
                monitor.fillMediaInfo(responseBuilder, mediaBuilder);
                mediaOffset = mediaBuilder.Finish();
                std::cout << "[NativeService] MediaInfo added to response" << std::endl;
            }

            flatbuffers::Offset<PinnedAppsResponse> pinnedAppsOffset;
            flatbuffers::Offset<IconExtractResponse> iconResponseOffset;
            flatbuffers::Offset<LnkParseResponse> lnkResponseOffset;

            if (action == ActionType_ACTION_GET_PINNED_APPS) {
                std::cout << "[NativeService] Getting pinned apps..." << std::endl;
                auto apps = TaskbarUtils::GetPinnedApps();
                std::vector<flatbuffers::Offset<PinnedAppInfo>> appInfos;
                
                for (size_t i = 0; i < apps.size(); ++i) {
                    const auto& app = apps[i];
                    
                    std::string idStr = WStringToString(app.id);
                    std::string nameStr = WStringToString(app.name);
                    std::string pathStr = WStringToString(app.path);
                    
                    std::cout << "[NativeService] App " << i << ": id='" << idStr << "', name='" << nameStr << "', path='" << pathStr << "'" << std::endl;
                    
                    auto idOffset = responseBuilder.CreateString(idStr);
                    auto nameOffset = responseBuilder.CreateString(nameStr);
                    auto pathOffset = responseBuilder.CreateString(pathStr);
                    
                    std::cout << "[NativeService] App " << i << " offsets: id=" << idOffset.o << ", name=" << nameOffset.o << ", path=" << pathOffset.o << std::endl;
                    
                    std::string iconDataBase64;
                    if (app.icon.valid && !app.icon.pixels.empty()) {
                        static const char* base64Chars =
                            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
                        const unsigned char* data = app.icon.pixels.data();
                        size_t len = app.icon.pixels.size();

                        for (size_t i = 0; i < len; i += 3) {
                            int a = data[i];
                            int b = (i + 1 < len) ? data[i + 1] : 0;
                            int c = (i + 2 < len) ? data[i + 2] : 0;

                            iconDataBase64.push_back(base64Chars[(a >> 2) & 0x3F]);
                            iconDataBase64.push_back(base64Chars[((a << 4) | (b >> 4)) & 0x3F]);
                            iconDataBase64.push_back(base64Chars[((b << 2) | (c >> 6)) & 0x3F]);
                            iconDataBase64.push_back(base64Chars[c & 0x3F]);
                        }

                        size_t padding = (len % 3);
                        if (padding > 0) {
                            for (size_t p = 0; p < 3 - padding; ++p) {
                                iconDataBase64[iconDataBase64.size() - 1 - p] = '=';
                            }
                        }

                        std::cout << "[NativeService] Encoded icon: " << app.icon.width << "x" << app.icon.height
                                  << ", " << len << " bytes -> " << iconDataBase64.size() << " base64 chars" << std::endl;
                    } else {
                        std::cout << "[NativeService] Icon extraction failed for: " << WStringToString(app.path) << std::endl;
                    }
                    auto iconDataStr = responseBuilder.CreateString(iconDataBase64);
                    
                    auto appInfo = CreatePinnedAppInfo(responseBuilder, idOffset, nameOffset, pathOffset, 
                                                        iconDataStr, app.icon.width, app.icon.height);
                    std::cout << "[NativeService] Created PinnedAppInfo at offset: " << appInfo.o << std::endl;
                    appInfos.push_back(appInfo);
                }
                
                auto appsVector = responseBuilder.CreateVector(appInfos);
                std::cout << "[NativeService] Created appsVector at offset: " << appsVector.o << std::endl;
                auto pinnedAppsBuilder = CreatePinnedAppsResponse(responseBuilder, appsVector);
                std::cout << "[NativeService] Created PinnedAppsResponse at offset: " << pinnedAppsBuilder.o << std::endl;
                pinnedAppsOffset = pinnedAppsBuilder;
                std::cout << "[NativeService] Found " << apps.size() << " pinned apps" << std::endl;
            }

            if (action == ActionType_ACTION_EXTRACT_ICON) {
                auto iconRequest = request->iconRequest();
                if (iconRequest) {
                    auto filePath = iconRequest->filePath();
                    int iconSize = iconRequest->iconSize();
                    std::wstring wFilePath = StringToWString(filePath->c_str());
                    
                    auto iconData = TaskbarUtils::ExtractIconFromExe(wFilePath, iconSize);
                    
                    std::string iconDataBase64;
                    if (iconData.valid && !iconData.pixels.empty()) {
                        static const char* base64Chars =
                            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
                        const unsigned char* data = iconData.pixels.data();
                        size_t len = iconData.pixels.size();

                        for (size_t i = 0; i < len; i += 3) {
                            int a = data[i];
                            int b = (i + 1 < len) ? data[i + 1] : 0;
                            int c = (i + 2 < len) ? data[i + 2] : 0;

                            iconDataBase64.push_back(base64Chars[(a >> 2) & 0x3F]);
                            iconDataBase64.push_back(base64Chars[((a << 4) | (b >> 4)) & 0x3F]);
                            iconDataBase64.push_back(base64Chars[((b << 2) | (c >> 6)) & 0x3F]);
                            iconDataBase64.push_back(base64Chars[c & 0x3F]);
                        }

                        size_t padding = (len % 3);
                        if (padding > 0) {
                            for (size_t p = 0; p < 3 - padding; ++p) {
                                iconDataBase64[iconDataBase64.size() - 1 - p] = '=';
                            }
                        }
                    }
                    
                    auto iconDataStr = responseBuilder.CreateString(iconDataBase64);
                    auto iconRespBuilder = CreateIconExtractResponse(responseBuilder, iconDataStr, 
                                                                      iconData.width, iconData.height, 
                                                                      iconData.valid);
                    iconResponseOffset = iconRespBuilder;
                }
            }

            if (action == ActionType_ACTION_PARSE_LNK) {
                auto lnkRequest = request->lnkRequest();
                if (lnkRequest) {
                    auto lnkPath = lnkRequest->lnkPath();
                    std::wstring wLnkPath = StringToWString(lnkPath->c_str());
                    
                    auto lnkInfo = TaskbarUtils::ParseLnkFile(wLnkPath);
                    
                    auto targetPathStr = responseBuilder.CreateString(WStringToString(lnkInfo.targetPath));
                    auto argsStr = responseBuilder.CreateString(WStringToString(lnkInfo.arguments));
                    auto workDirStr = responseBuilder.CreateString(WStringToString(lnkInfo.workingDirectory));
                    auto descStr = responseBuilder.CreateString(WStringToString(lnkInfo.description));
                    auto iconPathStr = responseBuilder.CreateString(WStringToString(lnkInfo.iconPath));
                    
                    auto lnkRespBuilder = CreateLnkParseResponse(responseBuilder, targetPathStr, 
                                                                  argsStr, workDirStr, descStr, 
                                                                  iconPathStr, lnkInfo.iconIndex, 
                                                                  lnkInfo.valid);
                    lnkResponseOffset = lnkRespBuilder;
                }
            }

            if (action == ActionType_ACTION_HIDE_SYSTEM_TASKBAR) {
                TaskbarUtils::HideSystemTaskbar();
                std::cout << "[NativeService] System taskbar hidden" << std::endl;
            }

            if (action == ActionType_ACTION_SHOW_SYSTEM_TASKBAR) {
                TaskbarUtils::ShowSystemTaskbar();
                std::cout << "[NativeService] System taskbar shown" << std::endl;
            }

            auto resp = CreateResponse(
                responseBuilder,
                requestId,
                ResponseStatus_STATUS_OK,
                0,
                includeCPU ? cpuOffset : 0,
                includeMemory ? memoryOffset : 0,
                includeBattery ? batteryOffset : 0,
                includeVolume ? volumeOffset : 0,
                includeMedia ? mediaOffset : 0,
                pinnedAppsOffset,
                iconResponseOffset,
                lnkResponseOffset
            );

            auto wrapper = CreateMessageWrapper(responseBuilder, Message_Response, resp.Union());
            responseBuilder.Finish(wrapper);

        } catch (const std::exception& e) {
            std::cerr << "[NativeService] Exception processing request: " << e.what() << std::endl;
            auto errorMsg = responseBuilder.CreateString(std::string("Exception: ") + e.what());
            auto resp = CreateResponse(responseBuilder, 0, ResponseStatus_STATUS_ERROR, errorMsg);
            auto wrapper = CreateMessageWrapper(responseBuilder, Message_Response, resp.Union());
            responseBuilder.Finish(wrapper);
        } catch (...) {
            std::cerr << "[NativeService] Unknown exception processing request" << std::endl;
            auto errorMsg = responseBuilder.CreateString("Unknown error");
            auto resp = CreateResponse(responseBuilder, 0, ResponseStatus_STATUS_ERROR, errorMsg);
            auto wrapper = CreateMessageWrapper(responseBuilder, Message_Response, resp.Union());
            responseBuilder.Finish(wrapper);
        }

        return std::vector<uint8_t>(responseBuilder.GetBufferPointer(), 
                                   responseBuilder.GetBufferPointer() + responseBuilder.GetSize());
    }

public:
    IPCService() : running(false), hPipe(INVALID_HANDLE_VALUE) {}
    
    void initializeMediaMonitor() {
        try {
            monitor.mediaMonitor.Start();
            std::cout << "[NativeService] MediaMonitor initialized successfully" << std::endl;
        } catch (const std::exception& e) {
            std::cerr << "[NativeService] Failed to initialize MediaMonitor: " << e.what() << std::endl;
        }
    }

    ~IPCService() {
        stop();
    }

    bool start() {
        std::cout << "[IPCService] Creating named pipe..." << std::endl;
        hPipe = CreateNamedPipe(
            PIPE_NAME.c_str(),
            PIPE_ACCESS_DUPLEX,
            PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT,
            PIPE_UNLIMITED_INSTANCES,
            BUFFER_SIZE,
            BUFFER_SIZE,
            0,
            NULL
        );

        if (hPipe == INVALID_HANDLE_VALUE) {
            std::cerr << "[IPCService] Failed to create pipe, error: " << GetLastError() << std::endl;
            return false;
        }

        std::cout << "[IPCService] Pipe created, waiting for connections..." << std::endl;
        running = true;

        while (running) {
            std::cout << "[IPCService] Waiting for client connection..." << std::endl;
            if (!ConnectNamedPipe(hPipe, NULL)) {
                DWORD error = GetLastError();
                if (error != ERROR_PIPE_CONNECTED) {
                    std::cerr << "[IPCService] ConnectNamedPipe failed, error: " << error << std::endl;
                    continue;
                }
            }
            std::cout << "[IPCService] Client connected!" << std::endl;

            bool clientConnected = true;
            while (clientConnected && running) {
                std::vector<uint8_t> requestBuffer;
                if (readMessage(requestBuffer)) {
                    std::cout << "[IPCService] Calling processRequest with " << requestBuffer.size() << " bytes" << std::endl;

                    std::vector<uint8_t> response = processRequest(requestBuffer);

                    std::cout << "[IPCService] Sending response: " << response.size() << " bytes" << std::endl;
                    if (!sendMessage(response)) {
                        std::cerr << "[IPCService] sendMessage failed, disconnecting..." << std::endl;
                        clientConnected = false;
                    }
                } else {
                    std::cout << "[IPCService] Read failed, disconnecting..." << std::endl;
                    clientConnected = false;
                }
            }

            DisconnectNamedPipe(hPipe);
            std::cout << "[IPCService] Client disconnected" << std::endl;
        }

        return true;
    }

    void stop() {
        running = false;
        if (hPipe != INVALID_HANDLE_VALUE) {
            CloseHandle(hPipe);
            hPipe = INVALID_HANDLE_VALUE;
        }
        std::cout << "[NativeService] Service stopped" << std::endl;
    }
};

int main() {
    std::cout << "[NativeService] === MAIN START ===" << std::endl;
    std::cout << "[NativeService] Starting native service..." << std::endl;

    std::cout << "[NativeService] Initializing WinRT STA..." << std::endl;
    try {
        winrt::init_apartment(winrt::apartment_type::single_threaded);
        std::cout << "[NativeService] WinRT STA initialized successfully" << std::endl;
    } catch (const winrt::hresult_error& e) {
        std::cerr << "[NativeService] Failed to initialize WinRT: " << winrt::to_string(e.message()) << std::endl;
        return 1;
    }

    SetConsoleCtrlHandler([](DWORD ctrlType) -> BOOL {
        if (ctrlType == CTRL_CLOSE_EVENT) {
            std::cout << "[NativeService] Shutting down..." << std::endl;
            return TRUE;
        }
        return FALSE;
    }, TRUE);

    std::cout << "[NativeService] Creating IPCService..." << std::endl;
    IPCService service;

    std::cout << "[NativeService] Initializing MediaMonitor..." << std::endl;
    service.initializeMediaMonitor();

    std::cout << "[NativeService] Starting IPC server..." << std::endl;
    service.start();

    std::cout << "[NativeService] === MAIN END ===" << std::endl;
    winrt::uninit_apartment();
    return 0;
}