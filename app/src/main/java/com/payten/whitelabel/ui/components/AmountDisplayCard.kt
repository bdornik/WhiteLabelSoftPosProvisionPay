package com.payten.whitelabel.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.R
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * Displays a transaction amount in a prominent card-style UI component.
 *
 * This composable presents the payment amount in a visually distinctive card with:
 * - Elevated white surface with rounded corners
 * - Large, bold amount text (40sp)
 * - "Amount" label above
 * - "RSD" currency indicator below
 *
 * Used across payment flow screens:
 * - CardProcessingScreen (during card tap)
 * - TipSelectionScreen (showing total)
 * - IpsQRScreen (showing total)
 * - PaymentMethodScreen (showing total)
 *
 * @param amount Formatted amount string (e.g., "2.340,00") - must be pre-formatted with
 *               thousand separators (.) and decimal comma (,) in Serbian format
 */
@Composable
fun AmountDisplayCard(
    amount: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
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
            Text(
                text = stringResource(R.string.card_processing_amount_label),
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amount,
                fontSize = 40.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

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
}