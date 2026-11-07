package com.apipocket.ui.request

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apipocket.R
import com.apipocket.data.remote.ApiError
import com.apipocket.data.remote.ApiResponse
import com.apipocket.ui.common.JsonCodeText
import com.apipocket.ui.common.StatusChip
import com.apipocket.ui.common.apiErrorText
import com.apipocket.util.JsonFormatter
import com.apipocket.util.formatBytes
import com.apipocket.util.formatDuration

private const val DISPLAY_LIMIT = 256 * 1024

@Composable
fun ResponsePanel(
    response: ApiResponse?,
    error: ApiError?,
    durationMs: Long,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        isSending -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.sending_in_progress),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        error != null && response == null -> Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = apiErrorText(error),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (durationMs > 0) {
                    Text(
                        text = formatDuration(durationMs),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        response == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(
                    Icons.Outlined.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
                Text(
                    text = stringResource(R.string.response_empty_title),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.response_empty_message),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> ResponseContent(response, modifier)
    }
}

@Composable
private fun ResponseContent(response: ApiResponse, modifier: Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pretty by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val isJson = remember(response.body, response.contentType) {
        JsonFormatter.looksLikeJson(response.body, response.contentType) &&
            JsonFormatter.isValid(response.body)
    }
    val prettyBody = remember(response.body, isJson) {
        if (isJson) JsonFormatter.pretty(response.body).getOrNull() else null
    }
    val displayed = if (pretty && prettyBody != null) prettyBody else response.body
    val displayText = displayed.take(DISPLAY_LIMIT)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusChip(response.statusCode)
            Text(
                text = response.statusMessage,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(response.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatBytes(response.bodyBytes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = response.protocol,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.response_body)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("${stringResource(R.string.response_headers)} (${response.headers.size})") }
            )
        }

        if (tab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isJson) {
                    FilterChip(
                        selected = pretty,
                        onClick = { pretty = true },
                        label = { Text(stringResource(R.string.response_pretty)) }
                    )
                    FilterChip(
                        selected = !pretty,
                        onClick = { pretty = false },
                        label = { Text(stringResource(R.string.response_raw)) }
                    )
                }
                Box(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(response.body))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (response.truncated) {
                Text(
                    text = stringResource(R.string.response_truncated_network),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (displayed.length > DISPLAY_LIMIT) {
                Text(
                    text = stringResource(R.string.response_truncated_display),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                if (isJson) {
                    JsonCodeText(displayText)
                } else {
                    Text(
                        text = displayText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        softWrap = false
                    )
                }
            }
        } else {
            if (response.headers.isEmpty()) {
                Text(
                    text = stringResource(R.string.response_no_headers),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(response.headers) { _, header ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = header.first,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = header.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString("${header.first}: ${header.second}"))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(R.string.action_copy),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
