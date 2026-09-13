package com.oorbitt.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Oorbitt brand palette — Elegant Emerald Green and Slate Blue
private val OorbittGreen = Color(0xFF00B894)
private val OorbittBlue = Color(0xFF0984E3)
private val OorbittPink = Color(0xFF00D2D3) // Muted teal-cyan
private val OorbittSurface = Color(0xFF1E1E2E)
private val OorbittSurfaceLight = Color(0xFFF5F5F5)

private val DarkColorScheme = darkColorScheme(
    primary = OorbittGreen,
    secondary = OorbittBlue,
    tertiary = OorbittPink,
    background = OorbittSurface,
    surface = Color(0xFF313244),
    onPrimary = Color(0xFF1E1E2E),
    onSecondary = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    onSurface = Color(0xFFCDD6F4),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B894),
    secondary = Color(0xFF0984E3),
    tertiary = Color(0xFF00D2D3),
    background = OorbittSurfaceLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E1E2E),
    onSurface = Color(0xFF1E1E2E),
)

@Composable
fun OorbittTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OorbittTypography,
        content = content
    )
}
