package com.android.aggregationspace.ui.pages.assistant

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.aggregationspace.R
import com.android.aggregationspace.api.AssistantApi
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** 单条聊天消息；isError=true 时以错误样式展示并允许点击重试 */
data class ChatMessage(
    val role: String,            // user / assistant
    val content: String,
    val isError: Boolean = false
)

/** 助手页配色（与全局深色毛玻璃风格统一） */
private val AssistantBlue = Color(0xFF0A84FF)
private val AssistantBubbleBg = Color.White.copy(alpha = 0.10f)
private val UserBubbleBg = Color(0xFF0A84FF)
private val InputBarBg = Color.Black.copy(alpha = 0.28f)
private val ErrorTextColor = Color(0xFFFF9F9F)
private val SubTextColor = Color.White.copy(alpha = 0.65f)

/** 空会话时展示的快捷提问 */
private val SUGGESTIONS = listOf(
    "你好，介绍一下你自己",
    "现在几点了？",
    "帮我记住：我喜欢科幻电影"
)

private const val PREF_NAME = "AssistantChat"
private const val KEY_SID = "session_id"
private const val KEY_MESSAGES = "messages"

/**
 * AI 助手聊天页（主流助手布局）
 *
 * @param newChatTrigger 顶部栏"新对话"按钮触发计数，大于 0 时开启新会话
 */
@Composable
fun AssistantPage(navController: NavController, newChatTrigger: Int = 0) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    val listState = rememberLazyListState()

    // 会话 ID：首次进入随机生成并持久化，"新对话"时更换（对应后端不同的记忆空间）
    var sessionId by remember {
        mutableStateOf(
            prefs.getInt(KEY_SID, -1).also { saved ->
                if (saved == -1) {
                    val newSid = Random.nextInt(1000, 1_000_000)
                    prefs.edit().putInt(KEY_SID, newSid).apply()
                }
            }.let { if (it == -1) prefs.getInt(KEY_SID, 1) else it }
        )
    }

    // 消息列表：初始化时从本地恢复上次会话（跨页面/重启不丢失）
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            runCatching {
                val raw = prefs.getString(KEY_MESSAGES, null)
                if (!raw.isNullOrBlank()) {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(ChatMessage(obj.optString("role"), obj.optString("content")))
                    }
                }
            }
        }
    }

    var input by remember { mutableStateOf("") }            // 输入框文本
    var isGenerating by remember { mutableStateOf(false) }  // 是否正在等待/接收回复
    var activeCall by remember { mutableStateOf<Call?>(null) }
    var manualStopped by remember { mutableStateOf(false) } // 用户是否手动点了停止

    /** 将当前消息快照写入本地存储 */
    fun persist() {
        val arr = JSONArray()
        messages.forEach { msg ->
            arr.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }
        prefs.edit().putString(KEY_MESSAGES, arr.toString()).apply()
    }

    /** 发送一条消息并接收流式回复（idx 为助手占位消息的下标） */
    fun send(text0: String) {
        val text = text0.trim()
        if (text.isEmpty() || isGenerating) return
        manualStopped = false

        messages.add(ChatMessage("user", text))
        messages.add(ChatMessage("assistant", ""))
        persist()
        input = ""
        isGenerating = true
        val assistantIndex = messages.lastIndex

        activeCall = AssistantApi.streamChat(
            message = text,
            sessionId = sessionId,
            userName = null,
            onDelta = { delta ->
                // 流式追加增量文本（回调已在主线程）
                val current = messages[assistantIndex]
                if (!current.isError) {
                    messages[assistantIndex] = current.copy(content = current.content + delta)
                }
            },
            onError = { errMsg ->
                messages[assistantIndex] = ChatMessage("assistant", errMsg, isError = true)
                isGenerating = false
                activeCall = null
                persist()
            },
            onDone = {
                val current = messages[assistantIndex]
                isGenerating = false
                activeCall = null
                // 手动停止或模型空回复时给出明确提示
                if (!current.isError && current.content.isEmpty()) {
                    messages[assistantIndex] = current.copy(
                        content = if (manualStopped) "（已停止回复）" else "（没有收到回复，请稍后再试）"
                    )
                }
                persist()
            }
        )
    }

    /** 中断当前流式回复 */
    fun stopGenerating() {
        manualStopped = true
        activeCall?.cancel()
    }

    /** 错误消息点击重试：移除错误气泡后重发上一条用户消息 */
    fun retryLast() {
        if (isGenerating) return
        val lastUserIndex = messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) return
        val lastQuestion = messages[lastUserIndex].content
        if (messages.lastOrNull()?.isError == true) messages.removeAt(messages.lastIndex)
        send(lastQuestion)
    }

    /** 开启新会话：停止当前回复、清空消息、更换会话 ID */
    fun startNewChat() {
        manualStopped = true
        activeCall?.cancel()
        isGenerating = false
        activeCall = null
        messages.clear()
        val newSid = Random.nextInt(1000, 1_000_000)
        sessionId = newSid
        prefs.edit().putInt(KEY_SID, newSid).remove(KEY_MESSAGES).apply()
        input = ""
    }

    // 响应顶部栏"新对话"动作
    LaunchedEffect(newChatTrigger) {
        if (newChatTrigger > 0) startNewChat()
    }

    // 离开页面时取消网络请求，避免泄漏
    DisposableEffect(Unit) {
        onDispose { activeCall?.cancel() }
    }

    // 新消息/流式增量时自动滚动到底部
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length ?: 0, isGenerating) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        if (messages.isEmpty()) {
            // ---------- 空会话欢迎区 ----------
            WelcomeScreen(
                onSuggestionClick = { send(it) }
            )
        } else {
            // ---------- 消息列表 ----------
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 56.dp,          // 为顶部栏留白
                    bottom = 172.dp       // 为底部输入栏 + 导航栏留白
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        msg = msg,
                        // 仅最后一条助手消息可能处于"流式输出中"
                        streaming = isGenerating &&
                            msg === messages.lastOrNull() &&
                            msg.role == "assistant",
                        onErrorClick = { retryLast() }
                    )
                }
            }
        }

        // ---------- 底部输入栏（悬浮于导航栏之上） ----------
        ChatInputBar(
            value = input,
            enabled = true,
            isGenerating = isGenerating,
            onValueChange = { input = it },
            onSend = { send(input) },
            onStop = { stopGenerating() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 10.dp, end = 10.dp, bottom = 104.dp)
        )
    }
}

