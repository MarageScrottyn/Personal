# Jetpack Compose 从零开始学习教程

> 本教程基于 `AggregationSpace` 项目编写，所有代码示例仅作参考展示，**请勿直接写入项目代码**。

***

## 第一章：Android 项目结构解析

### 1.1 项目整体结构

```
AggregationSpace/                    # 项目根目录
├── app/                             # 主应用模块
│   ├── src/
│   │   ├── main/                    # 主源码目录
│   │   │   ├── java/                # Java/Kotlin 源代码
│   │   │   │   └── com/android/aggregationspace/
│   │   │   │       ├── MainActivity.kt       # 主活动（入口）
│   │   │   │       └── ui/
│   │   │   │           └── theme/            # 主题配置
│   │   │   │               ├── Color.kt      # 颜色定义
│   │   │   │               ├── Theme.kt      # 主题样式
│   │   │   │               └── Type.kt       # 字体样式
│   │   │   └── res/                 # 资源文件
│   │   │       ├── drawable/        # 图片、矢量图
│   │   │       ├── mipmap-*/        # 应用图标（不同分辨率）
│   │   │       └── values/          # 配置文件（颜色、字符串、主题）
│   │   ├── test/                    # 单元测试
│   │   └── androidTest/             # 集成测试
│   ├── build.gradle.kts             # 模块构建配置
│   └── proguard-rules.pro           # 混淆规则
├── gradle/
│   └── wrapper/                     # Gradle 包装器
│       ├── gradle-wrapper.jar       # Gradle 执行文件
│       └── gradle-wrapper.properties # Gradle 版本配置
│   └── libs.versions.toml           # 依赖版本管理
├── build.gradle.kts                 # 项目级构建配置
├── settings.gradle.kts              # 项目设置（模块声明）
└── gradlew.bat                      # Windows Gradle 启动脚本
```

### 1.2 关键文件详解

#### 1.2.1 settings.gradle.kts

```kotlin
// 插件管理
pluginManagement {
    repositories {
        google { /* Google Maven 仓库 */ }
        mavenCentral() /* 中央仓库 */
        gradlePluginPortal() /* Gradle 插件仓库 */
    }
}

// 依赖管理
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Aggregation Space"
include(":app")  // 声明包含 app 模块
```

**作用**：配置项目的仓库来源和模块结构。

#### 1.2.2 build.gradle.kts (项目级)

```kotlin
plugins {
    // 声明可用插件（apply false 表示不在根项目应用）
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

**作用**：定义项目级插件，供子模块使用。

#### 1.2.3 app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)  // Android 应用插件
    alias(libs.plugins.kotlin.android)       // Kotlin Android 插件
    alias(libs.plugins.kotlin.compose)       // Kotlin Compose 插件
}

android {
    namespace = "com.android.aggregationspace"  // 包名
    compileSdk = 35                             // 编译 SDK 版本
    
    defaultConfig {
        applicationId = "com.android.aggregationspace"  // 应用 ID
        minSdk = 35                                      // 最低支持 SDK
        targetSdk = 35                                   // 目标 SDK
        versionCode = 1                                  // 版本号（整数）
        versionName = "1.0"                              // 版本名称
    }
    
    buildFeatures {
        compose = true  // 启用 Compose 功能
    }
}

dependencies {
    // 核心依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose 依赖（通过 BOM 管理版本）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)  // Material Design 3
}
```

**作用**：配置应用模块的编译选项和依赖关系。

#### 1.2.4 gradle/libs.versions.toml

```toml
[versions]
agp = "8.10.1"              # Android Gradle 插件版本
kotlin = "2.0.21"           # Kotlin 版本
composeBom = "2024.09.00"   # Compose BOM 版本

[libraries]
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**作用**：集中管理所有依赖版本，便于统一升级。

***

## 第二章：Kotlin 基础语法

### 2.1 变量声明

```kotlin
// 不可变变量（val = value）
val name: String = "Tom"
val age: Int = 25
val isStudent = true  // 类型自动推断

// 可变变量（var = variable）
var score: Double = 95.5
score = 88.0  // 可以重新赋值
```

### 2.2 函数声明

```kotlin
// 基本函数
fun greet(name: String): String {
    return "Hello, $name!"
}

