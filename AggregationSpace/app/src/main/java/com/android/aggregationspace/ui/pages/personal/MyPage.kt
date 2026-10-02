package com.android.aggregationspace.ui.pages.personal

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.aggregationspace.api.ApiEndpoints
import com.android.aggregationspace.api.ApiService
import com.android.aggregationspace.api.UserInfo
import com.android.aggregationspace.api.UserManager
import com.android.aggregationspace.ui.layout.Screen
import kotlinx.coroutines.launch

private const val TAG = "MyPage"

/** 个人中心页面 */
@Composable
fun MyPage(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // 观察用户信息状态
    val userInfo by UserManager.userInfo.collectAsState(initial = UserManager.getUser())
    val isLoggedIn = userInfo != null

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    // 头像上传中标记（头像处显示加载圈，防止重复点击）
    var uploadingAvatar by remember { mutableStateOf(false) }

    // 已登录时自动加载最新用户资料
    LaunchedEffect(Unit) {
        if (isLoggedIn) {
            loadUserProfile { }
        }
    }

    /**
     * 处理选中的头像图片：
     * 用系统相册选择器返回的 Uri 复制到缓存，再以 multipart PATCH 上传
     */
    fun uploadAvatar(uri: android.net.Uri) {
        scope.launch {
            uploadingAvatar = true
            try {
                val cacheFile = java.io.File(context.cacheDir, "avatar_pick.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                } ?: run {
                    uploadingAvatar = false
                    return@launch
                }
                // 头像走 PATCH /api/users/me/，文件字段名 avatar
                val result = ApiService.uploadFile(
                    endpoint = ApiEndpoints.User.PROFILE,
                    filePath = cacheFile.absolutePath,
                    fileName = "avatar.jpg",
                    fileField = "avatar",
                    httpMethod = "PATCH"
                )
                if (result.success) {
                    // 重新拉取资料，头像 URL 带新版本号会自动刷新
                    loadUserProfile { }
                }
            } finally {
                uploadingAvatar = false
            }
        }
    }

    // 系统相册图片选择器（Photo Picker，无需申请存储权限）
    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) uploadAvatar(uri)
    }

    /** 唤起相册选择头像 */
    fun pickAvatar() {
        avatarPicker.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 55.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 用户信息区域
            item {
                if (isLoggedIn) {
                    LoggedInHeader(
                        userInfo = userInfo!!,
                        uploadingAvatar = uploadingAvatar,
                        onAvatarClick = { pickAvatar() },
                        onEditNicknameClick = { showNicknameDialog = true },
                        onAdminClick = {
                            navController.navigate(Screen.AdminPage.route)
                        }
                    )
                } else {
                    NotLoggedInHeader(
                        onLoginClick = {
                            navController.navigate(Screen.Login.route)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 菜单列表
            if (isLoggedIn) {
                item {
                    MenuList(
                        userInfo = userInfo!!,
                        onChangeAvatarClick = { pickAvatar() },
                        onEditNicknameClick = { showNicknameDialog = true },
                        onChangePasswordClick = { showChangePasswordDialog = true },
                        onAdminClick = { navController.navigate(Screen.AdminPage.route) },
                        onLogoutClick = { showLogoutConfirmDialog = true }
                    )
                }
            } else {
                item {
                    MenuList(
                        userInfo = null,
                        onChangeAvatarClick = {},
                        onEditNicknameClick = {},
                        onChangePasswordClick = {},
                        onAdminClick = { navController.navigate(Screen.AdminPage.route) },
                        onLogoutClick = {}
                    )
                }
            }
        }

        // 修改昵称弹窗
        if (showNicknameDialog) {
            EditNicknameDialog(
                initialNickname = userInfo?.nickname.orEmpty(),
                onDismiss = { showNicknameDialog = false },
                onSuccess = {
                    showNicknameDialog = false
                    scope.launch { loadUserProfile { } }
                }
            )
        }

        // 修改密码弹窗
        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showChangePasswordDialog = false },
                onSuccess = { showChangePasswordDialog = false }
            )
        }

        // 退出登录确认弹窗
        if (showLogoutConfirmDialog) {
            LogoutConfirmDialog(
                onDismiss = { showLogoutConfirmDialog = false },
                onConfirm = {
                    UserManager.logout()
                    showLogoutConfirmDialog = false
                }
            )
        }
    }
}

