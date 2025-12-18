package com.payten.whitelabel.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.payten.whitelabel.R

/**
 * Typography configuration for the Payten WhiteLabel SoftPOS application.
 *
 * ## Custom Font Family:
 * The app uses **Myriad Pro** as its primary typeface for consistent branding
 * across all text elements. This professional font family is loaded from local
 * font resources in `/res/font/`.
 *
 * ### Available Weights:
 * - **Normal** (400) - Used for body text, descriptions, secondary information
 * - **Bold** (700) - Used for headers, buttons, emphasis, amounts
 *
 * ### Font Files:
 * - `myriadpro_regular.ttf` - Normal weight
 * - `myriadpro_bold.ttf` - Bold weight
 *
 * @see Color for color definitions used with typography
 * @see Theme for how typography integrates into Material3 theme
 */
val MyriadPro = FontFamily(
    Font(R.font.myriadpro_regular, FontWeight.Normal),
    Font(R.font.myriadpro_bold, FontWeight.Bold)
)