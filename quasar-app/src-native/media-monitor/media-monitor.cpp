#include "media-monitor.h"
#include "json_helper.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <sstream>
#include <iomanip>

using namespace winrt;
using namespace Windows::Foundation;
using namespace Windows::Storage::Streams;
using namespace Windows::Media::Control;

std::vector<uint8_t> StreamToBytes(IRandomAccessStream const& stream) {
    if (!stream) {
        return {};
    }
    
    try {
        uint64_t size = stream.Size();
        if (size == 0) {
            return {};
        }
        
        std::vector<uint8_t> bytes(static_cast<size_t>(size));
        stream.Seek(0);
        
        auto inputStream = stream.GetInputStreamAt(0);
        auto buffer = Buffer(static_cast<uint32_t>(size));
        auto readOperation = inputStream.ReadAsync(buffer, buffer.Capacity(), InputStreamOptions::None).get();
        
        auto dataReader = DataReader::FromBuffer(buffer);
        dataReader.ReadBytes(bytes);
        
        return bytes;
    } catch (...) {
        return {};
    }
}

std::string Base64Encode(const std::vector<uint8_t>& bytes) {
    static const std::string base64_chars = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        "abcdefghijklmnopqrstuvwxyz"
        "0123456789+/";
    
    std::string result;
    int i = 0;
    int j = 0;
    uint8_t char_array_3[3];
    uint8_t char_array_4[4];
    size_t len = bytes.size();
    
    while (len--) {
        char_array_3[i++] = bytes[bytes.size() - len - 1];
        if (i == 3) {
            char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
            char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
            char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
            char_array_4[3] = char_array_3[2] & 0x3f;
            
            for(i = 0; (i < 4) ; i++)
                result += base64_chars[char_array_4[i]];
            i = 0;
        }
    }
    
    if (i) {
        for(j = i; j < 3; j++)
            char_array_3[j] = '\0';
        
        char_array_4[0] = (char_array_3[0] & 0xfc) >> 2;
        char_array_4[1] = ((char_array_3[0] & 0x03) << 4) + ((char_array_3[1] & 0xf0) >> 4);
        char_array_4[2] = ((char_array_3[1] & 0x0f) << 2) + ((char_array_3[2] & 0xc0) >> 6);
        char_array_4[3] = char_array_3[2] & 0x3f;
        
        for (j = 0; (j < i + 1); j++)
            result += base64_chars[char_array_4[j]];
        
        while((i++ < 3))
            result += '=';
    }
    
    return result;
}

MediaMonitor::MediaMonitor() = default;

MediaMonitor::~MediaMonitor() {
    Stop();
}

