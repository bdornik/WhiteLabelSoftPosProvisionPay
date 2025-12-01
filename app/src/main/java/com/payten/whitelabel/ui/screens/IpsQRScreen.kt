package com.payten.whitelabel.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.QRDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.AmountDisplayCard
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.utils.IpsUtil
import com.payten.whitelabel.viewmodel.IPSShowQRViewModel
import kotlinx.coroutines.delay
import mu.KotlinLogging
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

private val logger = KotlinLogging.logger {}

@Composable
fun IpsQRScreen(
    amount: String,
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit,
    onTransactionComplete: (isSuccess: Boolean, statusCode: String, message: String, e2eRef: String) -> Unit,
    ipsViewModel: IPSShowQRViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isTransactionInProgress by remember { mutableStateOf(true) }

    // Format amount using AmountUtil
    val formattedAmount = remember(amount) {
        AmountUtil.formatAmount(amount)
    }

    // Generate QR Code
    LaunchedEffect(Unit) {
        val bitmap = generateQRCode(
            amount = amount.toLong(),
            formattedAmount = formattedAmount,
            sharedPreferences = sharedPreferences,
            ipsViewModel = ipsViewModel,
            context = context
        )
        qrBitmap = bitmap
    }

    // Timeout timer (210 seconds = 3.5 minutes)
    LaunchedEffect(Unit) {
        delay(210_000)
        if (isTransactionInProgress) {
            showTimeoutDialog = true
            ipsViewModel.disposeAllChecks()
        }
    }

    // Transaction status observers
    DisposableEffect(Unit) {
        val payObserver = androidx.lifecycle.Observer<com.payten.whitelabel.dto.CheckTransactionResponseDto> { response ->
            isTransactionInProgress = false
            val isSuccess = response.statusCode.equals("00", ignoreCase = true)
            onTransactionComplete(
                isSuccess,
                response.statusCode,
                response.approvalCode,
                response.creditTransferIdentificator
            )
        }

        val failedObserver = androidx.lifecycle.Observer<String> { error ->
            isTransactionInProgress = false
            onTransactionComplete(false, "05", error, "")
        }

        ipsViewModel.payTransaction.observeForever(payObserver)
        ipsViewModel.checkTransactionFailed.observeForever(failedObserver)

        onDispose {
            ipsViewModel.payTransaction.removeObserver(payObserver)
            ipsViewModel.checkTransactionFailed.removeObserver(failedObserver)
            ipsViewModel.disposeAllChecks()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            IpsHeader(
                onNavigateBack = {
                    isTransactionInProgress = false
                    ipsViewModel.disposeAllChecks()
                    onNavigateBack()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            AmountDisplayCard(formattedAmount)

            Spacer(modifier = Modifier.height(32.dp))

            // QR Code label
            Text(
                text = stringResource(R.string.ips_qr_scan_label),
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                qrBitmap?.let { bitmap ->
                    QRCodeBox(bitmap = bitmap)
                } ?: Box(
                    modifier = Modifier
                        .size(256.dp)
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFEB3223),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cancel button
            Button(
                onClick = {
                    isTransactionInProgress = false
                    ipsViewModel.disposeAllChecks()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEB3223)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ips_cancel_button),
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Timeout dialog
    if (showTimeoutDialog) {
        TimeoutDialog(
            onDismiss = {
                showTimeoutDialog = false
                isTransactionInProgress = false
                ipsViewModel.disposeAllChecks()
                onTransactionComplete(false, "05", context.getString(R.string.connection_timeout_message), "")
            }
        )
    }
}

@Composable
private fun IpsHeader(
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Text(
            text = "POS",
            fontSize = 18.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 1.sp
        )

        // Empty spacer for balance
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun QRCodeBox(bitmap: Bitmap) {
    Box(
        modifier = Modifier
            .size(256.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TimeoutDialog(
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
                text = stringResource(R.string.label_transaction_timeout),
                textAlign = TextAlign.Center,
                fontFamily = MyriadPro
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEB3223)
                )
            ) {
                Text(
                    text = stringResource(R.string.button_registration_back),
                    fontFamily = MyriadPro
                )
            }
        }
    )
}

// Helper functions
private fun generateQRCode(
    amount: Long,
    formattedAmount: String,
    sharedPreferences: KsPrefs,
    ipsViewModel: IPSShowQRViewModel,
    context: android.content.Context
): Bitmap {
    // Get and increment counter
    var counter = sharedPreferences.pull(SharedPreferencesKeys.COUNTER, 0)
    if (counter > 999999) {
        counter = 0
    }
    val newCounter = counter + 1
    sharedPreferences.push(SharedPreferencesKeys.COUNTER, newCounter)

    // Get merchant info
    val payerAccNumber = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_ACCOUNT_NUMBER, "")
    val merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "")
    val merchantPlaceName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "")
    val merchantAddress = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_ADDRESS, "")
    val mcc = sharedPreferences.pull(SharedPreferencesKeys.MCC, "")
    val terminalIdentificator = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, "")
    val paymentCode = sharedPreferences.pull(SharedPreferencesKeys.PAYMENT_CODE, "")
    val merchantIdentification = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID, "")

    val formattedDate = IpsUtil.getDate()
    val creditTransferIndicator = IpsUtil.createPaymentIdentificatorReference(
        terminalIdentificator,
        counter,
        formattedDate
    )

    // Format amount for API
    val newAmount = formattedAmount.replace(".", "").replace(",", ".")

    val amountAndCurrency = "${SupercaseConfig.CURRENCY_STRING}$amount"

    val dto = QRDto(
        "PT",
        "01",
        "1",
        payerAccNumber,
        merchantName,
        merchantPlaceName,
        merchantAddress,
        amountAndCurrency,
        mcc,
        creditTransferIndicator,
        paymentCode,
        terminalIdentificator,
        merchantIdentification,
        counter.toString(),
        formattedDate
    )

    val content = ipsViewModel.createQrCodeString(dto)

    logger.info { "QRCODE content: $content" }

    // Start checking transaction status
    ipsViewModel.check(creditTransferIndicator, terminalIdentificator, newAmount, content)

    // Generate QR bitmap
    return getQrCodeBitmap(content, context)
}

