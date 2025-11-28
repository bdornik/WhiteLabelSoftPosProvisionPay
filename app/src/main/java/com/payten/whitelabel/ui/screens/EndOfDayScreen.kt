package com.payten.whitelabel.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.CardTraffic
import com.payten.whitelabel.dto.CardTrafficPrint
import com.payten.whitelabel.dto.EndOfDay
import com.payten.whitelabel.dto.transactions.GetTransactionsRequest
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.utils.printer.PrintUtil
import com.payten.whitelabel.viewmodel.TrafficViewModel
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/**
 * EndOfDayScreen.kt
 *
 * This Composable screen provides the functionality for displaying and managing the End of Day (EOD) process.
 *
 * It fetches transaction data (Card and IPS) from the server for the current EOD period,
 * calculates traffic statistics (sales, voids, tips) for Mastercard, Visa, and IPS, and displays
 * them in a receipt-like format.
 *
 * The screen allows the user to:
 * 1. View the aggregated financial traffic data.
 * 2. Execute the End of Day process (which updates the last EOD date in local storage).
 * 3. Print the EOD slip via Bluetooth.
 * 4. Share the EOD summary text via system sharing options.
 *
 * @param sharedPreferences KsPrefs instance for accessing local configuration.
 * @param onNavigateBack Callback executed when the back button is clicked.
 * @param trafficViewModel ViewModel responsible for fetching transaction data from the server.
 */
