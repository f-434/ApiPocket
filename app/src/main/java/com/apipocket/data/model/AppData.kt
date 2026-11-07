package com.apipocket.data.model

import com.apipocket.util.newId
import kotlinx.serialization.Serializable

@Serializable
data class ApiCollection(
    val id: String = newId(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val requests: List<SavedRequest> = emptyList()
)

@Serializable
data class SavedRequest(
    val id: String = newId(),
    val name: String,
    val request: RequestConfig,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ApiEnvironment(
    val id: String = newId(),
    val name: String,
    val variables: List<KeyValue> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val activeEnvironmentId: String? = null,
    val timeoutSeconds: Int = 30,
    val historyLimit: Int = 200,
    val theme: ThemeMode = ThemeMode.SYSTEM
)

@Serializable
data class EditorDraft(
    val request: RequestConfig = RequestConfig(),
    val savedRequestId: String? = null,
    val collectionId: String? = null,
    val name: String = ""
)

@Serializable
data class AppData(
    val schemaVersion: Int = 1,
    val collections: List<ApiCollection> = emptyList(),
    val environments: List<ApiEnvironment> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val draft: EditorDraft = EditorDraft()
)
