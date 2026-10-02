package com.android.aggregationspace.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import com.android.aggregationspace.ui.style.liquidGlass


data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String?,
    val onClick: () -> Unit
)

@Composable
private fun ActionButton(action: TopBarAction) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF007AFF))
            .clickable { action.onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

@Composable
fun DynamicTopBar(
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
    title: String = "",
    baseColor: Color = Color.Red,
    modifier: Modifier = Modifier
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = 56.dp
    val totalHeight = statusBarHeight + navBarHeight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .zIndex(2f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showBackButton) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .liquidGlass()
                        .background(Color.White)
                        .clickable { onBackClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    ActionButton(action = action)
                }
            }
        }
    }
}

@Composable
fun TopBarWithScroll(
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
    title: String = "",
    baseColor: Color = Color.Red,
    content: @Composable () -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = 56.dp
    val totalHeight = statusBarHeight + navBarHeight

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showBackButton) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .liquidGlass()
                            .background(Color.White)
                            .clickable { onBackClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { action ->
                        ActionButton(action = action)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight)
        ) {
            content()
        }
    }
}

@Preview
@Composable
fun showTool() {
    DynamicTopBar(
        showBackButton = true,
        title = "Title",
        actions = listOf(
            TopBarAction(Icons.Default.MoreVert, "更多", {})
        )
    )
}