@Composable
fun EndOfDayScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit,
    trafficViewModel: TrafficViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var endOfDayDone by remember { mutableStateOf(false) }
    var sharedPrinted by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showEndOfDayDialog by remember { mutableStateOf(false) }
    var showEodNotDoneDialog by remember { mutableStateOf(false) }
    var showEodSuccessDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothConnection?>(null) }

    val masterTraffic = remember { mutableStateOf(CardTrafficPrint()) }
    val visaTraffic = remember { mutableStateOf(CardTrafficPrint()) }
    val flikTraffic = remember { mutableStateOf(CardTrafficPrint()) }
    val totalTraffic = remember { mutableStateOf(CardTrafficPrint()) }
    val endOfDay = remember { mutableStateOf(EndOfDay()) }

    val transactionResults by trafficViewModel.transactionResultsSuccess.observeAsState()
    val ipsTransactionResults by trafficViewModel.ipsTransactionResultsSuccess.observeAsState()

    val tipsEnabled = sharedPreferences.pull(SharedPreferencesKeys.TIPS, false)
    val ipsExists = sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS, false)

    LaunchedEffect(Unit) {
        endOfDay.value = endOfDay.value.copy(
            merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
            TID = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""),
            MID = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID, ""),
            lastEndOfDay = sharedPreferences.pull(SharedPreferencesKeys.END_OF_DAY_DATE, "")
        )

        val getTransactionsData = getTransactionRequest(sharedPreferences)
        trafficViewModel.getTransactionsFromServer(getTransactionsData)
    }

    LaunchedEffect(transactionResults) {
        transactionResults?.let { transactions ->
            val master = CardTraffic("mastercard")
            val visa = CardTraffic("visa")

            transactions.forEach { transaction ->
                transaction.amount = roundOffDecimal(transaction.amount.toDouble()).toString()

                val isVisa = isVisa(transaction.applicationId)
                val isMaster = isMaster(transaction.applicationId)
                val isApproved = transaction.responseCode == "00"

                if (isApproved && transaction.operationName != null) {
                    val isSale = transaction.operationName!!.contains("Sale") &&
                            !transaction.operationName!!.contains("Void") &&
                            (transaction.statusCode.equals("a", true) ||
                                    transaction.statusCode.equals("v", true))

                    val isVoid = transaction.operationName!!.contains("Void") &&
                            transaction.statusCode.equals("v", true)

                    val amount = transaction.amount.toDouble()
                    val tip = transaction.tipAmount.toDouble()

                    when {
                        isVisa && isSale -> {
                            visa.purchase = roundOffDecimal((visa.purchase ?: 0.0) + amount + tip)
                            visa.tipAmount = roundOffDecimal((visa.tipAmount ?: 0.0) + tip)
                            visa.purchaseNumber = (visa.purchaseNumber ?: 0) + 1
                        }
                        isVisa && isVoid -> {
                            visa.cancelPurchase = roundOffDecimal((visa.cancelPurchase ?: 0.0) + amount + tip)
                            visa.tipAmount = roundOffDecimal((visa.tipAmount ?: 0.0) - tip)
                            visa.cancelPurchaseNumber = (visa.cancelPurchaseNumber ?: 0) + 1
                        }
                        isMaster && isSale -> {
                            master.purchase = roundOffDecimal((master.purchase ?: 0.0) + amount + tip)
                            master.tipAmount = roundOffDecimal((master.tipAmount ?: 0.0) + tip)
                            master.purchaseNumber = (master.purchaseNumber ?: 0) + 1
                        }
                        isMaster && isVoid -> {
                            master.cancelPurchase = roundOffDecimal((master.cancelPurchase ?: 0.0) + amount + tip)
                            master.tipAmount = roundOffDecimal((master.tipAmount ?: 0.0) - tip)
                            master.cancelPurchaseNumber = (master.cancelPurchaseNumber ?: 0) + 1
                        }
                    }
                }
            }

            endOfDay.value = endOfDay.value.copy(data = listOf(master, visa))

            if (ipsExists) {
                val getTransactionsData = getTransactionRequest(sharedPreferences)
                trafficViewModel.getIpsTransactions(
                    getTransactionsData.userId,
                    getTransactionsData.dateFrom,
                    getTransactionsData.dateTo,
                    sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, "")
                )
            } else {
                populateTrafficData(
                    endOfDay.value,
                    masterTraffic,
                    visaTraffic,
                    flikTraffic,
                    totalTraffic,
                    context
                )
            }
        }
    }

    LaunchedEffect(ipsTransactionResults) {
        ipsTransactionResults?.let { ipsResponse ->
            val ips = CardTraffic("flik")

            ipsResponse.data.forEach { transaction ->
                if (transaction.statusCode == "00") {
                    ips.purchaseNumber = (ips.purchaseNumber ?: 0) + 1
                    ips.purchase = (ips.purchase ?: 0.0) + transaction.amount.toDouble()
                }
            }

            val updatedList = endOfDay.value.data?.toMutableList() ?: mutableListOf()
            updatedList.add(ips)
            endOfDay.value = endOfDay.value.copy(data = updatedList)

            populateTrafficData(
                endOfDay.value,
                masterTraffic,
                visaTraffic,
                flikTraffic,
                totalTraffic,
                context
            )
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
            EndOfDayHeader(
                onNavigateBack = {
                    if (!sharedPrinted) {
                        showExitDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                onEndOfDayClick = {
                    if (!endOfDayDone) {
                        showEndOfDayDialog = true
                    }
                },
                onPrintClick = {
                    try {
                        Log.d("EndOfDayScreen", "Print button clicked, endOfDayDone=$endOfDayDone, activity=$activity")
                        if (!endOfDayDone) {
                            showEodNotDoneDialog = true
                        } else {
                            sharedPrinted = true
                            if (activity != null) {
                                Log.d("EndOfDayScreen", "Creating print text...")
                                val shareText = createTextForPrint(
                                    endOfDay.value,
                                    masterTraffic.value,
                                    visaTraffic.value,
                                    flikTraffic.value,
                                    totalTraffic.value,
                                    tipsEnabled,
                                    ipsExists,
                                    context
                                )
                                Log.d("EndOfDayScreen", "Print text created, length=${shareText.length}")
                                Log.d("EndOfDayScreen", "Calling PrintUtil.printBluetooth...")
                                PrintUtil.printBluetooth(activity, selectedDevice, shareText)
                                Log.d("EndOfDayScreen", "PrintUtil.printBluetooth called successfully")
                            } else {
                                Log.e("EndOfDayScreen", "Activity is null, cannot print")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EndOfDayScreen", "Error in print click handler", e)
                    }
                },
                onShareClick = {
                    if (!endOfDayDone) {
                        showEodNotDoneDialog = true
                    } else {
                        sharedPrinted = true
                        val shareText = createTextForShare(
                            endOfDay.value,
                            masterTraffic.value,
                            visaTraffic.value,
                            flikTraffic.value,
                            totalTraffic.value,
                            tipsEnabled,
                            ipsExists,
                            context
                        )
                        ShareCompat.IntentBuilder(context)
                            .setText(shareText)
                            .setType("text/plain")
                            .setChooserTitle(context.getString(R.string.eod_share_title))
                            .startChooser()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ReceiptCard(
                    endOfDay = endOfDay.value,
                    masterTraffic = masterTraffic.value,
                    visaTraffic = visaTraffic.value,
                    flikTraffic = flikTraffic.value,
                    tipsEnabled = tipsEnabled,
                    ipsExists = ipsExists
                )
            }
        }
    }

    if (showExitDialog) {
        EndOfDayExitDialog(
            endOfDayDone = endOfDayDone,
            onConfirm = {
                showExitDialog = false
                onNavigateBack()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }

    if (showEndOfDayDialog) {
        EndOfDayConfirmDialog(
            onConfirm = {
                showEndOfDayDialog = false
                endOfDayDone = true
                val newEndOfDay = getCurrentDateTime()
                sharedPreferences.push(SharedPreferencesKeys.END_OF_DAY_DATE, newEndOfDay)
                endOfDay.value = endOfDay.value.copy(lastEndOfDay = newEndOfDay)
                showEodSuccessDialog = true
            },
            onDismiss = {
                showEndOfDayDialog = false
            }
        )
    }

    if (showEodNotDoneDialog) {
        AlertDialog(
            onDismissRequest = { showEodNotDoneDialog = false },
            title = { Text(stringResource(R.string.eod_must_be_done_error)) },
            confirmButton = {
                Button(
                    onClick = { showEodNotDoneDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEB3223)
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showEodSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showEodSuccessDialog = false },
            title = { Text(stringResource(R.string.eod_success_message)) },
            confirmButton = {
                Button(
                    onClick = { showEodSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun EndOfDayHeader(
    onNavigateBack: () -> Unit,
    onEndOfDayClick: () -> Unit,
    onPrintClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onEndOfDayClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.eod),
                    contentDescription = null,
                    tint = Color.Black
                )
            }

            IconButton(
                onClick = onPrintClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.print),
                    contentDescription = null,
                    tint = Color.Black
                )
            }

            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun ReceiptCard(
    endOfDay: EndOfDay,
    masterTraffic: CardTrafficPrint,
    visaTraffic: CardTrafficPrint,
    flikTraffic: CardTrafficPrint,
    tipsEnabled: Boolean,
    ipsExists: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Image(
            painter = painterResource(id = R.drawable.eod_slip),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 900.dp),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.eod_header_title),
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Icon(
                    painter = painterResource(R.drawable.eod),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            EODRow(
                label = stringResource(R.string.eod_merchant_label).uppercase(),
                value = endOfDay.merchantName ?: ""
            )
            EODRow(
                label = stringResource(R.string.eod_tid_label),
                value = endOfDay.TID ?: ""
            )
            EODRow(
                label = stringResource(R.string.eod_mid_label),
                value = endOfDay.MID ?: ""
            )
            EODRow(
                label = stringResource(R.string.eod_date_label).uppercase(),
                value = if (endOfDay.lastEndOfDay.isNullOrEmpty()) "" else
                    "${convertDateFormatForPrint(endOfDay.lastEndOfDay!!)} ${convertTimeFormatForPrint(endOfDay.lastEndOfDay!!)}"
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            // Mastercard section
            CardSection(
                title = stringResource(R.string.eod_mastercard_title),
                logoResId = R.drawable.mastercard,
                traffic = masterTraffic,
                showTips = tipsEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            // Visa section
            CardSection(
                title = stringResource(R.string.eod_visa_title),
                logoResId = R.drawable.visa,
                traffic = visaTraffic,
                showTips = tipsEnabled
            )

            // IPS section
            if (ipsExists) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                CardSection(
                    title = stringResource(R.string.eod_ips_title).uppercase(),
                    logoResId = R.drawable.ips,
                    traffic = flikTraffic,
                    showTips = false,
                    isIPS = true
                )
            }
        }
    }
}

@Composable
private fun CardSection(
    title: String,
    logoResId: Int,
    traffic: CardTrafficPrint,
    showTips: Boolean,
    isIPS: Boolean = false
) {
    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Image(
                painter = painterResource(logoResId),
                contentDescription = title,
                modifier = Modifier.height(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isIPS) {
            TrafficRow(
                label = stringResource(R.string.eod_approved_label),
                count = traffic.purchaseNumber?.takeIf { it.isNotEmpty() } ?: "0",
                value = traffic.purchase?.takeIf { it.isNotEmpty() } ?: "0,00 RSD"
            )

            TrafficRow(
                label = stringResource(R.string.eod_voided_label),
                count = traffic.cancelPurchaseNumber?.takeIf { it.isNotEmpty() } ?: "0",
                value = traffic.cancelPurchase?.takeIf { it.isNotEmpty() } ?: "0,00 RSD"
            )

            if (showTips) {
                TrafficRow(
                    label = stringResource(R.string.eod_tip_label),
                    count = "0",
                    value = traffic.tipAmount?.takeIf { it.isNotEmpty() } ?: "0,00 RSD",
                    hideCount = true
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.eod_total_label),
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = traffic.totalNumber?.takeIf { it.isNotEmpty() } ?: "0",
                    fontSize = 14.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )

                Text(
                    text = traffic.total?.takeIf { it.isNotEmpty() } ?: "0,00 RSD",
                    fontSize = 14.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun EODRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = MyriadPro,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TrafficRow(
    label: String,
    count: String,
    value: String,
    hideCount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = MyriadPro,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hideCount) {
                Text(
                    text = count,
                    fontSize = 12.sp,
                    fontFamily = MyriadPro,
                    color = Color.Black,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }

            Text(
                text = value,
                fontSize = 12.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.width(100.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun EndOfDayExitDialog(
    endOfDayDone: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.icon_warning),
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = if (endOfDayDone) {
                    stringResource(R.string.eod_exit_dialog_title_done)
                } else {
                    stringResource(R.string.eod_exit_dialog_title_not_done)
                },
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.eod_exit_dialog_message),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEB3223)
                )
            ) {
                Text(stringResource(R.string.eod_button_yes))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.eod_button_no))
            }
        }
    )
}

@Composable
private fun EndOfDayConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.icon_warning),
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.eod_confirm_dialog_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.eod_confirm_dialog_message),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEB3223)
                )
            ) {
                Text(stringResource(R.string.eod_button_yes))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.eod_button_no))
            }
        }
    )
}

