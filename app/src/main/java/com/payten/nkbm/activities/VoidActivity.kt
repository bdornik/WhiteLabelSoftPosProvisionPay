package com.payten.nkbm.activities

import android.app.*
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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.icmp10.cvms.api.*
import com.icmp10.cvms.codes.opCvms.CvmsResult
import com.icmp10.mtms.api.MTMSListener
import com.icmp10.mtms.codes.MTMSStatusCode
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult
import com.icmp10.mtms.codes.opTransact.TransactResult
import com.mastercard.sonic.controller.SonicController
import com.mastercard.sonic.listeners.OnCompleteListener
import com.payten.nkbm.R
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.databinding.ActivityVoidBinding
import com.payten.nkbm.databinding.DialogPinRegistrationBinding
import com.payten.nkbm.dto.AppToAppResponseDto
import com.payten.nkbm.dto.AppToAppSingleResponseDto
import com.payten.nkbm.dto.AppToAppSingleResponseStatusDto
import com.payten.nkbm.dto.TransactionDetailsDto
import com.payten.nkbm.enums.ErrorDescription
import com.payten.nkbm.enums.TransactionStatus
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.AmountUtil
import com.payten.nkbm.viewmodel.VoidViewModel
import com.sacbpp.core.bytes.ByteArray
import com.sacbpp.core.bytes.ByteArrayFactory
import com.simant.MainApplication
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
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.json.JSONObject
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule


