package com.payten.whitelabel.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.*
import androidx.core.graphics.toColorInt
import com.cioccarellia.ksprefs.KsPrefs
import com.icmp10.cvms.api.*
import com.icmp10.cvms.codes.opCvms.CvmsResult
import com.icmp10.mtms.api.MTMSListener
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult
import com.icmp10.mtms.codes.opTransact.TransactResult
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.ErrorDescription
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.AnimationScreen
import com.payten.whitelabel.ui.screens.CardProcessingScreen
import com.payten.whitelabel.ui.screens.PaymentProcessingScreen
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.utils.RealSoftPosProvider
import com.payten.whitelabel.utils.SoftPosProvider
import com.payten.whitelabel.viewmodel.PosViewModel
import com.sacbpp.core.bytes.ByteArray
import com.simcore.api.interfaces.DisplayInterface
import com.simcore.api.interfaces.LoyaltyActionListener
import com.simcore.api.interfaces.PaymentData
import com.simcore.api.interfaces.TransactionResultListener
import com.simcore.api.objects.UserInterfaceData
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.json.JSONObject
import javax.inject.Inject
import android.app.DialogFragment
import com.payten.whitelabel.ui.states.PaymentUiBridge

/**
 * HeadlessPaymentActivity.kt
 *
 * This Activity handles the payment transaction flow with card tap:
 * 1. Shows CardProcessingScreen - user taps card for payment
 * 2. Reads card via SDK transaction processing
 * 3. Shows PaymentProcessingScreen - white screen with red loading indicator (during PIN entry & online processing)
 * 4. Shows AnimationScreen - Visa/Mastercard animation on success
 * 5. Returns result to calling screen
 *
 * This follows the same pattern as HeadlessVoidActivity but uses Compose UI.
 */
