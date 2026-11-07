package com.apipocket.ui.environments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apipocket.R
import com.apipocket.data.model.KeyValue
import com.apipocket.ui.common.EmptyState
import com.apipocket.ui.common.KeyValueEditor
import com.apipocket.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentDetailScreen(
    vm: EnvironmentsViewModel,
    environmentId: String,
    onBack: () -> Unit
) {
    val environments by vm.environments.collectAsStateWithLifecycle()
    val activeId by vm.activeEnvironmentId.collectAsStateWithLifecycle()
    val environment = environments.firstOrNull { it.id == environmentId }

    var name by remember(environmentId) { mutableStateOf("") }
    var variables by remember(environmentId) { mutableStateOf<List<KeyValue>>(emptyList()) }
    var initialized by rememberSaveable(environmentId) { mutableStateOf(false) }

    LaunchedEffect(environment?.id) {
        val current = environment
        if (current != null && !initialized) {
            name = current.name
            variables = current.variables
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.environment_variables)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val current = environment ?: return@TextButton
                            vm.save(current.copy(name = name, variables = variables))
                            onBack()
                        },
                        enabled = environment != null && name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { padding ->
        if (environment == null) {
            EmptyState(
                icon = Icons.Outlined.Public,
                title = stringResource(R.string.environments_empty_title),
                message = stringResource(R.string.environments_empty_message),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (environment.id == activeId) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.environment_active)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Public, contentDescription = null)
                        }
                    )
                } else {
                    TextButton(onClick = { vm.setActive(environment.id) }) {
                        Text(stringResource(R.string.environment_set_active))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.environment_name)) },
                    singleLine = true
                )

                SectionCard(title = stringResource(R.string.environment_variables)) {
                    KeyValueEditor(
                        items = variables,
                        onItemsChange = { variables = it },
                        keyPlaceholder = stringResource(R.string.key_hint),
                        valuePlaceholder = stringResource(R.string.value_hint),
                        emptyHint = stringResource(R.string.variables_empty_hint)
                    )
                }

                Text(
                    text = stringResource(R.string.environments_empty_message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
