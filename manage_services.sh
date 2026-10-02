#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# 聚合空间 - Termux 服务一键管理脚本
# 管理三个服务：Django 后端(8000) / Vite 前端开发服务器(5173,热重载) / Cloudflare 隧道
#
# 用法：
#   bash ~/Personal/manage_services.sh start   [all|django|vite|tunnel]  启动服务(默认 all)
#   bash ~/Personal/manage_services.sh stop    [all|django|vite|tunnel]  停止服务(默认 all)
#   bash ~/Personal/manage_services.sh restart [all|django|vite|tunnel]  重启服务(默认 all)
#   bash ~/Personal/manage_services.sh status                            查看状态与健康检查
# ============================================================

# ---------- 路径与常量定义 ----------
HOME_DIR="$HOME"                           # Termux 家目录
BACKEND_DIR="$HOME_DIR/Personal/backend"   # Django 后端目录
FRONTEND_DIR="$HOME_DIR/Personal/frontend" # 前端 Vite 项目目录
LOG_DIR="$HOME_DIR/logs"                   # 统一日志目录
TUNNEL_NAME="marage-tunnel"                # Cloudflare 隧道名称
DOMAIN="https://marage.ccwu.cc"            # 对外访问域名

mkdir -p "$LOG_DIR"

# 判断指定特征的进程是否正在运行（$1 为进程命令行匹配特征）
is_running() {
  pgrep -f "$1" >/dev/null 2>&1
}

# ---------- 启动 / 停止：Django 后端（端口 8000）----------
start_django() {
  if is_running "manage.py runserver"; then
    echo "[已运行] Django 后端 (0.0.0.0:8000)"
  else
    echo "[启动中] Django 后端 (0.0.0.0:8000) ..."
    # setsid 让进程在独立会话运行，终端/SSH 关闭后不被回收
    cd "$BACKEND_DIR" && setsid python manage.py runserver 0.0.0.0:8000 \
      >"$LOG_DIR/django.log" 2>&1 </dev/null &
  fi
}
stop_django() { pkill -f "manage.py runserver" 2>/dev/null && echo "[停止] Django" || echo "[跳过] Django 未运行"; }

# ---------- 启动 / 停止：Vite 前端开发服务器（端口 5173，支持 HMR 热重载）----------
start_vite() {
  if is_running "[v]ite/bin/vite.js"; then
    echo "[已运行] Vite 开发服务器 (0.0.0.0:5173, 热重载)"
  else
    echo "[启动中] Vite 开发服务器 (0.0.0.0:5173, 热重载) ..."
    # 直接调用 vite 启动脚本（绕过 npm 包装层，避免 SSH 断开后子进程被回收）
    cd "$FRONTEND_DIR" && setsid node node_modules/vite/bin/vite.js \
      >"$LOG_DIR/vite.log" 2>&1 </dev/null &
  fi
}
# 用 [v] 技巧避免 pkill/pgrep 匹配到本管理脚本自身的命令行字符串
stop_vite() { pkill -f "[v]ite/bin/vite.js" 2>/dev/null && echo "[停止] Vite" || echo "[跳过] Vite 未运行"; }

# ---------- 启动 / 停止：Cloudflare 隧道（HTTP/2 协议，移动网络更稳）----------
start_tunnel() {
  if pgrep -x cloudflared >/dev/null 2>&1; then
    echo "[已运行] Cloudflare 隧道 ($TUNNEL_NAME)"
  else
    echo "[启动中] Cloudflare 隧道 ($TUNNEL_NAME, http2) ..."
    # 配置文件位于 ~/.cloudflared/config.yml，其中指定 protocol: http2 与路径分流规则
    cd "$HOME_DIR" && setsid cloudflared tunnel run "$TUNNEL_NAME" \
      >"$LOG_DIR/cloudflared.log" 2>&1 </dev/null &
  fi
}
stop_tunnel() { pkill -x cloudflared 2>/dev/null && echo "[停止] Cloudflare 隧道" || echo "[跳过] Cloudflare 未运行"; }

