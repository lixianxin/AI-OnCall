# AI Service API 参考

## 服务信息

| 项目 | 值 |
|------|-----|
| 服务地址 | http://121.40.241.161:8081 |
| 框架 | Spring Boot 3.4.5 + WebFlux (Netty) |
| Java 版本 | JDK 17 |
| 部署路径 | /opt/ai-oncall/ai-service.jar |
| 知识库路径 | /opt/ai-oncall/docs/ |

## 接口：流式聊天

### POST /api/chat/stream

**请求头**

`
Content-Type: application/json
Accept: text/event-stream
`

**请求体**

`json
{
  "message": "如何注册Tab",
  "conversationId": "android-1748860800000"
}
`

**响应**：SSE 事件流

### 配置项

| 参数 | 环境变量 | 默认值 | 说明 |
|------|---------|--------|------|
| server.port | — | 8081 | 服务端口 |
| deepseek.api-key | — | sk-207987ffc28548b497c903c5371ae8bd | DeepSeek API Key |
| deepseek.base-url | — | https://api.deepseek.com | API 地址 |
| deepseek.model | — | deepseek-chat | 模型名 |
| oncall.docs-path | ONCALL_DOCS_PATH | ../docs | 知识库目录 |
| oncall.docs-remote-url | ONCALL_DOCS_REMOTE_URL | (空) | 远程拉取地址 |

### Android 客户端配置

`kotlin
// client-android/.../open/config/OnCallConfig.kt
object OnCallConfig {
    const val AI_SERVICE_BASE_URL = ""http://121.40.241.161:8081"
    const val STREAM_CHAT_API = ""/api/chat/stream"
    const val CONNECT_TIMEOUT_SECONDS: Long = 30L
    const val READ_TIMEOUT_MINUTES: Long = 5L
    const val WRITE_TIMEOUT_SECONDS: Long = 30L
}
`

### 快速验证

`ash
curl -X POST http://121.40.241.161:8081/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"如何注册Tab","conversationId":"test"}'
`

## 相关来源

architecture/system-overview.md
api/sse-protocol.md