package com.apipocket.ui.common

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ColorScheme

private const val MAX_HIGHLIGHT_LENGTH = 200_000

@Composable
fun JsonCodeText(text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val annotated = remember(text, scheme) {
        if (text.length > MAX_HIGHLIGHT_LENGTH) AnnotatedString(text) else highlightJson(text, scheme)
    }
    SelectionContainer {
        Text(
            text = annotated,
            modifier = modifier,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            softWrap = false
        )
    }
}

fun highlightJson(text: String, scheme: ColorScheme): AnnotatedString {
    val dark = scheme.background.luminance() < 0.5f
    val keyColor = scheme.primary
    val stringColor = if (dark) Color(0xFF9CCC65) else Color(0xFF2E7D32)
    val numberColor = if (dark) Color(0xFF64B5F6) else Color(0xFF1565C0)
    val keywordColor = if (dark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)
    val punctuationColor = scheme.onSurfaceVariant

    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> {
                    val start = i
                    i++
                    while (i < text.length) {
                        if (text[i] == '\\') {
                            i += 2
                        } else if (text[i] == '"') {
                            i++
                            break
                        } else {
                            i++
                        }
                    }
                    val end = i.coerceAtMost(text.length)
                    var cursor = end
                    while (cursor < text.length && text[cursor].isWhitespace()) cursor++
                    val isKey = cursor < text.length && text[cursor] == ':'
                    withStyle(SpanStyle(color = if (isKey) keyColor else stringColor)) {
                        append(text.substring(start, end))
                    }
                }
                c == '-' || c.isDigit() -> {
                    val start = i
                    i++
                    while (i < text.length) {
                        val next = text[i]
                        if (next.isDigit() || next == '.' || next == 'e' || next == 'E' || next == '+' || next == '-') {
                            i++
                        } else {
                            break
                        }
                    }
                    withStyle(SpanStyle(color = numberColor)) {
                        append(text.substring(start, i))
                    }
                }
                c.isLetter() -> {
                    val start = i
                    i++
                    while (i < text.length && text[i].isLetter()) i++
                    val word = text.substring(start, i)
                    val color = if (word == "true" || word == "false" || word == "null") {
                        keywordColor
                    } else {
                        punctuationColor
                    }
                    withStyle(SpanStyle(color = color)) { append(word) }
                }
                c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',' -> {
                    withStyle(SpanStyle(color = punctuationColor)) { append(c) }
                    i++
                }
                else -> {
                    append(c)
                    i++
                }
            }
        }
    }
}
