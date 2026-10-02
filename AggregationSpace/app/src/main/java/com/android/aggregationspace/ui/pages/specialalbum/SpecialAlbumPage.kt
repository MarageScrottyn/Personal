package com.android.aggregationspace.ui.pages.specialalbum

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
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
import com.android.aggregationspace.ui.global.SpecialAlbumDataStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SpecialItem(
    val id: Int,
    val thumbnailUrl: String,
    val videoFile: String,
    val title: String,
    val duration: String,
    val description: String,
    val tags: List<String>
)

private const val TAG = "SpecialAlbum"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

/** 根据ID获取特辑视频（从内存数据存储获取） */
fun getSpecialVideoById(videoId: Int): SpecialItem {
    return SpecialAlbumDataStore.currentVideo ?: SpecialItem(
        id = videoId,
        thumbnailUrl = "",
        videoFile = "",
        title = "未知视频",
        duration = "00:00:00",
        description = "暂无描述",
        tags = emptyList()
    )
}

private fun jsonArrayToList(array: JSONArray): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until array.length()) {
        result.add(array.getString(i))
    }
    return result
}

fun parseSpecialListManual(json: String): List<SpecialItem> {
    val list = mutableListOf<SpecialItem>()

    try {
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val item = SpecialItem(
                id = obj.optInt("id", i),
                thumbnailUrl = obj.optString("thumbnail", ""),
                videoFile = obj.optString("video_file", ""),
                title = obj.optString("title", ""),
                duration = obj.optString("duration", "00:00:00"),
                description = obj.optString("slug", ""),
                tags = try {
                    jsonArrayToList(obj.getJSONArray("category_names"))
                } catch (e: Exception) {
                    emptyList()
                }
            )

            list.add(item)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析特辑数据失败: ${e.message}")
    }

    return list
}

@Composable
fun SpecialAlbumPage(navController: NavController) {
    // 使用全局缓存，避免返回首页时重新加载（隐私模式切换会自动清空）
    val paginationState = PageCache.pagination<SpecialItem>("specialEdit", PAGE_SIZE)
    val gridState = PageCache.gridState("specialEdit")
    val scope = rememberCoroutineScope()

    /** 加载第一页数据（仅在缓存为空时加载） */
    LaunchedEffect(Unit) {
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        paginationState.updateLoading(true)
        try {
            val result = ApiService.get(
                endpoint = ApiEndpoints.SpecialAlbum.LIST,
                queryParams = mapOf("type" to "special", "limit" to PAGE_SIZE.toString(), "offset" to "0"),
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseSpecialListManual(dataJson)
                    }
                    list
                }
            )

            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
                Log.d(TAG, "特辑数据加载成功: ${result.data!!.size} 条")
            } else {
                paginationState.setError(result.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取特辑数据失败: ${e.message}", e)
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
                        endpoint = ApiEndpoints.SpecialAlbum.LIST,
                        queryParams = mapOf(
                            "type" to "special",
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to paginationState.getNextOffset().toString()
                        ),
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseSpecialListManual(dataJson)
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
                    Log.e(TAG, "加载更多特辑失败: ${e.message}")
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
                LoadingFullScreen(message = "加载特辑中...")
            }
            paginationState.isLoading && paginationState.items.isNotEmpty() -> {
                SkeletonLoader(itemCount = 4, itemHeight = 200.dp)
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
                EmptyState(message = "暂无特辑")
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(paginationState.items, key = { it.id }) { video ->
                        SpecialVideoCard(video, navController)
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
private fun SpecialVideoCard(video: SpecialItem, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                SpecialAlbumDataStore.currentVideo = video
                navController.navigate(
                    "specialPlayer/${video.id}"
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
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
                        modifier = Modifier.size(40.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.duration,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1
                    )

                    if (video.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clipToBounds(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            video.tags.take(3).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF007AFF).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        color = Color(0xFF007AFF),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}