package com.apipocket.ui.collections

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.apipocket.data.model.SavedRequest
import com.apipocket.ui.common.ConfirmDialog
import com.apipocket.ui.common.EmptyState
import com.apipocket.ui.common.MethodChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    vm: CollectionsViewModel,
    collectionId: String,
    onBack: () -> Unit,
    onOpenRequest: (String, SavedRequest) -> Unit
) {
    val collections by vm.collections.collectAsStateWithLifecycle()
    val collection = collections.firstOrNull { it.id == collectionId }
    var showDeleteCollection by rememberSaveable { mutableStateOf(false) }
    var deleteRequestId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = collection?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (collection != null) {
                        IconButton(onClick = { showDeleteCollection = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (collection == null) {
            EmptyState(
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.collections_empty_title),
                message = stringResource(R.string.collections_empty_message),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        } else if (collection.requests.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.collection_empty_title),
                message = stringResource(R.string.collection_empty_message),
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
                items(collection.requests, key = { it.id }) { saved ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRequest(collection.id, saved) }
                            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MethodChip(saved.request.method)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = saved.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = saved.request.url.ifBlank { "-" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { deleteRequestId = saved.id }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDeleteCollection) {
        ConfirmDialog(
            title = stringResource(R.string.collection_delete_title),
            message = stringResource(R.string.collection_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.delete(collectionId)
                showDeleteCollection = false
                onBack()
            },
            onDismiss = { showDeleteCollection = false }
        )
    }

    deleteRequestId?.let { requestId ->
        ConfirmDialog(
            title = stringResource(R.string.saved_request_delete_title),
            message = stringResource(R.string.saved_request_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.deleteRequest(collectionId, requestId)
                deleteRequestId = null
            },
            onDismiss = { deleteRequestId = null }
        )
    }
}
