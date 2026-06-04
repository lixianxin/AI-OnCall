# 如何用 AI OnCall 诊断错误

## 支持的诊断方式

### 方式1：粘贴 Logcat

从 Android Studio 复制 logcat 日志，直接粘贴到聊天框：

`
2026-06-02 16:42:35.836 ClipboardService system_server E Denying clipboard access to com.android.chrome
`

### 方式2：粘贴异常堆栈

`java
java.lang.NullPointerException
    at com.oncall.ai.service.DocumentStore.loadFile(DocumentStore.java:82)
`

AI 会自动识别错误类型，从 troubleshooting/ 知识库中匹配解决方案。

### 方式3：输入错误码

`
错误码 1003 是什么意思？
`

## 支持诊断的错误

| 错误类型 | 触发关键词 | 知识库文档 |
|---------|-----------|-----------|
| NullPointerException | NullPointer, NPE, 空指针 | troubleshooting/nullpointer.md |
| SSLException | SSL, certificate, 证书 | troubleshooting/ssl-error.md |
| 连接超时 | ConnectTimeout, Connection refused | troubleshooting/connect-timeout.md |
| 读取超时 | SocketTimeout, Read timed out | troubleshooting/socket-timeout.md |
| 模拟器黑屏 | 黑屏, black screen, 模拟器 | troubleshooting/black-screen.md |
| SSE 断开 | SSE, 流断开, 回答中断 | troubleshooting/sse-connection.md |
| DeepSeek 错误 | DeepSeek, API key, 401 | troubleshooting/deepseek-error.md |

## 相关来源

troubleshooting/ 目录下全部文档