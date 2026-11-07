package com.apipocket.data.local

import androidx.datastore.core.DataStore
import com.apipocket.data.model.ApiCollection
import com.apipocket.data.model.ApiEnvironment
import com.apipocket.data.model.AppData
import com.apipocket.data.model.AppSettings
import com.apipocket.data.model.EditorDraft
import com.apipocket.data.model.HistoryEntry
import com.apipocket.data.model.RequestConfig
import com.apipocket.data.model.SavedRequest
import com.apipocket.data.transfer.ImportMerger
import com.apipocket.util.newId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppRepository(
    private val store: DataStore<AppData>,
    scope: CoroutineScope
) {
    val data: StateFlow<AppData> = store.data.stateIn(scope, SharingStarted.Eagerly, AppData())

    private suspend fun update(transform: (AppData) -> AppData) {
        store.updateData(transform)
    }

    // ---- History ----

    suspend fun addHistory(entry: HistoryEntry) = update { current ->
        val limit = current.settings.historyLimit.coerceIn(10, 2000)
        current.copy(history = (listOf(entry) + current.history).take(limit))
    }

    suspend fun deleteHistory(id: String) = update { current ->
        current.copy(history = current.history.filterNot { it.id == id })
    }

    suspend fun clearHistory() = update { it.copy(history = emptyList()) }

    // ---- Collections ----

    suspend fun createCollection(name: String): String {
        val id = newId()
        update { current -> current.copy(collections = current.collections + ApiCollection(id = id, name = name)) }
        return id
    }

    suspend fun renameCollection(id: String, name: String) = update { current ->
        current.copy(collections = current.collections.map { if (it.id == id) it.copy(name = name) else it })
    }

    suspend fun deleteCollection(id: String) = update { current ->
        current.copy(collections = current.collections.filterNot { it.id == id })
    }

    /**
     * Inserts a request into [collectionId], or updates the existing request with [existingId].
     * If [existingId] is set, the request is moved to [collectionId] when it was stored elsewhere.
     */
    suspend fun saveRequest(
        collectionId: String,
        name: String,
        request: RequestConfig,
        existingId: String? = null
    ): String {
        val id = existingId ?: newId()
        val now = System.currentTimeMillis()
        update { current ->
            if (current.collections.none { it.id == collectionId }) return@update current
            val existing = current.collections
                .asSequence()
                .flatMap { it.requests.asSequence() }
                .firstOrNull { it.id == id }
            val cleaned = current.collections.map { collection ->
                collection.copy(requests = collection.requests.filterNot { it.id == id })
            }
            val collections = cleaned.map { collection ->
                if (collection.id != collectionId) {
                    collection
                } else {
                    collection.copy(
                        requests = collection.requests + SavedRequest(
                            id = id,
                            name = name,
                            request = request,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
            }
            current.copy(collections = collections)
        }
        return id
    }

    suspend fun deleteSavedRequest(collectionId: String, requestId: String) = update { current ->
        current.copy(
            collections = current.collections.map { collection ->
                if (collection.id == collectionId) {
                    collection.copy(requests = collection.requests.filterNot { it.id == requestId })
                } else {
                    collection
                }
            }
        )
    }

    suspend fun addCollections(collections: List<ApiCollection>) = update { current ->
        current.copy(collections = current.collections + collections)
    }

    // ---- Environments ----

    suspend fun createEnvironment(name: String): String {
        val id = newId()
        update { current ->
            current.copy(environments = current.environments + ApiEnvironment(id = id, name = name))
        }
        return id
    }

    suspend fun updateEnvironment(environment: ApiEnvironment) = update { current ->
        current.copy(environments = current.environments.map { if (it.id == environment.id) environment else it })
    }

    suspend fun deleteEnvironment(id: String) = update { current ->
        current.copy(
            environments = current.environments.filterNot { it.id == id },
            settings = if (current.settings.activeEnvironmentId == id) {
                current.settings.copy(activeEnvironmentId = null)
            } else {
                current.settings
            }
        )
    }

    suspend fun setActiveEnvironment(id: String?) = update { current ->
        current.copy(settings = current.settings.copy(activeEnvironmentId = id))
    }

    // ---- Settings ----

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = update { current ->
        current.copy(settings = transform(current.settings))
    }

    // ---- Draft ----

    suspend fun saveDraft(draft: EditorDraft) = update { current ->
        current.copy(draft = draft)
    }

    // ---- Import / reset ----

    suspend fun replaceAll(newData: AppData) = update {
        newData.copy(schemaVersion = 1, history = newData.history.take(newData.settings.historyLimit.coerceIn(10, 2000)))
    }

    suspend fun mergeImport(imported: AppData) = update { current ->
        ImportMerger.merge(current, imported)
    }

    suspend fun clearAll() = update { AppData() }
}