// Helper functions
private fun populateTrafficData(
    endOfDay: EndOfDay,
    masterTraffic: MutableState<CardTrafficPrint>,
    visaTraffic: MutableState<CardTrafficPrint>,
    flikTraffic: MutableState<CardTrafficPrint>,
    totalTraffic: MutableState<CardTrafficPrint>,
    context: Context
) {
    var totalTip = 0.0
    var totalPurchase = 0.0
    var totalPurchaseNumber = 0
    var totalCancelPurchase = 0.0
    var totalCancelPurchaseNumber = 0
    var totalNumber = 0
    var totalTotal = 0.0

    endOfDay.data?.forEach { traffic ->
        when (traffic.type) {
            "mastercard" -> {
                val totalMasterNumber = (traffic.purchaseNumber ?: 0) +
                        (traffic.cancelPurchaseNumber ?: 0) +
                        (traffic.returnNumber ?: 0) +
                        (traffic.cancelReturnNumber ?: 0)

                val totalMaster = roundOffDecimal(
                    (traffic.purchase ?: 0.0) -
                            (traffic.cancelPurchase ?: 0.0) -
                            (traffic.returnPurchase ?: 0.0) +
                            (traffic.cancelReturnPurchase ?: 0.0)
                )

                masterTraffic.value = CardTrafficPrint(
                    type = context.getString(R.string.eod_mastercard_title),
                    purchaseNumber = traffic.purchaseNumber.toString(),
                    purchase = prettyDecimal(traffic.purchase ?: 0.0),
                    cancelPurchaseNumber = traffic.cancelPurchaseNumber.toString(),
                    cancelPurchase = prettyDecimal(traffic.cancelPurchase ?: 0.0),
                    tipAmount = AmountUtil.stringPretty(prettyDecimal(traffic.tipAmount ?: 0.0)),
                    totalNumber = totalMasterNumber.toString(),
                    total = prettyDecimal(totalMaster)
                )

                totalTip = roundOffDecimal(totalTip + (traffic.tipAmount ?: 0.0))
                totalPurchase = roundOffDecimal(totalPurchase + (traffic.purchase ?: 0.0))
                totalPurchaseNumber += traffic.purchaseNumber ?: 0
                totalCancelPurchase += traffic.cancelPurchase ?: 0.0
                totalCancelPurchaseNumber += traffic.cancelPurchaseNumber ?: 0
                totalNumber += totalMasterNumber
                totalTotal = roundOffDecimal(totalTotal + totalMaster)
            }

            "visa" -> {
                val totalVisaNumber = (traffic.purchaseNumber ?: 0) +
                        (traffic.cancelPurchaseNumber ?: 0) +
                        (traffic.returnNumber ?: 0) +
                        (traffic.cancelReturnNumber ?: 0)

                val totalVisa = roundOffDecimal(
                    (traffic.purchase ?: 0.0) -
                            (traffic.cancelPurchase ?: 0.0) -
                            (traffic.returnPurchase ?: 0.0) +
                            (traffic.cancelReturnPurchase ?: 0.0)
                )

                visaTraffic.value = CardTrafficPrint(
                    type = context.getString(R.string.eod_visa_title),
                    purchaseNumber = traffic.purchaseNumber.toString(),
                    purchase = prettyDecimal(traffic.purchase ?: 0.0),
                    cancelPurchaseNumber = traffic.cancelPurchaseNumber.toString(),
                    cancelPurchase = prettyDecimal(traffic.cancelPurchase ?: 0.0),
                    tipAmount = AmountUtil.stringPretty(prettyDecimal(traffic.tipAmount ?: 0.0)),
                    totalNumber = totalVisaNumber.toString(),
                    total = prettyDecimal(totalVisa)
                )

                totalTip = roundOffDecimal(totalTip + (traffic.tipAmount ?: 0.0))
                totalPurchase = roundOffDecimal(totalPurchase + (traffic.purchase ?: 0.0))
                totalPurchaseNumber += traffic.purchaseNumber ?: 0
                totalCancelPurchase += traffic.cancelPurchase ?: 0.0
                totalCancelPurchaseNumber += traffic.cancelPurchaseNumber ?: 0
                totalNumber += totalVisaNumber
                totalTotal = roundOffDecimal(totalTotal + totalVisa)
            }

            "flik" -> {
                val totalFlikNumber = (traffic.purchaseNumber ?: 0) +
                        (traffic.cancelPurchaseNumber ?: 0) +
                        (traffic.returnNumber ?: 0) +
                        (traffic.cancelReturnNumber ?: 0)

                val totalFlik = roundOffDecimal(
                    (traffic.purchase ?: 0.0) -
                            (traffic.cancelPurchase ?: 0.0) -
                            (traffic.returnPurchase ?: 0.0) +
                            (traffic.cancelReturnPurchase ?: 0.0)
                )

                flikTraffic.value = CardTrafficPrint(
                    type = context.getString(R.string.eod_ips_title),
                    totalNumber = totalFlikNumber.toString(),
                    total = prettyDecimal(totalFlik)
                )

                totalPurchase = roundOffDecimal(totalPurchase + (traffic.purchase ?: 0.0))
                totalPurchaseNumber += traffic.purchaseNumber ?: 0
                totalNumber += totalFlikNumber
                totalTotal = roundOffDecimal(totalTotal + totalFlik)
            }
        }
    }

    totalTraffic.value = CardTrafficPrint(
        type = context.getString(R.string.eod_grand_total_label),
        purchaseNumber = totalPurchaseNumber.toString(),
        purchase = AmountUtil.stringPretty(prettyDecimal(totalPurchase)),
        cancelPurchaseNumber = totalCancelPurchaseNumber.toString(),
        cancelPurchase = AmountUtil.stringPretty(prettyDecimal(totalCancelPurchase)),
        tipAmount = AmountUtil.stringPretty(prettyDecimal(totalTip)),
        totalNumber = totalNumber.toString(),
        total = AmountUtil.stringPretty(prettyDecimal(totalTotal))
    )
}

