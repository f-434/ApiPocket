package com.apipocket

import com.apipocket.data.model.ApiCollection
import com.apipocket.data.model.ApiEnvironment
import com.apipocket.data.model.AppData
import com.apipocket.data.model.HistoryEntry
import com.apipocket.data.model.RequestConfig
import com.apipocket.data.transfer.ImportMerger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportMergerTest {

    private fun history(id: String, timestamp: Long) = HistoryEntry(
        id = id,
        request = RequestConfig(url = "https://example.com/$id"),
        timestamp = timestamp
    )

    @Test
    fun regeneratesDuplicateHistoryIds() {
        val sharedId = "same-history-id"
        val current = AppData(history = listOf(history(sharedId, 1_000)))
        val imported = AppData(
            history = listOf(
                history(sharedId, 2_000),
                history("new-history-id", 3_000)
            )
        )

        val merged = ImportMerger.merge(current, imported)

        assertEquals(3, merged.history.size)
        assertEquals(3, merged.history.map { it.id }.toSet().size)
        assertTrue(merged.history.any { it.id == sharedId })
        assertTrue(merged.history.any { it.id == "new-history-id" })
    }

    @Test
    fun regeneratesDuplicateCollectionAndEnvironmentIds() {
        val current = AppData(
            collections = listOf(ApiCollection(id = "c1", name = "Mine")),
            environments = listOf(ApiEnvironment(id = "e1", name = "Mine"))
        )
        val imported = AppData(
            collections = listOf(
                ApiCollection(id = "c1", name = "Imported"),
                ApiCollection(id = "c1", name = "Imported duplicate")
            ),
            environments = listOf(ApiEnvironment(id = "e1", name = "Imported"))
        )

        val merged = ImportMerger.merge(current, imported)

        assertEquals(3, merged.collections.size)
        assertEquals(3, merged.collections.map { it.id }.toSet().size)
        assertEquals(2, merged.environments.size)
        assertEquals(2, merged.environments.map { it.id }.toSet().size)
    }

    @Test
    fun keepsNewestHistoryWithinLimit() {
        val current = AppData(history = listOf(history("old", 1)))
        val imported = AppData(history = listOf(history("new", 2)))

        val merged = ImportMerger.merge(current, imported)

        assertEquals("new", merged.history.first().id)
    }
}
