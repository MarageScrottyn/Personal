package com.android.aggregationspace.ui.layout

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.aggregationspace.ui.navigation.BottomNavExpanded
import com.android.aggregationspace.ui.navigation.BottomNavigation
import com.android.aggregationspace.ui.pages.picture.PicturePage
import com.android.aggregationspace.ui.pages.picture.FullscreenImageViewer
import com.android.aggregationspace.ui.pages.comic.ComicPage
import com.android.aggregationspace.ui.pages.comic.ComicDetails
import com.android.aggregationspace.ui.pages.comic.ComicReader
import com.android.aggregationspace.ui.album.AlbumPage
import com.android.aggregationspace.ui.album.AlbumViewerPage
import com.android.aggregationspace.ui.pages.login.LoginPage
import com.android.aggregationspace.ui.pages.personal.MyPage
import com.android.aggregationspace.ui.pages.manage.AdminPage
import com.android.aggregationspace.ui.pages.manage.UploadPage
import com.android.aggregationspace.ui.pages.cloud.CloudPage
import com.android.aggregationspace.ui.pages.notes.NotePage
import com.android.aggregationspace.ui.pages.assistant.AssistantPage
import com.android.aggregationspace.ui.pages.search.SearchPage
import com.android.aggregationspace.ui.pages.settings.SettingPage
import com.android.aggregationspace.ui.pages.vedio.VideoPage
import com.android.aggregationspace.ui.pages.vedio.VideoPlayer
import com.android.aggregationspace.ui.pages.vedio.MoviePage
import com.android.aggregationspace.ui.pages.vedio.TvPage
import com.android.aggregationspace.ui.pages.vedio.ShortVideoPage
import com.android.aggregationspace.ui.pages.specialalbum.SpecialAlbumPage
import com.android.aggregationspace.ui.pages.specialalbum.SpecialAlbumPlayer
import com.android.aggregationspace.ui.navigation.DynamicTopBar
import com.android.aggregationspace.ui.navigation.TopBarAction
import com.android.aggregationspace.api.UserManager
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Collections.emptyList

data class ScreenConfig(
    val showBottomNav: Boolean = true,
    val showTopBar: Boolean = true,
    val showBackButton: Boolean = false,
    val isMainScreen: Boolean = false
)