private fun getTransactionRequest(sharedPreferences: KsPrefs): GetTransactionsRequest {
    val tF = DateTimeFormatter.ofPattern("HH:mm:ss")
    val dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val localDateFrom = LocalDate.now().atStartOfDay().minus(90, ChronoUnit.DAYS)
    val localDateTo = LocalDate.now().atStartOfDay().plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS)

    var dateFrom = sharedPreferences.pull(SharedPreferencesKeys.END_OF_DAY_DATE, "")
    if (dateFrom.isEmpty()) {
        dateFrom = "${localDateFrom.format(dF)}T${localDateFrom.format(tF)}"
    }

    return GetTransactionsRequest(
        sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""),
        dateFrom,
        "${localDateTo.format(dF)}T${localDateTo.format(tF)}",
        sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, "")
    )
}

private fun isVisa(applicationID: String): Boolean {
    return applicationID == "A0000000031010" || applicationID == "A0000000032010"
}

private fun isMaster(applicationID: String): Boolean {
    return applicationID == "A0000000041010" || applicationID == "A0000000043060"
}

private fun roundOffDecimal(number: Double): Double {
    val symbols = DecimalFormatSymbols(Locale.ENGLISH)
    val df = DecimalFormat("#.##", symbols)
    df.roundingMode = RoundingMode.HALF_UP
    return df.format(number).toDouble()
}

