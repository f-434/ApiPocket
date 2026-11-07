package com.apipocket.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "-"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.getDefault(), "%.2f MB", mb)
    return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
}

fun formatDuration(ms: Long): String {
    return if (ms < 1000) {
        "$ms ms"
    } else {
        String.format(Locale.getDefault(), "%.2f s", ms / 1000.0)
    }
}

private val timestampFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

fun formatTimestamp(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(timestampFormatter)
}
