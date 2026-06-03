package github.leavesczy.compose_chat.open.config

/**
 * AI OnCall 独立配置中心（单例）
 *
 * 本配置仅服务于 AI Service，与 [OpenApiConfig]（Container Server 配置）完全解耦。
 * 所有 AI 通信参数在此集中管理，不依赖也不影响 Container Server。
 *
 * AI Service 核心能力：
 * - POST /api/chat/stream（SSE 流式聊天）
 * - SSE 长连接最长持续 5 分钟
 * - 连接超时 30 秒，写入超时 30 秒
 *
 * 多环境扩展说明：
 * - 当前为单环境常量模式。后续如需多环境切换（开发/测试/生产），
 *   可在本 object 内引入环境枚举 + 工厂方法，对外仍暴露 const val 语义。
 * - 示例扩展方向：Environment enum { DEV, STAGING, PROD }，根据
 *   BuildConfig 或启动参数选择对应 URL。
 */
object OnCallConfig {

    /** AI Service 基础地址，格式如 "http://ip:port"，不包含尾部斜杠 */
    const val AI_SERVICE_BASE_URL = "http://121.40.241.161:8081"

    /** 流式聊天接口路径，相对于 [AI_SERVICE_BASE_URL] */
    const val STREAM_CHAT_API = "/api/chat/stream"

    /** TCP 连接超时，单位：秒 */
    const val CONNECT_TIMEOUT_SECONDS: Long = 30L

    /** SSE 长连接读取超时，单位：分钟（匹配 LLM 推理最长 5 分钟） */
    const val READ_TIMEOUT_MINUTES: Long = 5L

    /** 写入超时，单位：秒 */
    const val WRITE_TIMEOUT_SECONDS: Long = 30L

}