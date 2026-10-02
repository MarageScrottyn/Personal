package com.android.aggregationspace.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import kotlinx.coroutines.delay
import com.android.aggregationspace.R

@Composable
fun VideoPlayerSurface(
    videoUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onFullscreenChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // 保存播放状态和进度，用于屏幕旋转后恢复
    var wasPlaying by rememberSaveable { mutableStateOf(false) }
    var savedPosition by rememberSaveable { mutableLongStateOf(0L) }
    var savedSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false
            volume = 1.0f

            if (savedPosition > 0) {
                seekTo(savedPosition)
            }
            playbackParameters = androidx.media3.common.PlaybackParameters(savedSpeed)

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    wasPlaying = playing
                }
            })
        }
    }

    // 屏幕旋转后恢复播放状态
    LaunchedEffect(exoPlayer, wasPlaying) {
        if (wasPlaying && !exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    // 监听播放状态，播放时延迟隐藏，暂停时立即显示
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(3000)
            showControls = false
            showVolumeSlider = false
            showMoreMenu = false
        } else {
            showControls = true
        }
    }

    // 持续更新进度和时长
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            savedPosition = exoPlayer.currentPosition
            val d = exoPlayer.duration
            if (d > 0) duration = d
            delay(200)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 全屏状态变化时立即设置屏幕方向，不依赖生命周期事件
    DisposableEffect(isFullscreen) {
        val activity = context as? Activity
        activity?.apply {
            if (isFullscreen) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.hide(WindowInsets.Type.statusBars())
                }
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsets.Type.statusBars())
                }
            }
        }
        onDispose {
            val activity = context as? Activity
            activity?.apply {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsets.Type.statusBars())
                }
            }
        }
    }

    BackHandler(isFullscreen) {
        onFullscreenChange(false)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f))
    ) {
        // 视频画面
        PlayerSurface(
            player = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )

        // 点击视频区域：切换控制栏显示和隐藏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    showControls = !showControls
                    showVolumeSlider = false
                    showMoreMenu = false
                }
        )

        if (showControls) {
            VideoControlsOverlay(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                isFullscreen = isFullscreen,
                volume = volume,
                showVolumeSlider = showVolumeSlider,
                showMoreMenu = showMoreMenu,
                playbackSpeed = playbackSpeed,
                onBack = onBack,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSeek = { progress ->
                    if (duration > 0) {
                        exoPlayer.seekTo((progress * duration).toLong())
                    }
                },
                onFullscreenClick = { onFullscreenChange(!isFullscreen) },
                // 点击音量按钮：显示/隐藏音量滑块，同时隐藏倍速菜单（两者互斥）
                onVolumeClick = {
                    showVolumeSlider = !showVolumeSlider
                    showMoreMenu = false
                },
                onVolumeChange = {
                    volume = it
                    exoPlayer.volume = it
                },
                // 点击更多按钮：显示/隐藏倍速菜单，同时隐藏音量滑块（两者互斥）
                onMoreClick = {
                    showMoreMenu = !showMoreMenu
                    showVolumeSlider = false
                },
                onSpeedChange = {
                    playbackSpeed = it
                    savedSpeed = it
                    exoPlayer.playbackParameters =
                        androidx.media3.common.PlaybackParameters(it)
                }
            )
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isFullscreen: Boolean,
    volume: Float,
    showVolumeSlider: Boolean,
    showMoreMenu: Boolean,
    playbackSpeed: Float,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onFullscreenClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMoreClick: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 顶部返回
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        // 底部控制栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
        ) {
            VideoBottomControlBar(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                isFullscreen = isFullscreen,
                volume = volume,
                showVolumeSlider = showVolumeSlider,
                showMoreMenu = showMoreMenu,
                playbackSpeed = playbackSpeed,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
                onFullscreenClick = onFullscreenClick,
                onVolumeClick = onVolumeClick,
                onVolumeChange = onVolumeChange,
                onMoreClick = onMoreClick,
                onSpeedChange = onSpeedChange
            )
        }
        // 中间播放按钮组
        VideoPlayButtons(
            isPlaying = isPlaying,
            onClick = onPlayPause
        )
    }
}

@Composable
fun VideoPlayButtons(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 中间播放按钮
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            val iconPainter = if (isPlaying)
                painterResource(R.drawable.baseline_pause_24)
            else
                painterResource(R.drawable.baseline_play_arrow_24)

            Icon(
                painter = iconPainter,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(48.dp) // 图标大小
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoBottomControlBar(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isFullscreen: Boolean,
    volume: Float,
    showVolumeSlider: Boolean,
    showMoreMenu: Boolean,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onFullscreenClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMoreClick: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                )
            )
            .padding(bottom = 8.dp, top = 60.dp)
    ) {
        // 声音设置
        Column {
            if (showVolumeSlider) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_volume_up_24),
                        contentDescription = "最小音量",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier
                            .width(200.dp)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        track = { state ->
                            BoxWithConstraints(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            ) {
                                val activeW = maxWidth * state.value
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.3f))
                                ) {
                                    Box(
                                        Modifier
                                            .width(activeW)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    )
                    Icon(
                        painter = painterResource(R.drawable.baseline_volume_up_24),
                        contentDescription = "最大音量",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // 倍速设置
            if (showMoreMenu) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "播放速度",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        speeds.forEach { speed ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (speed == playbackSpeed) Color.White else Color.White.copy(0.2f))
                                    .clickable { onSpeedChange(speed) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (speed == playbackSpeed) Color.Black else Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 功能菜单
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = onSeek,
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    track = { state ->
                        BoxWithConstraints(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            val activeW = maxWidth * state.value
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.3f))
                            ) {
                                Box(
                                    Modifier
                                        .width(activeW)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_volume_up_24),
                        contentDescription = "音量",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onVolumeClick() }
                    )

                    val fullScreenIcon = if (isFullscreen)
                        painterResource(R.drawable.baseline_fullscreen_exit_24)
                    else
                        painterResource(R.drawable.baseline_fullscreen_24)

                    Icon(
                        painter = fullScreenIcon,
                        contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onFullscreenClick() }
                    )

                    Icon(
                        painter = painterResource(R.drawable.baseline_more_vert_24),
                        contentDescription = "更多",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onMoreClick() }
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${String.format("%02d", seconds)}"
}