@AndroidEntryPoint
class VoidActivity : BaseActivity(), TransactionResultListener, LoyaltyActionListener,
    DisplayInterface, CVMSListener, MTMSListener {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityVoidBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private val sonicController = SonicController()
    private var isFailedTransaction = false

    private var providedPackageName = ""

    private var shouldIgnoreDecline = true

    lateinit var model: VoidViewModel

    private var isBackEnabled = true
    private var transactionFistResponse: TransactResult? = null

    private var data: TransactionDetailsDto? = null;

    private var pLedOn = 0x00

    private var paymentAdditionalData = ""

    private var tip = ""




    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

    val SIMCORE_TRUE: Byte = 0x01
    val ACTIVITY_TIMER: Long = 2000
    val SIMCORE_SCTERMINATE: Byte = 0x04
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVoidBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)


        val model2: VoidViewModel by viewModels()
        model = model2

        try {
            SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
            MainApplication.getInstance().paymentData.transactionType =
                PaymentData.TransactionType.GOODS.internalType
        } catch (e: Exception) {
            logException(e.message)
        }


        MainApplication.getInstance().paymentData.transactionType =
            PaymentData.TransactionType.VOID.internalType

        binding.currency.text = SupercaseConfig.CURRENCY_STRING
        if (intent.hasExtra("providedPackageName")) {
            providedPackageName = intent.getStringExtra("providedPackageName")!!
            isBackEnabled = false
            binding.back.visibility = View.INVISIBLE

            val amount = intent.getStringExtra("Amount")

            MainApplication.getInstance().paymentData.transactionId =
                intent.getStringExtra("authorizationCode")
            MainApplication.getInstance().setPaymentAmount(amount!!.toLong())

            val formattedAmount = formatAmount(amount.toLong())
            binding.amountValue.text = formattedAmount
        } else {
            data = intent.getSerializableExtra("data") as TransactionDetailsDto

            var amount = intent.getStringExtra("Amount")
            var sdkAmount = Math.round(amount!!.toDouble() * 100)

            var formattedAmount = formatAmount(sdkAmount)
            binding.amountValue.text = "$formattedAmount"

//            if (intent.hasExtra("Tip")) {
//                val res = AmountUtil.getAmountWithOutCurr(
//                    (amount.toDouble() + intent.getStringExtra("Tip")!!.toDouble()).toString()
//                )
//                var tip = intent.getStringExtra("Tip")
//
//
//                this.tip = AmountUtil.formatAmount(intent.getStringExtra("Tip").toString())
//                var sdkAmountTip = Math.round(tip!!.toDouble() * 100)
//
//                MainApplication.getInstance().paymentData.amountOther = sdkAmountTip
//                binding.amountValue.text = "$res"
//            }

            paymentAdditionalData =
                createJsonAdditionalData(
                    intent.getStringExtra("Tip").toString(),
                    intent.getStringExtra("uniqueId").toString()
                )

            logger.info { "paymentAdditionalData: " + paymentAdditionalData }

            if (!paymentAdditionalData.isEmpty()) {
                MainApplication.getInstance().paymentData.merchantAdditionalData =
                    paymentAdditionalData
            } else {
                MainApplication.getInstance().paymentData.merchantAdditionalData = "None"
            }

            if (intent.hasExtra("Tip")) {
                //paymentAdditionalData = createJsonAdditionalData(intent.getStringExtra("Tip").toString())
                tip = AmountUtil.getAmountNoCurr(intent.getStringExtra("Tip").toString())
//
//            displayAmount = AmountUtil.formatAmount(intent.getStringExtra("TotalAmount").toString())

                binding.amountValue.text = intent.getStringExtra("TotalAmount").toString()
            }

            MainApplication.getInstance().setPaymentAmount(sdkAmount)



        }





        binding.back.setOnClickListener {
            finish()
        }

        handleCancelButton(true)

        MainApplication.getInstance().mtmsListener.setListener(this)
        MainApplication.getInstance().cvmsListener.setListener(this)
        MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = this
        MainApplication.getInstance().loyaltyObserver.setLoyaltyActionListener(this)
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


        logger.info { "request: " +  MainApplication.getInstance().paymentData.toString() }

        binding.btn.setOnClickListener {
            SoftPOSSDK.setAutoMode(false)
            SoftPOSSDK.setCancelled(true)
            SoftPOSSDK.resetReaderOutcome()
            SoftPOSSDK.getInstance().transactionInterface.cancelTransaction()
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
            }

            unregisterReceiver(mReceiver)

            MainApplication.getInstance().mtmsListener.setListener(null)
            MainApplication.getInstance().cvmsListener.setListener(null)
            MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener =
                null
            MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = null
            MainApplication.getInstance().configurationInterface.setDisplayInterface(null)

            NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
        }
    }

    private fun formatAmount(sdkAmount: Long): String {
        var formattedAmount = ""
        formattedAmount = sdkAmount.toString()
        if (formattedAmount.length == 1) {
            formattedAmount = "0,0$formattedAmount"
        } else if (formattedAmount.length == 2)
            formattedAmount = "0,$formattedAmount"
        else {
            var preDotsString = "${formattedAmount?.subSequence(0, formattedAmount.length - 2)}"
            val postDotsString = "${
                formattedAmount?.subSequence(
                    formattedAmount.length - 2,
                    formattedAmount.length
                )
            }"
            formattedAmount = "${
                preDotsString.reversed()
                    .chunked(3)
                    .joinToString(".")
                    .reversed()
            },$postDotsString"
        }
        return formattedAmount
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        val discovery = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val tagFilters = arrayOf(discovery)
        val i = Intent(this, javaClass).addFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )

        var pi: PendingIntent? = null
        pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT)
        }
