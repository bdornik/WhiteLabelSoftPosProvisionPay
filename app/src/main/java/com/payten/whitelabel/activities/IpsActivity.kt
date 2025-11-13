package com.payten.whitelabel.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.CaptureManager
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.databinding.ActivityIpsBinding
import com.payten.whitelabel.databinding.DialogPinRegistrationBinding
import com.payten.whitelabel.dto.*
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.utils.IpsUtil
import com.payten.whitelabel.viewmodel.IPSShowQRViewModel
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class IpsActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityIpsBinding

    private lateinit var model: IPSShowQRViewModel
    private var timer: Timer? = null
    private var timemoutTimer: Timer? = null
    private var capture: CaptureManager? = null

    private var loaded = false
    private var isBackEnabled = true
    private var qr: QR? = null

    private var data = ""

    private var providedPackageName = ""
    private var formattedAmount = ""
    private var payDto: PayTransactionDto? = null;
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    @Inject
    lateinit var sharedPreferences: KsPrefs

    var amount = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIpsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        val tempModel: IPSShowQRViewModel by viewModels()
        model = tempModel

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundAmount)

        amount = intent.getStringExtra("Amount").toString()

        val showQR = sharedPreferences.pull(SharedPreferencesKeys.IPS_DEFAULT_PAYMENT_METHOD, "")
            .equals("P", true)

        if (intent.hasExtra("providedPackageName")) {
            providedPackageName = intent.getStringExtra("providedPackageName")!!
        }

        var sdkAmount = (amount!!.toFloat()).toLong()
        formattedAmount = AmountUtil.formatAmount(amount)

        binding.amountValue.text = "$formattedAmount"
        binding.suffix.text = SupercaseConfig.CURRENCY_STRING

        binding.back.setOnClickListener {
            if(isBackEnabled) {
                createTimeoutTimer()
                finish()
            }
        }

        if (showQR) {
            binding.btn.visibility = View.VISIBLE
            generateQRCode(sdkAmount)
            binding.btn.setOnClickListener {
                createTimeoutTimer()
                isBackEnabled = true
                finish()
            }
        }

        model.checkTransactionFailed.observe(this){
            val intent = Intent(this@IpsActivity, TransactionActivity::class.java)
            val transactionData = TransactionDetailsDto(
                aid = "",
                applicationLabel = "",
                authorizationCode = "",
                bankName = "",
                cardNumber = "",
                dateTime = LocalDateTime.now().toString(),
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                message = resources.getString(R.string.connection_timeout_message),
                operationName = resources.getString(R.string.label_operation_sales),
                response = "05",
                rrn = "",
                code = "",
                status = "",
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID),
                amount = format(amount),
                isIps = true,
                sdkStatus = null,
                billStatus = null,
                color = -1,
                recordId = "",
                listName = "",
                tipAmount = "0.0"

            )

            if (providedPackageName.isNotEmpty()){
                val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
                val obj = AppToAppResponseDto(
                    AppToAppSingleResponseDto(
                        AppToAppSingleResponseStatusDto(
                            "05", resources.getString(R.string.connection_timeout_message), transactionData.toString()
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
                setResult(Activity.RESULT_OK, intent)
            }else{
                intent.putExtra("data", transactionData)
                startActivity(intent)
            }
            finish()
        }

        model.payTransaction.observe(this){
            val intent = Intent(this@IpsActivity, TransactionActivity::class.java)
            val transactionData = TransactionDetailsDto(
                aid = "",
                applicationLabel = "",
                authorizationCode = "",
                bankName = "",
                cardNumber = "",
                dateTime = LocalDateTime.now().toString(),
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                message = it.statusCode,
                operationName = resources.getString(R.string.label_operation_sales),
                response = it.statusCode,
                rrn = it.creditTransferIdentificator,
                code = it.creditTransferIdentificator,
                status = it.statusCode,
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID),
                amount = format(amount),
                isIps = true,
                sdkStatus = null,
                billStatus = null,
                color = -1,
                recordId = "",
                listName = "",
                tipAmount = "0.0"

            )

            if (providedPackageName.isNotEmpty()){
                val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
                val obj = AppToAppResponseDto(
                    AppToAppSingleResponseDto(
                        AppToAppSingleResponseStatusDto(
                            it.statusCode, it.approvalCode, transactionData.toString()
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
                setResult(Activity.RESULT_OK, intent)
            }else{
                intent.putExtra("data", transactionData)
                startActivity(intent)
            }
            finish()

        }

        model.checkTransactionSuccessfull.observe(this) { _ ->
            run {
                successDialog(false, null)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()
        if (capture != null) {
            capture?.onResume()
        }
    }

    private fun handleDarkLightMode() {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.white)
            )
        } else {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.bigLabelBlack)
            )
        }
    }

    private fun format(input: String): String{
        return String.format("%.2f", input.toDouble() / 100)
    }

    private fun getQrCodeBitmap(qrCodeContent: String, firstColor: Int, secondColor: Int): Bitmap {
        val size = 256
        val hints = hashMapOf<EncodeHintType, Int>().also { it[EncodeHintType.MARGIN] = 1 }
        val bits = QRCodeWriter().encode(qrCodeContent, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    it.setPixel(
                        x,
                        y,
                        if (bits[x, y]) ContextCompat.getColor(
                            this,
                            firstColor
                        ) else ContextCompat.getColor(this, secondColor)
                    )
                }
            }
        }
    }


    private fun generateQRCode(amount: Long) {
        var counter = sharedPreferences.pull(SharedPreferencesKeys.COUNTER, 0)
        if (counter > 999999) {
            counter = 0
        }
        val newCounter = counter + 1
        sharedPreferences.push(SharedPreferencesKeys.COUNTER, newCounter)

        val payerAccNumber =
            sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_ACCOUNT_NUMBER, "")
        val merchantName =
            sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "")
        val merchantPlaceName =
            sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "")
        val merchantAddress =
            sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_ADDRESS, "")
        val mcc =
            sharedPreferences.pull(SharedPreferencesKeys.MCC, "")
        val terminalIdentificator =
            sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, "")
        val paymentCode =
            sharedPreferences.pull(SharedPreferencesKeys.PAYMENT_CODE, "")
        val tId =
            sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "")
        val merchantIdentification =
            sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID, "")

        val formattedDate = IpsUtil.getDate()

        val creditTransferIndicator = IpsUtil.createPaymentIdentificatorReference(terminalIdentificator, counter, formattedDate)

        var newAmount = formattedAmount.replace(".","").replace(",", ".")
