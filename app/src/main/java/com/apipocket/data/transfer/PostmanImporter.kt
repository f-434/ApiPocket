package com.apipocket.data.transfer

import com.apipocket.data.model.ApiCollection
import com.apipocket.data.model.ApiKeyLocation
import com.apipocket.data.model.AuthConfig
import com.apipocket.data.model.AuthType
import com.apipocket.data.model.BodyConfig
import com.apipocket.data.model.BodyType
import com.apipocket.data.model.FormField
import com.apipocket.data.model.HttpMethod
import com.apipocket.data.model.KeyValue
import com.apipocket.data.model.RequestConfig
import com.apipocket.data.model.SavedRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

data class PostmanImport(
    val collections: List<ApiCollection>,
    val requestCount: Int,
    val errors: List<String>
)

class PostmanImporter {

    fun import(root: JsonObject): PostmanImport {
        val info = root["info"] as? JsonObject
        val collectionName = info?.get("name").str() ?: "Postman"
        val items = root["item"] as? JsonArray ?: JsonArray(emptyList())
        val collections = mutableListOf<ApiCollection>()
        val rootRequests = mutableListOf<SavedRequest>()
        val errors = mutableListOf<String>()

        items.forEach { item ->
            val obj = item as? JsonObject ?: return@forEach
            val name = obj["name"].str() ?: "Untitled"
            val children = obj["item"] as? JsonArray
            val requestElement = obj["request"]
            if (children != null) {
                val requests = mutableListOf<SavedRequest>()
                collectItems(children, "", requests, errors)
                collections += ApiCollection(name = name, requests = requests)
            } else if (requestElement != null) {
                parseRequest(name, requestElement, errors)?.let { rootRequests += it }
            }
        }

        if (rootRequests.isNotEmpty() || collections.isEmpty()) {
            collections.add(0, ApiCollection(name = collectionName, requests = rootRequests))
        }
        return PostmanImport(collections, collections.sumOf { it.requests.size }, errors)
    }

    private fun collectItems(
        items: JsonArray,
        prefix: String,
        output: MutableList<SavedRequest>,
        errors: MutableList<String>
    ) {
        items.forEach { item ->
            val obj = item as? JsonObject ?: return@forEach
            val name = obj["name"].str() ?: "Untitled"
            val children = obj["item"] as? JsonArray
            if (children != null) {
                collectItems(children, "$prefix$name / ", output, errors)
            } else {
                obj["request"]?.let { request ->
                    parseRequest("$prefix$name", request, errors)?.let { output += it }
                }
            }
        }
    }

    private fun parseRequest(name: String, element: JsonElement, errors: MutableList<String>): SavedRequest? {
        return try {
            val primitive = element as? JsonPrimitive
            if (primitive != null && primitive.isString) {
                return SavedRequest(
                    name = name.ifBlank { "Untitled" },
                    request = RequestConfig(url = primitive.content)
                )
            }
            val obj = element as? JsonObject ?: return null
            val method = obj["method"].str()
                ?.uppercase()
                ?.let { runCatching { HttpMethod.valueOf(it) }.getOrNull() }
                ?: HttpMethod.GET
            val (url, params) = parseUrl(obj["url"])
            val headers = parseHeaders(obj["header"])
            val body = parseBody(obj["body"], headers)
            val auth = parseAuth(obj["auth"])
            SavedRequest(
                name = name.ifBlank { "Untitled" },
                request = RequestConfig(
                    method = method,
                    url = url,
                    params = params,
                    headers = headers,
                    body = body,
                    auth = auth
                )
            )
        } catch (e: Exception) {
            errors += "$name: ${e.message ?: "invalid request"}"
            null
        }
    }

    private fun parseUrl(element: JsonElement?): Pair<String, List<KeyValue>> {
        if (element == null) return "" to emptyList()
        val primitive = element as? JsonPrimitive
        if (primitive != null && primitive.isString) return primitive.content to emptyList()
        val obj = element as? JsonObject ?: return "" to emptyList()

        val raw = obj["raw"].str()
        val rawHasQuery = raw?.contains('?') == true
        val params = mutableListOf<KeyValue>()
        if (!rawHasQuery) {
            (obj["query"] as? JsonArray)?.forEach { query ->
                val q = query as? JsonObject ?: return@forEach
                val key = q["key"].str() ?: return@forEach
                params += KeyValue(
                    key = key,
                    value = q["value"].str() ?: "",
                    enabled = q["disabled"].bool() != true
                )
            }
        }
        if (!raw.isNullOrBlank()) return raw to params

        val protocol = obj["protocol"].str() ?: "https"
        val host = joinArrayOrString(obj["host"], ".")
        val path = joinArrayOrString(obj["path"], "/")
        val url = if (path.isBlank()) "$protocol://$host" else "$protocol://$host/$path"
        return url to params
    }

