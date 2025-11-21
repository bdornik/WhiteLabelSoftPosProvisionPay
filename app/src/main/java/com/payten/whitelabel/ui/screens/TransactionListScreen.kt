package com.payten.whitelabel.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.dto.TransactionDto
import com.payten.whitelabel.dto.transactions.GetTransactionsRequest
import com.payten.whitelabel.enums.TransactionStatus
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.viewmodel.TrafficViewModel
import com.payten.whitelabel.viewmodel.VoidTransactionState
import com.payten.whitelabel.viewmodel.VoidTransactionViewModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Transactions list screen with ViewModel integration.
 *
 * Displays list of transactions with filtering, sorting, and void functionality.
 *
 * @param sharedPreferences SharedPreferences instance
 * @param onNavigateBack Callback when back button is clicked
 * @param onTransactionDetailsClick Callback when details icon is clicked
 * @param onFilterClick Callback when filter button is clicked
 * @param trafficViewModel ViewModel for managing transactions
 * @param voidViewModel ViewModel for void operations
 */
@Composable
fun TransactionsListScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {},
    onTransactionDetailsClick: (TransactionDto) -> Unit = {},
    onFilterClick: () -> Unit = {},
    trafficViewModel: TrafficViewModel = hiltViewModel(),
    voidViewModel: VoidTransactionViewModel = hiltViewModel()
) {
    val transactions by trafficViewModel.transactionResultsSuccess.observeAsState(emptyList())
    val voidState by voidViewModel.voidState.observeAsState(VoidTransactionState.Idle)

    var expandedTransactionId by remember { mutableStateOf<String?>(null) }
    var showVoidConfirmDialog by remember { mutableStateOf<TransactionDto?>(null) }

    // Handle void state changes
    LaunchedEffect(voidState) {
        when (voidState) {
            is VoidTransactionState.Success -> {
                // Refresh transactions list after successful void
                val userId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
                val terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, "")
                val dateFrom = LocalDateTime.now().minusDays(90)
                val dateTo = LocalDateTime.now()
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

                trafficViewModel.getTransactionsFromServer(
                    GetTransactionsRequest(
                        userId = userId,
                        dateFrom = dateFrom.format(dateFormatter),
                        dateTo = dateTo.format(dateFormatter),
                        tid = terminalId
                    )
                )
            }
            else -> {}
        }
    }

    // Load transactions when screen loads
    LaunchedEffect(Unit) {
        val userId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
        val terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, "")

        val dateFrom = LocalDateTime.now().minusDays(90)
        val dateTo = LocalDateTime.now()

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        trafficViewModel.getTransactionsFromServer(
            GetTransactionsRequest(
                userId = userId,
                dateFrom = dateFrom.format(dateFormatter),
                dateTo = dateTo.format(dateFormatter),
                tid = terminalId
            )
        )
    }

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
            TransactionsListHeader(
                onNavigateBack = onNavigateBack,
                onFilterClick = onFilterClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isNullOrEmpty()) {
                EmptyTransactionsList()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions!!) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            isExpanded = expandedTransactionId == transaction.recordId,
                            onClick = {
                                expandedTransactionId = if (expandedTransactionId == transaction.recordId) {
                                    null
                                } else {
                                    transaction.recordId
                                }
                            },
                            onDetailsClick = {
                                onTransactionDetailsClick(transaction)
                            },
                            onVoidClick = {
                                showVoidConfirmDialog = transaction
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        showVoidConfirmDialog?.let { transaction ->
            VoidConfirmationDialog(
                transaction = transaction,
                onConfirm = {
                    val transactionData = TransactionDetailsDto(
                        aid = transaction.transactionId ?: "",
                        applicationLabel = transaction.applicationLabel ?: "",
                        authorizationCode = transaction.authorizationCode ?: "",
                        bankName = "",
                        cardNumber = transaction.maskedPAN ?: "",
                        dateTime = transaction.transactionDate.toString(),
                        merchantId = transaction.merchantId ?: "",
                        merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
                        message = transaction.screenMessage ?: "",
                        operationName = transaction.operationName ?: "Prodaja",
                        response = transaction.responseCode ?: "",
                        rrn = transaction.creaditTransferIdentificator ?: "",
                        code = transaction.recordId,
                        status = transaction.statusCode ?: "",
                        terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""),
                        amount = transaction.amount,
                        isIps = transaction.isIps ?: false,
                        sdkStatus = transaction.status,
                        billStatus = null,
                        color = -1,
                        recordId = transaction.recordId,
                        listName = "",
                        tipAmount = transaction.tipAmount
                    )

                    voidViewModel.startVoidTransaction(transactionData)
                    showVoidConfirmDialog = null
                    expandedTransactionId = null
                },
                onDismiss = {
                    showVoidConfirmDialog = null
                }
            )
        }

        // Processing overlay
        if (voidState is VoidTransactionState.Processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.void_processing),
                        color = Color.White,
                        fontFamily = MyriadPro,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Success dialog
        if (voidState is VoidTransactionState.Success) {
            AlertDialog(
                onDismissRequest = { voidViewModel.resetState() },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_success),
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.void_success_title),
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.void_success_message),
                        fontFamily = MyriadPro
                    )
                },
                confirmButton = {
                    Button(onClick = { voidViewModel.resetState() }) {
                        Text(
                            text = stringResource(R.string.dialog_button_ok_default),
                            fontFamily = MyriadPro
                        )
                    }
                }
            )
        }

        // Error dialog
        if (voidState is VoidTransactionState.Failed) {
            AlertDialog(
                onDismissRequest = { voidViewModel.resetState() },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_warning),
                        contentDescription = null,
                        tint = Color(0xFFEB3223),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.void_failed_title),
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = (voidState as VoidTransactionState.Failed).message,
                        fontFamily = MyriadPro
                    )
                },
                confirmButton = {
                    Button(onClick = { voidViewModel.resetState() }) {
                        Text(
                            text = stringResource(R.string.dialog_button_ok_default),
                            fontFamily = MyriadPro
                        )
                    }
                }
            )
        }
    }
}

