package com.payten.whitelabel.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for the Payten WhiteLabel SoftPOS application.
 *
 * This file defines all color constants used throughout the app's UI, following
 * Material Design 3 color system principles. Colors are organized by usage category.
 *
 * ## Brand Identity:
 * The primary brand color is Payten Red (#E53935), used for:
 * - Action buttons (payment, confirm, submit)
 * - Logo and branding elements
 * - Loading indicators
 * - Primary focus elements
 *
 * ## Usage Pattern:
 * These color values are mapped to semantic roles in Theme.kt's ColorScheme:
 * - `primary` → PaytenRed (main actions)
 * - `background` → BackgroundLight (screen backgrounds)
 * - `surface` → SurfaceWhite (cards, text fields)
 * - `error` → ErrorRed (validation errors, failed states)
 *
 * @see Theme for how these colors map to Material3 ColorScheme roles
 */

// Primary Colors
val PaytenRed = Color(0xFFE53935)           // Main action color (buttons, logo)
val PaytenRedLight = Color(0xFFEF5350)      // Lighter variant
val PaytenRedDark = Color(0xFFC62828)       // Darker variant

// Background Colors
val BackgroundLight = Color(0xFFEFF2FA)     // Light background
val BackgroundDark = Color(0xFF1A1A1A)      // Dark background

// Surface Colors
val SurfaceWhite = Color.White
val SurfaceGray = Color(0xFFF5F5F5)

// Text Colors
val TextPrimary = Color.Black
val TextSecondary = Color.Gray
val TextTertiary = Color.LightGray
val TextOnPrimary = Color.White

// Special Use Colors
val TermsBoxBackground = Color(0x65D7DEE2)
val TextFieldBorder = Color(0xFFE0E0E0)
val ErrorRed = Color(0xFFD32F2F)