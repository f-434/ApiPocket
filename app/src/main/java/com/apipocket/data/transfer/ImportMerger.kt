package com.apipocket.data.transfer

import com.apipocket.data.model.AppData
import com.apipocket.util.newId

object ImportMerger {

    fun merge(current: AppData, imported: AppData): AppData {
        val usedCollectionIds = current.collections.mapTo(mutableSetOf()) { it.id }
        val incomingCollections = imported.collections.map { collection ->
            if (collection.id in usedCollectionIds) {
                collection.copy(
                    id = newId(),
                    requests = collection.requests.map { it.copy(id = newId()) }
                )
            } else {
                usedCollectionIds += collection.id
                collection
            }
        }

        val usedEnvironmentIds = current.environments.mapTo(mutableSetOf()) { it.id }
        val incomingEnvironments = imported.environments.map { environment ->
            if (environment.id in usedEnvironmentIds) {
                environment.copy(id = newId())
            } else {
                usedEnvironmentIds += environment.id
                environment
            }
        }

        val limit = current.settings.historyLimit.coerceIn(10, 2000)
        val usedHistoryIds = current.history.mapTo(mutableSetOf()) { it.id }
        val incomingHistory = imported.history.map { entry ->
            if (entry.id in usedHistoryIds) {
                entry.copy(id = newId())
            } else {
                usedHistoryIds += entry.id
                entry
            }
        }
        val mergedHistory = (current.history + incomingHistory)
            .sortedByDescending { it.timestamp }
            .take(limit)

        return current.copy(
            collections = current.collections + incomingCollections,
            environments = current.environments + incomingEnvironments,
            history = mergedHistory
        )
    }
}
