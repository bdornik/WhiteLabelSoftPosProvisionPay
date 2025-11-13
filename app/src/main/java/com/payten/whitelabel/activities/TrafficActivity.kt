package com.payten.whitelabel.activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.adapters.TransactionAdapter
import com.payten.whitelabel.databinding.ActivityTrafficBinding
import com.payten.whitelabel.databinding.DialogPinRegistrationBinding
import com.payten.whitelabel.databinding.DialogTransactionBinding
import com.payten.whitelabel.dto.transactions.GetTransactionsRequest
import com.payten.whitelabel.dto.SendEmailReportDto
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.dto.TransactionDto
import com.payten.whitelabel.enums.TransactionSortType
import com.payten.whitelabel.enums.TransactionSource
import com.payten.whitelabel.enums.TransactionStatus
import com.payten.whitelabel.enums.TransactionStatusFilterType
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.utils.DateUtil
import com.payten.whitelabel.utils.DialogInteractionListener
import com.payten.whitelabel.utils.DialogUtility
import com.payten.whitelabel.utils.Utility
import com.payten.whitelabel.viewmodel.TrafficViewModel
import com.simant.MainApplication
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TrafficActivity : BaseActivity(), DialogInteractionListener{
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityTrafficBinding
    private lateinit var  transAdapter: TransactionAdapter
    private var posList = listOf<TransactionDto>()
    private var ipsList = listOf<TransactionDto>()

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Inject
    lateinit var sharedPreferences: KsPrefs

    lateinit var trafficModel: TrafficViewModel

    lateinit var dialogUtility: DialogUtility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrafficBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        dialogUtility = DialogUtility(this)
        dialogUtility.setmDialogInteractionListener(this)

        val model: TrafficViewModel by viewModels()
        trafficModel = model
        binding.recyclerView.layoutManager = GridLayoutManager(applicationContext, SPAN_COUNT)



        if(sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)){
            transAdapter = TransactionAdapter(sharedPreferences, this, true)
        }else{
            transAdapter = TransactionAdapter(sharedPreferences, this, false)
        }

        binding.recyclerView.adapter = transAdapter

        transAdapter.setOnItemClickListener(object : TransactionAdapter.OnItemClickListener{
            override fun onItemClick(transaction : TransactionDto) {
                cancelTransactionDialog(transaction)
            }
        })

        binding.btnFilter.setOnClickListener {
            val intent = Intent(this, FilterActivity::class.java)
            startActivity(intent)
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.btnReport.setOnClickListener {
            val intent = Intent(this, BillActivity::class.java)
            startActivity(intent)
        }

        model.cancelIpsTransactionSuccess.observe(this){
            voidDialog(false)
        }

        model.cancelIpsTransactionFailed.observe(this){
            voidDialog(true)
        }

        model.sendEmailSuccess.observe(this){
            emailDialog(false)
        }

        model.sendEmailFailed.observe(this){
            emailDialog(true)
        }

        model.transactionResultsSuccess.observe(this) {
            binding.spinner.visibility = View.GONE
            if (it == null) {
                binding.emptyContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                val sort = sharedPreferences.pull(
                    SharedPreferencesKeys.FILTER_SORT,
                    TransactionSortType.DateDesc.ordinal
                )
                val periodFrom = sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "")
                val periodTo = sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "")
                var localDateFrom = LocalDate.now().atStartOfDay().minus(90, ChronoUnit.DAYS)
                var localDateTo = LocalDate.now().atStartOfDay().plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS)

                if(periodFrom.isNotEmpty()){
                    localDateFrom = DateUtil.localDateFromString(periodFrom,false)
                }

                if(periodTo.isNotEmpty()){
                    localDateTo = DateUtil.localDateFromString(periodTo,false)
                }

