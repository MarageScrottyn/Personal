package com.android.aggregationspace.ui.pages.vedio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.aggregationspace.ui.components.VideoPlayerSurface

data class VideoDetail(
    val id: String,
    val thumbnailUrl: String,
    val title: String,
    val duration: String,
    val description: String,
    val videoUrl: String = "https://vjs.zencdn.net/v/oceans.mp4"
)

private fun getVideoById(videoId: String): VideoDetail {
    val videos = mapOf(
        "1" to VideoDetail("1", "https://picsum.photos/seed/video1/400/225", "风景纪录片", "01:23:45", "探索大自然的壮美景色"),
        "2" to VideoDetail("2", "https://picsum.photos/seed/video2/400/225", "城市夜景", "00:45:30", "繁华都市的夜晚"),
        "3" to VideoDetail("3", "https://picsum.photos/seed/video3/400/225", "海洋探秘", "02:15:00", "深海世界的奥秘"),
        "4" to VideoDetail("4", "https://picsum.photos/seed/video4/400/225", "雪山探险", "01:05:20", "攀登雪山的挑战"),
        "5" to VideoDetail("5", "https://picsum.photos/seed/video5/400/225", "美食制作", "00:05:30", "美味佳肴的制作过程"),
        "6" to VideoDetail("6", "https://picsum.photos/seed/video6/400/225", "旅行日记", "00:08:45", "精彩的旅行记录"),
        "7" to VideoDetail("7", "https://picsum.photos/seed/video7/400/225", "星空观测", "01:45:00", "浩瀚星空的魅力"),
        "8" to VideoDetail("8", "https://picsum.photos/seed/video8/400/225", "动物世界", "02:00:15", "野生动物的生活"),
        "9" to VideoDetail("9", "https://picsum.photos/seed/video9/400/225", "音乐MV", "00:04:30", "精彩的音乐视频"),
        "10" to VideoDetail("10", "https://picsum.photos/seed/video10/400/225", "科技前沿", "00:12:00", "最新科技资讯"),
        "m1" to VideoDetail("m1", "https://picsum.photos/seed/movie1/400/225", "电影大片", "02:30:00", "震撼的视觉盛宴"),
        "m2" to VideoDetail("m2", "https://picsum.photos/seed/movie2/400/225", "科幻巨制", "01:55:30", "未来世界的想象"),
        "m3" to VideoDetail("m3", "https://picsum.photos/seed/movie3/400/225", "喜剧电影", "01:40:00", "欢乐的观影体验"),
        "m4" to VideoDetail("m4", "https://picsum.photos/seed/movie4/400/225", "爱情故事", "01:45:00", "浪漫的爱情故事"),
        "t1" to VideoDetail("t1", "https://picsum.photos/seed/tv1/400/225", "热门剧集", "00:45:00", "精彩的剧情发展"),
        "t2" to VideoDetail("t2", "https://picsum.photos/seed/tv2/400/225", "悬疑剧集", "00:50:30", "扣人心弦的悬疑故事"),
        "t3" to VideoDetail("t3", "https://picsum.photos/seed/tv3/400/225", "古装剧", "00:48:00", "古代风情的演绎"),
        "t4" to VideoDetail("t4", "https://picsum.photos/seed/tv4/400/225", "现代剧", "00:42:00", "现代生活的写照"),
        "s1" to VideoDetail("s1", "https://picsum.photos/seed/short1/400/225", "搞笑短视频", "00:03:20", "轻松搞笑的内容"),
        "s2" to VideoDetail("s2", "https://picsum.photos/seed/short2/400/225", "美食教程", "00:05:10", "简单易学的美食制作"),
        "s3" to VideoDetail("s3", "https://picsum.photos/seed/short3/400/225", "旅行Vlog", "00:06:45", "精彩的旅行记录"),
        "s4" to VideoDetail("s4", "https://picsum.photos/seed/short4/400/225", "健身训练", "00:04:30", "专业的健身指导")
    )
    return videos[videoId] ?: VideoDetail(videoId, "https://picsum.photos/seed/default/400/225", "未知视频", "00:00:00", "暂无描述")
}

@Composable
fun VideoPlayer(navController: NavController, videoId: String) {
    val video = getVideoById(videoId)
    val isFullscreen = rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 透明背景，让 MainActivity 中的全局 AppBackground 可见
            .background(Color.Transparent)
            .statusBarsPadding()
    ) {
        VideoPlayerSurface(
            videoUrl = video.videoUrl,
            onBack = { navController.popBackStack() },
            isFullscreen = isFullscreen.value,
            onFullscreenChange = { isFullscreen.value = it }
        )

        if (!isFullscreen.value) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = video.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
