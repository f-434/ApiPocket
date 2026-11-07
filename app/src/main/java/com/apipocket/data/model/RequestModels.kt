package com.apipocket.data.model

import com.apipocket.util.newId
import kotlinx.serialization.Serializable

@Serializable
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

@Serializable
data class KeyValue(
    val id: String = newId(),
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true
)

@Serializable
enum class BodyType { NONE, JSON, RAW, FORM_DATA, X_WWW_FORM_URLENCODED }

@Serializable
data class FormField(
    val id: String = newId(),
    val key: String = "",
    val value: String = "",
    val enabled: Boolean = true,
    val isFile: Boolean = false,
    val fileName: String? = null,
    val uri: String? = null
)

@Serializable
data class BodyConfig(
    val type: BodyType = BodyType.NONE,
    val json: String = "{\n  \n}",
    val raw: String = "",
    val rawContentType: String = "text/plain",
    val formData: List<FormField> = emptyList(),
    val urlEncoded: List<KeyValue> = emptyList()
)

@Serializable
enum class AuthType { NONE, BEARER, BASIC, API_KEY }

@Serializable
enum class ApiKeyLocation { HEADER, QUERY }

@Serializable
data class AuthConfig(
    val type: AuthType = AuthType.NONE,
    val token: String = "",
    val username: String = "",
    val password: String = "",
    val keyName: String = "",
    val keyValue: String = "",
    val keyLocation: ApiKeyLocation = ApiKeyLocation.HEADER
)

@Serializable
data class RequestConfig(
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val params: List<KeyValue> = emptyList(),
    val headers: List<KeyValue> = emptyList(),
    val body: BodyConfig = BodyConfig(),
    val auth: AuthConfig = AuthConfig()
) {
    /** A short, human friendly label derived from the URL, used as default request name. */
    fun suggestedName(): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        val withoutQuery = trimmed.substringBefore('?')
        val last = withoutQuery.trimEnd('/').substringAfterLast('/')
        return last.ifBlank { withoutQuery }
    }
}
