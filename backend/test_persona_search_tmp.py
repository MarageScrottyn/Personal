"""临时脚本：验证史蒂文人设 + 网络搜索工具全链路，测试后删除"""
import json
import os
import urllib.request

import django

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "media_site.settings")
django.setup()

# ---------- 1) 单元级：直接调用 web_search / web_read ----------
from agent.tools import web_search, web_read

print("===== 单元测试 web_search =====")
res = web_search("OpenAI 最新模型")
for it in res.get("results", [])[:3]:
    print("-", it["title"], "|", it["url"][:60])
if res.get("error"):
    print("ERROR:", res["error"])

print("===== 单元测试 web_read =====")
if res.get("results"):
    content = web_read(res["results"][0]["url"])
    print("正文前120字:", content.get("content", content.get("error", ""))[:120])

# ---------- 2) 全链路：流式接口问人设 + 搜索 ----------
from django.contrib.auth import get_user_model
from rest_framework_simplejwt.tokens import RefreshToken

user = get_user_model().objects.first()
token = str(RefreshToken.for_user(user).access_token)


def ask(question, sid):
    body = json.dumps({"message": question, "session_id": sid}).encode()
    req = urllib.request.Request(
        "http://127.0.0.1:8000/api/chat/stream/",
        data=body,
        headers={
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json",
            "Accept": "text/event-stream",
        },
    )
    full = ""
    with urllib.request.urlopen(req, timeout=300) as resp:
        for raw in resp:
            line = raw.decode(errors="replace").rstrip()
            if line.startswith("data:") and '"delta"' in line:
                try:
                    full += json.loads(line[5:].strip()).get("delta", "")
                except Exception:
                    pass
    return full


print("===== 全链路：人设 =====")
print(ask("你是谁？用一两句话介绍一下你自己", 701))

print("===== 全链路：网络搜索 =====")
print(ask("帮我搜一下最近有什么AI方面的大新闻，简要总结", 702))
