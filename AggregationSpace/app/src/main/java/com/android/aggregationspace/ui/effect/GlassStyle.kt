import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * 液体水滴玻璃按钮
 */
fun Modifier.liquidGlassBlack(
    glassColor: Color = Color.White,
    intensity: Float = 0.7f,
    cornerRadius: Dp = 16.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        // 底部阴影 - 让水滴看起来浮在表面上
        .shadow(
            elevation = (2 * intensity).dp,  // 小高度，阴影紧贴
            shape = shape,
            clip = false
        )
        // 水滴主体 - 中心亮、边缘暗，模拟凸起
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    glassColor.copy(alpha = intensity * 0.2f),   // 中心 - 稍亮
                    glassColor.copy(alpha = intensity * 0.1f),   // 中间
                    glassColor.copy(alpha = intensity * 0.05f),  // 边缘 - 更透明
                ),
                center = Offset(0.5f, 0.4f),
                radius = 0.7f
            ),
            shape = shape
        )
        // 高光点 - 左上角强反光，模拟水滴表面的镜面反射
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.8f),   // 强光核心
                    Color.White.copy(alpha = intensity * 0.3f),   // 光晕
                    Color.White.copy(alpha = 0f),                 // 完全透明
                ),
                center = Offset(0.25f, 0.25f),
                radius = 0.25f
            ),
            shape = shape
        )
        // 边缘光 - 水滴边缘的环形光泽
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.5f),  // 左上角边缘亮
                    Color.White.copy(alpha = intensity * 0.2f),
                    Color.White.copy(alpha = intensity * 0.1f),
                    Color.White.copy(alpha = intensity * 0.2f),  // 右下角边缘稍亮
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}


/**
 * 液体水滴玻璃按钮 - 透亮白净
 */
fun Modifier.liquidGlass1(
    glassColor: Color = Color.White,
    intensity: Float = 0.7f,
    cornerRadius: Dp = 16.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = (2 * intensity).dp,
            shape = shape,
            clip = false
        )
        // 水滴主体 - 中心透白，边缘微暗形成凸起感
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.15f),   // 中心 - 透白
                    Color.White.copy(alpha = intensity * 0.1f),    // 中间
                    Color.White.copy(alpha = intensity * 0.05f),   // 边缘 - 稍暗
                ),
                center = Offset(0.5f, 0.4f),
                radius = 0.7f
            ),
            shape = shape
        )
        // 高光点 - 强白光
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.9f),    // 强白光核心
                    Color.White.copy(alpha = intensity * 0.4f),    // 光晕
                    Color.White.copy(alpha = 0f),                  // 完全透明
                ),
                center = Offset(0.25f, 0.25f),
                radius = 0.25f
            ),
            shape = shape
        )
        // 边缘光
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.5f),
                    Color.White.copy(alpha = intensity * 0.25f),
                    Color.White.copy(alpha = intensity * 0.15f),
                    Color.White.copy(alpha = intensity * 0.25f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}

/**
 * 液体水滴玻璃按钮 - 高透亮
 */
fun Modifier.liquidGlass2(
    glassColor: Color = Color.White,
    intensity: Float = 0.7f,
    cornerRadius: Dp = 16.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = (2 * intensity).dp,
            shape = shape,
            clip = false
        )
        // 水滴主体 - 非常淡，几乎透明
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.08f),   // 中心 - 极淡
                    Color.White.copy(alpha = intensity * 0.04f),   // 中间
                    Color.White.copy(alpha = intensity * 0.02f),   // 边缘
                ),
                center = Offset(0.5f, 0.4f),
                radius = 0.7f
            ),
            shape = shape
        )
        // 高光点 - 强烈的白色反光
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.95f),   // 强白光核心
                    Color.White.copy(alpha = intensity * 0.5f),    // 光晕
                    Color.White.copy(alpha = 0f),                  // 完全透明
                ),
                center = Offset(0.25f, 0.25f),
                radius = 0.3f
            ),
            shape = shape
        )
        // 边缘光
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.6f),
                    Color.White.copy(alpha = intensity * 0.3f),
                    Color.White.copy(alpha = intensity * 0.15f),
                    Color.White.copy(alpha = intensity * 0.3f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}

/**
 * 液体水滴玻璃 - 饱满凸起的水滴效果
 */