// 单行函数（表达式体）
fun add(a: Int, b: Int) = a + b

// 无返回值函数
fun printMessage(message: String) {
    println(message)
}

// 默认参数
fun introduce(name: String, age: Int = 18) {
    println("我叫 $name，今年 $age 岁")
}
```

### 2.3 空安全

```kotlin
// 可空类型（类型后加 ?）
var nullableName: String? = null

// 安全调用（?.）
val length = nullableName?.length  // 如果为 null，返回 null

// 空合并运算符（?:）
val safeName = nullableName ?: "Unknown"

// 非空断言（!!）- 谨慎使用
val upperName = nullableName!!.toUpperCase()  // 如果为 null 会抛出异常
```

### 2.4 数据类

```kotlin
data class User(
    val id: Int,
    val name: String,
    val email: String
)

// 自动生成：equals(), hashCode(), toString(), copy()
val user = User(1, "Alice", "alice@example.com")
val userCopy = user.copy(name = "Bob")
```

### 2.5 Lambda 表达式

```kotlin
// Lambda 基本语法
val sum: (Int, Int) -> Int = { a, b -> a + b }

// 简化调用
val result = sum(3, 5)  // 结果：8

// 常用高阶函数
val numbers = listOf(1, 2, 3, 4, 5)
val doubled = numbers.map { it * 2 }  // [2, 4, 6, 8, 10]
val evenNumbers = numbers.filter { it % 2 == 0 }  // [2, 4]
```

### 2.6 扩展函数

```kotlin
// 为 String 添加扩展函数
fun String.isEmail(): Boolean {
    return this.contains("@") && this.contains(".")
}

// 使用
val isValid = "test@example.com".isEmail()  // true
```

***

## 第三章：Jetpack Compose 核心概念

### 3.1 什么是 Jetpack Compose

Jetpack Compose 是 Android 官方的现代化声明式 UI 框架，使用 Kotlin 编写。

**传统 View 系统 vs Compose**：

| 特性      | 传统 View            | Compose   |
| ------- | ------------------ | --------- |
| UI 构建方式 | 命令式                | 声明式       |
| 布局定义    | XML 文件             | Kotlin 代码 |
| 更新方式    | findView + setText | 状态驱动      |
| 代码量     | 多                  | 少         |

### 3.2 @Composable 注解

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.runtime.Composable

@Composable
fun Greeting(name: String) {
    Text(text = "Hello, $name!")
}

// Composable 函数可以嵌套调用
@Composable
fun App() {
    Greeting("Android")
}
```

**关键点**：

- 使用 `@Composable` 注解标记
- 可以接收参数
- 不能有返回值（返回类型必须是 Unit）
- 只能在其他 Composable 函数中调用

### 3.3 Modifier（修饰符）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StyledText() {
    Text(
        text = "样式文本",
        modifier = Modifier
            .padding(16.dp)           // 内边距
            .size(100.dp)            // 尺寸
            .background(Color.Blue)  // 背景色
    )
}
```

**常用 Modifier**：

- `padding()` - 内边距
- `size()` - 宽高
- `fillMaxWidth()` - 填充父容器宽度
- `fillMaxHeight()` - 填充父容器高度
- `fillMaxSize()` - 填充父容器尺寸
- `background()` - 背景色
- `clickable()` - 点击事件
- `align()` - 对齐方式

### 3.4 Preview（预览）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("World")
}
```

**作用**：在 Android Studio 中实时预览 UI 效果，无需运行应用。

### 3.5 主题系统

项目中的主题配置位于 `ui/theme/` 目录：

```
ui/theme/
├── Color.kt      # 颜色定义
├── Theme.kt      # 主题样式
└── Type.kt       # 字体样式
```

#### Color.kt

```kotlin
// 项目实际代码示例
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

#### Theme.kt

```kotlin
// 项目实际代码示例
@Composable
fun AggregationSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

***

## 第四章：基础 UI 组件

