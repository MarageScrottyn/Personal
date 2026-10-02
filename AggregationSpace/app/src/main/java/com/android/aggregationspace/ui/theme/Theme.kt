package com.android.aggregationspace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.android.aggregationspace.ui.theme.GlassColorScheme

enum class AppTheme {
    Light,
    Dark,
    System
}

fun GlassColorScheme.toColorScheme(): ColorScheme {
    return lightColorScheme(
        // 这里必须把自定义属性映射到系统属性上
        primary = this.glassPrimary,
        onPrimary = Color.White,      // 你可能需要补充这些辅助色
        secondary = this.glassSecondary,
        onSecondary = Color.White,
        tertiary = this.glassTertiary,
        onTertiary = Color.White,
        background = this.glassBackground, // 建议补充背景色
        surface = Color(0xFFFFFBFE),
        // ... 其他颜色按需映射
    )
}

private val LightColorScheme = GlassColorScheme(
    glassPrimary = Blue01,
    glassSecondary = Green01,
    glassTertiary = White01,
    glassBackground = White02
)

private val DarkColorScheme = GlassColorScheme(
    glassPrimary = Purple40,
    glassSecondary = PurpleGrey40,
    glassTertiary = Pink40
)

@Composable
fun AggregationSpaceTheme(
    appTheme: AppTheme = AppTheme.Light,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val glassColorScheme = when(appTheme) {
        AppTheme.Light -> LightColorScheme
        AppTheme.Dark -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = glassColorScheme.toColorScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}