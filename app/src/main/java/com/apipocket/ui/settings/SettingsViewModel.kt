package com.apipocket.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apipocket.data.local.AppRepository
import com.apipocket.data.model.AppSettings
import com.apipocket.data.model.ThemeMode
import com.apipocket.data.transfer.ParsedImport
import com.apipocket.di.AppContainer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface SettingsEvent {
    data object ExportSuccess : SettingsEvent
    data object ExportFailed : SettingsEvent
    data object ImportFailed : SettingsEvent
    data class ImportOwnDone(val collections: Int, val environments: Int) : SettingsEvent
    data class ImportPostmanDone(val collections: Int, val requests: Int, val skipped: Int) : SettingsEvent
    data object ClearDone : SettingsEvent
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val repository: AppRepository = container.repository

    val settings: StateFlow<AppSettings> = repository.data
        .map { it.settings }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.data.value.settings)

    private val _pendingImport = MutableStateFlow<ParsedImport.Own?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun setTheme(mode: ThemeMode) = update { it.copy(theme = mode) }

    fun setTimeout(seconds: Int) = update { it.copy(timeoutSeconds = seconds.coerceIn(1, 300)) }

    fun setHistoryLimit(limit: Int) = update { it.copy(historyLimit = limit.coerceIn(10, 2000)) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun exportFileName(): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        return "apipocket-backup-$stamp.json"
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val json = container.transferService.exportToJson(repository.data.value)
            container.transferService.writeText(uri, json)
                .onSuccess { _events.emit(SettingsEvent.ExportSuccess) }
                .onFailure { _events.emit(SettingsEvent.ExportFailed) }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val text = container.transferService.readText(uri).getOrElse {
                _events.emit(SettingsEvent.ImportFailed)
                return@launch
            }
            when (val parsed = container.transferService.parseImport(text).getOrElse {
                _events.emit(SettingsEvent.ImportFailed)
                return@launch
            }) {
                is ParsedImport.Own -> _pendingImport.value = parsed
                is ParsedImport.Postman -> {
                    repository.addCollections(parsed.collections)
                    _events.emit(
                        SettingsEvent.ImportPostmanDone(
                            collections = parsed.collections.size,
                            requests = parsed.requestCount,
                            skipped = parsed.errors.size
                        )
                    )
                }
            }
        }
    }

    fun applyOwnImport(replace: Boolean) {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            if (replace) {
                repository.replaceAll(pending.data)
            } else {
                repository.mergeImport(pending.data)
            }
            _events.emit(
                SettingsEvent.ImportOwnDone(
                    collections = pending.data.collections.size,
                    environments = pending.data.environments.size
                )
            )
        }
    }

    fun cancelPendingImport() {
        _pendingImport.value = null
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _events.emit(SettingsEvent.ClearDone)
        }
    }
}