//                val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
//                val itemType = object : TypeToken<List<TransactionDto>>() {}.type
//                logger.info { "Payload: ${it.transactionsResponseData.payload}" }
//                var transactions =
//                    gson.fromJson<List<TransactionDto>>(
//                        it.transactionsResponseData.payload,
//                        itemType
//                    )
                for (transaction in it) {
                    logger.info { "Transaction if: ${transaction.recordId} responseCode: ${transaction.responseCode} statusCode: ${transaction.statusCode} screenMessage: ${transaction.screenMessage} date: ${transaction.transactionDate}" }
                    transaction.isIps = false
                    transaction.amountDouble = transaction.amount.toDouble()
                    if (transaction.statusCode.equals("a", true)) {
                        if (transaction.responseCode.equals("06", true)) {
                            transaction.status = TransactionStatus.Rejected
                        } else if (transaction.responseCode.equals("00", true)) {
                            logger.info { "Setting accepted" }
                            transaction.status = TransactionStatus.Accepted
                        } else if (transaction.responseCode.equals("17", true)) {
                            transaction.status = TransactionStatus.WrongPin
                        } else {
                            transaction.status = TransactionStatus.Rejected
                        }
                    } else if (transaction.statusCode.equals("f", true) || transaction.statusCode.equals("p", true) ) {
                        transaction.status = TransactionStatus.Rejected
                    } else if (transaction.statusCode.equals("v", true)) {
                        transaction.status = TransactionStatus.Voided
                    } else if (transaction.statusCode.equals("s", true)) {
                        transaction.status = TransactionStatus.PinNotEntered
                    }else if(transaction.statusCode.equals("d",true)){
                        transaction.status = TransactionStatus.Reversed
                    }

                    transaction.source = TransactionSource.POS
                }

                markNewestApproval(it)

                //posList = transactions
                posList = it

                if (posList.isEmpty()) {
                    binding.emptyContainer.visibility = View.VISIBLE
                } else {
                    binding.emptyContainer.visibility = View.GONE
                }

                var sortedTransactions = posList

                if (sort == TransactionSortType.DateDesc.ordinal) {
                    sortedTransactions = posList.sortedByDescending { it.transactionDate }
                } else if (sort == TransactionSortType.DateAsc.ordinal) {
                    sortedTransactions = posList.sortedBy { it.transactionDate }
                } else if (sort == TransactionSortType.AmountDesc.ordinal) {
                    sortedTransactions = posList.sortedByDescending { it.amountDouble }
                } else if (sort == TransactionSortType.AmountAsc.ordinal) {
                    sortedTransactions = posList.sortedBy { it.amountDouble }
                }

                val type = sharedPreferences.pull(
                    SharedPreferencesKeys.FILTER_TYPE,
                    TransactionSource.POS.ordinal
                )

                if (type == TransactionSource.IPS.ordinal) {
                    sortedTransactions = ipsList.sortedByDescending { it.transactionDate }
                }

                logger.info { "Sorted transactions: ${sortedTransactions}" }

                var filteredTransactions = sortedTransactions

                if (localDateFrom != null) {
                    filteredTransactions =
                        filteredTransactions.filter {
                            logger.info { "Transaction date: ${it.transactionDate} localdatefrom: ${localDateFrom}" }
                            it.transactionDate!! >= localDateFrom
                        }
                }
                if (localDateTo != null) {
                    filteredTransactions =
                        filteredTransactions.filter {
                            logger.info { "Transaction date: ${it.transactionDate} localDateTo: ${localDateTo}" }
                            it.transactionDate!! <= localDateTo }
                }
                logger.info { "First filtered transactions: ${filteredTransactions}" }

                val status = sharedPreferences.pull(
                    SharedPreferencesKeys.FILTER_STATUS,
                    TransactionStatusFilterType.ALL.ordinal
                )

                when (status) {
                    TransactionStatusFilterType.ALL.ordinal -> {

                    }
                    TransactionStatusFilterType.ACCEPTED.ordinal -> {
                        filteredTransactions =
                            filteredTransactions.filter { it.status == TransactionStatus.Accepted }
                    }
                    TransactionStatusFilterType.REJECTED.ordinal -> {
                        filteredTransactions =
                            filteredTransactions.filter { it.status != TransactionStatus.Accepted && it.status != TransactionStatus.Voided && it.status != TransactionStatus.Pending}
                    }
                    TransactionStatusFilterType.VOID.ordinal -> {
                        filteredTransactions =
                            filteredTransactions.filter { it.status == TransactionStatus.Voided }
                    }
                }

                logger.info { "Setting data list: ${filteredTransactions}" }
                transAdapter.setDataList(filteredTransactions)
                if(filteredTransactions.isEmpty()){
                    binding.recyclerView.visibility = View.INVISIBLE
                    binding.emptyContainer.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.emptyContainer.visibility = View.GONE
                }
            }
        }

        model.ipsTransactionResultsSuccess.observe(this) {
            binding.spinner.visibility = View.GONE
            if (it == null || it.data.isEmpty()) {
                binding.emptyContainer.visibility = View.VISIBLE
            }

            val sort = sharedPreferences.pull(
                SharedPreferencesKeys.FILTER_SORT,
                TransactionSortType.DateDesc.ordinal
            )
            var localDateFrom = LocalDateTime.now().minus(90, ChronoUnit.DAYS)
            var localDateTo = LocalDateTime.now().plus(1, ChronoUnit.DAYS)

            var transactions = mutableListOf<TransactionDto>()

            for (resp in it.data) {
                val tDto = TransactionDto(
                    resp.amount,
                    resp.amount.replace(',','.').toDouble(),
                    "",
                    "",
                    "a",
                    LocalDateTime.parse(resp.date),
                    resp.statusCode,
                    TransactionSource.IPS,
                    resp.statusCode,
                    TransactionStatus.Accepted,
                    "",
                    "",
                    sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID),
                    true,
                    resp.endToEndIdentificator,
                    "",
                    null,
                    "","0.0"
                )
                transactions.add(tDto)
            }

            for (transaction in transactions) {
                logger.info { "Transaction if: ${transaction.recordId} responseCode: ${transaction.responseCode} statusCode: ${transaction.statusCode} screenMessage: ${transaction.screenMessage}" }
                transaction.isIps = false
                transaction.amountDouble = transaction.amount.replace(',','.').toDouble()
                if (transaction.statusCode.equals("a", true)) {
                    if (transaction.responseCode.equals("06", true)) {
                        transaction.status = TransactionStatus.Voided
                    } else if (transaction.responseCode.equals("00", true)) {
                        logger.info { "Setting accepted" }
                        transaction.status = TransactionStatus.Accepted
                    } else if (transaction.responseCode.equals("17", true)) {
                        transaction.status = TransactionStatus.WrongPin
                    } else {
                        transaction.status = TransactionStatus.Rejected
                    }
                } else if (transaction.statusCode.equals(
                        "f",
                        true
                    ) || transaction.statusCode.equals("p", true)
                ) {
                    transaction.status = TransactionStatus.Rejected
                } else if (transaction.statusCode.equals("v", true)) {
                    transaction.status = TransactionStatus.Voided
                } else if (transaction.statusCode.equals("s", true)) {
                    transaction.status = TransactionStatus.PinNotEntered
                }

                transaction.isIps = true
                transaction.source = TransactionSource.IPS
            }

            posList = transactions.toList()

            logger.info { "posList ${posList.size}" }

            if (posList.isEmpty()) {
                binding.emptyContainer.visibility = View.VISIBLE
            } else {
                binding.emptyContainer.visibility = View.GONE
            }

            var sortedTransactions = posList

            if (sort == TransactionSortType.DateDesc.ordinal) {
                sortedTransactions = posList.sortedByDescending { it.transactionDate }
            } else if (sort == TransactionSortType.DateAsc.ordinal) {
                sortedTransactions = posList.sortedBy { it.transactionDate }
            } else if (sort == TransactionSortType.AmountDesc.ordinal) {
                sortedTransactions = posList.sortedByDescending { it.amountDouble }
            } else if (sort == TransactionSortType.AmountAsc.ordinal) {
                sortedTransactions = posList.sortedBy { it.amountDouble }
            }

