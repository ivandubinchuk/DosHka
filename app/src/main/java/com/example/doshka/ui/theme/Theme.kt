package com.example.doshka.ui.theme

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

// Темна кольорова схема — Industrial Neo-Brutalism
private val DarkColorScheme = darkColorScheme(
    primary = BrutalOrange,
    onPrimary = BrutalSurfaceLight,
    primaryContainer = BrutalOrangeDark,
    onPrimaryContainer = BrutalSurfaceLight,
    secondary = BrutalAmber,
    onSecondary = BrutalSurfaceDark,
    secondaryContainer = BrutalAmber,
    onSecondaryContainer = BrutalSurfaceDark,
    tertiary = BrutalGreen,
    onTertiary = BrutalSurfaceDark,
    error = BrutalRed,
    onError = BrutalSurfaceLight,
    errorContainer = BrutalRed,
    onErrorContainer = BrutalSurfaceLight,
    background = BrutalSurfaceDark,
    onBackground = BrutalTextPrimaryDark,
    surface = BrutalSurfaceDark,
    onSurface = BrutalTextPrimaryDark,
    surfaceVariant = BrutalCardDark,
    onSurfaceVariant = BrutalTextSecondaryDark,
    outline = BrutalBorderDark,
    outlineVariant = BrutalTextSecondaryDark
)

// Світла кольорова схема — Industrial Neo-Brutalism
private val LightColorScheme = lightColorScheme(
    primary = BrutalOrange,
    onPrimary = BrutalSurfaceLight,
    primaryContainer = BrutalOrangeDark,
    onPrimaryContainer = BrutalSurfaceLight,
    secondary = BrutalAmber,
    onSecondary = BrutalSurfaceDark,
    secondaryContainer = BrutalAmber,
    onSecondaryContainer = BrutalSurfaceDark,
    tertiary = BrutalGreen,
    onTertiary = BrutalSurfaceDark,
    error = BrutalRed,
    onError = BrutalSurfaceLight,
    errorContainer = BrutalRed,
    onErrorContainer = BrutalSurfaceLight,
    background = BrutalSurfaceLight,
    onBackground = BrutalTextPrimaryLight,
    surface = BrutalSurfaceLight,
    onSurface = BrutalTextPrimaryLight,
    surfaceVariant = BrutalCardLight,
    onSurfaceVariant = BrutalTextSecondaryLight,
    outline = BrutalBorderLight,
    outlineVariant = BrutalTextSecondaryLight
)

@Composable
fun DoshkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
