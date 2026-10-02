package com.android.aggregationspace.ui.components

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

private const val TAG = "PaginationState"

/**
 * 分页数据状态类
 * 管理列表数据的分页加载状态
 */
class PaginationState<T>(
    private val pageSize: Int = 20
) {
    /** 数据列表 */
    var items by mutableStateOf<List<T>>(emptyList())
        private set

    /** 是否正在加载 */
    var isLoading by mutableStateOf(false)
        private set

    /** 是否正在加载更多 */
    var isLoadingMore by mutableStateOf(false)
        private set

    /** 是否还有更多数据 */
    var hasMore by mutableStateOf(true)
        private set

    /** 错误信息 */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** 当前偏移量 */
    private var offset by mutableIntStateOf(0)

    /** 是否是首次加载 */
    var isFirstLoad by mutableStateOf(true)
        private set

    /** 重置状态 */
    fun reset() {
        items = emptyList()
        isLoading = false
        isLoadingMore = false
        hasMore = true
        errorMessage = null
        offset = 0
        isFirstLoad = true
    }

    /** 更新加载状态 */
    fun updateLoading(loading: Boolean) {
        isLoading = loading
        if (loading) {
            errorMessage = null
        }
    }

    /** 更新加载更多状态 */
    fun updateLoadingMore(loading: Boolean) {
        isLoadingMore = loading
    }

    /** 添加数据 */
    fun appendData(newItems: List<T>) {
        if (isFirstLoad) {
            items = newItems
            isFirstLoad = false
        } else {
            items = items + newItems
        }
        offset = items.size
        hasMore = newItems.size >= pageSize
        Log.d(TAG, "数据已加载: ${items.size} 条, hasMore=$hasMore")
    }

    /** 设置错误 */
    fun setError(error: String?) {
        errorMessage = error
        if (error != null) {
            isLoading = false
            isLoadingMore = false
        }
    }

    /** 获取下一次请求的offset参数 */
    fun getNextOffset(): Int {
        return offset
    }

    /**
     * 判断是否应该加载更多
     * 当列表滚动到接近底部时触发
     */
    fun shouldLoadMore(): Boolean {
        return !isLoading && !isLoadingMore && hasMore && errorMessage == null
    }
}

/**
 * 解析DRF分页响应
 * @return Triple(数据列表, 总数, 是否还有更多)
 */
fun <T> parsePaginatedResponse(
    json: String,
    parser: (String) -> List<T>
): Triple<List<T>, Int, Boolean> {
    return try {
        val trimmed = json.trimStart()
        if (trimmed.startsWith("[")) {
            val list = parser(json)
            Triple(list, list.size, false)
        } else {
            val obj = org.json.JSONObject(json)
            val count = obj.optInt("count", 0)
            val results = obj.optJSONArray("results")
            val list = if (results != null) {
                parser(results.toString())
            } else {
                emptyList()
            }
            val next = obj.optString("next", null)
            val hasMore = next != null && next.isNotEmpty()
            Triple(list, count, hasMore)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析分页响应失败: ${e.message}")
        Triple(emptyList(), 0, false)
    }
}

/**
 * 监听LazyListState滚动到底部触发加载更多
 * @param listState 列表状态
 * @param buffer 缓冲距离，距离底部多少项时触发
 * @param onLoadMore 加载更多回调
 */
@Composable
fun <T> InfiniteListLoader(
    listState: LazyListState,
    paginationState: PaginationState<T>,
    buffer: Int = 3,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, paginationState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .filter { lastIndex ->
                lastIndex >= listState.layoutInfo.totalItemsCount - buffer &&
                paginationState.shouldLoadMore()
            }
            .distinctUntilChanged()
            .collect {
                Log.d(TAG, "触发加载更多: lastIndex=$it")
                onLoadMore()
            }
    }
}

/**
 * 监听LazyGridState滚动到底部触发加载更多
 * @param gridState 网格状态
 * @param buffer 缓冲距离，距离底部多少行时触发
 * @param onLoadMore 加载更多回调
 */
@Composable
fun <T> InfiniteGridLoader(
    gridState: LazyGridState,
    paginationState: PaginationState<T>,
    buffer: Int = 3,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(gridState, paginationState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .filter { lastIndex ->
                lastIndex >= gridState.layoutInfo.totalItemsCount - buffer &&
                paginationState.shouldLoadMore()
            }
            .distinctUntilChanged()
            .collect {
                Log.d(TAG, "触发加载更多: lastIndex=$it")
                onLoadMore()
            }
    }
}

/**
 * 监听LazyStaggeredGridState滚动到底部触发加载更多
 * @param gridState 交错网格状态
 * @param buffer 缓冲距离，距离底部多少项时触发
 * @param onLoadMore 加载更多回调
 */
@Composable
fun <T> InfiniteStaggeredGridLoader(
    gridState: LazyStaggeredGridState,
    paginationState: PaginationState<T>,
    buffer: Int = 3,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(gridState, paginationState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .filter { lastIndex ->
                lastIndex >= gridState.layoutInfo.totalItemsCount - buffer &&
                paginationState.shouldLoadMore()
            }
            .distinctUntilChanged()
            .collect {
                Log.d(TAG, "触发加载更多: lastIndex=$it")
                onLoadMore()
            }
    }
}