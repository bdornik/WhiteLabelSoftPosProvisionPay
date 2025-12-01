package com.payten.whitelabel.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.navigation.NavController
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.dto.TransactionDto
import com.payten.whitelabel.dto.transactions.GetTransactionsRequest
import com.payten.whitelabel.enums.TransactionSortType
import com.payten.whitelabel.enums.TransactionSource
import com.payten.whitelabel.enums.TransactionStatus
import com.payten.whitelabel.enums.TransactionStatusFilterType
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
 * * LOGIC UPDATE: The Void button appears ONLY on the absolute latest voidable (Accepted)
 * transaction from the full history, regardless of current filters.
 */
@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    sharedPreferences: KsPrefs,
    navController: NavController? = null,
    onNavigateBack: () -> Unit = {},
    onTransactionDetailsClick: (TransactionDto) -> Unit = {},
    onFilterClick: () -> Unit = {},
    onNavigateToVoidProcessing: (TransactionDetailsDto) -> Unit = {}
) {
    val trafficViewModel: TrafficViewModel = if (navController != null) {
        hiltViewModel(
            viewModelStoreOwner = navController.getBackStackEntry(navController.graph.id)
        )
    } else {
        hiltViewModel()
    }

    val voidViewModel: VoidTransactionViewModel = if (navController != null) {
        hiltViewModel(
            viewModelStoreOwner = navController.getBackStackEntry(navController.graph.id)
        )
    } else {
        hiltViewModel()
    }
    val transactions by trafficViewModel.transactionResultsSuccess.observeAsState(emptyList())
    val isLoading by trafficViewModel.isLoading.observeAsState(false)
    val voidState by voidViewModel.voidState.observeAsState(VoidTransactionState.Idle)

    var showVoidConfirmDialog by remember { mutableStateOf<TransactionDto?>(null) }

    // Track filter changes
    var filterTrigger by remember { mutableIntStateOf(0) }

    // Apply filters
    val filteredTransactions = remember(transactions, filterTrigger) {
        applyFiltersAndSorting(transactions ?: emptyList(), sharedPreferences)
    }

    // CRITICAL FIX: Determine the absolute latest voidable transaction ID from the FULL list.
    // We ignore filters here. If the latest transaction is hidden by a filter,
    // no other transaction should inherit the "Void" button.
    val latestVoidableRecordId = remember(transactions) {
        transactions
            ?.filter { it.status == TransactionStatus.Accepted } // Only Accepted transactions can be voided
            ?.maxByOrNull { it.transactionDate ?: LocalDateTime.MIN } // Find the most recent one
            ?.recordId
    }

    val hasActiveFilters = remember(filterTrigger) {
        hasActiveFilters(sharedPreferences)
    }

    fun loadTransactions() {
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

    LaunchedEffect(voidState) {
        if (voidState is VoidTransactionState.Success) {
            loadTransactions()
        }
    }

    LaunchedEffect(Unit) {
        if (trafficViewModel.shouldLoadTransactions()) {
            loadTransactions()
        }
    }

    val filterApplied = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("filter_applied", false)
        ?.collectAsState()

    LaunchedEffect(filterApplied?.value) {
        if (filterApplied?.value == true) {
            loadTransactions()
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("filter_applied", false)
        }
    }

    val refreshNeeded = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("refresh_needed", false)
        ?.collectAsState()

    LaunchedEffect(refreshNeeded?.value) {
        if (refreshNeeded?.value == true) {
            loadTransactions()
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("refresh_needed", false)
        }
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
                onFilterClick = onFilterClick,
                hasActiveFilters = hasActiveFilters
            )

            Spacer(modifier = Modifier.height(16.dp))

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { loadTransactions() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredTransactions.isEmpty()) {
                    EmptyTransactionsList()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTransactions) { transaction ->
                            // Check if this specific transaction is the absolute latest voidable one
                            val showVoidButton = transaction.recordId == latestVoidableRecordId

                            TransactionCard(
                                transaction = transaction,
                                showVoidButton = showVoidButton,
                                onDetailsClick = { onTransactionDetailsClick(transaction) },
                                onVoidClick = { showVoidConfirmDialog = transaction }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }

        showVoidConfirmDialog?.let { transaction ->
            VoidConfirmationDialog(
                transaction = transaction,
                onConfirm = {
                    val transactionData = TransactionDetailsDto(
                        aid = transaction.applicationId,
                        applicationLabel = transaction.applicationLabel ?: "",
                        authorizationCode = transaction.recordId,
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
                    onNavigateToVoidProcessing(transactionData)
                    showVoidConfirmDialog = null
                },
                onDismiss = { showVoidConfirmDialog = null }
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionDto,
    showVoidButton: Boolean,
    onDetailsClick: () -> Unit,
    onVoidClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    val totalAmount = remember(transaction.amount, transaction.tipAmount) {
                        try {
                            val base = transaction.amount.toDouble()
                            val tip = if (transaction.tipAmount != "0.0") transaction.tipAmount.toDouble() else 0.0
                            (base + tip).toString()
                        } catch (_: Exception) { transaction.amount }
                    }

                    Text(
                        text = "${formatAmount(totalAmount)} ${stringResource(R.string.currency_rsd)}",
                        fontSize = 16.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.calendar), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(text = formatDate(transaction.transactionDate.toString()), fontSize = 12.sp, fontFamily = MyriadPro, color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(painter = painterResource(id = R.drawable.time), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(text = formatTime(transaction.transactionDate.toString()), fontSize = 12.sp, fontFamily = MyriadPro, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TransactionStatusBadge(status = transaction.status!!)
                }

                IconButton(onClick = onDetailsClick, modifier = Modifier.size(48.dp)) {
                    Icon(painter = painterResource(id = R.drawable.transaction_details), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(48.dp))
                }
            }

            // Persistent Void Button - Only shown if showVoidButton is true
            if (showVoidButton) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                    Button(
                        onClick = onVoidClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.transaction_void_button), fontSize = 16.sp, fontFamily = MyriadPro, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(name = "Void Button Visible (Latest)")
@Composable
fun PreviewTransactionCardWithVoid() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp).background(Color.White)) {
            TransactionCard(
                transaction = TransactionDto(
                    amount = "1234.56",
                    amountDouble = 1234.56,
                    recordId = "123456",
                    transactionId = "A0000000031010",
                    statusCode = "a",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 15, 45),
                    responseCode = "00",
                    source = TransactionSource.POS,
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
                showVoidButton = true,
                onDetailsClick = {},
                onVoidClick = {}
            )
        }
    }
}

@Preview(name = "Void Button Hidden (Older)")
@Composable
fun PreviewTransactionCardWithoutVoid() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp).background(Color.White)) {
            TransactionCard(
                TransactionDto(
                    amount = "1234.56",
                    amountDouble = 1234.56,
                    recordId = "123456",
                    transactionId = "A0000000031010",
                    statusCode = "a",
                    transactionDate = LocalDateTime.of(2025, 1, 21, 15, 45),
                    responseCode = "00",
                    source = TransactionSource.POS,
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
                showVoidButton = false, // Logic determined it's not latest
                onDetailsClick = {},
                onVoidClick = {}
            )
        }
    }
}

