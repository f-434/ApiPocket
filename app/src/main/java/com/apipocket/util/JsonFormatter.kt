package com.apipocket.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object JsonFormatter {

    private val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun pretty(text: String): Result<String> = runCatching {
        val element: JsonElement = Json.parseToJsonElement(text)
        prettyJson.encodeToString(JsonElement.serializer(), element)
    }

    fun isValid(text: String): Boolean = runCatching {
        Json.parseToJsonElement(text)
    }.isSuccess

    fun looksLikeJson(text: String, contentType: String?): Boolean {
        if (contentType?.contains("json", ignoreCase = true) == true) return true
        val trimmed = text.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }
}