void MediaMonitor::Start() {
    try {
        m_sessionManager = GlobalSystemMediaTransportControlsSessionManager::RequestAsync().get();
        
        m_sessionChangedToken = m_sessionManager.CurrentSessionChanged(
            { this, &MediaMonitor::OnCurrentSessionChanged }
        );
        
        m_currentSession = m_sessionManager.GetCurrentSession();
        
        if (m_currentSession) {
            m_mediaPropertiesChangedToken = m_currentSession.MediaPropertiesChanged(
                { this, &MediaMonitor::OnMediaPropsChanged }
            );
            
            m_playbackInfoChangedToken = m_currentSession.PlaybackInfoChanged(
                { this, &MediaMonitor::OnPlaybackInfoChanged }
            );
            
            m_timelineChangedToken = m_currentSession.TimelinePropertiesChanged(
                { this, &MediaMonitor::OnTimelineChanged }
            );
            
            // 获取播放状态
            auto playbackInfo = m_currentSession.GetPlaybackInfo();
            m_playing = (playbackInfo.PlaybackStatus() == 
                         GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing);
            std::cout << "[MediaMonitor] Session found, playback status: " << (m_playing ? "Playing" : "Not Playing") << std::endl;
            
            // 立即尝试获取一次数据（播放器可能已经推送了数据）
            std::cout << "[MediaMonitor] Attempting to get media properties..." << std::endl;
            
            // 使用线程 + 超时机制
            bool propsLoaded = false;
            std::thread propsThread([this, &propsLoaded]() {
                try {
                    auto props = m_currentSession.TryGetMediaPropertiesAsync().get();
                    if (props) {
                        m_title = winrt::to_string(props.Title());
                        m_artist = winrt::to_string(props.Artist());
                        m_albumTitle = winrt::to_string(props.AlbumTitle());
                        m_albumArtist = winrt::to_string(props.AlbumArtist());
                        m_trackNumber = props.TrackNumber();
                        m_albumTrackCount = props.AlbumTrackCount();
                        
                        // 获取封面图片
                        try {
                            auto thumbnail = props.Thumbnail();
                            if (thumbnail) {
                                auto stream = thumbnail.OpenReadAsync().get();
                                m_thumbnail = StreamToBytes(stream);
                                m_thumbnailBase64 = Base64Encode(m_thumbnail);
                                std::cout << "[MediaMonitor] Thumbnail loaded, size: " << m_thumbnail.size() << " bytes" << std::endl;
                            } else {
                                std::cout << "[MediaMonitor] No thumbnail available" << std::endl;
                                m_thumbnail.clear();
                                m_thumbnailBase64.clear();
                            }
                        } catch (...) {
                            std::cout << "[MediaMonitor] Thumbnail error" << std::endl;
                            m_thumbnail.clear();
                            m_thumbnailBase64.clear();
                        }
                        
                        m_lastUpdatedTime = std::chrono::duration_cast<std::chrono::milliseconds>(
                            std::chrono::system_clock::now().time_since_epoch()
                        ).count();
                        
                        std::cout << "[MediaMonitor] Initial media properties loaded" << std::endl;
                        std::cout << "  Title: \"" << m_title << "\"" << std::endl;
                        std::cout << "  Artist: \"" << m_artist << "\"" << std::endl;
                        std::cout << "  AlbumTitle: \"" << m_albumTitle << "\"" << std::endl;
                        std::cout << "  AlbumArtist: \"" << m_albumArtist << "\"" << std::endl;
                        propsLoaded = true;
                    }
                } catch (...) {
                    std::cerr << "[MediaMonitor] Initial media properties error" << std::endl;
                }
            });
            
            // 等待最多 3 秒
            if (propsThread.joinable()) {
                propsThread.detach(); // 不等待，让它后台运行
            }
            
            // 短暂等待一下看是否能快速获取到
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
            
            // 获取时间线属性（转换为秒，匹配 windows-smtc-monitor）
            try {
                auto timeline = m_currentSession.GetTimelineProperties();
                
                // 打印完整的 GetTimelineProperties 所有信息
                std::cout << "[MediaMonitor] GetTimelineProperties():" << std::endl;
                std::cout << "  Position: " << timeline.Position().count() << " (100ns)" << std::endl;
                std::cout << "  EndTime: " << timeline.EndTime().count() << " (100ns)" << std::endl;
                std::cout << "  StartTime: " << timeline.StartTime().count() << " (100ns)" << std::endl;
                std::cout << "  MinSeekTime: " << timeline.MinSeekTime().count() << " (100ns)" << std::endl;
                std::cout << "  MaxSeekTime: " << timeline.MaxSeekTime().count() << " (100ns)" << std::endl;
                
                // 尝试使用 EndTime - StartTime 计算 duration
                auto calculatedDuration = (timeline.EndTime().count() - timeline.StartTime().count()) / 10000000.0;
                std::cout << "  Calculated Duration (EndTime-StartTime): " << calculatedDuration << "s" << std::endl;
                
                m_position = timeline.Position().count() / 10000000.0;  // 100ns -> 秒
                m_duration = calculatedDuration;    // 使用计算的 duration
                std::cout << "[MediaMonitor] Initial timeline: position=" << m_position << "s, duration=" << m_duration << "s" << std::endl;
            } catch (const winrt::hresult_error& e) {
                std::cerr << "[MediaMonitor] Initial timeline error: " << winrt::to_string(e.message()) << std::endl;
            }
            
            // 通知一次更新
            notifyCallback();
        }
        
    } catch (const winrt::hresult_error& e) {
        std::cerr << "[MediaMonitor] Init Error: " << winrt::to_string(e.message()) << std::endl;
    } catch (const std::exception& e) {
        std::cerr << "[MediaMonitor] Init Error: " << e.what() << std::endl;
    }
}

void MediaMonitor::Stop() {
    if (m_currentSession) {
        // 移除事件监听
        m_currentSession.MediaPropertiesChanged(m_mediaPropertiesChangedToken);
        m_currentSession.PlaybackInfoChanged(m_playbackInfoChangedToken);
        m_currentSession.TimelinePropertiesChanged(m_timelineChangedToken);
        m_currentSession = nullptr;
    }
    
    if (m_sessionManager) {
        m_sessionManager.CurrentSessionChanged(m_sessionChangedToken);
        m_sessionManager = nullptr;
    }
}

void MediaMonitor::SetCallback(std::function<void(const json&)> callback) {
    m_callback = callback;
}

