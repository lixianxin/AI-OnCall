# 生成 Repository — AI OnCall 项目风格

Android 端数据层使用 Repository 模式 + Flow 异步流。

## 参考代码

`kotlin
// client-android/.../open/repository/OpenOnCallRepository.kt
class OpenOnCallRepository(
    private val apiClient: OnCallApiClient = OnCallApiClient()
) {
    fun stream(message: String): Flow<OnCallStreamEvent> {
        val body = JSONObject().apply {
            put(""message"", message)
            put(""conversationId"", ""android-"" + System.currentTimeMillis())
        }
        return flow {
            apiClient.ssePost(path = API_PATH, json = body).collect { line ->
                when (line) {
                    is OnCallSseLine.Text -> emit(parseSseLine(line.line))
                    is OnCallSseLine.Error -> emit(error)
                }
            }
        }
    }
}
`

## 生成模板

`kotlin
class Repository(
    private val apiClient: OnCallApiClient = OnCallApiClient()
) {
    fun fetchData(param: String): Flow<Result> = flow {
        val body = JSONObject().apply {
            put(""param"", param)
        }
        apiClient.ssePost(path = API_PATH, json = body).collect { line ->
            // 解析并 emit
        }
    }
}
`

## 相关来源

architecture/system-overview.md → repository/
api/sse-protocol.md