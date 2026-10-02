import { ipcMain } from 'electron';

class MediaController {
  constructor(mainWindow, dynamicIslandWindow) {
    console.log('[MediaController] Constructor starting...');
    this.mainWindow = mainWindow;
    this.dynamicIslandWindow = dynamicIslandWindow;
    this.currentTrack = null;
    this.isPlaying = false;
    this.currentPosition = 0;
    this.trackDuration = 240;
    this.playbackInterval = null;
    this.isEnabled = false;
    this.musicControlEnabled = false;
    this.nativeServiceAvailable = false;
    this.hasSystemMedia = false;

    this.mockTracks = [
      { id: 1, title: '夜曲', artist: '周杰伦', album: '十一月的萧邦', duration: 240, isSystemMedia: false },
      { id: 2, title: '稻香', artist: '周杰伦', album: '魔杰座', duration: 210, isSystemMedia: false },
      { id: 3, title: '晴天', artist: '周杰伦', album: '叶惠美', duration: 269, isSystemMedia: false },
      { id: 4, title: '七里香', artist: '周杰伦', album: '七里香', duration: 299, isSystemMedia: false },
      { id: 5, title: '简单爱', artist: '周杰伦', album: '范特西', duration: 273, isSystemMedia: false }
    ];

    this.currentMockTrackIndex = 0;

    console.log('[MediaController] Setting up IPC...');
    this.setupIPC();

    console.log('[MediaController] Constructor complete');
  }

  setupIPC() {
    ipcMain.handle('media:getCurrentTrack', () => {
      return {
        ...this.currentTrack,
        isPlaying: this.isPlaying,
        position: this.currentPosition,
        duration: this.trackDuration
      };
    });

    ipcMain.handle('media:play', async () => {
      console.log('[MediaController] Play command');
      this.mockPlay();
      return true;
    });

    ipcMain.handle('media:pause', async () => {
      console.log('[MediaController] Pause command');
      this.mockPause();
      return true;
    });

    ipcMain.handle('media:toggle', async () => {
      console.log('[MediaController] Toggle command');
      this.mockToggle();
      return true;
    });

    ipcMain.handle('media:next', async () => {
      console.log('[MediaController] Next command');
      this.mockNext();
      return true;
    });

    ipcMain.handle('media:previous', async () => {
      console.log('[MediaController] Previous command');
      this.mockPrevious();
      return true;
    });

    ipcMain.on('media:fromNativeService', (event, mediaInfo) => {
      this.handleNativeServiceMediaUpdate(mediaInfo);
    });

    ipcMain.on('nativeService:connected', () => {
      console.log('[MediaController] Native service connected');
      this.nativeServiceAvailable = true;
    });

    ipcMain.on('nativeService:disconnected', () => {
      console.log('[MediaController] Native service disconnected');
      this.nativeServiceAvailable = false;
    });
  }

  handleNativeServiceMediaUpdate(mediaInfo) {
    if (!this.isEnabled || !this.musicControlEnabled) return;
    if (!mediaInfo || !mediaInfo.title || mediaInfo.title === 'No Music Playing') {
      this.hasSystemMedia = false;
      this.useMockDataIfNeeded();
      return;
    }

    console.log('[MediaController] Processing native service media update:', mediaInfo);
    this.hasSystemMedia = true;

    this.stopPlaybackLoop();

    let coverBase64 = null;
    if (mediaInfo.thumbnail && mediaInfo.thumbnail.length > 0) {
      try {
        coverBase64 = `data:image/png;base64,${mediaInfo.thumbnail}`;
        console.log('[MediaController] Cover image found, size:', mediaInfo.thumbnailSize || mediaInfo.thumbnail.length, 'bytes');
      } catch (e) {
        console.log('[MediaController] Failed to parse cover image:', e.message);
      }
    }

    this.currentTrack = {
      id: mediaInfo.sourceAppId || Date.now(),
      title: mediaInfo.title || '未知歌曲',
      artist: mediaInfo.artist || '未知艺术家',
      album: mediaInfo.albumTitle || '',
      appName: mediaInfo.source || 'Windows SMTC',
      cover: coverBase64,
      isSystemMedia: true
    };

    this.isPlaying = mediaInfo.playing || false;
    this.currentPosition = mediaInfo.position || 0;
    this.trackDuration = mediaInfo.duration || 180;

    console.log('[MediaController] Updated from native service:', this.currentTrack.title, '-', this.currentTrack.artist);
    console.log('[MediaController] Playback status:', this.isPlaying, 'Position:', this.currentPosition, 'Duration:', this.trackDuration);

    if (this.isPlaying && this.trackDuration > 0) {
      this.startPlaybackLoop();
    }

    try {
      this.sendUpdate();
      console.log('[MediaController] sendUpdate called successfully');
    } catch (error) {
      console.error('[MediaController] Error in sendUpdate:', error);
    }
  }

  useMockDataIfNeeded() {
    if (!this.isEnabled || !this.musicControlEnabled) return;
    
    if (this.hasSystemMedia) {
      return;
    }

    this.stopPlaybackLoop();

    this.currentTrack = {
      ...this.mockTracks[this.currentMockTrackIndex],
      isSystemMedia: false
    };
    this.trackDuration = this.currentTrack.duration;
    this.currentPosition = 0;
    this.isPlaying = true;
    this.startPlaybackLoop();
    this.sendUpdate();

    console.log('[MediaController] Using mock data:', this.currentTrack.title);
  }

