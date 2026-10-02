package com.android.aggregationspace.ui.pages.manage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.android.aggregationspace.api.AuthImageLoader
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// 上传分类枚举
enum class UploadCategory {
    IMAGE, COMIC, ALBUM, VIDEO, SPECIAL
}

// 上传文件数据类
data class UploadFile(
    val id: String,
    val name: String,
    val previewUrl: String,
    val isCover: Boolean = false,
    val order: Int = 0,
    val filePath: String = "",
    val isLocalFile: Boolean = false
)

// 标签数据类
data class TagItem(
    val id: Int,
    val name: String,
    val slug: String
)

// 漫画章节数据类（草稿，上传前）
data class ComicChapterDraft(
    val title: String = "",
    val chapterNumber: Int = 1,
    val images: List<UploadFile> = emptyList()
)

/**
 * 上传页面入口 - 根据分类显示对应的上传表单
 */
@Composable
fun UploadPage(navController: NavController, initialCategory: UploadCategory = UploadCategory.IMAGE, embedded: Boolean = false) {
    when (initialCategory) {
        UploadCategory.IMAGE -> ImageUploadForm(navController, embedded)
        UploadCategory.ALBUM -> AlbumUploadForm(navController, embedded)
        UploadCategory.COMIC -> ComicUploadForm(navController, embedded)
        UploadCategory.VIDEO -> VideoUploadForm(navController, embedded, "video")
        UploadCategory.SPECIAL -> VideoUploadForm(navController, embedded, "special")
    }
}

// ==================== 共享组件 ====================

/**
 * 加载标签列表的共享逻辑
 */
@Composable
private fun rememberTagList(): Pair<List<TagItem>, Boolean> {
    var availableTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var isLoadingTags by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoadingTags = true
        val result = withContext(Dispatchers.IO) {
            ApiService.get(
                endpoint = ApiEndpoints.Category.LIST,
                responseParser = { json ->
                    try {
                        val gson = Gson()
                        val jsonElement = gson.fromJson(json, com.google.gson.JsonElement::class.java)
                        val jsonArray: JsonArray = if (jsonElement.isJsonArray) {
                            jsonElement.asJsonArray
                        } else if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("results")) {
                            jsonElement.asJsonObject.getAsJsonArray("results")
                        } else {
                            JsonArray()
                        }
                        val items = mutableListOf<TagItem>()
                        for (element in jsonArray) {
                            if (element.isJsonObject) {
                                val obj = element.asJsonObject
                                items.add(
                                    TagItem(
                                        id = try { obj.get("id")?.asInt ?: 0 } catch (e: Exception) { 0 },
                                        name = try { obj.get("name")?.asString ?: "" } catch (e: Exception) { "" },
                                        slug = try { obj.get("slug")?.asString ?: "" } catch (e: Exception) { "" }
                                    )
                                )
                            }
                        }
                        items
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            )
        }
        isLoadingTags = false
        if (result.success && result.data != null) {
            availableTags = result.data
        }
    }

    return Pair(availableTags, isLoadingTags)
}

/**
 * 标签选择器对话框
 */