### 4.1 Text（文本）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun TextExamples() {
    // 基础文本
    Text(text = "基础文本")
    
    // 带样式的文本
    Text(
        text = "带样式文本",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Blue,
        textAlign = TextAlign.Center
    )
}
```

**常用属性**：

- `text` - 文本内容
- `fontSize` - 字体大小
- `fontWeight` - 字重（Bold、Normal 等）
- `color` - 字体颜色
- `textAlign` - 对齐方式
- `maxLines` - 最大行数
- `lineHeight` - 行高

### 4.2 Button（按钮）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text

@Composable
fun ButtonExamples() {
    // 基础按钮
    Button(onClick = { /* 点击事件 */ }) {
        Text("点击我")
    }
    
    // 轮廓按钮
    OutlinedButton(onClick = { }) {
        Text("轮廓按钮")
    }
    
    // 禁用按钮
    Button(onClick = { }, enabled = false) {
        Text("禁用按钮")
    }
}
```

**常用属性**：

- `onClick` - 点击回调
- `enabled` - 是否启用
- `colors` - 颜色配置
- `shape` - 形状

### 4.3 TextField（输入框）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Text

@Composable
fun TextFieldExample() {
    val textState = remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = textState.value,
        onValueChange = { textState.value = it },
        label = { Text("请输入姓名") },
        placeholder = { Text("请输入...") },
        singleLine = true
    )
}
```

**常用属性**：

- `value` - 当前值
- `onValueChange` - 值变化回调
- `label` - 标签
- `placeholder` - 占位符
- `singleLine` - 是否单行
- `maxLines` - 最大行数
- `enabled` - 是否启用

### 4.4 Image（图片）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ImageExamples() {
    // 从资源文件加载图片
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = "应用图标",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(100.dp)
    )
}
```

**常用属性**：

- `painter` - 图片资源
- `contentDescription` - 无障碍描述（必需）
- `contentScale` - 缩放模式（Crop、Fit、FillBounds 等）

***

## 第五章：布局组件

### 5.1 Column（垂直布局）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

@Composable
fun ColumnExample() {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("第一行")
        Text("第二行")
        Text("第三行")
    }
}
```

**常用属性**：

- `horizontalAlignment` - 水平对齐（Start、Center、End）
- `verticalArrangement` - 垂直排列方式（Top、Center、Bottom、SpaceBetween 等）

### 5.2 Row（水平布局）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

@Composable
fun RowExample() {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("左侧文本")
        Text("右侧文本")
    }
}
```

**常用属性**：

- `verticalAlignment` - 垂直对齐（Top、Center、Bottom）
- `horizontalArrangement` - 水平排列方式

### 5.3 Box（层叠布局）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment

@Composable
fun BoxExample() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景
        Text(
            text = "背景文本",
            modifier = Modifier.align(Alignment.Center)
        )
        // 前景
        Text(
            text = "前景文本",
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
```

**常用属性**：

- `contentAlignment` - 内容对齐方式

### 5.4 Scaffold（脚手架布局）

```kotlin
// 项目实际代码示例（MainActivity.kt）

import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AggregationSpaceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

**作用**：提供标准的 Material Design 布局结构，包含：

- `topBar` - 顶部导航栏
- `bottomBar` - 底部导航栏
- `floatingActionButton` - 浮动按钮
- `content` - 主内容区域

### 5.5 布局结构与组件嵌套详解

#### 5.5.1 组件树的概念

在 Compose 中，UI 是由组件嵌套组成的**树状结构**。就像 HTML 中的 DOM 树一样，每个组件都可以包含子组件。

**视觉结构 vs 代码结构**：

```
┌─────────────────────────────────┐
│        Screen (屏幕)            │
│  ┌─────────────────────────┐   │
│  │     topBar (顶部栏)     │   │
│  └─────────────────────────┘   │
│  ┌─────────────────────────┐   │
│  │      content (内容)     │   │
│  │  ┌───────────────────┐  │   │
│  │  │   Column 垂直布局  │  │   │
│  │  │  ┌─────────────┐  │  │   │
│  │  │  │   Text      │  │  │   │
│  │  │  │   Button    │  │  │   │
│  │  │  │   Image     │  │  │   │
│  │  │  └─────────────┘  │  │   │
│  │  └───────────────────┘  │   │
│  └─────────────────────────┘   │
│  ┌─────────────────────────┐   │
│  │    bottomBar (底部栏)   │   │
│  │  ┌──────┬──────┬──────┐│   │
│  │  │ Tab1 │ Tab2 │ Tab3 ││   │
│  │  └──────┴──────┴──────┘│   │
│  └─────────────────────────┘   │
└─────────────────────────────────┘
```

**对应的代码结构**（缩进表示嵌套层级）：

```kotlin
Scaffold(                    // 最外层：脚手架布局
    topBar = {              // ├─ 顶部栏
        TopAppBar(...)      // │   └─ 顶部导航栏组件
    },
    bottomBar = {           // ├─ 底部栏
        NavigationBar(...)  // │   └─ 导航栏组件
    },
    content = { padding ->  // └─ 内容区域
        Column(...) {       //     └─ 垂直布局
            Text(...)       //         ├─ 文本
            Button(...)     //         ├─ 按钮
            Image(...)      //         └─ 图片
        }
    }
)
```

#### 5.5.2 完整示例：带底部导航栏的界面

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen() {
    // 当前选中的导航项
    var selectedItem by remember { mutableStateOf(0) }
    
    // 导航项列表
    val navItems = listOf("首页", "发现", "我的")
    
    Scaffold(
        // 顶部导航栏
        topBar = {
            TopAppBar(title = { Text("聚合空间") })
        },
        
        // 底部导航栏
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, label ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = label
                            )
                        },
                        label = { Text(label) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        },
        
        // 主内容区域
        content = { innerPadding ->
            // 使用 Column 垂直布局
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)  // 避开系统 UI 和底部导航栏
            ) {
                // 内容区域可以是任何组件
                when (selectedItem) {
                    0 -> HomeContent()
                    1 -> DiscoverContent()
                    2 -> ProfileContent()
                }
            }
        }
    )
}

