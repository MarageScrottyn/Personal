package com.android.aggregationspace.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock

data class ApiResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: T?,
    val error: Throwable? = null
)

private const val TAG = "ApiService"

object ApiService {
    private const val TIMEOUT = 30L
    private const val TOKEN_PREF_NAME = "AuthToken"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private lateinit var context: Context

    /** 同步锁，用于防止并发刷新Token */
    private val refreshLock = ReentrantLock()
    /** 最近一次刷新成功的时间戳（毫秒） */
    @Volatile
    private var lastRefreshTime = 0L
    /** 刷新间隔阈值（毫秒），在此时长内不重复刷新 */
    private const val REFRESH_INTERVAL_MS = 2000L

    fun init(context: Context) {
        this.context = context
    }

    /** 带认证的 OkHttpClient：使用拦截器自动附加 Token 和处理 401 刷新 */
    internal val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .build()
    }

    /** 独立的 OkHttpClient：用于 Token 刷新请求，避免被 AuthInterceptor 拦截 */
    private val refreshClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private fun getTokenPrefs(): SharedPreferences {
        return context.getSharedPreferences(TOKEN_PREF_NAME, Context.MODE_PRIVATE)
    }

    /** 获取 Access Token（带格式验证） */
    fun getAccessToken(): String? {
        val token = getTokenPrefs().getString(KEY_ACCESS_TOKEN, null)
        if (token == null) return null
        if (!isValidJwtFormat(token)) {
            Log.e(TAG, "Access Token格式无效，清除: ${token.take(20)}...")
            clearToken()
            return null
        }
        return token
    }

    /** 获取 Refresh Token */
    fun getRefreshToken(): String? {
        val token = getTokenPrefs().getString(KEY_REFRESH_TOKEN, null)
        if (token == null) return null
        if (!isValidJwtFormat(token)) {
            Log.e(TAG, "Refresh Token格式无效，清除")
            clearToken()
            return null
        }
        return token
    }

    /** 验证JWT令牌格式（三个部分用.分隔） */
    private fun isValidJwtFormat(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) return false
        return parts.all { it.isNotEmpty() }
    }

    /** 保存 Access Token */
    fun saveAccessToken(token: String) {
        if (!isValidJwtFormat(token)) {
            Log.e(TAG, "保存的Access Token格式无效: ${token.take(30)}...")
            return
        }
        getTokenPrefs().edit().putString(KEY_ACCESS_TOKEN, token).commit()
        Log.d(TAG, "Access Token已保存")
    }

    /** 保存 Refresh Token */
    fun saveRefreshToken(token: String) {
        if (!isValidJwtFormat(token)) {
            Log.e(TAG, "保存的Refresh Token格式无效: ${token.take(30)}...")
            return
        }
        getTokenPrefs().edit().putString(KEY_REFRESH_TOKEN, token).commit()
        Log.d(TAG, "Refresh Token已保存")
    }

    /** 保存登录时返回的所有 Token */
    fun saveTokens(accessToken: String, refreshToken: String) {
        saveAccessToken(accessToken)
        saveRefreshToken(refreshToken)
    }

    /** 清除所有 Token */
    fun clearToken() {
        getTokenPrefs().edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .commit()
        Log.d(TAG, "所有Token已清除")
    }

    private fun getBaseUrl(): String {
        return ApiConfig.getBaseUrl(context)
    }

    /** 将相对媒体路径解析为完整 URL（处理后端返回的 /media/... 相对路径） */
    fun resolveMediaUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = getBaseUrl()
        return if (path.startsWith("/")) "$base$path" else "$base/$path"
    }

    /**
     * OkHttp 认证拦截器：
     * 1. 仅对 API 请求（base URL 下的请求）自动添加 Authorization 头
     * 2. 收到 401 响应时自动刷新 Token 并重试
     */
    private class AuthInterceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val url = originalRequest.url.toString()
            val baseUrl = ApiConfig.getBaseUrl(ApiService.context)

            // 仅对 API 请求添加认证头
            val isApiRequest = url.startsWith(baseUrl)
            var request = originalRequest

            if (isApiRequest) {
                val token = ApiService.getAccessToken()
                val requestBuilder = originalRequest.newBuilder()
                if (token != null) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                // 隐私模式开启时，附加请求头让后端按普通用户处理，屏蔽特殊权限内容
                if (com.android.aggregationspace.ui.global.SettingsStore.privacyMode.value) {
                    requestBuilder.header("X-Privacy-Mode", "true")
                }
                request = requestBuilder.build()
            }

            // 发送请求
            var response = chain.proceed(request)

            // 如果是 API 请求且收到 401 响应，尝试刷新 Token 并重试
            if (isApiRequest && response.code == 401 && !isRefreshRequest(originalRequest)) {
                Log.w(TAG, "收到401响应，尝试刷新Token...")
                response.close()

                val newToken = ApiService.refreshAccessTokenSync()
                if (newToken != null) {
                    // 刷新成功：使用新 Token 重建请求并重试
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    response = chain.proceed(retryRequest)
                    Log.d(TAG, "Token刷新成功，请求已重试: code=${response.code}")
                } else {
                    // 刷新失败：清除所有登录状态，重新请求以返回 401 错误信息
                    // 不能直接返回已关闭的 response，否则 handleResponse 读取 body 时会抛 IllegalStateException
                    Log.e(TAG, "Token刷新失败，清除所有Token和用户信息")
                    ApiService.clearToken()
                    UserManager.clearUserInfo()
                    response = chain.proceed(request)
                }
            }

            return response
        }

        /** 判断是否是刷新 Token 的请求（避免无限递归） */
        private fun isRefreshRequest(request: Request): Boolean {
            return request.url.encodedPath.contains("/api/auth/refresh/")
        }
    }

    /**
     * 同步刷新 Access Token（在拦截器中调用，带线程安全保护）
     * 使用 ReentrantLock 确保同一时间只有一个线程在刷新 Token，
     * 其他线程等待刷新完成后直接使用新 Token，避免并发刷新导致 Refresh Token 失效
     */
    private fun refreshAccessTokenSync(): String? {
        val now = System.currentTimeMillis()

        // 双重检查：如果刚刷新过（2秒内），直接返回当前 Token
        val currentToken = getAccessToken()
        if (currentToken != null && now - lastRefreshTime < REFRESH_INTERVAL_MS) {
            Log.d(TAG, "最近已刷新过Token，直接返回当前Token")
            return currentToken
        }

        refreshLock.lock()
        try {
            // 再次检查（获取锁后其他线程可能已经刷新完成）
            val token = getAccessToken()
            if (token != null && now - lastRefreshTime < REFRESH_INTERVAL_MS) {
                Log.d(TAG, "其他线程已完成Token刷新，直接返回")
                return token
            }

            val refreshToken = getRefreshToken()
            if (refreshToken == null) {
                Log.e(TAG, "无Refresh Token，无法刷新")
                return null
            }

            val newToken = doRefreshRequest(refreshToken)
            if (newToken != null) {
                lastRefreshTime = System.currentTimeMillis()
            }
            return newToken
        } finally {
            refreshLock.unlock()
        }
    }

    /** 实际执行 Token 刷新的网络请求（在锁内调用） */
    private fun doRefreshRequest(refreshToken: String): String? {
        return try {
            val body = JSONObject().put("refresh", refreshToken).toString()
            val request = Request.Builder()
                .url(getBaseUrl() + ApiEndpoints.Auth.REFRESH)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = refreshClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                val newAccess = json.optString("access", "")
                val newRefresh = json.optString("refresh", "")
                if (newAccess.isNotEmpty()) {
                    saveAccessToken(newAccess)
                    if (newRefresh.isNotEmpty()) {
                        saveRefreshToken(newRefresh)
                    }
                    Log.d(TAG, "Token刷新成功")
                    newAccess
                } else {
                    Log.e(TAG, "Token刷新响应中无access字段")
                    null
                }
            } else {
                Log.e(TAG, "Token刷新失败: code=${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token刷新异常: ${e.message}")
            null
        }
    }

    suspend fun <T> get(
        endpoint: String,
        queryParams: Map<String, String>? = null,
        responseParser: (String) -> T
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder(getBaseUrl()).append(endpoint)
            queryParams?.let { params ->
                val hasQuery = endpoint.contains("?")
                var first = !hasQuery
                params.forEach { (key, value) ->
                    urlBuilder.append(if (first) "?" else "&").append(key).append("=").append(value)
                    first = false
                }
            }
            val fullUrl = urlBuilder.toString()
            Log.d(TAG, "GET 请求: $fullUrl")

            // 拦截器自动添加认证头，此处仅构建请求
            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "GET 响应: code=${response.code}, isSuccessful=${response.isSuccessful}")
            handleResponse(response, responseParser)
        } catch (e: IOException) {
            Log.e(TAG, "GET 请求失败: ${e.message}", e)
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    suspend fun <T> post(
        endpoint: String,
        body: Map<String, Any>? = null,
        responseParser: (String) -> T
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = if (body != null) {
                com.google.gson.Gson().toJson(body).toRequestBody("application/json; charset=utf-8".toMediaType())
            } else {
                "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
            }

            // 拦截器自动添加认证头，此处仅构建请求
            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()
            handleResponse(response, responseParser)
        } catch (e: IOException) {
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    suspend fun <T> put(
        endpoint: String,
        body: Map<String, Any>? = null,
        responseParser: (String) -> T
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = if (body != null) {
                com.google.gson.Gson().toJson(body).toRequestBody("application/json; charset=utf-8".toMediaType())
            } else {
                "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
            }

            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .put(jsonBody)
                .build()

            val response = client.newCall(request).execute()
            handleResponse(response, responseParser)
        } catch (e: IOException) {
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    /** PATCH 请求：用于局部更新（如修改个人资料） */
    suspend fun <T> patch(
        endpoint: String,
        body: Map<String, Any>? = null,
        responseParser: (String) -> T
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = if (body != null) {
                com.google.gson.Gson().toJson(body).toRequestBody("application/json; charset=utf-8".toMediaType())
            } else {
                "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
            }

            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .patch(jsonBody)
                .build()

            val response = client.newCall(request).execute()
            handleResponse(response, responseParser)
        } catch (e: IOException) {
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    suspend fun delete(
        endpoint: String,
        responseParser: (String) -> String = { it }
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .delete()
                .build()

            val response = client.newCall(request).execute()
            handleResponse(response, responseParser)
        } catch (e: IOException) {
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    /**
     * multipart 文件上传
     *
     * @param fileField 表单中的文件字段名（普通上传为 file，头像为 avatar）
     * @param httpMethod 请求方法，默认 POST；头像资料更新用 PATCH
     */
    suspend fun uploadFile(
        endpoint: String,
        filePath: String,
        fileName: String,
        fileType: String = "multipart/form-data",
        extraParams: Map<String, String>? = null,
        fileField: String = "file",
        httpMethod: String = "POST"
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext ApiResponse(false, -1, "文件不存在", null)
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fileField, fileName, file.asRequestBody(fileType.toMediaType()))
                .apply {
                    extraParams?.forEach { (key, value) ->
                        addFormDataPart(key, value)
                    }
                }
                .build()

            val builder = Request.Builder()
                .url(getBaseUrl() + endpoint)
            // 按指定方法提交表单（PATCH 用于资料更新类接口）
            when (httpMethod.uppercase()) {
                "PATCH" -> builder.patch(requestBody)
                "PUT" -> builder.put(requestBody)
                else -> builder.post(requestBody)
            }

            val response = client.newCall(builder.build()).execute()
            handleResponse(response) { it }
        } catch (e: IOException) {
            ApiResponse(false, -1, "文件上传失败: ${e.message}", null, e)
        }
    }

    suspend fun downloadFile(
        endpoint: String,
        savePath: String
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ApiResponse(false, response.code, "下载失败", null)
            }

            val body = response.body ?: return@withContext ApiResponse(false, -1, "响应体为空", null)

            val saveFile = File(savePath)
            saveFile.parentFile?.mkdirs()

            FileOutputStream(saveFile).use { outputStream ->
                body.source().use { source ->
                    outputStream.write(source.readByteArray())
                }
            }

            ApiResponse(true, 200, "下载成功", savePath)
        } catch (e: IOException) {
            ApiResponse(false, -1, "文件下载失败: ${e.message}", null, e)
        }
    }

    suspend fun getImageStream(endpoint: String): ApiResponse<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResponse(true, response.code, "获取成功", response.body)
            } else {
                ApiResponse(false, response.code, "获取失败", null)
            }
        } catch (e: IOException) {
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    suspend fun getVideoStream(endpoint: String): ApiResponse<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResponse(true, response.code, "获取成功", response.body)
            } else {
                ApiResponse(false, response.code, "获取失败", null)
            }
        } catch (e: IOException) {
            ApiResponse(false, -1, "网络请求失败: ${e.message}", null, e)
        }
    }

    /**
     * 多文件上传（multipart/form-data）
     */
    suspend fun uploadFiles(
        endpoint: String,
        files: List<Triple<String, String, String>>,
        params: Map<String, String>? = null
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            params?.forEach { (key, value) ->
                requestBodyBuilder.addFormDataPart(key, value)
            }

            for ((fieldName, fileName, filePath) in files) {
                val file = File(filePath)
                if (file.exists()) {
                    requestBodyBuilder.addFormDataPart(
                        fieldName,
                        fileName,
                        file.asRequestBody("image/*".toMediaType())
                    )
                }
            }

            val request = Request.Builder()
                .url(getBaseUrl() + endpoint)
                .post(requestBodyBuilder.build())
                .build()

            Log.d(TAG, "多文件上传: endpoint=$endpoint, files=${files.size}")
            val response = client.newCall(request).execute()
            handleResponse(response) { it }
        } catch (e: IOException) {
            Log.e(TAG, "多文件上传失败: ${e.message}", e)
            ApiResponse(false, -1, "多文件上传失败: ${e.message}", null, e)
        }
    }

    private fun <T> handleResponse(response: Response, parser: (String) -> T): ApiResponse<T> {
        val body = response.body?.string() ?: ""
        Log.d(TAG, "响应状态: code=${response.code}, isSuccessful=${response.isSuccessful}")

        // 401 错误处理：拦截器已尝试自动刷新 Token，此处仅返回错误
        if (response.code == 401) {
            Log.e(TAG, "收到401响应（拦截器刷新后仍失败）: $body")
            return ApiResponse(false, response.code, "认证失败: $body", null)
        }

        return if (response.isSuccessful) {
            try {
                ApiResponse(true, response.code, "请求成功", parser(body))
            } catch (e: Exception) {
                Log.e(TAG, "数据解析失败: ${e.message}", e)
                ApiResponse(false, response.code, "数据解析失败: ${e.message}", null, e)
            }
        } else {
            Log.e(TAG, "请求失败: code=${response.code}, body=$body")
            ApiResponse(false, response.code, "请求失败: $body", null)
        }
    }
}
