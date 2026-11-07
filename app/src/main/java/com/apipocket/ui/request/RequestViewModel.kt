package com.apipocket.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apipocket.data.model.ApiEnvironment
import com.apipocket.data.model.AuthConfig
import com.apipocket.data.model.BodyConfig
import com.apipocket.data.model.EditorDraft
import com.apipocket.data.model.HistoryEntry
import com.apipocket.data.model.HttpMethod
import com.apipocket.data.model.KeyValue
import com.apipocket.data.model.RequestConfig
import com.apipocket.data.model.SavedRequest
import com.apipocket.data.remote.ApiError
import com.apipocket.data.remote.ApiResponse
import com.apipocket.data.remote.ApiResult
import com.apipocket.di.AppContainer
import com.apipocket.util.JsonFormatter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestUiState(
    val draft: EditorDraft = EditorDraft(),
    val response: ApiResponse? = null,
    val error: ApiError? = null,
    val durationMs: Long = 0,
    val isSending: Boolean = false,
    val activeEnvironment: ApiEnvironment? = null,
    val unresolvedVariables: Set<String> = emptySet()
)

enum class SaveFailure { NAME_REQUIRED, COLLECTION_REQUIRED, UNKNOWN }

sealed interface RequestEvent {
    data object Saved : RequestEvent
    data class SaveFailed(val reason: SaveFailure) : RequestEvent
    data object JsonFormatted : RequestEvent
    data object JsonFormatFailed : RequestEvent
}