@AndroidEntryPoint
class HeadlessPaymentActivity : ComponentActivity(), TransactionResultListener, LoyaltyActionListener,
    DisplayInterface, CVMSListener, MTMSListener {

    private val logger = KotlinLogging.logger {}
    private val TAG = "HeadlessPaymentActivity"

    @Inject
    lateinit var sharedPreferences: KsPrefs
    lateinit var model: PosViewModel

    private var isFailedTransaction = false
    private var shouldIgnoreDecline = true
    private var tip = ""
    private var paymentAdditionalData = ""
    private var providedPackageName = ""
    private var amountInPare: Long = 0L
    private var tipAmountInPare: Long = 0L

    private var lbin: ByteArray? = null
    private var lHash: ByteArray? = null

    // UI state
    private val _paymentState = mutableStateOf<PaymentState>(PaymentState.WaitingForCard)
    private val paymentState: State<PaymentState> = _paymentState

    // Bridge Pattern: Default to Real implementation, but open for testing injection
    @VisibleForTesting
    var softPosProvider: SoftPosProvider = RealSoftPosProvider()

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                if (state == NfcAdapter.STATE_OFF) {
                    logger.error { "NFC turned off during payment" }
                    returnResult(RESULT_CANCELED, "NFC is disabled")
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger.info { "onCreate HeadlessPaymentActivity" }

        // Reset payment UI state for new payment transaction
        PaymentUiBridge.reset()

        val model2: PosViewModel by viewModels()
        model = model2

        // Check if this is app-to-app flow
        if (intent.hasExtra("providedPackageName")) {
            providedPackageName = intent.getStringExtra("providedPackageName") ?: ""
            logger.info { "HeadlessPaymentActivity started via app-to-app from: $providedPackageName" }
        }

        val amount = intent.getStringExtra("Amount")
        if (amount == null) {
            returnResult(RESULT_CANCELED, "Amount not provided")
            finish()
            return
        }

        if (intent.hasExtra("Tip")) {
            tip = intent.getStringExtra("Tip").toString()
        }

        paymentAdditionalData = createJsonAdditionalData(
            intent.getStringExtra("Tip").toString(),
            intent.getStringExtra("uniqueId").toString()
        )

        // Calculate amounts in pare
        amountInPare = try {
            amount.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            logger.error(e) { "Error parsing amount" }
            0L
        }

        tipAmountInPare = try {
            if (tip.isNotEmpty() && tip != "null") {
                tip.toLong()
            } else {
                0L
            }
        } catch (e: Exception) {
            logger.error(e) { "Error parsing tip" }
            0L
        }

        // Initialize SDK via provider
        val sdkReady = softPosProvider.initializeSdk(amount, tip, paymentAdditionalData)

        if (sdkReady) {
            // Register listeners via provider
            softPosProvider.registerListeners(this)

            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            this.registerReceiver(mReceiver, filter)

            // Check NFC via provider
            if (!softPosProvider.checkNfcEnabled()) {
                logger.error { "NFC Not enabled" }
                returnResult(RESULT_CANCELED, "NFC not enabled")
                finish()
                return
            }
        } else {
            logException("SDK not ready")
            returnResult(RESULT_CANCELED, "SDK not ready")
            finish()
            return
        }

        setContent {
            AppTheme {
                val state by paymentState

                when (state) {
                    is PaymentState.WaitingForCard -> {
                        CardProcessingScreen(
                            amountInPare = amountInPare,
                            tipAmount = tipAmountInPare,
                            onNavigateBack = {
                                logger.info { "Payment cancelled by user" }
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        )
                    }
                    is PaymentState.Processing -> {
                        PaymentProcessingScreen()
                    }
                    is PaymentState.ShowAnimation -> {
                        val animationState = state as PaymentState.ShowAnimation
                        AnimationScreen(
                            cardType = animationState.cardType,
                            onAnimationComplete = {
                                returnResult(RESULT_OK, "Success", animationState.transactionData)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        registerReceiver(mReceiver, filter)

        // Check NFC via provider
        if (softPosProvider.checkNfcEnabled()) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing) {
                    resetTransaction()
                }
            }, 1000)
        } else {
            logger.error { "NFC Not enabled" }
            returnResult(RESULT_CANCELED, "NFC not enabled")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        // Cancel via provider
        softPosProvider.cancelTransaction()

        try {
            unregisterReceiver(mReceiver)
        } catch (_: Exception) {
            // Already unregistered
        }

        //  Re-register listeners (Original logic maintained this)
        softPosProvider.registerListeners(this)
    }

    private fun resetTransaction() {
        runOnUiThread {
            try {
                logger.info { "resetTransaction: calling doTransaction..." }
                isFailedTransaction = false

                // Start transaction via provider
                softPosProvider.startTransaction(this@HeadlessPaymentActivity)

            } catch (e: Exception) {
                logger.error { "CRASH PREVENTED in resetTransaction: ${e.message}" }
                e.printStackTrace()
            }
        }
    }

    private fun returnResult(resultCode: Int, message: String? = null, transactionData: TransactionDetailsDto? = null) {
        // Check if this is app-to-app flow
        if (providedPackageName.isNotEmpty() && transactionData != null) {
            // App-to-app flow - return response in app-to-app format
            val gson = com.fatboyindustrial.gsonjavatime.Converters.registerLocalDateTime(
                com.google.gson.GsonBuilder()
            ).create()

            val status = transactionData.response ?: if (resultCode == RESULT_OK) "00" else "05"
            val responseMessage = transactionData.message ?: message ?: "Transaction processed"
            val dataResponse = gson.toJson(transactionData)

            val obj = com.payten.whitelabel.dto.AppToAppResponseDto(
                com.payten.whitelabel.dto.AppToAppSingleResponseDto(
                    com.payten.whitelabel.dto.AppToAppSingleResponseStatusDto(
                        status,
                        responseMessage,
                        dataResponse
                    ),
                    transactionData.recordId ?: ""
                )
            )

            logger.info { "Sending app-to-app response to $providedPackageName: $obj" }

            val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
            if (intent != null) {
                intent.putExtra("RESPONSE_JSON_STRING", gson.toJson(obj))
                setResult(RESULT_OK, intent)
            } else {
                logger.error { "Package $providedPackageName not found" }
                setResult(RESULT_CANCELED)
            }
        } else {
            // Internal flow - normal result
            val resultIntent = Intent()
            message?.let { resultIntent.putExtra("message", it) }
            transactionData?.let { resultIntent.putExtra("transaction_data", it) }
            setResult(resultCode, resultIntent)
        }
    }

    // TransactionResultListener implementations
    override fun onTransactionProcessing() {
        logger.info { "Transaction onTransactionProcessing" }
        playAudioIndication(true)
        updateLedState(0x02, true)
    }

    override fun onTransactionSuccessful() {
        logger.info { "Transaction onTransactionSuccessful" }
        playAudioIndication(true)
        updateLedState(0x0F, true)
    }

    override fun onTransactionDeclined() {
        logger.info { "Transaction onTransactionDeclined" }
        playAudioIndication(false)
        updateLedState(0x0F, false)
        if (shouldIgnoreDecline) {
            resetTransaction()
            return
        }

        returnResult(RESULT_CANCELED, "Transaction declined")
        finish()
    }

    override fun onTransactionEnded(p0: String?) {
        logger.info { "Transaction onTransactionEnded $p0" }

        if (p0?.contains("TRY_AGAIN") == true) {
            shouldIgnoreDecline = true
        } else {
            returnResult(RESULT_CANCELED, "Transaction ended: $p0")
            finish()
        }
    }

    override fun onTransactionCancelled() {
        logger.info { "Transaction onTransactionCancelled" }
        returnResult(RESULT_CANCELED, "Transaction cancelled")
        finish()
    }

    override fun onTransactionNotStarted(p0: String?) {
        logger.info { "SDK onTransactionNotStarted: $p0" }

        if (p0?.contains("Parameters not Ready", ignoreCase = true) == true) {
            logger.info { "SDK params not ready. Retrying in 2 seconds..." }

            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    resetTransaction()
                }
            }, 2000)
        } else {
            playAudioIndication(false)
            returnResult(RESULT_CANCELED, "Transaction not started: $p0")
            finish()
        }
    }

    override fun onTransactionOnline() {
        logger.info { "Transaction onTransactionOnline" }
        updateLedState(0x0F, true)
    }

    override fun onOnlineRequest(): ByteArray? {
        logger.info { "Transaction onOnlineRequest" }
        return null
    }

    override fun onBatchApproval() {
        logger.info { "Transaction onBatchApproval" }
    }

    override fun onBatchDeclined() {
        logger.info { "Transaction onBatchDeclined" }
    }

    override fun onOnlineResponse(p0: TransactResult?) {
        logger.info { "Transaction onOnlineResponse1 ${p0?.mtmsStatusCode.toString()}" }
        shouldIgnoreDecline = false
    }

    override fun onOnlineResponse(p0: GetTransactionResult?) {
        logger.info { "onOnlineResponse2 response data: ${p0?.transactionResponseData?.responseCode} statusCode: ${p0?.transactionResponseData?.statusCode}" }

        if (p0?.transactionResponseData?.statusCode.equals("p", true)) {
            return
        }

        if (p0?.transactionResponseData?.statusCode.equals("A", true)) {
            // Convert tip from pare (cents) to decimal format (RSD)
            val tipInDecimal = try {
                if (tip.isNotEmpty()) {
                    val tipInPare = tip.toLong()
                    (tipInPare / 100.0).toString()
                } else {
                    "0.0"
                }
            } catch (e: Exception) {
                logger.error { "Error converting tip: ${e.message}" }
                "0.0"
            }

            val transactionData = p0?.transactionResponseData?.let {
                TransactionDetailsDto(
                    aid = p0.transactionResponseData.aid,
                    applicationLabel = p0.transactionResponseData.applicationLabel,
                    authorizationCode = p0.transactionResponseData.authorizationCode,
                    bankName = "",
                    cardNumber = p0.transactionResponseData.maskedPAN,
                    dateTime = p0.transactionResponseData.transactionDate,
                    merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID),
                    merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                    message = p0.transactionResponseData.screenMessage,
                    operationName = "Prodaja",
                    response = p0.transactionResponseData.responseCode,
                    rrn = "",
                    code = p0.transactionResponseData.recordId,
                    status = p0.transactionResponseData.statusCode,
                    terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID),
                    amount = p0.transactionResponseData.amount,
                    isIps = false,
                    sdkStatus = null,
                    billStatus = null,
                    recordId = p0.transactionResponseData!!.recordId,
                    listName = "",
                    color = -1,
                    tipAmount = tipInDecimal
                )
            }

            if (p0?.transactionResponseData?.responseCode.equals("00", true)) {
                updateLedState(0x0F, true)

                // Show animation based on card type
                val cardLabel = p0?.transactionResponseData?.applicationLabel
                if (cardLabel?.contains("visa", true) == true || cardLabel?.contains("card", true) == true) {
                    Log.d(TAG, "Starting animation for card: $cardLabel")
                    PaymentUiBridge.setAnimationStarted()
                    runOnUiThread {
                        _paymentState.value = PaymentState.ShowAnimation(cardLabel, transactionData!!)
                    }
                } else {
                    // No animation for other card types
                    Log.d(TAG, "No animation for card: $cardLabel")
                    returnResult(RESULT_OK, "Success", transactionData)
                    finish()
                }
                return
            } else {
                returnResult(RESULT_CANCELED, "Transaction failed", transactionData)
                finish()
            }
        }

        if (p0?.transactionResponseData?.statusCode.equals("D", true)) {
            returnResult(RESULT_CANCELED, "Transaction declined")
            finish()
        }
    }

    override fun onOnlineResponse(p0: CvmsResult?) {
        logger.info { "Transaction onOnlineResponse3 ${p0?.cvmsStatusCode.toString()}" }
        shouldIgnoreDecline = false
    }

    // CVMSListener implementations
    override fun onCVMEEntered(p0: Int) {
        logger.info { "Pin entered: $p0" }
        shouldIgnoreDecline = false
        runOnUiThread {
            _paymentState.value = PaymentState.Processing
        }

        // BRIDGE PATTERN: Use provider for CVM transaction
        softPosProvider.startPinEntry(this, PaymentData.TransactionType.GOODS.internalType.toInt())
    }

    override fun onCVMETimeout() {
        // BRIDGE PATTERN: Cleanup via provider
        softPosProvider.setAutoMode(false)
        softPosProvider.setCancelled(true)
        softPosProvider.resetReaderOutcome()
        softPosProvider.cancelTransaction()

        returnResult(RESULT_CANCELED, "PIN timeout")
        finish()
    }

    override fun onCVMECancelled() {
        logger.info { "onCVMECancelled" }
        // BRIDGE PATTERN: Cleanup via provider
        softPosProvider.setAutoMode(false)
        softPosProvider.setCancelled(true)
        softPosProvider.resetReaderOutcome()
        softPosProvider.cancelTransaction()

        returnResult(RESULT_CANCELED, "PIN cancelled")
        finish()
    }

    @SuppressLint("DefaultLocale")
    override fun getDialogConfiguration(): CVMEDlgFragmentConfigurator {
        Log.d(TAG, "getDialogConfiguration() called - SDK requesting PIN dialog setup")
        val config = CVMEDlgFragmentConfigurator()

        // Keypad button IDs
        config.key0Id = R.id.keypad_0
        config.key1Id = R.id.keypad_1
        config.key2Id = R.id.keypad_2
        config.key3Id = R.id.keypad_3
        config.key4Id = R.id.keypad_4
        config.key5Id = R.id.keypad_5
        config.key6Id = R.id.keypad_6
        config.key7Id = R.id.keypad_7
        config.key8Id = R.id.keypad_8
        config.key9Id = R.id.keypad_9
        config.key0ContainerId = R.id.keypad_0_container
        config.key1ContainerId = R.id.keypad_1_container
        config.key2ContainerId = R.id.keypad_2_container
        config.key3ContainerId = R.id.keypad_3_container
        config.key4ContainerId = R.id.keypad_4_container
        config.key5ContainerId = R.id.keypad_5_container
        config.key6ContainerId = R.id.keypad_6_container
        config.key7ContainerId = R.id.keypad_7_container
        config.key8ContainerId = R.id.keypad_8_container
        config.key9ContainerId = R.id.keypad_9_container

        // Clear (backspace) and cancel buttons
        config.clearId = R.id.keypad_clear
        config.clearContainerId = R.id.keypad_clear_container
        config.cancelId = R.id.keypad_cancel
        config.cancelContainerId = R.id.keypad_cancel_container

        // OK button (hidden in layout)
        config.okId = R.id.keypad_ok
        config.okContainerId = R.id.keypad_ok_container

        // Text views
        config.wildcardTextViewId = R.id.pinText
        config.infoTextViewId = R.id.infoText
        config.countDownTextViewId = R.id.countDownText

        // Layout configuration
        config.layoutResourceID = R.layout.fragment_pin_entry
        config.dialogTheme = android.R.style.Theme_NoTitleBar_Fullscreen
        config.dialogStyle = DialogFragment.STYLE_NORMAL
        config.activity = this@HeadlessPaymentActivity
        config.autoRandomOrder = false
        config.maxPINLength = 4
        config.infoText = ""
        config.countDownTextFormat = "Preostalo sekundi %s"
        config.countDownTimeInSeconds = 30
        config.isResetTimerOnClear = true
        config.restartTimerOnKeyInSeconds = 5

        val keyConfig = CVMEElementKeyConfig()
        keyConfig.textColor = "#000000".toColorInt()
        keyConfig.backgroundColor = Color.TRANSPARENT
        keyConfig.fontSize = 28
        keyConfig.height = 40
        keyConfig.width = 40
        keyConfig.font = Typeface.DEFAULT_BOLD
        val randomAngle = CVMEElementKeyProperty()
        randomAngle.max = 0
        randomAngle.min = 0
        randomAngle.isActive = false
        keyConfig.randomAngle = randomAngle
        config.keyConfig = keyConfig

        val clearConfig = CVMEElementConfig()
        clearConfig.text = ""
        clearConfig.textColor = "#000000".toColorInt()
        clearConfig.backgroundColor = "#FFFFFF".toColorInt()
        clearConfig.fontSize = 1
        clearConfig.height = 1
        clearConfig.width = 1
        clearConfig.font = Typeface.DEFAULT
        config.clearConfig = clearConfig

        val cancelConfig = CVMEElementConfig()
        cancelConfig.text = ""
        cancelConfig.textColor = "#E53935".toColorInt()
        cancelConfig.backgroundColor = "#FFFFFF".toColorInt()
        cancelConfig.fontSize = 1
        cancelConfig.height = 1
        cancelConfig.width = 1
        cancelConfig.font = Typeface.DEFAULT
        config.cancelConfig = cancelConfig

        val okConfig = CVMEElementConfig()
        okConfig.text = ""
        okConfig.textColor = "#000000".toColorInt()
        okConfig.backgroundColor = "#FFFFFF".toColorInt()
        okConfig.fontSize = 1
        okConfig.height = 1
        okConfig.width = 1
        okConfig.font = Typeface.DEFAULT
        config.okConfig = okConfig

        setupPinIndicatorUpdates()
        return config
    }

    @Suppress("DEPRECATION")
    private fun setupPinIndicatorUpdates() {
        var attemptCount = 0
        val maxAttempts = 5

        @RequiresApi(Build.VERSION_CODES.O)
        fun trySetup() {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    attemptCount++
                    val fragmentManager = fragmentManager
                    val fragments = fragmentManager?.fragments
                    var found = false

                    fragments?.forEach { fragment ->
                        fragment?.view?.let { dialogView ->
                            val pinTextView = dialogView.findViewById<TextView>(R.id.pinText)
                            val indicator1 = dialogView.findViewById<View>(R.id.pin_indicator_1)
                            val indicator2 = dialogView.findViewById<View>(R.id.pin_indicator_2)
                            val indicator3 = dialogView.findViewById<View>(R.id.pin_indicator_3)
                            val indicator4 = dialogView.findViewById<View>(R.id.pin_indicator_4)

                            if (pinTextView != null && indicator1 != null) {
                                found = true
                                pinTextView.addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        val pinLength = s?.length ?: 0
                                        runOnUiThread {
                                            indicator1.setBackgroundResource(if (pinLength >= 1) R.drawable.pin_indicator_filled else R.drawable.pin_indicator_empty)
                                            indicator2?.setBackgroundResource(if (pinLength >= 2) R.drawable.pin_indicator_filled else R.drawable.pin_indicator_empty)
                                            indicator3?.setBackgroundResource(if (pinLength >= 3) R.drawable.pin_indicator_filled else R.drawable.pin_indicator_empty)
                                            indicator4?.setBackgroundResource(if (pinLength >= 4) R.drawable.pin_indicator_filled else R.drawable.pin_indicator_empty)
                                        }
                                    }
                                    override fun afterTextChanged(s: Editable?) {}
                                })
                            }
                        }
                    }

                    if (!found && attemptCount < maxAttempts) {
                        trySetup()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 300L * attemptCount)
        }
        trySetup()
    }

    // LoyaltyActionListener implementations
    override fun onBINDetected(bin: ByteArray, panHash: ByteArray): Int {
        lbin = bin.clone()
        lHash = panHash.clone()
        return 0x01
    }

    override fun onLoyaltyOnlineRequest(): ByteArray? {
        return null
    }

    override fun onLoyaltyOnlineResponse(p0: TransactResult?) {
    }

    // DisplayInterface implementations
    override fun displayStop(p0: UserInterfaceData?) {
    }

    override fun displayMessage(p0: UserInterfaceData?) {
        logger.info { "Display message $p0" }
        if (p0?.uirdStatus == UserInterfaceData.UIRDStatus.UIRD_STATUS_CARD_READ_SUCCESSFULLY) {
            playAudioIndication(true)
            updateLedState(0x04, true)
            updateLedState(0x0F, true)
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        _paymentState.value = PaymentState.Processing
                    }
                }
            }, 1000)
        }
    }

    override fun onTransactionIdle() {
        logger.info { "onTransactionIdle" }
        updateLedState(0x01, true)
    }

    override fun onTransactionReadyToRead() {
        logger.info { "onTransactionReadyToRead" }
        updateLedState(0x01, true)
    }

    private fun updateLedState(ledMask: Int, isSuccess: Boolean) {
        // Update LED state through the PaymentUiBridge for CardProcessingScreen
        runOnUiThread {
            PaymentUiBridge.updateLedState(ledMask, isSuccess)
        }
    }

    private fun playAudioIndication(isSuccessTone: Boolean) {
        try {
            val mToneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
            if (isSuccessTone) {
                mToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 600)
                Handler().postDelayed({ mToneGenerator.release() }, 601)
            } else {
                mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
                Handler().postDelayed({ mToneGenerator.release() }, 501)
            }
        } catch (e: Exception) {
            logger.info { "RING RING: ${e.message}" }
        }
    }

    private fun createJsonAdditionalData(tip: String, uniqueId: String): String {
        val paytenAdditionalData = JSONObject()
        val paytenTrnx = JSONObject()
        try {
            if (uniqueId != "null") {
                logger.info { "uniqueId: $uniqueId" }
                paytenTrnx.put("uniqueId", uniqueId)
            }
            if (tip != "null") {
                logger.info { "Tip: $tip" }
                paytenTrnx.put("tipAmount", createTipData(tip))
            }
        } catch (e: java.lang.Exception) {
            logger.error { e.message }
        }
        paytenAdditionalData.put("paytenTransactionRequest", paytenTrnx)
        logger.info { "AddtionalData: $paytenAdditionalData" }
        return paytenAdditionalData.toString()
    }

    private fun createTipData(tip: String): String {
        val formatedTip = tip.replace(".", "").replace(",", "")
        return formatedTip.padStart(12, '0')
    }

    private fun logException(message: String?) {
        model.logError(
            model.createErrorLog(
                sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                sharedPreferences.pull(SharedPreferencesKeys.USER_ID),
                "SDK Status: $message",
                ErrorDescription.transaction.name,
                this
            )
        )
    }
}

/**
 * Sealed class representing payment transaction states
 */
sealed class PaymentState {
    object WaitingForCard : PaymentState()
    object Processing : PaymentState()
    data class ShowAnimation(val cardType: String, val transactionData: TransactionDetailsDto) : PaymentState()
}