sealed class Screen(
    val route: String,
    val title: String,
    val icon: @Composable () -> Unit = {},
    val config: ScreenConfig = ScreenConfig()
) {
    data object Picture : Screen(
        route = "picture",
        title = "图片",
        icon = { Icon(Icons.Default.Add, contentDescription = "图片") },
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object Comic : Screen(
        route = "comic",
        title = "漫画",
        icon = { Icon(Icons.Default.Search, contentDescription = "漫画") },
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object Video : Screen(
        route = "video",
        title = "视频",
        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "视频") },
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object ComicDetails : Screen(
        route = "comicDetails/{comicId}",
        title = "漫画详情",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = true,
            showBackButton = true,
            isMainScreen = false
        )
    )

    data object ComicReader : Screen(
        route = "comicReader/{comicId}",
        title = "漫画阅读",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = true,
            showBackButton = true,
            isMainScreen = false
        )
    )

    data object FullscreenImage : Screen(
        route = "fullscreenImage/{imageUrl}",
        title = "图片查看",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = false,
            showBackButton = false,
            isMainScreen = false
        )
    )

    data object VideoPlayer : Screen(
        route = "videoPlayer/{videoId}",
        title = "视频播放",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = false,
            showBackButton = false,
            isMainScreen = false
        )
    )

    data object SpecialAlbumPlayer : Screen(
        route = "specialPlayer/{videoId}",
        title = "特辑播放",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = false,
            showBackButton = false,
            isMainScreen = false
        )
    )

    data object PictureAlbum : Screen(
        route = "pictureAlbum",
        title = "图集",
        icon = { Icon(Icons.Default.MoreVert, contentDescription = "图集") },
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object PictureAlbumViewer : Screen(
        route = "pictureAlbumViewer/{albumId}?title={title}&images={images}",
        title = "图集查看",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = true,
            showBackButton = true,
            isMainScreen = false
        )
    )

    data object MyPage : Screen(
        route = "my",
        title = "我的",
        icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object AdminPage : Screen(
        route = "admin",
        title = "管理",
        icon = { Icon(Icons.Default.Settings, contentDescription = "管理") },
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object UploadPage : Screen(
        route = "upload?category={category}",
        title = "上传",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = true,
            showBackButton = true,
            isMainScreen = false
        )
    )

    data object CloudDrive : Screen(
        route = "cloudDrive?parentId={parentId}",
        title = "云盘",
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object NotePage : Screen(
        route = "normalNote",
        title = "笔记",
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    /** AI 助手页：资源分类入口，进入后是完整的助手聊天界面 */
    data object Assistant : Screen(
        route = "assistant",
        title = "助手",
        config = ScreenConfig(
            showBottomNav = true,
            showTopBar = true,
            showBackButton = false,
            isMainScreen = true
        )
    )

    data object Login : Screen(
        route = "login",
        title = "登录",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = false,
            showBackButton = true,
            isMainScreen = false
        )
    )

    /** 设置页：在导航栏底部菜单"我的"->"设置"中进入 */
    data object Settings : Screen(
        route = "settings",
        title = "设置",
        config = ScreenConfig(
            showBottomNav = false,
            showTopBar = true,
            showBackButton = true,
            isMainScreen = false
        )
    )
}

private val allScreens = listOf(
    Screen.Picture,
    Screen.Comic,
    Screen.Video,
    Screen.ComicDetails,
    Screen.ComicReader,
    Screen.FullscreenImage,
    Screen.VideoPlayer,
    Screen.SpecialAlbumPlayer,
    Screen.PictureAlbum,
    Screen.PictureAlbumViewer,
    Screen.MyPage,
    Screen.AdminPage,
    Screen.UploadPage,
    Screen.CloudDrive,
    Screen.NotePage,
    Screen.Assistant,
    Screen.Settings
)

private val mainScreens = allScreens.filter { it.config.isMainScreen }

private fun findScreenByRoute(route: String?): Screen? {
    if (route == null) return null
    return allScreens.find { it.route.startsWith(route.split("/").first()) }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var expanded by remember { mutableStateOf(BottomNavExpanded.None) }
    var selectedMainMenu by remember { mutableStateOf<String?>("图片") }
    var showComicReaderTopBar by remember { mutableStateOf(true) }

    // 云盘和笔记的上传触发器（顶部栏按钮触发，页面内观察并弹出对话框）
    var cloudUploadTrigger by remember { mutableStateOf(0) }
    var noteCreateTrigger by remember { mutableStateOf(0) }
    // 助手页"新对话"触发器（顶部栏按钮触发，页面内观察后清空当前会话）
    var assistantNewChatTrigger by remember { mutableStateOf(0) }
    // 管理页添加触发器（仅标签管理页响应，弹出新增标签对话框）
    var adminAddTrigger by remember { mutableStateOf(0) }
    // 管理页当前选中的标签名（用于控制顶部栏添加按钮是否显示）
    var adminCurrentTab by remember { mutableStateOf("IMAGE") }

    // 观察用户登录状态，Token 刷新失败导致用户信息被清除时自动跳转登录页
    val userInfo by UserManager.userInfo.collectAsState()
    val currentRouteForAuth = navController.currentBackStackEntryAsState().value?.destination?.route
    androidx.compose.runtime.LaunchedEffect(userInfo) {
        if (userInfo == null && currentRouteForAuth != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
            }
        }
    }

    fun getCurrentPageActions(route: String?): List<TopBarAction> {
        return when (route) {
            // 图片页：上传图片
            Screen.Picture.route -> listOf(
                TopBarAction(Icons.Default.Add, "上传图片", {
                    navController.navigate("upload?category=IMAGE")
                })
            )
            // 漫画页：上传漫画
            Screen.Comic.route -> listOf(
                TopBarAction(Icons.Default.Add, "上传漫画", {
                    navController.navigate("upload?category=COMIC")
                })
            )
            // 视频页：上传视频
            Screen.Video.route -> listOf(
                TopBarAction(Icons.Default.Add, "上传视频", {
                    navController.navigate("upload?category=VIDEO")
                })
            )
            // 图集页：上传图集
            Screen.PictureAlbum.route -> listOf(
                TopBarAction(Icons.Default.Add, "上传图集", {
                    navController.navigate("upload?category=ALBUM")
                })
            )
            // 云盘页：上传文件到当前文件夹
            Screen.CloudDrive.route -> listOf(
                TopBarAction(Icons.Default.Add, "上传文件", {
                    cloudUploadTrigger++
                })
            )
            // 笔记页：新建笔记
            Screen.NotePage.route -> listOf(
                TopBarAction(Icons.Default.Add, "新建笔记", {
                    noteCreateTrigger++
                })
            )
            // 助手页：新建对话
            Screen.Assistant.route -> listOf(
                TopBarAction(Icons.Default.Add, "新对话", {
                    assistantNewChatTrigger++
                })
            )
            Screen.MyPage.route -> emptyList()
            // 管理页：仅在"标签管理"标签下显示添加按钮（其他分类的上传表单已内嵌在分页中）
            Screen.AdminPage.route -> if (adminCurrentTab == "TAG_MANAGE") listOf(
                TopBarAction(Icons.Default.Add, "添加标签", {
                    adminAddTrigger++
                })
            ) else emptyList()
            else -> emptyList()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = findScreenByRoute(currentRoute) ?: Screen.Picture

    if (currentScreen !is Screen.ComicReader) {
        showComicReaderTopBar = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 使用透明背景，让 MainActivity 中 AppBackground 设置的背景色/图可见
            .background(Color.Transparent)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Picture.route,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = expanded != BottomNavExpanded.None,
                    onClick = { expanded = BottomNavExpanded.None }
                )
        ) {
            composable(Screen.Picture.route) { PicturePage(navController) }
            composable(Screen.Comic.route) { ComicPage(navController) }

            composable(Screen.Video.route) { VideoPage(navController) }

            composable("videoPlayer/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                VideoPlayer(navController, videoId)
            }

            composable("specialPlayer/{videoId}") { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                SpecialAlbumPlayer(navController, videoId.toInt())
            }

            composable("movie") { MoviePage(navController) }

            composable("tv") { TvPage(navController) }

            composable("shortVideo") { ShortVideoPage(navController) }
            composable("specialEdit") { SpecialAlbumPage(navController) }

            composable(Screen.ComicDetails.route) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getString("comicId") ?: ""
                ComicDetails(navController, comicId)
            }

            composable(Screen.ComicReader.route) { backStackEntry ->
                val comicId = backStackEntry.arguments?.getString("comicId") ?: ""
                ComicReader(navController, comicId) { showTopBar ->
                    showComicReaderTopBar = showTopBar
                }
            }

            composable(Screen.FullscreenImage.route) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                val imageUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8)
                FullscreenImageViewer(navController, imageUrl)
            }

            composable(Screen.PictureAlbum.route) { AlbumPage(navController) }

            composable(Screen.PictureAlbumViewer.route) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                val encodedImages = backStackEntry.arguments?.getString("images") ?: ""
                val title = if (encodedTitle.isNotEmpty()) URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8) else ""
                val imagesJson = if (encodedImages.isNotEmpty()) URLDecoder.decode(encodedImages, StandardCharsets.UTF_8) else ""
                AlbumViewerPage(navController, albumId, title, imagesJson)
            }

            composable(Screen.MyPage.route) { MyPage(navController) }

            composable(Screen.Login.route) { LoginPage(navController) }

            composable(Screen.AdminPage.route) {
                AdminPage(
                    navController = navController,
                    addTrigger = adminAddTrigger,
                    onTabChanged = { tabName -> adminCurrentTab = tabName }
                )
            }

            // 云盘页：支持 parentId 参数进入子文件夹
            composable(
                Screen.CloudDrive.route,
                arguments = listOf(navArgument("parentId") { defaultValue = "" })
            ) { backStackEntry ->
                val parentId = backStackEntry.arguments?.getString("parentId") ?: ""
                CloudPage(navController, parentId, cloudUploadTrigger)
            }

            composable("normalNote") { NotePage(navController, noteCreateTrigger) }

            // AI 助手聊天页（顶部栏"新对话"通过 trigger 通知页面）
            composable(Screen.Assistant.route) {
                AssistantPage(navController, assistantNewChatTrigger)
            }

            // 上传页：支持通过 category 参数预选上传类型
            composable(
                Screen.UploadPage.route,
                arguments = listOf(navArgument("category") { defaultValue = "IMAGE" })
            ) { backStackEntry ->
                val categoryStr = backStackEntry.arguments?.getString("category") ?: "IMAGE"
                // 将字符串转换为 UploadCategory 枚举，转换失败时默认为图片
                val category = try {
                    com.android.aggregationspace.ui.pages.manage.UploadCategory.valueOf(categoryStr)
                } catch (e: Exception) {
                    com.android.aggregationspace.ui.pages.manage.UploadCategory.IMAGE
                }
                UploadPage(navController, category)
            }

            composable("about") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "关于", color = Color.White)
                }
            }

            composable(
                "search?query={query}",
                arguments = listOf(navArgument("query") { defaultValue = "" })
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                SearchPage(navController, query)
            }

            composable("settings") { SettingPage(navController) }
        }

        val shouldShowTopBar = currentScreen?.config?.showTopBar == true && 
            !(currentScreen is Screen.ComicReader && !showComicReaderTopBar)
        if (shouldShowTopBar) {
            DynamicTopBar(
                showBackButton = currentScreen.config.showBackButton || navController.previousBackStackEntry != null,
                onBackClick = { navController.popBackStack() },
                actions = getCurrentPageActions(currentRoute),
                title = currentScreen.title
            )
        }

        if (currentScreen?.config?.showBottomNav == true) {
            BottomNavigation(
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                },
                onMenuClick = { menuName ->
                    selectedMainMenu = menuName
                    when (menuName) {
                        "资源" -> navController.navigate("cloudDrive?parentId=")
                        else -> {
                            val screen = mainScreens.find { it.title == menuName }
                            if (screen != null) {
                                navController.navigate(screen.route)
                            }
                        }
                    }
                },
                selectedMainMenu = selectedMainMenu,
                onSubMenuClick = { route ->
                    navController.navigate(route)
                    expanded = BottomNavExpanded.None
                },
                onSearchClick = { query ->
                    navController.navigate("search?query=$query")
                }
            )
        }
    }
}