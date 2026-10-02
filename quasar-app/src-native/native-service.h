#pragma once

#include <windows.h>
#include <string>
#include <nlohmann/json.hpp>

using json = nlohmann::json;

const std::string PIPE_NAME = "\\\\.\\pipe\\NativeServicePipe";
const int BUFFER_SIZE = 4096;

// 系统监控类
class SystemMonitor {
public:
    json getCPUUsage();
    json getMemoryUsage();
    json getBatteryStatus();
    json getVolumeLevel();
    json getAllStats();
};

// IPC 服务类
class IPCService {
private:
    HANDLE hPipe;
    SystemMonitor monitor;
    bool running;

    std::string readMessage();
    bool sendMessage(const std::string& message);
    json processRequest(const std::string& request);

public:
    IPCService();
    bool start();
    void stop();
};

// 错误码定义
enum class ErrorCode {
    SUCCESS = 0,
    PIPE_CREATE_FAILED = 1001,
    PIPE_CONNECT_FAILED = 1002,
    READ_FAILED = 1003,
    WRITE_FAILED = 1004,
    JSON_PARSE_ERROR = 2001,
    UNKNOWN_ACTION = 2002,
    COM_INIT_FAILED = 3001,
    DEVICE_ENUM_CREATE_FAILED = 3002,
    AUDIO_ENDPOINT_FAILED = 3003
};

// 工具函数
std::string formatErrorMessage(DWORD errorCode);
std::string getCurrentTimestamp();