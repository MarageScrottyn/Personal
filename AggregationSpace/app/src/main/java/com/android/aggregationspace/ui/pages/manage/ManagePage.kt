package com.android.aggregationspace.ui.pages.manage

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

// 标签数据类，对应后端 Category 模型
data class CategoryItem(
    val id: Int,
    val name: String,
    val slug: String,
    val permissionLevel: String = "regular",
    val parentId: Int? = null
)

// 管理页标签类型：图片、漫画、图集、视频、特辑、标签管理
private enum class AdminTab(val label: String) {
    IMAGE("图片"),
    COMIC("漫画"),
    ALBUM("图集"),
    VIDEO("视频"),
    SPECIAL("特辑"),
    TAG_MANAGE("标签管理")
}

// 标签对应的 UploadCategory 映射
private fun AdminTab.toUploadCategory(): UploadCategory = when (this) {
    AdminTab.IMAGE -> UploadCategory.IMAGE
    AdminTab.COMIC -> UploadCategory.COMIC
    AdminTab.ALBUM -> UploadCategory.ALBUM
    AdminTab.VIDEO -> UploadCategory.VIDEO
    AdminTab.SPECIAL -> UploadCategory.SPECIAL
    AdminTab.TAG_MANAGE -> UploadCategory.IMAGE  // 不会用到
}

