package com.android.aggregationspace.ui.style

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.multiShadow(
    color1: Color = Color.White.copy(alpha = 0.5f),
    offsetX1: Dp = 2.dp,
    offsetY1: Dp = 2.dp,
    blur1: Dp = 2.dp,
    spread1: Dp = 0.dp,
    color2: Color = Color.Black.copy(alpha = 0.1f),
    offsetX2: Dp = 7.dp,
    offsetY2: Dp = 7.dp,
    blur2: Dp = 20.dp,
    spread2: Dp = 0.dp,
    color3: Color = Color.Black.copy(alpha = 0.1f),
    offsetX3: Dp = 4.dp,
    offsetY3: Dp = 4.dp,
    blur3: Dp = 5.dp,
    spread3: Dp = 0.dp,
    borderRadius: Dp = 0.dp
): Modifier = this.drawBehind {
    val cornerRadiusPx = borderRadius.toPx()

    // 封装一个绘制单层阴影的函数
    fun drawShadowLayer(
        color: Color, offsetX: Dp, offsetY: Dp, blur: Dp, spread: Dp
    ) {
        if (blur == 0.dp && spread == 0.dp && color.alpha == 0f) return

        val paint = Paint().apply {
            this.color = color
            // Compose 的 blur 对应 CSS 的 blur-radius，这里做近似映射
            this.asFrameworkPaint().setShadowLayer(
                blur.toPx(),
                offsetX.toPx(),
                offsetY.toPx(),
                color.toArgb()
            )
        }

        drawIntoCanvas { canvas ->
            // spread 效果：通过扩大绘制区域来模拟
            val spreadPx = spread.toPx()
            val left = spreadPx
            val top = spreadPx
            val right = size.width - spreadPx
            val bottom = size.height - spreadPx

            canvas.drawRoundRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                radiusX = cornerRadiusPx,
                radiusY = cornerRadiusPx,
                paint = paint
            )
        }
    }

    // 按照 CSS 的顺序从下往上绘制（先绘制底层大阴影，再绘制顶层小阴影）
    drawShadowLayer(color2, offsetX2, offsetY2, blur2, spread2) // 7px 7px 20px
    drawShadowLayer(color3, offsetX3, offsetY3, blur3, spread3) // 4px 4px 5px
    drawShadowLayer(color1, offsetX1, offsetY1, blur1, spread1) // inset 2px 2px 2px (注意：inset 需要特殊处理，见下方说明)
}

// 内阴影的替代绘制逻辑（性能开销较大，建议仅在必要时使用）
fun Modifier.insetShadow(
    color: Color,
    offsetX: Dp,
    offsetY: Dp,
    blur: Dp,
    borderRadius: Dp
) = this.drawWithContent {
    drawContent() // 先画原内容
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            this.asFrameworkPaint().setShadowLayer(
                blur.toPx(), offsetX.toPx(), offsetY.toPx(), color.toArgb()
            )
            // 关键：使用 SrcIn 将外部阴影裁剪到内部
            blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
        }
        // 绘制一个比组件稍大的区域，利用 blendMode 只保留组件内部的重叠部分
        canvas.drawRect(
            left = -size.width, top = -size.height,
            right = size.width * 2, bottom = size.height * 2,
            paint = paint
        )
    }
}

fun Modifier.liquidGlass(
    borderRadius: Dp = 12.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    topHighlightColor: Color = Color.White,
    topHighlightAlpha: Float = 0.35f,
    bottomShadowColor: Color = Color.Black,
    bottomShadowAlpha: Float = 0.1f,
    outerShadowRadius: Float = 12f,
    outerShadowSpread: Float = 5f,
    outerShadowAlpha: Float = 0.06f
): Modifier = this
    .dropShadow(shape = androidx.compose.foundation.shape.RoundedCornerShape(borderRadius)) {
        radius = outerShadowRadius
        spread = outerShadowSpread
        color = Color.Black.copy(alpha = outerShadowAlpha)
    }
    .dropShadow(shape = androidx.compose.foundation.shape.RoundedCornerShape(borderRadius)) {
        radius = outerShadowRadius * 0.5f
        spread = outerShadowSpread * 0.4f
        color = Color.Black.copy(alpha = outerShadowAlpha * 1.5f)
    }
    .drawBehind {
        val cornerRadiusPx = borderRadius.toPx()
        
        drawRoundRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    topHighlightColor.copy(alpha = topHighlightAlpha),
                    topHighlightColor.copy(alpha = topHighlightAlpha * 0.5f),
                    topHighlightColor.copy(alpha = topHighlightAlpha * 0.17f),
                    topHighlightColor.copy(alpha = 0f)
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height)
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    bottomShadowColor.copy(alpha = 0f),
                    bottomShadowColor.copy(alpha = bottomShadowAlpha * 0.25f),
                    bottomShadowColor.copy(alpha = bottomShadowAlpha * 0.75f),
                    bottomShadowColor.copy(alpha = bottomShadowAlpha)
                ),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height)
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    }