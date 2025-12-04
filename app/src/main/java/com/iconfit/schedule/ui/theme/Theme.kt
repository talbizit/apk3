package com.iconfit.schedule.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Colors - Standard Material blue/teal theme
val Primary = Color(0xFF1976D2)  // Material Blue 700
val PrimaryVariant = Color(0xFF1565C0)  // Material Blue 800
val Secondary = Color(0xFF26A69A)  // Teal 400
val SecondaryVariant = Color(0xFF00897B)  // Teal 600
val Background = Color(0xFFFAFAFA)
val Surface = Color(0xFFFFFFFF)
val Error = Color(0xFFD32F2F)
val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF212121)
val OnSurface = Color(0xFF212121)
val OnError = Color(0xFFFFFFFF)
val Favorite = Color(0xFFFFC107)  // Amber
val FavoriteHighlight = Color(0xFFFFF8E1)

// Category colors
val CyclingColor = Color(0xFFE53935)
val YogaColor = Color(0xFF8E24AA)
val PilatesColor = Color(0xFF3949AB)
val DanceColor = Color(0xFFD81B60)
val StrengthColor = Color(0xFF43A047)
val CardioColor = Color(0xFFFF5722)
val AquaColor = Color(0xFF00ACC1)
val MartialArtsColor = Color(0xFF6D4C41)
val StretchColor = Color(0xFF7CB342)
val FunctionalColor = Color(0xFFFF7043)
val OtherColor = Color(0xFF757575)

// Dark theme colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnBackground = Color(0xFFE1E1E1)
val DarkOnSurface = Color(0xFFE1E1E1)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = Error,
    onError = OnError
)

@Composable
fun IconFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
