package com.android.aggregationspace.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** 用户信息数据类 */
data class UserInfo(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    val userType: String = "user",
    // 昵称：展示用名称，为空时界面回退显示登录账号
    val nickname: String = "",
    // 头像完整地址，为空时显示默认人像图标
    val avatar: String = "",
    val createdAt: String? = null,
    val lastLogin: String? = null
) {
    val isAdmin: Boolean get() = userType == "admin"
    val isVip: Boolean get() = userType == "vip"

    /** 界面优先展示的名称：有昵称用昵称，否则回退到登录账号 */
    val displayName: String get() = nickname.ifBlank { username }
}

/** 用户管理器 - 负责登录状态、用户信息的存储和观察 */
object UserManager {
    private const val TAG = "UserManager"
    private const val USER_PREF_NAME = "UserInfo"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_USER_TYPE = "user_type"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_LAST_LOGIN = "last_login"

    private lateinit var context: Context
    private lateinit var userPrefs: SharedPreferences

    /** 用户信息状态流，UI 可观察此流 */
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    /** 是否已登录 */
    val isLoggedIn: Boolean get() = ApiService.getAccessToken() != null

    /** 初始化，在 Application 中调用 */
    fun init(context: Context) {
        this.context = context.applicationContext
        userPrefs = this.context.getSharedPreferences(USER_PREF_NAME, Context.MODE_PRIVATE)
        loadUserInfo()
    }

    /** 从本地存储加载用户信息 */
    private fun loadUserInfo() {
        val id = userPrefs.getInt(KEY_USER_ID, 0)
        if (id > 0) {
            _userInfo.value = UserInfo(
                id = id,
                username = userPrefs.getString(KEY_USERNAME, "") ?: "",
                email = userPrefs.getString(KEY_EMAIL, "") ?: "",
                userType = userPrefs.getString(KEY_USER_TYPE, "user") ?: "user",
                nickname = userPrefs.getString(KEY_NICKNAME, "") ?: "",
                avatar = userPrefs.getString(KEY_AVATAR, "") ?: "",
                createdAt = userPrefs.getString(KEY_CREATED_AT, null),
                lastLogin = userPrefs.getString(KEY_LAST_LOGIN, null)
            )
            Log.d(TAG, "已加载用户信息: ${_userInfo.value?.username}")
        }
    }

    /** 保存用户信息到本地（同步提交确保立即生效） */
    fun saveUserInfo(user: UserInfo) {
        userPrefs.edit().apply {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_USER_TYPE, user.userType)
            putString(KEY_NICKNAME, user.nickname)
            putString(KEY_AVATAR, user.avatar)
            putString(KEY_CREATED_AT, user.createdAt)
            putString(KEY_LAST_LOGIN, user.lastLogin)
        }.commit()
        _userInfo.value = user
        Log.d(TAG, "用户信息已保存: ${user.username}, type=${user.userType}")
    }

    /** 从 JSON 字符串解析并保存用户信息 */
    fun saveUserInfoFromJson(json: String) {
        try {
            val obj = JSONObject(json)
            saveUserInfo(UserInfo(
                id = obj.optInt("id", 0),
                username = obj.optString("username", ""),
                email = obj.optString("email", ""),
                userType = obj.optString("user_type", "user"),
                nickname = obj.optString("nickname", ""),
                avatar = obj.optString("avatar", ""),
                createdAt = obj.optString("created_at", null),
                lastLogin = obj.optString("last_login", null)
            ))
        } catch (e: Exception) {
            Log.e(TAG, "解析用户信息失败: ${e.message}", e)
        }
    }

    /** 清除用户信息（退出登录时调用） */
    fun clearUserInfo() {
        userPrefs.edit().clear().apply()
        _userInfo.value = null
        Log.d(TAG, "用户信息已清除")
    }

    /** 退出登录：清除 token 和用户信息 */
    fun logout() {
        ApiService.clearToken()
        clearUserInfo()
    }

    /** 获取当前用户信息（可能为 null 表示未登录） */
    fun getUser(): UserInfo? = _userInfo.value
}