// 首页内容
@Composable
fun HomeContent() {
    Column {
        Text(text = "首页内容", modifier = Modifier.padding(16.dp))
        Text(text = "这是首页的主要内容区域")
    }
}

// 发现页内容
@Composable
fun DiscoverContent() {
    Column {
        Text(text = "发现内容", modifier = Modifier.padding(16.dp))
        Text(text = "这里可以展示推荐内容")
    }
}

// 个人中心内容
@Composable
fun ProfileContent() {
    Column {
        Text(text = "个人中心", modifier = Modifier.padding(16.dp))
        Text(text = "用户信息和设置")
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AggregationSpaceTheme {
        HomeScreen()
    }
}
```

#### 5.5.3 代码结构逐层解析

**第一层：Scaffold**

```kotlin
Scaffold(
    topBar = { ... },
    bottomBar = { ... },
    content = { padding -> ... }
)
```

- **作用**：提供整体布局框架
- **topBar**：顶部导航栏区域
- **bottomBar**：底部导航栏区域
- **content**：主内容区域（会自动避开 topBar 和 bottomBar）

**第二层：NavigationBar（底部导航栏）**

```kotlin
NavigationBar {
    NavigationBarItem(
        icon = { Icon(...) },
        label = { Text("首页") },
        selected = selectedItem == index,
        onClick = { selectedItem = index }
    )
}
```

- **NavigationBar**：Material Design 的底部导航栏容器
- **NavigationBarItem**：单个导航项，包含图标和文字
- **selected**：当前是否选中
- **onClick**：点击切换选中状态

**第三层：内容区域**

```kotlin
content = { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding)) {
        // 实际内容
    }
}
```

- **innerPadding**：Scaffold 自动计算的内边距，确保内容不被顶部栏和底部栏遮挡
- **Column**：垂直布局容器，用于组织内容
- **when (selectedItem)**：根据选中的导航项显示不同内容

**第四层：具体内容组件**

```kotlin
@Composable
fun HomeContent() {
    Column {
        Text(text = "首页内容")
        // 更多组件...
    }
}
```

- 将不同页面的内容封装为独立的 Composable 函数
- 便于维护和复用

#### 5.5.4 布局嵌套的关键原则

1. **缩进表示层级**：代码的缩进深度对应 UI 的嵌套深度
2. **容器在前，内容在后**：先定义布局容器（Column、Row、Box），再在其中放置内容组件
3. **Modifier 链**：通过 `.padding()`, `.fillMaxSize()` 等修饰符控制布局
4. **状态驱动**：使用 `mutableStateOf` 管理选中状态，状态变化自动更新 UI
5. **单一职责**：每个 Composable 函数只负责一个功能

#### 5.5.5 常见布局模式

| 模式      | 适用场景   | 代码结构                                       |
| ------- | ------ | ------------------------------------------ |
| **列表页** | 大量数据展示 | `Scaffold` → `LazyColumn` → `Item`         |
| **详情页** | 单条数据展示 | `Scaffold` → `Column` → `Text/Image`       |
| **表单页** | 用户输入   | `Scaffold` → `Column` → `TextField/Button` |
| **导航页** | 多页面切换  | `Scaffold` → `bottomBar` + `NavHost`       |

#### 5.5.6 Modifier 完整属性详解

`Modifier` 是 Compose 中用于修改组件布局和样式的核心工具。以下是常用属性的完整列表：

##### 布局尺寸类

| 属性                | 类型              | 说明               | 使用示例                                    |
| ----------------- | --------------- | ---------------- | --------------------------------------- |
| `fillMaxWidth()`  | `Float = 1f`    | 填充父容器宽度          | `.fillMaxWidth()`                       |
| `fillMaxHeight()` | `Float = 1f`    | 填充父容器高度          | `.fillMaxHeight()`                      |
| `fillMaxSize()`   | `Float = 1f`    | 填充父容器宽高          | `.fillMaxSize()`                        |
| `width()`         | `Dp`            | 设置固定宽度           | `.width(100.dp)`                        |
| `height()`        | `Dp`            | 设置固定高度           | `.height(50.dp)`                        |
| `size()`          | `Dp` / `Dp, Dp` | 设置宽高             | `.size(50.dp)` / `.size(100.dp, 50.dp)` |
| `weight()`        | `Float`         | 权重分配（Row/Column） | `.weight(1f)`                           |

##### 内边距类

| 属性          | 类型               | 说明                  | 使用示例                                 |
| ----------- | ---------------- | ------------------- | ------------------------------------ |
| `padding()` | `Dp`             | 四边等距内边距             | `.padding(16.dp)`                    |
| `padding()` | `Dp, Dp`         | 水平/垂直内边距            | `.padding(16.dp, 8.dp)`              |
| `padding()` | `Dp, Dp, Dp, Dp` | 上/下/左/右内边距          | `.padding(8.dp, 16.dp, 8.dp, 16.dp)` |
| `padding()` | `PaddingValues`  | 使用 PaddingValues 对象 | `.padding(PaddingValues(16.dp))`     |

##### 背景与形状类

| 属性             | 类型             | 说明        | 使用示例                                               |
| -------------- | -------------- | --------- | -------------------------------------------------- |
| `background()` | `Color`        | 设置背景颜色    | `.background(Color.Red)`                           |
| `background()` | `Color, Shape` | 设置背景颜色和形状 | `.background(Color.Red, RoundedCornerShape(8.dp))` |
| `clip()`       | `Shape`        | 裁剪为指定形状   | `.clip(RoundedCornerShape(8.dp))`                  |

##### 对齐类

| 属性                    | 类型          | 说明      | 使用示例                                   |
| --------------------- | ----------- | ------- | -------------------------------------- |
| `align()`             | `Alignment` | 子组件对齐方式 | `.align(Alignment.Center)`             |
| `wrapContentWidth()`  | `Alignment` | 宽度包裹内容  | `.wrapContentWidth(Alignment.Center)`  |
| `wrapContentHeight()` | `Alignment` | 高度包裹内容  | `.wrapContentHeight(Alignment.Center)` |

##### 交互类

| 属性            | 类型           | 说明        | 使用示例                           |
| ------------- | ------------ | --------- | ------------------------------ |
| `clickable()` | `() -> Unit` | 添加点击事件    | `.clickable { doSomething() }` |
| `padding()`   | `Dp`         | 添加点击区域内边距 | `.clickable {}.padding(16.dp)` |

##### 偏移类

| 属性         | 类型       | 说明    | 使用示例                            |
| ---------- | -------- | ----- | ------------------------------- |
| `offset()` | `Dp, Dp` | 设置偏移量 | `.offset(x = 10.dp, y = 10.dp)` |

##### 其他常用

| 属性             | 类型                 | 说明         | 使用示例                                                  |
| -------------- | ------------------ | ---------- | ----------------------------------------------------- |
| `border()`     | `Dp, Color`        | 添加边框       | `.border(2.dp, Color.Gray)`                           |
| `border()`     | `Dp, Color, Shape` | 添加边框和形状    | `.border(2.dp, Color.Gray, RoundedCornerShape(8.dp))` |
| `alpha()`      | `Float`            | 设置透明度（0-1） | `.alpha(0.5f)`                                        |
| `scrollable()` | `ScrollState`      | 添加滚动       | `.scrollable(scrollState)`                            |

##### 形状常量

| 形状                                   | 说明      | 使用示例                                                |
| ------------------------------------ | ------- | --------------------------------------------------- |
| `CircleShape`                        | 圆形      | `.clip(CircleShape)`                                |
| `RoundedCornerShape(Dp)`             | 圆角矩形    | `.clip(RoundedCornerShape(8.dp))`                   |
| `RoundedCornerShape(Dp, Dp, Dp, Dp)` | 四个角不同圆角 | `.clip(RoundedCornerShape(8.dp, 0.dp, 8.dp, 0.dp))` |

##### ⚠️ 重要注意事项：修饰符顺序

**修饰符的顺序决定绘制顺序，错误的顺序会导致意外效果！**

```kotlin
// 仅作参考示例，请勿直接写入项目

