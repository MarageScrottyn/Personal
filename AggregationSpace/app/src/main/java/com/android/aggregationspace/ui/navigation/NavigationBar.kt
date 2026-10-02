package com.android.aggregationspace.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.aggregationspace.ui.style.liquidGlass
import com.android.aggregationspace.R
import com.android.aggregationspace.ui.effect.liquidGlass4
import com.android.aggregationspace.ui.effect.frostedGlass


enum class BottomNavExpanded {
    None,
    Menu,
    Search
}

data class SubMenuItem(
    val iconResId: Int,
    val label: String,
    val route: String
)

data class MainMenuItem(
    val label: String,
    val iconResId: Int
)

private val mainMenuItems = listOf(
    MainMenuItem("图片", R.drawable.baseline_image_24),
    MainMenuItem("视频", R.drawable.baseline_video_library_24),
    MainMenuItem("资源", R.drawable.baseline_folder_24),
    MainMenuItem("我的", R.drawable.baseline_person_24)
)

private val subMenuMap = mapOf(
    "图片" to listOf(
        SubMenuItem(
            iconResId = R.drawable.baseline_image_24,
            label = "图片",
            route = "picture"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_filter_hdr_24,
            label = "漫画",
            route = "comic"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_photo_library_24,
            label = "图集",
            route = "pictureAlbum"
        )
    ),
    "视频" to listOf(
        SubMenuItem(
            iconResId = R.drawable.baseline_video_library_24,
            label = "视频",
            route = "video"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_movie_24,
            label = "特辑",
            route = "specialEdit"
        )
    ),
    "资源" to listOf(
        SubMenuItem(
            iconResId = R.drawable.baseline_cloud_24,
            label = "云盘",
            route = "cloudDrive"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_notes_24,
            label = "笔记",
            route = "normalNote"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_smart_toy_24,
            label = "助手",
            route = "assistant"
        )
    ),
    "我的" to listOf(
        SubMenuItem(
            iconResId = R.drawable.baseline_person_24,
            label = "个人设置",
            route = "my"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_settings_suggest_24,
            label = "管理",
            route = "admin"
        ),
        SubMenuItem(
            iconResId = R.drawable.baseline_settings_24,
            label = "设置",
            route = "settings"
        )
    )
)