/** 空会话欢迎区：助手头像 + 问候语 + 快捷问题 */
@Composable
private fun WelcomeScreen(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(bottom = 120.dp, top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AssistantAvatar(size = 64)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "你好，我是史蒂文",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "您的专属智能管家，随时待命 · 对话数据只保存在本机",
            color = SubTextColor,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(28.dp))
        SUGGESTIONS.forEach { suggestion ->
            SuggestionChip(text = suggestion, onClick = { onSuggestionClick(suggestion) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** 快捷问题胶囊按钮 */
@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
    }
}

/** 单条消息气泡：用户靠右蓝色，助手靠左半透明玻璃风 */
@Composable
private fun MessageBubble(
    msg: ChatMessage,
    streaming: Boolean,
    onErrorClick: () -> Unit
) {
    // 用 BoxWithConstraints 取得行宽，把气泡最大宽度限制为屏幕的 82%
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.82f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            val isUser = msg.role == "user"
            if (!isUser) {
                AssistantAvatar(size = 34)
                Spacer(Modifier.width(8.dp))
            }

            // 助手：首条消息左上角小圆角，其余大圆角；用户反之
            val bubbleShape = if (isUser) {
                RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
            } else {
                RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
            }
            val bubbleColor = when {
                msg.isError -> Color(0xFF4A2A2A)
                isUser -> UserBubbleBg
                else -> AssistantBubbleBg
            }
            val textColor = if (msg.isError) ErrorTextColor else Color.White

            Box(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .then(
                        if (msg.isError) Modifier.clickable(onClick = onErrorClick) else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when {
                    // 已开始流式输出：展示文本并在尾部显示闪烁光标
                    streaming && msg.content.isNotEmpty() ->
                        Text(text = msg.content + " ▍", color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
                    // 等待首个字：三点"正在输入"动画
                    streaming && msg.content.isEmpty() -> TypingDots()
                    // 错误消息：附重试提示
                    msg.isError -> Text(
                        text = msg.content + "\n\n点击重试",
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    else -> Text(
                        text = msg.content,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            if (isUser) {
                Spacer(Modifier.width(8.dp))
                UserAvatar(size = 34)
            }
        }
    }
}

/** 助手头像：蓝色渐变圆角方块 + 机器人图标 */
@Composable
private fun AssistantAvatar(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size.dp * 0.32f))
            .background(
                Brush.linearGradient(listOf(Color(0xFF5AC8FA), Color(0xFF0A84FF)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_smart_toy_24),
            contentDescription = "助手",
            tint = Color.White,
            modifier = Modifier.size((size * 0.62f).dp)
        )
    }
}

/** 用户头像：深灰圆形 + 人像图标 */
@Composable
private fun UserAvatar(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_person_24),
            contentDescription = "我",
            tint = Color.White,
            modifier = Modifier.size((size * 0.6f).dp)
        )
    }
}

/** 三点"正在输入"动画（依次淡入淡出） */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.25f at 0
                        1f at index * 150 + 150
                        0.25f at index * 150 + 450
                        0.25f at 900
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

/**
 * 底部输入栏：圆角输入框 + 发送/停止圆形按钮
 * 生成中按钮变为停止键，可中断回复
 */
@Composable
private fun ChatInputBar(
    value: String,
    enabled: Boolean,
    isGenerating: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(InputBarBg)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
            .padding(start = 18.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
            .heightIn(min = 46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            enabled = enabled,
            maxLines = 5,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(AssistantBlue),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = if (isGenerating) "助手回复中…" else "给助手发消息",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )

        Spacer(Modifier.width(8.dp))

        // 发送/停止按钮：生成中显示停止，空闲时根据是否有内容切换可用态
        val canSend = value.isNotBlank() && !isGenerating
        val buttonBg = when {
            isGenerating -> Color(0xFFFF453A)
            canSend -> AssistantBlue
            else -> Color.White.copy(alpha = 0.22f)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .clickable(enabled = isGenerating || canSend) {
                    if (isGenerating) onStop() else onSend()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (isGenerating) R.drawable.baseline_stop_24 else R.drawable.baseline_send_24
                ),
                contentDescription = if (isGenerating) "停止生成" else "发送",
                tint = Color.White,
                modifier = Modifier.size(if (isGenerating) 16.dp else 20.dp)
            )
        }
    }
}