// ❌ 错误：背景会超出圆角
modifier = Modifier
    .background(Color.Blue)
    .clip(RoundedCornerShape(12.dp))

// ✅ 正确：先裁剪再填充背景
modifier = Modifier
    .clip(RoundedCornerShape(12.dp))
    .background(Color.Blue)
```

**推荐顺序**：

```
尺寸 → 约束 → 形状 → 背景 → 边框 → 内边距 → 内容对齐 → 交互
     ↓         ↓        ↓        ↓        ↓        ↓          ↓
fillMaxSize → clip → background → border → padding → align → clickable
```

##### 完整示例

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ModifierExample() {
    Box(
        modifier = Modifier
            .size(150.dp)                    // 1. 设置尺寸
            .clip(RoundedCornerShape(12.dp)) // 2. 裁剪圆角
            .background(Color.Blue)          // 3. 设置背景
            .clickable {                    // 4. 添加点击事件
                println("Clicked!")
            }
    ) {
        Text(
            text = "Hello",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)    // 子组件对齐
                .padding(16.dp)             // 内边距
        )
    }
}
```

### 5.6 LazyColumn / LazyRow（懒加载列表）

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text

@Composable
fun LazyListExample() {
    val items = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
    
    LazyColumn {
        items(items) { item ->
            Text(text = item, modifier = Modifier.padding(16.dp))
        }
    }
}
```

**作用**：只渲染可见的列表项，适用于大数据量列表。

***

## 第六章：状态管理

### 6.1 什么是状态

状态是决定 UI 如何显示的数据。在 Compose 中，状态变化会自动触发 UI 重组。

### 6.2 remember 和 mutableStateOf

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column

@Composable
fun StateExample() {
    // remember：在重组时保持状态
    // mutableStateOf：创建可观察的状态
    val count = remember { mutableStateOf(0) }
    
    Column {
        Text(text = "计数: ${count.value}")
        Button(onClick = { count.value++ }) {
            Text("增加")
        }
    }
}
```

