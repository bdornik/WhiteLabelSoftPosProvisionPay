package com.payten.whitelabel.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.databinding.ActivityTransactionBinding
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.dto.slip.Slip
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsRequest
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.utils.Utility
import com.payten.whitelabel.utils.printer.PrintUtil
import com.payten.whitelabel.viewmodel.TransactionViewModel
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityTransactionBinding

    lateinit var transactionModel: TransactionViewModel


    var shareText = "";
    var tip = ""

    private var selectedDevice: BluetoothConnection? = null;

    lateinit var activity: Activity

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model: TransactionViewModel by viewModels()
        transactionModel = model
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        activity = this



        binding.billLayout.post {
            // Start bill hidden inside "grayLayout"
            binding.billLayout.translationY = -binding.billLayout.height.toFloat()  // hidden above slot

            // Animate it sliding down (like printing)
            binding.billLayout.animate()
                .translationY(0f)   // slide to fully visible
                .setDuration(2000)  // printer speed
                .setInterpolator(LinearInterpolator())
                .start()
        }


        val data: TransactionDetailsDto =
            intent.getSerializableExtra("data") as TransactionDetailsDto
        val fromAdapter = intent.getBooleanExtra("fromAdapter", false)

        if (data.applicationLabel?.contains("visa", true) == true) {
            binding.visa.visibility = View.VISIBLE
        } else if (data.applicationLabel?.contains("card", true) == true) {
            binding.mcard.visibility = View.VISIBLE
        }

        if (data.recordId != null)
            transactionModel.getTransactionDetailFromServer(
                GetTransactionDetailsRequest(
                    data.recordId.toString(), sharedPreferences.pull(
                        SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""
                    )
                )
            )

        transactionModel.transactionResultsSuccess.observe(this) {
            logger.info { "RecordId: ${data.recordId}" }

            logger.info { "TransactionDetails respionse: $it" }


            if (it?.additionalDataIn.toString().contains("installmentSelected")) {
                val startIndex = it?.additionalDataIn.toString()
                    .indexOf("\"installmentSelected\":") + "\"installmentSelected\":".length
                val endIndex = it?.additionalDataIn.toString().indexOf(",", startIndex)

                // Extract the substring and convert it to an integer
                val installmentSelectedString =
                    it?.additionalDataIn.toString().substring(startIndex, endIndex).trim()
                val installmentSelected = installmentSelectedString.toIntOrNull()
                logger.info { "INFO $installmentSelected" }

//                if (installmentSelected != null && installmentSelected > 0 && !it?.operationName.equals("Void")) {
//                    binding.installmentLabel.visibility = View.VISIBLE
//                    binding.installment.visibility = View.VISIBLE
//
//                    binding.line11.visibility = View.VISIBLE
//                    binding.installment.text = installmentSelected.toString()
//
//                    this.installment = installmentSelected.toString()
//
//                    //binding.line12.visibility = View.VISIBLE
//                    binding.installment.text = installmentSelected.toString()
//
//
//
//                }
            }

//            if(it?.additionalDataIn.toString().contains("uniqueId")){
//                val jsonObject = JSONObject(it?.additionalDataIn.toString())
//                val transactionRequest: JSONObject =
//                    jsonObject.getJSONObject("paytenTransactionRequest")
//                val uniqueId = transactionRequest.optString("uniqueId", "")
//
//                binding.labelUniqueId.visibility = View.VISIBLE
//                binding.uniqueId.visibility = View.VISIBLE
//                binding.line13.visibility = View.VISIBLE
//                binding.uniqueId.text = uniqueId
//
//                this.uniqueID = uniqueId
//            }

            if (it?.additionalDataIn.toString().contains("tipAmount")
                && !it?.additionalDataIn.toString().contains("\"tipAmount\":null")
            ) {
                val jsonObject = JSONObject(it?.additionalDataIn.toString())
                val transactionRequest: JSONObject =
                    jsonObject.getJSONObject("paytenTransactionRequest")
                val tipAmount = transactionRequest.optString("tipAmount", "000000000000")


                binding.labelTip.visibility = View.VISIBLE
                binding.tip.visibility = View.VISIBLE
                binding.line0.visibility = View.VISIBLE

                binding.labelAmount.visibility = View.VISIBLE
                binding.amount.visibility = View.VISIBLE
                binding.line01.visibility = View.VISIBLE

                binding.tip.text = AmountUtil.getAmount(reverseTipData(tipAmount))
                binding.amount.text = AmountUtil.getAmount(data.amount)

                binding.transactionAmount.text =
                    AmountUtil.getAmountWithOutCurr((reverseTipData(tipAmount).toDouble() + data.amount.toDouble()).toString())

                this.tip = "${reverseTipData(tipAmount)} ${SupercaseConfig.CURRENCY_STRING}"

            }

        }



        if (data.response.equals("00", true)) {
            binding.status.text =
                resources.getString(R.string.transaction_status_accepted).toUpperCase()
            binding.status.setTextColor(ContextCompat.getColor(this, R.color.globalGreen))
        } else if (data.response.equals("06", true) && data.isIps) {
            binding.status.text =
                resources.getString(R.string.transaction_status_canceled).toUpperCase()
            binding.status.setTextColor(ContextCompat.getColor(this, R.color.transaction_red))
        } else {
            binding.status.text =
                resources.getString(R.string.transaction_status_rejected).toUpperCase()
            binding.status.setTextColor(ContextCompat.getColor(this, R.color.transaction_red))
        }

        if (data.listName != "")
            binding.status.text = data.listName
        if (data.color != -1)
            binding.status.setTextColor(ContextCompat.getColor(this, data.color))

        var formattedAmount = ""

        if (data.isIps) {
            binding.labelE2E.text = getString(R.string.label_transaction_e2e_ips)
            binding.labelE2E.visibility = View.VISIBLE
            binding.e2e.text = data.rrn
            binding.e2e.visibility = View.VISIBLE
            binding.line1.visibility = View.VISIBLE


            binding.labelCardNumber.visibility = View.GONE
            binding.cardNumber.visibility = View.GONE
            binding.line6.visibility = View.GONE

            binding.labelAuthorizationCode.visibility = View.GONE
            binding.authorizationCode.visibility = View.GONE
            binding.line7.visibility = View.GONE

            binding.labelResponse.visibility = View.GONE
            binding.response.visibility = View.GONE
            binding.line9.visibility = View.GONE

            binding.aidLabel.visibility = View.GONE
            binding.aid.visibility = View.GONE
            binding.line9.visibility = View.GONE
            binding.line10.visibility = View.GONE

            formattedAmount = AmountUtil.getAmountNoCurr(data.amount.replace(",","."))


        }else{
            formattedAmount = AmountUtil.getAmountNoCurr(data.amount)
        }
        binding.merchantId.text = data.merchantId
        binding.terminalId.text = data.terminalId
        binding.merchantName.text = data.merchantName
        binding.cardNumber.text = data.cardNumber
        binding.authorizationCode.text = data.authorizationCode
        binding.operationName.text = data.operationName
        binding.response.text = data.response
        binding.message.text = data.message
        if (data.aid == null || data.aid.equals(""))
            binding.aidLabel.visibility = View.GONE
        binding.aid.text = data.aid






        binding.transactionAmount.text = "${formattedAmount}"

        if (intent.hasExtra("tip")) {
            binding.labelTip.visibility = View.VISIBLE
            binding.tip.visibility = View.VISIBLE
            binding.line0.visibility = View.VISIBLE

            binding.labelAmount.visibility = View.VISIBLE
            binding.amount.visibility = View.VISIBLE
            binding.line01.visibility = View.VISIBLE

            binding.tip.text = intent.getStringExtra("tip") + " " + SupercaseConfig.CURRENCY_STRING
            binding.amount.text = AmountUtil.getAmount(data.amount)

            binding.transactionAmount.text =
                intent.getStringExtra("totalAmount")

        }
        val slovenian = Locale.forLanguageTag(Utility.getLanguage(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0)))

        val dF = DateTimeFormatter.ofPattern("dd.MMMM yyyy",slovenian)
        var tF = DateTimeFormatter.ofPattern("HH:mm")

        val dateTime = LocalDateTime.parse(data.dateTime)
        binding.transactionDate.text = "${dateTime.format(dF)}"
        binding.transactionTime.text = "${dateTime.format(tF)}"

        binding.back.setOnClickListener {
            if (fromAdapter) {
                finish()
            } else {
                val intent = Intent(applicationContext, LandingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        binding.share.setOnClickListener {
            binding.share.setOnClickListener {
                if (data.isIps) {
                    shareText =
                        "${resources.getString(R.string.hint_amount)}: ${binding.transactionAmount.text}\n" +
                                "${resources.getString(R.string.share_date)}: ${binding.transactionDate.text}\n" +
                                "${resources.getString(R.string.label_transaction_status)} ${binding.status.text}\n" +
                                "${resources.getString(R.string.label_transaction_e2e_ips)} ${binding.e2e.text}\n" +
                                "${resources.getString(R.string.label_transaction_merchant_id)} ${binding.merchantId.text}\n" +
                                "${resources.getString(R.string.label_transaction_terminal_id)} ${binding.terminalId.text}\n" +
                                "${resources.getString(R.string.label_transaction_merchant_name)} ${binding.merchantName.text}\n" +
                                "${resources.getString(R.string.label_transaction_operation_name)} ${binding.operationName.text}\n" +
                                "${resources.getString(R.string.label_transaction_message)} ${binding.message.text}\n"
//            } else {
//                shareText =
//                    "${resources.getString(R.string.hint_amount)}: ${binding.transactionAmount.text}\n" +
//                            "${resources.getString(R.string.share_date)} ${binding.transactionDate.text}\n" +
//                            "${resources.getString(R.string.label_transaction_status)} ${binding.status.text}\n" +
//                            "${resources.getString(R.string.label_transaction_merchant_id)} ${binding.merchantId.text}\n" +
//                            "${resources.getString(R.string.label_transaction_terminal_id)} ${binding.terminalId.text}\n" +
//                            "${resources.getString(R.string.label_transaction_merchant_name)} ${binding.merchantName.text}\n" +
//                            "${resources.getString(R.string.label_transaction_card_number)} ${binding.cardNumber.text}\n" +
//                            "${resources.getString(R.string.label_transaction_authorization_code)} ${binding.authorizationCode.text}\n" +
//                            "${resources.getString(R.string.label_transaction_operation_name)} ${binding.operationName.text}\n" +
//                            "${resources.getString(R.string.label_transaction_response)} ${binding.response.text}\n" +
//                            "${resources.getString(R.string.label_transaction_message)} ${binding.message.text}\n"
//            }
//
//            if (data.aid != null && data.aid.isNotEmpty())
//                shareText =
//                    "${shareText.trim()}\n${resources.getString(R.string.label_transaction_aid)} ${binding.aid.text}\n"
//
//            if (intent.hasExtra("installmentNumber"))
//                shareText =
//                    "${shareText.trim()}\n${resources.getString(R.string.installment_number)} ${binding.installment.text}\n"
//
//


                } else {
                    var cardType = "VISA"

                    if (data.applicationLabel?.contains("card", true) == true)
                        cardType = "MASTERCARD"

                    val slip = Slip(
                        binding.status.text as String,
                        binding.transactionAmount.text as String,
                        binding.amount.text as String,
                        binding.tip.text as String,
                        binding.transactionDate.text as String,
                        binding.merchantId.text as String,
                        binding.terminalId.text as String,
                        binding.merchantName.text as String,
                        binding.cardNumber.text as String,
                        binding.authorizationCode.text as String,
                        binding.operationName.text as String,
                        binding.response.text as String,
                        binding.message.text as String,
                        "",
                        "",
                        cardType,
                        resources
                    )

                    shareText = slip.toString()
                }


                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(shareText)
                    .setType("text/plain")
                    .setChooserTitle(resources.getString(R.string.share_title))
                    .startChooser();
            }



            binding.print.setOnClickListener {
                var holder = "MASTERCARD"
                if (data.applicationLabel?.contains("visa", true) == true)
                    holder = "VISA"


                shareText =
                    """
                    [L]
                    [C]================================
                    [L]
                    [C]${resources.getString(R.string.hint_amount)}: ${binding.transactionAmount.text}
                    [L]
                    [L]${resources.getString(R.string.share_date)} ${binding.transactionDate.text}
                    [L]${resources.getString(R.string.label_transaction_status)} ${binding.status.text}
                    [L]${resources.getString(R.string.label_transaction_merchant_id)} ${binding.merchantId.text}
                    [L]${resources.getString(R.string.label_transaction_terminal_id)} ${binding.terminalId.text}
                    [L]${resources.getString(R.string.label_transaction_merchant_name)} ${binding.merchantName.text}
                    [L]${resources.getString(R.string.label_transaction_card_number)} ${binding.cardNumber.text}
                    [L]${resources.getString(R.string.label_transaction_authorization_code)} ${binding.authorizationCode.text}
                    [L]${resources.getString(R.string.label_transaction_operation_name)} ${binding.operationName.text}
                    [L]${resources.getString(R.string.label_transaction_response)} ${binding.response.text}
                    [L]${resources.getString(R.string.label_transaction_message)} ${binding.message.text}
                    [L]
                    [C]<b>${holder}</b>
                    [L]
                    [C]================================
               """.trimIndent()
                shareText = Utility.convertSerbianToEnglish(shareText)

                logger.info { shareText }
                PrintUtil.printBluetooth(this, selectedDevice, shareText)

            }
        }


        binding.print.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(view: View): Boolean {
                PrintUtil.browseBluetoothDevice(activity, selectedDevice)
                return true
            }
        })

    }

    override fun onBackPressed() {
        val intent = Intent(applicationContext, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
    }


    fun reverseTipData(formattedTip: String): String {
        // Remove leading zeros
        val numericValue = formattedTip.trimStart('0')

        // Handle empty or zero values
        if (numericValue.isEmpty()) return "0.00"

        // Insert a decimal point two places from the end
        val result = if (numericValue.length > 2) {
            numericValue.substring(0, numericValue.length - 2) + "." + numericValue.substring(
                numericValue.length - 2
            )
        } else {
            "0." + numericValue.padStart(2, '0')
        }

        return result
    }
}