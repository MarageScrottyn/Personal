package com.android.aggregationspace.ui.pages.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.aggregationspace.api.ApiConfig
import com.android.aggregationspace.ui.global.AppFontFamily
import com.android.aggregationspace.ui.global.AppFontSize
import com.android.aggregationspace.ui.global.SettingsStore
import com.android.aggregationspace.ui.global.ThemeMode

/** 设置项数据模型 */
data class SettingItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val hasSwitch: Boolean = false,
    val switchDefault: Boolean = false,
    val onClick: (() -> Unit)? = null
)

/** 设置分组数据模型 */
data class SettingSection(
    val title: String,
    val items: List<SettingItem>
)

/** 设置页主入口 */
@Composable
fun SettingPage(navController: NavController) {
    val context = LocalContext.current
    val appVersion = getAppVersion(context)

    // 观察响应式设置状态
    val themeMode by SettingsStore.themeMode.collectAsState()
    val language by SettingsStore.language.collectAsState()
    val fontFamily by SettingsStore.fontFamily.collectAsState()
    val fontSize by SettingsStore.fontSize.collectAsState()
    val bgColor by SettingsStore.bgColor.collectAsState()
    val bgImageUri by SettingsStore.bgImageUri.collectAsState()
    val privacyMode by SettingsStore.privacyMode.collectAsState()

    // 关于/API 配置相关本地状态
    var aboutClickCount by remember { mutableIntStateOf(0) }
    var showApiConfig by remember { mutableStateOf(ApiConfig.isApiConfigEnabled(context)) }
    var apiUrl by remember { mutableStateOf(ApiConfig.getBaseUrl(context)) }
    var apiConfigSwitch by remember { mutableStateOf(showApiConfig) }
    var showApiDialog by remember { mutableStateOf(false) }

    // 弹窗控制状态
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontFamilyDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showBgImageActionDialog by remember { mutableStateOf(false) }

    // 背景图片选择器（系统图库）
    // 使用 OpenDocument 而非 GetContent，因为只有 OpenDocument 才支持 takePersistableUriPermission
    // GetContent 返回的 URI 在应用重启后无法再访问，会导致背景图片设置失效
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // 持久化读取权限，避免重启后无法访问
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // 部分机型不支持持久化，忽略异常即可
            }
            SettingsStore.setBgImageUri(uri.toString())
        }
    }

    /** 关于项点击 5 次后开启 API 配置 */
    val handleAboutClick: () -> Unit = {
        aboutClickCount++
        if (aboutClickCount >= 5) {
            showApiConfig = true
            apiConfigSwitch = true
            ApiConfig.setApiConfigEnabled(context, true)
        }
    }

    val handleApiUrlChange: (String) -> Unit = {
        apiUrl = it
        ApiConfig.setBaseUrl(context, it)
    }

    val handleApiConfigSwitchChange: (Boolean) -> Unit = {
        apiConfigSwitch = it
        showApiConfig = it
        ApiConfig.setApiConfigEnabled(context, it)
        if (!it) {
            aboutClickCount = 0
        }
    }

    // 构建设置分组列表
    val sections = mutableListOf(
        SettingSection(
            title = "外观",
            items = listOf(
                SettingItem(
                    icon = Icons.Default.Favorite,
                    title = "深浅模式",
                    subtitle = themeMode.displayName,
                    onClick = { showThemeDialog = true }
                ),
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "语言模式",
                    subtitle = language
                ),
                SettingItem(
                    icon = Icons.Default.Edit,
                    title = "字体样式",
                    subtitle = fontFamily.displayName,
                    onClick = { showFontFamilyDialog = true }
                ),
                SettingItem(
                    icon = Icons.Default.Star,
                    title = "字体大小",
                    subtitle = fontSize.displayName,
                    onClick = { showFontSizeDialog = true }
                ),
                SettingItem(
                    icon = Icons.Default.Settings,
                    title = "背景颜色",
                    subtitle = bgColor,
                    onClick = { showColorPickerDialog = true }
                ),
                SettingItem(
                    icon = Icons.Default.Menu,
                    title = "背景图片",
                    subtitle = if (bgImageUri.isNullOrEmpty()) "默认纯色背景" else "已选择自定义图片",
                    onClick = { showBgImageActionDialog = true }
                )
            )
        ),
        SettingSection(
            title = "其他",
            items = listOf(
                SettingItem(
                    icon = Icons.Default.Person,
                    title = "隐私与安全",
                    subtitle = if (privacyMode) "已开启：权限已降级为普通" else "关闭：保持当前权限",
                    hasSwitch = true,
                    switchDefault = privacyMode,
                    onClick = { SettingsStore.setPrivacyMode(!privacyMode) }
                ),
                SettingItem(
                    icon = Icons.Default.MoreVert,
                    title = "关于",
                    subtitle = appVersion,
                    onClick = handleAboutClick
                )
            )
        )
    )

    if (showApiConfig) {
        sections.add(
            SettingSection(
                title = "API配置",
                items = listOf(
                    SettingItem(
                        Icons.Default.Settings,
                        "启用API配置",
                        "控制是否显示API地址输入项",
                        hasSwitch = true,
                        switchDefault = apiConfigSwitch,
                        onClick = { handleApiConfigSwitchChange(!apiConfigSwitch) }
                    ),
                    SettingItem(Icons.Default.Add, "API根地址", apiUrl)
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 56.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sections.forEach { section ->
                item {
                    SettingSectionView(section) { item ->
                        if (item.onClick != null) {
                            item.onClick()
                        }
                        if (section.title == "API配置" && item.title == "API根地址") {
                            showApiDialog = true
                        }
                    }
                }
            }
        }
    }

    // ===== 各类弹窗 =====
    if (showApiDialog) {
        ApiConfigInputDialog(
            currentUrl = apiUrl,
            onUrlChange = handleApiUrlChange,
            onDismiss = { showApiDialog = false }
        )
    }
    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "深浅模式",
            options = ThemeMode.values().map { it.displayName to it },
            currentOption = themeMode,
            onSelect = { SettingsStore.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
    if (showFontFamilyDialog) {
        SingleChoiceDialog(
            title = "字体样式",
            options = AppFontFamily.values().map { it.displayName to it },
            currentOption = fontFamily,
            onSelect = { SettingsStore.setFontFamily(it) },
            onDismiss = { showFontFamilyDialog = false }
        )
    }
    if (showFontSizeDialog) {
        SingleChoiceDialog(
            title = "字体大小",
            options = AppFontSize.values().map { it.displayName to it },
            currentOption = fontSize,
            onSelect = { SettingsStore.setFontSize(it) },
            onDismiss = { showFontSizeDialog = false }
        )
    }
    if (showColorPickerDialog) {
        ColorPickerDialog(
            currentColor = bgColor,
            onColorSelected = { SettingsStore.setBgColor(it) },
            onDismiss = { showColorPickerDialog = false }
        )
    }
    if (showBgImageActionDialog) {
        BgImageActionDialog(
            hasCustomImage = !bgImageUri.isNullOrEmpty(),
            onPickImage = {
                showBgImageActionDialog = false
                // OpenDocument 需要传入 mimeType 数组
                pickImageLauncher.launch(arrayOf("image/*"))
            },
            onClearImage = {
                showBgImageActionDialog = false
                SettingsStore.setBgImageUri(null)
            },
            onDismiss = { showBgImageActionDialog = false }
        )
    }
}

/**
 * 通用单选弹窗
 * @param T 选项数据类型，必须可比较
 */
@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<String, T>>,
    currentOption: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (value == currentOption) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** 颜色选择器弹窗，提供预设色板与自定义 #****** 输入 */
@Composable
private fun ColorPickerDialog(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 预设色板（已美化的渐变色系）
    val presetColors = remember {
        listOf(
            "#FF0A0E27", "#FF1A1A2E", "#FF16213E", "#FF0F3460",
            "#FF533483", "#FF8E2DE2", "#FF4A00E0", "#FF6D5DFC",
            "#FF00C9FF", "#FF92EFFD", "#FF00DBDE", "#FFFC00FF",
            "#FFFF6B6B", "#FFEE9CA7", "#FFFFD93D", "#FFFFF1EB",
            "#FF6BCB77", "#FF4D96FF", "#FFFFFFFF", "#FF000000"
        )
    }

    var customInput by remember { mutableStateOf(currentColor) }
    var parseError by remember { mutableStateOf(false) }

    /** 校验输入是否为合法 #RRGGBB 或 #AARRGGBB 格式 */
    fun isValidHex(input: String): Boolean {
        val normalized = input.removePrefix("#")
        return normalized.length == 6 || normalized.length == 8
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("背景颜色", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 预设色板：每行 5 个色块，避免使用 LazyVerticalGrid 引起的测量冲突
                presetColors.chunked(5).forEach { rowColors ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SettingsStore.parseColorSafe(hex))
                                    .clickable {
                                        onColorSelected(hex)
                                        onDismiss()
                                    }
                                    .then(
                                        if (hex.equals(currentColor, ignoreCase = true)) {
                                            Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
                                        } else Modifier
                                    )
                            )
                        }
                        // 行不足 5 个时填充空位以保持对齐
                        repeat(5 - rowColors.size) {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("自定义颜色", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // 自定义输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 实时预览色块
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isValidHex(customInput)) SettingsStore.parseColorSafe(customInput)
                                else Color.Gray
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = customInput,
                        onValueChange = {
                            // 自动转大写并加 # 前缀
                            var formatted = it.uppercase().removePrefix("#")
                            if (formatted.length > 8) formatted = formatted.take(8)
                            customInput = "#$formatted"
                            parseError = !isValidHex(customInput)
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }

                if (parseError) {
                    Text(
                        text = "格式应为 #RRGGBB 或 #AARRGGBB",
                        color = Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        if (isValidHex(customInput)) {
                            onColorSelected(customInput)
                            onDismiss()
                        }
                    },
                    enabled = isValidHex(customInput)
                ) {
                    Text("应用自定义颜色", color = if (isValidHex(customInput)) Color(0xFF007AFF) else Color.White.copy(alpha = 0.4f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** 背景图片操作弹窗：选择图片或清除自定义图片 */
@Composable
private fun BgImageActionDialog(
    hasCustomImage: Boolean,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("背景图片", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onPickImage()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从本地选择图片", color = Color.White, fontSize = 15.sp)
                }
                if (hasCustomImage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearImage() }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除并使用纯色背景", color = Color(0xFFFF6B6B), fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** API 配置输入弹窗 */
@Composable
private fun ApiConfigInputDialog(
    currentUrl: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置API根地址", color = Color.White) },
        text = {
            Column {
                Text("请输入服务器地址:", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                BasicTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Text("默认地址: https://marage.ccwu.cc", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUrlChange(inputUrl)
                onDismiss()
            }) {
                Text("确定", color = Color(0xFF007AFF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** 获取应用版本号 */
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "版本 ${packageInfo.versionName}"
    } catch (e: Exception) {
        "版本 1.0.0"
    }
}

/** 设置分组渲染 */
@Composable
private fun SettingSectionView(section: SettingSection, onItemClick: (SettingItem) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = section.title,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            section.items.forEachIndexed { index, item ->
                SettingRow(item, showDivider = index < section.items.size - 1, onClick = { onItemClick(item) })
            }
        }
    }
}

/** 单行设置项渲染 */
@Composable
private fun SettingRow(item: SettingItem, showDivider: Boolean = false, onClick: () -> Unit) {
    // 行内开关独立记忆状态，避免重组时丢失
    var switchState by remember(item.title) { mutableStateOf(item.switchDefault) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { onClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
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
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = item.subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (item.hasSwitch) {
                Switch(
                    checked = switchState,
                    onCheckedChange = {
                        switchState = it
                        item.onClick?.invoke()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF007AFF),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "进入",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        // 分隔线
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp, end = 16.dp)
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
        }
    }
}