@SuppressLint("UseKtx")
private fun getQrCodeBitmap(
    qrCodeContent: String,
    context: android.content.Context
): Bitmap {
    val size = 512
    val hints = hashMapOf<EncodeHintType, Int>().also { it[EncodeHintType.MARGIN] = 1 }
    val bits = QRCodeWriter().encode(qrCodeContent, BarcodeFormat.QR_CODE, size, size, hints)

    return createBitmap(size, size, Bitmap.Config.RGB_565).also {
        for (x in 0 until size) {
            for (y in 0 until size) {
                it[x, y] = if (bits[x, y]) {
                    ContextCompat.getColor(context, R.color.bigLabelBlack)
                } else {
                    ContextCompat.getColor(context, R.color.white)
                }
            }
        }
    }
}

/**
 * Preview-friendly version of IpsQRScreen without ViewModel or real QR generation
 */
@Composable
private fun IpsQRScreenContent(
    formattedAmount: String,
    qrBitmap: Bitmap?,
    onNavigateBack: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            IpsHeader(onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(32.dp))

            // Amount display
            AmountDisplayCard(formattedAmount)

            Spacer(modifier = Modifier.height(32.dp))

            // QR Code label
            Text(
                text = stringResource(R.string.ips_qr_scan_label),
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                qrBitmap?.let { bitmap ->
                    QRCodeBox(bitmap = bitmap)
                } ?: Box(
                    modifier = Modifier
                        .size(256.dp)
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFEB3223),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cancel button
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEB3223)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ips_cancel_button),
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "IPS QR Screen - Loading")
@Composable
fun IpsQRScreenPreviewLoading() {
    com.payten.whitelabel.ui.theme.AppTheme {
        IpsQRScreenContent(
            formattedAmount = "1.234,56",
            qrBitmap = null,
            onNavigateBack = {},
            onCancel = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "IPS QR Screen - With QR")
@Composable
fun IpsQRScreenPreviewWithQR() {
    val context = LocalContext.current
    val qrBitmap = remember {
        getQrCodeBitmap(
            qrCodeContent = "https://example.com/pay?amount=123456",
            context = context
        )
    }

    com.payten.whitelabel.ui.theme.AppTheme {
        IpsQRScreenContent(
            formattedAmount = "1.234,56",
            qrBitmap = qrBitmap,
            onNavigateBack = {},
            onCancel = {}
        )
    }
}