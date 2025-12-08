package com.payten.whitelabel.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.cioccarellia.ksprefs.KsPrefs
import com.icmp10.cvms.api.*
import com.icmp10.cvms.codes.opCvms.CvmsResult
import com.icmp10.mtms.api.MTMSListener
import com.icmp10.mtms.codes.MTMSStatusCode
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult
import com.icmp10.mtms.codes.opTransact.TransactResult
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.TransactionStatus
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.CardProcessingScreen
import com.payten.whitelabel.ui.screens.VoidProcessingScreen
import com.payten.whitelabel.ui.theme.AppTheme
import com.sacbpp.core.bytes.ByteArray
import com.sacbpp.core.bytes.ByteArrayFactory
import com.simant.MainApplication
import com.simant.softpos.api.CVMTransactionApi
import com.simant.softpos.api.TransactionApi
import com.simcore.api.SoftPOSSDK
import com.simcore.api.interfaces.DisplayInterface
import com.simcore.api.interfaces.LoyaltyActionListener
import com.simcore.api.interfaces.PaymentData
import com.simcore.api.interfaces.TransactionResultListener
import com.simcore.api.objects.UserInterfaceData
import com.simcore.api.providers.CardCommunicationProvider
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * HeadlessVoidActivity.kt
 *
 * This Activity handles the void transaction flow with card tap confirmation:
 * 1. Shows CardProcessingScreen - user taps card to confirm void
 * 2. Reads card via SDK transaction processing
 * 3. Shows VoidProcessingScreen - white screen with red loading indicator
 * 4. Processes void via SDK
 * 5. Returns result to calling screen
 *
 * This follows the same pattern as VoidActivity but uses Compose UI.
 */