/**
 * Void confirmation dialog
 */
@Composable
private fun VoidConfirmationDialog(
    transaction: TransactionDto,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.icon_warning),
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.void_confirmation_title),
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.void_confirmation_message),
                    fontFamily = MyriadPro
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${formatAmount(transaction.amount)} ${stringResource(R.string.currency_rsd)}",
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEB3223)
                )
            ) {
                Text(
                    text = stringResource(R.string.void_confirm_button),
                    fontFamily = MyriadPro
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.void_cancel_button),
                    fontFamily = MyriadPro
                )
            }
        }
    )
}

/**
 * Header with back and filter buttons.
 */
@Composable
private fun TransactionsListHeader(
    onNavigateBack: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Text(
            text = stringResource(R.string.transactions_title),
            fontSize = 20.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onFilterClick) {
                Icon(
                    painter = painterResource(id = R.drawable.filter),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Single transaction card with expandable void button.
 */
@Composable
private fun TransactionCard(
    transaction: TransactionDto,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onVoidClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${formatAmount(transaction.amount)} ${stringResource(R.string.currency_rsd)}",
                        fontSize = 16.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendar),
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatDate(transaction.transactionDate.toString()),
                            fontSize = 12.sp,
                            fontFamily = MyriadPro,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            painter = painterResource(id = R.drawable.time),
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatTime(transaction.transactionDate.toString()),
                            fontSize = 12.sp,
                            fontFamily = MyriadPro,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TransactionStatusBadge(status = transaction.status!!)
                }

                IconButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.transaction_details),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = onVoidClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.transaction_void_button),
                            fontSize = 16.sp,
                            fontFamily = MyriadPro,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Transaction status badge - outlined style with light background.
 */
