package com.android.aggregationspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.android.aggregationspace.ui.global.SettingsStore
import com.android.aggregationspace.ui.global.ThemeMode
import com.android.aggregationspace.ui.layout.MainScreen
import com.android.aggregationspace.ui.theme.AggregationSpaceTheme
import com.android.aggregationspace.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            // 观察主题模式
            val themeMode by SettingsStore.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            AggregationSpaceTheme(
                appTheme = if (isDark) AppTheme.Dark else AppTheme.Light
            ) {
                // 应用全局背景：图片优先，否则使用纯色
                AppBackground {
                    MainScreen()
                }
            }
        }
    }
}

/**
 * 全局背景容器
 *
 * 根据设置渲染背景图片（自定义图）或纯色背景：
 * - 若用户选择了背景图片 URI，则全屏渲染图片
 * - 否则使用设置中的背景颜色作为纯色背景
 */
@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    val bgColor by SettingsStore.bgColor.collectAsState()
    val bgImageUri by SettingsStore.bgImageUri.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsStore.parseColorSafe(bgColor))
    ) {
        // 有自定义背景图时叠加显示
        if (!bgImageUri.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(bgImageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "背景图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}
