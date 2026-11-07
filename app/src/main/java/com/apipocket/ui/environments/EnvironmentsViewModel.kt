package com.apipocket.ui.environments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apipocket.data.local.AppRepository
import com.apipocket.data.model.ApiEnvironment
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EnvironmentEvent {
    data class Created(val id: String) : EnvironmentEvent
}

class EnvironmentsViewModel(private val repository: AppRepository) : ViewModel() {

    val environments: StateFlow<List<ApiEnvironment>> = repository.data
        .map { it.environments }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.data.value.environments)

    val activeEnvironmentId: StateFlow<String?> = repository.data
        .map { it.settings.activeEnvironmentId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.data.value.settings.activeEnvironmentId)

    private val _events = MutableSharedFlow<EnvironmentEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<EnvironmentEvent> = _events.asSharedFlow()

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createEnvironment(name.trim())
            _events.emit(EnvironmentEvent.Created(id))
        }
    }

    fun save(environment: ApiEnvironment) {
        if (environment.name.isBlank()) return
        viewModelScope.launch { repository.updateEnvironment(environment.copy(name = environment.name.trim())) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteEnvironment(id) }
    }

    fun setActive(id: String?) {
        viewModelScope.launch { repository.setActiveEnvironment(id) }
    }
}
