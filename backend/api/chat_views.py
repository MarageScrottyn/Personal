"""
chat_views.py - 对话 API 端点
提供两个接口：
- /api/chat/        同步完整返回（Agent 模式：带工具调用，适合非流式场景）
- /api/chat/stream/ SSE 流式返回（带人格、历史记忆与工具调用，适合 App/网页实时打字效果）
"""
import json

from django.http import JsonResponse, StreamingHttpResponse
from django.views.decorators.csrf import csrf_exempt

from agent.agent import run, _summarize, SUMMARY_THRESHOLD, _parse_tool_calls
from agent.llm import chat_completion
from agent.memory import (
    append_msg, get_history, get_summary, recall,
)
from agent.prompts import build_system, TOOLS
from agent.tools import call as call_tool
from agent.server import is_ready

# 流式工具调用最大轮数（模型调用工具后回填结果再生成，防止死循环）
MAX_TOOL_STEPS = 3
# 内容尾部缓冲长度：防止 <tool 标签被拆分到多个增量中漏检
XML_HOLD = 24


def _check_ready():
    """检查 llama-server 是否已就绪，未就绪返回 503 响应"""
    if not is_ready():
        return JsonResponse(
            {"error": "模型服务尚未就绪，请稍后再试"},
            status=503,
        )
    return None


def _build_context_messages(sid, user_name):
    """
    组装带人格、长期记忆、历史摘要与近期对话的上下文消息

    参数:
        sid: 会话 ID
        user_name: 主人名字（可选）

    返回:
        OpenAI 格式消息列表（不含本轮用户输入）
    """
    summary = get_summary(sid)
    history = get_history(sid)[-20:]
    memory_text = recall() + ("\n\n" + summary if summary else "")
    msgs = [{
        "role": "system",
        "content": build_system(user_name, memory_text)
    }]
    msgs += history
    return msgs


@csrf_exempt
def chat(request):
    """
    同步对话接口，完整返回助手回复

    请求体 JSON:
        {
            "message": "用户输入文本",
            "session_id": 1,        // 可选，会话 ID
            "user": "主人名字"       // 可选
        }

    返回 JSON:
        {"reply": "助手回复文本"}
    """
    # 模型未就绪时直接返回提示
    not_ready = _check_ready()
    if not_ready:
        return not_ready

    body = json.loads(request.body)
    reply = run(
        body["message"],
        sid=body.get("session_id", 1),
        user_name=body.get("user"),
    )
    return JsonResponse({"reply": reply})


@csrf_exempt
def chat_stream(request):
    """
    SSE 流式对话接口（带人格、多轮记忆与工具调用）
    将 llama-server 的流式 chunk 透传给前端，同时把本轮对话写入会话记忆；
    模型发起工具调用时自动执行工具并回填结果，再流式生成最终回答

    请求体 JSON:
        {
            "message": "用户输入文本",
            "session_id": 1,     // 可选，会话 ID
            "user": "主人名字"   // 可选
        }

    SSE 事件:
        event: delta   data: {"delta": "增量文本"}
        event: error   data: {"error": "错误描述"}
        event: done    data: {}
    """
    # 模型未就绪时直接返回提示
    not_ready = _check_ready()
    if not_ready:
        return not_ready

    body = json.loads(request.body)
    user_text = body["message"]
    sid = body.get("session_id", 1)
    user_name = body.get("user")

    # 组装上下文（人格 + 长期记忆 + 历史摘要 + 近期对话）并追加上本轮输入
    messages = _build_context_messages(sid, user_name)
    messages.append({"role": "user", "content": user_text})
    # 先落库用户消息，保证中断/异常时对话记录也完整
    append_msg(sid, "user", user_text)

    def gen():
        visible_parts = []  # 收集所有实际推送给客户端的可见文本，用于写入记忆
        try:
            # 工具调用循环：模型调用工具 -> 执行并回填结果 -> 再次流式生成
            for _ in range(MAX_TOOL_STEPS):
                vis, full, calls = yield from _stream_phase(messages)
                visible_parts.append(vis)
                if not calls:
                    break
                # 组装 assistant 工具调用消息（OpenAI 格式），供下一轮渲染
                messages.append({
                    "role": "assistant",
                    "content": full or None,
                    "tool_calls": [
                        {
                            "id": f"call_{i + 1}",
                            "type": "function",
                            "function": {
                                "name": name,
                                "arguments": args if isinstance(args, str)
                                else json.dumps(args, ensure_ascii=False),
                            },
                        }
                        for i, (name, args) in enumerate(calls)
                    ],
                })
                # 执行每个工具，并把结果以 tool 角色回填进上下文
                for i, (name, args) in enumerate(calls):
                    result = call_tool(name, args)
                    messages.append({
                        "role": "tool",
                        "tool_call_id": f"call_{i + 1}",
                        "content": json.dumps(result, ensure_ascii=False),
                    })
            else:
                # 连续多轮都在调工具：最后一轮禁用工具，强制模型给出文本回答
                vis, _full, _calls = yield from _stream_phase(messages, use_tools=False)
                visible_parts.append(vis)
        except Exception as e:
            # 上游异常时通知前端，避免输入框一直处于等待状态
            err = json.dumps({"error": f"模型响应中断：{e}"}, ensure_ascii=False)
            yield f"event: error\ndata: {err}\n\n"

        # 可见回复写入历史记忆；消息过多时触发自动摘要，防止上下文溢出
        full_reply = "".join(visible_parts)
        if full_reply.strip():
            append_msg(sid, "assistant", full_reply)
            if len(get_history(sid)) > SUMMARY_THRESHOLD:
                try:
                    _summarize(sid)
                except Exception:
                    pass
        yield "event: done\ndata: {}\n\n"

    resp = StreamingHttpResponse(gen(), content_type="text/event-stream")
    resp["Cache-Control"] = "no-cache"
    resp["X-Accel-Buffering"] = "no"  # 禁止 nginx 缓冲
    return resp


