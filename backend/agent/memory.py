"""
memory.py - SQLite 记忆存储
存储会话历史、长期事实、会话摘要，防止上下文爆炸
"""
import sqlite3
import time
from pathlib import Path

# 数据库文件路径
DB = Path("agent/data/agent.db")


def get():
    """获取数据库连接，并确保表已创建"""
    DB.parent.mkdir(exist_ok=True)
    c = sqlite3.connect(DB)
    c.executescript("""
        CREATE TABLE IF NOT EXISTS facts(id INTEGER PRIMARY KEY, k TEXT, v TEXT, ts REAL);
        CREATE TABLE IF NOT EXISTS sessions(id INTEGER PRIMARY KEY, title TEXT, created REAL);
        CREATE TABLE IF NOT EXISTS messages(id INTEGER PRIMARY KEY, sid INTEGER, role TEXT, content TEXT, ts REAL);
        CREATE TABLE IF NOT EXISTS summaries(sid INTEGER PRIMARY KEY, text TEXT, ts REAL);
    """)
    return c


def save_fact(k, v):
    """
    保存一条长期记忆事实

    参数:
        k: 键名
        v: 值
    """
    c = get()
    c.execute("INSERT INTO facts(k,v,ts) VALUES(?,?,?)", (k, v, time.time()))
    c.commit()
    c.close()


def recall(limit=30):
    """
    召回近期长期记忆，拼接为文本

    参数:
        limit: 返回条数上限

    返回:
        记忆文本字符串
    """
    c = get()
    rows = c.execute("SELECT k,v FROM facts ORDER BY ts DESC LIMIT ?", (limit,)).fetchall()
    c.close()
    return "\n".join(f"{k}: {v}" for k, v in rows)


def append_msg(sid, role, content):
    """
    追加一条消息到会话历史

    参数:
        sid: 会话 ID
        role: 角色（user/assistant/tool）
        content: 消息内容
    """
    c = get()
    c.execute("INSERT INTO messages(sid,role,content,ts) VALUES(?,?,?,?)", (sid, role, content, time.time()))
    c.commit()
    c.close()


def get_history(sid, limit=40):
    """
    获取会话历史消息

    参数:
        sid: 会话 ID
        limit: 返回条数上限

    返回:
        消息列表，按时间正序排列
    """
    c = get()
    rows = c.execute("SELECT role,content FROM messages WHERE sid=? ORDER BY id DESC LIMIT ?", (sid, limit)).fetchall()
    c.close()
    return [{"role": r, "content": c} for r, c in reversed(rows)]


def set_summary(sid, text):
    """
    设置/更新会话摘要

    参数:
        sid: 会话 ID
        text: 摘要文本
    """
    c = get()
    c.execute("REPLACE INTO summaries(sid,text,ts) VALUES(?,?,?)", (sid, text, time.time()))
    c.commit()
    c.close()


def get_summary(sid):
    """
    获取会话摘要

    参数:
        sid: 会话 ID

    返回:
        摘要文本，不存在则返回空字符串
    """
    c = get()
    r = c.execute("SELECT text FROM summaries WHERE sid=?", (sid,)).fetchone()
    c.close()
    return r[0] if r else ""
