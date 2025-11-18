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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.icmp10.cvms.api.*
import com.icmp10.cvms.codes.opCvms.CvmsResult
import com.icmp10.mtms.api.MTMSListener
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult
import com.icmp10.mtms.codes.opTransact.TransactResult
import com.mastercard.sonic.controller.SonicController
import com.mastercard.sonic.controller.SonicType
import com.mastercard.sonic.listeners.OnCompleteListener
import com.mastercard.sonic.listeners.OnPrepareListener
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.databinding.ActivityPosBinding
import com.payten.whitelabel.databinding.DialogPinRegistrationBinding
import com.payten.whitelabel.dto.AppToAppResponseDto
import com.payten.whitelabel.dto.AppToAppSingleResponseDto
import com.payten.whitelabel.dto.AppToAppSingleResponseStatusDto
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
import androidx.core.graphics.toColorInt
import org.threeten.bp.LocalDateTime


@AndroidEntryPoint
class PosActivity : BaseActivity(), TransactionResultListener, LoyaltyActionListener,
    DisplayInterface, CVMSListener, MTMSListener {
    private val logger = KotlinLogging.logger {}
    private val TAG = "PosActivity"

    private lateinit var binding: ActivityPosBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs
    lateinit var model: PosViewModel

    private val sonicController = SonicController()
    private var isFailedTransaction = false
    private var shouldIgnoreDecline = true

    private var providedPackageName = ""

    private var pLedOn = 0x00

    private var data = ""
    private var paymentAdditionalData = ""

    private var tip = ""

    val SIMCORE_TRUE: Byte = 0x01
    private var lbin: ByteArray? = null
    private var lHash: ByteArray? = null

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    NfcAdapter.EXTRA_ADAPTER_STATE,
                    NfcAdapter.STATE_OFF
                )
                when (state) {
                    NfcAdapter.STATE_ON -> Toast.makeText(
                        applicationContext,
                        "NFC ON",
                        Toast.LENGTH_LONG
                    ).show()

                    NfcAdapter.STATE_OFF -> {
                        Toast.makeText(
                            applicationContext,
                            "Enable NFC and Try Again",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    NfcAdapter.STATE_TURNING_ON -> Toast.makeText(
                        applicationContext,
                        "NFC ADAPTER TURNING ON",
                        Toast.LENGTH_LONG
                    ).show()

                    NfcAdapter.STATE_TURNING_OFF -> {
                        Toast.makeText(
                            applicationContext,
                            "Enable NFC and Restart SOFTPOS",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger.info { "onCreate PosActivity" }

        val model2: PosViewModel by viewModels()
        model = model2

        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundAmount)

        try {
            SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
            MainApplication.getInstance().paymentData.transactionType =
                PaymentData.TransactionType.GOODS.internalType
        } catch (e: Exception) {
            logException(e.message)
        }

        MainApplication.getInstance().paymentData.transactionType =
            PaymentData.TransactionType.GOODS.internalType

        val amount = intent.getStringExtra("Amount")
        if (intent.hasExtra("providedPackageName")) {
            providedPackageName = intent.getStringExtra("providedPackageName")!!
        }

        var formattedAmount = AmountUtil.formatAmount(amount)

        binding.currency.text = SupercaseConfig.CURRENCY_STRING

        if (intent.hasExtra("Tip")) {
            tip = AmountUtil.formatAmount(intent.getStringExtra("Tip").toString())
            formattedAmount = AmountUtil.formatAmount(intent.getStringExtra("TotalAmount").toString())
            binding.amountValue.text = formattedAmount
        }

        paymentAdditionalData = createJsonAdditionalData(
            intent.getStringExtra("Tip").toString(),
            intent.getStringExtra("uniqueId").toString()
        )

        binding.amountValue.text = formattedAmount

        binding.back.setOnClickListener {
            finish()
        }

        handleCancelButton()

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
                } else {
                    MainApplication.getInstance().setRealProviders()
                }
            }

            if (MainApplication.getInstance().cardCommunicationProvider != null) {
                if (MainApplication.getInstance().cardCommunicationProvider.interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
                    MainApplication.getInstance().cardCommunicationProvider.connectReader(this)
                    Log.e("FIME", "connectReader")
                }
                if (MainApplication.getInstance().cardCommunicationProvider.interfaceType == CardCommunicationProvider.InterfaceType.STATIC) {
                    MainApplication.getInstance().cardCommunicationProvider.connectReader(this)
                }
            }

            if (amount != null) {
                MainApplication.getInstance().setPaymentAmount(AmountUtil.setAmount(amount))

                if (!paymentAdditionalData.isEmpty()) {
                    MainApplication.getInstance().paymentData.merchantAdditionalData =
                        paymentAdditionalData
                } else {
                    MainApplication.getInstance().paymentData.merchantAdditionalData = "None"
                }
            }

            binding.btn.setOnClickListener {
                SoftPOSSDK.setAutoMode(false)
                SoftPOSSDK.setCancelled(true)
                SoftPOSSDK.resetReaderOutcome()
                SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
                finish()
            }
        } else {
            logException("")
            Toast.makeText(this, "Transaction Error", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        if (packageName.isEmpty()) {
            try {
                SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
            } catch (e: Exception) {
                logger.error { "Cannot cancel transaction: ${e}" }
                logException(e.message)
            }

            unregisterReceiver(mReceiver)

            MainApplication.getInstance().mtmsListener.setListener(null)
            MainApplication.getInstance().cvmsListener.setListener(null)
            MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = null
            MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = null
            MainApplication.getInstance().configurationInterface.setDisplayInterface(null)

            NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()

        val discovery = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val tagFilters = arrayOf(discovery)
        val i = Intent(this, javaClass).addFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                val dialog = Dialog(this)
                val dialogBinding: DialogPinRegistrationBinding =
                    DialogPinRegistrationBinding.inflate(layoutInflater)

                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text = this.getString(R.string.label_nfc_off)
                dialogBinding.btn.text = this.getString(R.string.button_nfc_go)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                    startActivity(intent)
                    finish()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            } else {
                MainApplication.getInstance().setRealProviders()
                TransactionApi.doTransaction(
                    this@PosActivity,
                    MainApplication.getInstance().paymentData
                )
            }
        }
    }

    private fun resetTransaction() {
        try {
            runOnUiThread {
                isFailedTransaction = false
                binding.spinner.visibility = View.GONE
                TransactionApi.doTransaction(
                    this@PosActivity,
                    MainApplication.getInstance().paymentData
                )
            }
        } catch (e: Exception) {
            logger.error { "Starting transaction failed: $e" }
            logException(e.message)
        }
    }

    private fun handleDarkLightMode() {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
            binding.amountValue.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.amountValue.setBackgroundColor(
                ContextCompat.getColor(this, R.color.globalBlackDialog)
            )
        } else {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
            binding.amountValue.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.amountValue.setBackgroundColor(
                ContextCompat.getColor(this, R.color.amountBackground)
            )
        }
    }

    private fun handleCancelButton() {
        // Empty implementation
    }

    override fun onTransactionProcessing() {
        Log.i("LED","onTransactionProcessing")
        logger.info { "Transaction onTransactionProcessing" }
        setLedIndicators(0x02, true)
    }

    override fun onTransactionSuccessful() {
        Log.i("LED","onTransactionSuccessful")
        logger.info { "Transaction onTransactionSuccessful" }
        setLedIndicators(0x0F, false)
    }

    override fun onTransactionDeclined() {
        Log.i("LED","onTransactionDeclined")
        logger.info { "Transaction onTransactionDeclined" }

        playAudioIndication(false)
        setLedIndicators(0x0F, false)

        if (shouldIgnoreDecline) {
            resetTransaction()
            return
        }

        this@PosActivity.runOnUiThread {
            try {
                val dialog = Dialog(this)
                val dialogBinding: DialogPinRegistrationBinding =
                    DialogPinRegistrationBinding.inflate(layoutInflater)

                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text = this.getString(R.string.label_transaction_declined)
                dialogBinding.btn.text = this.getString(R.string.transaction_button_reset)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                    this.resetTransaction()
                    binding.pinOverlay.visibility = View.INVISIBLE
                    binding.back.visibility = View.INVISIBLE
                    binding.btn.visibility = View.VISIBLE
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            } catch (e: Exception) {
                logException(e.message)
            }
        }
    }

    override fun onTransactionEnded(p0: String?) {
        Log.i("LED","onTransactionEnded")
        logger.info { "Transaction onTransactionEnded ${p0}" }

        setLedIndicators(0x0F, false)
        if (p0?.contains("TRY_AGAIN") == true) {
            shouldIgnoreDecline = true
        } else {
            this@PosActivity.runOnUiThread {
                try {
                    val dialog = Dialog(this)
                    val dialogBinding: DialogPinRegistrationBinding =
                        DialogPinRegistrationBinding.inflate(layoutInflater)

                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                    dialogBinding.warningLabel.text = this.getString(R.string.label_transaction_expired)
                    dialogBinding.btn.text = this.getString(R.string.transaction_void_button_reset)

                    dialogBinding.btn.setOnClickListener {
                        dialog.dismiss()
                        finish()
                    }
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                } catch (_: Exception) {

                }
            }
        }
    }

    override fun onTransactionCancelled() {
        Log.i("LED","onTransactionCancelled")
        setLedIndicators(0x0F, false)
        logger.info { "Transaction onTransactionCancelled" }
        finish()
    }

    override fun onTransactionNotStarted(p0: String?) {
        Log.i("LED","onTransactionNotStarted")
        logger.info { "Transaction SDK onTransactionNotStarted ${p0}" }
        playAudioIndication(false)
        setLedIndicators(0x0F, false)
        if (p0.equals("Parameters not Ready Yet", true)) {
            Timer().schedule(2000) {
                resetTransaction()
            }
        }
    }

    override fun onTransactionOnline() {
        Log.i("LED","onTransactionOnline")
        setLedIndicators(0x0F, true)
        this@PosActivity.runOnUiThread {
            Timer().schedule(1000) {
                runOnUiThread {
                    binding.pinOverlay.visibility = View.VISIBLE
                    binding.back.visibility = View.INVISIBLE
                }
            }
            binding.btn.visibility = View.INVISIBLE
            binding.spinner.visibility = View.VISIBLE
            handleCancelButton()
        }
        val mToneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
        mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
        logger.info { "Transaction onTransactionOnline" }
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
            this@PosActivity.runOnUiThread {
                try {
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
                            operationName = resources.getString(R.string.label_operation_sales),
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
                            tipAmount = if (tip.isNotEmpty()) tip else "0.0"
                        )
                    }

                    if (p0?.transactionResponseData?.operationName.equals("Void", true)) {
                        transactionData?.operationName = "Storno"
                    }

                    if (providedPackageName.isEmpty()) {

                        if (!p0?.transactionResponseData?.responseCode.equals("00", true)) {
                            val resultIntent = Intent().apply {
                                putExtra("transaction_data", transactionData)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            if (p0?.transactionResponseData?.applicationLabel!!.contains("visa", true)) {
                                // VISA animation
                                binding.visaViewLayout.visibility = View.VISIBLE
                                binding.pinOverlay.visibility = View.GONE
                                binding.visaView.isSoundEffectsEnabled = true
                                binding.visaView.setBackgroundColor("#ffffff".toColorInt())
                                binding.visaView.backdropColor = "#ffffff".toColorInt()
                                binding.visaView.animate {
                                    val resultIntent = Intent().apply {
                                        putExtra("transaction_data", transactionData)
                                    }
                                    setResult(RESULT_OK, resultIntent)
                                    finish()
                                }
                            } else if (p0.transactionResponseData?.applicationLabel!!.contains("card", true)) {
                                // MASTERCARD animation
                                binding.mcardView.visibility = View.VISIBLE
                                sonicController.prepare(
                                    context = this,
                                    sonicType = SonicType.SOUND_AND_ANIMATION,
                                    onPrepareListener = object : OnPrepareListener {
                                        override fun onPrepared(statusCode: Int) {
                                            playSonicAndReturn(transactionData)
                                        }
                                    }
                                )
                            } else {
                                val resultIntent = Intent().apply {
                                    putExtra("transaction_data", transactionData)
                                }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        }
                    } else {
                        val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
                        val intent = packageManager.getLaunchIntentForPackage(providedPackageName)

                        var status = transactionData?.response
                        if (status == null) status = "05"

                        var message = transactionData?.message
                        if (message == null) message = "Placanje neuspesno"

                        data = gson.toJson(transactionData)
                        val obj = AppToAppResponseDto(
                            AppToAppSingleResponseDto(
                                AppToAppSingleResponseStatusDto(
                                    status, message, data
                                ), p0?.transactionResponseData!!.recordId
                            )
                        )
                        logger.info("Sending to packagename: ${providedPackageName} dto: ${obj}")
                        if (intent != null) {
                            intent.putExtra("RESPONSE_JSON_STRING", gson.toJson(obj))
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Prosledjen packageName nije validan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                } catch (e: Exception) {
                    logException(e.message.toString())
                }
            }
        }

        if (p0?.transactionResponseData?.statusCode.equals("D", true)) {
            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            handleDialogDarkMode(dialogBinding)
            dialogBinding.icon.setImageResource(R.drawable.icon_warning)
            dialogBinding.warningLabel.text = this.getString(R.string.label_transaction_declined)
            dialogBinding.btn.text = this.getString(R.string.transaction_void_button_reset)

            dialogBinding.btn.setOnClickListener {
                dialog.dismiss()

                val transactionData = TransactionDetailsDto(
                    aid = "",
                    applicationLabel = "",
                    authorizationCode = "",
                    bankName = "",
                    cardNumber = "",
                    dateTime = LocalDateTime.now().toString(),
                    merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID),
                    merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                    message = this.getString(R.string.label_transaction_declined),
                    operationName = resources.getString(R.string.label_operation_sales),
                    response = "05",
                    rrn = "",
                    code = "",
                    status = "D",
                    terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID),
                    amount = intent.getStringExtra("Amount") ?: "0.00",
                    isIps = false,
                    sdkStatus = null,
                    billStatus = null,
                    color = -1,
                    recordId = "",
                    listName = "",
                    tipAmount = if (tip.isNotEmpty()) tip else "0.0"
                )

                val resultIntent = Intent().apply {
                    putExtra("transaction_data", transactionData)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setContentView(dialogBinding.root)
            dialog.show()
        }
    }

    override fun onOnlineResponse(p0: CvmsResult?) {
        logger.info { "Transaction onOnlineResponse3 ${p0?.cvmsStatusCode.toString()}" }
        shouldIgnoreDecline = false
    }

    override fun onCVMEEntered(p0: Int) {
        logger.info { "Pin entered: ${p0}" }
        binding.pinOverlay.visibility = View.VISIBLE
        binding.back.visibility = View.INVISIBLE
        shouldIgnoreDecline = false
        CVMTransactionApi.doTransactionPCPOC(
            this,
            MainApplication.getInstance().paymentData.transactionType
        )
    }

    override fun onCVMETimeout() {
        SoftPOSSDK.setAutoMode(false)
        SoftPOSSDK.setCancelled(true)
        SoftPOSSDK.resetReaderOutcome()
        SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()

        val dialog = Dialog(this)
        val dialogBinding: DialogPinRegistrationBinding =
            DialogPinRegistrationBinding.inflate(layoutInflater)

        dialogBinding.icon.setImageResource(R.drawable.icon_warning)
        dialogBinding.warningLabel.text = this.getString(R.string.label_pin_not_entered)
        dialogBinding.btn.text = this.getString(R.string.label_pin_not_entered_button)

        dialogBinding.btn.setOnClickListener {
            finish()
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    override fun onCVMECancelled() {
        logger.info { "onCVMECancelled" }

        if (providedPackageName.isEmpty()) {
            SoftPOSSDK.setAutoMode(false)
            SoftPOSSDK.setCancelled(true)
            SoftPOSSDK.resetReaderOutcome()
            SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
            finish()
        } else {
            returnFailToApp()
        }
    }

    private fun playSonicAndReturn(transactionData: TransactionDetailsDto?) {
        binding.pinOverlay.visibility = View.INVISIBLE
        binding.back.visibility = View.INVISIBLE
        if (!sonicController.isPlaying) {
            sonicController.play(onCompleteListener = object : OnCompleteListener {
                override fun onComplete(statusCode: Int) {
                    val resultIntent = Intent().apply {
                        putExtra("transaction_data", transactionData)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }, sonicView = binding.mcardView)
        }
    }

    @SuppressLint("DefaultLocale")
    override fun getDialogConfiguration(): CVMEDlgFragmentConfigurator {
        Log.i(TAG,"getDialogConfiguration")

        val config = CVMEDlgFragmentConfigurator()

        config.key0ContainerId = R.id.keypad_1_container
        R.id.keypad_2

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

        config.activity = this@PosActivity

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

        config.infoText = "Info Text"
        config.infoText = CurrencyTable.FormattedAmount(
            String.format(
                "%06d",
                MainApplication.getInstance().paymentData.amountTransaction
            ), MainApplication.getInstance().paymentData.currencyCode
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

    override fun onBINDetected(bin: ByteArray, panHash: ByteArray): Int {
        lbin = bin.clone()
        lHash = panHash.clone()
        return SIMCORE_TRUE.toInt()
    }

    override fun onLoyaltyOnlineRequest(): ByteArray? {
        return null
    }

    override fun onLoyaltyOnlineResponse(p0: TransactResult?) {
        // Empty implementation
    }

    override fun displayStop(p0: UserInterfaceData?) {
        // Empty implementation
    }

    override fun displayMessage(p0: UserInterfaceData?) {
        Log.i("LED","displayMessage")
        logger.info { "Display message $p0" }

        if (p0?.uirdStatus == UserInterfaceData.UIRDStatus.UIRD_STATUS_CARD_READ_SUCCESSFULLY) {
            playAudioIndication(true)
            setLedIndicators(0x04, true)
        }
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

    private fun returnFailToApp() {
        val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
        val obj = AppToAppResponseDto(
            AppToAppSingleResponseDto(
                AppToAppSingleResponseStatusDto(
                    "05", resources.getString(R.string.pin_transaction_cancel), data
                ), ""
            )
        )
        val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
        logger.info("Sending to packagename: ${providedPackageName} dto: ${obj}")
        if (intent != null) {
            intent.putExtra("RESPONSE_JSON_STRING", gson.toJson(obj))
        } else {
            Toast.makeText(
                applicationContext,
                "Prosledjen packageName nije validan",
                Toast.LENGTH_LONG
            ).show()
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun handleDialogDarkMode(dialogBinding: DialogPinRegistrationBinding) {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            dialogBinding.root.setBackgroundColor(
                ContextCompat.getColor(this, R.color.globalBlackDialog)
            )
            dialogBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
            dialogBinding.btn.setBackgroundColor(
                ContextCompat.getColor(this, R.color.globalBlackDialog)
            )
        }
    }

    override fun onTransactionIdle() {
        Log.i("LED","onTransactionIdle")
        setLedIndicators(0x01, true)
    }

    override fun onTransactionReadyToRead() {
        Log.i("LED","onTransactionReadyToRead")
        setLedIndicators(0x01, true)
    }

    private fun setLedIndicators(ledOn: Int, isLedON: Boolean) {
        if (!isLedON) pLedOn = 0
        this@PosActivity.runOnUiThread {
            if ((ledOn and 0x01 == 0x01) && (pLedOn and 0x01 != 0x01)) {
                Log.i("LED", "1 | $isLedON")
                ledIndicator(1, isLedON)
            }
            if ((ledOn and 0x02 == 0x02) && (pLedOn and 0x02 != 0x02)) {
                Log.i("LED", "2 | $isLedON")
                ledIndicator(2, isLedON)
            }
            if ((ledOn and 0x04 == 0x04) && (pLedOn and 0x04 != 0x04)) {
                Log.i("LED", "3 | $isLedON")
                ledIndicator(3, isLedON)
            }
            if ((ledOn and 0x08 == 0x08) && (pLedOn and 0x08 != 0x08)) {
                Log.i("LED", "4 | $isLedON")
                ledIndicator(4, isLedON)
            }
            if ((isLedON) && (pLedOn and ledOn != ledOn)) {
                Log.i("LED", "5 | $isLedON")
                pLedOn += ledOn
            }
        }
    }

    private fun ledIndicator(ledId: Int, isLedON: Boolean) {
        val imageView = when (ledId) {
            1 -> binding.visualIndicatorLed1
            2 -> binding.visualIndicatorLed2
            3 -> binding.visualIndicatorLed3
            4 -> binding.visualIndicatorLed4
            else -> binding.visualIndicatorLed4
        }
        val drawable: Int = if (isLedON)
            R.drawable.circle_primary
        else
            R.drawable.circle_grey
        imageView.setBackgroundResource(drawable)
    }

    private fun playAudioIndication(isSuccessTone: Boolean) {
        try {
            this@PosActivity.runOnUiThread {
                val mToneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
                if (isSuccessTone) {
                    mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
                    Handler().postDelayed({
                        mToneGenerator.release()
                    }, 501)
                } else {
                    mToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 600)
                    Handler().postDelayed({
                        mToneGenerator.release()
                    }, 601)
                }
            }
        } catch (e: Exception) {
            logger.info { "RING RING:  $e.message" }
        }
    }

    fun createJsonAdditionalData(tip: String, uniqueId: String): String {
        val paytenAdditionalData = JSONObject()
        val paytenTrnx = JSONObject()
        try {
            if (uniqueId != "null") {
                logger.info { "uniqueId:  $uniqueId" }
                paytenTrnx.put("uniqueId", uniqueId)
            }
            if (tip != "null") {
                logger.info { "Tip:  $tip" }
                paytenTrnx.put("tipAmount", createTipData(tip))
            }
        } catch (e: java.lang.Exception) {
            logger.error { e.message }
        }

        paytenAdditionalData.put("paytenTransactionRequest", paytenTrnx)
        logger.info { "AddtionalData:  $paytenAdditionalData" }
        return paytenAdditionalData.toString()
    }

    fun createTipData(tip: String): String {
        val formatedTip = tip.replace(".","").replace(",","")
        return formatedTip.padStart(12, '0')
    }
}