private fun prettyDecimal(decimal: Double): String {
    val symbols = DecimalFormatSymbols(Locale.ENGLISH)
    val df = DecimalFormat("###,###,###.00", symbols)
    df.roundingMode = RoundingMode.DOWN
    val roundoff = df.format(decimal)

    var amount = roundoff.replace(".", "").replace(",", "")

    if (decimal == 0.0) {
        return "0,00 ${SupercaseConfig.CURRENCY_STRING}"
    }

    val preDotsString = amount.dropLast(2)
    val postDotsString = amount.substring(amount.length - 2)

    amount = when {
        decimal < 1 && decimal > 0 -> "0,$postDotsString"
        decimal < 0 && decimal > -1 -> "-0,$postDotsString"
        else -> {
            "${preDotsString.reversed().chunked(3).joinToString(".").reversed()},$postDotsString"
        }
    }

    return "$amount ${SupercaseConfig.CURRENCY_STRING}"
}

private fun getCurrentDateTime(): String {
    val localDateFrom = LocalDateTime.now()
    val tF = DateTimeFormatter.ofPattern("HH:mm:ss")
    val dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return "${localDateFrom.format(dF)}T${localDateFrom.format(tF)}"
}

private fun convertDateFormatForPrint(inputDate: String): String {
    return try {
        if (inputDate.isEmpty()) return ""
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        outputFormat.format(date!!)
    } catch (_: Exception) {
        inputDate
    }
}

