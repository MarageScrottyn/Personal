"""
tools.py - 工具注册表
使用装饰器方式注册工具，方便后续扩展新工具
"""
import json
import re
import datetime
import xml.etree.ElementTree as ET
from pathlib import Path

import requests

# 工具注册表：工具名 -> 函数
REGISTRY = {}

# 网络请求公共头：模拟浏览器，避免被搜索源拦截
_WEB_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    ),
    "Accept-Language": "zh-CN,zh;q=0.9",
}


def tool(name):
    """
    工具注册装饰器

    参数:
        name: 工具名称，需与 prompts.py 中 TOOLS 定义的 name 一致
    """
    def deco(fn):
        REGISTRY[name] = fn
        return fn
    return deco


@tool("remember")
def remember(key, value, **_):
    """
    记住一条事实/偏好到 SQLite 长期记忆

    参数:
        key: 记忆键名
        value: 记忆值
    """
    from .memory import save_fact
    save_fact(key, value)
    return {"ok": True, "msg": f"已记住 {key}={value}"}


@tool("get_datetime")
def get_datetime(**_):
    """获取当前日期时间"""
    now = datetime.datetime.now()
    # 星期映射为中文，方便助手直接用中文作答
    weekdays = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
    return {
        "date": now.strftime("%Y-%m-%d"),
        "time": now.strftime("%H:%M:%S"),
        "weekday": weekdays[now.weekday()],
    }


@tool("add_reminder")
def add_reminder(text, time=None, **_):
    """
    添加一条提醒/待办，写入文本文件

    参数:
        text: 提醒内容
        time: 提醒时间（可选，格式 YYYY-MM-DD HH:MM）
    """
    p = Path("agent/data/reminders.txt")
    p.parent.mkdir(exist_ok=True)
    with p.open("a", encoding="utf-8") as f:
        f.write(f"{time or '尽快'}\t{text}\n")
    return {"ok": True, "msg": f"已添加提醒：{text}"}


@tool("web_search")
def web_search(query, **_):
    """
    联网搜索：调用必应 RSS 接口（国内可达、免密钥），返回前几条结果

    参数:
        query: 搜索关键词

    返回:
        {"results": [{"title", "url", "snippet"}, ...]} 或 {"error": ...}
    """
    try:
        r = requests.get(
            "https://cn.bing.com/search",
            params={"q": query, "format": "rss", "count": "6"},
            headers=_WEB_HEADERS,
            timeout=12,
            allow_redirects=True,
        )
        r.raise_for_status()
        root = ET.fromstring(r.content)
        results = []
        for item in root.iter("item"):
            title = (item.findtext("title") or "").strip()
            link = (item.findtext("link") or "").strip()
            # 摘要中可能带 HTML 标签，统一去除并截断，节省模型上下文
            snippet = re.sub(r"<[^>]+>", "", item.findtext("description") or "").strip()
            if title:
                results.append({
                    "title": title,
                    "url": link,
                    "snippet": snippet[:150],
                })
            if len(results) >= 5:
                break
        if not results:
            return {"results": [], "msg": "没有搜索到相关内容，可尝试更换关键词"}
        return {"results": results}
    except Exception as e:
        return {"error": f"搜索失败：{e}"}


@tool("web_read")
def web_read(url, **_):
    """
    打开网页并提取正文文字（去除脚本/样式/标签，截取前 2000 字）

    参数:
        url: 完整网页地址

    返回:
        {"url", "content"} 或 {"error": ...}
    """
    try:
        if not url.startswith(("http://", "https://")):
            return {"error": "URL 需以 http:// 或 https:// 开头"}
        r = requests.get(url, headers=_WEB_HEADERS, timeout=12, allow_redirects=True)
        r.raise_for_status()
        # 编码未声明时尝试自动推断，避免中文乱码
        if not r.encoding or r.encoding.lower() == "iso-8859-1":
            r.encoding = r.apparent_encoding
        text = r.text
        # 去掉 script/style 块与全部 HTML 标签，再压缩空白
        text = re.sub(r"(?is)<(script|style)[^>]*>.*?</\1>", " ", text)
        text = re.sub(r"<[^>]+>", " ", text)
        text = re.sub(r"\s+", " ", text).strip()
        if not text:
            return {"error": "未能从网页中提取到文字内容"}
        return {"url": url, "content": text[:2000]}
    except Exception as e:
        return {"error": f"网页读取失败：{e}"}


def call(name, arguments):
    """
    执行工具调用

    参数:
        name: 工具名称
        arguments: 工具参数（JSON 字符串或字典）

    返回:
        工具执行结果字典
    """
    # 解析参数：支持 JSON 字符串或字典
    try:
        args = json.loads(arguments) if isinstance(arguments, str) else (arguments or {})
    except Exception:
        args = {}

    # 查找工具函数
    fn = REGISTRY.get(name)
    if not fn:
        return {"error": f"unknown tool {name}"}

    # 执行工具，捕获异常
    try:
        return fn(**args)
    except Exception as e:
        return {"error": str(e)}
