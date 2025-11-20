package com.payten.whitelabel.activities

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import com.cioccarellia.ksprefs.KsPrefs
import com.icmp10.cvms.api.*
import com.icmp10.cvms.codes.opCvms.CvmsResult
import com.icmp10.mtms.api.MTMSListener
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult
import com.icmp10.mtms.codes.opTransact.TransactResult
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.ErrorDescription
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.viewmodel.PosViewModel
import com.sacbpp.core.bytes.ByteArray
import com.simant.MainApplication
import com.simant.sample.SimantApplication
import com.simant.softpos.api.CVMTransactionApi
import com.simant.softpos.api.TransactionApi
import com.simant.utils.CurrencyTable
import com.simcore.api.SoftPOSSDK
import com.simcore.api.interfaces.DisplayInterface
import com.simcore.api.interfaces.LoyaltyActionListener
import com.simcore.api.interfaces.PaymentData
import com.simcore.api.interfaces.TransactionResultListener
import com.simcore.api.objects.UserInterfaceData
import com.simcore.api.providers.CardCommunicationProvider
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule
import com.payten.whitelabel.R
import android.app.PendingIntent
import androidx.appcompat.app.AppCompatActivity
import kotlin.getValue
import androidx.core.graphics.toColorInt

@AndroidEntryPoint
class HeadlessPaymentActivity : AppCompatActivity(), TransactionResultListener, LoyaltyActionListener,
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

    private var lbin: ByteArray? = null
    private var lHash: ByteArray? = null

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                when (state) {
                    NfcAdapter.STATE_OFF -> {
                        returnResult(RESULT_CANCELED, "NFC is disabled")
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger.info { "onCreate HeadlessPaymentActivity" }

        // Make window invisible
        setTheme(android.R.style.Theme_Translucent_NoTitleBar)

        val model2: PosViewModel by viewModels()
        model = model2

        try {
            SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
            MainApplication.getInstance().paymentData.transactionType = PaymentData.TransactionType.GOODS.internalType
        } catch (e: Exception) {
            logException(e.message)
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

        val sdkStatus = SimantApplication.getSDKStatus()
        logger.info { "SDK Status: $sdkStatus" }

        if (sdkStatus == 0) {
            MainApplication.getInstance().mtmsListener.setListener(this)
            MainApplication.getInstance().cvmsListener.setListener(this)
            MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = this
            MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = this
            MainApplication.getInstance().configurationInterface.setDisplayInterface(this)

            val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            this.registerReceiver(mReceiver, filter)

            if (SoftPOSSDK.getCardCommunicationProvider().interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
                if (!SoftPOSSDK.getCardCommunicationProvider().isEnabled) {
                    logger.error { "NFC Not enabled" }
                    returnResult(RESULT_CANCELED, "NFC not enabled")
                    finish()
                    return
                } else {
                    MainApplication.getInstance().setRealProviders()
                }
            }

            if (MainApplication.getInstance().cardCommunicationProvider != null) {
                if (MainApplication.getInstance().cardCommunicationProvider.interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
                    MainApplication.getInstance().cardCommunicationProvider.connectReader(this)
                }
                if (MainApplication.getInstance().cardCommunicationProvider.interfaceType == CardCommunicationProvider.InterfaceType.STATIC) {
                    MainApplication.getInstance().cardCommunicationProvider.connectReader(this)
                }
            }

            MainApplication.getInstance().setPaymentAmount(AmountUtil.setAmount(amount))

            if (!paymentAdditionalData.isEmpty()) {
                MainApplication.getInstance().paymentData.merchantAdditionalData = paymentAdditionalData
            } else {
                MainApplication.getInstance().paymentData.merchantAdditionalData = "None"
            }
        } else {
            logException("SDK not ready")
            returnResult(RESULT_CANCELED, "SDK not ready")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val discovery = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val tagFilters = arrayOf(discovery)
        val i = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pi: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT)
        }

        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, pi, tagFilters, null)

        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        this.registerReceiver(mReceiver, filter)

        if (SoftPOSSDK.getCardCommunicationProvider().interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
            if (!SoftPOSSDK.getCardCommunicationProvider().isEnabled) {
                logger.error { "NFC Not enabled" }
                returnResult(RESULT_CANCELED, "NFC not enabled")
                finish()
            } else {
                MainApplication.getInstance().setRealProviders()
                TransactionApi.doTransaction(this@HeadlessPaymentActivity, MainApplication.getInstance().paymentData)
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

        unregisterReceiver(mReceiver)
        MainApplication.getInstance().mtmsListener.setListener(null)
        MainApplication.getInstance().cvmsListener.setListener(null)
        MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = null
        MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = null
        MainApplication.getInstance().configurationInterface.setDisplayInterface(null)
        NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
    }

    private fun resetTransaction() {
        try {
            isFailedTransaction = false
            TransactionApi.doTransaction(this@HeadlessPaymentActivity, MainApplication.getInstance().paymentData)
        } catch (e: Exception) {
            logger.error { "Starting transaction failed: $e" }
            logException(e.message)
        }
    }

    private fun returnResult(resultCode: Int, message: String? = null, transactionData: TransactionDetailsDto? = null) {
        val resultIntent = Intent()
        message?.let { resultIntent.putExtra("message", it) }
        transactionData?.let { resultIntent.putExtra("transaction_data", it) }
        setResult(resultCode, resultIntent)
    }

    // TransactionResultListener implementations
    override fun onTransactionProcessing() {
        logger.info { "Transaction onTransactionProcessing" }
        playAudioIndication(true)
    }

    override fun onTransactionSuccessful() {
        logger.info { "Transaction onTransactionSuccessful" }
        playAudioIndication(true)
    }

    override fun onTransactionDeclined() {
        logger.info { "Transaction onTransactionDeclined" }
        playAudioIndication(false)

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
        logger.info { "Transaction SDK onTransactionNotStarted $p0" }
        playAudioIndication(false)

        if (p0.equals("Parameters not Ready Yet", true)) {
            Timer().schedule(2000) {
                resetTransaction()
            }
        } else {
            returnResult(RESULT_CANCELED, "Transaction not started: $p0")
            finish()
        }
    }

    override fun onTransactionOnline() {
        logger.info { "Transaction onTransactionOnline" }
        val mToneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
        mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
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
                    tipAmount = tip
                )
            }

            if (p0?.transactionResponseData?.responseCode.equals("00", true)) {
                returnResult(RESULT_OK, "Success", transactionData)
            } else {
                returnResult(RESULT_CANCELED, "Transaction failed", transactionData)
            }
            finish()
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
        CVMTransactionApi.doTransactionPCPOC(this, MainApplication.getInstance().paymentData.transactionType)
    }

    override fun onCVMETimeout() {
        SoftPOSSDK.setAutoMode(false)
        SoftPOSSDK.setCancelled(true)
        SoftPOSSDK.resetReaderOutcome()
        SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()

        returnResult(RESULT_CANCELED, "PIN timeout")
        finish()
    }

    override fun onCVMECancelled() {
        logger.info { "onCVMECancelled" }
        SoftPOSSDK.setAutoMode(false)
        SoftPOSSDK.setCancelled(true)
        SoftPOSSDK.resetReaderOutcome()
        SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()

        returnResult(RESULT_CANCELED, "PIN cancelled")
        finish()
    }

    @SuppressLint("DefaultLocale")
    override fun getDialogConfiguration(): CVMEDlgFragmentConfigurator {
        val config = CVMEDlgFragmentConfigurator()

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
        config.autoRandomOrder = false
        config.okId = R.id.keypad_ok
        config.clearId = R.id.keypad_clear
        config.cancelId = R.id.keypad_cancel
        config.wildcardTextViewId = R.id.pinText
        config.infoTextViewId = R.id.infoText
        config.countDownTextViewId = R.id.countDownText
        config.clearContainerId = R.id.keypad_clear_container
        config.cancelContainerId = R.id.keypad_cancel_container
        config.okContainerId = R.id.keypad_ok_container
        config.layoutResourceID = R.layout.fragment_pin_entry
        config.dialogTheme = android.R.style.Theme_NoTitleBar_Fullscreen
        config.dialogStyle = DialogFragment.STYLE_NORMAL
        config.activity = this@HeadlessPaymentActivity

        val okConfig = CVMEElementConfig()
        okConfig.text = "OK"
        okConfig.textColor = "#000000".toColorInt()
        okConfig.backgroundColor = "#28a745".toColorInt()
        okConfig.fontSize = 50
        okConfig.height = 100
        okConfig.width = 100
        okConfig.font = Typeface.create("Roboto", Typeface.NORMAL)
        config.okConfig = okConfig

        val clearConfig = CVMEElementConfig()
        clearConfig.text = "Clr"
        clearConfig.textColor = "#000000".toColorInt()
        clearConfig.backgroundColor = "#ffff00".toColorInt()
        clearConfig.fontSize = 50
        clearConfig.height = 100
        clearConfig.width = 100
        clearConfig.font = Typeface.create("Roboto", Typeface.NORMAL)
        config.clearConfig = clearConfig

        val cancelConfig = CVMEElementConfig()
        cancelConfig.text = "Can"
        cancelConfig.textColor = "#000000".toColorInt()
        cancelConfig.backgroundColor = "#EB3223".toColorInt()
        cancelConfig.fontSize = 50
        cancelConfig.height = 100
        cancelConfig.width = 100
        cancelConfig.font = Typeface.create("Roboto", Typeface.NORMAL)
        config.cancelConfig = cancelConfig

        config.infoText = CurrencyTable.FormattedAmount(
            String.format("%06d", MainApplication.getInstance().paymentData.amountTransaction),
            MainApplication.getInstance().paymentData.currencyCode
        )
        config.countDownTextFormat = "Preostalo sekundi %s"
        config.countDownTimeInSeconds = 30
        config.isResetTimerOnClear = true
        config.restartTimerOnKeyInSeconds = 5

        val keyConfig = CVMEElementKeyConfig()
        keyConfig.textColor = "#000000".toColorInt()
        keyConfig.backgroundColor = "#FFFFFF".toColorInt()
        keyConfig.fontSize = 90
        keyConfig.height = 120
        keyConfig.width = 120
        keyConfig.font = Typeface.create("Roboto", Typeface.NORMAL)

        val randomAngle = CVMEElementKeyProperty()
        randomAngle.max = 0
        randomAngle.min = 0
        randomAngle.isActive = false
        keyConfig.randomAngle = randomAngle
        config.keyConfig = keyConfig

        okConfig.text = ""
        config.okConfig = okConfig
        clearConfig.text = ""
        config.clearConfig = clearConfig
        cancelConfig.text = ""
        config.cancelConfig = cancelConfig

        return config
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
        }
    }

    override fun onTransactionIdle() {
        logger.info { "onTransactionIdle" }
    }

    override fun onTransactionReadyToRead() {
        logger.info { "onTransactionReadyToRead" }
    }

    private fun playAudioIndication(isSuccessTone: Boolean) {
        try {
            val mToneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
            if (isSuccessTone) {
                mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
                Handler().postDelayed({ mToneGenerator.release() }, 501)
            } else {
                mToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 600)
                Handler().postDelayed({ mToneGenerator.release() }, 601)
            }
        } catch (e: Exception) {
            logger.info { "RING RING: $e.message" }
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