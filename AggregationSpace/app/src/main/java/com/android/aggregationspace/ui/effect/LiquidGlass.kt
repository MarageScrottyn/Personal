package com.android.aggregationspace.ui.effect

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LiquidGlassConfig(
    val blurRadius: Dp = 2.dp,
    val tint: Color = Color.Unspecified,
    val surfaceColor: Color = Color.Unspecified,
    val isInteractive: Boolean = true,
    val highlightColor: Color = Color.White,
    val highlightOpacity: Float = 0.15f,
    val pressScale: Float = 4f,
    val dragScale: Float = 4f
) {
    companion object {
        val Default = LiquidGlassConfig()
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    config: LiquidGlassConfig = LiquidGlassConfig.Default
): Modifier {
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope,
            highlightColor = config.highlightColor,
            highlightOpacity = config.highlightOpacity
        )
    }

    return this
        .graphicsLayer {
            clip = true
            this.shape = shape
            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
        }
        .drawBehind {
            if (config.surfaceColor.isSpecified) {
                drawRect(config.surfaceColor)
            }
            if (config.tint.isSpecified) {
                drawRect(config.tint, blendMode = BlendMode.Hue)
                drawRect(config.tint.copy(alpha = 0.75f))
            }
        }
        .graphicsLayer {
            renderEffect = createBlurEffect(config.blurRadius.toPx())
            val progress = interactiveHighlight.pressProgress
            if (config.isInteractive && progress > 0f) {
                val scale = lerp(1f, 1f + config.pressScale.dp.toPx() / size.height, progress)
                val maxOffset = size.minDimension
                val initialDerivative = 0.05f
                val offset = interactiveHighlight.offset
                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                val maxDragScale = config.dragScale.dp.toPx() / size.height
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension)
                scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension)
            }
        }
        .then(
            if (config.isInteractive) {
                Modifier
                    .then(interactiveHighlight.modifier)
                    .then(interactiveHighlight.gestureModifier)
            } else {
                Modifier
            }
        )
}

private fun createBlurEffect(radius: Float): androidx.compose.ui.graphics.RenderEffect? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            androidx.compose.ui.graphics.BlurEffect(radius, radius, androidx.compose.ui.graphics.TileMode.Clamp)
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    onClick: () -> Unit,
    config: LiquidGlassConfig = LiquidGlassConfig.Default
): Modifier {
    return this
        .liquidGlass(shape, config)
        .clickable(
            interactionSource = null,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
}

@Composable
fun LiquidGlassBox(
    shape: Shape,
    modifier: Modifier = Modifier,
    config: LiquidGlassConfig = LiquidGlassConfig.Default,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.liquidGlass(shape, config),
        content = content
    )
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: LiquidGlassConfig = LiquidGlassConfig.Default,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .liquidGlass(androidx.compose.foundation.shape.RoundedCornerShape(24.dp), config)
//            .height(48.dp)
            .padding(horizontal = 16.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center,
        content = content
    )
}

private class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
    val highlightColor: Color = Color.White,
    val highlightOpacity: Float = 0.15f
) {

    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(highlightColor.copy((0.08f + highlightOpacity) * progress), blendMode = BlendMode.Plus)
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            }
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }
}

private suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)
        val drag = initialDown
        onDragStart(down)
        onDrag(drag, Offset.Zero)
        val upEvent = drag(pointerId = drag.id, onDrag = { onDrag(it, it.positionChange()) })
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.firstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.firstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.firstOrNull { it.pressed }
            if (otherDown == null) return dragEvent else pointer = otherDown.id
        } else {
            if (dragEvent.previousPosition != dragEvent.position) return dragEvent
        }
    }
}