private fun convertTimeFormatForPrint(inputDate: String): String {
    return try {
        if (inputDate.isEmpty()) return ""
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        outputFormat.format(date!!)
    } catch (_: Exception) {
        inputDate
    }
}

private fun trimForPrint(text: String): String {
    var returnText = text
    if (returnText.length > 12) {
        returnText = returnText.take(12) + ".:"
    } else {
        val addSpace = 12 - returnText.length
        for (i in 0 until addSpace) {
            returnText = "$returnText "
        }
    }
    return returnText
}

private fun createTextForShare(
    endOfDay: EndOfDay,
    masterTraffic: CardTrafficPrint,
    visaTraffic: CardTrafficPrint,
    flikTraffic: CardTrafficPrint,
    totalTraffic: CardTrafficPrint,
    tipsEnabled: Boolean,
    ipsExists: Boolean,
    context: Context
): String {
    return buildString {
        appendLine("==============================")
        appendLine()
        appendLine(context.getString(R.string.eod_header_title).uppercase())
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_merchant_label))}      ${endOfDay.merchantName ?: ""}")
        appendLine("${trimForPrint(context.getString(R.string.eod_tid_label))}        ${endOfDay.TID ?: ""}")
        appendLine("${trimForPrint(context.getString(R.string.eod_mid_label))}        ${endOfDay.MID ?: ""}")
        appendLine("${trimForPrint(context.getString(R.string.eod_date_label))}       ${convertDateFormatForPrint(endOfDay.lastEndOfDay ?: "")}")
        appendLine("${trimForPrint(context.getString(R.string.eod_time_label))}       ${convertTimeFormatForPrint(endOfDay.lastEndOfDay ?: "")}")
        appendLine()
        appendLine("==============================")
        appendLine(context.getString(R.string.eod_mastercard_title).uppercase())
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_approved_label))}   ${masterTraffic.purchaseNumber}     ${masterTraffic.purchase}")
        appendLine("${trimForPrint(context.getString(R.string.eod_voided_label))}    ${masterTraffic.cancelPurchaseNumber}   ${masterTraffic.cancelPurchase}")
        if (tipsEnabled) {
            appendLine("${trimForPrint(context.getString(R.string.eod_tip_label))}       ${masterTraffic.tipAmount}")
        }
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_total_label))}  ${masterTraffic.totalNumber}    ${masterTraffic.total}")
        appendLine("==============================")
        appendLine(context.getString(R.string.eod_visa_title).uppercase())
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_approved_label))}   ${visaTraffic.purchaseNumber}   ${visaTraffic.purchase}")
        appendLine("${trimForPrint(context.getString(R.string.eod_voided_label))}    ${visaTraffic.cancelPurchaseNumber}    ${visaTraffic.cancelPurchase}")
        if (tipsEnabled) {
            appendLine("${trimForPrint(context.getString(R.string.eod_tip_label))}        ${visaTraffic.tipAmount}")
        }
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_total_label))}  ${visaTraffic.totalNumber}  ${visaTraffic.total}")
        if (ipsExists) {
            appendLine("==============================")
            appendLine(context.getString(R.string.eod_ips_title).uppercase())
            appendLine()
            appendLine("${trimForPrint(context.getString(R.string.eod_total_label))}  ${flikTraffic.totalNumber}  ${flikTraffic.total}")
        }
        appendLine("==============================")
        appendLine(context.getString(R.string.eod_grand_total_label))
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_approved_label))}   ${totalTraffic.purchaseNumber}  ${totalTraffic.purchase}")
        appendLine("${trimForPrint(context.getString(R.string.eod_voided_label))}    ${totalTraffic.cancelPurchaseNumber}   ${totalTraffic.cancelPurchase}")
        if (tipsEnabled) {
            appendLine("${trimForPrint(context.getString(R.string.eod_tip_label))}        ${totalTraffic.tipAmount}")
        }
        appendLine()
        appendLine("${trimForPrint(context.getString(R.string.eod_total_label))}  ${totalTraffic.totalNumber}     ${totalTraffic.total}")
        appendLine()
        appendLine("==============================")
        appendLine()
        appendLine(SupercaseConfig.INSTITUTION)
    }
}

