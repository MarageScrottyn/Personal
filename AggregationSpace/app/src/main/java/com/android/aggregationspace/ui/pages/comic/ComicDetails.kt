package com.android.aggregationspace.ui.pages.comic

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.api.AuthImageLoader
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import com.android.aggregationspace.ui.components.Tag
import com.android.aggregationspace.ui.style.liquidGlass
import org.json.JSONObject

private const val TAG = "ComicDetails"

/** 漫画详情数据类 */
data class ComicDetailData(
    val id: Int,
    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String,
    val labels: List<String>,
    val chapterCount: Int,
    val updatedAt: String,
    val chapterImages: List<String>  // 第一章的图片列表（用于阅读）
)

/** 从后端 JSON 解析漫画详情 */
private fun parseComicDetail(json: String): ComicDetailData? {
    return try {
        val obj = JSONObject(json)
        // 解析标签列表
        val labels = mutableListOf<String>()
        try {
            val labelArray = obj.getJSONArray("category_names")
            for (i in 0 until labelArray.length()) {
                labels.add(labelArray.getString(i))
            }
        } catch (e: Exception) {
            Log.w(TAG, "无标签数据")
        }

        // 解析章节图片（取第一章的所有图片）
        val chapterImages = mutableListOf<String>()
        try {
            val chapters = obj.getJSONArray("chapters")
            if (chapters.length() > 0) {
                val firstChapter = chapters.getJSONObject(0)
                val images = firstChapter.getJSONArray("images")
                for (i in 0 until images.length()) {
                    val imgPath = images.getString(i)
                    // 后端返回的是相对路径（/media/...），需要解析为完整 URL
                    chapterImages.add(ApiService.resolveMediaUrl(imgPath))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "无章节图片数据")
        }

        // 格式化更新时间
        val updatedAt = obj.optString("updated_at", "").replace("T", " ").take(10)

        ComicDetailData(
            id = obj.optInt("id", 0),
            title = obj.optString("title", "未知漫画"),
            author = obj.optString("author", "未知作者"),
            description = obj.optString("description", "暂无简介"),
            coverUrl = obj.optString("cover_image", ""),
            labels = labels,
            chapterCount = try { obj.getJSONArray("chapters").length() } catch (e: Exception) { 0 },
            updatedAt = updatedAt,
            chapterImages = chapterImages
        )
    } catch (e: Exception) {
        Log.e(TAG, "解析漫画详情失败: ${e.message}")
        null
    }
}

@Composable
fun ComicDetails(navController: NavController, comicId: String) {
    // comicId 实际上是 slug（后端 ComicDetailView 使用 slug 作为查找字段）
    val slug = comicId
    var detail by remember { mutableStateOf<ComicDetailData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 从后端获取漫画详情
    LaunchedEffect(slug) {
        isLoading = true
        errorMessage = null
        try {
            val result = ApiService.get<String>(
                endpoint = ApiEndpoints.Comic.DETAIL + "$slug/",
                responseParser = { json -> json }
            )
            if (result.success && result.data != null) {
                detail = parseComicDetail(result.data!!)
                if (detail == null) {
                    errorMessage = "解析漫画详情失败"
                }
            } else {
                errorMessage = result.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取漫画详情失败: ${e.message}")
            errorMessage = "网络请求失败: ${e.message}"
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 透明背景，让 MainActivity 中的全局 AppBackground 可见
            .background(Color.Transparent)
            .padding(start = 10.dp, end = 10.dp, top = 56.dp)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF333333))
                }
            }
            errorMessage != null -> {
                ErrorState(
                    message = errorMessage!!,
                    onRetry = { detail = null; isLoading = true; /* 触发重组 */ }
                )
            }
            detail != null -> {
                val data = detail!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 漫画封面
                        AsyncImage(
                            model = data.coverUrl,
                            contentDescription = data.title,
                            contentScale = ContentScale.Crop,
                            imageLoader = AuthImageLoader.imageLoader,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "作者：${data.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "章节：${data.chapterCount}话",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Text(
                                text = "更新：${data.updatedAt}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "简介",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (data.labels.isNotEmpty()) {
                        Text(
                            text = "标签",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.labels.forEach { label ->
                                Tag(label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // 开始阅读按钮
                    if (data.chapterImages.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .liquidGlass()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        // 传递 slug 给阅读页
                                        navController.navigate("comicReader/$slug")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "开始阅读",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
            else -> {
                EmptyState(message = "未找到漫画数据")
            }
        }
    }
}
