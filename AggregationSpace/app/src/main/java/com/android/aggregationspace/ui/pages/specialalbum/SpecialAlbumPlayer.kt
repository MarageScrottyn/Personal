package com.android.aggregationspace.ui.pages.specialalbum

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.android.aggregationspace.ui.components.VideoPlayerSurface
import com.android.aggregationspace.ui.components.parsePaginatedResponse
import com.android.aggregationspace.ui.global.SpecialAlbumDataStore
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SpecialAlbumPlayer"

private fun jsonArrayToList(array: JSONArray): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until array.length()) {
        result.add(array.getString(i))
    }
    return result
}

private fun parseSpecialListFromApi(json: String): List<SpecialItem> {
    val list = mutableListOf<SpecialItem>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val item = SpecialItem(
                id = obj.getInt("id"),
                thumbnailUrl = obj.optString("thumbnail", ""),
                videoFile = obj.optString("video_file", ""),
                title = obj.optString("title", ""),
                duration = obj.optString("duration", "00:00:00"),
                description = obj.optString("description", ""),
                tags = try {
                    jsonArrayToList(obj.getJSONArray("category_names"))
                } catch (e: Exception) {
                    emptyList()
                }
            )
            list.add(item)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析推荐数据失败: ${e.message}")
    }
    return list
}

@Composable
fun SpecialAlbumPlayer(navController: NavController, videoId: Int) {
    val video = SpecialAlbumDataStore.currentVideo
    val isFullscreen = rememberSaveable { mutableStateOf(false) }
    
    // 推荐数据
    var recommendList by remember { mutableStateOf<List<SpecialItem>>(emptyList()) }
    var isLoadingRecommend by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 透明背景，让 MainActivity 中的全局 AppBackground 可见
            .background(Color.Transparent)
            .statusBarsPadding()
    ) {
        if (isFullscreen.value) {
            VideoPlayerSurface(
                videoUrl = video?.videoFile ?: "",
                onBack = { navController.popBackStack() },
                isFullscreen = true,
                onFullscreenChange = { isFullscreen.value = it }
            )
        } else {
            VideoPlayerSurface(
                videoUrl = video?.videoFile ?: "",
                onBack = { navController.popBackStack() },
                isFullscreen = false,
                onFullscreenChange = { isFullscreen.value = it }
            )

            Column(
                modifier = Modifier
                        .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = video?.title ?: "",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = video?.description ?: "",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "推荐特辑",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 加载推荐数据
                LaunchedEffect(Unit) {
                    isLoadingRecommend = true
                    try {
                        val result = ApiService.get(
                            endpoint = ApiEndpoints.SpecialAlbum.RECOMMEND,
                            queryParams = mapOf("type" to "special"),
                            responseParser = { json ->
                                val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                    parseSpecialListFromApi(dataJson)
                                }
                                list
                            }
                        )
                        if (result.success && result.data != null) {
                            recommendList = result.data!!
                            Log.d(TAG, "推荐数据加载成功: ${recommendList.size} 条")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "加载推荐数据失败: ${e.message}")
                    } finally {
                        isLoadingRecommend = false
                    }
                }

                if (isLoadingRecommend) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendList, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        SpecialAlbumDataStore.currentVideo = item
                                        navController.navigate("specialPlayer/${item.id}")
                                    }
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
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
                                        modifier = Modifier.size(32.dp)
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
                                            text = item.duration,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1
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