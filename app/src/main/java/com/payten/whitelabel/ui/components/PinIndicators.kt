package com.payten.whitelabel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Visual PIN entry indicator displaying 4 circular dots.
 *
 * Shows user progress when entering a 4-digit PIN code through visual feedback:
 * - Empty circles (gray border) for digits not yet entered
 * - Filled circles (themed primary color) for entered digits
 * - Red filled circles in error state (wrong PIN)
 *
 * The component displays exactly 4 circles in a horizontal row with 16dp spacing,
 * filling them from left to right as the user enters each digit.
 *
 * Used in:
 * - PinSetupScreen (creating new PIN)
 * - PinLoginScreen (entering PIN to login)
 * - ChangePinVerificationScreen (verifying old PIN)
 *
 * @param pinLength Current number of PIN digits entered (0-4). Determines how many
 *                  circles appear filled.
 * @param isError Whether to show error state with red circles instead of primary color.
 *                Used when PIN validation fails to provide visual feedback.
 */
@Composable
fun PinIndicators(
    pinLength: Int,
    isError: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val isFilled = index < pinLength

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> MaterialTheme.colorScheme.error
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> Color.White
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = when {
                            isError -> MaterialTheme.colorScheme.error
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> Color.LightGray
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}