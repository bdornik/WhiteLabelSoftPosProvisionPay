package com.payten.whitelabel.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Main theme file containing the usage of defined colours and typography.
 *
 * Currently only supporting light theme.
 * */
private val LightColorScheme = lightColorScheme(
    // Primary colors - used for main actions (buttons, FABs)
    primary = PaytenRed,
    onPrimary = TextOnPrimary,
    primaryContainer = PaytenRedLight,
    onPrimaryContainer = TextPrimary,

    // Secondary colors
    secondary = TextSecondary,
    onSecondary = TextOnPrimary,
    secondaryContainer = SurfaceGray,
    onSecondaryContainer = TextPrimary,

    // Tertiary colors - used for T&C box
    tertiary = TermsBoxBackground,
    onTertiary = TextSecondary,

    // Error colors
    error = ErrorRed,
    onError = TextOnPrimary,

    // Background colors
    background = BackgroundLight,
    onBackground = TextPrimary,

    // Surface colors - used for cards, text fields
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,
)

private val DarkColorScheme = darkColorScheme(
    primary = PaytenRed,
    onPrimary = TextOnPrimary,
    primaryContainer = PaytenRedDark,
    onPrimaryContainer = TextOnPrimary,

    secondary = TextTertiary,
    onSecondary = TextPrimary,

    tertiary = TermsBoxBackground,
    onTertiary = TextTertiary,

    error = ErrorRed,
    onError = TextOnPrimary,

    background = BackgroundDark,
    onBackground = TextOnPrimary,

    surface = Color(0xFF2A2A2A),
    onSurface = TextOnPrimary,
)
/**
 * Main theme composable for the Payten POS application.
 *
 * Wraps app content with Material3 theming using custom color scheme.
 *
 * @param darkTheme Whether to use dark theme. Currently always uses light theme.
 * @param content The composable content to be themed.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For now, always use light theme
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}