#pragma once

#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Media.Control.h>
#include <winrt/Windows.Media.Playback.h>
#include <winrt/Windows.Storage.Streams.h>
#include <string>
#include <functional>
#include <vector>

namespace nlohmann {
    class json;
}

class MediaMonitor {
private:
    winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSessionManager m_sessionManager{ nullptr };
    winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSession m_currentSession{ nullptr };
    winrt::event_token m_sessionChangedToken{};
    winrt::event_token m_mediaPropertiesChangedToken{};
    winrt::event_token m_playbackInfoChangedToken{};
    winrt::event_token m_timelineChangedToken{};
    
    // 缓存数据 - 匹配 windows-smtc-monitor 接口
    std::string m_title = "";
    std::string m_artist = "";
    std::string m_albumTitle = "";           // 字段名改为 albumTitle
    std::string m_albumArtist = "";          // 新增：专辑艺术家
    int m_trackNumber = 0;                   // 新增：曲目编号
    int m_albumTrackCount = 0;               // 新增：专辑曲目总数
    bool m_playing = false;
    double m_position = 0;                   // 当前播放位置（秒，匹配 windows-smtc-monitor）
    double m_duration = 0;                   // 总时长（秒，匹配 windows-smtc-monitor）
    int64_t m_lastUpdatedTime = 0;           // 新增：最后更新时间戳
    std::vector<uint8_t> m_thumbnail;        // 封面图片数据（二进制）
    std::string m_thumbnailBase64;           // 封面图片Base64编码
    
    // 回调函数
    std::function<void(const nlohmann::json&)> m_callback = nullptr;
    
    // 内部更新逻辑
    void UpdateInfo();

public:
    MediaMonitor();
    ~MediaMonitor();

    // 初始化并开始监听
    void Start();
    
    // 停止监听
    void Stop();
    
    // 设置回调函数
    void SetCallback(std::function<void(const nlohmann::json&)> callback);
    
    // 获取当前媒体信息 (供外部调用)
    nlohmann::json getMediaInfo();

    // 通知回调
    void notifyCallback();

private:
    // 事件处理函数
    void OnCurrentSessionChanged(
        winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSessionManager sender,
        winrt::Windows::Foundation::IInspectable const& args
    );
    
    void OnMediaPropsChanged(
        winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSession const& sender,
        winrt::Windows::Foundation::IInspectable const& args
    );
    
    void OnPlaybackInfoChanged(
        winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSession const& sender,
        winrt::Windows::Foundation::IInspectable const& args
    );
    
    void OnTimelineChanged(
        winrt::Windows::Media::Control::GlobalSystemMediaTransportControlsSession const& sender,
        winrt::Windows::Foundation::IInspectable const& args
    );
};