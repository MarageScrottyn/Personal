package com.android.aggregationspace.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 助手对话 API 客户端
 *
 * 专门对接后端 SSE 流式接口 /api/chat/stream/：
 * - 使用独立的长超时 OkHttpClient（回复可能持续输出较久）
 * - 逐行读取 text/event-stream，解析 delta/done/error 事件
 * - 所有回调统一切到主线程，方便 Compose 直接更新状态
 */
object AssistantApi {

    private const val TAG = "AssistantApi"

    /** 流式专用客户端：从 ApiService.client 派生（继承自动鉴权/401刷新），仅把读超时放宽到 10 分钟 */
    private val streamClient: OkHttpClient by lazy {
        ApiService.client.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** 主线程 Handler，用于把后台读取线程的回调投递回 UI 线程 */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 发起一次流式对话（立即返回 OkHttp Call，可在外部 cancel 中断）
     *
     * @param message 用户本轮输入
     * @param sessionId 会话 ID（与后端记忆对应，新建对话时更换）
     * @param userName 主人名字（可选，用于助手人格）
     * @param onDelta 每收到一段增量文本时回调（主线程）
     * @param onError 出错时回调错误文案（主线程），包含模型未就绪(503)等情况
     * @param onDone 本次流式回复正常结束回调（主线程）
     */
    fun streamChat(
        message: String,
        sessionId: Int,
        userName: String?,
        onDelta: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: () -> Unit
    ): Call {
        // 组装请求体（后端约定字段：message / session_id / user）
        val payload = JSONObject()
            .put("message", message)
            .put("session_id", sessionId)
            .apply { if (!userName.isNullOrBlank()) put("user", userName) }
            .toString()

        val builder = Request.Builder()
            // resolveMediaUrl 可把相对路径拼成完整域名地址；鉴权头由 ApiService 的拦截器自动附加
            .url(ApiService.resolveMediaUrl(ApiEndpoints.Assistant.CHAT_STREAM))
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")

        val call = streamClient.newCall(builder.build())

        // OkHttp 同步 execute 需放在后台线程；回调通过 mainHandler 切回主线程
        Thread {
            try {
                call.execute().use { response ->
                    // 非 2xx：读取错误信息（如 503 模型未就绪，后端返回 {"error": "..."}）
                    if (!response.isSuccessful) {
                        val raw = response.body?.string().orEmpty()
                        val msg = parseErrorOr(raw, "请求失败（HTTP ${response.code}）")
                        post { onError(msg) }
                        return@Thread
                    }

                    val source = response.body?.charStream()?.buffered()
                    if (source == null) {
                        post { onError("响应内容为空") }
                        return@Thread
                    }

                    // SSE 协议按行解析：事件名在 "event:" 行，数据在紧跟的 "data:" 行
                    var currentEvent = ""
                    source.use { reader ->
                        reader.forEachLine { line ->
                            when {
                                line.startsWith("event:") ->
                                    currentEvent = line.removePrefix("event:").trim()

                                line.startsWith("data:") -> {
                                    val data = line.removePrefix("data:").trim()
                                    when (currentEvent) {
                                        "delta" -> {
                                            val delta = JSONObject(data).optString("delta", "")
                                            if (delta.isNotEmpty()) post { onDelta(delta) }
                                        }
                                        "error" -> {
                                            val err = JSONObject(data).optString(
                                                "error", "模型响应中断"
                                            )
                                            post { onError(err) }
                                        }
                                    }
                                    currentEvent = ""
                                }
                            }
                        }
                    }
                    post { onDone() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "流式对话结束: ${e.javaClass.simpleName} ${e.message}")
                // 用户主动取消时静默结束，不作为错误提示
                if (call.isCanceled()) {
                    post { onDone() }
                } else {
                    post { onError("网络连接失败：${e.message ?: "请稍后再试"}") }
                }
            }
        }.start()

        return call
    }

    /** 优先从 JSON 中取 error 字段，取不到则使用兜底文案 */
    private fun parseErrorOr(raw: String, fallback: String): String =
        try {
            JSONObject(raw).optString("error", fallback)
        } catch (e: Exception) {
            fallback
        }

    /** 主线程投递辅助 */
    private inline fun post(crossinline action: () -> Unit) {
        mainHandler.post { action() }
    }
}
