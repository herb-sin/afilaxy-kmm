package com.afilaxy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

private val DarkColorScheme = darkColorScheme(
    primary = AflixyBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = AflixyBlueDark,
    onPrimaryContainer = TextOnPrimary,
    
    secondary = AflixyGreen,
    onSecondary = TextOnPrimary,
    secondaryContainer = AflixyGreenDark,
    onSecondaryContainer = TextOnPrimary,
    
    tertiary = AflixyRed,
    onTertiary = TextOnPrimary,
    
    background = BackgroundDark,
    onBackground = TextOnPrimary,
    
    surface = SurfaceDark,
    onSurface = TextOnPrimary,
    
    error = AflixyRedLight,
    onError = TextOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = AflixyBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = AflixyBlueLight,
    onPrimaryContainer = TextPrimary,
    
    secondary = AflixyGreen,
    onSecondary = TextOnPrimary,
    secondaryContainer = AflixyGreenLight,
    onSecondaryContainer = TextPrimary,
    
    tertiary = AflixyRed,
    onTertiary = TextOnPrimary,
    
    background = BackgroundLight,
    onBackground = TextPrimary,
    
    surface = SurfaceLight,
    onSurface = TextPrimary,
    
    error = AflixyRed,
    onError = TextOnPrimary
)

@Composable
fun AflixyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as ComponentActivity
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(
                    scrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = SystemBarStyle.light(
                    scrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT
                )
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AflixyTypography,
        content = content
    )
}
