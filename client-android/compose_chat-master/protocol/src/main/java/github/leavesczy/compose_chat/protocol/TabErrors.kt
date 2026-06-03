package github.leavesczy.compose_chat.protocol

object TabErrors {

    const val DUPLICATE_ID = 1001
    const val CONTAINER_VERSION_TOO_LOW = 1002
    const val MISSING_REQUIRED_FIELD = 1003
    const val LIFECYCLE_TIMEOUT = 1004
    const val LIFECYCLE_EXCEPTION = 1005
    const val PERMISSION_DENIED = 1006

    const val CALLBACK_TIMEOUT_MS = 5_000L

    fun description(code: Int): String = when (code) {
        DUPLICATE_ID -> "Tab ID 重复注册"
        CONTAINER_VERSION_TOO_LOW -> "容器版本过低"
        MISSING_REQUIRED_FIELD -> "缺失必填字段"
        LIFECYCLE_TIMEOUT -> "生命周期回调超时"
        LIFECYCLE_EXCEPTION -> "生命周期回调异常"
        PERMISSION_DENIED -> "权限不足"
        else -> "未知错误 ($code)"
    }

    fun shouldShowFallback(code: Int): Boolean {
        return code == LIFECYCLE_TIMEOUT || code == LIFECYCLE_EXCEPTION
    }

}

