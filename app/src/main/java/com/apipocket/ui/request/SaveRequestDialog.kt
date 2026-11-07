package com.apipocket.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.apipocket.R
import com.apipocket.data.model.ApiCollection
import com.apipocket.data.model.EditorDraft

@Composable
fun SaveRequestDialog(
    draft: EditorDraft,
    collections: List<ApiCollection>,
    onDismiss: () -> Unit,
    onSave: (
        collectionId: String?,
        newCollectionName: String,
        requestName: String,
        updateExisting: Boolean
    ) -> Unit
) {
    val suggestedName = draft.name.ifBlank { draft.request.suggestedName() }
    var requestName by rememberSaveable { mutableStateOf(suggestedName) }
    var createNew by rememberSaveable { mutableStateOf(collections.isEmpty()) }
    var newCollectionName by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable {
        mutableStateOf(draft.collectionId ?: collections.firstOrNull()?.id)
    }
    var updateExisting by rememberSaveable { mutableStateOf(draft.savedRequestId != null) }
    var collectionMenu by remember { mutableStateOf(false) }

    val canSave = requestName.isNotBlank() &&
        (if (createNew) newCollectionName.isNotBlank() else selectedId != null)
    val selectedCollection = collections.firstOrNull { it.id == selectedId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = requestName,
                    onValueChange = { requestName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.request_name_hint)) },
                    singleLine = true
                )

                if (draft.savedRequestId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = updateExisting,
                            onCheckedChange = { updateExisting = it }
                        )
                        Text(
                            text = stringResource(
                                if (updateExisting) {
                                    R.string.save_dialog_update_existing
                                } else {
                                    R.string.save_dialog_save_as_new
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = createNew,
                        onCheckedChange = { createNew = it }
                    )
                    Text(
                        text = stringResource(R.string.save_dialog_create_collection),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (createNew) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.save_dialog_new_collection)) },
                        singleLine = true
                    )
                } else {
                    Box {
                        OutlinedButton(
                            onClick = { collectionMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedCollection?.name
                                    ?: stringResource(R.string.save_dialog_collection),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = collectionMenu,
                            onDismissRequest = { collectionMenu = false }
                        ) {
                            collections.forEach { collection ->
                                DropdownMenuItem(
                                    text = { Text(collection.name) },
                                    onClick = {
                                        selectedId = collection.id
                                        collectionMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        if (createNew) null else selectedId,
                        if (createNew) newCollectionName else "",
                        requestName,
                        updateExisting
                    )
                },
                enabled = canSave
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
