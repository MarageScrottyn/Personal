package com.android.aggregationspace.ui.pages.picture

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import com.android.aggregationspace.ui.components.InfiniteStaggeredGridLoader
import com.android.aggregationspace.ui.components.LoadingFullScreen
import com.android.aggregationspace.ui.components.LoadingMoreFooter
import com.android.aggregationspace.ui.components.PaginationState
import com.android.aggregationspace.ui.components.parsePaginatedResponse
import com.android.aggregationspace.ui.components.SkeletonLoader
import com.android.aggregationspace.ui.components.PictureCard
import com.android.aggregationspace.ui.global.PageCache
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val TAG = "PicturePage"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

data class PictureItem(
    val id: String,
    val imageUrl: String
)

/** 从API解析图片列表 */
private fun parseImageListFromApi(json: String): List<PictureItem> {
    val list = mutableListOf<PictureItem>()
    try {
        val jsonArray = org.json.JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val item = PictureItem(
                id = obj.optString("id", "$i"),
                imageUrl = obj.optString("image_file", obj.optString("path", ""))
            )
            if (item.imageUrl.isNotEmpty()) {
                list.add(item)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析图片数据失败: ${e.message}")
    }
    return list
}

@Composable
fun PicturePage(navController: NavController) {
    // 使用全局缓存，避免返回首页时重新加载（隐私模式切换会自动清空）
    val paginationState = PageCache.pagination<PictureItem>("picture", PAGE_SIZE)
    val gridState = PageCache.staggeredGridState("picture")
    val scope = rememberCoroutineScope()

    /** 加载第一页数据（仅在缓存为空时加载） */
    LaunchedEffect(Unit) {
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        paginationState.updateLoading(true)
        try {
            val result = ApiService.get(
                endpoint = ApiEndpoints.Image.LIST,
                // type=image：图片分类只拉取独立单张图片，排除图集图片/封面，实现与图集的隔离
                queryParams = mapOf("limit" to PAGE_SIZE.toString(), "offset" to "0", "type" to "image"),
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseImageListFromApi(dataJson)
                    }
                    list
                }
            )
            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
                Log.d(TAG, "图片数据加载成功: ${result.data!!.size} 条")
            } else {
                paginationState.setError(result.message)
                Log.e(TAG, "图片加载失败: ${result.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取图片数据失败: ${e.message}")
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
                        endpoint = ApiEndpoints.Image.LIST,
                        // 分页加载同样限定 type=image，保持图片分类与图集隔离
                        queryParams = mapOf(
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to paginationState.getNextOffset().toString(),
                            "type" to "image"
                        ),
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseImageListFromApi(dataJson)
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
                    Log.e(TAG, "加载更多图片失败: ${e.message}")
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
                LoadingFullScreen(message = "加载图片中...")
            }
            paginationState.isLoading && paginationState.items.isNotEmpty() -> {
                SkeletonLoader(itemCount = 4)
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
                EmptyState(message = "暂无图片")
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 56.dp),
                    verticalItemSpacing = 4.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = paginationState.items, key = { it.id }) { picture ->
                        PictureCard(
                            imageUrl = picture.imageUrl,
                            onClick = {
                                navController.navigate("fullscreenImage/${URLEncoder.encode(picture.imageUrl, StandardCharsets.UTF_8)}")
                            }
                        )
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