void MediaMonitor::notifyCallback() {
    if (m_callback) {
        json info = {};
        json_set(info, "type", "media");
        json_set(info, "title", m_title.empty() ? "No Music Playing" : m_title);
        json_set(info, "artist", m_artist);
        json_set(info, "albumTitle", m_albumTitle);
        json_set(info, "albumArtist", m_albumArtist);
        json_set(info, "playing", m_playing);
        json_set(info, "source", "Windows SMTC");
        json_set(info, "thumbnail", m_thumbnailBase64);
        
        // 直接从 GetTimelineProperties() 获取 position 和 duration
        if (m_currentSession) {
            try {
                auto timeline = m_currentSession.GetTimelineProperties();
                double position = timeline.Position().count() / 10000000.0;
                double duration = (timeline.EndTime().count() - timeline.StartTime().count()) / 10000000.0;
                json_set(info, "position", position);
                json_set(info, "duration", duration);
            } catch (const winrt::hresult_error& e) {
                json_set(info, "position", 0.0);
                json_set(info, "duration", 0.0);
            }
        } else {
            json_set(info, "position", 0.0);
            json_set(info, "duration", 0.0);
        }
        
        m_callback(info);
    }
}

void MediaMonitor::OnCurrentSessionChanged(
    GlobalSystemMediaTransportControlsSessionManager sender,
    IInspectable const& args
) {
    // 移除旧会话的事件监听
    if (m_currentSession) {
        m_currentSession.MediaPropertiesChanged(m_mediaPropertiesChangedToken);
        m_currentSession.PlaybackInfoChanged(m_playbackInfoChangedToken);
        m_currentSession.TimelinePropertiesChanged(m_timelineChangedToken);
    }
    
    // 获取新会话
    m_currentSession = sender.GetCurrentSession();
    
    if (m_currentSession) {
        // 绑定新会话的事件
        m_mediaPropertiesChangedToken = m_currentSession.MediaPropertiesChanged(
            { this, &MediaMonitor::OnMediaPropsChanged }
        );
        
        m_playbackInfoChangedToken = m_currentSession.PlaybackInfoChanged(
            { this, &MediaMonitor::OnPlaybackInfoChanged }
        );
        
        UpdateInfo();
    } else {
        // 没有正在播放的会话
        m_title = "";
        m_artist = "";
        m_albumTitle = "";
        m_albumArtist = "";
        m_playing = false;
        m_thumbnail.clear();
        m_thumbnailBase64.clear();
        
        if (m_callback) {
            json info = {};
            json_set(info, "type", "media");
            json_set(info, "title", "No Music Playing");
            json_set(info, "artist", "");
            json_set(info, "albumTitle", "");
            json_set(info, "albumArtist", "");
            json_set(info, "playing", false);
            json_set(info, "source", "Windows SMTC");
            
            m_callback(info);
        }
    }
}

void MediaMonitor::OnMediaPropsChanged(
    GlobalSystemMediaTransportControlsSession const& sender,
    IInspectable const& args
) {
    std::cout << "[MediaMonitor] MediaPropertiesChanged event received" << std::endl;
    
    try {
        auto props = sender.TryGetMediaPropertiesAsync().get();
        if (props) {
            m_title = winrt::to_string(props.Title());
            m_artist = winrt::to_string(props.Artist());
            m_albumTitle = winrt::to_string(props.AlbumTitle());
            m_albumArtist = winrt::to_string(props.AlbumArtist());
            m_trackNumber = props.TrackNumber();
            m_albumTrackCount = props.AlbumTrackCount();
            
            // 获取封面图片
            try {
                auto thumbnail = props.Thumbnail();
                if (thumbnail) {
                    auto stream = thumbnail.OpenReadAsync().get();
                    m_thumbnail = StreamToBytes(stream);
                    m_thumbnailBase64 = Base64Encode(m_thumbnail);
                    std::cout << "[MediaMonitor] Thumbnail updated, size: " << m_thumbnail.size() << " bytes" << std::endl;
                } else {
                    m_thumbnail.clear();
                    m_thumbnailBase64.clear();
                }
            } catch (const winrt::hresult_error& e) {
                std::cerr << "[MediaMonitor] Thumbnail update error: " << winrt::to_string(e.message()) << std::endl;
                m_thumbnail.clear();
                m_thumbnailBase64.clear();
            }
            
            m_lastUpdatedTime = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now().time_since_epoch()
            ).count();
            
            std::cout << "[MediaMonitor] Media properties updated:" << std::endl;
            std::cout << "  Title: \"" << m_title << "\"" << std::endl;
            std::cout << "  Artist: \"" << m_artist << "\"" << std::endl;
            std::cout << "  AlbumTitle: \"" << m_albumTitle << "\"" << std::endl;
            std::cout << "  AlbumArtist: \"" << m_albumArtist << "\"" << std::endl;
        }
    } catch (const winrt::hresult_error& e) {
        std::cerr << "[MediaMonitor] MediaPropertiesChanged error: " << winrt::to_string(e.message()) << std::endl;
    }
    
    // 通知更新
    notifyCallback();
}