@SuppressLint("MissingColorAlphaChannel")
@Composable
private fun TransactionStatusBadge(status: TransactionStatus) {
    val (backgroundColor, borderColor, textColor, text) = when (status) {
        TransactionStatus.Accepted -> Quadruple(
            Color(0xFF4CAF50).copy(alpha = 0.1f),
            Color(0xFF4CAF50),
            Color(0xFF4CAF50),
            stringResource(R.string.transaction_status_accepted_label)
        )
        TransactionStatus.Rejected -> Quadruple(
            Color(0xFFEB3223).copy(alpha = 0.1f),
            Color(0xFFEB3223),
            Color(0xFFEB3223),
            stringResource(R.string.transaction_status_rejected_label)
        )
        TransactionStatus.Voided -> Quadruple(
            Color(0xFFFFA000).copy(alpha = 0.1f),
            Color(0xFFFFA000),
            Color(0xFFFFA000),
            stringResource(R.string.transaction_status_voided)
        )
        TransactionStatus.Pending -> Quadruple(
            Color(0xFFFFA000).copy(alpha = 0.1f),
            Color(0xFFFFA000),
            Color(0xFFFFA000),
            stringResource(R.string.transaction_status_pending)
        )
        TransactionStatus.WrongPin -> Quadruple(
            Color(0xFFEB3223).copy(alpha = 0.1f),
            Color(0xFFEB3223),
            Color(0xFFEB3223),
            stringResource(R.string.transaction_status_wrong_pin_message)
        )
        TransactionStatus.PinNotEntered -> Quadruple(
            Color(0xFFFFA000).copy(alpha = 0.1f),
            Color(0xFFFFA000),
            Color(0xFFFFA000),
            stringResource(R.string.transaction_status_pin_not_entered_message)
        )
        TransactionStatus.Reversed -> Quadruple(
            Color(0xFF757575).copy(alpha = 0.1f),
            Color(0xFF757575),
            Color(0xFF757575),
            stringResource(R.string.transaction_status_reversed_message)
        )
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

// Helper data class for status badge colors
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Empty state when no transactions.
 */
@Composable
private fun EmptyTransactionsList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.traffic),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = stringResource(R.string.transactions_empty),
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
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
        dateTime.substringBefore("T")
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
        dateTime.substringAfter("T").substringBefore(".")
    }
}

/**
 * Preview-friendly version without ViewModel
 */