/** 已登录用户头部 */
@Composable
private fun LoggedInHeader(
    userInfo: UserInfo,
    uploadingAvatar: Boolean,
    onAvatarClick: () -> Unit,
    onEditNicknameClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像：有头像显示网络图片，无头像显示默认人像；右下角相机角标，点击更换
        Box(
            modifier = Modifier
                .size(88.dp)
                .clickable(enabled = !uploadingAvatar) { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (userInfo.avatar.isNotBlank()) {
                // 头像地址需经 resolveMediaUrl 拼接服务器域名，并走带鉴权的 ImageLoader
                coil3.compose.AsyncImage(
                    model = ApiService.resolveMediaUrl(userInfo.avatar),
                    contentDescription = "头像",
                    imageLoader = com.android.aggregationspace.api.AuthImageLoader.imageLoader,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "头像",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // 上传中：整体半透明遮罩 + 加载圈；空闲：右下角相机角标
            if (uploadingAvatar) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF667eea))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            com.android.aggregationspace.R.drawable.baseline_photo_camera_24
                        ),
                        contentDescription = "更换头像",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 昵称（首页展示昵称而非登录账号）+ 编辑图标
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = userInfo.displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑昵称",
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onEditNicknameClick() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 权限标签：独占一行，不再与昵称并排
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        userInfo.isAdmin -> Color(0xFFFF6B6B).copy(alpha = 0.85f)
                        userInfo.isVip -> Color(0xFFFFD93D).copy(alpha = 0.85f)
                        else -> Color.White.copy(alpha = 0.2f)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = when {
                    userInfo.isAdmin -> "管理员"
                    userInfo.isVip -> "VIP"
                    else -> "普通用户"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 登录账号（小字展示，区别于展示昵称）
        Text(
            text = "账号：${userInfo.username}",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 邮箱
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = userInfo.email.ifEmpty { "未绑定邮箱" },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // 管理员按钮
        if (userInfo.isAdmin) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFF667eea).copy(alpha = 0.8f))
                    .clickable { onAdminClick() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "进入管理后台",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** 未登录用户头部 */
@Composable
private fun NotLoggedInHeader(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "头像",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "欢迎使用",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "登录后享受更多服务",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667eea),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "登录 / 注册",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** 菜单列表 */
@Composable
private fun MenuList(
    userInfo: UserInfo?,
    onChangeAvatarClick: () -> Unit,
    onEditNicknameClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onAdminClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 菜单项
        if (userInfo != null) {
            // 设置头像
            MenuItem(
                icon = Icons.Default.AccountCircle,
                title = "设置头像",
                subtitle = "从相册选择一张照片作为头像",
                onClick = onChangeAvatarClick
            )

            // 编辑昵称
            MenuItem(
                icon = Icons.Default.Edit,
                title = "编辑昵称",
                subtitle = "设置个人中心展示的名称",
                onClick = onEditNicknameClick
            )

            // 账户设置
            MenuItem(
                icon = Icons.Default.Lock,
                title = "修改密码",
                subtitle = "定期更换密码更安全",
                onClick = onChangePasswordClick
            )

            // 管理后台（仅管理员）
            if (userInfo.isAdmin) {
                MenuItem(
                    icon = Icons.Default.Settings,
                    title = "内容管理",
                    subtitle = "管理图片、视频、漫画等资源",
                    onClick = onAdminClick
                )
            }

            // 退出登录
            MenuItem(
                icon = Icons.Default.ExitToApp,
                title = "退出登录",
                subtitle = null,
                onClick = onLogoutClick,
                isDestructive = true
            )
        } else {
            // 未登录时也能看到的功能
            MenuItem(
                icon = Icons.Default.Settings,
                title = "内容管理",
                subtitle = "需要管理员权限",
                onClick = onAdminClick
            )
        }
    }
}

/** 单个菜单项 */
@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFFF6B6B) else Color.White,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = if (isDestructive) Color(0xFFFF6B6B) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 修改昵称弹窗 */
@Composable
private fun EditNicknameDialog(
    initialNickname: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var nickname by remember { mutableStateOf(initialNickname) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "编辑昵称",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        // 界面上限制最多 30 字，与后端一致
                        if (it.length <= 30) nickname = it
                        errorMsg = null
                    },
                    label = { Text("昵称（留空则显示账号）", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = getTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    errorMsg = null
                    scope.launch {
                        try {
                            // PATCH 个人资料，仅提交昵称字段
                            val result = ApiService.patch(
                                endpoint = ApiEndpoints.User.PROFILE,
                                body = mapOf("nickname" to nickname.trim()),
                                responseParser = { it }
                            )
                            if (result.success) {
                                onSuccess()
                            } else {
                                errorMsg = result.message
                            }
                        } catch (e: Exception) {
                            errorMsg = "请求失败: ${e.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isLoading) onDismiss() }
            ) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** 修改密码弹窗 */
@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "修改密码",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMsg = null
                    },
                    label = { Text("当前密码", color = Color.White.copy(alpha = 0.7f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = getTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMsg = null
                    },
                    label = { Text("新密码（至少6位）", color = Color.White.copy(alpha = 0.7f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = getTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMsg = null
                    },
                    label = { Text("确认新密码", color = Color.White.copy(alpha = 0.7f)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = getTextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPassword.isBlank() || newPassword.isBlank()) {
                        errorMsg = "请填写所有密码字段"
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        errorMsg = "新密码至少需要 6 位"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMsg = "两次输入的新密码不一致"
                        return@Button
                    }
                    isLoading = true
                    errorMsg = null
                    scope.launch {
                        try {
                            val result = ApiService.post(
                                endpoint = ApiEndpoints.User.CHANGE_PASSWORD,
                                body = mapOf(
                                    "current_password" to currentPassword,
                                    "new_password" to newPassword
                                ),
                                responseParser = { it }
                            )
                            if (result.success) {
                                onSuccess()
                            } else {
                                errorMsg = result.message
                            }
                        } catch (e: Exception) {
                            errorMsg = "请求失败: ${e.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("确认修改")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isLoading) onDismiss() }
            ) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** 退出登录确认弹窗 */
@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "退出登录",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "确定要退出当前账号吗？",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
            ) {
                Text("退出登录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF2A2A3E)
    )
}

/** 加载用户资料 */
private suspend fun loadUserProfile(onError: (String?) -> Unit) {
    try {
        val result = ApiService.get(
            endpoint = ApiEndpoints.User.PROFILE,
            responseParser = { json ->
                Log.d(TAG, "用户资料: $json")
                json
            }
        )
        if (result.success) {
            UserManager.saveUserInfoFromJson(result.data ?: "")
        } else {
            onError(result.message)
        }
    } catch (e: Exception) {
        onError(e.message)
    }
}

/** 统一的输入框颜色 */
@Composable
private fun getTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF667eea),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
    cursorColor = Color.White
)