@Composable
fun MiddleFunction(isExpanded: Boolean, selectedMainMenu: String?, onSubMenuClick: (String) -> Unit) {
    val targetWidth by animateDpAsState(
        targetValue = if (!isExpanded) 226.dp else 346.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val subMenuItems = selectedMainMenu?.let { subMenuMap[it] } ?: emptyList()

    Box(
        modifier = Modifier
            .width(targetWidth)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .liquidGlass4(
                tintColor = Color.White,
                blurAlpha = 0.2f,        // 稍微提高透明度
                cornerRadius = 24.dp,
                borderAlpha = 0.4f        // 加强边框
            ),
        contentAlignment = Alignment.Center
    ) {
        if (subMenuItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                subMenuItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSubMenuClick(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.height(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(item.iconResId),
                                contentDescription = item.label,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = item.label,
                                color = Color.White,
                                fontSize = 9.sp,
                                lineHeight = 9.sp
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }
    }
}

@Composable
fun MenuItem(menuName: String, width: Dp, iconResId: Int, isSelected: Boolean = false, onClick: () -> Unit) {
    val actualBg = if (isSelected) Color(0xFF007AFF) else Color.Transparent
    val backgroundModifier = if (isSelected) {
        Modifier
            .frostedGlass(
                tintColor = Color.White,
                blurAlpha = 0.1f,
                cornerRadius = 24.dp
            )
    } else {
        Modifier
            .background(Color.Transparent)

    }
    Box(
        modifier = Modifier
            .width(width - 1.dp)
            .height(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = backgroundModifier
                .clip(RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
//                    .fillMaxHeight()
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(iconResId),
                    contentDescription = menuName,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = menuName,
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

fun Modifier.contrast(contrast: Float): Modifier {
    val colorMatrix = ColorMatrix(floatArrayOf(
        contrast, 0f,       0f,       0f, 0.5f * (1f - contrast),
        0f,       contrast, 0f,       0f, 0.5f * (1f - contrast),
        0f,       0f,       contrast, 0f, 0.5f * (1f - contrast),
        0f,       0f,       0f,       1f, 0f
    ))
    return this.drawWithContent {
        drawContent()
        // 或者使用 graphicsLayer + renderEffect
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MenuNavigation(menuItems: List<MainMenuItem>, isExpanded: Boolean, selectedMenu: String?, onExpandedChange: (Boolean) -> Unit, onMenuItemClick: (String) -> Unit) {
    val targetWidth by animateDpAsState(
        targetValue = if (isExpanded) 286.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val targetHeight by animateDpAsState(
        targetValue = if (isExpanded) 54.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val itemCount = menuItems.size
    val expandedWidth = targetWidth / itemCount

    Box(
        modifier = Modifier
            .width(targetWidth)
            .height(targetHeight)
            .clip(
                if (isExpanded) RoundedCornerShape(28.dp)
                else CircleShape
            )
            .liquidGlass4(
                tintColor = Color.White,
                blurAlpha = 0.2f,        // 稍微提高透明度
                cornerRadius = 24.dp,
                borderAlpha = 0.4f        // 加强边框
            )
            .then(
                if (!isExpanded) Modifier.clickable { onExpandedChange(true) }
                else Modifier
            )
    ) {
        if (isExpanded) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    menuItems.forEach { item ->
                        MenuItem(
                            menuName = item.label,
                            isSelected = item.label == selectedMenu,
                            onClick = {
                                onExpandedChange(false)
                                onMenuItemClick(item.label)
                            },
                            width = expandedWidth,
                            iconResId = item.iconResId
                        )
                    }
                }
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.baseline_menu_24),
                contentDescription = "展开菜单",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuNavigationPreview() {
    MenuNavigation(
        menuItems = mainMenuItems,
        isExpanded = true,
        selectedMenu = "图片",
        onExpandedChange = {},
        onMenuItemClick = {}
    )
}

@Composable
fun SearchNavigation(isExpanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSearchClick: (String) -> Unit) {
    val targetWidth by animateDpAsState(
        targetValue = if (isExpanded) 282.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val targetHeight by animateDpAsState(
        targetValue = if (isExpanded) 54.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    var searchText by remember { mutableStateOf("")}

    Box(
        modifier = Modifier
            .width(targetWidth)
            .height(targetHeight)
                .clip(
                    if (isExpanded) RoundedCornerShape(28.dp)
                    else CircleShape
                )
            .liquidGlass4(
                tintColor = Color.White,
                blurAlpha = 0.2f,        // 稍微提高透明度
                cornerRadius = 24.dp,
                borderAlpha = 0.4f        // 加强边框
            )
                .clickable { if (!isExpanded) onExpandedChange(true) }
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            if (searchText.isNotEmpty()) {
                                onSearchClick(searchText)
                                searchText = ""
                                onExpandedChange(false)
                            }
                        }
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_search_24),
                            tint = MaterialTheme.colorScheme.background,
                            contentDescription = "Search"
                        )
                    }
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.baseline_search_24),
                tint = MaterialTheme.colorScheme.background,
                contentDescription = "Search",
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BottomNavigation(
    expanded: BottomNavExpanded,
    onExpandedChange: (BottomNavExpanded) -> Unit,
    onMenuClick: (String) -> Unit,
    selectedMainMenu: String?,
    onSubMenuClick: (String) -> Unit,
    onSearchClick: (String) -> Unit
) {
    val menuItems = mainMenuItems

    val baseHeight = 84.dp
    val expandedHeight = 54.dp
    val middleHeight = 48.dp
    val spacing = 12.dp

    val bottomPadding by animateDpAsState(
        targetValue = when (expanded) {
            BottomNavExpanded.None -> 32.dp
            else -> 25.dp
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val totalHeight by animateDpAsState(
        targetValue = when (expanded) {
            BottomNavExpanded.Menu, BottomNavExpanded.Search -> 4.dp + middleHeight + spacing + expandedHeight + bottomPadding
            BottomNavExpanded.None -> baseHeight
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    val leftRightPadding by animateDpAsState(
        targetValue = when (expanded) {
            BottomNavExpanded.None -> 28.dp
            else -> 25.dp
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(452.dp)
                .height(totalHeight)
                .background(Color.Transparent)
                .padding(
                    start = leftRightPadding,
                    end = leftRightPadding,
                    top = 4.dp,
                    bottom = bottomPadding
                )
        ) {
            if (expanded == BottomNavExpanded.None) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MenuNavigation(
                        menuItems = menuItems,
                        isExpanded = false,
                        selectedMenu = selectedMainMenu,
                        onExpandedChange = {
                            onExpandedChange(BottomNavExpanded.Menu)
                        },
                        onMenuItemClick = onMenuClick
                    )

                    MiddleFunction(
                        isExpanded = false,
                        selectedMainMenu = selectedMainMenu,
                        onSubMenuClick = onSubMenuClick
                    )

                    SearchNavigation(
                        isExpanded = false,
                        onExpandedChange ={
                            onExpandedChange(BottomNavExpanded.Search)
                        },
                        onSearchClick = onSearchClick
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(middleHeight),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        MiddleFunction(
                            isExpanded = true,
                            selectedMainMenu = selectedMainMenu,
                            onSubMenuClick = onSubMenuClick
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(expandedHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MenuNavigation(
                            menuItems = menuItems,
                            isExpanded = expanded == BottomNavExpanded.Menu,
                            selectedMenu = selectedMainMenu,
                            onExpandedChange = {
                                onExpandedChange(if (it) BottomNavExpanded.Menu else BottomNavExpanded.None)
                            },
                            onMenuItemClick = onMenuClick
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        SearchNavigation(
                            isExpanded = expanded == BottomNavExpanded.Search,
                            onExpandedChange = {
                                onExpandedChange(if (it) BottomNavExpanded.Search else BottomNavExpanded.None)
                            },
                            onSearchClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun testShow() {
    Box(
        Modifier
            .fillMaxHeight()
            .clickable {}
    )
}