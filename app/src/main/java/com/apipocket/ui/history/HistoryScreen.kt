package com.apipocket.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apipocket.R
import com.apipocket.data.model.HistoryEntry
import com.apipocket.ui.common.ConfirmDialog
import com.apipocket.ui.common.EmptyState
import com.apipocket.ui.common.MethodChip
import com.apipocket.ui.common.apiErrorTypeText
import com.apipocket.ui.common.statusColor
import com.apipocket.util.formatDuration
import com.apipocket.util.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onOpenRequest: (HistoryEntry, Boolean) -> Unit
) {
    val history by vm.history.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true
            )

            if (history.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.history_empty_title),
                    message = stringResource(R.string.history_empty_message),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(history, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onOpen = { onOpenRequest(entry, false) },
                            onResend = { onOpenRequest(entry, true) },
                            onDelete = { pendingDelete = entry.id }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            message = stringResource(R.string.history_clear_message),
            confirmLabel = stringResource(R.string.action_clear),
            onConfirm = {
                vm.clear()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete_request_title),
            message = stringResource(R.string.delete_request_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.delete(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onResend: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MethodChip(entry.request.method)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.request.url.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.statusCode != null) {
                    Text(
                        text = "HTTP ${entry.statusCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor(entry.statusCode)
                    )
                } else if (entry.errorType != null) {
                    Text(
                        text = apiErrorTypeText(entry.errorType),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = formatDuration(entry.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onResend) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.action_resend),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.action_delete),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
