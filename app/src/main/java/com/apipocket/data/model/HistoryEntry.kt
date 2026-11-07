package com.apipocket.data.model

import com.apipocket.util.newId
import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
    val id: String = newId(),
    val request: RequestConfig,
    val timestamp: Long = System.currentTimeMillis(),
    val statusCode: Int? = null,
    val durationMs: Long = 0,
    val responseSize: Long = 0,
    val errorType: String? = null,
    val errorDetail: String? = null
)
