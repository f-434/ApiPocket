package com.apipocket.ui.request

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.apipocket.R
import com.apipocket.data.model.ApiKeyLocation
import com.apipocket.data.model.AuthType
import com.apipocket.data.model.BodyType
import com.apipocket.data.model.RequestConfig
import com.apipocket.ui.common.FormDataEditor
import com.apipocket.ui.common.KeyValueEditor
import com.apipocket.util.JsonFormatter

@Composable
fun RequestEditor(
    request: RequestConfig,
    onUpdate: ((RequestConfig) -> RequestConfig) -> Unit,
    onFormatJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.request_tab_params)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.request_tab_headers)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.request_tab_body)) })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(stringResource(R.string.request_tab_auth)) })
        }
        when (tab) {
            0 -> ParamsTab(request, onUpdate, Modifier.weight(1f))
            1 -> HeadersTab(request, onUpdate, Modifier.weight(1f))
            2 -> BodyTab(request, onUpdate, onFormatJson, Modifier.weight(1f))
            else -> AuthTab(request, onUpdate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ParamsTab(
    request: RequestConfig,
    onUpdate: ((RequestConfig) -> RequestConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        KeyValueEditor(
            items = request.params,
            onItemsChange = { items -> onUpdate { it.copy(params = items) } },
            emptyHint = stringResource(R.string.no_params_hint)
        )
    }
}

@Composable
private fun HeadersTab(
    request: RequestConfig,
    onUpdate: ((RequestConfig) -> RequestConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        KeyValueEditor(
            items = request.headers,
            onItemsChange = { items -> onUpdate { it.copy(headers = items) } },
            emptyHint = stringResource(R.string.no_headers_hint)
        )
    }
}

@Composable
private fun BodyTab(
    request: RequestConfig,
    onUpdate: ((RequestConfig) -> RequestConfig) -> Unit,
    onFormatJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val body = request.body
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChoiceChips(
            options = listOf(
                BodyType.NONE to stringResource(R.string.body_none),
                BodyType.JSON to stringResource(R.string.body_json),
                BodyType.RAW to stringResource(R.string.body_raw),
                BodyType.FORM_DATA to stringResource(R.string.body_form_data),
                BodyType.X_WWW_FORM_URLENCODED to stringResource(R.string.body_urlencoded)
            ),
            selected = body.type,
            onSelect = { type -> onUpdate { it.copy(body = it.body.copy(type = type)) } }
        )

        when (body.type) {
            BodyType.NONE -> Text(
                text = stringResource(R.string.body_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BodyType.JSON -> {
                val valid = remember(body.json) {
                    body.json.isBlank() || JsonFormatter.isValid(body.json)
                }
                OutlinedTextField(
                    value = body.json,
                    onValueChange = { value -> onUpdate { it.copy(body = it.body.copy(json = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onFormatJson) {
                        Text(stringResource(R.string.action_format))
                    }
                    TextButton(
                        onClick = { onUpdate { it.copy(body = it.body.copy(json = "")) } }
                    ) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
                if (!valid) {
                    Text(
                        text = stringResource(R.string.json_invalid),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            BodyType.RAW -> {
                OutlinedTextField(
                    value = body.rawContentType,
                    onValueChange = { value -> onUpdate { it.copy(body = it.body.copy(rawContentType = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.body_content_type)) },
                    textStyle = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = body.raw,
                    onValueChange = { value -> onUpdate { it.copy(body = it.body.copy(raw = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }

            BodyType.FORM_DATA -> FormDataEditor(
                items = body.formData,
                onItemsChange = { items -> onUpdate { it.copy(body = it.body.copy(formData = items)) } }
            )

            BodyType.X_WWW_FORM_URLENCODED -> KeyValueEditor(
                items = body.urlEncoded,
                onItemsChange = { items -> onUpdate { it.copy(body = it.body.copy(urlEncoded = items)) } }
            )
        }
    }
}

@Composable
private fun AuthTab(
    request: RequestConfig,
    onUpdate: ((RequestConfig) -> RequestConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val auth = request.auth
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChoiceChips(
            options = listOf(
                AuthType.NONE to stringResource(R.string.auth_none),
                AuthType.BEARER to stringResource(R.string.auth_bearer),
                AuthType.BASIC to stringResource(R.string.auth_basic),
                AuthType.API_KEY to stringResource(R.string.auth_api_key)
            ),
            selected = auth.type,
            onSelect = { type -> onUpdate { it.copy(auth = it.auth.copy(type = type)) } }
        )

        when (auth.type) {
            AuthType.NONE -> Text(
                text = stringResource(R.string.auth_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AuthType.BEARER -> {
                OutlinedTextField(
                    value = auth.token,
                    onValueChange = { value -> onUpdate { it.copy(auth = it.auth.copy(token = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_token)) },
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }

            AuthType.BASIC -> {
                OutlinedTextField(
                    value = auth.username,
                    onValueChange = { value -> onUpdate { it.copy(auth = it.auth.copy(username = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_username)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = auth.password,
                    onValueChange = { value -> onUpdate { it.copy(auth = it.auth.copy(password = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            AuthType.API_KEY -> {
                OutlinedTextField(
                    value = auth.keyName,
                    onValueChange = { value -> onUpdate { it.copy(auth = it.auth.copy(keyName = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_key_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = auth.keyValue,
                    onValueChange = { value -> onUpdate { it.copy(auth = it.auth.copy(keyValue = value)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_key_value)) },
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.auth_key_location),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ChoiceChips(
                    options = listOf(
                        ApiKeyLocation.HEADER to stringResource(R.string.auth_header),
                        ApiKeyLocation.QUERY to stringResource(R.string.auth_query)
                    ),
                    selected = auth.keyLocation,
                    onSelect = { location ->
                        onUpdate { it.copy(auth = it.auth.copy(keyLocation = location)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}
