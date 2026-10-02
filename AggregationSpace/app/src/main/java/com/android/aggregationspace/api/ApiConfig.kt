package com.android.aggregationspace.api

import android.content.Context
import android.content.SharedPreferences

object ApiConfig {
    private const val PREF_NAME = "ApiConfig"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_CONFIG_ENABLED = "api_config_enabled"
    // 默认服务器地址：使用 Cloudflare 域名（HTTPS）
    private const val DEFAULT_BASE_URL = "https://marage.ccwu.cc"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(context: Context): String {
        return getPreferences(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        getPreferences(context).edit().putString(KEY_BASE_URL, url).apply()
    }

    fun isApiConfigEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_API_CONFIG_ENABLED, false)
    }

    fun setApiConfigEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_API_CONFIG_ENABLED, enabled).apply()
    }
}