**关键点**：

- `remember` 确保状态在重组时不丢失
- `mutableStateOf` 创建可观察状态，修改时自动触发重组

### 6.3 状态提升（State Hoisting）

```kotlin
// 仅作参考示例，请勿直接写入项目

@Composable
fun CounterScreen() {
    val count = remember { mutableStateOf(0) }
    
    Column {
        // 将状态传递给子组件
        DisplayCount(count = count.value)
        ControlButtons(
            onIncrement = { count.value++ },
            onDecrement = { count.value-- }
        )
    }
}

@Composable
fun DisplayCount(count: Int) {
    Text(text = "计数: $count")
}

@Composable
fun ControlButtons(onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row {
        Button(onClick = onDecrement) { Text("-") }
        Button(onClick = onIncrement) { Text("+") }
    }
}
```

**原则**：状态应提升到需要它的最高层级组件中。

### 6.4 状态容器模式

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class CounterState {
    var count by mutableStateOf(0)
    
    fun increment() {
        count++
    }
    
    fun decrement() {
        count--
    }
}

@Composable
fun CounterScreen() {
    val state = remember { CounterState() }
    
    Column {
        Text(text = "计数: ${state.count}")
        Button(onClick = { state.increment() }) {
            Text("增加")
        }
    }
}
```

***

## 第七章：导航

### 7.1 依赖配置

在 `app/build.gradle.kts` 中添加导航依赖：

```kotlin
// 仅作参考示例，请勿直接写入项目
implementation("androidx.navigation:navigation-compose:2.7.7")
```

### 7.2 创建导航图

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigateToDetail = { 
                navController.navigate("detail") 
            })
        }
        composable("detail") {
            DetailScreen(onNavigateBack = { 
                navController.popBackStack() 
            })
        }
    }
}

@Composable
fun HomeScreen(onNavigateToDetail: () -> Unit) {
    Button(onClick = onNavigateToDetail) {
        Text("跳转到详情页")
    }
}

@Composable
fun DetailScreen(onNavigateBack: () -> Unit) {
    Button(onClick = onNavigateBack) {
        Text("返回")
    }
}
```

