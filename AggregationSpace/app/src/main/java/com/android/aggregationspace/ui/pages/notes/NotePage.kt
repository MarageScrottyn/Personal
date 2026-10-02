package com.android.aggregationspace.ui.pages.notes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.aggregationspace.R
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.ui.components.EmptyState
import com.android.aggregationspace.ui.components.ErrorState
import com.android.aggregationspace.ui.components.InfiniteListLoader
import com.android.aggregationspace.ui.components.LoadingFullScreen
import com.android.aggregationspace.ui.components.LoadingMoreFooter
import com.android.aggregationspace.ui.components.PaginationState
import com.android.aggregationspace.ui.components.parsePaginatedResponse
import com.android.aggregationspace.ui.components.SkeletonLoader
import com.android.aggregationspace.ui.global.PageCache
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "NotePage"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

data class NoteItem(
    val id: Int,
    val title: String,
    val content: String,
    val date: String
)

private fun parseNoteListFromApi(json: String): List<NoteItem> {
    val list = mutableListOf<NoteItem>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val item = NoteItem(
                id = obj.optInt("id", i),
                title = obj.optString("title", "未命名笔记"),
                content = obj.optString("content", ""),
                date = obj.optString("updated_at", "").replace("T", " ").take(16)
            )
            list.add(item)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析笔记数据失败: ${e.message}")
    }
    return list
}

@Composable
fun NotePage(navController: NavController, createTrigger: Int = 0) {
    // 使用全局缓存，避免返回首页时重新加载（隐私模式切换会自动清空）
    val paginationState = PageCache.pagination<NoteItem>("note", PAGE_SIZE)
    val listState = PageCache.listState("note")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 笔记创建对话框状态
    var showCreateDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // 监听顶部栏创建笔记触发器
    LaunchedEffect(createTrigger) {
        if (createTrigger > 0) {
            showCreateDialog = true
            noteTitle = ""
            noteContent = ""
        }
    }

    /** 加载第一页数据（仅在缓存为空时加载） */
    LaunchedEffect(Unit) {
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        paginationState.updateLoading(true)
        try {
            val result = ApiService.get(
                endpoint = ApiEndpoints.Note.LIST,
                queryParams = mapOf("limit" to PAGE_SIZE.toString(), "offset" to "0"),
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseNoteListFromApi(dataJson)
                    }
                    list
                }
            )
            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
                Log.d(TAG, "笔记数据加载成功: ${result.data!!.size} 条")
            } else {
                paginationState.setError(result.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取笔记数据失败: ${e.message}")
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
                        endpoint = ApiEndpoints.Note.LIST,
                        queryParams = mapOf(
                            "limit" to PAGE_SIZE.toString(),
                            "offset" to paginationState.getNextOffset().toString()
                        ),
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseNoteListFromApi(dataJson)
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
                    Log.e(TAG, "加载更多笔记失败: ${e.message}")
                    paginationState.setError("加载更多失败")
                } finally {
                    paginationState.updateLoadingMore(false)
                }
            }
        }
    }

    /** 监听滚动加载 */
    InfiniteListLoader(
        listState = listState,
        paginationState = paginationState,
        buffer = 3,
        onLoadMore = loadMore
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp)
    ) {
        when {
            paginationState.isLoading && paginationState.items.isEmpty() -> {
                LoadingFullScreen(message = "加载笔记中...")
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
                EmptyState(message = "暂无笔记")
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(paginationState.items, key = { it.id }) { note ->
                        NoteCard(note)
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

        FloatingActionButton(
            onClick = {
                showCreateDialog = true
                noteTitle = ""
                noteContent = ""
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF007AFF),
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "新建笔记")
        }
    }

    // 新建笔记对话框
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建笔记", color = Color.Black) },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            scope.launch {
                                isCreating = true
                                val result = ApiService.post(
                                    endpoint = ApiEndpoints.Note.CREATE,
                                    body = mapOf(
                                        "title" to noteTitle,
                                        "content" to noteContent
                                    ),
                                    responseParser = { it }
                                )
                                isCreating = false
                                if (result.success) {
                                    // 刷新列表
                                    paginationState.reset()
                                    paginationState.updateLoading(true)
                                    val refreshResult = ApiService.get(
                                        endpoint = ApiEndpoints.Note.LIST,
                                        queryParams = mapOf("limit" to PAGE_SIZE.toString(), "offset" to "0"),
                                        responseParser = { json ->
                                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                                parseNoteListFromApi(dataJson)
                                            }
                                            list
                                        }
                                    )
                                    if (refreshResult.success && refreshResult.data != null) {
                                        paginationState.appendData(refreshResult.data!!)
                                    }
                                    paginationState.updateLoading(false)
                                }
                                showCreateDialog = false
                            }
                        }
                    }
                ) { Text("确定", color = Color(0xFF007AFF)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    // 创建中提示
    if (isCreating) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun NoteCard(note: NoteItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF007AFF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_edit_note_24),
                contentDescription = note.title,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = note.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = note.content,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .heightIn(min = 0.dp)
            )
            Text(
                text = note.date,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}