fun Modifier.liquidGlass3(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.12f,
    cornerRadius: Dp = 20.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        // 底部投影 - 水滴在表面上的阴影，稍大才有凸起感
        .shadow(
            elevation = 1.4.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.15f),
            spotColor = Color.Black.copy(alpha = 0.2f),
            clip = false
        )
        .clip(shape)
        // 水滴主体 - 中心亮、边缘暗，模拟水滴曲面
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    tintColor.copy(alpha = blurAlpha * 1.8f),  // 中心稍亮
                    tintColor.copy(alpha = blurAlpha * 1.2f),
                    tintColor.copy(alpha = blurAlpha * 0.8f),
                    tintColor.copy(alpha = blurAlpha * 0.5f),  // 边缘更透明
                ),
                center = Offset(0.5f, 0.45f),
                radius = 0.75f
            )
        )
        // 强高光点 - 水滴表面的镜面反射
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),   // 强光核心
                    Color.White.copy(alpha = 0.3f),   // 光晕
                    Color.White.copy(alpha = 0.05f),  // 扩散
                    Color.Transparent
                ),
                center = Offset(0.25f, 0.2f),
                radius = 0.3f
            )
        )
        // 底部反光 - 水滴底部的微弱反光
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(0.6f, 0.85f),
                radius = 0.25f
            )
        )
        // 边缘光泽 - 水滴表面张力的环形光
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),   // 左上亮
                    Color.White.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.08f),  // 右下暗
                    Color.White.copy(alpha = 0.2f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}


/**
 * 玻璃按钮样式 - 白色背景也可见
 */
fun Modifier.liquidGlass4(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.15f,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.25f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        // 底层阴影 - 让按钮在白色背景上也能看到轮廓
        .shadow(
            elevation = 4.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.1f),
            spotColor = Color.Black.copy(alpha = 0.15f),
            clip = false
        )
        .clip(shape)
        // 玻璃主体
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    tintColor.copy(alpha = blurAlpha * 1.3f),
                    tintColor.copy(alpha = blurAlpha * 0.9f),
                    tintColor.copy(alpha = blurAlpha * 0.7f),
                    tintColor.copy(alpha = blurAlpha * 0.8f)
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        // 顶部高光
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        // 左上角光点
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(0.2f, 0.2f),
                radius = 0.25f
            )
        )
        // 边框 - 白色背景上靠边框区分
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.6f),
                    Color.White.copy(alpha = borderAlpha * 0.3f),
                    Color.White.copy(alpha = borderAlpha * 0.6f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}


/**
 * 玻璃按钮样式
 */
fun Modifier.glassButton(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.15f,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.25f,
    shadowAlpha: Float = 0.1f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = 1.2.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = shadowAlpha),
            spotColor = Color.Black.copy(alpha = shadowAlpha * 1.5f),
            clip = true
        )
        .shadow(
            elevation = 1.2.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = shadowAlpha),
            spotColor = Color.Black.copy(alpha = shadowAlpha * 1.5f),
            clip = true
        )
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    tintColor.copy(alpha = blurAlpha * 1.3f),
                    tintColor.copy(alpha = blurAlpha * 0.9f),
                    tintColor.copy(alpha = blurAlpha * 0.7f),
                    tintColor.copy(alpha = blurAlpha * 0.8f)
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(0.2f, 0.2f),
                radius = 0.25f
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.6f),
                    Color.White.copy(alpha = borderAlpha * 0.3f),
                    Color.White.copy(alpha = borderAlpha * 0.6f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}



/**
 * 磨砂白玻璃样式
 */
fun Modifier.liquidGlass5(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.15f,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.3f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        // 柔和的底部阴影 - 模拟玻璃下方的投影
        .shadow(
            elevation = 1.4.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.12f)
        )
        // 玻璃主体 - 半透明毛玻璃
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    tintColor.copy(alpha = blurAlpha * 1.2f),
                    tintColor.copy(alpha = blurAlpha * 0.8f),
                    tintColor.copy(alpha = blurAlpha * 0.6f),
                    tintColor.copy(alpha = blurAlpha)
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        // 顶部强高光 - 玻璃表面反光
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        // 左上角光点
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(0.2f, 0.2f),
                radius = 0.3f
            )
        )
        // 玻璃边框
        .border(
            width = 0.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.5f),
                    Color.White.copy(alpha = borderAlpha * 0.2f),
                    Color.White.copy(alpha = borderAlpha * 0.5f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}