@Composable
private fun TagSelectorDialog(
    availableTags: List<TagItem>,
    isLoadingTags: Boolean,
    selectedTags: List<TagItem>,
    onToggleTag: (TagItem) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签", color = Color.Black) },
        text = {
            if (isLoadingTags) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            } else if (availableTags.isEmpty()) {
                Text("暂无可用标签，请先在标签管理中添加", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableTags) { tag ->
                        val isSelected = selectedTags.any { it.id == tag.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF007AFF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .clickable { onToggleTag(tag) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, "已选择", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(tag.name, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(tag.slug, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定", color = Color(0xFF007AFF)) } },
        dismissButton = { TextButton(onClick = { onClear(); onDismiss() }) { Text("清空", color = Color.Gray) } }
    )
}

/**
 * 上传单个图片到 /api/upload/image/，返回服务器路径
 */
private suspend fun uploadImageForPath(filePath: String, fileName: String, extraParams: Map<String, String> = emptyMap()): String? {
    val result = ApiService.uploadFile(
        endpoint = ApiEndpoints.Image.UPLOAD,
        filePath = filePath,
        fileName = fileName,
        extraParams = extraParams
    )
    if (result.success && result.data != null) {
        try {
            val gson = Gson()
            val json = gson.fromJson(result.data, com.google.gson.JsonElement::class.java)
            if (json.isJsonObject) {
                return json.asJsonObject.get("path")?.asString
            }
        } catch (_: Exception) { }
    }
    return null
}

/**
 * 上传单个视频到 /api/upload/video/，返回 Pair(视频路径, 缩略图路径)
 */
private suspend fun uploadVideoForPath(filePath: String, fileName: String, videoType: String = "video"): Pair<String, String?>? {
    val result = ApiService.uploadFile(
        endpoint = ApiEndpoints.Video.UPLOAD,
        filePath = filePath,
        fileName = fileName,
        extraParams = mapOf("type" to videoType)
    )
    if (result.success && result.data != null) {
        try {
            val gson = Gson()
            val json = gson.fromJson(result.data, com.google.gson.JsonElement::class.java)
            if (json.isJsonObject) {
                val path = json.asJsonObject.get("path")?.asString ?: return null
                val thumbnail = json.asJsonObject.get("thumbnail")?.asString
                return Pair(path, thumbnail)
            }
        } catch (_: Exception) { }
    }
    return null
}

/**
 * 通用文本输入框样式
 */
@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 3
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF007AFF),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedLabelColor = Color(0xFF007AFF),
            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

/**
 * 已选标签展示条
 */
@Composable
private fun SelectedTagsBar(selectedTags: List<TagItem>, onRemove: (TagItem) -> Unit) {
    if (selectedTags.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF007AFF))
                        .clickable { onRemove(tag) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tag.name, color = Color.White, fontSize = 12.sp)
                        Icon(Icons.Default.Close, "移除", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp).padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

/**
 * 上传按钮（嵌入模式 - 跟随内容排列 / 非嵌入模式 - 底部固定栏）
 */
@Composable
private fun BoxScope.UploadActionButton(
    isUploading: Boolean,
    uploadProgress: Int,
    buttonText: String,
    enabled: Boolean,
    embedded: Boolean,
    onCancel: () -> Unit,
    onUpload: () -> Unit
) {
    if (embedded) {
        // 嵌入模式：上传按钮放在列表末尾
        if (isUploading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = uploadProgress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF007AFF),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Text("上传中... $uploadProgress%", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) Color(0xFF007AFF) else Color.White.copy(alpha = 0.1f))
                    .clickable(enabled = enabled) { onUpload() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(buttonText, color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    } else {
        // 非嵌入模式：底部固定操作栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(16.dp)
        ) {
            if (isUploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = uploadProgress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF007AFF),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Text("上传中... $uploadProgress%", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onCancel() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (enabled) Color(0xFF007AFF) else Color.White.copy(alpha = 0.1f))
                            .clickable(enabled = enabled) { onUpload() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("上传", color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ==================== 图片上传表单 ====================

/**
 * 图片上传表单 - 批量上传图片 + 标签 + 标题前缀
 * API: POST /api/admin/images/batch-upload/ (multipart)
 */
@Composable
private fun ImageUploadForm(navController: NavController, embedded: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var titlePrefix by remember { mutableStateOf("图片") }
    var uploadedFiles by remember { mutableStateOf<List<UploadFile>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val (availableTags, isLoadingTags) = rememberTagList()
    var selectedTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var showTagSelector by remember { mutableStateOf(false) }

    // 多文件选择器
    val multiFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newFiles = mutableListOf<UploadFile>()
        val startId = (uploadedFiles.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
        for ((index, uri) in uris.withIndex()) {
            try {
                val fileName = getFileNameFromUri(context, uri)
                val tempFile = copyUriToTempFile(context, uri, "upload_${startId + index}_$fileName")
                if (tempFile != null) {
                    newFiles.add(UploadFile(
                        id = (startId + index).toString(),
                        name = fileName,
                        previewUrl = uri.toString(),
                        order = uploadedFiles.size + index,
                        filePath = tempFile.absolutePath,
                        isLocalFile = true
                    ))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        uploadedFiles = uploadedFiles + newFiles
    }

    Box(modifier = Modifier.fillMaxSize().then(if (embedded) Modifier else Modifier.statusBarsPadding().padding(top = 56.dp))) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { Text("上传图片", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp)) }

            // 标题前缀
            item { StyledTextField(titlePrefix, { titlePrefix = it }, "标题前缀（每张图片标题为：前缀+序号）", Modifier.fillMaxWidth(), singleLine = true) }

            // 标签选择
            item {
                Text("选择标签", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                SelectedTagsBar(selectedTags) { tag -> selectedTags = selectedTags.filter { it.id != tag.id } }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { showTagSelector = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLoadingTags) "加载标签中..." else "点击选择标签 (已选${selectedTags.size}个)", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }

            // 文件上传区域
            item {
                Text("选择图片（支持多选）", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF007AFF)).clickable { multiFileLauncher.launch("image/*") }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, "添加图片", tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("添加图片文件", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                }
            }

            // 已选文件列表
            itemsIndexed(uploadedFiles) { index, file ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        AsyncImage(model = file.previewUrl, contentDescription = file.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), imageLoader = AuthImageLoader.imageLoader)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("第${index + 1}张", color = Color.White, fontSize = 14.sp)
                        Text(file.name, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.15f)).clickable { uploadedFiles = uploadedFiles.filter { it.id != file.id } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, "删除", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 上传按钮
            item {
                UploadActionButton(
                    isUploading = isUploading, uploadProgress = uploadProgress,
                    buttonText = "批量上传图片", enabled = uploadedFiles.isNotEmpty(), embedded = embedded,
                    onCancel = { navController.popBackStack() },
                    onUpload = {
                        if (uploadedFiles.isNotEmpty()) { uploadError = null; isUploading = true; uploadProgress = 0 }
                    }
                )
            }
        }

        if (showTagSelector) {
            TagSelectorDialog(
                availableTags = availableTags, isLoadingTags = isLoadingTags,
                selectedTags = selectedTags,
                onToggleTag = { tag ->
                    selectedTags = if (selectedTags.any { it.id == tag.id }) selectedTags.filter { it.id != tag.id } else selectedTags + tag
                },
                onClear = { selectedTags = emptyList() },
                onDismiss = { showTagSelector = false }
            )
        }
    }

    // 上传逻辑：批量上传图片到 /api/admin/images/batch-upload/
    LaunchedEffect(isUploading) {
        if (isUploading && uploadError == null) {
            try {
                val params = mutableMapOf<String, String>()
                if (titlePrefix.isNotBlank()) params["title_prefix"] = titlePrefix
                if (selectedTags.isNotEmpty()) params["categories"] = selectedTags.joinToString(",") { it.id.toString() }

                val filesList = uploadedFiles.filter { it.isLocalFile && it.filePath.isNotEmpty() }.map { Triple("images", it.name, it.filePath) }
                if (filesList.isEmpty()) { uploadError = "没有可上传的文件"; isUploading = false; return@LaunchedEffect }

                uploadProgress = 50
                val result = withContext(Dispatchers.IO) {
                    ApiService.uploadFiles(endpoint = ApiEndpoints.Image.BATCH_UPLOAD, files = filesList, params = params)
                }

                if (result.success) {
                    uploadProgress = 100
                    delay(300)
                    if (embedded) {
                        // 嵌入模式：显示成功提示并重置表单，不返回上一页
                        Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                        uploadedFiles = emptyList()
                        selectedTags = emptyList()
                        titlePrefix = "图片"
                        isUploading = false
                        uploadProgress = 0
                    } else {
                        navController.popBackStack()
                    }
                } else {
                    uploadError = result.message; isUploading = false
                }
            } catch (e: Exception) {
                uploadError = "上传异常: ${e.message}"; isUploading = false
            }
        }
    }

    uploadError?.let { error ->
        AlertDialog(
            onDismissRequest = { uploadError = null },
            title = { Text("上传失败", color = Color.Black) },
            text = { Text(error, color = Color.Gray) },
            confirmButton = { TextButton(onClick = { uploadError = null }) { Text("确定", color = Color(0xFF007AFF)) } }
        )
    }
}

// ==================== 图集上传表单 ====================

/**
 * 图集上传表单 - 标题 + 描述 + 作者 + 多图 + 标签
 * API: POST /api/albums/create/ (multipart)
 */
@Composable
private fun AlbumUploadForm(navController: NavController, embedded: Boolean) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var uploadedFiles by remember { mutableStateOf<List<UploadFile>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val (availableTags, isLoadingTags) = rememberTagList()
    var selectedTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var showTagSelector by remember { mutableStateOf(false) }

    val multiFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newFiles = mutableListOf<UploadFile>()
        val startId = (uploadedFiles.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
        for ((index, uri) in uris.withIndex()) {
            try {
                val fileName = getFileNameFromUri(context, uri)
                val tempFile = copyUriToTempFile(context, uri, "upload_${startId + index}_$fileName")
                if (tempFile != null) {
                    newFiles.add(UploadFile(
                        id = (startId + index).toString(),
                        name = fileName,
                        previewUrl = uri.toString(),
                        isCover = uploadedFiles.isEmpty() && index == 0,
                        order = uploadedFiles.size + index,
                        filePath = tempFile.absolutePath,
                        isLocalFile = true
                    ))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        uploadedFiles = uploadedFiles + newFiles
    }

    Box(modifier = Modifier.fillMaxSize().then(if (embedded) Modifier else Modifier.statusBarsPadding().padding(top = 56.dp))) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { Text("上传图集", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp)) }

            item { StyledTextField(title, { title = it }, "标题", Modifier.fillMaxWidth(), singleLine = true) }
            item { StyledTextField(description, { description = it }, "描述", Modifier.fillMaxWidth().padding(top = 12.dp)) }
            item { StyledTextField(author, { author = it }, "作者", Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true) }

            // 标签选择
            item {
                Text("选择标签", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                SelectedTagsBar(selectedTags) { tag -> selectedTags = selectedTags.filter { it.id != tag.id } }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { showTagSelector = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLoadingTags) "加载标签中..." else "点击选择标签 (已选${selectedTags.size}个)", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }

            // 文件上传区域
            item {
                Text("选择图片（第一张为封面）", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF007AFF)).clickable { multiFileLauncher.launch("image/*") }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, "添加图片", tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("添加图片文件", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                }
            }

            // 已选文件列表（可设封面）
            itemsIndexed(uploadedFiles) { index, file ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        AsyncImage(model = file.previewUrl, contentDescription = file.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), imageLoader = AuthImageLoader.imageLoader)
                        if (file.isCover) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF007AFF)).align(Alignment.TopEnd).padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("封", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("第${index + 1}张" + (if (file.isCover) "（封面）" else ""), color = Color.White, fontSize = 14.sp)
                        Text(file.name, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    // 设为封面按钮
                    if (!file.isCover) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF007AFF).copy(alpha = 0.15f)).clickable {
                                uploadedFiles = uploadedFiles.map { it.copy(isCover = it.id == file.id) }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, "设为封面", tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                        }
                    }
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.15f)).clickable { uploadedFiles = uploadedFiles.filter { it.id != file.id } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, "删除", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 上传按钮
            item {
                UploadActionButton(
                    isUploading = isUploading, uploadProgress = uploadProgress,
                    buttonText = "上传图集", enabled = title.isNotBlank() && uploadedFiles.isNotEmpty(), embedded = embedded,
                    onCancel = { navController.popBackStack() },
                    onUpload = {
                        if (title.isNotBlank() && uploadedFiles.isNotEmpty()) { uploadError = null; isUploading = true; uploadProgress = 0 }
                    }
                )
            }
        }

        if (showTagSelector) {
            TagSelectorDialog(
                availableTags = availableTags, isLoadingTags = isLoadingTags,
                selectedTags = selectedTags,
                onToggleTag = { tag ->
                    selectedTags = if (selectedTags.any { it.id == tag.id }) selectedTags.filter { it.id != tag.id } else selectedTags + tag
                },
                onClear = { selectedTags = emptyList() },
                onDismiss = { showTagSelector = false }
            )
        }
    }

    // 上传逻辑：POST /api/albums/create/ (multipart)
    LaunchedEffect(isUploading) {
        if (isUploading && uploadError == null) {
            try {
                val params = mutableMapOf<String, String>()
                params["title"] = title
                if (description.isNotBlank()) params["description"] = description
                if (author.isNotBlank()) params["author"] = author
                if (selectedTags.isNotEmpty()) params["categories"] = selectedTags.joinToString(",") { it.id.toString() }

                val filesList = uploadedFiles.filter { it.isLocalFile && it.filePath.isNotEmpty() }.map { Triple("files", it.name, it.filePath) }
                if (filesList.isEmpty()) { uploadError = "没有可上传的文件"; isUploading = false; return@LaunchedEffect }

                uploadProgress = 50
                val result = withContext(Dispatchers.IO) {
                    ApiService.uploadFiles(endpoint = ApiEndpoints.Album.CREATE, files = filesList, params = params)
                }

                if (result.success) {
                    uploadProgress = 100
                    delay(300)
                    if (embedded) {
                        // 嵌入模式：显示成功提示并重置表单，不返回上一页
                        Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                        uploadedFiles = emptyList()
                        selectedTags = emptyList()
                        title = ""
                        description = ""
                        author = ""
                        isUploading = false
                        uploadProgress = 0
                    } else {
                        navController.popBackStack()
                    }
                } else {
                    uploadError = result.message; isUploading = false
                }
            } catch (e: Exception) {
                uploadError = "上传异常: ${e.message}"; isUploading = false
            }
        }
    }

    uploadError?.let { error ->
        AlertDialog(
            onDismissRequest = { uploadError = null },
            title = { Text("上传失败", color = Color.Black) },
            text = { Text(error, color = Color.Gray) },
            confirmButton = { TextButton(onClick = { uploadError = null }) { Text("确定", color = Color(0xFF007AFF)) } }
        )
    }
}

// ==================== 漫画上传表单 ====================

/**
 * 漫画上传表单 - 标题 + slug + 描述 + 作者 + 封面 + 章节 + 标签
 * 流程：先上传封面和章节图片，再 POST JSON 到 /api/admin/comics/create/
 */
@Composable
private fun ComicUploadForm(navController: NavController, embedded: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var coverFile by remember { mutableStateOf<UploadFile?>(null) }
    var chapters by remember { mutableStateOf<List<ComicChapterDraft>>(listOf(ComicChapterDraft())) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadStatus by remember { mutableStateOf("") }

    val (availableTags, isLoadingTags) = rememberTagList()
    var selectedTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var showTagSelector by remember { mutableStateOf(false) }

    // 封面选择器
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val fileName = getFileNameFromUri(context, it)
                val tempFile = copyUriToTempFile(context, it, "cover_$fileName")
                if (tempFile != null) {
                    coverFile = UploadFile(id = "cover", name = fileName, previewUrl = it.toString(), isCover = true, filePath = tempFile.absolutePath, isLocalFile = true)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 章节图片选择器（用 remember 保存当前选择的章节索引）
    var currentChapterIndex by remember { mutableStateOf(0) }
    val chapterImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (currentChapterIndex < chapters.size) {
            val newFiles = mutableListOf<UploadFile>()
            val existing = chapters[currentChapterIndex].images
            val startId = (existing.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1
            for ((index, uri) in uris.withIndex()) {
                try {
                    val fileName = getFileNameFromUri(context, uri)
                    val tempFile = copyUriToTempFile(context, uri, "comic_ch${currentChapterIndex}_${startId + index}_$fileName")
                    if (tempFile != null) {
                        newFiles.add(UploadFile(
                            id = "${currentChapterIndex}_${startId + index}",
                            name = fileName, previewUrl = uri.toString(),
                            order = existing.size + index, filePath = tempFile.absolutePath, isLocalFile = true
                        ))
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            chapters = chapters.toMutableList().also {
                it[currentChapterIndex] = it[currentChapterIndex].copy(images = it[currentChapterIndex].images + newFiles)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().then(if (embedded) Modifier else Modifier.statusBarsPadding().padding(top = 56.dp))) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { Text("上传漫画", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp)) }

            item { StyledTextField(title, { title = it }, "标题", Modifier.fillMaxWidth(), singleLine = true) }
            item { StyledTextField(slug, { slug = it }, "Slug（URL 标识，英文/数字）", Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true) }
            item { StyledTextField(description, { description = it }, "描述", Modifier.fillMaxWidth().padding(top = 12.dp)) }
            item { StyledTextField(author, { author = it }, "作者", Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true) }

            // 标签选择
            item {
                Text("选择标签", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                SelectedTagsBar(selectedTags) { tag -> selectedTags = selectedTags.filter { it.id != tag.id } }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { showTagSelector = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLoadingTags) "加载标签中..." else "点击选择标签 (已选${selectedTags.size}个)", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }

            // 封面上传
            item {
                Text("封面图片", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { coverLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverFile == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, "上传封面", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Text("点击上传封面", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    } else {
                        AsyncImage(model = coverFile!!.previewUrl, contentDescription = "封面", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), imageLoader = AuthImageLoader.imageLoader)
                    }
                }
            }

            // 章节列表
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("章节列表（${chapters.size} 章）", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF007AFF)).clickable {
                            chapters = chapters + ComicChapterDraft(title = "第${chapters.size + 1}章", chapterNumber = chapters.size + 1)
                        }.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("添加章节", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // 各章节
            itemsIndexed(chapters) { chapterIndex, chapter ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("第 ${chapter.chapterNumber} 章", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (chapters.size > 1) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.15f)).clickable {
                                chapters = chapters.filterIndexed { i, _ -> i != chapterIndex }
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, "删除章节", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = chapter.title, onValueChange = { newTitle ->
                            chapters = chapters.toMutableList().also { it[chapterIndex] = it[chapterIndex].copy(title = newTitle) }
                        },
                        label = { Text("章节标题") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF007AFF), unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    // 章节图片
                    Text("图片数：${chapter.images.size}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF007AFF).copy(alpha = 0.3f)).clickable {
                            currentChapterIndex = chapterIndex
                            chapterImageLauncher.launch("image/*")
                        }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, "添加图片", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("添加章节图片", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                    // 章节图片预览（横向滚动展示数量）
                    if (chapter.images.isNotEmpty()) {
                        Text("已选 ${chapter.images.size} 张图片", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // 上传按钮
            item {
                if (isUploading && uploadStatus.isNotBlank()) {
                    Text(uploadStatus, color = Color(0xFF007AFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                UploadActionButton(
                    isUploading = isUploading, uploadProgress = uploadProgress,
                    buttonText = "上传漫画", enabled = title.isNotBlank() && slug.isNotBlank(), embedded = embedded,
                    onCancel = { navController.popBackStack() },
                    onUpload = {
                        if (title.isNotBlank() && slug.isNotBlank()) { uploadError = null; isUploading = true; uploadProgress = 0 }
                    }
                )
            }
        }

        if (showTagSelector) {
            TagSelectorDialog(
                availableTags = availableTags, isLoadingTags = isLoadingTags,
                selectedTags = selectedTags,
                onToggleTag = { tag ->
                    selectedTags = if (selectedTags.any { it.id == tag.id }) selectedTags.filter { it.id != tag.id } else selectedTags + tag
                },
                onClear = { selectedTags = emptyList() },
                onDismiss = { showTagSelector = false }
            )
        }
    }

    // 上传逻辑：
    // 1. 上传封面图片 → 获取路径
    // 2. 逐章节上传图片 → 收集路径
    // 3. POST JSON 到 /api/admin/comics/create/
    LaunchedEffect(isUploading) {
        if (isUploading && uploadError == null) {
            try {
                uploadStatus = "正在上传封面..."
                uploadProgress = 5

                // 1. 上传封面
                var coverPath: String? = null
                if (coverFile != null && coverFile!!.isLocalFile) {
                    coverPath = withContext(Dispatchers.IO) {
                        uploadImageForPath(coverFile!!.filePath, coverFile!!.name)
                    }
                }
                uploadProgress = 15

                // 2. 上传各章节图片
                val chapterData = mutableListOf<Map<String, Any>>()
                val totalChapters = chapters.size
                for ((index, chapter) in chapters.withIndex()) {
                    uploadStatus = "正在上传第 ${chapter.chapterNumber} 章图片 (${index + 1}/$totalChapters)..."
                    val imagePaths = mutableListOf<String>()
                    for (imgFile in chapter.images) {
                        if (imgFile.isLocalFile && imgFile.filePath.isNotEmpty()) {
                            val path = withContext(Dispatchers.IO) {
                                uploadImageForPath(imgFile.filePath, imgFile.name, mapOf("comic_slug" to slug, "chapter_index" to chapter.chapterNumber.toString()))
                            }
                            if (path != null) imagePaths.add(path)
                        }
                    }
                    chapterData.add(mapOf(
                        "title" to chapter.title,
                        "chapter_number" to chapter.chapterNumber,
                        "images" to imagePaths
                    ))
                    uploadProgress = 15 + ((index + 1).toFloat() / totalChapters * 70).toInt()
                }

                // 3. 提交漫画 JSON 数据
                uploadStatus = "正在创建漫画记录..."
                uploadProgress = 90

                val body = mutableMapOf<String, Any>(
                    "title" to title,
                    "slug" to slug,
                    "description" to description,
                    "categories" to selectedTags.map { it.id }
                )
                if (author.isNotBlank()) body["author"] = author
                if (coverPath != null) body["cover_image"] = coverPath
                body["chapters"] = chapterData

                val result = withContext(Dispatchers.IO) {
                    ApiService.post(
                        endpoint = ApiEndpoints.Comic.CREATE,
                        body = body,
                        responseParser = { it }
                    )
                }

                if (result.success) {
                    uploadProgress = 100
                    uploadStatus = ""
                    delay(300)
                    if (embedded) {
                        // 嵌入模式：显示成功提示并重置表单，不返回上一页
                        Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                        title = ""
                        slug = ""
                        description = ""
                        author = ""
                        coverFile = null
                        chapters = listOf(ComicChapterDraft())
                        selectedTags = emptyList()
                        isUploading = false
                        uploadProgress = 0
                    } else {
                        navController.popBackStack()
                    }
                } else {
                    uploadError = result.message; isUploading = false; uploadStatus = ""
                }
            } catch (e: Exception) {
                uploadError = "上传异常: ${e.message}"; isUploading = false; uploadStatus = ""
            }
        }
    }

    uploadError?.let { error ->
        AlertDialog(
            onDismissRequest = { uploadError = null },
            title = { Text("上传失败", color = Color.Black) },
            text = { Text(error, color = Color.Gray) },
            confirmButton = { TextButton(onClick = { uploadError = null }) { Text("确定", color = Color(0xFF007AFF)) } }
        )
    }
}

// ==================== 视频/特辑上传表单 ====================

/**
 * 视频上传表单 - 标题 + slug + 描述 + 时长 + 标签 + 视频文件
 * 流程：先上传视频文件（返回路径+缩略图），再 POST JSON 到 /api/admin/videos/create/
 * @param videoType "video" 或 "special"
 */
@Composable
private fun VideoUploadForm(navController: NavController, embedded: Boolean, videoType: String) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("0") }
    var videoFile by remember { mutableStateOf<UploadFile?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadStatus by remember { mutableStateOf("") }

    val (availableTags, isLoadingTags) = rememberTagList()
    var selectedTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var showTagSelector by remember { mutableStateOf(false) }

    val isSpecial = videoType == "special"
    val formTitle = if (isSpecial) "上传特辑" else "上传视频"

    // 视频文件选择器
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val fileName = getFileNameFromUri(context, it)
                val tempFile = copyUriToTempFile(context, it, "video_$fileName")
                if (tempFile != null) {
                    videoFile = UploadFile(id = "video", name = fileName, previewUrl = it.toString(), filePath = tempFile.absolutePath, isLocalFile = true)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().then(if (embedded) Modifier else Modifier.statusBarsPadding().padding(top = 56.dp))) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { Text(formTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp)) }

            item { StyledTextField(title, { title = it }, "标题", Modifier.fillMaxWidth(), singleLine = true) }
            item { StyledTextField(slug, { slug = it }, "Slug（URL 标识，英文/数字）", Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true) }
            item { StyledTextField(description, { description = it }, "描述", Modifier.fillMaxWidth().padding(top = 12.dp)) }
            item { StyledTextField(duration, { duration = it.filter { c -> c.isDigit() } }, "时长（秒）", Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true) }

            // 标签选择
            item {
                Text("选择标签", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                SelectedTagsBar(selectedTags) { tag -> selectedTags = selectedTags.filter { it.id != tag.id } }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { showTagSelector = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLoadingTags) "加载标签中..." else "点击选择标签 (已选${selectedTags.size}个)", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }

            // 视频文件选择
            item {
                Text("选择视频文件", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).clickable { videoLauncher.launch("video/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (videoFile == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, "上传视频", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Text("点击选择视频文件", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                            Text("支持 MP4/AVI/MOV/MKV/WEBM", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("已选择视频", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(videoFile!!.name, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            Text("点击重新选择", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }

            // 上传按钮
            item {
                if (isUploading && uploadStatus.isNotBlank()) {
                    Text(uploadStatus, color = Color(0xFF007AFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                UploadActionButton(
                    isUploading = isUploading, uploadProgress = uploadProgress,
                    buttonText = formTitle, enabled = title.isNotBlank() && slug.isNotBlank() && videoFile != null, embedded = embedded,
                    onCancel = { navController.popBackStack() },
                    onUpload = {
                        if (title.isNotBlank() && slug.isNotBlank() && videoFile != null) { uploadError = null; isUploading = true; uploadProgress = 0 }
                    }
                )
            }
        }

        if (showTagSelector) {
            TagSelectorDialog(
                availableTags = availableTags, isLoadingTags = isLoadingTags,
                selectedTags = selectedTags,
                onToggleTag = { tag ->
                    selectedTags = if (selectedTags.any { it.id == tag.id }) selectedTags.filter { it.id != tag.id } else selectedTags + tag
                },
                onClear = { selectedTags = emptyList() },
                onDismiss = { showTagSelector = false }
            )
        }
    }

    // 上传逻辑：
    // 1. 上传视频文件到 /api/upload/video/ → 返回 path + thumbnail
    // 2. POST JSON 到 /api/admin/videos/create/ 创建视频记录
    LaunchedEffect(isUploading) {
        if (isUploading && uploadError == null) {
            try {
                uploadStatus = "正在上传视频文件..."
                uploadProgress = 10

                // 1. 上传视频文件
                val videoResult = withContext(Dispatchers.IO) {
                    uploadVideoForPath(videoFile!!.filePath, videoFile!!.name, videoType)
                }

                if (videoResult == null) {
                    uploadError = "视频文件上传失败"; isUploading = false; uploadStatus = ""; return@LaunchedEffect
                }

                val (videoPath, thumbnailPath) = videoResult
                uploadProgress = 70
                uploadStatus = "正在创建视频记录..."

                // 2. 提交视频 JSON 数据
                val body = mutableMapOf<String, Any>(
                    "title" to title,
                    "slug" to slug,
                    "description" to description,
                    "video_file" to videoPath,
                    "categories" to selectedTags.map { it.id },
                    "duration" to (duration.toIntOrNull() ?: 0),
                    "video_type" to videoType,
                    "tags" to ""
                )
                if (thumbnailPath != null) body["thumbnail"] = thumbnailPath

                val result = withContext(Dispatchers.IO) {
                    ApiService.post(
                        endpoint = ApiEndpoints.Video.CREATE,
                        body = body,
                        responseParser = { it }
                    )
                }

                if (result.success) {
                    uploadProgress = 100
                    uploadStatus = ""
                    delay(300)
                    if (embedded) {
                        // 嵌入模式：显示成功提示并重置表单，不返回上一页
                        Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                        title = ""
                        slug = ""
                        description = ""
                        duration = "0"
                        videoFile = null
                        selectedTags = emptyList()
                        isUploading = false
                        uploadProgress = 0
                    } else {
                        navController.popBackStack()
                    }
                } else {
                    uploadError = result.message; isUploading = false; uploadStatus = ""
                }
            } catch (e: Exception) {
                uploadError = "上传异常: ${e.message}"; isUploading = false; uploadStatus = ""
            }
        }
    }

    uploadError?.let { error ->
        AlertDialog(
            onDismissRequest = { uploadError = null },
            title = { Text("上传失败", color = Color.Black) },
            text = { Text(error, color = Color.Gray) },
            confirmButton = { TextButton(onClick = { uploadError = null }) { Text("确定", color = Color(0xFF007AFF)) } }
        )
    }
}

// ==================== 工具函数 ====================

/** 从 URI 获取文件名 */
private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var fileName = "unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex >= 0) {
                fileName = it.getString(displayNameIndex)
            }
        }
    }
    if (fileName == "unknown") {
        fileName = uri.lastPathSegment ?: "unknown"
    }
    return fileName
}

/** 将 URI 内容复制到临时文件 */
private fun copyUriToTempFile(context: android.content.Context, uri: Uri, fileName: String): File? {
    return try {
        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
