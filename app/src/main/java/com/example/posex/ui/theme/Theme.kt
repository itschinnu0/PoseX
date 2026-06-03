package com.example.posex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PoseXColorScheme = darkColorScheme(
    primary = PoseXAccent,
    onPrimary = PoseXOnPrimary,
    background = PoseXBackground,
    onBackground = PoseXOnBackground,
    surface = PoseXSurface,
    onSurface = PoseXOnSurface,
    error = PoseXError,
    onError = PoseXOnBackground,
    secondary = PoseXSuccess,
    onSecondary = PoseXOnPrimary
)

@Composable
fun PoseXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PoseXColorScheme,
        typography = Typography,
        content = content
    )
}
