package com.android.aggregationspace.ui.pages.comic

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.ui.components.ComicCard
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import com.android.aggregationspace.ui.components.InfiniteStaggeredGridLoader
import com.android.aggregationspace.ui.components.LoadingFullScreen
import com.android.aggregationspace.ui.components.LoadingMoreFooter
import com.android.aggregationspace.ui.components.PaginationState
import com.android.aggregationspace.ui.components.parsePaginatedResponse
import com.android.aggregationspace.ui.components.SkeletonLoader
import com.android.aggregationspace.ui.global.PageCache
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ComicPage"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

data class ComicItem(
    val id: String,
    val slug: String,
    val imageUrl: String,
    val title: String,
    val labels: List<String>
)

private fun jsonArrayToList(array: JSONArray): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until array.length()) {
        result.add(array.getString(i))
    }
    return result
}

private fun parseComicListFromApi(json: String): List<ComicItem> {
    val list = mutableListOf<ComicItem>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val item = ComicItem(
                id = obj.optString("id", "$i"),
                slug = obj.optString("slug", ""),
                imageUrl = obj.optString("cover_image", ""),
                title = obj.optString("title", ""),
                labels = try {
                    jsonArrayToList(obj.getJSONArray("category_names"))
                } catch (e: Exception) {
                    emptyList()
                }
            )
            list.add(item)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析漫画数据失败: ${e.message}")
    }
    return list
}

@Composable
fun ComicPage(navController: NavController) {
    // 使用全局缓存，避免返回首页时重新加载（隐私模式切换会自动清空）
    val paginationState = PageCache.pagination<ComicItem>("comic", PAGE_SIZE)
    val gridState = PageCache.staggeredGridState("comic")
    val scope = rememberCoroutineScope()

    /** 加载第一页数据（仅在缓存为空时加载） */
    LaunchedEffect(Unit) {
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        paginationState.updateLoading(true)
        try {
            val result = ApiService.get(
                endpoint = ApiEndpoints.Comic.LIST,
                queryParams = mapOf("limit" to PAGE_SIZE.toString(), "offset" to "0"),
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseComicListFromApi(dataJson)
                    }
                    list
                }
            )
            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
                Log.d(TAG, "漫画数据加载成功: ${result.data!!.size} 条")
            } else {
                paginationState.setError(result.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取漫画数据失败: ${e.message}")
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
                        endpoint = ApiEndpoints.Comic.LIST,
                        queryParams = mapOf(
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to paginationState.getNextOffset().toString()
                        ),
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseComicListFromApi(dataJson)
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
                    Log.e(TAG, "加载更多漫画失败: ${e.message}")
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
                LoadingFullScreen(message = "加载漫画中...")
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
                EmptyState(message = "暂无漫画")
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    items(items = paginationState.items, key = { it.id }) { comic ->
                        ComicCard(
                            imageURL = comic.imageUrl,
                            imageName = comic.title,
                            labelList = comic.labels,
                            onClick = {
                                // 后端 ComicDetailView 使用 slug 作为查找字段
                                navController.navigate("comicDetails/${comic.slug}")
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