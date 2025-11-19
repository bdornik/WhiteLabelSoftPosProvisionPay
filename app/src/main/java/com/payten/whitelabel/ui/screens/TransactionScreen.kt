package com.payten.whitelabel.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Transaction receipt screen.
 *
 * Displays transaction details in receipt style with dynamic card logo.
 *
 * @param transactionData Transaction details to display
 * @param onNavigateHome Callback to navigate back to home
 * @param onShare Callback when share button is clicked
 * @param onPrint Callback when print button is clicked
 */
@Composable
fun TransactionScreen(
    transactionData: TransactionDetailsDto,
    onNavigateHome: () -> Unit = {},
    onShare: () -> Unit = {},
    onPrint: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TransactionHeader(
                onNavigateBack = onNavigateHome,
                onPrint = onPrint,
                onShare = onShare
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                ReceiptCard(transactionData = transactionData)
            }
        }
    }
}

/**
 * Header with back, print, and share buttons.
 */
@Composable
private fun TransactionHeader(
    onNavigateBack: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(
            onClick = onNavigateBack
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onPrint,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.print),
                    contentDescription = stringResource(R.string.transaction_print),
                    tint = Color.Black
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share),
                    contentDescription = stringResource(R.string.transaction_share),
                    tint = Color.Black
                )
            }
        }
    }
}

/**
 * Receipt card with transaction details.
 */
