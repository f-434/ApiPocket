package com.apipocket.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apipocket.data.local.AppRepository
import com.apipocket.data.model.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: AppRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> =
        combine(repository.data, _query) { data, query ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                data.history
            } else {
                data.history.filter {
                    it.request.url.contains(trimmed, ignoreCase = true) ||
                        it.request.method.name.contains(trimmed, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.data.value.history)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteHistory(id) }
    }

    fun clear() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