//            val type = sharedPreferences.pull(
//                SharedPreferencesKeys.FILTER_TYPE,
//                TransactionSource.POS.ordinal
//            )

//            if (type == TransactionSource.IPS.ordinal) {
//                sortedTransactions = ipsList.sortedByDescending { it.transactionDate }
//            }

            logger.info { "sorted ${sortedTransactions.size}" }

            var filteredTransactions = sortedTransactions

            if (localDateFrom != null) {
                filteredTransactions =
                    filteredTransactions.filter { it.transactionDate!! >= localDateFrom }
            }
            if (localDateTo != null) {
                filteredTransactions =
                    filteredTransactions.filter { it.transactionDate!! <= localDateTo }
            }

            val status = sharedPreferences.pull(
                SharedPreferencesKeys.FILTER_STATUS,
                TransactionStatusFilterType.ALL.ordinal
            )

            when (status) {
                TransactionStatusFilterType.ALL.ordinal -> {

                }
                TransactionStatusFilterType.ACCEPTED.ordinal -> {
                    filteredTransactions =
                        filteredTransactions.filter { it.status == TransactionStatus.Accepted }
                }
                TransactionStatusFilterType.REJECTED.ordinal -> {
                    filteredTransactions =
                        filteredTransactions.filter { it.status != TransactionStatus.Accepted && it.status != TransactionStatus.Voided && it.status != TransactionStatus.Pending }
                }
                TransactionStatusFilterType.VOID.ordinal -> {
                    filteredTransactions =
                        filteredTransactions.filter { it.status == TransactionStatus.Voided }
                }
            }

            logger.info { "Setting ${filteredTransactions.size} transactions to list" }
            transAdapter.setDataList(filteredTransactions)
        }

    }

    private fun reloadTransactions() {
        val periodFrom = sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "")
        val periodTo = sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "")
        var localDateFrom = LocalDate.now().atStartOfDay().minus(90, ChronoUnit.DAYS)
        var localDateTo = LocalDate.now().atStartOfDay().plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS)

        if(periodFrom.isNotEmpty()){
            localDateFrom = DateUtil.localDateFromString(periodFrom,true)
        }

        if(periodTo.isNotEmpty()){
            localDateTo = DateUtil.localDateFromString(periodTo,true)
        }

        var tF2 = DateTimeFormatter.ofPattern("HH:mm:ss")
        var dF2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        binding.share.setOnClickListener {
            dialogUtility.displaySendReportDialog("${localDateFrom.format(dF2)} ${localDateFrom.format(tF2)}", "${localDateTo.format(dF2)} ${localDateTo.format(tF2)}")
        }
        try {

//            val getTransactionsData = GetTransactionsInputData()
//            var tF = DateTimeFormatter.ofPattern("HHmmss")
//            var dF = DateTimeFormatter.ofPattern("yyyyMMdd")
//
//            getTransactionsData.fromDate = "${localDateFrom.format(dF)}${localDateFrom.format(tF)}"
//            getTransactionsData.toDate = "${localDateTo.format(dF)}${localDateTo.format(tF)}"



            var tF = DateTimeFormatter.ofPattern("HH:mm:ss")
            var dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")


            val dateFrom = "${localDateFrom.format(dF)}T${localDateFrom.format(tF)}"
            val dateTo = "${localDateTo.format(dF)}T${localDateTo.format(tF)}"

            val getTransactionsData = GetTransactionsRequest(sharedPreferences.pull(SharedPreferencesKeys.USER_ID), dateFrom, dateTo,sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID))

            val type = sharedPreferences.pull(
                SharedPreferencesKeys.FILTER_TYPE,
                TransactionSource.POS.ordinal
            )

            binding.spinner.visibility = View.VISIBLE
            if(sharedPreferences.pull(SharedPreferencesKeys.POS_EXISTS, false)) {
                if(type == TransactionSource.POS.ordinal) {
                    if (!MainApplication.getInstance().getConfigurationInterface().isReady()){

                    } else {
                        trafficModel.getTransactionsFromServer(getTransactionsData)
                    }
                } else {
                    trafficModel.getIpsTransactions(
                        sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""),
                        dateFrom,
                        dateTo,
                        sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, "")
                    )
                }
            } else {
                // only ips
                if(sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS, false)) {
                    if(type == TransactionSource.IPS.ordinal) {
                        trafficModel.getIpsTransactions(
                            sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""),
                            dateFrom,
                            dateTo,
                            sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, "")
                        )
                    }
                }
            }
        } catch (e: Exception){
            logger.error { e.message }
        }

        binding.spinner.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        setPeriod()
        handleDarkLightMode()
        binding.spinner.visibility = View.VISIBLE
        reloadTransactions()
    }

    private fun handleDarkLightMode() {
    }

    private fun setPeriod(){
        val periodFrom = sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "")
        val periodTo = sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "")

        if(periodFrom == "" && periodTo == ""){
            binding.period.text = ""
        }else{
            binding.period.text = "$periodFrom - $periodTo"
        }

    }

    private fun cancelTransactionDialog(transaction : TransactionDto){
        val dialog = Dialog(this)
        val dialogBinding : DialogTransactionBinding = DialogTransactionBinding.inflate(layoutInflater)

        handleDialogDarkLightMode(dialogBinding)



        val slovenian = Locale.forLanguageTag(Utility.getLanguage(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0)))

        val dF = DateTimeFormatter.ofPattern("dd.MMM yyyy",slovenian)
        var tF = DateTimeFormatter.ofPattern("HH:mm")


        dialogBinding.transactionDate.text = "${transaction.transactionDate?.format(dF)}"
        dialogBinding.transactionTime.text = "${transaction.transactionDate?.format(tF)}"

        dialogBinding.amountValue.text = "${AmountUtil.dialogAmount(transaction.amount)}"
        if(!transaction.tipAmount.equals("0.0")){
            val res = AmountUtil.getAmountWithOutCurr((transaction.tipAmount.toDouble() + transaction.amount.toDouble()).toString())

            dialogBinding.amountValue.text = "$res ${resources.getString(R.string.suffix_text)}"
        }

        dialogBinding.e2eValue.text = transaction.recordId

        if(transaction.status == TransactionStatus.Accepted ){
            dialogBinding.statusValue.text = this.getString(R.string.transaction_status_accepted).toUpperCase()
            dialogBinding.statusValue.setTextColor(ContextCompat.getColor(this, R.color.globalGreen))
        }

        dialogBinding.btnQuit.setOnClickListener{
            dialog.dismiss()
            val intent = Intent(this, VoidActivity::class.java)
//            if (p0?.transactionResponseData != null) {
            val transactionData = TransactionDetailsDto(
                aid = transaction.transactionId,
                applicationLabel = transaction.applicationLabel,
                authorizationCode = transaction.authorizationCode,
                bankName = "",
                cardNumber = transaction.maskedPAN,
                dateTime = transaction.transactionDate.toString(),
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                message = transaction.screenMessage!!,
                operationName = resources.getString(R.string.label_operation_sales),
                response = transaction.responseCode,
                rrn = "",
                code = transaction.recordId,
                status = transaction.responseCode,
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID),
                amount = transaction.amount,
                isIps = transaction.isIps == true,
                sdkStatus = transaction.status,
                listName = "",
                billStatus = null,
                color = -1,
                recordId = "",
                tipAmount = "0.0"

            )
            intent.putExtra("data", transactionData)
            if (transaction.amount != null) {
                intent.putExtra("Amount", transaction.amount)
            }

            if (transaction.tipAmount != "0.0"){
                var totalAmount = AmountUtil.getAmountNoCurr((transaction.amount.toDouble().plus(transaction.tipAmount.toDouble())).toString())
                intent.putExtra("TotalAmount", totalAmount)
                intent.putExtra("Tip", transaction.tipAmount)
            }
            MainApplication.getInstance().paymentData.transactionId = transaction.recordId
            startActivity(intent)
        }

        dialogBinding.btnCanceled.setOnClickListener{
            dialog.dismiss()
        }

        dialog.setContentView(dialogBinding.root)
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }


    private fun voidDialog(failed: Boolean){
        this@TrafficActivity.runOnUiThread {
            binding.spinner.visibility = View.GONE
            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            if (failed) {
                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text = this.getString(R.string.label_void_failed)
                dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
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
                    finish()
                    dialog.dismiss()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
            }
        }
    }

    private fun emailDialog(failed: Boolean){
        this@TrafficActivity.runOnUiThread {
            binding.spinner.visibility = View.GONE
            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            if (failed) {
                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text = this.getString(R.string.send_email_failed)
                dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
            } else {
                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_success)
                dialogBinding.warningLabel.text = this.getString(R.string.send_email_success)
                dialogBinding.btn.text = this.getString(R.string.button_registration_success)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
            }
        }
    }

    private fun handleDialogDarkMode(dialogBinding : DialogPinRegistrationBinding){
        if(sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)){
            dialogBinding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
            dialogBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
            dialogBinding.btn.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
        }
    }

    private fun handleDialogDarkLightMode(dBinding : DialogTransactionBinding){

//        if(sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)){
//            dBinding.detailsLabel.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
//            dBinding.dateLabel.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
//            dBinding.dateValue.setTextColor(ContextCompat.getColor(this, R.color.white))
//            dBinding.amountLabel.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
//            dBinding.amountValue.setTextColor(ContextCompat.getColor(this, R.color.white))
//            dBinding.e2eLabel.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
//            dBinding.e2eValue.setTextColor(ContextCompat.getColor(this, R.color.white))
//            dBinding.statusLabel.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
//            dBinding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
//            dBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
//            dBinding.btnQuit.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
//        }else{
//            dBinding.detailsLabel.setTextColor(ContextCompat.getColor(this, R.color.suffix))
//            dBinding.dateLabel.setTextColor(ContextCompat.getColor(this, R.color.suffix))
//            dBinding.dateValue.setTextColor(ContextCompat.getColor(this, R.color.black))
//            dBinding.amountLabel.setTextColor(ContextCompat.getColor(this, R.color.suffix))
//            dBinding.amountValue.setTextColor(ContextCompat.getColor(this, R.color.black))
//            dBinding.e2eLabel.setTextColor(ContextCompat.getColor(this, R.color.suffix))
//            dBinding.e2eValue.setTextColor(ContextCompat.getColor(this, R.color.black))
//            dBinding.statusLabel.setTextColor(ContextCompat.getColor(this, R.color.suffix))
//            dBinding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
//            dBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.black))
//            dBinding.btnQuit.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
//        }

    }

    companion object{
        const val SPAN_COUNT = 1
    }

    override fun onSendReportToEmail(
        userEmail: String?,
        dateFrom: String?,
        dateTo: String?,
        fileFormat: String?
    ) {
        logger.info { "Sending to: $userEmail from: $dateFrom to: $dateTo format: $fileFormat" }
        binding.spinner.visibility = View.VISIBLE
        trafficModel.sendEmailReport(SendEmailReportDto(sharedPreferences.pull(SharedPreferencesKeys.USER_TID, ""), dateFrom!!, dateTo!!, userEmail!!, fileFormat!!))
    }


    fun markNewestApproval(transactions: List<TransactionDto>): List<TransactionDto> {
        // napravi kopiju liste i resetuj newest svima
        transactions.forEach { it.newest = false }

        // nadji poslednju (najnoviju) Approval transakciju
        val newestApproval = transactions
            //.filter { it.status == TransactionStatus.Accepted }
            .maxByOrNull { it.transactionDate ?: LocalDateTime.MIN }

        if (newestApproval?.status == TransactionStatus.Accepted)
            newestApproval.newest = true

        return transactions
    }


    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.remove(SharedPreferencesKeys.FILTER_TYPE)
        sharedPreferences.remove(SharedPreferencesKeys.FILTER_STATUS)
        sharedPreferences.remove(SharedPreferencesKeys.FILTER_SORT)
        sharedPreferences.remove(SharedPreferencesKeys.DATE_FROM)
        sharedPreferences.remove(SharedPreferencesKeys.DATE_TO)
    }
}