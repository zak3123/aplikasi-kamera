package com.fatih.adaptivecompositioncamera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CameraDarkScheme = darkColorScheme(
    primary = Color(0xFF9EE6D8),
    secondary = Color(0xFFF6C177),
    tertiary = Color(0xFFA8C7FA),
    background = Color(0xFF0E1116),
    surface = Color(0xFF171A21),
    surfaceVariant = Color(0xFF252A33),
    onPrimary = Color(0xFF09201B),
    onSecondary = Color(0xFF2A1700),
    onBackground = Color(0xFFF2F5F7),
    onSurface = Color(0xFFF2F5F7),
)

@Composable
fun AdaptiveCompositionCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CameraDarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