//        val pi = PendingIntent.getActivity(this, 0, i, 0)

        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, pi, tagFilters, null)

        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        this.registerReceiver(mReceiver, filter)

        if (SoftPOSSDK.getCardCommunicationProvider().interfaceType == CardCommunicationProvider.InterfaceType.INTERNAL_NFC) {
            if (!SoftPOSSDK.getCardCommunicationProvider().isEnabled) {
                logger.error { "NFC Not enabled" }
                val dialog = Dialog(this)
                val dialogBinding: DialogPinRegistrationBinding =
                    DialogPinRegistrationBinding.inflate(layoutInflater)

                handleDialogDarkMode(dialogBinding)
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
            } else {
                MainApplication.getInstance().setRealProviders()
                logger.info { "Doing transaction with data: ${MainApplication.getInstance().paymentData}" }
                TransactionApi.doTransaction(
                    this@VoidActivity,
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
                    this@VoidActivity,
                    MainApplication.getInstance().paymentData
                )
            }
        } catch (e: Exception) {
            logger.error { "Starting transaction failed: ${e}" }
            //logException(e.message)
        }
    }

    private fun successfulTransaction() {
        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun handleDialogDarkMode(dialogBinding: DialogPinRegistrationBinding) {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            dialogBinding.root.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
            dialogBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
            dialogBinding.btn.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
        }

        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.back.drawable),
            ContextCompat.getColor(this, R.color.colorPrimary)
        )
    }

    private fun handleCancelButton(isEnabled: Boolean) {
        if (isEnabled) {
            binding.btn.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.buttonGreen);
            binding.btn.setTextColor(ContextCompat.getColorStateList(this, R.color.white))
            binding.btn.isEnabled = true
        } else {
            binding.btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray);
            binding.btn.setTextColor(ContextCompat.getColorStateList(this, R.color.white))
            binding.btn.isEnabled = false
        }
    }

    override fun onTransactionProcessing() {
        logger.info { "Transaction onTransactionProcessing" }
        setLedIndicators(0x02, true)

    }

    override fun onTransactionSuccessful() {
        logger.info { "Transaction onTransactionSuccessful" }
        setLedIndicators(0x0F, false)
    }

    override fun onTransactionDeclined() {
        logger.info { "Transaction onTransactionDeclined" }

        playAudioIndication(false)
        setLedIndicators(0x0F, false)


        if (shouldIgnoreDecline) {
            resetTransaction()
            return
        }

        this@VoidActivity.runOnUiThread {
            try {
                val dialog = Dialog(this)
                val dialogBinding: DialogPinRegistrationBinding =
                    DialogPinRegistrationBinding.inflate(layoutInflater)

//                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text =
                    "${this.getString(R.string.label_transaction_declined)}"
                dialogBinding.btn.text = this.getString(R.string.transaction_button_reset)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()

                    this.resetTransaction()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            } catch (e: Exception) {
                //logException(e.message)
            }
        }
    }

    override fun onTransactionEnded(p0: String?) {
        logger.info { "Transaction onTransactionEnded ${p0}" }

        setLedIndicators(0x0F, false)

        if (p0?.contains("TRY_AGAIN") == true) {
            shouldIgnoreDecline = true
        } else {
            this@VoidActivity.runOnUiThread {
                try {
                    if (providedPackageName.isEmpty())
                        voidDialog(true)
                    else
                        responseToApp(null)
                } catch (e: Exception) {

                }
            }
        }

    }

    override fun onTransactionCancelled() {
        logger.info { "Transaction onTransactionCancelled" }
        setLedIndicators(0x0F, false)
        finish()
    }

    override fun onTransactionNotStarted(p0: String?) {
        logger.info { "Transaction SDK onTransactionNotStarted ${p0}" }
        playAudioIndication(false)
        setLedIndicators(0x0F, false)
    }

    override fun onTransactionOnline() {
        setLedIndicators(0x0F, true)
        this@VoidActivity.runOnUiThread {
            Timer().schedule(1000) {
                runOnUiThread {
                    binding.pinOverlay.visibility = View.VISIBLE
                    binding.back.visibility = View.INVISIBLE
                }
            }
            binding.spinner.visibility = View.VISIBLE
            handleCancelButton(true)
        }
        val mToneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
        mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
        logger.info { "Transaction onTransactionOnline" }

//        resetTransaction()
    }

    override fun onOnlineRequest(): ByteArray? {
        logger.info { "Transaction onOnlineRequest" }
//        return null
        if (MainApplication.getInstance().paymentData.transactionType.equals(
                PaymentData.TransactionType.VOID.internalType,
                true
            )
        ) {
            //ByteArray refundTags = ByteArrayFactory.getInstance().fromHexString("5A06123456789012");
            if (MainApplication.getInstance().paymentData.transactionId.length > 0) {
                //String recordID = "1192";
                val refundTagS = String.format(
                    "DF829050%02X%s",
                    MainApplication.getInstance().paymentData.transactionId.length,
                    ByteArrayFactory.getInstance()
                        .getByteArray(MainApplication.getInstance().paymentData.transactionId.toByteArray())
                        .getHexString()
                )
                val refundTags = ByteArrayFactory.getInstance().fromHexString(refundTagS)
                MainApplication.getInstance().paymentData.transactionId =
                    ""
                return refundTags
            }
        }
        MainApplication.getInstance().paymentData.transactionId = ""
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
        logger.info { "providedPackageName: " + providedPackageName }
        if (p0?.mtmsStatusCode == MTMSStatusCode.SUCCESS) {
            if (providedPackageName.isEmpty()) {
                voidDialog(false)
            } else {
                transactionFistResponse = p0
            }
        }
    }

    override fun onOnlineResponse(p0: GetTransactionResult?) {
        logger.info {
            "Transaction onOnlineResponse2 ${
                p0?.getTransactionResponseData().toString()
            }"
        }
        logger.info { "providedPackageName 2: " + providedPackageName }
        if (transactionFistResponse?.mtmsStatusCode == MTMSStatusCode.SUCCESS && transactionFistResponse != null) {
            if (providedPackageName.isEmpty()) {
                voidDialog(false)
            } else {
                responseToApp(p0)
            }
        }

    }

    override fun onOnlineResponse(p0: CvmsResult?) {
        logger.info { "Transaction onOnlineResponse3 ${p0?.cvmsStatusCode.toString()}" }
    }

    override fun onCVMEEntered(p0: Int) {
        logger.info { "Pin entered: ${p0}" }
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
    }

    override fun onCVMECancelled() {
        resetTransaction()
    }

    //Create private method to play sonic
    private fun playSonic(intent: Intent) {
        if (!sonicController.isPlaying) {
            sonicController.play(onCompleteListener = object : OnCompleteListener {
                override fun onComplete(statusCode: Int) {
                    resultLauncher.launch(intent)
                }
            }, sonicView = binding.mcardView)
        }
    }


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
        config.autoRandomOrder = true
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

        config.activity = this@VoidActivity

        val okConfig = CVMEElementConfig()
        okConfig.text = "OK"
        okConfig.textColor = Color.parseColor("#000000")
        okConfig.backgroundColor = Color.parseColor("#28a745")
        okConfig.fontSize = 50
        okConfig.height = 100
        okConfig.width = 100
        okConfig.font = Typeface.create("Arial", Typeface.NORMAL)
        config.okConfig = okConfig

        val clearConfig = CVMEElementConfig()
        clearConfig.text = "Clr"
        clearConfig.textColor = Color.parseColor("#000000")
        clearConfig.backgroundColor = Color.parseColor("#ffff00")
        clearConfig.fontSize = 50
        clearConfig.height = 100
        clearConfig.width = 100
        clearConfig.font = Typeface.create("Arial", Typeface.NORMAL)
        config.clearConfig = clearConfig

        val cancelConfig = CVMEElementConfig()
        cancelConfig.text = "Can"
        cancelConfig.textColor = Color.parseColor("#000000")
        cancelConfig.backgroundColor = Color.parseColor("#ff0000")
        cancelConfig.fontSize = 50
        cancelConfig.height = 100
        cancelConfig.width = 100
        cancelConfig.font = Typeface.create("Arial", Typeface.NORMAL)
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
        keyConfig.textColor = Color.parseColor("#000000")
        keyConfig.backgroundColor = Color.parseColor("#FFFFFF")
        keyConfig.fontSize = 60
        keyConfig.height = 120
        keyConfig.width = 120
        keyConfig.font = Typeface.create("Roboto", Typeface.NORMAL)

        val randomAngle = CVMEElementKeyProperty()
        randomAngle.max = 0
        randomAngle.min = 0
        randomAngle.isActive = false
        keyConfig.randomAngle = randomAngle

        config.keyConfig = keyConfig

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
//        TODO("Not yet implemented")
    }

    override fun displayStop(p0: UserInterfaceData?) {
//        TODO("Not yet implemented")
    }

    override fun displayMessage(p0: UserInterfaceData?) {
        logger.info { "Display message ${p0}" }

        if (p0?.uirdStatus == UserInterfaceData.UIRDStatus.UIRD_STATUS_CARD_READ_SUCCESSFULLY) {
            playAudioIndication(true)
            setLedIndicators(0x04, true)
        }
    }

    private fun voidDialog(failed: Boolean) {
        this@VoidActivity.runOnUiThread {
            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            val intent = Intent(this, TransactionActivity::class.java)

            if (tip.isNotEmpty()){
                intent.putExtra("tip", tip)
                intent.putExtra("totalAmount",  binding.amountValue.text)
                data?.tipAmount = tip

            }
            data?.operationName = resources.getString(R.string.transaction_operation_void)
            if (failed) {
                data?.response = "06"
                data?.sdkStatus = TransactionStatus.Rejected
                intent.putExtra("data", data)

                intent.putExtra("fromVoid", true)
                startActivity(intent)
                finish()
            } else {
                data?.response = "00"
                data?.sdkStatus = TransactionStatus.Voided
                intent.putExtra("data", data)
                intent.putExtra("fromVoid", true)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun responseToApp(p0: GetTransactionResult?) {
        var dataResponse = ""
        val transactionData = p0?.transactionResponseData?.let {
            TransactionDetailsDto(
                aid = p0?.transactionResponseData?.aid,
                applicationLabel = p0?.transactionResponseData?.applicationLabel,
                authorizationCode = it.authorizationCode,
                bankName = "",
                cardNumber = p0?.transactionResponseData!!.maskedPAN,
                dateTime = p0?.transactionResponseData!!.transactionDate,
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                message = p0?.transactionResponseData!!.screenMessage,
                operationName = "Storno",
                response = p0?.transactionResponseData!!.responseCode,
                rrn = "",
                code = p0?.transactionResponseData!!.recordId,
                status = p0?.transactionResponseData!!.statusCode,
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID),
                amount = p0?.transactionResponseData!!.amount,
                isIps = false,
                sdkStatus = null,
                listName = "",
                billStatus = null,
                color = 0,
                recordId = null,
                tipAmount = "0.0"

            )
        }

        if (p0?.transactionResponseData?.operationName!!.contains("Void", true))
            transactionData?.operationName = "Storno"


        var status = transactionData?.response
        if (status == null)
            status = "05"

        var message = transactionData?.message
        if (message == null)
            message = "Void neuspesan"

        val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
        val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
        dataResponse = gson.toJson(transactionData)


        var recordId = ""
        if (p0 != null)
            recordId = p0?.transactionResponseData!!.recordId

        val obj = AppToAppResponseDto(
            AppToAppSingleResponseDto(
                AppToAppSingleResponseStatusDto(
                    status,
                    message,
                    dataResponse
                ),
                recordId
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
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onTransactionIdle() {
        setLedIndicators(0x01, true)
        //showErrorFlag = true
    }

    override fun onTransactionReadyToRead() {
        setLedIndicators(0x01, true)
    }


    private fun setLedIndicators(ledOn: Int, isLedON: Boolean) {
        if (isLedON == false) pLedOn = 0;
        this@VoidActivity?.runOnUiThread(Runnable() {
            if ((ledOn and 0x01 == 0x01) && (pLedOn and 0x01 != 0x01))
                ledIndicator(1, isLedON)
            if ((ledOn and 0x02 == 0x02) && (pLedOn and 0x02 != 0x02))
                ledIndicator(2, isLedON)
            if ((ledOn and 0x04 == 0x04) && (pLedOn and 0x04 != 0x04))
                ledIndicator(3, isLedON)
            if ((ledOn and 0x08 == 0x08) && (pLedOn and 0x08 != 0x08))
                ledIndicator(4, isLedON)
            if ((isLedON) && (pLedOn and ledOn != ledOn)) pLedOn += ledOn;
        })
    }

    private fun ledIndicator(ledId: Int, isLedON: Boolean) {
        var imageView: View?
        var drawable: Int
        when (ledId) {
            1 -> imageView = binding.visualIndicatorLed1
            2 -> imageView = binding.visualIndicatorLed2
            3 -> imageView = binding.visualIndicatorLed3
            4 -> imageView = binding.visualIndicatorLed4
            else -> imageView = binding.visualIndicatorLed4
        }
        if (isLedON)
            drawable = R.drawable.circle_primary
        else
            drawable = R.drawable.circle_grey
        imageView?.setBackgroundResource(drawable)
    }

    private fun playAudioIndication(isSuccessTone: Boolean) {
        try {
            this@VoidActivity.runOnUiThread {
                val mToneGenerator =
                    ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
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
            //AppLog.instance.error(AppLogKeys.PlayAudioError, "${e.javaClass.name} ${e.localizedMessage} ${e.stackTraceToString()}")
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
        var formatedTip = tip.replace(".","").replace(",","")

        return formatedTip.padStart(12, '0')
    }


}