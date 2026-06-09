package com.example.mytorch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TorchAmber,
    secondary = TorchAmberDark,
    background = SurfaceDark,
    surface = SurfaceDarkElevated,
    onPrimary = SurfaceDark,
    onSecondary = SurfaceDark,
    onBackground = OnSurfaceBright,
    onSurface = OnSurfaceBright
)

@Composable
fun MyTorchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}