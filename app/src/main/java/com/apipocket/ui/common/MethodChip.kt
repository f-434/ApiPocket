package com.apipocket.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apipocket.data.model.HttpMethod

fun methodColor(method: HttpMethod): Color = when (method) {
    HttpMethod.GET -> Color(0xFF4CAF50)
    HttpMethod.POST -> Color(0xFFFF9800)
    HttpMethod.PUT -> Color(0xFF42A5F5)
    HttpMethod.PATCH -> Color(0xFFAB47BC)
    HttpMethod.DELETE -> Color(0xFFEF5350)
    HttpMethod.HEAD, HttpMethod.OPTIONS -> Color(0xFF90A4AE)
}

fun statusColor(code: Int): Color = when (code) {
    in 200..299 -> Color(0xFF4CAF50)
    in 300..399 -> Color(0xFF42A5F5)
    in 400..499 -> Color(0xFFFF9800)
    else -> Color(0xFFEF5350)
}

@Composable
fun MethodChip(method: HttpMethod, modifier: Modifier = Modifier) {
    val color = methodColor(method)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = method.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusChip(code: Int, modifier: Modifier = Modifier) {
    val color = statusColor(code)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = code.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
