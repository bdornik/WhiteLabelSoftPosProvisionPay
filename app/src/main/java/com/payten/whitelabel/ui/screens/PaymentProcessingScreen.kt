package com.payten.whitelabel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.R
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * PaymentProcessingScreen.kt
 *
 * Displays a white screen with a red loading indicator while a payment transaction is being processed.
 * This screen appears after the card has been tapped and while the SDK processes the transaction.
 *
 * Design:
 * - White background
 * - Red circular progress indicator
 * - "Processing payment..." text
 */
@Composable
fun PaymentProcessingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Red loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.primary, // Red color matching app theme
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Processing text
            Text(
                text = stringResource(R.string.payment_processing_message),
                fontSize = 20.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.payment_processing_subtitle),
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(name = "Payment Processing Screen")
@Composable
fun PaymentProcessingScreenPreview() {
    AppTheme {
        PaymentProcessingScreen()
    }
}
