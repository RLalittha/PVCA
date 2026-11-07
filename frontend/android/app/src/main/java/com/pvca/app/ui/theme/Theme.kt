package com.pvca.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PvcaPrimaryBlue,
    onPrimary = PvcaDarkTextPrimary,
    primaryContainer = PvcaDeepBlue,
    onPrimaryContainer = PvcaCyan,
    secondary = PvcaCyan,
    onSecondary = PvcaDarkBackground,
    background = PvcaDarkBackground,
    onBackground = PvcaDarkTextPrimary,
    surface = PvcaDarkSurface,
    onSurface = PvcaDarkTextPrimary,
    surfaceVariant = PvcaDarkSurfaceVariant,
    onSurfaceVariant = PvcaDarkTextSecondary,
    error = PvcaError,
    onError = PvcaDarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PvcaPrimaryBlue,
    onPrimary = PvcaLightBackground,
    primaryContainer = PvcaLightSurfaceVariant,
    onPrimaryContainer = PvcaDeepBlue,
    secondary = PvcaDeepBlue,
    onSecondary = PvcaLightBackground,
    background = PvcaLightBackground,
    onBackground = PvcaLightTextPrimary,
    surface = PvcaLightSurface,
    onSurface = PvcaLightTextPrimary,
    surfaceVariant = PvcaLightSurfaceVariant,
    onSurfaceVariant = PvcaLightTextSecondary,
    error = PvcaError,
    onError = PvcaLightBackground
)

@Composable
fun PvcaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PvcaTypography,
        content = content
    )
}