@Preview(name = "List Screen Simulation")
@Composable
fun PreviewTransactionsListScreenContent() {
    val transactions = listOf(
        TransactionDto(
            amount = "2340.00",
            amountDouble = 2340.00,
            recordId = "123456",
            transactionId = "A0000000031010",
            statusCode = "a",
            transactionDate = LocalDateTime.of(2025, 1, 21, 14, 30),
            responseCode = "00",
            source = TransactionSource.POS,
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
            source = TransactionSource.POS,
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
            source = TransactionSource.IPS,
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
            source = TransactionSource.POS,
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
    )

    // Logic simulation
    val latestVoidable = transactions
        .filter { it.status == TransactionStatus.Accepted }
        .maxByOrNull { it.transactionDate!! }?.recordId

    AppTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(transactions) { item ->
                    TransactionCard(
                        transaction = item,
                        showVoidButton = item.recordId == latestVoidable,
                        onDetailsClick = {},
                        onVoidClick = {}
                    )
                }
            }
        }
    }
}

// --- BOILERPLATE HELPERS AND COMPONENTS ---

@Composable
private fun VoidConfirmationDialog(
    transaction: TransactionDto,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painter = painterResource(id = R.drawable.icon_warning), contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(48.dp)) },
        title = { Text(text = stringResource(R.string.void_confirmation_title), fontFamily = MyriadPro, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text = stringResource(R.string.void_confirmation_message), fontFamily = MyriadPro)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${formatAmount(transaction.amount)} ${stringResource(R.string.currency_rsd)}", fontFamily = MyriadPro, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB3223))) {
                Text(text = stringResource(R.string.void_confirm_button), fontFamily = MyriadPro)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(text = stringResource(R.string.void_cancel_button), fontFamily = MyriadPro) }
        }
    )
}

