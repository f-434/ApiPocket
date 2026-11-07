package com.apipocket.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apipocket.R
import com.apipocket.data.model.ThemeMode
import com.apipocket.ui.common.ConfirmDialog
import com.apipocket.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pendingImport by vm.pendingImport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importFrom(uri)
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            val message = when (event) {
                SettingsEvent.ExportSuccess -> context.getString(R.string.export_success)
                SettingsEvent.ExportFailed -> context.getString(R.string.export_failed)
                SettingsEvent.ImportFailed -> context.getString(R.string.import_failed)
                is SettingsEvent.ImportOwnDone -> context.getString(
                    R.string.import_own_success,
                    event.collections,
                    event.environments
                )
                is SettingsEvent.ImportPostmanDone -> {
                    val base = context.getString(
                        R.string.import_postman_success,
                        event.collections,
                        event.requests
                    )
                    if (event.skipped > 0) {
                        "$base · ${context.getString(R.string.import_skipped, event.skipped)}"
                    } else {
                        base
                    }
                }
                SettingsEvent.ClearDone -> context.getString(R.string.clear_done)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = stringResource(R.string.settings_request_section)) {
                DropdownSetting(
                    label = stringResource(R.string.settings_request_timeout),
                    value = stringResource(R.string.settings_timeout_value, settings.timeoutSeconds),
                    options = listOf(10, 30, 60, 120).map {
                        it.toString() to stringResource(R.string.settings_timeout_value, it)
                    },
                    onSelect = { vm.setTimeout(it.toInt()) }
                )
                DropdownSetting(
                    label = stringResource(R.string.settings_history_limit),
                    value = stringResource(R.string.settings_history_limit_value, settings.historyLimit),
                    options = listOf(50, 100, 200, 500).map {
                        it.toString() to stringResource(R.string.settings_history_limit_value, it)
                    },
                    onSelect = { vm.setHistoryLimit(it.toInt()) }
                )
            }

            SectionCard(title = stringResource(R.string.settings_appearance_section)) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark)
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.theme == mode,
                            onClick = { vm.setTheme(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_data_section)) {
                ActionRow(
                    icon = Icons.Outlined.FileDownload,
                    label = stringResource(R.string.settings_export)
                ) { exportLauncher.launch(vm.exportFileName()) }
                ActionRow(
                    icon = Icons.Outlined.FileUpload,
                    label = stringResource(R.string.settings_import)
                ) {
                    importLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
                }
                ActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    label = stringResource(R.string.settings_clear_all),
                    tint = MaterialTheme.colorScheme.error
                ) { showClearDialog = true }
            }

            SectionCard(title = stringResource(R.string.settings_about_section)) {
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_storage_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_title),
            message = stringResource(R.string.settings_clear_message),
            confirmLabel = stringResource(R.string.action_clear),
            onConfirm = {
                vm.clearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (pendingImport != null) {
        AlertDialog(
            onDismissRequest = vm::cancelPendingImport,
            title = { Text(stringResource(R.string.import_choose_mode_title)) },
            text = { Text(stringResource(R.string.import_choose_mode_message)) },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.applyOwnImport(false) }) {
                        Text(stringResource(R.string.import_merge))
                    }
                    TextButton(onClick = { vm.applyOwnImport(true) }) {
                        Text(
                            text = stringResource(R.string.import_replace),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelPendingImport) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(value)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelect(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}
