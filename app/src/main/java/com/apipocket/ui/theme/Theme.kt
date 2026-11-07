package com.apipocket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.apipocket.data.model.ThemeMode

val AppTypography = Typography()

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = Slate,
    secondaryContainer = SlateLight,
    tertiary = Amber,
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = DarkTeal,
    onPrimary = OnTealContainer,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = OnDarkTealContainer,
    secondary = SlateLight,
    tertiary = Color(0xFFFBBF24),
    error = Color(0xFFFCA5A5)
)

@Composable
fun ApiPocketTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
