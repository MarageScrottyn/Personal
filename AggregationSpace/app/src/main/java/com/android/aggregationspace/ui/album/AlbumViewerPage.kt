package com.android.aggregationspace.ui.album

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AlbumViewerPage"

/** 从 JSON 字符串解析图片列表 */
private fun parseImagesFromJson(json: String): List<AlbumImageItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<AlbumImageItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                AlbumImageItem(
                    id = obj.optString("id", "$i"),
                    imageUrl = obj.optString("imageUrl", ""),
                    title = obj.optString("title", ""),
                    order = obj.optInt("order", i)
                )
            )
        }
        list
    } catch (e: Exception) {
        Log.e(TAG, "解析传入图片数据失败: ${e.message}")
        emptyList()
    }
}

/** 图集详情页：展示图集内所有图片，优先使用传入数据秒开 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumViewerPage(
    navController: NavController,
    albumId: String,
    initialTitle: String = "",
    initialImagesJson: String = ""
) {
    // 先用传入数据秒开
    var images by remember { mutableStateOf(parseImagesFromJson(initialImagesJson)) }
    var albumTitle by remember { mutableStateOf(initialTitle.ifEmpty { "加载中..." }) }
    var albumAuthor by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 后台静默刷新最新数据
    LaunchedEffect(albumId) {
        if (albumId.isBlank()) return@LaunchedEffect
        isRefreshing = true
        try {
            val result = withContext(Dispatchers.IO) {
                ApiService.get(
                    endpoint = ApiEndpoints.Album.DETAIL + albumId + "/",
                    responseParser = { json -> json }
                )
            }

            if (result.success) {
                val jsonObj = JSONObject(result.data ?: "{}")
                albumTitle = jsonObj.optString("title", albumTitle)
                albumAuthor = jsonObj.optString("author", "")

                // 解析图片列表
                val imagesArray = jsonObj.optJSONArray("images")
                val parsedImages = mutableListOf<AlbumImageItem>()

                if (imagesArray != null) {
                    for (i in 0 until imagesArray.length()) {
                        val item = imagesArray.getJSONObject(i)
                        parsedImages.add(
                            AlbumImageItem(
                                id = item.optString("id", "$i"),
                                imageUrl = item.optString("path", ""),
                                title = item.optString("title", ""),
                                order = item.optInt("order", i)
                            )
                        )
                    }
                }

                // 如果 images 为空，尝试用 image_urls
                if (parsedImages.isEmpty()) {
                    val urlsArray = jsonObj.optJSONArray("image_urls")
                    if (urlsArray != null) {
                        for (i in 0 until urlsArray.length()) {
                            parsedImages.add(
                                AlbumImageItem(
                                    id = "img_$i",
                                    imageUrl = urlsArray.optString(i, ""),
                                    order = i
                                )
                            )
                        }
                    }
                }

                // 添加封面图
                val coverUrl = jsonObj.optString("cover_image", "")
                if (coverUrl.isNotEmpty() && parsedImages.none { it.imageUrl == coverUrl }) {
                    parsedImages.add(0, AlbumImageItem(
                        id = "cover",
                        imageUrl = coverUrl,
                        title = "封面",
                        order = 0
                    ))
                }

                // 只有当 API 返回了图片才更新（覆盖传入数据）
                if (parsedImages.isNotEmpty()) {
                    images = parsedImages
                }
                Log.d(TAG, "刷新成功: ${images.size} 张图片")
            } else {
                errorMessage = result.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新异常: ${e.message}")
        }
        isRefreshing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = albumTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (albumAuthor.isNotEmpty()) {
                        Text(
                            text = "作者: $albumAuthor",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A1A)
            )
        )

        // 内容区域
        when {
            // 首次加载且没有传入数据时显示 loading
            images.isEmpty() && initialImagesJson.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            }

            errorMessage != null && images.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "加载失败",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            images.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无图片",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            else -> {
                // 图片瀑布流
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 72.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    state = rememberLazyListState()
                ) {
                    items(
                        items = images,
                        key = { it.id }
                    ) { imageItem ->
                        AlbumImageCard(
                            imageItem = imageItem,
                            onClick = {
                                val encodedUrl = Uri.encode(imageItem.imageUrl)
                                navController.navigate("fullscreenImage/$encodedUrl")
                            }
                        )
                    }
                }

                // 刷新中指示器（底部小字）
                if (isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "正在加载最新数据...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/** 单张图片卡片 */
@Composable
private fun AlbumImageCard(
    imageItem: AlbumImageItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFF1A1A1A))
    ) {
        AsyncImage(
            model = imageItem.imageUrl,
            contentDescription = imageItem.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(),
            imageLoader = AuthImageLoader.imageLoader
        )

        // 图片序号水印
//        Box(
//            modifier = Modifier
//                .padding(12.dp)
//                .background(Color.Black.copy(alpha = 0.5f))
//                .padding(horizontal = 8.dp, vertical = 4.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = "${imageItem.order + 1}",
//                color = Color.White,
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }
    }
}
