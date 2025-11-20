package com.payten.whitelabel.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.R
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * Card processing screen - shows "Tap card" UI while PosActivity runs in background.
 *
 * This screen only displays UI. PosActivity is launched by navigation.
 *
 * @param amountInPare Transaction amount in minor units
 * @param tipAmount Tip amount in minor units
 * @param onNavigateBack Callback when back button is clicked (cancels transaction)
 */
@Composable
fun CardProcessingScreen(
    amountInPare: Long,
    tipAmount: Long = 0L,
    onNavigateBack: () -> Unit = {}
) {
    val displayAmount = formatAmount(amountInPare + tipAmount)

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
            CardProcessingHeader(
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(32.dp))

            AmountDisplayCard(
                amount = displayAmount
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.card_processing_tap_card_label),
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_phone_pos),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            }

            ProcessingIndicator()

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mastercard),
                    contentDescription = null,
                    modifier = Modifier.height(16.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.visa),
                    contentDescription = null,
                    modifier = Modifier.height(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.card_processing_cancel),
                    fontSize = 18.sp,
                    fontFamily = MyriadPro,
                    letterSpacing = 1.sp,
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
private fun CardProcessingHeader(
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
            text = stringResource(R.string.card_processing_title),
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
 * Amount display card with currency.
 */
@Composable
private fun AmountDisplayCard(
    amount: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.card_processing_amount_label),
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.currency_rsd),
                        fontSize = 12.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amount,
                fontSize = 40.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

/**
 * LED status indicators (4 circles) with animation.
 */
@Composable
private fun ProcessingIndicator() {
    var currentLed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            currentLed = (currentLed + 1) % 4
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(24.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (index == currentLed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.LightGray
                        },
                        shape = CircleShape
                    )
            )
        }
    }
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
fun CardProcessingScreenPreview() {
    AppTheme {
        CardProcessingScreen(
            amountInPare = 234000L,
            tipAmount = 23400L
        )
    }
}