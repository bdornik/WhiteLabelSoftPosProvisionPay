package com.payten.whitelabel.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.payten.whitelabel.R
import com.payten.whitelabel.activities.IpsActivity
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import kotlin.jvm.java

/**
 * Tip selection screen.
 *
 * User selects tip percentage or no tip.
 *
 * @param amountInPare Transaction amount in minor units (pare)
 * @param paymentMethod Selected payment method
 * @param onNavigateBack Callback when back button is clicked
 * @param onTransactionComplete Callback with transaction data when hardware activity completes
 */
@Composable
fun TipSelectionScreen(
    amountInPare: Long,
    paymentMethod: PaymentMethod,
    onNavigateBack: () -> Unit = {},
    onContinueCard: (tipAmount: Long) -> Unit = {},
    onTransactionComplete: (TransactionDetailsDto) -> Unit = {}
) {
    val TAG = "TipSelectionScreen"
    val context = LocalContext.current

    var selectedTip by remember { mutableStateOf<TipOption?>(null) }

    val displayAmount = formatAmount(amountInPare)

    // Calculate final amount with tip
    val finalAmount = remember(amountInPare, selectedTip) {
        selectedTip?.let { tip ->
            when (tip) {
                is TipOption.Percentage -> amountInPare + (amountInPare * tip.percent / 100)
                TipOption.NoTip -> amountInPare
                TipOption.Custom -> amountInPare // TODO: implement custom input
            }
        } ?: amountInPare
    }

    val isButtonEnabled = selectedTip != null

    val ipsTransactionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "IPS transaction returned: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            val transactionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("transaction_data", TransactionDetailsDto::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra("transaction_data") as? TransactionDetailsDto
            }

            if (transactionData != null) {
                Log.d(TAG, "IPS transaction completed: ${transactionData.response}")
                onTransactionComplete(transactionData)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TipSelectionHeader(
                title = stringResource(R.string.tip_selection_title),
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFF001F1F),
                                    0.6f to Color(0xFF001F3F),
                                    1.0f to Color(0xFF0051A8)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.payment_method_amount_label),
                            fontSize = 18.sp,
                            fontFamily = MyriadPro,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.currency_rsd),
                                fontSize = 14.sp,
                                fontFamily = MyriadPro,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = displayAmount,
                        fontSize = 44.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.tip_selection_label),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TipOptionCard(
                        label = "5%",
                        isSelected = selectedTip == TipOption.Percentage(5),
                        onClick = {
                            Log.d(TAG, "5% tip selected")
                            selectedTip = TipOption.Percentage(5)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    TipOptionCard(
                        label = "10%",
                        isSelected = selectedTip == TipOption.Percentage(10),
                        onClick = {
                            Log.d(TAG, "10% tip selected")
                            selectedTip = TipOption.Percentage(10)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TipOptionCard(
                        label = "15%",
                        isSelected = selectedTip == TipOption.Percentage(15),
                        onClick = {
                            Log.d(TAG, "15% tip selected")
                            selectedTip = TipOption.Percentage(15)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    TipOptionCard(
                        label = "20%",
                        isSelected = selectedTip == TipOption.Percentage(20),
                        onClick = {
                            Log.d(TAG, "20% tip selected")
                            selectedTip = TipOption.Percentage(20)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TipOptionCard(
                        label = stringResource(R.string.tip_custom),
                        isSelected = selectedTip == TipOption.Custom,
                        onClick = {
                            Log.d(TAG, "Custom tip selected")
                            selectedTip = TipOption.Custom
                            // TODO: Show custom input dialog
                        },
                        modifier = Modifier.weight(1f)
                    )

                    TipOptionCard(
                        label = stringResource(R.string.tip_no_tip),
                        isSelected = selectedTip == TipOption.NoTip,
                        onClick = {
                            Log.d(TAG, "No tip selected")
                            selectedTip = TipOption.NoTip
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedTip?.let { _ ->
                        val tipAmount = finalAmount - amountInPare

                        Log.d(TAG, "Confirm clicked")

                        when (paymentMethod) {
                            PaymentMethod.CARD -> {
                                onContinueCard(tipAmount)
                            }
                            PaymentMethod.IPS -> {
                                val intent = Intent(context, IpsActivity::class.java).apply {
                                    putExtra("Amount", finalAmount.toString())
                                }
                                ipsTransactionLauncher.launch(intent)
                            }
                        }
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
                    text = stringResource(R.string.tip_confirm),
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
private fun TipSelectionHeader(
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
 * Tip option card.
 */
@Composable
private fun TipOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.75f)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Tip option sealed class.
 */
sealed class TipOption {
    data class Percentage(val percent: Int) : TipOption()
    object NoTip : TipOption()
    object Custom : TipOption()
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
fun TipSelectionScreenPreview() {
    AppTheme {
        TipSelectionScreen(
            amountInPare = 234000L,
            paymentMethod = PaymentMethod.CARD,
            onNavigateBack = {},
            onTransactionComplete = {}
        )
    }
}