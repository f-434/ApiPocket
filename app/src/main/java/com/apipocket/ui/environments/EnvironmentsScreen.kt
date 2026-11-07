package com.apipocket.ui.environments

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.apipocket.data.model.ApiEnvironment
import com.apipocket.ui.common.ConfirmDialog
import com.apipocket.ui.common.EmptyState
import com.apipocket.ui.common.TextInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentsScreen(
    vm: EnvironmentsViewModel,
    onOpenEnvironment: (String) -> Unit
) {
    val environments by vm.environments.collectAsStateWithLifecycle()
    val activeId by vm.activeEnvironmentId.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is EnvironmentEvent.Created) {
                onOpenEnvironment(event.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.environments_title)) },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.environment_new)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (environments.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Public,
                title = stringResource(R.string.environments_empty_title),
                message = stringResource(R.string.environments_empty_message),
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
                items(environments, key = { it.id }) { environment ->
                    EnvironmentRow(
                        environment = environment,
                        active = environment.id == activeId,
                        onActivate = { vm.setActive(environment.id) },
                        onOpen = { onOpenEnvironment(environment.id) },
                        onDelete = { deleteTargetId = environment.id }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = stringResource(R.string.environment_new),
            label = stringResource(R.string.environment_name),
            confirmLabel = stringResource(R.string.action_add),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                vm.create(name)
                showCreate = false
            }
        )
    }

    if (deleteTargetId != null) {
        ConfirmDialog(
            title = stringResource(R.string.environment_delete_title),
            message = stringResource(R.string.environment_delete_message),
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
private fun EnvironmentRow(
    environment: ApiEnvironment,
    active: Boolean,
    onActivate: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = active, onClick = onActivate)
        Icon(
            Icons.Outlined.Public,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = environment.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${environment.variables.size} ${stringResource(R.string.environment_variables)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (active) {
            Text(
                text = stringResource(R.string.environment_active),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
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
