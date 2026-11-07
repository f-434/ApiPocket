package com.apipocket.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apipocket.data.local.AppRepository
import com.apipocket.data.model.ApiCollection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(private val repository: AppRepository) : ViewModel() {

    val collections: StateFlow<List<ApiCollection>> = repository.data
        .map { it.collections }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.data.value.collections)

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createCollection(name.trim()) }
    }

    fun rename(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameCollection(id, name.trim()) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteCollection(id) }
    }

    fun deleteRequest(collectionId: String, requestId: String) {
        viewModelScope.launch { repository.deleteSavedRequest(collectionId, requestId) }
    }
}
