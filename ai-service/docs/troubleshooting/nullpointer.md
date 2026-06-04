# NullPointerException — AI OnCall 项目排查

## 错误特征

`
java.lang.NullPointerException
    at com.example.UserService.save(UserService.java:82)
`

在 AI OnCall 项目中出现 NPE 的常见位置：

`
ai-service/ 中：
- DocumentStore.loadFile() → docsPath 目录不存在
- DeepSeekClient.extractDeltaContent() → 响应 JSON 中 choices/delta 为 null
- ConversationStore.getOrCreate() → 首次会话

client-android/ 中：
- OpenOnCallRepository.parseSseLine() → JSON 解析异常
- OpenTabContentHost → tab 为 null 时未判空
`

## 根因

AI OnCall 项目中 NPE 的典型原因：

1. **服务端 docs 路径不存在**：
   `
   WARN DocumentStore: Docs directory not found: /opt/ai-oncall/../docs
   `
   解决：启动时用 -DONCALL_DOCS_PATH=/opt/ai-oncall/docs 指定正确路径

2. **DeepSeek API 返回异常**：
   `
   DeepSeek API error: status=401, body={"error":"invalid api key"}
   `
   api-key 无效或过期时，响应体中没有 choices[0].delta.content，extractDeltaContent 返回 null

3. **Android 端 SSE 解析异常**：
   `
   SSE_PARSE_ERROR: JSON 格式不匹配
   `
   服务端返回的事件格式与 Android 端期望的字段不匹配

## 排查步骤

1. 查看堆栈顶部：确定是服务端还是 Android 端
2. 服务端：检查 /opt/ai-oncall/app.log
3. Android：过滤 logcat 中 OnCallApiClient 或 OpenOnCallRepository
4. 确认对象在调用前是否已初始化

## 解决方案

### 服务端
`java
// DocumentStore 中防御性判空
public List<DocumentChunk> search(String query, int topK) {
    if (chunks.isEmpty()) return List.of();  // 避免 NPE
    // ...
}

// DeepSeekClient 中判空
var contentNode = delta.get("content");
if (contentNode == null || contentNode.isNull()) return null;
`

### Android 端
`kotlin
// 使用 optString 而非 getString，null 时返回默认值
val text = obj.optString("delta", "")
val intent = obj.optString("intent", "GENERAL_CHAT")
`

## 相关来源

architecture/system-overview.md
faq/how-to-deploy.md