    private fun joinArrayOrString(element: JsonElement?, separator: String): String {
        return when (element) {
            is JsonArray -> element.mapNotNull { it.str() }.joinToString(separator)
            is JsonPrimitive -> element.contentOrNull ?: ""
            else -> ""
        }
    }

    private fun parseHeaders(element: JsonElement?): List<KeyValue> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val key = obj["key"].str() ?: return@mapNotNull null
            KeyValue(
                key = key,
                value = obj["value"].str() ?: "",
                enabled = obj["disabled"].bool() != true
            )
        }
    }

    private fun parseBody(element: JsonElement?, headers: List<KeyValue>): BodyConfig {
        val obj = element as? JsonObject ?: return BodyConfig()
        return when (obj["mode"].str()) {
            "raw" -> {
                val raw = obj["raw"].str() ?: ""
                val language = (obj["options"] as? JsonObject)
                    ?.get("raw")?.let { it as? JsonObject }
                    ?.get("language").str()
                val contentType = headers.firstOrNull { it.key.equals("Content-Type", true) }?.value
                if (language.equals("json", true) || contentType?.contains("json", true) == true) {
                    BodyConfig(type = BodyType.JSON, json = raw)
                } else {
                    BodyConfig(
                        type = BodyType.RAW,
                        raw = raw,
                        rawContentType = contentType?.substringBefore(';')?.trim() ?: "text/plain"
                    )
                }
            }
            "urlencoded" -> BodyConfig(
                type = BodyType.X_WWW_FORM_URLENCODED,
                urlEncoded = parseKeyValues(obj["urlencoded"])
            )
            "formdata" -> BodyConfig(
                type = BodyType.FORM_DATA,
                formData = parseFormData(obj["formdata"])
            )
            else -> BodyConfig()
        }
    }

    private fun parseKeyValues(element: JsonElement?): List<KeyValue> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val key = obj["key"].str() ?: return@mapNotNull null
            KeyValue(
                key = key,
                value = obj["value"].str() ?: "",
                enabled = obj["disabled"].bool() != true
            )
        }
    }

    private fun parseFormData(element: JsonElement?): List<FormField> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val key = obj["key"].str() ?: return@mapNotNull null
            val type = obj["type"].str()
            val srcElement = obj["src"]
            val src = when (srcElement) {
                is JsonPrimitive -> srcElement.contentOrNull
                is JsonArray -> srcElement.firstOrNull().str()
                else -> null
            }
            val isFile = type == "file" || src != null
            FormField(
                key = key,
                value = obj["value"].str() ?: "",
                enabled = obj["disabled"].bool() != true,
                isFile = isFile,
                fileName = src?.substringAfterLast('/')?.substringAfterLast('\\')?.substringBefore('?'),
                uri = null
            )
        }
    }

    private fun parseAuth(element: JsonElement?): AuthConfig {
        val obj = element as? JsonObject ?: return AuthConfig()
        fun values(key: String): Map<String, String> {
            val array = obj[key] as? JsonArray ?: return emptyMap()
            return array.mapNotNull { it as? JsonObject }.mapNotNull { entry ->
                val entryKey = entry["key"].str() ?: return@mapNotNull null
                entryKey to (entry["value"].str() ?: "")
            }.toMap()
        }

        return when (obj["type"].str()) {
            "bearer" -> AuthConfig(type = AuthType.BEARER, token = values("bearer")["token"] ?: "")
            "basic" -> {
                val v = values("basic")
                AuthConfig(type = AuthType.BASIC, username = v["username"] ?: "", password = v["password"] ?: "")
            }
            "apikey" -> {
                val v = values("apikey")
                AuthConfig(
                    type = AuthType.API_KEY,
                    keyName = v["key"] ?: "",
                    keyValue = v["value"] ?: "",
                    keyLocation = if (v["in"]?.equals("query", true) == true) {
                        ApiKeyLocation.QUERY
                    } else {
                        ApiKeyLocation.HEADER
                    }
                )
            }
            else -> AuthConfig()
        }
    }

    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement?.bool(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
}