@Composable
private fun TransactionsListHeader(onNavigateBack: () -> Unit, onFilterClick: () -> Unit, hasActiveFilters: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)
        Text(text = stringResource(R.string.transactions_title), fontSize = 20.sp, fontFamily = MyriadPro, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        IconButton(onClick = onFilterClick) {
            Icon(painter = painterResource(id = R.drawable.filter), contentDescription = null, tint = if (hasActiveFilters) Color(0xFFEB3223) else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@SuppressLint("MissingColorAlphaChannel")
@Composable
private fun TransactionStatusBadge(status: TransactionStatus) {
    val (backgroundColor, borderColor, textColor, text) = when (status) {
        TransactionStatus.Accepted -> Quadruple(Color(0xFF4CAF50).copy(alpha = 0.1f), Color(0xFF4CAF50), Color(0xFF4CAF50), stringResource(R.string.transaction_status_accepted_label))
        TransactionStatus.Rejected -> Quadruple(Color(0xFFEB3223).copy(alpha = 0.1f), Color(0xFFEB3223), Color(0xFFEB3223), stringResource(R.string.transaction_status_rejected_label))
        TransactionStatus.Voided -> Quadruple(Color(0xFFFFA000).copy(alpha = 0.1f), Color(0xFFFFA000), Color(0xFFFFA000), stringResource(R.string.transaction_status_voided))
        else -> Quadruple(Color.Gray.copy(alpha = 0.1f), Color.Gray, Color.Gray, "Unknown")
    }
    Box(modifier = Modifier.background(color = backgroundColor, shape = RoundedCornerShape(6.dp)).border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 2.dp)) {
        Text(text = text, fontSize = 12.sp, fontFamily = MyriadPro, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun EmptyTransactionsList() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painter = painterResource(id = R.drawable.traffic), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(80.dp))
            Text(text = stringResource(R.string.transactions_empty), fontSize = 16.sp, fontFamily = MyriadPro, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatAmount(amount: String): String {
    return try { String.format("%.2f", amount.toDouble()).replace(".", ",") } catch (_: Exception) { amount }
}

private fun formatDate(dateTime: String): String {
    return try { LocalDateTime.parse(dateTime).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) } catch (_: Exception) { dateTime.substringBefore("T") }
}

private fun formatTime(dateTime: String): String {
    return try { LocalDateTime.parse(dateTime).format(DateTimeFormatter.ofPattern("HH:mm")) } catch (_: Exception) { dateTime.substringAfter("T").substringBefore(".") }
}

private fun applyFiltersAndSorting(transactions: List<TransactionDto>, sharedPreferences: KsPrefs): List<TransactionDto> {
    if (transactions.isEmpty()) return emptyList()
    val filterType = sharedPreferences.pull(SharedPreferencesKeys.FILTER_TYPE, TransactionSource.POS.ordinal)
    val filterStatus = sharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, TransactionStatusFilterType.ALL.ordinal)
    val filterSort = sharedPreferences.pull(SharedPreferencesKeys.FILTER_SORT, TransactionSortType.DateDesc.ordinal)
    val dateFromStr = sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "")
    val dateToStr = sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    val dateFrom: LocalDateTime? = if (dateFromStr.isNotEmpty()) try { LocalDateTime.parse(dateFromStr, dateTimeFormatter) } catch (_: Exception) { null } else null
    val dateTo: LocalDateTime? = if (dateToStr.isNotEmpty()) try { LocalDateTime.parse(dateToStr, dateTimeFormatter) } catch (_: Exception) { null } else null
    var filteredList = transactions
    if (filterType == TransactionSource.POS.ordinal) filteredList = filteredList.filter { it.source == TransactionSource.POS }
    else if (filterType == TransactionSource.IPS.ordinal) filteredList = filteredList.filter { it.source == TransactionSource.IPS }
    when (filterStatus) {
        TransactionStatusFilterType.ACCEPTED.ordinal -> filteredList = filteredList.filter { it.status == TransactionStatus.Accepted }
        TransactionStatusFilterType.REJECTED.ordinal -> filteredList = filteredList.filter { it.status == TransactionStatus.Rejected }
        TransactionStatusFilterType.VOID.ordinal -> filteredList = filteredList.filter { it.status == TransactionStatus.Voided }
    }
    if (dateFrom != null || dateTo != null) {
        filteredList = filteredList.filter { transaction ->
            transaction.transactionDate?.let {
                (dateFrom == null || !it.isBefore(dateFrom)) && (dateTo == null || !it.isAfter(dateTo))
            } ?: true
        }
    }
    return when (filterSort) {
        TransactionSortType.DateAsc.ordinal -> filteredList.sortedBy { it.transactionDate }
        TransactionSortType.DateDesc.ordinal -> filteredList.sortedByDescending { it.transactionDate }
        TransactionSortType.AmountAsc.ordinal -> filteredList.sortedBy { it.amountDouble }
        TransactionSortType.AmountDesc.ordinal -> filteredList.sortedByDescending { it.amountDouble }
        else -> filteredList
    }
}

private fun hasActiveFilters(sharedPreferences: KsPrefs): Boolean {
    return sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "").isNotEmpty() ||
            sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "").isNotEmpty() ||
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, TransactionStatusFilterType.ALL.ordinal) != TransactionStatusFilterType.ALL.ordinal ||
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_SORT, TransactionSortType.DateDesc.ordinal) != TransactionSortType.DateDesc.ordinal ||
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_TYPE, TransactionSource.POS.ordinal) != TransactionSource.POS.ordinal
}