@Composable
private fun TransactionsListScreenContent(
    transactions: List<TransactionDto>,
    onNavigateBack: () -> Unit = {},
    onTransactionDetailsClick: (TransactionDto) -> Unit = {},
    onVoidClick: (TransactionDto) -> Unit = {},
    onFilterClick: () -> Unit = {}
) {
    var expandedTransactionId by remember { mutableStateOf<String?>(null) }

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
            TransactionsListHeader(
                onNavigateBack = onNavigateBack,
                onFilterClick = onFilterClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                EmptyTransactionsList()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            isExpanded = expandedTransactionId == transaction.recordId,
                            onClick = {
                                expandedTransactionId = if (expandedTransactionId == transaction.recordId) {
                                    null
                                } else {
                                    transaction.recordId
                                }
                            },
                            onDetailsClick = {
                                onTransactionDetailsClick(transaction)
                            },
                            onVoidClick = {
                                onVoidClick(transaction)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(name = "Transactions List - Multiple")
@Composable
fun TransactionsListScreenPreview() {
    AppTheme {
        TransactionsListScreenContent(
            transactions = listOf(
                TransactionDto(
                    amount = "2340.00",
                    amountDouble = 2340.00,
                    recordId = "123456",
                    transactionId = "A0000000031010",
                    statusCode = "a",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 14, 30),
                    responseCode = "00",
                    source = com.payten.whitelabel.enums.TransactionSource.POS,
                    screenMessage = "Approved",
                    status = TransactionStatus.Accepted,
                    authorizationCode = "046667",
                    maskedPAN = "************5804",
                    merchantId = "DU160014",
                    isIps = false,
                    creaditTransferIdentificator = "",
                    applicationLabel = "VISA",
                    operationName = null,
                    applicationId = "A0000000031010",
                    tipAmount = "0.0",
                    newest = true
                ),
                TransactionDto(
                    amount = "1500.00",
                    amountDouble = 1500.00,
                    recordId = "123457",
                    transactionId = "",
                    statusCode = "f",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 13, 15),
                    responseCode = "06",
                    source = com.payten.whitelabel.enums.TransactionSource.POS,
                    screenMessage = "Rejected",
                    status = TransactionStatus.Rejected,
                    authorizationCode = "",
                    maskedPAN = "************1234",
                    merchantId = "DU160014",
                    isIps = false,
                    creaditTransferIdentificator = "",
                    applicationLabel = "Mastercard",
                    operationName = null,
                    applicationId = "",
                    tipAmount = "0.0",
                    newest = false
                ),
                TransactionDto(
                    amount = "890.50",
                    amountDouble = 890.50,
                    recordId = "987654321012",
                    transactionId = "",
                    statusCode = "a",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 12, 45),
                    responseCode = "00",
                    source = com.payten.whitelabel.enums.TransactionSource.IPS,
                    screenMessage = "Approved",
                    status = TransactionStatus.Accepted,
                    authorizationCode = "",
                    maskedPAN = "",
                    merchantId = "DU160014",
                    isIps = true,
                    creaditTransferIdentificator = "987654321012",
                    applicationLabel = "",
                    operationName = null,
                    applicationId = "",
                    tipAmount = "0.0",
                    newest = false
                ),
                TransactionDto(
                    amount = "450.00",
                    amountDouble = 450.00,
                    recordId = "123458",
                    transactionId = "A0000000031010",
                    statusCode = "v",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 10, 20),
                    responseCode = "06",
                    source = com.payten.whitelabel.enums.TransactionSource.POS,
                    screenMessage = "Voided",
                    status = TransactionStatus.Voided,
                    authorizationCode = "046668",
                    maskedPAN = "************9876",
                    merchantId = "DU160014",
                    isIps = false,
                    creaditTransferIdentificator = "",
                    applicationLabel = "VISA",
                    operationName = null,
                    applicationId = "A0000000031010",
                    tipAmount = "0.0",
                    newest = false
                )
            ),
            onNavigateBack = {},
            onTransactionDetailsClick = {},
            onVoidClick = {},
            onFilterClick = {}
        )
    }
}

@Preview(name = "Transactions List - Empty")
@Composable
fun TransactionsListScreenEmptyPreview() {
    AppTheme {
        TransactionsListScreenContent(
            transactions = emptyList(),
            onNavigateBack = {},
            onTransactionDetailsClick = {},
            onVoidClick = {},
            onFilterClick = {}
        )
    }
}

@Preview(name = "Transactions List - Single")
@Composable
fun TransactionsListScreenSinglePreview() {
    AppTheme {
        TransactionsListScreenContent(
            transactions = listOf(
                TransactionDto(
                    amount = "1234.56",
                    amountDouble = 1234.56,
                    recordId = "123456",
                    transactionId = "A0000000031010",
                    statusCode = "a",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 15, 45),
                    responseCode = "00",
                    source = com.payten.whitelabel.enums.TransactionSource.POS,
                    screenMessage = "Approved",
                    status = TransactionStatus.Accepted,
                    authorizationCode = "046667",
                    maskedPAN = "************5804",
                    merchantId = "DU160014",
                    isIps = false,
                    creaditTransferIdentificator = "",
                    applicationLabel = "VISA",
                    operationName = null,
                    applicationId = "A0000000031010",
                    tipAmount = "0.0",
                    newest = true
                )
            ),
            onNavigateBack = {},
            onTransactionDetailsClick = {},
            onVoidClick = {},
            onFilterClick = {}
        )
    }
}