  mockPlay() {
    if (this.isPlaying) return;

    if (this.currentTrack?.isSystemMedia) {
      this.isPlaying = true;
      this.sendUpdate();
    } else {
      this.isPlaying = true;
      this.startPlaybackLoop();
      this.sendUpdate();
    }
  }

  mockPause() {
    if (!this.isPlaying) return;

    if (this.currentTrack?.isSystemMedia) {
      this.isPlaying = false;
      this.sendUpdate();
    } else {
      this.isPlaying = false;
      this.stopPlaybackLoop();
      this.sendUpdate();
    }
  }

  mockToggle() {
    if (this.isPlaying) {
      this.mockPause();
    } else {
      this.mockPlay();
    }
  }

  mockNext() {
    if (this.currentTrack?.isSystemMedia) return;

    this.currentMockTrackIndex = (this.currentMockTrackIndex + 1) % this.mockTracks.length;
    this.currentTrack = { ...this.mockTracks[this.currentMockTrackIndex], isSystemMedia: false };
    this.currentPosition = 0;
    this.trackDuration = this.currentTrack.duration;

    if (this.isPlaying) {
      this.stopPlaybackLoop();
      this.startPlaybackLoop();
    }

    this.sendUpdate();
  }

  mockPrevious() {
    if (this.currentTrack?.isSystemMedia) return;

    this.currentMockTrackIndex = (this.currentMockTrackIndex - 1 + this.mockTracks.length) % this.mockTracks.length;
    this.currentTrack = { ...this.mockTracks[this.currentMockTrackIndex], isSystemMedia: false };
    this.currentPosition = 0;
    this.trackDuration = this.currentTrack.duration;

    if (this.isPlaying) {
      this.stopPlaybackLoop();
      this.startPlaybackLoop();
    }

    this.sendUpdate();
  }

  startPlaybackLoop() {
    if (this.playbackInterval) return;

    this.playbackInterval = setInterval(() => {
      if (this.currentPosition >= this.trackDuration) {
        this.mockNext();
        return;
      }
      
      this.currentPosition += 0.1;
      this.sendUpdate();
    }, 100);
  }

  stopPlaybackLoop() {
    if (this.playbackInterval) {
      clearInterval(this.playbackInterval);
      this.playbackInterval = null;
    }
  }

  pollTimeline() {
    if (!this.isEnabled || !this.musicControlEnabled) return;
    
    if (this.currentTrack?.isSystemMedia && this.isPlaying) {
      this.sendUpdate();
    }
  }

  setEnabled(enabled) {
    console.log('[MediaController] setEnabled:', enabled);
    this.isEnabled = enabled;
    
    if (!enabled) {
      this.stopPlaybackLoop();
      this.currentTrack = null;
      this.isPlaying = false;
      this.currentPosition = 0;
      this.trackDuration = 0;
      this.hasSystemMedia = false;
    } else {
      this.tryStartMonitoring();
    }
  }

  setMusicControlEnabled(enabled) {
    console.log('[MediaController] setMusicControlEnabled:', enabled);
    this.musicControlEnabled = enabled;
    
    if (!enabled) {
      this.stopPlaybackLoop();
    } else {
      this.tryStartMonitoring();
    }
  }

  tryStartMonitoring() {
    if (!this.isEnabled || !this.musicControlEnabled) return;
    
    if (this.nativeServiceAvailable) {
      console.log('[MediaController] Native service available, waiting for media updates...');
    } else {
      console.log('[MediaController] Native service not available, using mock data');
      this.useMockDataIfNeeded();
    }
  }

  sendUpdate() {
    try {
      console.log('[MediaController] sendUpdate check - isEnabled:', this.isEnabled, 'musicControlEnabled:', this.musicControlEnabled, 'has currentTrack:', !!this.currentTrack);
      if (!this.currentTrack) {
        console.log('[MediaController] sendUpdate skipped - no currentTrack');
        return;
      }
      
      // 即使 isEnabled 或 musicControlEnabled 为 false，仍然发送音乐信息更新
      // 让前端决定是否显示

      const updateData = {
        ...this.currentTrack,
        isPlaying: this.isPlaying,
        position: this.currentPosition,
        duration: this.trackDuration
      };

      if (this.mainWindow && !this.mainWindow.isDestroyed()) {
        this.mainWindow.webContents.send('media-update', updateData);
      }

      if (this.dynamicIslandWindow && !this.dynamicIslandWindow.isDestroyed()) {
        this.dynamicIslandWindow.webContents.send('media-update', updateData);
        console.log('[MediaController] Sent to dynamicIslandWindow');
      } else {
        console.log('[MediaController] dynamicIslandWindow not available');
      }
    } catch (error) {
      console.error('[MediaController] Error in sendUpdate method:', error);
    }
  }

  destroy() {
    this.stopPlaybackLoop();
    this.currentTrack = null;
    this.isPlaying = false;
    this.currentPosition = 0;
    this.trackDuration = 0;
  }
}

export default MediaController;
