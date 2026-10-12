package dev.igor.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    onPrimary = Color(0xFF0A1A6B),
    primaryContainer = Color(0xFF3D5AFE),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF2A2D3A),
    onSecondaryContainer = Color(0xFFE3E5F0),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1F2029),
    onSurfaceVariant = Color(0xFFB8BACB),
)

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
