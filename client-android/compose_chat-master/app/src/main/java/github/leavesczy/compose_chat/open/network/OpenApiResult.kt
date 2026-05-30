package github.leavesczy.compose_chat.open.network

sealed class OpenApiResult<out T> {

    data class Success<T>(val data: T) : OpenApiResult<T>()

    data class Failed(
        val code: String,
        val message: String,
        val traceId: String? = null
    ) : OpenApiResult<Nothing>()

}