# ---------- 停止全部服务 ----------
stop_all() {
  echo "[停止中] 正在停止全部服务 ..."
  stop_django
  stop_vite
  stop_tunnel
  sleep 2
  echo "全部服务已停止。"
}

# ---------- 启动全部服务并做健康检查 ----------
start_all() {
  start_django
  start_vite
  start_tunnel
  echo ""
  echo "等待服务就绪（约 14 秒）..."
  sleep 14
  show_status
}

# ---------- 查看运行状态与健康检查 ----------
show_status() {
  echo "================ 进程状态 ================"
  if is_running "manage.py runserver"; then echo " Django     : 运行中"; else echo " Django     : 未运行"; fi
  if is_running "[v]ite/bin/vite.js"; then echo " Vite       : 运行中 (热重载)"; else echo " Vite       : 未运行"; fi
  if pgrep -x cloudflared >/dev/null 2>&1; then echo " Cloudflared: 运行中"; else echo " Cloudflared: 未运行"; fi

  echo "================ 本地端口 ================"
  # 401 表示 Django 在线但接口需要认证（属正常）
  curl -s -m 5 -o /dev/null -w " Django  (8000): HTTP %{http_code}\n" http://127.0.0.1:8000/api/categories/ 2>/dev/null \
    || echo " Django  (8000): 无响应"
  # 200 表示 Vite 开发服务器在线
  curl -s -m 5 -o /dev/null -w " Vite    (5173): HTTP %{http_code}\n" http://127.0.0.1:5173/ 2>/dev/null \
    || echo " Vite    (5173): 无响应"

  echo "================ 公网域名 ================"
  # 经 Cloudflare 隧道访问后端接口（401=链路正常；530/000=隧道或移动网络波动，可稍后重试）
  curl -s -m 12 -o /dev/null -w " $DOMAIN/api  : HTTP %{http_code}\n" "$DOMAIN/api/categories/" 2>/dev/null \
    || echo " $DOMAIN : 访问失败（隧道可能正在重连，请稍后再试）"
  echo ""
  echo " 网页入口: $DOMAIN/        后台管理: $DOMAIN/admin/"
  echo " 日志目录: $LOG_DIR/  (django.log / vite.log / cloudflared.log)"
}

# ---------- 命令分发 ----------
ACTION="${1:-}"
TARGET="${2:-all}"

# 针对单个服务执行 start/stop/restart
handle_single() {
  local act="$1" svc="$2"
  case "$svc" in
    django)
      [ "$act" != "start" ] && stop_django
      [ "$act" = "restart" ] && sleep 2
      [ "$act" != "stop" ] && start_django
      ;;
    vite)
      [ "$act" != "start" ] && stop_vite
      [ "$act" = "restart" ] && sleep 2
      [ "$act" != "stop" ] && start_vite
      ;;
    tunnel)
      [ "$act" != "start" ] && stop_tunnel
      [ "$act" = "restart" ] && sleep 2
      [ "$act" != "stop" ] && start_tunnel
      ;;
    *)
      echo "未知服务: $svc （可选: django / vite / tunnel / all）"; exit 1 ;;
  esac
}

case "$ACTION" in
  status)
    show_status
    ;;
  start)
    if [ "$TARGET" = "all" ]; then start_all; else handle_single start "$TARGET"; fi
    ;;
  stop)
    if [ "$TARGET" = "all" ]; then stop_all; else handle_single stop "$TARGET"; fi
    ;;
  restart)
    if [ "$TARGET" = "all" ]; then
      stop_all; start_all
    else
      handle_single restart "$TARGET"
      echo "等待服务就绪（约 10 秒）..."
      sleep 10
      show_status
    fi
    ;;
  *)
    echo "用法: bash $0 {start|stop|restart|status} [all|django|vite|tunnel]"
    exit 1
    ;;
esac
