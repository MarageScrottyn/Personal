"""
apps.py - Agent 应用配置
Django 启动时通过 ready() 钩子自动拉起 llama-server
"""
import os
import sys

from django.apps import AppConfig


class AgentConfig(AppConfig):
    name = "agent"
    default_auto_field = "django.db.models.BigAutoField"

    def ready(self):
        """
        Django 应用就绪回调
        仅在启动 Web 服务时拉起 llama-server，避免 migrate/shell 等命令误触发
        """
        runserver = "runserver" in sys.argv

        if runserver:
            # runserver 自带 reloader，会有父进程+子进程两次调用
            # RUN_MAIN=true 表示实际处理请求的子进程，只在此处启动一次
            if os.environ.get("RUN_MAIN") != "true":
                return
        elif not any(x in sys.argv for x in ("gunicorn", "uwsgi")):
            # 非 Web 服务命令（migrate / createsuperuser / shell 等）不启动模型
            return

        # 延迟导入，避免 apps.py 加载时的循环依赖
        from .server import start_llama_server

        start_llama_server()
