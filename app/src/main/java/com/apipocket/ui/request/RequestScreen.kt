package com.apipocket.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apipocket.R
import com.apipocket.data.model.ApiCollection
import com.apipocket.data.model.HttpMethod
import com.apipocket.ui.common.methodColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    vm: RequestViewModel,
    collections: List<ApiCollection>,
    onOpenEnvironments: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var mainTab by rememberSaveable { mutableIntStateOf(0) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isSending, state.response, state.error) {
        if (state.isSending || state.response != null || state.error != null) {
            mainTab = 1
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            val message = when (event) {
                RequestEvent.Saved -> context.getString(R.string.saved_success)
                is RequestEvent.SaveFailed -> when (event.reason) {
                    SaveFailure.NAME_REQUIRED -> context.getString(R.string.save_need_name)
                    SaveFailure.COLLECTION_REQUIRED -> context.getString(R.string.save_need_collection)
                    SaveFailure.UNKNOWN -> context.getString(R.string.save_failed)
                }
                RequestEvent.JsonFormatted -> context.getString(R.string.json_formatted)
                RequestEvent.JsonFormatFailed -> context.getString(R.string.json_format_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    AssistChip(
                        onClick = onOpenEnvironments,
                        label = {
                            Text(
                                text = state.activeEnvironment?.name
                                    ?: stringResource(R.string.env_none),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Public,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.widthIn(max = 150.dp)
                    )
                    IconButton(onClick = vm::newRequest) {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = stringResource(R.string.action_new_request)
                        )
                    }
                    IconButton(
                        onClick = { showSaveDialog = true },
                        enabled = state.draft.request.url.isNotBlank()
                    ) {
                        Icon(
                            Icons.Outlined.Save,
                            contentDescription = stringResource(R.string.action_save)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            RequestBar(
                method = state.draft.request.method,
                url = state.draft.request.url,
                isSending = state.isSending,
                onMethodChange = vm::setMethod,
                onUrlChange = vm::setUrl,
                onSend = vm::send,
                onCancel = vm::cancelSend
            )

            if (state.unresolvedVariables.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.unresolved_vars,
                        state.unresolvedVariables.joinToString(", ")
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            TabRow(selectedTabIndex = mainTab) {
                Tab(
                    selected = mainTab == 0,
                    onClick = { mainTab = 0 },
                    text = { Text(stringResource(R.string.tab_edit)) }
                )
                Tab(
                    selected = mainTab == 1,
                    onClick = { mainTab = 1 },
                    text = { Text(stringResource(R.string.tab_response)) }
                )
            }

            if (mainTab == 0) {
                RequestEditor(
                    request = state.draft.request,
                    onUpdate = vm::updateRequest,
                    onFormatJson = vm::formatJson,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ResponsePanel(
                    response = state.response,
                    error = state.error,
                    durationMs = state.durationMs,
                    isSending = state.isSending,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveRequestDialog(
            draft = state.draft,
            collections = collections,
            onDismiss = { showSaveDialog = false },
            onSave = { collectionId, newCollectionName, requestName, updateExisting ->
                vm.saveToCollection(
                    collectionId = collectionId,
                    newCollectionName = newCollectionName,
                    requestName = requestName,
                    updateExisting = updateExisting
                )
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun RequestBar(
    method: HttpMethod,
    url: String,
    isSending: Boolean,
    onMethodChange: (HttpMethod) -> Unit,
    onUrlChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MethodDropdown(method = method, onSelect = onMethodChange)
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = stringResource(R.string.url_hint),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        FilledIconButton(
            onClick = { if (isSending) onCancel() else onSend() },
            enabled = isSending || url.isNotBlank()
        ) {
            Icon(
                imageVector = if (isSending) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(
                    if (isSending) R.string.action_cancel else R.string.action_send
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodDropdown(
    method: HttpMethod,
    onSelect: (HttpMethod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = method.name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .width(110.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = MaterialTheme.typography.labelLarge.copy(
                color = methodColor(method),
                fontWeight = FontWeight.Bold
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HttpMethod.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.name,
                            color = methodColor(item),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
