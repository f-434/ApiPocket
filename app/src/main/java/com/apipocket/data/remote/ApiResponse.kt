package com.apipocket.data.remote

sealed class ApiError {
    data object InvalidUrl : ApiError()
    data object Dns : ApiError()
    data object Connect : ApiError()
    data object Timeout : ApiError()
    data object Ssl : ApiError()
    data object Cancelled : ApiError()
    data class FileAccess(val name: String?) : ApiError()
    data class InvalidHeader(val detail: String) : ApiError()
    data class Unknown(val detail: String?) : ApiError()
}

data class ApiResponse(
    val statusCode: Int,
    val statusMessage: String,
    val protocol: String,
    val headers: List<Pair<String, String>>,
    val contentType: String?,
    val body: String,
    val bodyBytes: Long,
    val truncated: Boolean,
    val durationMs: Long
)

sealed class ApiResult {
    data class Success(val response: ApiResponse) : ApiResult()
    data class Failure(val error: ApiError, val durationMs: Long) : ApiResult()
}