@OptIn(FlowPreview::class)
class RequestViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.repository
    private val resolver = container.variableResolver

    private val _state = MutableStateFlow(RequestUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<RequestEvent> = _events.asSharedFlow()

    private val draftChanges = MutableSharedFlow<EditorDraft>(extraBufferCapacity = 64)
    private var sendJob: Job? = null

    init {
        viewModelScope.launch {
            var initialized = false
            repository.data.collect { data ->
                val environment = data.environments.firstOrNull { it.id == data.settings.activeEnvironmentId }
                if (!initialized) {
                    initialized = true
                    val draft = data.draft
                    _state.update {
                        it.copy(
                            draft = draft,
                            activeEnvironment = environment,
                            unresolvedVariables = unresolved(draft.request, environment)
                        )
                    }
                    draftChanges.tryEmit(draft)
                } else {
                    _state.update {
                        it.copy(
                            activeEnvironment = environment,
                            unresolvedVariables = unresolved(it.draft.request, environment)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            draftChanges.debounce(800).collect { draft ->
                repository.saveDraft(draft)
            }
        }
    }

    fun updateRequest(transform: (RequestConfig) -> RequestConfig) {
        updateDraft { it.copy(request = transform(it.request)) }
    }

    fun setMethod(method: HttpMethod) = updateRequest { it.copy(method = method) }

    fun setUrl(url: String) = updateRequest { it.copy(url = url) }

    fun updateParams(items: List<KeyValue>) = updateRequest { it.copy(params = items) }

    fun updateHeaders(items: List<KeyValue>) = updateRequest { it.copy(headers = items) }

    fun updateBody(transform: (BodyConfig) -> BodyConfig) =
        updateRequest { it.copy(body = transform(it.body)) }

    fun updateAuth(transform: (AuthConfig) -> AuthConfig) =
        updateRequest { it.copy(auth = transform(it.auth)) }

    fun setRequestName(name: String) = updateDraft { it.copy(name = name) }

    fun loadFromHistory(entry: HistoryEntry) {
        loadDraft(EditorDraft(request = entry.request, name = entry.request.suggestedName()))
    }

    fun loadFromSaved(collectionId: String, saved: SavedRequest) {
        loadDraft(
            EditorDraft(
                request = saved.request,
                savedRequestId = saved.id,
                collectionId = collectionId,
                name = saved.name
            )
        )
    }

    fun newRequest() {
        loadDraft(EditorDraft())
    }

    private fun loadDraft(draft: EditorDraft) {
        val environment = _state.value.activeEnvironment
        _state.update {
            it.copy(
                draft = draft,
                response = null,
                error = null,
                durationMs = 0,
                unresolvedVariables = unresolved(draft.request, environment)
            )
        }
        draftChanges.tryEmit(draft)
    }

    private fun updateDraft(transform: (EditorDraft) -> EditorDraft) {
        val environment = _state.value.activeEnvironment
        val current = _state.value.draft
        val updated = transform(current)
        _state.update {
            it.copy(draft = updated, unresolvedVariables = unresolved(updated.request, environment))
        }
        draftChanges.tryEmit(updated)
    }

    fun formatJson() {
        JsonFormatter.pretty(_state.value.draft.request.body.json)
            .onSuccess { pretty ->
                updateBody { it.copy(json = pretty) }
                _events.tryEmit(RequestEvent.JsonFormatted)
            }
            .onFailure {
                _events.tryEmit(RequestEvent.JsonFormatFailed)
            }
    }

    fun send() {
        if (_state.value.isSending) return
        val request = _state.value.draft.request
        if (request.url.isBlank()) {
            _state.update { it.copy(error = ApiError.InvalidUrl, response = null, durationMs = 0) }
            return
        }
        val environment = _state.value.activeEnvironment
        val variables = resolver.variablesOf(environment)
        val timeout = repository.data.value.settings.timeoutSeconds
        _state.update { it.copy(isSending = true, error = null) }
        sendJob = viewModelScope.launch {
            when (val result = container.executor.execute(request, variables, timeout)) {
                is ApiResult.Success -> {
                    val response = result.response
                    _state.update {
                        it.copy(
                            isSending = false,
                            response = response,
                            error = null,
                            durationMs = response.durationMs
                        )
                    }
                    repository.addHistory(
                        HistoryEntry(
                            request = request,
                            statusCode = response.statusCode,
                            durationMs = response.durationMs,
                            responseSize = response.bodyBytes
                        )
                    )
                }
                is ApiResult.Failure -> {
                    if (result.error == ApiError.Cancelled) {
                        _state.update { it.copy(isSending = false) }
                    } else {
                        _state.update {
                            it.copy(
                                isSending = false,
                                response = null,
                                error = result.error,
                                durationMs = result.durationMs
                            )
                        }
                        repository.addHistory(
                            HistoryEntry(
                                request = request,
                                durationMs = result.durationMs,
                                errorType = result.error.storageKey(),
                                errorDetail = result.error.storageDetail()
                            )
                        )
                    }
                }
            }
        }
    }

    fun cancelSend() {
        container.executor.cancelActiveCall()
        sendJob?.cancel()
        sendJob = null
        _state.update {
            it.copy(isSending = false, response = null, error = ApiError.Cancelled, durationMs = 0)
        }
    }

    fun saveToCollection(
        collectionId: String?,
        newCollectionName: String,
        requestName: String,
        updateExisting: Boolean
    ) {
        val name = requestName.trim()
        if (name.isEmpty()) {
            _events.tryEmit(RequestEvent.SaveFailed(SaveFailure.NAME_REQUIRED))
            return
        }
        if (collectionId == null && newCollectionName.isBlank()) {
            _events.tryEmit(RequestEvent.SaveFailed(SaveFailure.COLLECTION_REQUIRED))
            return
        }
        viewModelScope.launch {
            try {
                val targetId = if (newCollectionName.isNotBlank()) {
                    repository.createCollection(newCollectionName.trim())
                } else {
                    collectionId
                }
                if (targetId == null) {
                    _events.emit(RequestEvent.SaveFailed(SaveFailure.COLLECTION_REQUIRED))
                    return@launch
                }
                val draft = _state.value.draft
                val existingId = if (updateExisting) draft.savedRequestId else null
                val savedId = repository.saveRequest(targetId, name, draft.request, existingId)
                updateDraft { it.copy(savedRequestId = savedId, collectionId = targetId, name = name) }
                _events.emit(RequestEvent.Saved)
            } catch (e: Exception) {
                _events.emit(RequestEvent.SaveFailed(SaveFailure.UNKNOWN))
            }
        }
    }

    private fun unresolved(request: RequestConfig, environment: ApiEnvironment?): Set<String> {
        val variables = resolver.variablesOf(environment)
        val texts = buildList {
            add(request.url)
            request.params.forEach { add(it.key); add(it.value) }
            request.headers.forEach { add(it.key); add(it.value) }
            add(request.body.json)
            add(request.body.raw)
            request.body.formData.forEach { add(it.key); add(it.value) }
            request.body.urlEncoded.forEach { add(it.key); add(it.value) }
            add(request.auth.token)
            add(request.auth.username)
            add(request.auth.password)
            add(request.auth.keyName)
            add(request.auth.keyValue)
        }
        val result = mutableSetOf<String>()
        texts.forEach { result += resolver.unresolved(it, variables) }
        return result
    }

    private fun ApiError.storageKey(): String = when (this) {
        ApiError.InvalidUrl -> "invalid_url"
        ApiError.Dns -> "dns"
        ApiError.Connect -> "connect"
        ApiError.Timeout -> "timeout"
        ApiError.Ssl -> "ssl"
        ApiError.Cancelled -> "cancelled"
        is ApiError.FileAccess -> "file_access"
        is ApiError.InvalidHeader -> "invalid_header"
        is ApiError.Unknown -> "unknown"
    }

    private fun ApiError.storageDetail(): String? = when (this) {
        is ApiError.FileAccess -> name
        is ApiError.InvalidHeader -> detail
        is ApiError.Unknown -> detail
        else -> null
    }
}