/**
 * 磨砂玻璃样式
 */
fun Modifier.frostedGlass(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.1f,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.2f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = 1.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.1f)
        )
        .clip(shape)
        .background(tintColor.copy(alpha = blurAlpha))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        .border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = borderAlpha),
            shape = shape
        )
}


/**
 * 黑凹玻璃按钮
 */
fun Modifier.frostedLiquidGlass(
    glassColor: Color = Color.White,
    intensity: Float = 0.7f,
    cornerRadius: Dp = 16.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = (1.5f * intensity).dp,
            shape = shape,
            clip = false
        )
        // 磨砂主体 - 均匀透白
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.12f),
                    Color.White.copy(alpha = intensity * 0.08f),
                    Color.White.copy(alpha = intensity * 0.04f),
                ),
                center = Offset(0.5f, 0.4f),
                radius = 0.7f
            ),
            shape = shape
        )
        // 柔和高光
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.6f),
                    Color.White.copy(alpha = intensity * 0.2f),
                    Color.White.copy(alpha = 0f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 0.3f
            ),
            shape = shape
        )
        .border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = intensity * 0.3f),
            shape = shape
        )
}


/**
 * 磨砂玻璃 - 均匀雾面效果
 */
fun Modifier.frostedGlass3(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.08f,
    cornerRadius: Dp = 20.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        // 小阴影 - 磨砂表面更平
        .shadow(
            elevation = 3.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.12f),
            clip = false
        )
        .clip(shape)
        // 均匀的半透明
        .background(tintColor.copy(alpha = blurAlpha))
        // 极淡的顶部高光
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        // 细边框
        .border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = 0.2f),
            shape = shape
        )
}


/**
 * 磨砂玻璃按钮样式
 */
fun Modifier.frostedGlass4(
    tintColor: Color = Color.White,
    blurAlpha: Float = 0.1f,
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.2f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = 3.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.12f),
            clip = false
        )
        .clip(shape)
        .background(tintColor.copy(alpha = blurAlpha))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        )
        .border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = borderAlpha),
            shape = shape
        )
}


/**
 * 彩色液体水滴玻璃
 */
fun Modifier.coloredLiquidGlass(
    color: Color,
    blurAlpha: Float = 0.15f,
    cornerRadius: Dp = 20.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = color.copy(alpha = 0.15f),
            spotColor = color.copy(alpha = 0.2f),
            clip = false
        )
        .clip(shape)
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = blurAlpha * 1.8f),
                    color.copy(alpha = blurAlpha * 1.2f),
                    color.copy(alpha = blurAlpha * 0.8f),
                    color.copy(alpha = blurAlpha * 0.5f),
                ),
                center = Offset(0.5f, 0.45f),
                radius = 0.75f
            )
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(0.25f, 0.2f),
                radius = 0.3f
            )
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(0.6f, 0.85f),
                radius = 0.25f
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    color.copy(alpha = 0.3f),
                    color.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.25f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}


/**
 * 彩色液体水滴玻璃按钮
 */
fun Modifier.coloredLiquidGlass1(
    color: Color,
    intensity: Float = 0.7f,
    cornerRadius: Dp = 16.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = (2 * intensity).dp,
            shape = shape,
            ambientColor = color.copy(alpha = 0.15f),
            spotColor = color.copy(alpha = 0.2f),
            clip = false
        )
        // 彩色主体 - 中心亮、透
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.18f),   // 中心透白
                    color.copy(alpha = intensity * 0.12f),         // 过渡到颜色
                    color.copy(alpha = intensity * 0.06f),         // 边缘
                ),
                center = Offset(0.5f, 0.4f),
                radius = 0.7f
            ),
            shape = shape
        )
        // 白色高光
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.9f),
                    Color.White.copy(alpha = intensity * 0.4f),
                    Color.White.copy(alpha = 0f),
                ),
                center = Offset(0.25f, 0.25f),
                radius = 0.25f
            ),
            shape = shape
        )
        // 边缘光
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.5f),
                    color.copy(alpha = intensity * 0.3f),
                    color.copy(alpha = intensity * 0.15f),
                    Color.White.copy(alpha = intensity * 0.25f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}


/**
 * 彩色液体水滴玻璃
 */
