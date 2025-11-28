package com.payten.whitelabel.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.payten.whitelabel.R
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.components.NumericKeypad
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * Custom tip entry screen.
 *
 * User enters custom tip amount using numeric keypad.
 * Amount is displayed in RSD currency format.
 *
 * @param onNavigateBack Callback when back button is clicked
 * @param onContinue Callback with tip amount when user clicks continue (amount in minor units - pare)
 */
@Composable
fun CustomTipEntryScreen(
    onNavigateBack: () -> Unit = {},
    onContinue: (Long) -> Unit = {}
) {
    val TAG = "CustomTipEntryScreen"

    // Tip amount in minor units
    var tipInPare by remember { mutableLongStateOf(0L) }

    val displayAmount = remember(tipInPare) {
        formatAmount(tipInPare)
    }

    val isButtonEnabled = tipInPare >= 0 // Allow zero tip

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            CustomTipEntryHeader(
                title = stringResource(R.string.custom_tip_title),
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayAmount,
                        fontSize = 32.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.currency_rsd),
                        fontSize = 16.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                NumericKeypad(
                    onNumberClick = { number ->
                        val newAmount = tipInPare * 10 + number

                        if (newAmount <= 999_999_999L) {
                            tipInPare = newAmount
                            Log.d(TAG, "Tip updated: $newAmount pare (${formatAmount(newAmount)} RSD)")
                        }
                    },
                    onBackspaceClick = {
                        tipInPare /= 10
                        Log.d(TAG, "Backspace: new tip = $tipInPare pare")
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    Log.d(TAG, "Continue clicked - tip: $tipInPare pare (${formatAmount(tipInPare)} RSD)")
                    onContinue(tipInPare)
                },
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.custom_tip_confirm),
                    fontSize = 20.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Header with back button and title.
 */
@Composable
private fun CustomTipEntryHeader(
    title: String,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(40.dp))
    }
}

/**
 * Format amount from pare to RSD with thousand separators and decimal point.
 *
 * @param amountInPare Amount in minor units.
 * @return Formatted string.
 */
private fun formatAmount(amountInPare: Long): String {
    val dinars = amountInPare / 100
    val pare = amountInPare % 100

    val formattedDinars = dinars.toString().reversed().chunked(3).joinToString(".").reversed()

    val formattedPare = pare.toString().padStart(2, '0')

    return "$formattedDinars,$formattedPare"
}

@Preview
@Composable
fun CustomTipEntryScreenPreview() {
    AppTheme {
        CustomTipEntryScreen(
            onNavigateBack = {},
            onContinue = {}
        )
    }
}