@Composable
private fun ReceiptCard(transactionData: TransactionDetailsDto) {
    val cardLogo = getCardLogo(transactionData)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Image(
            painter = painterResource(id = R.drawable.slip),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentScale = ContentScale.FillWidth
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.transaction_receipt_amount_label),
                    fontSize = 14.sp,
                    fontFamily = MyriadPro,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = stringResource(R.string.currency_rsd),
                    fontSize = 12.sp,
                    fontFamily = MyriadPro,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatAmount(transactionData.amount),
                fontSize = 32.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDate(transactionData.dateTime),
                    fontSize = 12.sp,
                    fontFamily = MyriadPro,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    painter = painterResource(id = R.drawable.time),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime(transactionData.dateTime),
                    fontSize = 12.sp,
                    fontFamily = MyriadPro,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_amount),
                value = "${formatAmount(transactionData.amount)} ${stringResource(R.string.currency_rsd)}"
            )

            if (transactionData.tipAmount != "0.0" && transactionData.tipAmount.isNotEmpty()) {
                TransactionDetailRow(
                    label = stringResource(R.string.transaction_receipt_tip),
                    value = "${formatAmount(transactionData.tipAmount)} ${stringResource(R.string.currency_rsd)}"
                )
            }

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_status),
                value = transactionData.message,
                valueColor = if (transactionData.response.equals("00", ignoreCase = true))
                    Color(0xFF4CAF50) else Color(0xFFEB3223)
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_mid),
                value = transactionData.merchantId
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_tid),
                value = transactionData.terminalId
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_merchant),
                value = transactionData.merchantName
            )

            if (!transactionData.isIps) {
                TransactionDetailRow(
                    label = stringResource(R.string.transaction_receipt_card_number),
                    value = transactionData.cardNumber
                )

                TransactionDetailRow(
                    label = stringResource(R.string.transaction_receipt_auth_code),
                    value = transactionData.authorizationCode
                )
            } else {
                TransactionDetailRow(
                    label = stringResource(R.string.transaction_receipt_e2e),
                    value = transactionData.rrn
                )
            }

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_operation),
                value = transactionData.operationName
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_response),
                value = transactionData.response
            )

            TransactionDetailRow(
                label = stringResource(R.string.transaction_receipt_message),
                value = transactionData.message
            )

            if (!transactionData.aid.isNullOrEmpty()) {
                TransactionDetailRow(
                    label = stringResource(R.string.transaction_receipt_aid),
                    value = transactionData.aid,
                    isLast = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cardLogo != null) {
                Image(
                    painter = painterResource(id = cardLogo),
                    contentDescription = null,
                    modifier = Modifier.height(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Single transaction detail row.
 */
@Composable
private fun TransactionDetailRow(
    label: String,
    value: String?,
    valueColor: Color = Color.Black,
    isLast: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isLast) {
            HorizontalDivider(
                color = Color(0xFFE0E0E0).copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

/**
 * Get card logo based on transaction data.
 */
private fun getCardLogo(transactionData: TransactionDetailsDto): Int? {
    return when {
        transactionData.isIps -> null // IPS has no logo
        transactionData.applicationLabel?.contains("visa", ignoreCase = true) == true -> R.drawable.visa
        transactionData.applicationLabel?.contains("master", ignoreCase = true) == true -> R.drawable.mastercard
        else -> null // Unknown - no logo
    }
}

/**
 * Format amount from string to display format.
 */
@SuppressLint("DefaultLocale")
private fun formatAmount(amount: String): String {
    return try {
        val value = amount.toDouble()
        String.format("%.2f", value).replace(".", ",")
    } catch (_: Exception) {
        amount
    }
}

/**
 * Format date from ISO string.
 */
private fun formatDate(dateTime: String): String {
    return try {
        val parsed = LocalDateTime.parse(dateTime)
        parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (_: Exception) {
        dateTime
    }
}

/**
 * Format time from ISO string.
 */
private fun formatTime(dateTime: String): String {
    return try {
        val parsed = LocalDateTime.parse(dateTime)
        parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        dateTime
    }
}

@Preview(name = "Success - VISA")
@Composable
fun TransactionScreenSuccessVisa() {
    AppTheme {
        TransactionScreen(
            transactionData = TransactionDetailsDto(
                aid = "A0000000031010",
                applicationLabel = "VISA",
                authorizationCode = "046667",
                bankName = "",
                cardNumber = "************5804",
                dateTime = "2025-01-15T14:30:00",
                merchantId = "DU160014",
                merchantName = "Petar Petrović",
                message = "000 Approved",
                operationName = "Prodaja",
                response = "00",
                rrn = "",
                code = "DU160014",
                status = "A",
                terminalId = "DU160014",
                amount = "234.00",
                isIps = false,
                sdkStatus = null,
                billStatus = null,
                color = -1,
                recordId = "123",
                listName = "",
                tipAmount = "23.40"
            )
        )
    }
}

@Preview(name = "Success - Mastercard")
@Composable
fun TransactionScreenSuccessMastercard() {
    AppTheme {
        TransactionScreen(
            transactionData = TransactionDetailsDto(
                aid = "A0000000041010",
                applicationLabel = "Mastercard",
                authorizationCode = "046667",
                bankName = "",
                cardNumber = "************5804",
                dateTime = "2025-01-15T14:30:00",
                merchantId = "DU160014",
                merchantName = "Petar Petrović",
                message = "000 Approved",
                operationName = "Prodaja",
                response = "00",
                rrn = "",
                code = "DU160014",
                status = "A",
                terminalId = "DU160014",
                amount = "234.00",
                isIps = false,
                sdkStatus = null,
                billStatus = null,
                color = -1,
                recordId = "123",
                listName = "",
                tipAmount = "23.40"
            )
        )
    }
}

@Preview(name = "IPS Transaction")
@Composable
fun TransactionScreenIPS() {
    AppTheme {
        TransactionScreen(
            transactionData = TransactionDetailsDto(
                aid = "",
                applicationLabel = "",
                authorizationCode = "",
                bankName = "",
                cardNumber = "",
                dateTime = "2025-01-15T14:30:00",
                merchantId = "DU160014",
                merchantName = "Petar Petrović",
                message = "000 Approved",
                operationName = "Prodaja",
                response = "00",
                rrn = "123456789012",
                code = "DU160014",
                status = "A",
                terminalId = "DU160014",
                amount = "234.00",
                isIps = true,
                sdkStatus = null,
                billStatus = null,
                color = -1,
                recordId = "123",
                listName = "",
                tipAmount = "0.0"
            )
        )
    }
}