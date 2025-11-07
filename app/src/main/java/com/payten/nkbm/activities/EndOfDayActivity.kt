package com.payten.nkbm.activities

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.payten.nkbm.R
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.databinding.ActivityEndOfDayBinding
import com.payten.nkbm.databinding.DialogQuestionBinding
import com.payten.nkbm.dto.CardTraffic
import com.payten.nkbm.dto.CardTrafficPrint
import com.payten.nkbm.dto.EndOfDay
import com.payten.nkbm.dto.transactions.GetTransactionsRequest
import com.payten.nkbm.enums.ErrorDescription
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.AmountUtil
import com.payten.nkbm.utils.Utility
import com.payten.nkbm.utils.printer.PrintUtil
import com.payten.nkbm.viewmodel.EndOfDayViewModel
import com.payten.nkbm.viewmodel.TrafficViewModel
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class EndOfDayActivity : BaseActivity() {
    private val TAG = "EndOfDayActivity"

    private val logger = KotlinLogging.logger {}


    private lateinit var binding: ActivityEndOfDayBinding

    var shareText = "";
    var cpclText = "";

    var endOfDayDone = false

    private var selectedDevice: BluetoothConnection? = null;

    lateinit var getTransactionsData: GetTransactionsRequest


    val endOfDay = EndOfDay()
    var masterTraffic = CardTrafficPrint()
    var visaTraffic = CardTrafficPrint()
    var flikTraffic = CardTrafficPrint()
    var totalTraffic = CardTrafficPrint()

    @Inject
    lateinit var sharedPreferences: KsPrefs

    lateinit var trafficModel: TrafficViewModel
    lateinit var endOfDayModel: EndOfDayViewModel

    var activity: Activity = this

    var sharedPrinted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityEndOfDayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)


        val model: TrafficViewModel by viewModels()
        trafficModel = model

        val model2: EndOfDayViewModel by viewModels()
        endOfDayModel = model2

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



        binding.back.setOnClickListener {
            if (!sharedPrinted)
                showDialogQuestion()
            else
                finish()
        }

        if (!sharedPreferences.pull(SharedPreferencesKeys.TIPS, false)) {
            binding.labelTipMastercard.visibility = View.GONE
            binding.tipMastercard.visibility = View.GONE

            binding.labelTipVisa.visibility = View.GONE
            binding.tipVisa.visibility = View.GONE

            binding.labelTipTotal.visibility = View.GONE
            binding.tipTotal.visibility = View.GONE
        }

        if (!sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS,false)){
            binding.line9.visibility = View.GONE
            binding.labelFlik.visibility = View.GONE
            binding.empty3.visibility = View.GONE
            binding.labelTotalFlik.visibility = View.GONE
            binding.totalNumberFlik.visibility = View.GONE
            binding.totalFlik.visibility = View.GONE
        }


        endOfDay.merchantName =
            sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "")
        endOfDay.TID = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, "")
        endOfDay.MID = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID, "")

        endOfDay.lastEndOfDay = sharedPreferences.pull(SharedPreferencesKeys.END_OF_DAY_DATE, "")


        getTransactionData()



        Log.i(TAG, "endOfDay started value set")


        binding.endOfDayButton.setOnClickListener {
            if (!endOfDayDone) {
                endOfDayDone = true

                var newEndOfDay = getCurrentDateTime()

                sharedPreferences.push(SharedPreferencesKeys.END_OF_DAY_DATE, newEndOfDay)
                endOfDay.lastEndOfDay = newEndOfDay
                binding.lastEndOfDay.text =
                    "${convertDateFormatForPrint(endOfDay.lastEndOfDay.toString())} ${
                        convertTimeFormatForPrint(endOfDay.lastEndOfDay.toString())
                    }"
            }

            Utility.showDialogInfo(
                this,
                resources.getString(R.string.label_end_of_day_done),
                true,
                sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)
            )
        }

        trafficModel.ipsTransactionResultsSuccess.observe(this) {
            val ipsTraffic = CardTraffic("flik")


            for (transaction in it.data) {
                logger.info { "IPS trnx: " + transaction }

                if (transaction.statusCode.equals("00")) {
                    ipsTraffic.purchaseNumber = ipsTraffic.purchaseNumber?.plus(1)
                    ipsTraffic.purchase = ipsTraffic.purchase?.plus(transaction.amount.toDouble())
                }
            }

            val updatedList = endOfDay.data?.toMutableList() ?: mutableListOf()
            updatedList.add(ipsTraffic)
            endOfDay.data = updatedList

            populateData()

        }


        trafficModel.transactionResultsSuccess.observe(this) {
            val masterTraffic = CardTraffic("mastercard")
            val visaTraffic = CardTraffic("visa")

            if (it == null) {
                endOfDay.data = listOf(masterTraffic, visaTraffic)

                Log.i(TAG, "TEST: " + endOfDay.data)
                populateData()
                return@observe
            }

            Log.i(TAG, it.toString())



            for (transaction in it) {
                //Log.i(TAG, "TRANSACTIONS: " + transaction.toString())
                transaction.amount =
                    (Math.round(transaction.amount.toDouble()!! * 100) / 100.0).toString()
                Log.i(
                    TAG,
                    "TRANSACTION: ${transaction.applicationLabel}, ${transaction.responseCode}, ${transaction.operationName}, ${transaction.statusCode}, ${transaction.amount}"
                )

                if (isVisa(transaction.applicationId.toString()) && transaction.responseCode.equals(
                        "00"
                    )
                ) {
//                    if ((transaction.operationName.equals("Sale") || transaction.operationName.equals(
//                            "Sale with PIN"
//                        ) || transaction.operationName.equals("Sale with Tip"))
                    if ((transaction.operationName!!.contains("Sale") && !transaction.operationName!!.contains(
                            "Void"
                        ))
                        && (transaction.statusCode.equals(
                            "a",
                            true
                        ) || transaction.statusCode.equals("v", true))
                    ) {
                        var result = visaTraffic.purchase?.plus(transaction.amount.toDouble()!!)
                        result = result?.plus(transaction.tipAmount.toDouble()!!)
                        val tip = visaTraffic.tipAmount?.plus(transaction.tipAmount.toDouble()!!)

                        visaTraffic.tipAmount = roundOffDecimal(tip!!)
                        visaTraffic.purchase = roundOffDecimal(result!!)
                        visaTraffic.purchaseNumber = visaTraffic.purchaseNumber?.plus(1)
                    }
                    if (transaction.operationName!!.contains("Void") && transaction.statusCode.equals(
                            "v",
                            true
                        )
                    ) {

                        var result =
                            visaTraffic.cancelPurchase?.plus(transaction.amount.toDouble()!!)
                        result = result?.plus(transaction.tipAmount.toDouble()!!)

                        visaTraffic.tipAmount =
                            roundOffDecimal(visaTraffic.tipAmount?.minus(transaction.tipAmount.toDouble()!!)!!)
                        visaTraffic.cancelPurchase = roundOffDecimal(result!!)
                        visaTraffic.cancelPurchaseNumber = visaTraffic.cancelPurchaseNumber?.plus(1)
                    }

                }
                if (isMaster(transaction.applicationId.toString()) && transaction.responseCode.equals(
                        "00"
                    )
                ) {
                    //PRODUKCIJA BACKEND KADA SE IZMENI (turski) OVO ODKOMENTARISATI
                    //if (transaction.responseCode.equals("00")) { //ovo izbrisati
//                    if ((transaction.operationName.equals("Sale") || transaction.operationName.equals(
//                            "Sale with PIN"
//                        ) || transaction.operationName.equals("Sale with Tip"))
                    if ((transaction.operationName!!.contains("Sale") && !transaction.operationName!!.contains(
                            "Void"
                        ))
                        && (transaction.statusCode.equals(
                            "a",
                            true
                        ) || transaction.statusCode.equals("v", true))
                    ) {
                        var result = masterTraffic.purchase?.plus(transaction.amount.toDouble()!!)
                        result = result?.plus(transaction.tipAmount.toDouble()!!)

                        val tip = masterTraffic.tipAmount?.plus(transaction.tipAmount.toDouble()!!)

                        masterTraffic.tipAmount = roundOffDecimal(tip!!)
                        masterTraffic.purchase = roundOffDecimal(result!!)
                        masterTraffic.purchaseNumber = masterTraffic.purchaseNumber?.plus(1)
                    }
                    if (transaction.operationName!!.contains("Void") && transaction.statusCode.equals(
                            "v",
                            true
                        )
                    ) {
                        var result =
                            masterTraffic.cancelPurchase?.plus(transaction.amount.toDouble()!!)
                        result = result?.plus(transaction.tipAmount.toDouble()!!)


                        masterTraffic.tipAmount =
                            roundOffDecimal(masterTraffic.tipAmount?.minus(transaction.tipAmount.toDouble()!!)!!)
                        masterTraffic.cancelPurchase = roundOffDecimal(result!!)
                        masterTraffic.cancelPurchaseNumber =
                            masterTraffic.cancelPurchaseNumber?.plus(1)
                    }
                }
            }

            endOfDay.data = listOf(masterTraffic, visaTraffic)

            Log.i(TAG, "TEST: " + endOfDay.data)


            if (sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS, false))
                trafficModel.getIpsTransactions(
                    getTransactionsData.userId,
                    getTransactionsData.dateFrom,
                    getTransactionsData.dateTo,
                    sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID)
                )
            else {
                populateData()
            }

        }

        binding.share.setOnClickListener {
            if (!endOfDayDone) {
                Utility.showDialogInfo(
                    this,
                    resources.getString(R.string.label_end_of_day_must_be_done),
                    false,
                    sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)
                )
                return@setOnClickListener
            } else {
                sharedPrinted = true


                createTextForShare()
                //shareText = Utility.convertSerbianToEnglish(shareText)

                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(shareText)
                    .setType("text/plain")
                    .setChooserTitle(resources.getString(R.string.share_title))
                    .startChooser();

            }
        }

        binding.print.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(view: View): Boolean {
                selectedDevice = PrintUtil.browseBluetoothDevice(activity, selectedDevice)
                return true
            }
        })

        binding.print.setOnClickListener {

            if (!endOfDayDone) {
                Utility.showDialogInfo(
                    this,
                    resources.getString(R.string.label_end_of_day_must_be_done),
                    false,
                    sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)
                )
                return@setOnClickListener
            }

            sharedPrinted = true
            shareText = createTextForPrint()

            shareText = Utility.convertSerbianToEnglish(shareText)

            logger.info { shareText }
            PrintUtil.printBluetooth(this, selectedDevice, shareText)

        }

    }

    private fun isVisa(applicationID: String): Boolean {
        return applicationID == "A0000000031010" || applicationID == "A0000000032010"
    }

    private fun isMaster(applicationID: String): Boolean {
        return applicationID == "A0000000041010" || applicationID == "A0000000043060"
    }

    private fun getTransactionData() {
        var tF = DateTimeFormatter.ofPattern("HH:mm:ss")
        var dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        var localDateFrom = LocalDate.now().atStartOfDay().minus(90, ChronoUnit.DAYS)
        var localDateTo =
            LocalDate.now().atStartOfDay().plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS)

        Log.i(TAG, "FROM NOW: ${localDateFrom.format(dF)}${localDateFrom.format(tF)}")
        Log.i(
            TAG,
            "FROM NEW: ${
                sharedPreferences.pull(
                    SharedPreferencesKeys.END_OF_DAY_DATE,
                    localDateFrom
                )
            }"
        )

        //var getTransactionsData = GetTransactionsInputData()

        var dateFrom = sharedPreferences.pull(SharedPreferencesKeys.END_OF_DAY_DATE, "")

        if (dateFrom.isEmpty())
            dateFrom = "${localDateFrom.format(dF)}T${localDateFrom.format(tF)}"


        getTransactionsData = GetTransactionsRequest(
            sharedPreferences.pull(SharedPreferencesKeys.USER_ID),
            dateFrom,
            "${localDateTo.format(dF)}T${localDateTo.format(tF)}",
            sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID)
        )




        trafficModel.getTransactionsFromServer(getTransactionsData)
        Log.i(TAG, "trafficModel called")
    }

    private fun trimForPrint(text: String): String {
        var retrunText = text
        if (retrunText.length > 12) {
            retrunText = retrunText.substring(0, 12) + ".:"
        } else {
            val addSpace = 12 - retrunText.length
            for (i in 0..addSpace) {
                retrunText = "$retrunText "
            }
        }

        return retrunText;
    }

    fun populateData() {

        var totalTip = 0.00

        var totalPurchase = 0.00
        var totalPurchaseNumber = 0

        var totalCancelPurchase = 0.00
        var totalCancelPurchaseNumber = 0

        var totalNumber = 0;
        var totalTotal = 0.00;




        for (traffic in endOfDay.data!!) {
            Log.i(TAG, traffic.toString())
            if (traffic.type.equals("mastercard")) {

                //SVE ODKKOMENTARISTATI NAKON UPDATE BACKENDA
                binding.purchaseMastercardNumber.text = traffic.purchaseNumber.toString()
                binding.purchaseMastercard.text =
                    traffic.purchase?.let { prettyDecimal(roundOffDecimal(it!!)) }
                masterTraffic.type = resources.getString(R.string.label_mastercard)
                masterTraffic.purchaseNumber = traffic.purchaseNumber.toString()
                masterTraffic.purchase = traffic.purchase?.let { prettyDecimal(it) }

                binding.stornoNumberMastercard.text = traffic.cancelPurchaseNumber.toString()
                binding.stornoMastercard.text = traffic.cancelPurchase?.let { prettyDecimal(it) }
                masterTraffic.cancelPurchaseNumber = traffic.cancelPurchaseNumber.toString()
                masterTraffic.cancelPurchase = traffic.cancelPurchase?.let { prettyDecimal(it) }

                masterTraffic.returnNumber = traffic.returnNumber.toString()
                masterTraffic.returnPurchase = traffic.returnPurchase?.let { prettyDecimal(it) }
                masterTraffic.cancelReturnNumber = traffic.cancelReturnNumber.toString()
                masterTraffic.cancelReturnPurchase =
                    traffic.cancelReturnPurchase?.let { prettyDecimal(it) }

                binding.tipMastercard.text = traffic.tipAmount?.let { prettyDecimal(it) }
                masterTraffic.tipAmount =
                    AmountUtil.stringPretty(prettyDecimal(traffic.tipAmount!!))


                val totalMasterNumber =
                    (traffic.purchaseNumber?.plus(traffic.cancelPurchaseNumber!!)
                        ?.plus(traffic.returnNumber!!)
                        ?.plus(traffic.cancelReturnNumber!!))

                val totalMaster = roundOffDecimal(
                    (traffic.purchase
                        //?.plus(traffic.tipAmount!!)
                        ?.minus(traffic.cancelPurchase!!)
                        ?.minus(traffic.returnPurchase!!)
                        ?.plus(traffic.cancelReturnPurchase!!))!!
                )

                totalTip = roundOffDecimal(traffic.tipAmount?.let { totalTip.plus(it) }!!)

                totalPurchase = roundOffDecimal(traffic.purchase?.let { totalPurchase.plus(it) }!!)
                totalPurchaseNumber += traffic.purchaseNumber!!

                totalCancelPurchase = traffic.cancelPurchase?.let { totalCancelPurchase.plus(it) }!!
                totalCancelPurchaseNumber += traffic.cancelPurchaseNumber!!

                totalNumber += totalMasterNumber!!
                totalTotal = roundOffDecimal(totalMaster!!.plus(totalTotal))


                binding.totalNumberMastercard.text = totalMasterNumber.toString()
                binding.totalMastercard.text = prettyDecimal(totalMaster)
                masterTraffic.totalNumber = totalMasterNumber.toString()
                masterTraffic.total = prettyDecimal(totalMaster)

            }

            if (traffic.type.equals("visa")) {

                visaTraffic.type = resources.getString(R.string.label_visa)
                binding.purchaseNumberVisa.text = traffic.purchaseNumber.toString()
                binding.purchaseVisa.text = traffic.purchase?.let { prettyDecimal(it) }
                visaTraffic.purchaseNumber = traffic.purchaseNumber.toString()
                visaTraffic.purchase = traffic.purchase?.let { prettyDecimal(it) }

                binding.stornoNumberVisa.text = traffic.cancelPurchaseNumber.toString()
                binding.stornoVisa.text = traffic.cancelPurchase?.let { prettyDecimal(it) }
                visaTraffic.cancelPurchaseNumber = traffic.cancelPurchaseNumber.toString()
                visaTraffic.cancelPurchase = traffic.cancelPurchase?.let { prettyDecimal(it) }

                binding.tipVisa.text = traffic.tipAmount?.let { prettyDecimal(it) }
                visaTraffic.tipAmount = AmountUtil.stringPretty(prettyDecimal(traffic.tipAmount!!))


                visaTraffic.returnNumber = traffic.returnNumber.toString()
                visaTraffic.returnPurchase = traffic.returnPurchase?.let { prettyDecimal(it) }

                visaTraffic.cancelReturnNumber = traffic.cancelReturnNumber.toString()
                visaTraffic.cancelReturnPurchase =
                    traffic.cancelReturnPurchase?.let { prettyDecimal(it) }

                val totalVisaNumber = (traffic.purchaseNumber?.plus(traffic.cancelPurchaseNumber!!)
                    ?.plus(traffic.returnNumber!!)
                    ?.plus(traffic.cancelReturnNumber!!))

                val totalVisaTotal = roundOffDecimal(
                    (traffic.purchase
                        //?.plus(traffic.tipAmount!!)
                        ?.minus(traffic.cancelPurchase!!)
                        ?.minus(traffic.returnPurchase!!)
                        ?.plus(traffic.cancelReturnPurchase!!))!!
                )
                Log.i(TAG, totalVisaTotal.toString())

                totalTip = roundOffDecimal(traffic.tipAmount?.let { totalTip.plus(it) }!!)

                totalPurchase = roundOffDecimal(traffic.purchase?.let { totalPurchase.plus(it) }!!)
                totalPurchaseNumber += traffic.purchaseNumber!!

                totalCancelPurchase = traffic.cancelPurchase?.let { totalCancelPurchase.plus(it) }!!
                totalCancelPurchaseNumber += traffic.cancelPurchaseNumber!!

                totalNumber += totalVisaNumber!!
                totalTotal = roundOffDecimal(totalVisaTotal!!.plus(totalTotal))



                binding.totalNumberVisa.text = totalVisaNumber.toString()
                binding.totalVisa.text = prettyDecimal(totalVisaTotal.toDouble())
                visaTraffic.totalNumber = totalVisaNumber.toString()
                visaTraffic.total = prettyDecimal(totalVisaTotal.toDouble())

            }

            if (traffic.type.equals("flik")) {
                flikTraffic.type = resources.getString(R.string.label_flik)


                val totalFlikNumber =
                    (traffic.purchaseNumber?.plus(traffic.cancelPurchaseNumber!!)
                        ?.plus(traffic.returnNumber!!)
                        ?.plus(traffic.cancelReturnNumber!!))

                val totalFlik = roundOffDecimal(
                    (traffic.purchase
                        //?.plus(traffic.tipAmount!!)
                        ?.minus(traffic.cancelPurchase!!)
                        ?.minus(traffic.returnPurchase!!)
                        ?.plus(traffic.cancelReturnPurchase!!))!!
                )


                binding.totalNumberFlik.text = totalFlikNumber.toString()
                binding.totalFlik.text = prettyDecimal(totalFlik)
                flikTraffic.totalNumber = totalFlikNumber.toString()
                flikTraffic.total = prettyDecimal(totalFlik)


                totalPurchase = roundOffDecimal(traffic.purchase?.let { totalPurchase.plus(it) }!!)
                totalPurchaseNumber += traffic.purchaseNumber!!


                totalNumber += totalFlikNumber!!
                totalTotal = roundOffDecimal(totalFlik!!.plus(totalTotal))

            }
            //PRODUKCIJA KADA SE IZMENI OVO ODKOMENTARISATI

        }
        binding.merchantName.text = endOfDay.merchantName
        binding.terminalId.text = endOfDay.TID
        binding.merchantId.text = endOfDay.MID
        binding.lastEndOfDay.text =
            "${convertDateFormatForPrint(endOfDay.lastEndOfDay.toString())} ${
                convertTimeFormatForPrint(endOfDay.lastEndOfDay.toString())
            }"


        totalTraffic.type = resources.getString(R.string.label_total)
        binding.tipTotal.text = AmountUtil.stringPretty(prettyDecimal(totalTip))
        totalTraffic.tipAmount = AmountUtil.stringPretty(prettyDecimal(totalTip))

        binding.purchaseNumberTotal.text = totalPurchaseNumber.toString()
        binding.purchaseTotal.text = AmountUtil.stringPretty(prettyDecimal(totalPurchase))
        totalTraffic.purchaseNumber = totalPurchaseNumber.toString()
        totalTraffic.purchase = AmountUtil.stringPretty(prettyDecimal(totalPurchase))

        binding.stornoNumberTotal.text = totalCancelPurchaseNumber.toString()
        binding.stornoTotal.text = AmountUtil.stringPretty(prettyDecimal(totalCancelPurchase))
        totalTraffic.cancelPurchaseNumber = totalCancelPurchaseNumber.toString()
        totalTraffic.cancelPurchase = AmountUtil.stringPretty(prettyDecimal(totalCancelPurchase))

        binding.totalNumberTotal.text = totalNumber.toString()
        binding.totalTotal.text = AmountUtil.stringPretty(prettyDecimal(totalTotal))
        totalTraffic.totalNumber = totalNumber.toString()
        totalTraffic.total = AmountUtil.stringPretty(prettyDecimal(totalTotal))
    }

    fun roundOffDecimal(number: Double): Double {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)

        val df = DecimalFormat("#.##", symbols)
        df.roundingMode = java.math.RoundingMode.HALF_UP
        return df.format(number).toDouble()
    }

    private fun prettyDecimal(decimal: Double): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val df = DecimalFormat("###,###,###.00", symbols)
        df.roundingMode = RoundingMode.DOWN
        val roundoff = df.format(decimal)

        //var amount = String.format("%.2f", decimal).replace(".", "")
        var amount = roundoff.replace(".", "").replace(",", "")


        if (decimal == 0.0) {
            return "0,00 ${SupercaseConfig.CURRENCY_STRING}"
        }
        var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
        val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"

        if (decimal < 1 && decimal > 0) {
            amount = "0,$postDotsString"
        } else if (decimal < 0 && decimal > -1) {
            amount = "-0,$postDotsString"
        } else {
            amount = "${
                preDotsString.reversed()
                    .chunked(3)
                    .joinToString(".")
                    .reversed()
            },$postDotsString"
        }

        return "$amount ${SupercaseConfig.CURRENCY_STRING}"

    }

    fun showDialogQuestion() {


        val dialog = Dialog(this)
        val dialogBinding: DialogQuestionBinding =
            DialogQuestionBinding.inflate(layoutInflater)

        handleDialogQuestionDarkModeInfo(dialogBinding)
        dialog.getWindow()!!.setBackgroundDrawableResource(R.color.transparent)
        dialogBinding.icon.setImageResource(R.drawable.icon_warning)

        if (!endOfDayDone) {
            dialogBinding.warningLabel.text =
                resources.getString(R.string.label_end_of_day_question_first)
        } else {
            dialogBinding.warningLabel.text =
                resources.getString(R.string.label_end_of_day_question)

        }

        dialogBinding.warningLabelQuestion.text = resources.getString(R.string.label_are_you_shure)

        dialogBinding.btnYes.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialogBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }


    private fun handleDialogQuestionDarkModeInfo(dialogBinding: DialogQuestionBinding) {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            dialogBinding.root.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
            dialogBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
            dialogBinding.btnNo.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
        }
    }


    private fun createTextForShare() {
        shareText = buildString {
            appendLine("==============================")
            appendLine()
            appendLine(resources.getString(R.string.label_end_of_day).uppercase())
            appendLine()
            appendLine("${trimForPrint(resources.getString(R.string.label_transaction_merchant_name))}      ${endOfDay.merchantName}")
            appendLine("${trimForPrint(resources.getString(R.string.label_terminal_id))}        ${endOfDay.TID}")
            appendLine("${trimForPrint(resources.getString(R.string.label_transaction_merchant_id))}        ${endOfDay.MID}")
            appendLine("${trimForPrint(resources.getString(R.string.label_transaction_date))}       ${convertDateFormatForPrint(endOfDay.lastEndOfDay.toString())}")
            appendLine("${trimForPrint(resources.getString(R.string.label_time))}       ${convertTimeFormatForPrint(endOfDay.lastEndOfDay.toString())}")
            appendLine()
            appendLine("==============================")
            appendLine(resources.getString(R.string.label_mastercard).uppercase())
            appendLine()
            appendLine("${trimForPrint(resources.getString(R.string.label_purchase))}   ${masterTraffic.purchaseNumber}     ${masterTraffic.purchase}")
            appendLine("${trimForPrint(resources.getString(R.string.label_cancel_purchase))}    ${masterTraffic.cancelPurchaseNumber}   ${masterTraffic.cancelPurchase}")
            if (sharedPreferences.pull(SharedPreferencesKeys.TIPS,false)) {
                appendLine("${trimForPrint(resources.getString(R.string.label_tip))}       ${masterTraffic.tipAmount}")
            }
            appendLine()

            appendLine("${trimForPrint(resources.getString(R.string.label_total))}  ${masterTraffic.totalNumber}    ${masterTraffic.total}")
            appendLine("==============================")
            appendLine(resources.getString(R.string.label_visa).uppercase())
            appendLine()
            appendLine("${trimForPrint(resources.getString(R.string.label_purchase))}   ${visaTraffic.purchaseNumber}   ${visaTraffic.purchase}")
            appendLine("${trimForPrint(resources.getString(R.string.label_cancel_purchase))}    ${visaTraffic.cancelPurchaseNumber}    ${visaTraffic.cancelPurchase}")

            if (sharedPreferences.pull(SharedPreferencesKeys.TIPS,false)) {
                appendLine("${trimForPrint(resources.getString(R.string.label_tip))}        ${visaTraffic.tipAmount}")
            }
            appendLine()


            appendLine("${trimForPrint(resources.getString(R.string.label_total))}  ${visaTraffic.totalNumber}  ${visaTraffic.total}")

            if (sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS,false)) {
                appendLine("==============================")
                appendLine(resources.getString(R.string.label_flik).uppercase())
                appendLine()
                appendLine("${trimForPrint(resources.getString(R.string.label_total))}  ${flikTraffic.totalNumber}  ${flikTraffic.total}")
            }

            appendLine("==============================")
            appendLine(resources.getString(R.string.label_total_header))
            appendLine()
            appendLine("${trimForPrint(resources.getString(R.string.label_purchase))}   ${totalTraffic.purchaseNumber}  ${totalTraffic.purchase}")
            appendLine("${trimForPrint(resources.getString(R.string.label_cancel_purchase))}    ${totalTraffic.cancelPurchaseNumber}   ${totalTraffic.cancelPurchase}")
            if (sharedPreferences.pull(SharedPreferencesKeys.TIPS,false)) {
                appendLine("${trimForPrint(resources.getString(R.string.label_tip))}        ${totalTraffic.tipAmount}")
            }
            appendLine()
            appendLine("${trimForPrint(resources.getString(R.string.label_total))}  ${totalTraffic.totalNumber}     ${totalTraffic.total}")
            appendLine()
            appendLine("==============================")
            appendLine()
            appendLine(SupercaseConfig.INSTITUTION)
        }

    }

    private fun createTextForPrint(): String {
        val builder = StringBuilder()

        builder.appendLine("[L]")
        builder.appendLine("[C]================================")
        builder.appendLine("[L]")
        builder.appendLine(
            "[C]<u><font size='wide'>${
                resources.getString(R.string.label_end_of_day).uppercase()
            }</font></u>"
        )
        builder.appendLine("[L]")
        builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_transaction_merchant_name))}[R]${endOfDay.merchantName}")
        builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_terminal_id))}[R]${endOfDay.TID}")
        builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_transaction_merchant_id))}[R]${endOfDay.MID}")
        builder.appendLine(
            "[L]${trimForPrint(resources.getString(R.string.label_transaction_date))}[R]${
                convertDateFormatForPrint(
                    endOfDay.lastEndOfDay.toString()
                )
            }"
        )
        builder.appendLine(
            "[L]${trimForPrint(resources.getString(R.string.label_time))}[R]${
                convertTimeFormatForPrint(
                    endOfDay.lastEndOfDay.toString()
                )
            }"
        )
        builder.appendLine()
        builder.appendLine("[C]================================")

        fun appendTrafficSection(title: String, traffic: CardTrafficPrint) {

            builder.appendLine("[C]$title")
            builder.appendLine("[L]")

            if (!title.equals("FLIK")) {
                builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_purchase))} ${traffic.purchaseNumber}[R]${traffic.purchase}")
                builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_cancel_purchase))} ${traffic.cancelPurchaseNumber}[R]${traffic.cancelPurchase}")
                if (sharedPreferences.pull(SharedPreferencesKeys.TIPS, false)) {
                    builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_tip))} [R]${traffic.tipAmount}")
                }
            }

            builder.appendLine("[L]")
            builder.appendLine("[L]${trimForPrint(resources.getString(R.string.label_total))} ${traffic.totalNumber}[R]${traffic.total}")
            builder.appendLine("[C]================================")
        }

        // Add sections
        appendTrafficSection(
            resources.getString(R.string.label_mastercard).uppercase(),
            masterTraffic
        )

        if (sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS,false))
            appendTrafficSection(resources.getString(R.string.label_flik).uppercase(), flikTraffic)

        appendTrafficSection(resources.getString(R.string.label_visa).uppercase(), visaTraffic)
        appendTrafficSection(resources.getString(R.string.label_total_header), totalTraffic)

        builder.appendLine("[L]")
        builder.appendLine("[C]${SupercaseConfig.INSTITUTION}")

        val printData = builder.toString()

        return printData
    }

    override fun onBackPressed() {
        if (!sharedPrinted)
            showDialogQuestion()
        else
            super.onBackPressed()

    }

    private fun getCurrentDateTime(): String {
        var localDateFrom = LocalDateTime.now()
        var tF = DateTimeFormatter.ofPattern("HH:mm:ss")
        var dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")


        return "${localDateFrom.format(dF)}T${localDateFrom.format(tF)}"
    }

    private fun convertDateFormatForPrint(inputDate: String): String {
        try {
            var inputDateString = inputDate

            if (inputDate.isEmpty())
                return ""

            if (inputDate.length == 14)
                inputDateString = changeDateFromat(inputDate)

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            val date = inputFormat.parse(inputDateString)
            return outputFormat.format(date)
        } catch (e: Exception) {
            return inputDate
        }

    }


    private fun convertTimeFormatForPrint(inputDate: String): String {
        try {
            var inputDateString = inputDate
            if (inputDate.isEmpty())
                return ""

            if (inputDate.length == 14)
                inputDateString = changeDateFromat(inputDate)

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            val date = inputFormat.parse(inputDateString)
            return outputFormat.format(date)
        } catch (e: Exception) {
            return inputDate
        }
    }

    private fun changeDateFromat(date: String): String {
        logger.info("old date: " + date)
        val inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        // Parse the input date string
        val dateTime = LocalDateTime.parse(date, inputFormatter)

        // Format the parsed date-time into the desired format
        return dateTime.format(outputFormatter)

    }

    private fun logException(message: String?, error: String) {
        endOfDayModel.logError(
            endOfDayModel.createErrorLog(
                sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                sharedPreferences.pull(SharedPreferencesKeys.USER_ID),
                error,
                ErrorDescription.printing.name,
                this
            )
        )
    }

    // added new

    fun createShareText(): String {
        return """
                    [L]
                    [C]================================
                    [L]
                    [C]<u><font size='wide'>${
            resources.getString(R.string.label_end_of_day).uppercase()
        }</font></u>
                    [L]
                    [L]${trimForPrint(resources.getString(R.string.label_transaction_merchant_name))}[R]${endOfDay.merchantName}
                    [L]${trimForPrint(resources.getString(R.string.label_terminal_id))}[R]${endOfDay.TID}
                    [L]${trimForPrint(resources.getString(R.string.label_transaction_merchant_id))}[R]${endOfDay.MID}
                    [L]${trimForPrint(resources.getString(R.string.label_transaction_date))}[R]${
            convertDateFormatForPrint(
                endOfDay.lastEndOfDay.toString()
            )
        }
                    [L]${trimForPrint(resources.getString(R.string.label_time))}[R]${
            convertTimeFormatForPrint(
                endOfDay.lastEndOfDay.toString()
            )
        }
                    
                    [C]================================
                    [C]${resources.getString(R.string.label_total_header)}
                    [L]
                    [L]${trimForPrint(resources.getString(R.string.label_purchase))} ${totalTraffic.purchaseNumber}[R]${totalTraffic.purchase}
                    [L]${trimForPrint(resources.getString(R.string.label_cancel_purchase))} ${totalTraffic.cancelPurchaseNumber}[R]${totalTraffic.cancelPurchase}
                    [L]
                    [L]${trimForPrint(resources.getString(R.string.label_total))} ${totalTraffic.totalNumber}[R]${totalTraffic.total}
                    [L]
                    [C]================================
                    [L]
                    [C]${SupercaseConfig.INSTITUTION}
                """.trimIndent()
    }

    fun createCpclText(): String {
        return """
                    ! 0 200 200 1100 1
                    LEFT
                    TEXT 7 0 0 40 ================================
                    CENTER
                    TEXT 5 0 0 100 ${resources.getString(R.string.label_end_of_day).uppercase()}
                    LEFT
                    TEXT 7 0 0 160 ${trimForPrint(resources.getString(R.string.label_transaction_merchant_name))}
                    RIGHT
                    TEXT 7 0 0 160 ${endOfDay.merchantName}
                    LEFT
                    TEXT 7 0 0 190 ${trimForPrint(resources.getString(R.string.label_terminal_id))}
                    RIGHT
                    TEXT 7 0 0 190 ${endOfDay.TID}
                    LEFT
                    TEXT 7 0 0 220 ${trimForPrint(resources.getString(R.string.label_transaction_merchant_id))}
                    RIGHT
                    TEXT 7 0 0 220 ${endOfDay.MID}
                    LEFT
                    TEXT 7 0 0 250 ${trimForPrint(resources.getString(R.string.label_transaction_date))}
                    RIGHT
                    TEXT 7 0 0 250 ${convertDateFormatForPrint(endOfDay.lastEndOfDay.toString())}
                    LEFT
                    TEXT 7 0 0 280 ${trimForPrint(resources.getString(R.string.label_time))}
                    RIGHT
                    TEXT 7 0 0 280 ${convertTimeFormatForPrint(endOfDay.lastEndOfDay.toString())}
                    LEFT
                    TEXT 7 0 0 340 ================================
                    CENTER
                    TEXT 7 0 0 370 ${resources.getString(R.string.label_mastercard)}
                    LEFT
                    TEXT 7 0 0 430 ${trimForPrint(resources.getString(R.string.label_purchase))} ${masterTraffic.purchaseNumber}
                    RIGHT
                    TEXT 7 0 0 430 ${masterTraffic.purchase}
                    LEFT
                    TEXT 7 0 0 460 ${trimForPrint(resources.getString(R.string.label_cancel_purchase))} ${masterTraffic.cancelPurchaseNumber}
                    RIGHT
                    TEXT 7 0 0 460 ${masterTraffic.cancelPurchase}
                    LEFT
                    TEXT 7 0 0 510 ${trimForPrint(resources.getString(R.string.label_total))} ${masterTraffic.totalNumber}
                    RIGHT
                    TEXT 7 0 0 510 ${masterTraffic.total}
                    LEFT
                    TEXT 7 0 0 570 ================================
                    CENTER
                    TEXT 7 0 0 600 ${resources.getString(R.string.label_visa)}
                    LEFT
                    TEXT 7 0 0 660 ${trimForPrint(resources.getString(R.string.label_purchase))} ${visaTraffic.purchaseNumber}
                    RIGHT
                    TEXT 7 0 0 660 ${visaTraffic.purchase}
                    LEFT
                    TEXT 7 0 0 690 ${trimForPrint(resources.getString(R.string.label_cancel_purchase))} ${visaTraffic.cancelPurchaseNumber}
                    RIGHT
                    TEXT 7 0 0 690 ${visaTraffic.cancelPurchase}
                    LEFT
                    TEXT 7 0 0 740 ${trimForPrint(resources.getString(R.string.label_total))} ${visaTraffic.totalNumber}
                    RIGHT
                    TEXT 7 0 0 740 ${visaTraffic.total}
                    LEFT
                    TEXT 7 0 0 800 ================================
                    CENTER
                    TEXT 7 0 0 830 ${resources.getString(R.string.label_total_header)}
                    LEFT
                    TEXT 7 0 0 890 ${trimForPrint(resources.getString(R.string.label_purchase))} ${totalTraffic.purchaseNumber}
                    RIGHT
                    TEXT 7 0 0 890 ${totalTraffic.purchase}
                    LEFT
                    TEXT 7 0 0 920 ${trimForPrint(resources.getString(R.string.label_cancel_purchase))} ${totalTraffic.cancelPurchaseNumber}
                    RIGHT
                    TEXT 7 0 0 920 ${totalTraffic.cancelPurchase}
                    LEFT
                    TEXT 7 0 0 970 ${trimForPrint(resources.getString(R.string.label_total))} ${totalTraffic.totalNumber}
                    RIGHT
                    TEXT 7 0 0 970 ${totalTraffic.total}
                    LEFT
                    TEXT 7 0 0 1030 ================================
                    CENTER
                    TEXT 7 0 0 1080 ${SupercaseConfig.INSTITUTION}
                    FORM
                    PRINT

                """.trimIndent()

    }


    fun buildDynamicCpcl(endOfDay: EndOfDay, trafficList: List<CardTrafficPrint>): String {
        val sb = StringBuilder()
        var y = 40

        sb.appendLine("! 0 200 200 2000 1")
        sb.appendLine("LEFT")
        sb.appendLine("TEXT 7 0 0 $y ===============================")
        y += 40
        sb.appendLine("CENTER")
        sb.appendLine("TEXT 5 0 0 $y ${resources.getString(R.string.label_end_of_day).uppercase()}")
        y += 60

        // Header info
        val headerItems = listOf(
            Pair(
                resources.getString(R.string.label_transaction_merchant_name),
                endOfDay.merchantName
            ),
            Pair(resources.getString(R.string.label_terminal_id), endOfDay.TID),
            Pair(resources.getString(R.string.label_transaction_merchant_id), endOfDay.MID),
            Pair(
                resources.getString(R.string.label_transaction_date),
                convertDateFormatForPrint(endOfDay.lastEndOfDay.toString())
            ),
            Pair(
                resources.getString(R.string.label_time),
                convertTimeFormatForPrint(endOfDay.lastEndOfDay.toString())
            ),
        )
        headerItems.forEach { (label, value) ->
            sb.appendLine("LEFT")
            sb.appendLine("TEXT 7 0 0 $y ${trimForPrint(label)}")
            sb.appendLine("RIGHT")
            sb.appendLine("TEXT 7 0 0 $y $value")
            y += 30
        }

        sb.appendLine("LEFT")
        sb.appendLine("TEXT 7 0 0 $y ===============================")
        y += 40

        // Loop through card sections (like MasterCard, Visa)
        trafficList.forEach { traffic ->
            sb.appendLine("CENTER")
            sb.appendLine("TEXT 7 0 0 $y ${traffic.type?.uppercase()}")
            y += 40

            sb.appendLine("LEFT")
            sb.appendLine("TEXT 7 0 0 $y ${trimForPrint(resources.getString(R.string.label_purchase))} ${traffic.purchaseNumber}")
            sb.appendLine("RIGHT")
            sb.appendLine("TEXT 7 0 0 $y ${traffic.purchase}")
            y += 30

            sb.appendLine("LEFT")
            sb.appendLine("TEXT 7 0 0 $y ${trimForPrint(resources.getString(R.string.label_cancel_purchase))} ${traffic.cancelPurchaseNumber}")
            sb.appendLine("RIGHT")
            sb.appendLine("TEXT 7 0 0 $y ${traffic.cancelPurchase}")
            y += 50

            if (sharedPreferences.pull(SharedPreferencesKeys.TIPS)) {
                y -= 20
                sb.appendLine("LEFT")
                sb.appendLine("TEXT 7 0 0 $y ${trimForPrint(resources.getString(R.string.label_tip))}")
                sb.appendLine("RIGHT")
                sb.appendLine("TEXT 7 0 0 $y ${traffic.tipAmount}")
                y += 50
            }

            sb.appendLine("LEFT")
            sb.appendLine("TEXT 7 0 0 $y ${trimForPrint(resources.getString(R.string.label_total))} ${traffic.totalNumber}")
            sb.appendLine("RIGHT")
            sb.appendLine("TEXT 7 0 0 $y ${traffic.total}")
            y += 40

            sb.appendLine("LEFT")
            sb.appendLine("TEXT 7 0 0 $y ===============================")
            y += 40
        }

        // Footer
        sb.appendLine("CENTER")
        sb.appendLine("TEXT 7 0 0 $y ${SupercaseConfig.INSTITUTION}")
        y += 60

        sb.appendLine("FORM")
        sb.appendLine("PRINT")
        sb.appendLine("")


        var result = sb.toString().trimIndent()

        result = result.replace("! 0 200 200 2000 1", "! 0 200 200 " + y + " 1")
        return result
    }

}