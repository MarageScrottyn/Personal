package com.android.aggregationspace.ui.pages.vedio

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.android.aggregationspace.api.ApiConfig
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.api.AuthImageLoader
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import com.android.aggregationspace.ui.components.InfiniteGridLoader
import com.android.aggregationspace.ui.components.LoadingFullScreen
import com.android.aggregationspace.ui.components.LoadingMoreFooter
import com.android.aggregationspace.ui.components.PaginationState
import com.android.aggregationspace.ui.components.parsePaginatedResponse
import com.android.aggregationspace.ui.components.SkeletonLoader
import com.android.aggregationspace.ui.global.PageCache
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "VideoPage"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

data class VideoItem(
    val id: Int,
    val thumbnailUrl: String,
    val videoFile: String,
    val title: String,
    val duration: String,
    val type: String
)

private fun jsonArrayToList(array: JSONArray): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until array.length()) {
        result.add(array.getString(i))
    }
    return result
}

private fun parseVideoListFromApi(json: String): List<VideoItem> {
    val list = mutableListOf<VideoItem>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val item = VideoItem(
                id = obj.optInt("id", i),
                thumbnailUrl = obj.optString("thumbnail", ""),
                videoFile = obj.optString("video_file", ""),
                title = obj.optString("title", ""),
                duration = obj.optString("duration", "00:00:00"),
                type = obj.optString("video_type", "video")
            )
            list.add(item)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析视频数据失败: ${e.message}")
    }
    return list
}

@Composable
fun VideoPage(navController: NavController) {
    // 使用全局缓存，避免返回首页时重新加载（隐私模式切换会自动清空）
    val paginationState = PageCache.pagination<VideoItem>("video", PAGE_SIZE)
    val gridState = PageCache.gridState("video")
    val scope = rememberCoroutineScope()

    /** 加载第一页数据（仅在缓存为空时加载） */
    LaunchedEffect(Unit) {
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        paginationState.updateLoading(true)
        try {
            val result = ApiService.get(
                endpoint = ApiEndpoints.Video.LIST,
                queryParams = mapOf("type" to "video", "limit" to PAGE_SIZE.toString(), "offset" to "0"),
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseVideoListFromApi(dataJson)
                    }
                    list
                }
            )
            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
            } else {
                paginationState.setError(result.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取视频数据失败: ${e.message}")
            paginationState.setError("网络请求失败")
        } finally {
            paginationState.updateLoading(false)
        }
    }

    /** 加载更多数据的函数 */
    val loadMore = {
        if (paginationState.shouldLoadMore()) {
            scope.launch {
                paginationState.updateLoadingMore(true)
                try {
                    val result = ApiService.get(
                        endpoint = ApiEndpoints.Video.LIST,
                        queryParams = mapOf(
                            "type" to "video",
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to paginationState.getNextOffset().toString()
                        ),
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseVideoListFromApi(dataJson)
                            }
                            list
                        }
                    )
                    if (result.success && result.data != null) {
                        paginationState.appendData(result.data!!)
                    } else {
                        paginationState.setError(result.message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "加载更多视频失败: ${e.message}")
                    paginationState.setError("加载更多失败")
                } finally {
                    paginationState.updateLoadingMore(false)
                }
            }
        }
    }

    /** 监听滚动加载 */
    InfiniteGridLoader(
        gridState = gridState,
        paginationState = paginationState,
        buffer = 3,
        onLoadMore = loadMore
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 56.dp)
            .statusBarsPadding()
    ) {
        when {
            paginationState.isLoading && paginationState.items.isEmpty() -> {
                LoadingFullScreen(message = "加载视频中...")
            }
            paginationState.isLoading && paginationState.items.isNotEmpty() -> {
                SkeletonLoader(itemCount = 4, itemHeight = 180.dp)
            }
            paginationState.errorMessage != null && paginationState.items.isEmpty() -> {
                ErrorState(
                    message = paginationState.errorMessage ?: "加载失败",
                    onRetry = {
                        paginationState.reset()
                    }
                )
            }
            paginationState.items.isEmpty() -> {
                EmptyState(message = "暂无视频")
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(paginationState.items, key = { it.id }) { video ->
                        VideoCard(video, navController)
                    }
                    item {
                        LoadingMoreFooter(
                            isLoading = paginationState.isLoadingMore,
                            hasMore = paginationState.hasMore
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoviePage(navController: NavController) {
    VideoPage(navController)
}

@Composable
fun TvPage(navController: NavController) {
    VideoPage(navController)
}

@Composable
fun ShortVideoPage(navController: NavController) {
    VideoPage(navController)
}

@Composable
private fun VideoCard(video: VideoItem, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                navController.navigate("videoPlayer/${video.id}")
            }
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            imageLoader = AuthImageLoader.imageLoader
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}