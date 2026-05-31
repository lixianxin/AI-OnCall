package github.leavesczy.compose_chat.open.config

object OpenApiConfig {

    const val CONTAINER_VERSION = 1

    const val DEFAULT_BASE_URL = "http://121.40.241.161:8080"

    // 当前阶段服务端 mock 已部署到云端。这里集中保存联调用账号，后续接入真实账号体系时只需要替换登录来源。
    const val DEFAULT_ACCOUNT = "opentab-admin"

    const val DEFAULT_PASSWORD = "admin123"

}
