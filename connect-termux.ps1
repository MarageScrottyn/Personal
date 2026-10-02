# ============================================================
# connect-termux.ps1
# 离开局域网时，经 Cloudflare 隧道（ssh.marage.ccwu.cc）远程连接 Termux
#
# 用法：
#   .\connect-termux.ps1                # 打开交互式 SSH
#   .\connect-termux.ps1 "uptime"       # 直接执行远端命令
#   scp -P 2222 -i g:\Personal\.ssh_key 本地文件 u0_a354@127.0.0.1:~/   # 传文件
# 原理：先在本机后台启动 cloudflared access tcp（127.0.0.1:2222 ->
#       Cloudflare 边缘 -> 家中 Termux 的 sshd:8022），再用密钥登录
# ============================================================

# ---- 固定参数 ----
$cloudflared = "g:\Personal\cloudflared\cloudflared.exe"   # cloudflared 程序位置
$hostname    = "ssh.marage.ccwu.cc"                        # 隧道 SSH 子域名
$localPort   = 2222                                       # 本地监听端口
$sshKey      = "g:\Personal\.ssh_key"                      # 登录 Termux 的私钥
$sshUser     = "u0_a354"                                   # Termux 用户名

# 快速探测本地端口是否已在监听（比 Test-NetConnection 快）
function Test-PortOpen($port) {
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $async = $client.BeginConnect("127.0.0.1", $port, $null, $null)
        if ($async.AsyncWaitHandle.WaitOne(600)) {
            $client.EndConnect($async)
            $client.Close()
            return $true
        }
        $client.Close()
    } catch { }
    return $false
}

# ---- 1) 确保本地 cloudflared 隧道已启动 ----
if (-not (Test-PortOpen $localPort)) {
    Write-Host "[启动] 正在建立到 $hostname 的安全隧道 ..." -ForegroundColor Cyan
    # 注意：必须重定向标准输出/错误，隐藏窗口启动时 cloudflared 无控制台会直接退出
    $logDir = Split-Path $cloudflared -Parent
    try {
        $proc = Start-Process -WindowStyle Hidden -FilePath $cloudflared -PassThru `
            -ArgumentList @(
                "access", "tcp",
                "--hostname", $hostname,
                "--url", "localhost:$localPort"
            ) `
            -RedirectStandardOutput "$logDir\access-out.log" `
            -RedirectStandardError  "$logDir\access-err.log"
        Start-Sleep -Seconds 2
        if ($proc.HasExited) {
            Write-Host "[诊断] cloudflared 启动后立即退出，退出码=$($proc.ExitCode)" -ForegroundColor Yellow
            Get-Content "$logDir\access-err.log" -ErrorAction SilentlyContinue |
                ForEach-Object { Write-Host "       $_" -ForegroundColor Yellow }
        }
    } catch {
        Write-Host "[诊断] Start-Process 失败：$($_.Exception.Message)" -ForegroundColor Yellow
    }
    # 等待本地监听端口就绪（冷启动首次拨号 Cloudflare 边缘可能需要 10~20 秒）
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Milliseconds 600
        if (Test-PortOpen $localPort) { break }
    }
}

# ---- 2) 经本地端口 SSH 登录 Termux（透传脚本参数）----
if (Test-PortOpen $localPort) {
    ssh -i $sshKey -p $localPort `
        -o StrictHostKeyChecking=accept-new `
        -o ConnectTimeout=20 `
        "$sshUser@127.0.0.1" @args
} else {
    Write-Host "[失败] 隧道端口 $localPort 未就绪，请检查网络后重试" -ForegroundColor Red
    exit 1
}