void MediaMonitor::OnPlaybackInfoChanged(
    GlobalSystemMediaTransportControlsSession const& sender,
    IInspectable const& args
) {
    std::cout << "[MediaMonitor] PlaybackInfoChanged event received" << std::endl;
    
    try {
        auto playbackInfo = sender.GetPlaybackInfo();
        
        // 打印完整的 GetPlaybackInfo 数据
        std::cout << "[MediaMonitor] GetPlaybackInfo():" << std::endl;
        std::cout << "  PlaybackStatus: ";
        switch (playbackInfo.PlaybackStatus()) {
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing:
                std::cout << "Playing"; break;
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Paused:
                std::cout << "Paused"; break;
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Stopped:
                std::cout << "Stopped"; break;
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Closed:
                std::cout << "Closed"; break;
            default:
                std::cout << "Unknown";
        }
        std::cout << std::endl;
        
        m_playing = (playbackInfo.PlaybackStatus() == 
                     GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing);
    } catch (const winrt::hresult_error& e) {
        std::cerr << "[MediaMonitor] PlaybackInfoChanged error: " << winrt::to_string(e.message()) << std::endl;
    }
    
    notifyCallback();
}

void MediaMonitor::UpdateInfo() {
    if (!m_currentSession) return;

    try {
        // 获取媒体属性
        try {
            auto props = m_currentSession.TryGetMediaPropertiesAsync().get();
            if (props) {
                m_title = winrt::to_string(props.Title());
                m_artist = winrt::to_string(props.Artist());
                m_albumTitle = winrt::to_string(props.AlbumTitle());
                m_albumArtist = winrt::to_string(props.AlbumArtist());
                
                // 获取封面图片
                try {
                    auto thumbnail = props.Thumbnail();
                    if (thumbnail) {
                        auto stream = thumbnail.OpenReadAsync().get();
                        m_thumbnail = StreamToBytes(stream);
                        m_thumbnailBase64 = Base64Encode(m_thumbnail);
                        std::cout << "[MediaMonitor] Thumbnail loaded in UpdateInfo, size: " << m_thumbnail.size() << " bytes" << std::endl;
                    } else {
                        m_thumbnail.clear();
                        m_thumbnailBase64.clear();
                    }
                } catch (const winrt::hresult_error& e) {
                    std::cerr << "[MediaMonitor] Thumbnail error in UpdateInfo: " << winrt::to_string(e.message()) << std::endl;
                    m_thumbnail.clear();
                    m_thumbnailBase64.clear();
                }
                
                std::cout << "[MediaMonitor] Media properties:" << std::endl;
                std::cout << "  Title: \"" << m_title << "\" (empty: " << m_title.empty() << ")" << std::endl;
                std::cout << "  Artist: \"" << m_artist << "\" (empty: " << m_artist.empty() << ")" << std::endl;
                std::cout << "  AlbumTitle: \"" << m_albumTitle << "\" (empty: " << m_albumTitle.empty() << ")" << std::endl;
            } else {
                std::cerr << "[MediaMonitor] Media properties is null" << std::endl;
            }
        } catch (const winrt::hresult_error& e) {
            std::cerr << "[MediaMonitor] Media properties error: " << winrt::to_string(e.message()) << std::endl;
        }
        
        // 获取播放信息 (GetPlaybackInfo)
        auto playbackInfo = m_currentSession.GetPlaybackInfo();
        m_playing = (playbackInfo.PlaybackStatus() == 
                     GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing);
        
        // 打印完整的 GetPlaybackInfo 数据
        std::cout << "[MediaMonitor] GetPlaybackInfo():" << std::endl;
        std::cout << "  PlaybackStatus: ";
        switch (playbackInfo.PlaybackStatus()) {
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Playing:
                std::cout << "Playing"; break;
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Paused:
                std::cout << "Paused"; break;
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Stopped:
                std::cout << "Stopped"; break;
            case GlobalSystemMediaTransportControlsSessionPlaybackStatus::Closed:
                std::cout << "Closed"; break;
            default:
                std::cout << "Unknown";
        }
        std::cout << std::endl;
        
        // 获取时间线属性 (GetTimelineProperties)
        try {
            auto timeline = m_currentSession.GetTimelineProperties();
            m_position = timeline.Position().count() / 10000000.0; // 转换为秒
            m_duration = timeline.EndTime().count() / 10000000.0; // 转换为秒
            
            // 打印完整的 GetTimelineProperties 数据
            std::cout << "[MediaMonitor] GetTimelineProperties():" << std::endl;
            std::cout << "  Position: " << timeline.Position().count() << " (100ns) = " << m_position << "s" << std::endl;
            std::cout << "  EndTime: " << timeline.EndTime().count() << " (100ns) = " << m_duration << "s" << std::endl;
            std::cout << "  StartTime: " << timeline.StartTime().count() << " (100ns)" << std::endl;
        } catch (const winrt::hresult_error& e) {
            std::cerr << "[MediaMonitor] GetTimelineProperties Error: " << winrt::to_string(e.message()) << std::endl;
            m_position = 0;
            m_duration = 0;
        }
        
        // 调用回调
        if (m_callback) {
            json info = {};
            json_set(info, "type", "media");
            json_set(info, "title", m_title);
            json_set(info, "artist", m_artist);
            json_set(info, "albumTitle", m_albumTitle);
            json_set(info, "playing", m_playing);
            json_set(info, "position", m_position);
            json_set(info, "duration", m_duration);
            json_set(info, "source", "Windows SMTC");
            
            if (!m_thumbnailBase64.empty()) {
                json_set(info, "thumbnail", m_thumbnailBase64);
                json_set(info, "thumbnailSize", m_thumbnail.size());
            }
            
            m_callback(info);
        }
        
    } catch (const winrt::hresult_error& e) {
        std::cerr << "[MediaMonitor] Update Error: " << winrt::to_string(e.message()) << std::endl;
    } catch (const std::exception& e) {
        std::cerr << "[MediaMonitor] Update Error: " << e.what() << std::endl;
    }
}

