# SSE 连接断开排查

## 问题现象

Android 端 AI OnCall 回答到一半断连，或完全收不到回复。

## 常见原因

### 1. OkHttp readTimeout 过短

`kotlin
// 正确配置（client-android/.../open/config/OnCallConfig.kt）
const val READ_TIMEOUT_MINUTES: Long = 5L  // LLM 推理最长 5 分钟
`

### 2. DeepSeek API Key 失效

服务端日志出现：
`
DeepSeek API error: status=401, body={"error":"invalid api key"}
`
在 /opt/ai-oncall/application.yml 中更新 api-key。

### 3. 服务端未启动

`
curl http://121.40.241.161:8081/api/chat/stream
→ Connection refused
`

解决：SSH 到服务器重启服务。

### 4. Android 网络问题

模拟器用 10.0.2.2 访问宿主机：
`kotlin
// OnCallConfig.kt
const val AI_SERVICE_BASE_URL = ""http://10.0.2.2:8081""  // 模拟器
// const val AI_SERVICE_BASE_URL = ""http://121.40.241.161:8081""  // 远程服务器
`

## 验证步骤

`ash
# 1. 服务端是否运行
ps aux | grep ai-service

# 2. 端口是否监听
netstat -tlnp | grep 8081

# 3. 直接 curl 测试
curl -X POST http://localhost:8081/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"test","conversationId":"ping"}'
`

## 相关来源

api/ai-service-api.md
api/sse-protocol.md