private fun createTextForPrint(
    endOfDay: EndOfDay,
    masterTraffic: CardTrafficPrint,
    visaTraffic: CardTrafficPrint,
    flikTraffic: CardTrafficPrint,
    totalTraffic: CardTrafficPrint,
    tipsEnabled: Boolean,
    ipsExists: Boolean,
    context: Context
): String {
    val builder = StringBuilder()

    builder.appendLine("[L]")
    builder.appendLine("[C]================================")
    builder.appendLine("[L]")
    builder.appendLine("[C]<u><font size='wide'>${context.getString(R.string.eod_header_title).uppercase()}</font></u>")
    builder.appendLine("[L]")
    builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_merchant_label))}[R]${endOfDay.merchantName ?: ""}")
    builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_tid_label))}[R]${endOfDay.TID ?: ""}")
    builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_mid_label))}[R]${endOfDay.MID ?: ""}")
    builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_date_label))}[R]${convertDateFormatForPrint(endOfDay.lastEndOfDay ?: "")}")
    builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_time_label))}[R]${convertTimeFormatForPrint(endOfDay.lastEndOfDay ?: "")}")
    builder.appendLine()
    builder.appendLine("[C]================================")

    fun appendTrafficSection(title: String, traffic: CardTrafficPrint) {
        builder.appendLine("[C]$title")
        builder.appendLine("[L]")

        if (!title.equals("IPS", ignoreCase = true)) {
            builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_approved_label))} ${traffic.purchaseNumber}[R]${traffic.purchase}")
            builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_voided_label))} ${traffic.cancelPurchaseNumber}[R]${traffic.cancelPurchase}")
            if (tipsEnabled) {
                builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_tip_label))} [R]${traffic.tipAmount}")
            }
        }

        builder.appendLine("[L]")
        builder.appendLine("[L]${trimForPrint(context.getString(R.string.eod_total_label))} ${traffic.totalNumber}[R]${traffic.total}")
        builder.appendLine("[C]================================")
    }

    appendTrafficSection(context.getString(R.string.eod_mastercard_title).uppercase(), masterTraffic)
    appendTrafficSection(context.getString(R.string.eod_visa_title).uppercase(), visaTraffic)
    if (ipsExists) {
        appendTrafficSection(context.getString(R.string.eod_ips_title).uppercase(), flikTraffic)
    }
    appendTrafficSection(context.getString(R.string.eod_grand_total_label), totalTraffic)

    builder.appendLine("[L]")
    builder.appendLine("[C]${SupercaseConfig.INSTITUTION}")

    return builder.toString()
}

