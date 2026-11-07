package com.apipocket.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.apipocket.data.model.AppData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
    isLenient = true
}

object AppDataSerializer : Serializer<AppData> {
    override val defaultValue: AppData = AppData()

    override suspend fun readFrom(input: InputStream): AppData {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            AppJson.decodeFromString(AppData.serializer(), bytes.decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Failed to decode app data", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Failed to decode app data", e)
        }
    }

    override suspend fun writeTo(t: AppData, output: OutputStream) {
        output.write(AppJson.encodeToString(AppData.serializer(), t).encodeToByteArray())
    }
}

val Context.appDataStore: DataStore<AppData> by dataStore(
    fileName = "apipocket.json",
    serializer = AppDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AppData() }
)
