package com.android.aggregationspace.ui.global

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.android.aggregationspace.api.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 主题模式：浅色/深色/跟随系统 */
enum class ThemeMode(val displayName: String) {
    LIGHT("浅色"),
    DARK("深色"),
    SYSTEM("跟随系统")
}

/** 字体样式：默认/衬线/等宽/手写 */
enum class AppFontFamily(val displayName: String, val composeName: androidx.compose.ui.text.font.FontFamily) {
    DEFAULT("默认", androidx.compose.ui.text.font.FontFamily.Default),
    SERIF("衬线", androidx.compose.ui.text.font.FontFamily.Serif),
    MONOSPACE("等宽", androidx.compose.ui.text.font.FontFamily.Monospace),
    CURSIVE("手写", androidx.compose.ui.text.font.FontFamily.Cursive)
}

/** 字体大小档位：小/中/大/特大，附带缩放比例 */
enum class AppFontSize(val displayName: String, val scale: Float) {
    SMALL("小", 0.85f),
    MEDIUM("中", 1.0f),
    LARGE("大", 1.15f),
    XLARGE("特大", 1.3f)
}

/**
 * 应用设置存储器
 *
 * 负责持久化以下设置并对外暴露响应式 StateFlow：
 * - 主题模式（浅色/深色/跟随系统）
 * - 语言模式（暂时仅中文）
 * - 字体样式与字体大小
 * - 背景颜色与背景图片
 * - 隐私模式开关（开启时本地将账户权限置为普通，关闭后恢复）
 */
object SettingsStore {
    private const val TAG = "SettingsStore"
    private const val PREF_NAME = "AppSettings"

    // 各设置项的存储键
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_FONT_FAMILY = "font_family"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_BG_IMAGE_URI = "bg_image_uri"
    private const val KEY_PRIVACY_MODE = "privacy_mode"
    private const val KEY_ORIGINAL_USER_TYPE = "original_user_type"

    // 默认值
    private const val DEFAULT_BG_COLOR = "#FF0A0E27"
    private const val DEFAULT_LANGUAGE = "简体中文"

    private lateinit var prefs: SharedPreferences

    // ===== 响应式状态流，UI 通过 collectAsState 观察 =====
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(DEFAULT_LANGUAGE)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _fontFamily = MutableStateFlow(AppFontFamily.DEFAULT)
    val fontFamily: StateFlow<AppFontFamily> = _fontFamily.asStateFlow()

    private val _fontSize = MutableStateFlow(AppFontSize.MEDIUM)
    val fontSize: StateFlow<AppFontSize> = _fontSize.asStateFlow()

    private val _bgColor = MutableStateFlow(DEFAULT_BG_COLOR)
    val bgColor: StateFlow<String> = _bgColor.asStateFlow()

    private val _bgImageUri = MutableStateFlow<String?>(null)
    val bgImageUri: StateFlow<String?> = _bgImageUri.asStateFlow()

    private val _privacyMode = MutableStateFlow(false)
    val privacyMode: StateFlow<Boolean> = _privacyMode.asStateFlow()

    /** 初始化，需在 Application.onCreate 中调用 */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadSettings()
    }

    /** 从本地存储加载所有设置到状态流 */
    private fun loadSettings() {
        _themeMode.value = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)

        _language.value = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

        _fontFamily.value = runCatching {
            AppFontFamily.valueOf(prefs.getString(KEY_FONT_FAMILY, AppFontFamily.DEFAULT.name)!!)
        }.getOrDefault(AppFontFamily.DEFAULT)

        _fontSize.value = runCatching {
            AppFontSize.valueOf(prefs.getString(KEY_FONT_SIZE, AppFontSize.MEDIUM.name)!!)
        }.getOrDefault(AppFontSize.MEDIUM)

        _bgColor.value = prefs.getString(KEY_BG_COLOR, DEFAULT_BG_COLOR) ?: DEFAULT_BG_COLOR
        _bgImageUri.value = prefs.getString(KEY_BG_IMAGE_URI, null)
        _privacyMode.value = prefs.getBoolean(KEY_PRIVACY_MODE, false)
    }

    /** 设置主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    /** 设置语言（暂时仅支持简体中文，预留接口） */
    fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        _language.value = language
    }

    /** 设置字体样式 */
    fun setFontFamily(family: AppFontFamily) {
        prefs.edit().putString(KEY_FONT_FAMILY, family.name).apply()
        _fontFamily.value = family
    }

    /** 设置字体大小档位 */
    fun setFontSize(size: AppFontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, size.name).apply()
        _fontSize.value = size
    }

    /** 设置背景颜色（#RRGGBB 或 #AARRGGBB 格式） */
    fun setBgColor(colorHex: String) {
        prefs.edit().putString(KEY_BG_COLOR, colorHex).apply()
        _bgColor.value = colorHex
    }

    /** 设置背景图片 URI，传 null 表示清除使用纯色背景 */
    fun setBgImageUri(uri: String?) {
        prefs.edit().putString(KEY_BG_IMAGE_URI, uri).apply()
        _bgImageUri.value = uri
    }

    /**
     * 设置隐私模式开关
     *
     * - 开启：保存当前用户类型为原始类型，然后将本地用户类型置为 "user"
     * - 关闭：从存储中读取原始类型并恢复，清除临时存储
     *
     * 通过 UserManager 的 StateFlow 自动触发 UI 立即刷新
     */
    fun setPrivacyMode(enabled: Boolean) {
        if (enabled) {
            // 首次开启时保存原始用户类型，避免重复开启覆盖
            if (!prefs.contains(KEY_ORIGINAL_USER_TYPE)) {
                val currentUser = UserManager.getUser()
                if (currentUser != null && currentUser.userType != "user") {
                    prefs.edit().putString(KEY_ORIGINAL_USER_TYPE, currentUser.userType).apply()
                    Log.d(TAG, "已保存原始用户类型: ${currentUser.userType}")
                    // 本地降级为普通用户，UI 立即响应
                    UserManager.saveUserInfo(currentUser.copy(userType = "user"))
                }
            }
        } else {
            // 关闭时恢复原始类型
            val originalType = prefs.getString(KEY_ORIGINAL_USER_TYPE, null)
            val currentUser = UserManager.getUser()
            if (originalType != null && currentUser != null) {
                UserManager.saveUserInfo(currentUser.copy(userType = originalType))
                Log.d(TAG, "已恢复原始用户类型: $originalType")
            }
            prefs.edit().remove(KEY_ORIGINAL_USER_TYPE).apply()
        }
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
        _privacyMode.value = enabled

        // 清空所有分类页面的缓存，确保下次进入时按新权限重新加载
        // 这样隐私模式开启后特辑/特殊权限内容会被立即从列表中过滤掉
        PageCache.clearAll()
    }

    /** 将 #RRGGBB / #AARRGGBB 格式字符串解析为 Compose Color，解析失败返回默认背景色 */
    fun parseColorSafe(colorHex: String): Color {
        return runCatching {
            // 支持 #RGB / #RRGGBB / #AARRGGBB
            val normalized = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
            Color(android.graphics.Color.parseColor(normalized))
        }.getOrDefault(Color(parseLongFromHex(DEFAULT_BG_COLOR)))
    }

    /** 将默认色字符串转换为 Long，用于 Color(Long) 构造 */
    private fun parseLongFromHex(hex: String): Long {
        return hex.removePrefix("#").toLong(16)
    }
}
