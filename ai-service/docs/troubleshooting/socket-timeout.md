# SocketTimeoutException — AI OnCall 项目排查

## 项目场景

SSE 流式读取过程中超时。

## Android 端配置

`kotlin
// client-android/.../open/config/OnCallConfig.kt
const val READ_TIMEOUT_MINUTES: Long = 5L   // SSE 长连接 5 分钟
const val WRITE_TIMEOUT_SECONDS: Long = 30L
`

## 常见原因

1. readTimeout 过短：LLM 推理时间超过 OkHttp readTimeout
2. 网络丢包：公网环境偶发
3. DeepSeek 响应慢：高峰时段

## 排查步骤

1. 检查 app.log 中 DeepSeek API 调用耗时
2. 确认 Android 端 readTimeout 为 5 分钟
3. 本地 curl 测试排除网络问题

## 相关来源

api/ai-service-api.md
troubleshooting/sse-connection.md