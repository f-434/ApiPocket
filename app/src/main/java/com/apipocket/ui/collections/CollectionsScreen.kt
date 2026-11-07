package com.apipocket.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apipocket.R
import com.apipocket.data.model.ApiCollection
import com.apipocket.ui.common.ConfirmDialog
import com.apipocket.ui.common.EmptyState
import com.apipocket.ui.common.TextInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    vm: CollectionsViewModel,
    onOpenCollection: (String) -> Unit
) {
    val collections by vm.collections.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renameTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collections_title)) },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.collection_new)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (collections.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.collections_empty_title),
                message = stringResource(R.string.collections_empty_message),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(collections, key = { it.id }) { collection ->
                    CollectionCard(
                        collection = collection,
                        onOpen = { onOpenCollection(collection.id) },
                        onRename = { renameTargetId = collection.id },
                        onDelete = { deleteTargetId = collection.id }
                    )
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = stringResource(R.string.collection_new),
            label = stringResource(R.string.collection_name),
            confirmLabel = stringResource(R.string.action_add),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                vm.create(name)
                showCreate = false
            }
        )
    }

    val renameTarget = collections.firstOrNull { it.id == renameTargetId }
    if (renameTarget != null) {
        TextInputDialog(
            title = stringResource(R.string.collection_rename),
            label = stringResource(R.string.collection_name),
            initialValue = renameTarget.name,
            confirmLabel = stringResource(R.string.action_save),
            onDismiss = { renameTargetId = null },
            onConfirm = { name ->
                vm.rename(renameTarget.id, name)
                renameTargetId = null
            }
        )
    }

    if (deleteTargetId != null) {
        ConfirmDialog(
            title = stringResource(R.string.collection_delete_title),
            message = stringResource(R.string.collection_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deleteTargetId?.let(vm::delete)
                deleteTargetId = null
            },
            onDismiss = { deleteTargetId = null }
        )
    }
}

@Composable
private fun CollectionCard(
    collection: ApiCollection,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.collection_requests_count, collection.requests.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
