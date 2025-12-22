package com.payten.whitelabel.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.R
import com.payten.whitelabel.ui.components.AmountDisplayCard
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * Payment method selection screen.
 *
 * Presents the user with two payment options after amount entry:
 * - **Card Payment** (NFC contactless) - For physical card tap transactions
 * - **IPS Payment** (QR code) - For mobile banking QR code payments
 *
 * Each payment method is displayed as a large, selectable card with an icon,
 * title, and description. Only one method can be selected at a time.
 *
 * ## Payment Method Options:
 *
 * ### CARD (NFC Contactless):
 * - Uses Host Card Emulation (HCE) via payment SDK
 * - Customer taps physical card on device
 * - Supports Visa and Mastercard, for now
 * - Proceeds to tip selection, then card tap screen
 * - May require PIN entry for certain transactions
 *
 * ### IPS (Instant Payment System):
 * - Generates QR code for mobile banking apps
 * - Customer scans QR with their banking app
 * - Real-time payment via Serbian IPS infrastructure
 * - Proceeds to tip selection, then QR display screen
 * - No physical card required
 *
 * ## UI Layout:
 * - Header with back button and "Payment Method" title
 * - Amount display card showing transaction amount
 * - Two large selection cards in vertical layout:
 *   * Card payment option with card icon
 *   * IPS payment option with QR/mobile icon
 * - Continue button (enabled only after selection)
 *
 * ## Selection Behavior:
 * - Cards use radio button style - only one can be selected
 * - Selected card highlights with checkmark indicator
 * - Continue button grays out until a method is selected
 *
 * ## Navigation Flow:
 * ```
 * amount_entry → payment_method → tip_selection → [card_tap or ips_qr]
 * ```
 *
 * @param amountInPare Transaction amount in minor units (pare/cents). Displayed
 *                     in the amount card and passed to next screen.
 * @param onNavigateBack Callback when back button clicked - returns to amount entry screen
 * @param onContinue Callback when continue button clicked - receives selected PaymentMethod
 *                   (either PaymentMethod.CARD or PaymentMethod.IPS), navigates to
 *                   tip_selection route with amount and method parameters
 *
 * @see PaymentMethod enumeration for CARD and IPS options
 * @see TipSelectionScreen for next step in payment flow
 * @see AmountEntryScreen for previous step in payment flow
 */
@Composable
fun PaymentMethodScreen(
    amountInPare: Long,
    onNavigateBack: () -> Unit = {},
    onContinue: (PaymentMethod) -> Unit = {}
) {
    val TAG = "PaymentMethodScreen"

    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }

    val displayAmount = formatAmount(amountInPare)

    val isButtonEnabled = selectedMethod != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PaymentMethodHeader(
                title = stringResource(R.string.payment_method_title),
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(32.dp))

            AmountDisplayCard(displayAmount)

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = stringResource(R.string.payment_method_select_label),
                fontSize = 18.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 24.dp),
                letterSpacing = TextUnit(2f, TextUnitType.Sp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PaymentMethodOption(
                    icon = R.drawable.card,
                    title = stringResource(R.string.payment_method_card),
                    logos = listOf(
                        R.drawable.icon_mastercard,
                        R.drawable.icon_visa
                    ),
                    isSelected = selectedMethod == PaymentMethod.CARD,
                    onClick = {
                        Log.d(TAG, "Card payment selected")
                        selectedMethod = PaymentMethod.CARD
                    }
                )

                PaymentMethodOption(
                    icon = R.drawable.qr,
                    title = stringResource(R.string.payment_method_ips),
                    logos = listOf(R.drawable.icon_ips),
                    isSelected = selectedMethod == PaymentMethod.IPS,
                    onClick = {
                        Log.d(TAG, "IPS payment selected")
                        selectedMethod = PaymentMethod.IPS
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedMethod?.let { method ->
                        Log.d(TAG, "Continue with $method")
                        onContinue(method)
                    }
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
                    text = stringResource(R.string.payment_method_continue),
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
private fun PaymentMethodHeader(
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
 * Payment method option card.
 */
@Composable
private fun PaymentMethodOption(
    icon: Int,
    title: String,
    logos: List<Int>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    logos.forEach { logo ->
                        Image(
                            painter = painterResource(id = logo),
                            contentDescription = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                CustomCheckbox(isChecked = isSelected)
            }
        }
    }
}
@Composable
private fun CustomCheckbox(
    isChecked: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (isChecked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = if (isChecked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


/**
 * Payment method enum.
 */
enum class PaymentMethod {
    CARD,
    IPS
}

/**
 * Format amount from pare to RSD.
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
fun PaymentMethodScreenPreview() {
    AppTheme {
        PaymentMethodScreen(
            amountInPare = 234000L,
            onNavigateBack = {},
            onContinue = {}
        )
    }
}