json MediaMonitor::getMediaInfo() {
    json data = {};
    json_set(data, "type", "media");
    json_set(data, "title", m_title.empty() ? "No Music Playing" : m_title);
    json_set(data, "artist", m_artist);
    json_set(data, "albumTitle", m_albumTitle);
    json_set(data, "albumArtist", m_albumArtist);
    json_set(data, "playing", m_playing);
    json_set(data, "source", "Windows SMTC");
    json_set(data, "thumbnail", m_thumbnailBase64);
    
    // 直接从 GetTimelineProperties() 获取 position 和 duration
    if (m_currentSession) {
        try {
            auto timeline = m_currentSession.GetTimelineProperties();
            double position = timeline.Position().count() / 10000000.0;
            double duration = (timeline.EndTime().count() - timeline.StartTime().count()) / 10000000.0;
            json_set(data, "position", position);
            json_set(data, "duration", duration);
        } catch (const winrt::hresult_error& e) {
            json_set(data, "position", 0.0);
            json_set(data, "duration", 0.0);
        }
    } else {
        json_set(data, "position", 0.0);
        json_set(data, "duration", 0.0);
    }
    
    return data;
}

void MediaMonitor::OnTimelineChanged(
    GlobalSystemMediaTransportControlsSession const& sender,
    IInspectable const& args
) {
    std::cout << "[MediaMonitor] TimelinePropertiesChanged event received" << std::endl;
    
    try {
        auto timeline = sender.GetTimelineProperties();
        
        // 直接打印完整的 GetTimelineProperties 所有信息
        std::cout << "[MediaMonitor] GetTimelineProperties():" << std::endl;
        std::cout << "  Position: " << timeline.Position().count() << " (100ns)" << std::endl;
        std::cout << "  EndTime: " << timeline.EndTime().count() << " (100ns)" << std::endl;
        std::cout << "  StartTime: " << timeline.StartTime().count() << " (100ns)" << std::endl;
        std::cout << "  MinSeekTime: " << timeline.MinSeekTime().count() << " (100ns)" << std::endl;
        std::cout << "  MaxSeekTime: " << timeline.MaxSeekTime().count() << " (100ns)" << std::endl;
        
        // 尝试使用 EndTime - StartTime 计算 duration
        auto calculatedDuration = (timeline.EndTime().count() - timeline.StartTime().count()) / 10000000.0;
        std::cout << "  Calculated Duration (EndTime-StartTime): " << calculatedDuration << "s" << std::endl;
    } catch (const winrt::hresult_error& e) {
        std::cerr << "[MediaMonitor] TimelinePropertiesChanged error: " << winrt::to_string(e.message()) << std::endl;
    }
    
    notifyCallback();
}