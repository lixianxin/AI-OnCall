# ConnectTimeoutException — AI OnCall 项目排查

## 项目场景

AI OnCall 项目中使用 OkHttp 连接服务端时出现连接超时。

## Android 端配置

`kotlin
// client-android/.../open/config/OnCallConfig.kt
const val AI_SERVICE_BASE_URL = ""http://121.40.241.161:8081""
const val CONNECT_TIMEOUT_SECONDS: Long = 30L
`

## 常见原因

1. 模拟器 localhost 问题：
   - 模拟器 localhost 指向自身，不是宿主机
   - 应使用 10.0.2.2 访问宿主机，或直接使用公网 IP

2. 服务端未启动：
   - 确认服务端运行：ps aux | grep ai-service
   - 确认端口监听：netstat -tlnp | grep 8081

3. 防火墙拦截端口 8081

## 排查步骤

`ash
# 服务端排查
curl http://121.40.241.161:8081
# 如果无响应：
ssh root@121.40.241.161
ps aux | grep ai-service    # 进程是否运行
netstat -tlnp | grep 8081   # 端口是否监听
`

## 相关来源

faq/how-to-deploy.md
troubleshooting/sse-connection.md