fun Modifier.coloredLiquidGlass3(
    color: Color,
    blurAlpha: Float = 0.15f,
    cornerRadius: Dp = 20.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)

    return this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = color.copy(alpha = 0.15f),
            spotColor = color.copy(alpha = 0.2f),
            clip = false
        )
        .clip(shape)
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = blurAlpha * 1.8f),
                    color.copy(alpha = blurAlpha * 1.2f),
                    color.copy(alpha = blurAlpha * 0.8f),
                    color.copy(alpha = blurAlpha * 0.5f),
                ),
                center = Offset(0.5f, 0.45f),
                radius = 0.75f
            )
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(0.25f, 0.2f),
                radius = 0.3f
            )
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(0.6f, 0.85f),
                radius = 0.25f
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.5f),
                    color.copy(alpha = 0.3f),
                    color.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.25f),
                ),
                center = Offset(0.3f, 0.3f),
                radius = 1.0f
            ),
            shape = shape
        )
}


@Preview
@Composable
fun GlassButtonDemo() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
            .background(Color.White.copy(0.11f))
,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 基础玻璃按钮
            Box(
                modifier = Modifier
                    .liquidGlassBlack(
                        glassColor = Color.White,
                        intensity = 0.7f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("玻璃按钮", color = Color(0xFF555555), fontSize = 16.sp)
            }

            // 基础玻璃按钮1
            Box(
                modifier = Modifier
                    .liquidGlass1(
                        glassColor = Color.White,
                        intensity = 0.7f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("玻璃按钮", color = Color(0xFF555555), fontSize = 16.sp)
            }

            // 基础玻璃按钮2
            Box(
                modifier = Modifier
                    .liquidGlass2(
                        glassColor = Color.White,
                        intensity = 0.7f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("玻璃按钮", color = Color(0xFF555555), fontSize = 16.sp)
            }

            // 基础玻璃按钮3
            Box(
                modifier = Modifier
                    .liquidGlass3(
                        tintColor = Color.White,
                        blurAlpha = 0.12f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("玻璃按钮", color = Color(0xFF555555), fontSize = 16.sp)
            }

            // 基础玻璃按钮4
            Box(
                modifier = Modifier
                    .liquidGlass4(
                        tintColor = Color.White,
                        blurAlpha = 0.2f,        // 稍微提高透明度
                        cornerRadius = 24.dp,
                        borderAlpha = 0.4f        // 加强边框
                    )
                    .clickable { }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("玻璃按钮", color = Color(0xFF555555), fontSize = 16.sp)
            }

            // 基础玻璃按钮5
            Box(
                modifier = Modifier
                    .glassButton(
                        tintColor = Color.White,
                        blurAlpha = 0.25f,
                        cornerRadius = 24.dp,
                        borderAlpha = 0.5f,
                        shadowAlpha = 0.35f      // 大幅加强
                    )
                    .clickable { }
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("玻璃按钮", color = Color(0xFF555555), fontSize = 16.sp)
            }


            // 磨砂玻璃按钮
            Box(
                modifier = Modifier
                    .frostedGlass(
                        tintColor = Color.White,
                        blurAlpha = 0.1f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 48.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("磨砂玻璃", color = Color.White, fontSize = 18.sp)
            }

            // 磨砂玻璃按钮
            Box(
                modifier = Modifier
                    .frostedGlass3(
                        tintColor = Color.White,
                        blurAlpha = 0.1f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 48.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("磨砂玻璃", color = Color.White, fontSize = 18.sp)
            }

            // 磨砂玻璃按钮
            Box(
                modifier = Modifier
                    .frostedGlass4(
                        tintColor = Color.White,
                        blurAlpha = 0.1f,
                        cornerRadius = 24.dp
                    )
                    .clickable { }
                    .padding(horizontal = 48.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("磨砂玻璃", color = Color.White, fontSize = 18.sp)
            }

            // 彩色水滴
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    Color(0xFF64B5F6) to "蓝色",
                    Color(0xFFE57373) to "红色",
                ).forEach { (color, name) ->
                    Box(
                        modifier = Modifier
                            .coloredLiquidGlass(
                                color = color,
                                blurAlpha = 0.15f,
                                cornerRadius = 20.dp
                            )
                            .clickable { }
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name, color = Color.White, fontSize = 16.sp)
                    }

                    Box(
                        modifier = Modifier
                            .coloredLiquidGlass3(
                                color = color,
                                blurAlpha = 0.15f,
                                cornerRadius = 20.dp
                            )
                            .clickable { }
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}