### 7.3 传递参数

```kotlin
// 仅作参考示例，请勿直接写入项目

NavHost(navController = navController, startDestination = "home") {
    composable("home") {
        HomeScreen(onNavigateToDetail = { itemId ->
            navController.navigate("detail/$itemId")
        })
    }
    composable("detail/{itemId}") { backStackEntry ->
        val itemId = backStackEntry.arguments?.getString("itemId")
        DetailScreen(itemId = itemId)
    }
}
```

***

## 第八章：Material Design 3 主题

### 8.1 颜色系统

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),      // 主色调
    secondary = Color(0xFF625b71),    // 次要色调
    tertiary = Color(0xFF7D5260),     // 第三色调
    background = Color(0xFFFFFBFE),   // 背景色
    surface = Color(0xFFFFFBFE),      // 表面色
    onPrimary = Color.White,          // 主色调上的文字颜色
    onBackground = Color(0xFF1C1B1F), // 背景上的文字颜色
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    // ...
)
```

### 8.2 字体系统

```kotlin
// 项目实际代码示例（Type.kt）

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // ... 其他样式
)
```

### 8.3 应用主题

```kotlin
// 项目实际代码示例（Theme.kt）

@Composable
fun AggregationSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### 8.4 使用主题颜色

```kotlin
// 仅作参考示例，请勿直接写入项目

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun ThemedText() {
    Text(
        text = "使用主题颜色",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleLarge
    )
}
```

***

## 附录：常用资源

### 学习资源

1. **官方文档**：[developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose)
2. **Compose 教程**：[developer.android.com/courses/pathways/compose](https://developer.android.com/courses/pathways/compose)
3. **Material Design 3**：[m3.material.io](https://m3.material.io/)

### 常用组件速查

| 组件               | 用途     |
| ---------------- | ------ |
| `Text`           | 显示文本   |
| `Button`         | 按钮     |
| `OutlinedButton` | 轮廓按钮   |
| `TextField`      | 文本输入框  |
| `Image`          | 显示图片   |
| `Column`         | 垂直布局   |
| `Row`            | 水平布局   |
| `Box`            | 层叠布局   |
| `Scaffold`       | 标准布局结构 |
| `LazyColumn`     | 懒加载列表  |
| `Card`           | 卡片     |
| `Switch`         | 开关     |
| `Checkbox`       | 复选框    |
| `RadioButton`    | 单选按钮   |

### 常用 Modifier 速查

| Modifier          | 用途   |
| ----------------- | ---- |
| `padding()`       | 内边距  |
| `size()`          | 尺寸   |
| `fillMaxWidth()`  | 填充宽度 |
| `fillMaxHeight()` | 填充高度 |
| `fillMaxSize()`   | 填充尺寸 |
| `background()`    | 背景色  |
| `clickable()`     | 点击事件 |
| `align()`         | 对齐   |
| `weight()`        | 权重分配 |

***

> **学习建议**：建议按照章节顺序逐步学习，每学完一个章节后，尝试自己编写简单的示例代码来巩固理解。Compose 的核心是"声明式"思维，多练习才能掌握。