//        if (newAmount.startsWith(",")) {
//            newAmount = "0${newAmount}"
//        }



        val amountAndCurrency = "${SupercaseConfig.CURRENCY_STRING}${this.amount}"
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
        val content = model.createQrCodeString(dto)

        model.check(creditTransferIndicator, terminalIdentificator, newAmount, content)

        createTimeoutTimer()

        logger.info { "QRCODE content: ${content}" }
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            binding.frameImage.setImageBitmap(
                getQrCodeBitmap(
                    content,
                    R.color.white,
                    R.color.globalBlack
                )
            )
        } else {
            binding.frameImage.setImageBitmap(
                getQrCodeBitmap(
                    content,
                    R.color.bigLabelBlack,
                    R.color.white
                )
            )
        }
    }

    fun stopTimeoutTimer() {
        if(timemoutTimer != null){
            logger.info { "Stopping timeout timer..." }
        }
        timemoutTimer?.cancel()
    }

    fun createTimeoutTimer() {
        stopTimeoutTimer()
        logger.info { "Starting timeout timer..." }
        timemoutTimer = Timer()
        timemoutTimer!!.schedule(object: TimerTask(){
            override fun run() {

                runOnUiThread {
                    val dialog = Dialog(this@IpsActivity)
                    val dialogBinding: DialogPinRegistrationBinding =
                        DialogPinRegistrationBinding.inflate(layoutInflater)
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                    dialogBinding.warningLabel.text =
                        this@IpsActivity.getString(R.string.label_transaction_timeout)
                    dialogBinding.btn.text = this@IpsActivity.getString(R.string.button_registration_back)

                    dialogBinding.btn.setOnClickListener {
                        dialog.dismiss()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                }
            }
        }, 210000 )
    }

    fun getJulianDate(d: Date?): String? {
        val day: String =
            formatDate(d, "DDD", TimeZone.getDefault())!!
        val year: String =
            formatDate(d, "yy", TimeZone.getDefault())!!
        return year + day
    }

    fun formatDate(d: Date?, pattern: String?, timeZone: TimeZone?): String? {
        val df = DateFormat.getDateTimeInstance() as SimpleDateFormat
        df.timeZone = timeZone
        df.applyPattern(pattern)
        return df.format(d)
    }

    fun padleft(s: String, len: Int, c: Char): String? {
        var s = s
        s = s.trim { it <= ' ' }
        if (s.length > len) {
            return s.substring(s.length - 6)
        }
        val d = StringBuilder(len)
        var fill = len - s.length
        while (fill-- > 0) d.append(c)
        d.append(s)
        return d.toString()
    }

    private fun createPaymentIdentificatorReference(tid: String, counter: Int): String {
        logger.info { "Creatin paymentIdentificator tid: $tid counter: $counter" }
        var paymentIdentificationReference = ""
        try {
            paymentIdentificationReference =
                tid + getJulianDate(Date()) + padleft(counter.toString(), 6, '0')
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return paymentIdentificationReference
    }

    private fun readQrCode(qrDecoded: String) {
        val qrCodeKeyValuePairs: Map<String, String> =
            model.createQrCodeKeyValueMapFromString(qrDecoded)!!
        if (model.containsRequiredTags(qrCodeKeyValuePairs)) {
            qr = model.createQRcodeObjectFromValues(qrCodeKeyValuePairs)
        } else {
            successDialog(true, null)
            return
        }
        if (qr != null && model.containsRequiredFieldValues(qr!!)) {
//            successDialog(false)
            // call api
        } else {
            successDialog(true, null)
            return
        }
    }

    private fun successDialog(failed: Boolean, error: String?) {
        this@IpsActivity.runOnUiThread {
            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            if (failed) {
                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text =
                    "${this.getString(R.string.label_transaction_failed)} ${this.getString(R.string.label_transaction_failed_reason)}: $error"
                dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
            } else {
                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_success)
                dialogBinding.warningLabel.text = this.getString(R.string.label_void_success)
                dialogBinding.btn.text = this.getString(R.string.button_registration_success)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                    setResult(Activity.RESULT_OK)

                    if (providedPackageName.isEmpty()) {
                        finish()
                    } else {
                        val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
                        val obj = AppToAppResponseDto(
                            AppToAppSingleResponseDto(
                                AppToAppSingleResponseStatusDto("00", "Placanje uspesno",data), ""
                            )
                        )
                        val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
                        logger.info("Sending to packagename: ${providedPackageName} dto: ${obj}")
                        if (intent != null) {
                            Toast.makeText(
                                applicationContext,
                                "Uspesno placanje",
                                Toast.LENGTH_LONG
                            ).show()
                            intent.putExtra("RESPONSE_JSON_STRING", gson.toJson(obj))
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Prosledjen packageName nije validan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
            }
        }
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

    override fun onBackPressed() {
        if(isBackEnabled) {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        capture?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        stopTimeoutTimer()
        capture?.onDestroy()
    }
}