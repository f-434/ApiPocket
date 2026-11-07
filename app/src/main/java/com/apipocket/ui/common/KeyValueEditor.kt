package com.apipocket.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apipocket.R
import com.apipocket.data.model.KeyValue

@Composable
fun KeyValueEditor(
    items: List<KeyValue>,
    onItemsChange: (List<KeyValue>) -> Unit,
    modifier: Modifier = Modifier,
    keyPlaceholder: String? = null,
    valuePlaceholder: String? = null,
    emptyHint: String? = null,
    addLabel: String? = null
) {
    val keyHint = keyPlaceholder ?: stringResource(R.string.key_hint)
    val valueHint = valuePlaceholder ?: stringResource(R.string.value_hint)
    val addText = addLabel ?: stringResource(R.string.add_row)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, item ->
            key(item.id) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = item.enabled,
                        onCheckedChange = { checked ->
                            onItemsChange(items.replaceAt(index, item.copy(enabled = checked)))
                        }
                    )
                    OutlinedTextField(
                        value = item.key,
                        onValueChange = { value ->
                            onItemsChange(items.replaceAt(index, item.copy(key = value)))
                        },
                        modifier = Modifier.weight(0.42f),
                        placeholder = { Text(keyHint, style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = item.value,
                        onValueChange = { value ->
                            onItemsChange(items.replaceAt(index, item.copy(value = value)))
                        },
                        modifier = Modifier.weight(0.58f),
                        placeholder = { Text(valueHint, style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            onItemsChange(items.filterIndexed { i, _ -> i != index })
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (items.isEmpty() && emptyHint != null) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = { onItemsChange(items + KeyValue()) }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(addText)
        }
    }
}

internal fun <T> List<T>.replaceAt(index: Int, value: T): List<T> {
    if (index !in indices) return this
    return toMutableList().also { it[index] = value }
}