// Preview support
@androidx.compose.ui.tooling.preview.Preview(
    name = "End of Day Screen - Full",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun EndOfDayScreenPreview() {
    val sampleEndOfDay = EndOfDay(
        merchantName = "Test Merchant",
        TID = "12345678",
        MID = "87654321",
        lastEndOfDay = "2025-01-27T14:30:00",
        data = listOf()
    )

    val sampleMasterTraffic = CardTrafficPrint(
        type = "MASTERCARD",
        purchaseNumber = "15",
        purchase = "12.500,50 RSD",
        cancelPurchaseNumber = "2",
        cancelPurchase = "1.200,00 RSD",
        tipAmount = "500,00 RSD",
        totalNumber = "17",
        total = "11.300,50 RSD"
    )

    val sampleVisaTraffic = CardTrafficPrint(
        type = "VISA",
        purchaseNumber = "23",
        purchase = "18.750,75 RSD",
        cancelPurchaseNumber = "3",
        cancelPurchase = "2.100,00 RSD",
        tipAmount = "750,00 RSD",
        totalNumber = "26",
        total = "16.650,75 RSD"
    )

    val sampleFlikTraffic = CardTrafficPrint(
        type = "IPS",
        purchaseNumber = "0",
        purchase = "0,00 RSD",
        cancelPurchaseNumber = "0",
        cancelPurchase = "0,00 RSD",
        tipAmount = "0,00 RSD",
        totalNumber = "8",
        total = "5.400,00 RSD"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with buttons
            EndOfDayHeader(
                onNavigateBack = {},
                onEndOfDayClick = {},
                onPrintClick = {},
                onShareClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Receipt card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ReceiptCard(
                    endOfDay = sampleEndOfDay,
                    masterTraffic = sampleMasterTraffic,
                    visaTraffic = sampleVisaTraffic,
                    flikTraffic = sampleFlikTraffic,
                    tipsEnabled = true,
                    ipsExists = true
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Receipt Card Only",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun ReceiptCardPreview() {
    val sampleEndOfDay = EndOfDay(
        merchantName = "Test Merchant",
        TID = "12345678",
        MID = "87654321",
        lastEndOfDay = "2025-01-27T14:30:00",
        data = listOf()
    )

    val sampleMasterTraffic = CardTrafficPrint(
        type = "MASTERCARD",
        purchaseNumber = "15",
        purchase = "12.500,50 RSD",
        cancelPurchaseNumber = "2",
        cancelPurchase = "1.200,00 RSD",
        tipAmount = "500,00 RSD",
        totalNumber = "17",
        total = "11.300,50 RSD"
    )

    val sampleVisaTraffic = CardTrafficPrint(
        type = "VISA",
        purchaseNumber = "23",
        purchase = "18.750,75 RSD",
        cancelPurchaseNumber = "3",
        cancelPurchase = "2.100,00 RSD",
        tipAmount = "750,00 RSD",
        totalNumber = "26",
        total = "16.650,75 RSD"
    )

    val sampleFlikTraffic = CardTrafficPrint(
        type = "IPS",
        purchaseNumber = "0",
        purchase = "0,00 RSD",
        cancelPurchaseNumber = "0",
        cancelPurchase = "0,00 RSD",
        tipAmount = "0,00 RSD",
        totalNumber = "8",
        total = "5.400,00 RSD"
    )

    ReceiptCard(
        endOfDay = sampleEndOfDay,
        masterTraffic = sampleMasterTraffic,
        visaTraffic = sampleVisaTraffic,
        flikTraffic = sampleFlikTraffic,
        tipsEnabled = true,
        ipsExists = true
    )
}