@AndroidEntryPoint
class HeadlessVoidActivity : ComponentActivity(), TransactionResultListener, LoyaltyActionListener,
    DisplayInterface, CVMSListener, MTMSListener {

    private val logger = KotlinLogging.logger {}
    private val TAG = "HeadlessVoidActivity"

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private var originalRecordId: String = ""
    private var originalCardNumber: String = ""
    private var originalAmount: Double = 0.0
    private var transactionFirstResponse: TransactResult? = null
    private var shouldIgnoreDecline = true
    private var providedPackageName: String = "" // For app-to-app flow

    private var lbin: ByteArray? = null
    private var lHash: ByteArray? = null

    // UI state
    private val _voidState = mutableStateOf<VoidState>(VoidState.WaitingForCard)
    private val voidState: State<VoidState> = _voidState

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                if (state == NfcAdapter.STATE_OFF) {
                    logger.error { "NFC turned off during void transaction" }
                    returnError("NFC turned off")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reset PaymentUiBridge state
        com.payten.whitelabel.ui.states.PaymentUiBridge.reset()

        // Check if this is app-to-app flow or internal flow
        if (intent.hasExtra("providedPackageName")) {
            // App-to-app flow
            providedPackageName = intent.getStringExtra("providedPackageName") ?: ""
            originalRecordId = intent.getStringExtra("authorizationCode") ?: ""
            val amountString = intent.getStringExtra("Amount") ?: "0"
            originalAmount = amountString.toDoubleOrNull() ?: 0.0
            originalCardNumber = "" // Not available in app-to-app

            logger.info { "HeadlessVoidActivity started via app-to-app from: $providedPackageName, authCode: $originalRecordId, amount: $originalAmount" }
        } else {
            // Internal flow
            originalRecordId = intent.getStringExtra("recordId") ?: ""
            originalCardNumber = intent.getStringExtra("cardNumber") ?: ""
            originalAmount = intent.getDoubleExtra("amount", 0.0)

            logger.info { "HeadlessVoidActivity started internally for recordId: $originalRecordId, amount: $originalAmount" }
        }

        try {
            // Set transaction type to VOID (don't cancel here - it interferes with other transactions)
            MainApplication.getInstance().paymentData.transactionType = PaymentData.TransactionType.VOID.internalType

            // Set the transaction ID (recordId) for void
            MainApplication.getInstance().paymentData.transactionId = originalRecordId

            // Set the amount
            val sdkAmount = (originalAmount * 100).roundToLong()
            MainApplication.getInstance().setPaymentAmount(sdkAmount)

            logger.info { "SDK configured - Type: VOID, RecordId: $originalRecordId, Amount: $sdkAmount" }
        } catch (e: Exception) {
            logger.error(e) { "Error configuring SDK: ${e.message}" }
            returnError("Failed to initialize: ${e.message}")
            return
        }

        // Set up SDK listeners
        MainApplication.getInstance().mtmsListener.setListener(this)
        MainApplication.getInstance().cvmsListener.setListener(this)
        MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = this
        MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = this
        MainApplication.getInstance().configurationInterface.setDisplayInterface(this)

        // Register NFC state receiver
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        registerReceiver(mReceiver, filter)

        setContent {
            AppTheme {
                val state by voidState

                when (state) {
                    is VoidState.WaitingForCard -> {
                        CardProcessingScreen(
                            amountInPare = (originalAmount * 100).toLong(),
                            tipAmount = 0L,
                            onNavigateBack = {
                                logger.info { "Void cancelled by user" }
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        )
                    }
                    is VoidState.Processing -> {
                        VoidProcessingScreen()
                    }
                    is VoidState.Success -> {
                        // Will finish and return result
                    }
                    is VoidState.Error -> {
                        // Will finish and return error
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        logger.info { "onResume - starting void transaction" }

        if (SoftPOSSDK.getCardCommunicationProvider().interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
            if (!SoftPOSSDK.getCardCommunicationProvider().isEnabled) {
                logger.error { "NFC Not enabled" }
                returnError("NFC is not enabled")
            } else {
                MainApplication.getInstance().setRealProviders()
                logger.info { "Starting transaction with data: ${MainApplication.getInstance().paymentData}" }

                try {
                    // Start the void transaction
                    TransactionApi.doTransaction(this, MainApplication.getInstance().paymentData)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to start void transaction: ${e.message}" }
                    returnError("Failed to start transaction: ${e.message}")
                }
            }
        }

        if (MainApplication.getInstance().cardCommunicationProvider != null) {
            if (MainApplication.getInstance().cardCommunicationProvider.interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
                MainApplication.getInstance().cardCommunicationProvider.connectReader(this)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
        } catch (e: Exception) {
            logger.error { "Cannot cancel transaction: $e" }
        }

        try {
            unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            logger.error { "Error unregistering receiver: $e" }
        }

        MainApplication.getInstance().mtmsListener.setListener(null)
        MainApplication.getInstance().cvmsListener.setListener(null)
        MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = null
        MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = null
        MainApplication.getInstance().configurationInterface.setDisplayInterface(null)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up SDK state to prevent it from interfering with future transactions
        try {
            // Reset transaction flags
            SoftPOSSDK.setAutoMode(false)
            SoftPOSSDK.setCancelled(true)
            SoftPOSSDK.resetReaderOutcome()

            // Reset transaction type back to GOODS
            MainApplication.getInstance().paymentData.transactionType = PaymentData.TransactionType.GOODS.internalType

            // Clear transaction ID
            MainApplication.getInstance().paymentData.transactionId = ""

            logger.info { "HeadlessVoidActivity cleaned up SDK state" }
        } catch (e: Exception) {
            logger.error(e) { "Error cleaning up SDK state: ${e.message}" }
        }
    }

    // TransactionResultListener implementations
    override fun onTransactionIdle() {
        logger.info { "Transaction idle" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x01, true)
        }
    }

    override fun onTransactionReadyToRead() {
        logger.info { "Transaction ready to read" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x01, true)
        }
    }

    override fun onTransactionProcessing() {
        logger.info { "Void transaction processing" }
        // Don't switch to Processing screen yet - let LEDs show
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x02, true)
        }
    }

    override fun onTransactionSuccessful() {
        logger.info { "Void transaction successful - waiting for online response" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x0F, false)
        }
        // Don't finish yet - wait for onOnlineResponse
    }

    override fun onTransactionDeclined() {
        logger.info { "Void transaction declined" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x0F, false)
        }

        if (shouldIgnoreDecline) {
            // Retry transaction
            shouldIgnoreDecline = false
            runOnUiThread {
                try {
                    TransactionApi.doTransaction(this, MainApplication.getInstance().paymentData)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to retry transaction: ${e.message}" }
                    returnError("Transaction declined")
                }
            }
        } else {
            returnError("Transaction declined")
        }
    }

    override fun onTransactionEnded(message: String?) {
        logger.info { "Void transaction ended: $message" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x0F, false)

            if (message?.contains("TRY_AGAIN") == true) {
                shouldIgnoreDecline = true
            } else {
                returnError(message ?: "Transaction ended")
            }
        }
    }

    override fun onTransactionCancelled() {
        logger.info { "Void transaction cancelled" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x0F, false)

            // Clean up SDK state BEFORE finishing to prevent race conditions
            cleanupSDKState()

            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onTransactionNotStarted(message: String?) {
        logger.error { "Void transaction not started: $message" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x0F, false)
            returnError(message ?: "Transaction not started")
        }
    }

    override fun onTransactionOnline() {
        logger.info { "Transaction going online" }
        runOnUiThread {
            com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x0F, true)

            // Wait a bit for user to see all LEDs light up, then switch to processing screen
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _voidState.value = VoidState.Processing
            }, 1000) // 1 second delay to show all LEDs
        }
    }

    override fun onOnlineRequest(): ByteArray? {
        logger.info { "onOnlineRequest - returning recordId: $originalRecordId" }

        // Return the original transaction's recordId in the correct format
        // This is critical for voiding to work
        if (MainApplication.getInstance().paymentData.transactionType.equals(
                PaymentData.TransactionType.VOID.internalType, true
            )
        ) {
            if (MainApplication.getInstance().paymentData.transactionId.isNotEmpty()) {
                val refundTagS = String.format(
                    "DF829050%02X%s",
                    MainApplication.getInstance().paymentData.transactionId.length,
                    ByteArrayFactory.getInstance()
                        .getByteArray(MainApplication.getInstance().paymentData.transactionId.toByteArray())
                        .hexString
                )
                val refundTags = ByteArrayFactory.getInstance().fromHexString(refundTagS)
                MainApplication.getInstance().paymentData.transactionId = ""
                return refundTags
            }
        }
        MainApplication.getInstance().paymentData.transactionId = ""
        return null
    }

    override fun onBatchApproval() {
        logger.info { "Batch approval" }
    }

    override fun onBatchDeclined() {
        logger.info { "Batch declined" }
    }

    // MTMSListener implementations
    override fun onOnlineResponse(p0: TransactResult?) {
        logger.info { "onOnlineResponse (TransactResult): ${p0?.mtmsStatusCode}" }
        if (p0?.mtmsStatusCode == MTMSStatusCode.SUCCESS) {
            transactionFirstResponse = p0
        }
    }

    override fun onOnlineResponse(p0: GetTransactionResult?) {
        logger.info { "onOnlineResponse (GetTransactionResult): ${p0?.transactionResponseData}" }

        if (transactionFirstResponse?.mtmsStatusCode == MTMSStatusCode.SUCCESS && transactionFirstResponse != null) {
            runOnUiThread {
                // Void was successful
                _voidState.value = VoidState.Success
            }

            val voidedTransaction = TransactionDetailsDto(
                aid = "",
                applicationLabel = "",
                authorizationCode = "",
                bankName = "",
                cardNumber = originalCardNumber,
                dateTime = org.threeten.bp.LocalDateTime.now().toString(),
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID, ""),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
                message = "Voided",
                operationName = "Void",
                response = "00",
                rrn = p0?.transactionResponseData?.rrn ?: "",
                code = originalRecordId,
                status = "v",
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""),
                amount = originalAmount.toString(),
                isIps = false,
                sdkStatus = TransactionStatus.Voided,
                billStatus = null,
                color = -1,
                recordId = originalRecordId,
                listName = "",
                tipAmount = "0.0"
            )

            returnSuccess(voidedTransaction)
        } else {
            returnError("Void failed - no success response")
        }
    }

    override fun onOnlineResponse(p0: CvmsResult?) {
        logger.info { "onOnlineResponse (CvmsResult): ${p0?.cvmsStatusCode}" }
    }

    // CVMSListener implementations
    override fun onCVMEEntered(p0: Int) {
        logger.info { "PIN entered: $p0" }
        CVMTransactionApi.doTransactionPCPOC(
            this,
            MainApplication.getInstance().paymentData.transactionType
        )
    }

    override fun onCVMETimeout() {
        logger.error { "PIN entry timeout" }
        SoftPOSSDK.setAutoMode(false)
        SoftPOSSDK.setCancelled(true)
        SoftPOSSDK.resetReaderOutcome()
        SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
        returnError("PIN entry timeout")
    }

    override fun onCVMECancelled() {
        logger.info { "PIN entry cancelled" }
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun getDialogConfiguration(): CVMEDlgFragmentConfigurator? {
        // PIN entry not expected for void, but implement if needed
        return null
    }

    // LoyaltyActionListener implementations
    override fun onBINDetected(bin: ByteArray, panHash: ByteArray): Int {
        lbin = bin.clone()
        lHash = panHash.clone()
        return 0x01 // SIMCORE_TRUE
    }

    override fun onLoyaltyOnlineRequest(): ByteArray? {
        return null
    }

    override fun onLoyaltyOnlineResponse(p0: TransactResult?) {
        // Not needed for void
    }

    // DisplayInterface implementations
    override fun displayStop(p0: UserInterfaceData?) {
        // Not needed
    }

    override fun displayMessage(p0: UserInterfaceData?) {
        logger.info { "Display message: ${p0?.uirdStatus}" }

        if (p0?.uirdStatus == UserInterfaceData.UIRDStatus.UIRD_STATUS_CARD_READ_SUCCESSFULLY) {
            logger.info { "Card read successfully" }
            // Don't switch to Processing screen yet - let LEDs show
            runOnUiThread {
                com.payten.whitelabel.ui.states.PaymentUiBridge.updateLedState(0x04, true)
            }
        }
    }

    private fun returnSuccess(transactionData: TransactionDetailsDto) {
        // Clean up SDK state BEFORE finishing to prevent race conditions
        cleanupSDKState()

        // Check if this is app-to-app flow
        if (providedPackageName.isNotEmpty()) {
            // App-to-app flow - return response in app-to-app format
            val gson = com.fatboyindustrial.gsonjavatime.Converters.registerLocalDateTime(
                com.google.gson.GsonBuilder()
            ).create()

            val status = transactionData.response ?: "05"
            val message = transactionData.message ?: "Void neuspesan"
            val dataResponse = gson.toJson(transactionData)

            val obj = com.payten.whitelabel.dto.AppToAppResponseDto(
                com.payten.whitelabel.dto.AppToAppSingleResponseDto(
                    com.payten.whitelabel.dto.AppToAppSingleResponseStatusDto(
                        status,
                        message,
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
            // Internal flow
            val resultIntent = Intent().apply {
                putExtra("transaction_data", transactionData)
                putExtra("success", true)
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun returnError(errorMessage: String) {
        // Clean up SDK state BEFORE finishing to prevent race conditions
        cleanupSDKState()

        val resultIntent = Intent().apply {
            putExtra("error_message", errorMessage)
            putExtra("success", false)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun cleanupSDKState() {
        try {
            // Reset transaction flags
            SoftPOSSDK.setAutoMode(false)
            SoftPOSSDK.setCancelled(true)
            SoftPOSSDK.resetReaderOutcome()

            // Reset transaction type back to GOODS
            MainApplication.getInstance().paymentData.transactionType = PaymentData.TransactionType.GOODS.internalType

            // Clear transaction ID
            MainApplication.getInstance().paymentData.transactionId = ""

            logger.info { "HeadlessVoidActivity cleaned up SDK state" }
        } catch (e: Exception) {
            logger.error(e) { "Error cleaning up SDK state: ${e.message}" }
        }
    }
}

/**
 * Sealed class representing void transaction states
 */
sealed class VoidState {
    object WaitingForCard : VoidState()
    object Processing : VoidState()
    object Success : VoidState()
    data class Error(val message: String) : VoidState()
}
