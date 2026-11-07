package com.apipocket.data.transfer

import android.content.Context
import android.net.Uri
import com.apipocket.data.local.AppJson
import com.apipocket.data.model.ApiCollection
import com.apipocket.data.model.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

sealed class ParsedImport {
    data class Own(val data: AppData) : ParsedImport()
    data class Postman(
        val collections: List<ApiCollection>,
        val requestCount: Int,
        val errors: List<String>
    ) : ParsedImport()
}

class TransferService(private val context: Context) {

    fun exportToJson(data: AppData): String = AppJson.encodeToString(AppData.serializer(), data)

    suspend fun readText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().decodeToString()
            } ?: error("Unable to open file")
        }
    }

    suspend fun writeText(uri: Uri, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(text.toByteArray())
            } ?: error("Unable to open file")
        }
    }

    fun parseImport(text: String): Result<ParsedImport> = runCatching {
        val element = Json.parseToJsonElement(text)
        val obj = element as? JsonObject ?: error("Unsupported JSON")
        if (obj.containsKey("info") && obj.containsKey("item")) {
            val result = PostmanImporter().import(obj)
            ParsedImport.Postman(result.collections, result.requestCount, result.errors)
        } else {
            val data = AppJson.decodeFromString(AppData.serializer(), text)
            ParsedImport.Own(data)
        }
    }
}
