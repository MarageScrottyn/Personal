package com.android.aggregationspace.ui.album

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.api.AuthImageLoader
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import com.android.aggregationspace.ui.components.InfiniteStaggeredGridLoader
import com.android.aggregationspace.ui.components.LoadingFullScreen
import com.android.aggregationspace.ui.components.LoadingMoreFooter
import com.android.aggregationspace.ui.components.PaginationState
import com.android.aggregationspace.ui.components.parsePaginatedResponse
import com.android.aggregationspace.ui.global.PageCache
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class AlbumItem(
    val id: String,
    val coverUrl: String,
    val name: String,
    val author: String,
    val images: List<AlbumImageItem> = emptyList()
)

data class AlbumImageItem(
    val id: String,
    val imageUrl: String,
    val title: String = "",
    val order: Int = 0
)

private const val TAG = "AlbumPage"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

/** 从 API 响应中解析图集列表 */
private fun parseAlbumListFromApi(json: String): List<AlbumItem> {
    val list = mutableListOf<AlbumItem>()
    try {
        // 兼容两种格式：直接是数组，或包装在 results 字段中
        val albumsArray = if (json.trimStart().startsWith("[")) {
            JSONArray(json)
        } else {
            val jsonObj = JSONObject(json)
            if (jsonObj.has("results")) {
                jsonObj.getJSONArray("results")
            } else if (jsonObj.has("id")) {
                JSONArray().apply { put(jsonObj) }
            } else {
                JSONArray()
            }
        }

        for (i in 0 until albumsArray.length()) {
            val item = albumsArray.getJSONObject(i)
            val id = item.optString("id", "")
            val resourceId = item.optString("resource_id", "")
            val name = item.optString("title", item.optString("name", ""))
            val author = item.optString("author", "")
            val coverUrl = item.optString("cover_image", "")

            // 解析 images 列表
            val imagesArray = item.optJSONArray("images")
            val parsedImages = mutableListOf<AlbumImageItem>()
            if (imagesArray != null) {
                for (j in 0 until imagesArray.length()) {
                    val img = imagesArray.getJSONObject(j)
                    parsedImages.add(
                        AlbumImageItem(
                            id = img.optString("id", "$j"),
                            imageUrl = img.optString("path", ""),
                            title = img.optString("title", ""),
                            order = img.optInt("order", j)
                        )
                    )
                }
            }

            list.add(
                AlbumItem(
                    id = resourceId.ifEmpty { id },
                    coverUrl = coverUrl,
                    name = name,
                    author = author,
                    images = parsedImages
                )
            )
        }
        Log.d(TAG, "解析成功: ${list.size} 个图集")
    } catch (e: Exception) {
        Log.e(TAG, "解析异常: ${e.message}", e)
    }
    return list
}

@Composable
fun AlbumPage(navController: NavController) {
    // 使用全局缓存，避免返回首页时重新加载（隐私模式切换会自动清空）
    val paginationState = PageCache.pagination<AlbumItem>("album", PAGE_SIZE)
    val gridState = PageCache.staggeredGridState("album")
    val scope = rememberCoroutineScope()

    /** 加载第一页数据（仅在缓存为空时加载） */
    LaunchedEffect(Unit) {
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        paginationState.updateLoading(true)
        try {
            val result = ApiService.get(
                endpoint = ApiEndpoints.Album.LIST,
                queryParams = mapOf("limit" to PAGE_SIZE.toString(), "offset" to "0"),
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseAlbumListFromApi(dataJson)
                    }
                    list
                }
            )
            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
                Log.d(TAG, "图集数据加载成功: ${result.data!!.size} 条")
            } else {
                paginationState.setError(result.message)
                Log.e(TAG, "图集加载失败: ${result.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取图集数据失败: ${e.message}")
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
                        endpoint = ApiEndpoints.Album.LIST,
                        queryParams = mapOf(
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to paginationState.getNextOffset().toString()
                        ),
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseAlbumListFromApi(dataJson)
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
                    Log.e(TAG, "加载更多图集失败: ${e.message}")
                    paginationState.setError("加载更多失败")
                } finally {
                    paginationState.updateLoadingMore(false)
                }
            }
        }
    }

    /** 监听滚动加载 */
    InfiniteStaggeredGridLoader(
        gridState = gridState,
        paginationState = paginationState,
        buffer = 3,
        onLoadMore = loadMore
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when {
            paginationState.isLoading && paginationState.items.isEmpty() -> {
                LoadingFullScreen(message = "加载图集中...")
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
                EmptyState(message = "暂无图集")
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 56.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = paginationState.items, key = { it.id }) { album ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 将图片列表序列化为 JSON 传递给查看页
                                    val imagesJson = JSONArray().apply {
                                        album.images.forEach { img ->
                                            put(JSONObject().apply {
                                                put("id", img.id)
                                                put("imageUrl", img.imageUrl)
                                                put("title", img.title)
                                                put("order", img.order)
                                            })
                                        }
                                    }.toString()
                                    val encodedImages = Uri.encode(imagesJson)
                                    val encodedTitle = Uri.encode(album.name)
                                    navController.navigate(
                                        "pictureAlbumViewer/${album.id}?title=$encodedTitle&images=$encodedImages"
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = album.coverUrl,
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    imageLoader = AuthImageLoader.imageLoader
                                )
                            }
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )
                            Text(
                                text = album.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            )
                        }
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
