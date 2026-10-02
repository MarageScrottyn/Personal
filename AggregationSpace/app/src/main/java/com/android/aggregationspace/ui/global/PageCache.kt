package com.android.aggregationspace.ui.global

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import com.android.aggregationspace.ui.components.PaginationState

private const val TAG = "PageCache"

/**
 * 全局页面缓存单例
 *
 * 用于保存各分类页（图集、漫画、视频、图片、笔记、特辑、云盘）的分页数据和滚动位置，
 * 避免用户从详情页返回首页时重新加载整个列表。
 *
 * - 第一次进入某页：缓存不存在 → 创建状态并加载首页数据
 * - 返回首页再次进入：缓存已存在 → 直接复用，不重新加载
 * - 隐私模式切换 / 用户主动下拉刷新：调用 [clear] / [clearAll] 清空对应缓存
 */
object PageCache {

    /** 各页面的分页状态（按页面 key 存取） */
    private val paginationStates = mutableMapOf<String, PaginationState<*>>()

    /** 各页面的 LazyListState（用于 LazyColumn） */
    private val listStates = mutableMapOf<String, LazyListState>()

    /** 各页面的 LazyGridState（用于 LazyVerticalGrid） */
    private val gridStates = mutableMapOf<String, LazyGridState>()

    /** 各页面的 LazyStaggeredGridState（用于 LazyVerticalStaggeredGrid） */
    private val staggeredGridStates = mutableMapOf<String, LazyStaggeredGridState>()

    /**
     * 获取或创建某页面的分页状态
     *
     * @param key 页面唯一标识（如 "album"、"comic"）
     * @param pageSize 分页大小
     * @return 该页面对应的 PaginationState，类型 T 由调用方指定
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> pagination(key: String, pageSize: Int = 20): PaginationState<T> {
        return paginationStates.getOrPut(key) { PaginationState<T>(pageSize) } as PaginationState<T>
    }

    /** 获取或创建某页面的 LazyListState（LazyColumn） */
    fun listState(key: String): LazyListState {
        return listStates.getOrPut(key) { LazyListState() }
    }

    /** 获取或创建某页面的 LazyGridState（LazyVerticalGrid） */
    fun gridState(key: String): LazyGridState {
        return gridStates.getOrPut(key) { LazyGridState() }
    }

    /** 获取或创建某页面的 LazyStaggeredGridState（LazyVerticalStaggeredGrid） */
    fun staggeredGridState(key: String): LazyStaggeredGridState {
        return staggeredGridStates.getOrPut(key) { LazyStaggeredGridState() }
    }

    /**
     * 清空指定页面的缓存（分页 + 滚动位置）
     * 用于隐私模式切换、下拉刷新等场景
     */
    fun clear(key: String) {
        paginationStates.remove(key)
        listStates.remove(key)
        gridStates.remove(key)
        staggeredGridStates.remove(key)
        Log.d(TAG, "已清空页面缓存: $key")
    }

    /**
     * 清空所有页面缓存
     * 用于隐私模式切换，确保所有分类页都重新加载
     */
    fun clearAll() {
        paginationStates.clear()
        listStates.clear()
        gridStates.clear()
        staggeredGridStates.clear()
        Log.d(TAG, "已清空所有页面缓存")
    }

    /** 判断指定页面是否已有缓存数据（用于决定是否触发首次加载） */
    fun <T> hasData(key: String): Boolean {
        val state = paginationStates[key] as? PaginationState<T>
        return state != null && state.items.isNotEmpty()
    }
}
