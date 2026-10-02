"""
server.py - llama-server 进程管理
Django 启动时自动拉起本地模型服务，避免手动启动
就绪检测放在后台线程，不阻塞 Django 启动
"""
import os
import socket
import subprocess
import threading
import time
import logging

logger = logging.getLogger("agent")

# llama-server 可执行文件路径
LLAMA_SERVER_BIN = os.path.expanduser("~/llama.cpp/build/bin/llama-server")
# 模型文件路径
MODEL_PATH = os.path.expanduser("~/llama.cpp/models/MiniCPM5-2B-GGUF/MiniCPM5-2B-Q4_K_M.gguf")
# 服务监听地址与端口（需与 llm.py 中 LLAMA_URL 保持一致）
LLAMA_HOST = "127.0.0.1"
LLAMA_PORT = 8080

# 启动参数（与 Termux 手动启动命令一致，额外关闭思考模式以提速）
# 注意：新版 llama.cpp 的 CORS 参数为 --cors-origins（默认即 *，本地服务无需显式设置），
# 旧参数 --cors-origin 已被移除，传入会导致进程直接启动失败
LLAMA_ARGS = [
    "--host", LLAMA_HOST,
    "--port", str(LLAMA_PORT),
    "-c", "8192",
    "-t", "6",
    "--temp", "1.0",
    "--top-p", "0.95",
    "--min-p", "0.0",
    "--jinja",
    # 关闭思考/推理模式以提速（替代已废弃的 --chat-template-kwargs 写法）
    "--reasoning", "off",
]

# 日志文件路径
LOG_FILE = os.path.join("agent", "data", "llama-server.log")
# 后台就绪检测最大等待秒数（手机 CPU 加载 2B 模型较慢，给足时间）
MAX_WAIT_SECONDS = 180


def is_port_in_use(host, port):
    """
    检测端口是否已被占用（用于判断 llama-server 是否已运行）

    参数:
        host: 主机地址
        port: 端口号

    返回:
        True 表示端口已占用（服务在运行），False 表示未占用
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(1)
        try:
            s.connect((host, port))
            return True
        except (ConnectionRefusedError, OSError):
            return False


def is_ready():
    """llama-server 是否就绪（端口可连接）"""
    return is_port_in_use(LLAMA_HOST, LLAMA_PORT)


def _wait_for_ready():
    """
    后台线程：轮询等待 llama-server 就绪
    最长等待 MAX_WAIT_SECONDS 秒，就绪或超时均打印日志
    """
    elapsed = 0
    while elapsed < MAX_WAIT_SECONDS:
        if is_ready():
            logger.info("llama-server 启动成功，服务已就绪")
            return
        time.sleep(2)
        elapsed += 2
    logger.warning(
        f"llama-server 启动超时（{MAX_WAIT_SECONDS}s），端口未就绪，"
        f"可查看日志: {LOG_FILE}"
    )


def start_llama_server():
    """
    后台启动 llama-server 进程（非阻塞）
    - 若端口已被占用则跳过（服务已运行）
    - 若二进制或模型文件缺失则跳过并打印警告
    - 启动后立即返回，就绪检测由后台线程负责
    """
    # 已运行则跳过，避免重复启动
    if is_port_in_use(LLAMA_HOST, LLAMA_PORT):
        logger.info("llama-server 已在运行，跳过启动")
        return

    # 检查二进制文件是否存在
    if not os.path.exists(LLAMA_SERVER_BIN):
        logger.warning(f"llama-server 二进制不存在: {LLAMA_SERVER_BIN}，跳过启动")
        return

    # 检查模型文件是否存在
    if not os.path.exists(MODEL_PATH):
        logger.warning(f"模型文件不存在: {MODEL_PATH}，跳过启动")
        return

    # 组装启动命令
    cmd = [LLAMA_SERVER_BIN, "-m", MODEL_PATH] + LLAMA_ARGS
    logger.info(f"启动 llama-server: {' '.join(cmd)}")

    # 确保日志目录存在
    os.makedirs(os.path.dirname(LOG_FILE), exist_ok=True)

    try:
        # 后台启动，输出重定向到日志文件，新会话使其脱离 Django 进程
        log_f = open(LOG_FILE, "ab")
        subprocess.Popen(
            cmd,
            stdout=log_f,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    except Exception as e:
        logger.error(f"启动 llama-server 失败: {e}")
        return

    # 启动后台线程轮询就绪状态，不阻塞 Django 启动
    t = threading.Thread(target=_wait_for_ready, daemon=True)
    t.start()
    logger.info("llama-server 已发起启动，后台加载中，Django 继续启动...")

