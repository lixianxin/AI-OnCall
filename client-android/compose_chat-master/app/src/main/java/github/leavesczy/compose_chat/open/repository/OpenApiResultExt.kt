package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.network.OpenApiResult

inline fun <T, R> OpenApiResult<T>.map(transform: (T) -> R): OpenApiResult<R> {
    return when (this) {
        is OpenApiResult.Success -> runCatching {
            OpenApiResult.Success(data = transform(data))
        }.getOrElse { error ->
            OpenApiResult.Failed(code = "PARSE_ERROR", message = error.message ?: "Parse error")
        }

        is OpenApiResult.Failed -> this
    }
}
