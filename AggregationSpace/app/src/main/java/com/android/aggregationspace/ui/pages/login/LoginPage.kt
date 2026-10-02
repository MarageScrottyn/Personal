package com.android.aggregationspace.ui.pages.login

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.android.aggregationspace.api.UserManager
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "LoginPage"

/** 登录/注册页面 */
@Composable
fun LoginPage(navController: NavController) {
    // 当前模式：登录 或 注册
    var isLoginMode by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    /** 执行登录 */
    fun doLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "请填写用户名和密码"
            return
        }
        isLoading = true
        errorMessage = null
        successMessage = null
        scope.launch {
            try {
                val result = ApiService.post(
                    endpoint = ApiEndpoints.Auth.LOGIN,
                    body = mapOf("username" to username, "password" to password),
                    responseParser = { json ->
                        Log.d(TAG, "登录响应: $json")
                        json
                    }
                )
                if (result.success) {
                    // 解析登录响应，提取 token 和用户信息
                    try {
                        val jsonObj = JSONObject(result.data ?: "")
                        val accessToken = jsonObj.optString("access", "")
                        val refreshToken = jsonObj.optString("refresh", "")
                        val userObj = jsonObj.optJSONObject("user")
                        if (accessToken.isNotEmpty()) {
                            // 保存 Access Token 和 Refresh Token（支持自动刷新）
                            ApiService.saveTokens(accessToken, refreshToken)
                            // 验证Token是否正确保存
                            val savedToken = ApiService.getAccessToken()
                            Log.d(TAG, "Token保存验证: saved=${savedToken != null}, 长度=${savedToken?.length}")
                            
                            // 保存用户信息
                            if (userObj != null) {
                                UserManager.saveUserInfoFromJson(userObj.toString())
                            }
                            Log.d(TAG, "登录成功")
                            navController.popBackStack()
                        } else {
                            errorMessage = "登录响应异常：未获取到Token"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析登录响应失败: ${e.message}", e)
                        errorMessage = "登录响应解析失败"
                    }
                } else {
                    errorMessage = result.message
                    Log.e(TAG, "登录失败: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "登录异常: ${e.message}", e)
                errorMessage = "登录请求失败: ${e.message}"
            }
            isLoading = false
        }
    }

    /** 执行注册 */
    fun doRegister() {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            errorMessage = "请填写完整信息"
            return
        }
        if (password != confirmPassword) {
            errorMessage = "两次输入的密码不一致"
            return
        }
        if (password.length < 6) {
            errorMessage = "密码至少需要 6 位"
            return
        }
        isLoading = true
        errorMessage = null
        successMessage = null
        scope.launch {
            try {
                val result = ApiService.post(
                    endpoint = ApiEndpoints.Auth.REGISTER,
                    body = mapOf("username" to username, "email" to email, "password" to password),
                    responseParser = { json ->
                        Log.d(TAG, "注册响应: $json")
                        json
                    }
                )
                if (result.success) {
                    successMessage = "注册成功，请登录"
                    // 切换到登录模式
                    isLoginMode = true
                } else {
                    errorMessage = result.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "注册异常: ${e.message}", e)
                errorMessage = "注册请求失败: ${e.message}"
            }
            isLoading = false
        }
    }

    // UI 部分
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo / 图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🚀",
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 标题
            Text(
                text = if (isLoginMode) "欢迎回来" else "创建账号",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isLoginMode) "请登录您的账号" else "请填写信息注册新账号",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 错误提示
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // 成功提示
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = Color(0xFF4ADE80),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // 用户名输入框
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                label = { Text("用户名", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF667eea),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    cursorColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 邮箱输入框（仅注册时显示）
            if (!isLoginMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("邮箱", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        cursorColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 密码输入框
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("密码", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF667eea),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    cursorColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 确认密码输入框（仅注册时显示）
            if (!isLoginMode) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("确认密码", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        cursorColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提交按钮
            Button(
                onClick = {
                    if (isLoginMode) doLogin() else doRegister()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "登录" else "注册",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 切换模式
            Row(
                modifier = Modifier.clickable {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                    successMessage = null
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoginMode) "还没有账号？" else "已有账号？",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = if (isLoginMode) "立即注册" else "立即登录",
                    color = Color(0xFF667eea),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
