package com.android.aggregationspace.ui.pages.cloud

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import org.json.JSONArray

private const val TAG = "CloudPage"

/** 分页大小，与后端保持一致 */
private const val PAGE_SIZE = 20

data class CloudFile(
    val id: Int,
    val name: String,
    val size: String,
    val modified: String,
    val isDirectory: Boolean,
    val filePath: String = ""
)

private fun parseCloudListFromApi(json: String): List<CloudFile> {
    val list = mutableListOf<CloudFile>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val type = obj.optString("file_type", "file")
            val item = CloudFile(
                id = obj.optInt("id", 0),
                name = obj.optString("name", "未命名"),
                size = obj.optString("file_size", "—"),
                modified = obj.optString("updated_at", "").replace("T", " ").take(16),
                isDirectory = type == "folder",
                filePath = obj.optString("file_path", "")
            )
            list.add(item)
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析云盘数据失败: ${e.message}")
    }
    return list
}

@Composable
fun CloudPage(navController: NavController, parentId: String = "", uploadTrigger: Int = 0) {
    // 根据当前文件夹 parentId 生成独立缓存 key（根目录用 "cloud_root"）
    // 这样不同文件夹的数据互不干扰，且切换文件夹后能保留之前浏览位置
    val cacheKey = if (parentId.isEmpty()) "cloud_root" else "cloud_$parentId"
    val paginationState = PageCache.pagination<CloudFile>(cacheKey, PAGE_SIZE)
    val listState = PageCache.listState(cacheKey)
    val scope = rememberCoroutineScope()
    // 用于在协程中访问应用上下文
    val context = LocalContext.current

    // 上传/新建文件夹相关 UI 状态
    var showUploadDialog by remember { mutableStateOf(false) }   // 是否显示上传操作选择对话框
    var showFolderDialog by remember { mutableStateOf(false) }   // 是否显示新建文件夹对话框
    var folderName by remember { mutableStateOf("") }            // 新建文件夹名称输入
    var isUploading by remember { mutableStateOf(false) }        // 是否正在上传文件

    /** 监听上传触发器：uploadTrigger > 0 时弹出上传操作对话框 */
    LaunchedEffect(uploadTrigger) {
        if (uploadTrigger > 0) {
            showUploadDialog = true
        }
    }

    /** 文件选择器：选择文件后复制到临时文件并上传到云盘 */
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                // 将 Uri 复制到临时文件，然后上传
                val tempFile = copyUriToTempFile(context, uri)
                if (tempFile != null) {
                    val result = ApiService.uploadFile(
                        endpoint = ApiEndpoints.CloudDrive.LIST,  // POST /api/cloud/ 创建文件记录
                        filePath = tempFile.absolutePath,
                        fileName = tempFile.name,
                        fileType = "application/octet-stream",
                        extraParams = mapOf(
                            "name" to tempFile.name,
                            "file_type" to "file",
                            "parent" to parentId
                        )
                    )
                    if (result.success) {
                        // 刷新列表 - 重置 paginationState
                        paginationState.reset()
                    }
                }
                isUploading = false
                showUploadDialog = false
            }
        }
    }

    /** 加载第一页数据，parentId 变化时重新加载 */
    LaunchedEffect(parentId) {
        // 已有缓存数据则不重新加载（切换文件夹时除外）
        if (paginationState.items.isNotEmpty()) return@LaunchedEffect
        // 先重置分页状态（清空旧数据），再开启加载状态
        paginationState.reset()
        paginationState.updateLoading(true)
        try {
            // 根据是否有 parentId 构造查询参数
            val queryParams = mutableMapOf(
                "limit" to PAGE_SIZE.toString(),
                "offset" to "0"
            )
            if (parentId.isNotEmpty()) {
                queryParams["parent"] = parentId
            }
            val result = ApiService.get(
                endpoint = ApiEndpoints.CloudDrive.LIST,
                queryParams = queryParams,
                responseParser = { json ->
                    val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                        parseCloudListFromApi(dataJson)
                    }
                    list
                }
            )
            if (result.success && result.data != null) {
                paginationState.appendData(result.data!!)
                Log.d(TAG, "云盘数据加载成功: ${result.data!!.size} 条, parentId=$parentId")
            } else {
                paginationState.setError(result.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取云盘数据失败: ${e.message}")
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
                    val queryParams = mutableMapOf(
                        "limit" to PAGE_SIZE.toString(),
                        "offset" to paginationState.getNextOffset().toString()
                    )
                    if (parentId.isNotEmpty()) {
                        queryParams["parent"] = parentId
                    }
                    val result = ApiService.get(
                        endpoint = ApiEndpoints.CloudDrive.LIST,
                        queryParams = queryParams,
                        responseParser = { json ->
                            val (list, _, _) = parsePaginatedResponse(json) { dataJson ->
                                parseCloudListFromApi(dataJson)
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
                    Log.e(TAG, "加载更多云盘数据失败: ${e.message}")
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

    /** 点击文件夹时进入子页：携带 parentId 导航到新的云盘页 */
    val onFolderClick: (CloudFile) -> Unit = { file ->
        if (file.isDirectory) {
            navController.navigate("cloudDrive?parentId=${file.id}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp)
    ) {
        // 上传中遮罩提示，覆盖在列表之上
        if (isUploading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        when {
            paginationState.isLoading && paginationState.items.isEmpty() -> {
                LoadingFullScreen(message = "加载文件中...")
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
                EmptyState(message = "暂无文件")
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(paginationState.items) { file ->
                        CloudFileItem(file, onFolderClick)
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

    // 上传操作选择对话框：上传文件 / 新建文件夹
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("云盘操作", color = Color.Black) },
            text = {
                Column {
                    // 上传文件选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch("*/*") }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_upload_24), contentDescription = "上传文件", tint = Color(0xFF007AFF))
                        Text("上传文件", color = Color.Black, modifier = Modifier.padding(start = 12.dp))
                    }
                    // 新建文件夹选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showUploadDialog = false
                                showFolderDialog = true
                                folderName = ""
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.baseline_folder_24), contentDescription = "新建文件夹", tint = Color(0xFF007AFF))
                        Text("新建文件夹", color = Color.Black, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }

    // 新建文件夹对话框
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("新建文件夹", color = Color.Black) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (folderName.isNotBlank()) {
                        scope.launch {
                            val body = mutableMapOf("name" to folderName, "file_type" to "folder")
                            if (parentId.isNotEmpty()) body["parent"] = parentId
                            val result = ApiService.post(
                                endpoint = ApiEndpoints.CloudDrive.CREATE_FOLDER,
                                body = body,
                                responseParser = { it }
                            )
                            if (result.success) {
                                paginationState.reset()
                            }
                            showFolderDialog = false
                        }
                    }
                }) { Text("确定", color = Color(0xFF007AFF)) }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("取消", color = Color.Gray) }
            }
        )
    }
}

@Composable
private fun CloudFileItem(file: CloudFile, onFolderClick: (CloudFile) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable {
                // 文件夹点击进入子页，文件暂不处理
                if (file.isDirectory) {
                    onFolderClick(file)
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (file.isDirectory) Color(0xFF2196F3).copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            val folderIcon = if (file.isDirectory) {
                painterResource(R.drawable.baseline_folder_24)
            } else {
                painterResource(R.drawable.baseline_description_24)
            }
            Icon(
                painter = folderIcon,
                contentDescription = file.name,
                tint = if (file.isDirectory) Color(0xFF2196F3) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (file.isDirectory) file.modified else "${file.size} · ${file.modified}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "更多",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 将 Uri 复制到临时文件用于上传 */
private fun copyUriToTempFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val fileName = if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else "upload_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        }
    } catch (e: Exception) {
        Log.e("CloudPage", "复制文件失败: ${e.message}")
        null
    }
}
