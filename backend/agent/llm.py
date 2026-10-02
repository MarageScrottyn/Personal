"""
llm.py - 封装 llama-server 的 HTTP 调用
统一关闭思考模式（reasoning_content），避免拖慢速度和浪费 token
"""
import requests

# llama-server 的 OpenAI 兼容接口地址（本地运行在 Termux 上）
LLAMA_URL = "http://127.0.0.1:8080/v1/chat/completions"
# 模型名称
MODEL = "MiniCPM5-2B"


def chat_completion(messages, tools=None, stream=False, **kw):
    """
    调用 llama-server 的聊天补全接口

    参数:
        messages: 消息列表，格式为 [{"role": "user", "content": "..."}]
        tools: 工具定义列表（OpenAI 格式），为 None 则不启用工具调用
        stream: 是否流式返回
        **kw: 其他参数（temperature, top_p, min_p, max_tokens 等）

    返回:
        requests.Response 对象
    """
    # 构造请求 payload，统一关闭思考模式
    # 注意：stream 必须写入请求体，否则 llama-server 返回完整 JSON 而非 SSE 流
    payload = {
        "model": MODEL,
        "messages": messages,
        "stream": stream,
        "temperature": kw.get("temperature", 0.7),
        "top_p": kw.get("top_p", 0.95),
        "min_p": kw.get("min_p", 0.0),
        "max_tokens": kw.get("max_tokens", 512),
        "chat_template_kwargs": {"enable_thinking": False},
    }
    # 如果提供了工具定义，则启用工具调用
    if tools:
        payload["tools"] = tools
        payload["tool_choice"] = "auto"

    # 发送 POST 请求，流式由 stream 参数控制
    r = requests.post(LLAMA_URL, json=payload, stream=stream, timeout=300)
    r.raise_for_status()
    return r
