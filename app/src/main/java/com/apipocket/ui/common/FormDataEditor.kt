package com.apipocket.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apipocket.R
import com.apipocket.data.model.FormField
import com.apipocket.util.queryDisplayName
import com.apipocket.util.takePersistableReadPermission

@Composable
fun FormDataEditor(
    items: List<FormField>,
    onItemsChange: (List<FormField>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestItems by rememberUpdatedState(items)
    val latestOnChange by rememberUpdatedState(onItemsChange)
    var pendingFieldId by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val fieldId = pendingFieldId
        pendingFieldId = null
        if (uri != null && fieldId != null) {
            context.takePersistableReadPermission(uri)
            val name = context.contentResolver.queryDisplayName(uri)
            latestOnChange(
                latestItems.map { field ->
                    if (field.id == fieldId) {
                        field.copy(uri = uri.toString(), fileName = name ?: field.fileName)
                    } else {
                        field
                    }
                }
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, field ->
            key(field.id) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = field.enabled,
                            onCheckedChange = { checked ->
                                onItemsChange(items.replaceAt(index, field.copy(enabled = checked)))
                            }
                        )
                        OutlinedTextField(
                            value = field.key,
                            onValueChange = { value ->
                                onItemsChange(items.replaceAt(index, field.copy(key = value)))
                            },
                            modifier = Modifier.weight(0.4f),
                            placeholder = { Text(stringResource(R.string.key_hint), style = MaterialTheme.typography.bodySmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        if (field.isFile) {
                            OutlinedButton(
                                onClick = {
                                    pendingFieldId = field.id
                                    filePicker.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Icon(
                                    Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = field.fileName ?: stringResource(R.string.form_no_file),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = field.value,
                                onValueChange = { value ->
                                    onItemsChange(items.replaceAt(index, field.copy(value = value)))
                                },
                                modifier = Modifier.weight(0.6f),
                                placeholder = { Text(stringResource(R.string.value_hint), style = MaterialTheme.typography.bodySmall) },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                        }
                        IconButton(
                            onClick = {
                                onItemsChange(
                                    items.replaceAt(
                                        index,
                                        field.copy(
                                            isFile = !field.isFile,
                                            uri = null,
                                            fileName = null,
                                            value = if (field.isFile) field.value else ""
                                        )
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (field.isFile) Icons.Outlined.TextFields else Icons.Outlined.AttachFile,
                                contentDescription = stringResource(
                                    if (field.isFile) R.string.form_type_text else R.string.form_type_file
                                ),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onItemsChange(items.filterIndexed { i, _ -> i != index }) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (field.isFile && field.uri == null) {
                        Text(
                            text = stringResource(R.string.form_file_missing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        TextButton(onClick = { onItemsChange(items + FormField()) }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add_row))
        }
    }
}
