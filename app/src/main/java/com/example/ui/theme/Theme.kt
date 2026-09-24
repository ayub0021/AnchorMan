package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AnchorColorScheme = lightColorScheme(
    primary = AnchorCyan,
    onPrimary = AnchorDark,
    primaryContainer = AnchorCyanLight,
    onPrimaryContainer = AnchorDark,
    secondary = AnchorYellow,
    onSecondary = AnchorDark,
    error = AnchorRed,
    onError = AnchorDark,
    background = AnchorBg,
    onBackground = AnchorDark,
    surface = AnchorCardBg,
    onSurface = AnchorDark,
    surfaceVariant = AnchorCardCompleted,
    onSurfaceVariant = AnchorDark,
    outline = AnchorBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AnchorColorScheme,
        typography = Typography,
        content = content
    )
}