@Composable
fun AdminPage(
    navController: NavController,
    addTrigger: Int = 0,
    onTabChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(AdminTab.IMAGE) }

    // 标签管理相关状态
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(false) }
    var categoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var categoryNameInput by remember { mutableStateOf("") }
    var categorySlugInput by remember { mutableStateOf("") }
    var categoryPermissionInput by remember { mutableStateOf("regular") }

    // 标签加载错误信息
    var categoryError by remember { mutableStateOf<String?>(null) }

    // 加载标签列表
    val loadCategories: () -> Unit = {
        scope.launch {
            isLoadingCategories = true
            categoryError = null
            android.util.Log.d("AdminPage", "开始加载标签列表: endpoint=${ApiEndpoints.Category.LIST}")
            val result = ApiService.get(
                endpoint = ApiEndpoints.Category.LIST,
                responseParser = { json ->
                    android.util.Log.d("AdminPage", "标签列表原始响应: $json")
                    parseCategoryList(json)
                }
            )
            isLoadingCategories = false
            if (result.success && result.data != null) {
                categories = result.data
                android.util.Log.d("AdminPage", "标签加载成功: ${categories.size} 个标签")
                if (categories.isEmpty()) {
                    categoryError = "暂无标签，请点击右上角添加按钮新增"
                }
            } else {
                categoryError = result.message ?: "加载失败"
                android.util.Log.e("AdminPage", "标签加载失败: ${result.message}")
                Toast.makeText(context, "加载标签失败: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 首次进入加载标签；切换到标签管理标签时也重新加载
    LaunchedEffect(Unit) { loadCategories() }
    LaunchedEffect(selectedTab) {
        // 通知外部当前选中的标签，用于控制顶部栏按钮显示
        onTabChanged(selectedTab.name)
        if (selectedTab == AdminTab.TAG_MANAGE) {
            loadCategories()
        }
    }

    // 监听顶部栏添加按钮触发：仅标签管理页响应（弹出新增标签对话框）
    // 其他分类的上传表单已直接内嵌在对应分页中显示，无需跳转
    LaunchedEffect(addTrigger) {
        if (addTrigger > 0 && selectedTab == AdminTab.TAG_MANAGE) {
            editingCategory = null
            categoryNameInput = ""
            categorySlugInput = ""
            categoryPermissionInput = "regular"
            categoryDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标签切换栏（水平滚动）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.12f))
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 标签内容区域
            if (selectedTab == AdminTab.TAG_MANAGE) {
                // 标签管理页
                if (isLoadingCategories) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                } else if (categoryError != null && categories.isEmpty()) {
                    // 错误或空状态提示
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = categoryError!!,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // 重试按钮
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF007AFF))
                                .clickable { loadCategories() }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("重试", color = Color.White, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryItemRow(
                                category = category,
                                onEdit = {
                                    editingCategory = category
                                    categoryNameInput = category.name
                                    categorySlugInput = category.slug
                                    categoryPermissionInput = category.permissionLevel
                                    categoryDialog = true
                                },
                                onDelete = {
                                    scope.launch {
                                        val result = ApiService.delete(
                                            endpoint = ApiEndpoints.Category.DELETE + category.slug + "/delete/"
                                        )
                                        if (result.success) {
                                            categories = categories.filter { it.id != category.id }
                                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "删除失败: ${result.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // 上传类型分页：直接内嵌对应分类的上传表单（跟随页面排列，无底部固定按钮）
                UploadPage(
                    navController = navController,
                    initialCategory = selectedTab.toUploadCategory(),
                    embedded = true
                )
            }
        }
    }

    // 标签编辑/新增对话框
    if (categoryDialog) {
        val isEdit = editingCategory != null
        AlertDialog(
            onDismissRequest = { categoryDialog = false },
            title = {
                Text(
                    text = if (isEdit) "修改标签" else "新增标签",
                    color = Color.Black
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { categoryNameInput = it },
                        label = { Text("标签名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = categorySlugInput,
                        onValueChange = { categorySlugInput = it },
                        label = { Text("标签标识（英文，用于URL）") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    // 权限级别选择
                    Text(
                        text = "权限级别",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 普通权限
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (categoryPermissionInput == "regular") Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.2f))
                                .clickable { categoryPermissionInput = "regular" }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "普通权限",
                                color = if (categoryPermissionInput == "regular") Color.White else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        // 特殊权限
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (categoryPermissionInput == "special") Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.2f))
                                .clickable { categoryPermissionInput = "special" }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "特殊权限",
                                color = if (categoryPermissionInput == "special") Color.White else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (categoryNameInput.isNotBlank()) {
                            // slug 为空时自动从名称生成
                            val slug = if (categorySlugInput.isNotBlank()) {
                                categorySlugInput
                            } else {
                                categoryNameInput.lowercase()
                                    .replace(Regex("[^a-z0-9]"), "-")
                                    .replace(Regex("-+"), "-")
                                    .trim('-')
                            }

                            scope.launch {
                                val body = mutableMapOf(
                                    "name" to categoryNameInput,
                                    "slug" to slug,
                                    "permission_level" to categoryPermissionInput
                                )
                                val result = if (isEdit) {
                                    val target = editingCategory!!
                                    ApiService.put(
                                        endpoint = ApiEndpoints.Category.UPDATE + target.slug + "/update/",
                                        body = body,
                                        responseParser = { it }
                                    )
                                } else {
                                    ApiService.post(
                                        endpoint = ApiEndpoints.Category.CREATE,
                                        body = body,
                                        responseParser = { it }
                                    )
                                }

                                if (result.success) {
                                    loadCategories()
                                    Toast.makeText(context, if (isEdit) "修改成功" else "新增成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "操作失败: ${result.message}", Toast.LENGTH_SHORT).show()
                                }
                                categoryDialog = false
                            }
                        }
                    }
                ) {
                    Text("确定", color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            }
        )
    }
}

/** 解析后端返回的标签列表 JSON */
private fun parseCategoryList(json: String): List<CategoryItem> {
    if (json.isBlank()) {
        android.util.Log.w("AdminPage", "标签列表响应为空")
        return emptyList()
    }
    return try {
        val gson = Gson()
        val jsonElement = gson.fromJson(json, com.google.gson.JsonElement::class.java)
        if (jsonElement == null || jsonElement.isJsonNull) {
            android.util.Log.w("AdminPage", "标签列表JSON为null")
            return emptyList()
        }

        val jsonArray: JsonArray = when {
            jsonElement.isJsonArray -> jsonElement.asJsonArray
            jsonElement.isJsonObject && jsonElement.asJsonObject.has("results") -> {
                jsonElement.asJsonObject.getAsJsonArray("results")
            }
            else -> {
                android.util.Log.w("AdminPage", "标签列表格式未知: $json")
                JsonArray()
            }
        }

        val items = mutableListOf<CategoryItem>()
        for (element in jsonArray) {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                // Gson 的 get() 返回 JsonNull 而非 Kotlin null，需要用扩展函数安全处理
                items.add(
                    CategoryItem(
                        id = obj.safeInt("id", 0),
                        name = obj.safeString("name", ""),
                        slug = obj.safeString("slug", ""),
                        permissionLevel = obj.safeString("permission_level", "regular"),
                        parentId = obj.safeIntOrNull("parent")
                    )
                )
            }
        }
        android.util.Log.d("AdminPage", "解析到 ${items.size} 个标签")
        items
    } catch (e: Exception) {
        android.util.Log.e("AdminPage", "解析标签列表异常: ${e.message}", e)
        emptyList()
    }
}

/** 标签项行组件（显示名称、标识、权限级别） */
@Composable
private fun CategoryItemRow(
    category: CategoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF007AFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name.first().toString(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = category.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = category.slug,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            // 权限级别标签
            Text(
                text = if (category.permissionLevel == "special") "特殊权限" else "普通权限",
                color = if (category.permissionLevel == "special") Color(0xFFFF9800) else Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 编辑按钮
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF).copy(alpha = 0.15f))
                .clickable { onEdit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "修改",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
        }

        // 删除按钮
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = 0.15f))
                .clickable { onDelete() }
                .padding(start = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = Color.Red,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Gson 的 get() 在字段值为 JSON null 时返回 JsonNull 对象（而非 Kotlin null），
// 直接调用 asInt/asString 会抛 UnsupportedOperationException，需要安全处理

/** 内容列表项数据类（通用，适用于图片/图集/漫画/视频/特辑） */
data class ContentItem(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val categories: List<String>,
    val description: String,
    val extra: String = ""  // 额外信息（如视频时长、漫画作者等）
)

/** 根据标签类型获取 API 端点和查询参数 */
private fun getApiConfig(tab: AdminTab): Pair<String, Map<String, String>> {
    return when (tab) {
        // 图片管理传 type=all：管理员需看到全部图片（含图集图片/封面）；后端会校验仅管理员生效
        AdminTab.IMAGE -> ApiEndpoints.Image.LIST to mapOf("type" to "all")
        AdminTab.ALBUM -> ApiEndpoints.Album.LIST to emptyMap()
        AdminTab.COMIC -> ApiEndpoints.Comic.LIST to emptyMap()
        AdminTab.VIDEO -> ApiEndpoints.Video.LIST to mapOf("video_type" to "video")
        AdminTab.SPECIAL -> ApiEndpoints.SpecialAlbum.LIST to mapOf("video_type" to "special")
        AdminTab.TAG_MANAGE -> "" to emptyMap()  // 不会用到
    }
}

/** 解析内容列表 JSON */
private fun parseContentList(json: String, tab: AdminTab): List<ContentItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val gson = Gson()
        val jsonElement = gson.fromJson(json, com.google.gson.JsonElement::class.java)
        if (jsonElement == null || jsonElement.isJsonNull) return emptyList()

        val jsonArray: JsonArray = when {
            jsonElement.isJsonArray -> jsonElement.asJsonArray
            jsonElement.isJsonObject && jsonElement.asJsonObject.has("results") ->
                jsonElement.asJsonObject.getAsJsonArray("results")
            else -> JsonArray()
        }

        val items = mutableListOf<ContentItem>()
        for (element in jsonArray) {
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject

            // 根据分类提取封面图 URL（字段名不同）
            val coverUrl = when (tab) {
                AdminTab.IMAGE -> obj.safeString("image_file", "")
                AdminTab.ALBUM -> obj.safeString("cover_image", "")
                AdminTab.COMIC -> obj.safeString("cover_image", "")
                AdminTab.VIDEO, AdminTab.SPECIAL -> obj.safeString("thumbnail", "")
                AdminTab.TAG_MANAGE -> ""
            }

            // 解析标签列表
            val categories = mutableListOf<String>()
            try {
                val catArray = obj.getAsJsonArray("category_names")
                if (catArray != null) {
                    for (i in 0 until catArray.size()) {
                        categories.add(catArray[i].asString)
                    }
                }
            } catch (e: Exception) { }

            // 根据分类提取额外信息
            val extra = when (tab) {
                AdminTab.VIDEO, AdminTab.SPECIAL -> {
                    val duration = obj.safeInt("duration", 0)
                    if (duration > 0) "${duration / 60}:${String.format("%02d", duration % 60)}" else ""
                }
                AdminTab.COMIC -> obj.safeString("author", "")
                AdminTab.ALBUM -> obj.safeString("author", "")
                else -> ""
            }

            items.add(ContentItem(
                id = obj.safeInt("id", 0),
                title = obj.safeString("title", "未知"),
                coverUrl = coverUrl,
                categories = categories,
                description = obj.safeString("description", ""),
                extra = extra
            ))
        }
        items
    } catch (e: Exception) {
        android.util.Log.e("AdminPage", "解析内容列表异常: ${e.message}", e)
        emptyList()
    }
}

/** 内容列表组件：根据标签类型从后端获取数据并显示 */
@Composable
private fun ContentListSection(tab: AdminTab, navController: NavController) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 加载内容列表
    LaunchedEffect(tab) {
        isLoading = true
        errorMessage = null
        val (endpoint, queryParams) = getApiConfig(tab)
        if (endpoint.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        scope.launch {
            val result = ApiService.get(
                endpoint = endpoint,
                queryParams = queryParams,
                responseParser = { json -> parseContentList(json, tab) }
            )
            isLoading = false
            if (result.success && result.data != null) {
                items = result.data
                if (items.isEmpty()) {
                    errorMessage = "暂无${tab.label}内容，点击右上角 + 添加"
                }
            } else {
                errorMessage = result.message ?: "加载失败"
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        }
        errorMessage != null && items.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(items) { item ->
                    ContentItemRow(item = item, tab = tab)
                }
            }
        }
    }
}

/** 内容项行组件 */
@Composable
private fun ContentItemRow(item: ContentItem, tab: AdminTab) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面图
        coil3.compose.AsyncImage(
            model = item.coverUrl,
            contentDescription = item.title,
            imageLoader = com.android.aggregationspace.api.AuthImageLoader.imageLoader,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            // 标签
            if (item.categories.isNotEmpty()) {
                Text(
                    text = item.categories.joinToString(" · "),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            // 额外信息（视频时长、作者等）
            if (item.extra.isNotBlank()) {
                val extraLabel = when (tab) {
                    AdminTab.VIDEO, AdminTab.SPECIAL -> "时长: "
                    AdminTab.COMIC, AdminTab.ALBUM -> "作者: "
                    else -> ""
                }
                Text(
                    text = "$extraLabel${item.extra}",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/** 安全读取字符串字段，null/缺失时返回默认值 */
private fun JsonObject.safeString(key: String, default: String = ""): String {
    val element = get(key) ?: return default
    return if (element.isJsonNull) default else element.asString
}

/** 安全读取整数字段，null/缺失时返回默认值 */
private fun JsonObject.safeInt(key: String, default: Int = 0): Int {
    val element = get(key) ?: return default
    return if (element.isJsonNull) default else element.asInt
}

/** 安全读取可为空的整数字段，null/缺失时返回 null */
private fun JsonObject.safeIntOrNull(key: String): Int? {
    val element = get(key) ?: return null
    return if (element.isJsonNull) null else element.asInt
}
