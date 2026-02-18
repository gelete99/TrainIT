package com.example.trainit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TrainITDarkScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Bg,

    secondary = BrandBlueDark,
    onSecondary = Bg,

    tertiary = Success,            // ancla Success
    onTertiary = Bg,

    background = Bg,
    onBackground = TextPrimary,    // ancla TextPrimary

    surface = Surface,
    onSurface = TextPrimary,

    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,  // ancla TextSecondary

    error = Error,
    onError = Bg,

    outline = Outline,

    inversePrimary = Warning       // ancla Warning
)


@Composable
fun TrainITTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Para TFG: mejor consistente, sin colores dinámicos
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Por ahora forzamos dark (si quieres luego damos opción light)
    val scheme = TrainITDarkScheme

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
