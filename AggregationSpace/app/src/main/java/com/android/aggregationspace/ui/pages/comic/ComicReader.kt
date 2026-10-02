package com.android.aggregationspace.ui.pages.comic

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.api.AuthImageLoader
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ComicReader"

/** 漫画阅读页数据 */
data class ComicReaderPage(
    val pageNumber: Int,
    val imageUrl: String
)

/** 从后端漫画详情 JSON 中提取所有章节的图片（合并为连续阅读列表） */
private fun parseComicPages(json: String): List<ComicReaderPage> {
    val pages = mutableListOf<ComicReaderPage>()
    try {
        val obj = JSONObject(json)
        val chapters = obj.getJSONArray("chapters")
        var pageNum = 1
        // 遍历所有章节，合并图片为连续阅读列表
        for (i in 0 until chapters.length()) {
            val chapter = chapters.getJSONObject(i)
            val images: JSONArray = chapter.optJSONArray("images") ?: JSONArray()
            for (j in 0 until images.length()) {
                val imgPath = images.getString(j)
                // 后端返回的是相对路径（/media/...），需要解析为完整 URL
                pages.add(ComicReaderPage(
                    pageNumber = pageNum++,
                    imageUrl = ApiService.resolveMediaUrl(imgPath)
                ))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析漫画阅读页数据失败: ${e.message}")
    }
    return pages
}

@Composable
fun ComicReader(
    navController: NavController,
    comicId: String,
    onShowTopBarChange: (Boolean) -> Unit
) {
    // comicId 实际上是 slug（后端 ComicDetailView 使用 slug 作为查找字段）
    val slug = comicId
    var pages by remember { mutableStateOf<List<ComicReaderPage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    var showTopBar by remember { mutableStateOf(true) }

    // 从后端获取漫画详情，提取章节图片
    LaunchedEffect(slug) {
        isLoading = true
        errorMessage = null
        try {
            val result = ApiService.get<String>(
                endpoint = ApiEndpoints.Comic.DETAIL + "$slug/",
                responseParser = { json -> json }
            )
            if (result.success && result.data != null) {
                pages = parseComicPages(result.data!!)
                if (pages.isEmpty()) {
                    errorMessage = "该漫画暂无阅读内容"
                }
            } else {
                errorMessage = result.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取漫画阅读数据失败: ${e.message}")
            errorMessage = "网络请求失败: ${e.message}"
        }
        isLoading = false
    }

    // 自动隐藏顶栏
    LaunchedEffect(showTopBar, isLoading) {
        if (showTopBar && !isLoading && pages.isNotEmpty()) {
            delay(3000)
            showTopBar = false
            onShowTopBarChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showTopBar = !showTopBar
                        onShowTopBarChange(showTopBar)
                    }
                )
            }
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            errorMessage != null -> {
                ErrorState(
                    message = errorMessage!!,
                    onRetry = { pages = emptyList(); isLoading = true }
                )
            }
            pages.isEmpty() -> {
                EmptyState(message = "暂无阅读内容")
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(pages) { _, page ->
                        AsyncImage(
                            model = page.imageUrl,
                            contentDescription = "漫画第${page.pageNumber}页",
                            contentScale = ContentScale.FillWidth,
                            imageLoader = AuthImageLoader.imageLoader,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