def _stream_phase(messages, use_tools=True):
    """
    执行单阶段的流式生成（生成器，配合 yield from 使用）

    处理两种工具调用载体：
    1. llama.cpp 解析后的 OpenAI tool_calls 增量片段（跨 chunk 累积）
    2. 内容中内联的 MiniCPM5 XML 格式 <tool>...</tool>（命中后抑制输出，
       尾部保留 XML_HOLD 字符缓冲防止标签被增量拆分漏检）

    参数:
        messages: 发送给模型的上下文消息
        use_tools: 是否携带工具定义（最后一轮强制文本回答时传 False）

    生成:
        event: delta 的 SSE 数据（仅可见文本，工具标签不外泄）

    返回:
        (可见文本, 完整内容, 工具调用列表[(name, arguments), ...])
    """
    r = chat_completion(messages, tools=TOOLS if use_tools else None, stream=True)
    buf = ""           # 完整内容缓冲
    emitted = 0        # 已推送给客户端的字符位置
    tool_mode = False  # 是否已进入工具调用模式（命中后不再输出内容）
    frag = {}          # OpenAI 格式工具调用片段：index -> {name, args}
    vis_parts = []     # 实际推送的可见文本片段

    def _emit(piece):
        """推送一段可见文本给客户端并记录"""
        if piece:
            vis_parts.append(piece)
            yield f"event: delta\ndata: {json.dumps({'delta': piece}, ensure_ascii=False)}\n\n"

    try:
        for line in r.iter_lines():
            if not line.startswith(b"data: "):
                continue
            data = line[6:].decode()
            if data.strip() == "[DONE]":
                break
            try:
                delta = json.loads(data)["choices"][0]["delta"]
            except Exception:
                continue

            # 1) 收集 OpenAI 格式的工具调用增量片段
            tcs = delta.get("tool_calls")
            if tcs:
                for tc in tcs:
                    slot = frag.setdefault(tc.get("index", 0), {"name": "", "args": ""})
                    fn = tc.get("function") or {}
                    if fn.get("name"):
                        slot["name"] += fn["name"]
                    if fn.get("arguments"):
                        slot["args"] += fn["arguments"]
                continue

            # 2) 处理正文增量（带 <tool 标签抑制）
            c = delta.get("content") or ""
            if not c or tool_mode:
                continue
            buf += c
            # 在未确认安全的区域查找工具标签起始
            idx = buf.find("<tool", emitted)
            if idx != -1:
                # 命中工具标签：只推送标签之前的文本，之后转入工具模式
                if idx > emitted:
                    yield from _emit(buf[emitted:idx])
                emitted = idx
                tool_mode = True
            else:
                # 末尾保留 XML_HOLD 字符不推送，防止标签被拆分漏检
                safe = max(0, len(buf) - XML_HOLD)
                if safe > emitted:
                    yield from _emit(buf[emitted:safe])
                    emitted = safe
        # 流结束后处理残余缓冲（此时不会再有新文本）
        if not tool_mode and len(buf) > emitted:
            idx = buf.find("<tool", emitted)
            if idx != -1:
                yield from _emit(buf[emitted:idx])
                emitted = idx
                tool_mode = True
            else:
                yield from _emit(buf[emitted:])
                emitted = len(buf)
    finally:
        # 无论正常结束还是异常都释放上游连接
        r.close()

    # 解析工具调用：优先 OpenAI tool_calls 片段，其次回退到内容中的 XML 格式
    calls = []
    if frag:
        calls = [(s["name"], s["args"]) for s in frag.values() if s["name"]]
    elif tool_mode or "<tool" in buf:
        # 复用同步链路的 XML 解析逻辑（只需 content 属性）
        dummy = type("M", (), {"tool_calls": None, "content": buf})()
        calls = _parse_tool_calls(dummy)
    return "".join(vis_parts), buf, calls
