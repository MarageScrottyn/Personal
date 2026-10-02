"""
agent.py - Agent 核心循环
流程：检索记忆 -> 工具调用循环 -> 自动摘要
"""
import json
import re

from .llm import chat_completion
from .prompts import build_system, TOOLS
from .memory import (
    get_summary, get_history, append_msg, set_summary, recall, get
)
from .tools import call

# 工具调用最大轮数，防止死循环
MAX_STEPS = 5
# 消息数超过此阈值触发自动摘要
SUMMARY_THRESHOLD = 30


def _parse_tool_calls(resp_msg):
    """
    解析模型返回的工具调用，兼容两种格式：
    1. llama.cpp 的 OpenAI tool_calls 字段
    2. MiniCPM5 的 XML 格式 <tool><name>...</name><arguments>...</arguments></tool>

    参数:
        resp_msg: 模型返回的消息对象（需具备 tool_calls 和 content 属性）

    返回:
        工具调用列表 [(name, arguments), ...]
    """
    calls = []
    # 优先处理 OpenAI 标准 tool_calls 字段
    if getattr(resp_msg, "tool_calls", None):
        for tc in resp_msg.tool_calls:
            f = tc.function
            calls.append((f.name, f.arguments))
    else:
        # 回退到 XML 格式解析（MiniCPM5 风格）
        content = resp_msg.content or ""
        for m in re.finditer(
            r"<tool>\s*<name>(.*?)</name>\s*<arguments>(.*?)</arguments>\s*</tool>",
            content, re.S
        ):
            calls.append((m.group(1).strip(), m.group(2).strip()))
    return calls


def run(user_text, sid=1, user_name=None):
    """
    执行一次 Agent 对话

    参数:
        user_text: 用户输入文本
        sid: 会话 ID，默认为 1
        user_name: 主人名字（可选）

    返回:
        助手最终回复文本
    """
    # 1. 组装消息：system + 历史摘要 + 近期历史
    summary = get_summary(sid)
    history = get_history(sid)[-20:]
    msgs = [{
        "role": "system",
        "content": build_system(user_name, recall() + ("\n\n" + summary if summary else ""))
    }]
    msgs += history
    msgs.append({"role": "user", "content": user_text})
    append_msg(sid, "user", user_text)

    # 2. Agent 工具调用循环
    msg = None
    for _ in range(MAX_STEPS):
        r = chat_completion(msgs, tools=TOOLS).json()
        msg = r["choices"][0]["message"]
        msgs.append(msg)
        # 解析工具调用
        calls = _parse_tool_calls(type("M", (), {
            "tool_calls": msg.get("tool_calls"),
            "content": msg.get("content"),
        }))
        if not calls:
            break
        # 执行每个工具调用，并将结果加入消息列表
        for name, args in calls:
            result = call(name, args)
            msgs.append({
                "role": "tool",
                "tool_call_id": "call_1",
                "content": json.dumps(result, ensure_ascii=False)
            })

    # 获取最终回复并保存
    final = msg.get("content") or "" if msg else ""
    append_msg(sid, "assistant", final)

    # 3. 多轮自动摘要（防止 8K 上下文溢出）
    if len(get_history(sid)) > SUMMARY_THRESHOLD:
        _summarize(sid)

    return final


def _summarize(sid):
    """
    对长会话进行摘要压缩，删除已被摘要的旧消息

    参数:
        sid: 会话 ID
    """
    history = get_history(sid)
    old_summary = get_summary(sid)
    msgs = [
        {"role": "system", "content": "请把以下对话压缩为关键事实摘要（保留人名、偏好、计划、结论），中文，不超过200字。"},
        {"role": "user", "content": f"已有摘要：{old_summary}\n\n对话：\n" + json.dumps(history, ensure_ascii=False)},
    ]
    r = chat_completion(msgs, max_tokens=256).json()
    set_summary(sid, r["choices"][0]["message"]["content"])

    # 删除已被摘要的旧消息，只保留最近 10 条
    c = get()
    ids = c.execute("SELECT id FROM messages WHERE sid=? ORDER BY id DESC LIMIT 10", (sid,)).fetchall()
    if ids:
        placeholders = ",".join(str(i[0]) for i in ids)
        c.execute(f"DELETE FROM messages WHERE sid=? AND id NOT IN ({placeholders})", (sid,))
        c.commit()
    c.close()
