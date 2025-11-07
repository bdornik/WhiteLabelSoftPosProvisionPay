package com.payten.nkbm.adapters

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.activities.TransactionActivity
import com.payten.nkbm.databinding.TransactionLayoutBinding
import com.payten.nkbm.dto.TransactionDetailsDto
import com.payten.nkbm.dto.TransactionDto
import com.payten.nkbm.enums.TransactionSource
import com.payten.nkbm.enums.TransactionStatus
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.AmountUtil
import com.payten.nkbm.utils.DateUtil
import com.payten.nkbm.utils.Utility
import mu.KotlinLogging
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class TransactionAdapter(
    var sharedPreferences: KsPrefs,
    var context: Context,
    var isDarkMode: Boolean
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    private var dataList = emptyList<TransactionDto>()
    private lateinit var mListener: OnItemClickListener
    private lateinit var binding: TransactionLayoutBinding
    private val logger = KotlinLogging.logger {}

    interface OnItemClickListener {
        fun onItemClick(transaction: TransactionDto)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    internal fun setDataList(dataList: List<TransactionDto>) {
        this.dataList = dataList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        binding = TransactionLayoutBinding.inflate(LayoutInflater.from(parent.context))
        return TransactionViewHolder(binding, mListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val data = dataList[position]
        holder.setIsRecyclable(false)

//        if(data.statusCode.equals("a", true)){
//            data.status = TransactionStatus.Accepted
//        } else if (data.statusCode.equals("f", true)){
//            data.status = TransactionStatus.Rejected
//        }

        val slovenian = Locale.forLanguageTag(Utility.getLanguage(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0)))

        val dF = DateTimeFormatter.ofPattern("dd.MMMM yyyy",slovenian)
        var tF = DateTimeFormatter.ofPattern("HH:mm")


        holder.date.text = "${data.transactionDate?.format(tF)}"
        holder.time.text =  "${data.transactionDate?.format(dF)}"



        var formattedAmount = AmountUtil.getAmountWithOutCurr(data.amount)

        if (data.tipAmount != "0.0"){

            formattedAmount = AmountUtil.getAmountWithOutCurr((data.tipAmount.toDouble() + data.amount.toDouble()).toString())
        }

        holder.amount.text = formattedAmount

        holder.e2e.text = data.recordId

        holder.btnCancel.setOnClickListener {
            holder.listener.onItemClick(data)
        }

        holder.btnCancel.visibility = View.GONE



        logger.info { "Status for row: ${position} is ${data.status}" }
        var status = ""
        var color = R.color.globalGreen
        holder.background.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        //holder.icon.setImageResource(R.drawable.icon_pos)

        if (data.status == TransactionStatus.Accepted) {
            //holder.icon.setImageResource(R.drawable.icon_pos)
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.buttonGreen))
            holder.status.text = context.getString(R.string.transaction_status_accepted).toUpperCase()
            status = context.getString(R.string.transaction_status_accepted)
            color = R.color.buttonGreen

//            if (compareDates(data.transactionDate?.toLocalDate().toString()))
//                holder.btnCancel.visibility = View.VISIBLE

            if (data.newest){
                //holder.btnCancel.visibility = View.VISIBLE
                if (compareDates(data.transactionDate?.toLocalDate().toString()))
                    holder.btnCancel.visibility = View.VISIBLE
            }
        } else if (data.status == TransactionStatus.Rejected) {
//            holder.icon.setImageResource(R.drawable.icon_pos_failed)
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.transaction_red))
            holder.status.text =
                context.getString(R.string.transaction_status_rejected).toUpperCase()
        } else if (data.status == TransactionStatus.Voided) {
            if (data.operationName!!.contains("Void", true)) {
                //holder.status.setTextColor(ContextCompat.getColor(context, R.color.globalGreen))
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.transaction_red))
                holder.status.text =
                    context.getString(R.string.transaction_status_canceled).toUpperCase()
            } else {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.pinButtonBorder))
                holder.status.text =
                    context.getString(R.string.transaction_status_accepted).toUpperCase()
            }
            holder.background.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.amountBackground
                )
            )
            //holder.status.text = context.getString(R.string.transaction_status_canceled)
        } else if (data.status == TransactionStatus.PinNotEntered) {
//            holder.icon.setImageResource(R.drawable.icon_pos_failed)
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.transaction_red))
            holder.status.text =
                context.getString(R.string.transaction_status_pin_not_entered).toUpperCase()
        } else if (data.status == TransactionStatus.WrongPin) {
//            holder.icon.setImageResource(R.drawable.icon_pos_failed)
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.transaction_red))
            holder.status.text =
                context.getString(R.string.transaction_status_wrong_pin).toUpperCase()
        } else if (data.status == TransactionStatus.Reversed) {
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.transaction_red))
            holder.status.text =
                context.getString(R.string.transaction_status_reversed).toUpperCase()
        }

        if (data.source == TransactionSource.POS) {
            holder.icon.setImageResource(R.drawable.icon_pos)
        } else {
            holder.icon.setImageResource(R.drawable.icon_flik_transactions)
            holder.btnCancel.visibility = View.GONE
        }

        holder.background.setOnClickListener {
            var statusString = data.statusCode
            var listName = ""
            var operationName = ""
            var color = R.color.globalGreen

            if (data.operationName != null && data.operationName!!.contains("Void", true))
                operationName = context.getString(R.string.transaction_operation_void)
            else
                operationName = context.getString(R.string.label_operation_sales)




            if (data.status == TransactionStatus.Accepted) {

                color = R.color.globalGreen
                listName = context.getString(R.string.transaction_status_accepted).toUpperCase()

            } else if (data.status == TransactionStatus.Rejected) {

                color = R.color.transaction_red
                listName =
                    context.getString(R.string.transaction_status_rejected).toUpperCase()
            } else if (data.status == TransactionStatus.Voided) {
                if (data.operationName!!.contains("Void", true)) {

                    color = R.color.transaction_red
                    listName =
                        context.getString(R.string.transaction_status_canceled).toUpperCase()
                } else {
                    color = R.color.pinButtonBorder
                    listName =
                        context.getString(R.string.transaction_status_accepted).toUpperCase()
                }

            } else if (data.status == TransactionStatus.PinNotEntered) {

                color = R.color.transaction_red
                listName =
                    context.getString(R.string.transaction_status_pin_not_entered).toUpperCase()
            } else if (data.status == TransactionStatus.WrongPin) {
                color = R.color.transaction_red
                listName = context.getString(R.string.transaction_status_wrong_pin).toUpperCase()
            } else if (data.status == TransactionStatus.Reversed) {
                color = R.color.transaction_red
                listName = context.getString(R.string.transaction_status_reversed).toUpperCase()
            }





            val intent = Intent(context, TransactionActivity::class.java)
//            if (p0?.transactionResponseData != null) {
            val transactionData = TransactionDetailsDto(
                aid = data.transactionId,
                applicationLabel = data.applicationLabel,
                authorizationCode = data.authorizationCode,
                bankName = "",
                cardNumber = data.maskedPAN,
                dateTime = data.transactionDate.toString(),
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME),
                message = data.screenMessage!!,
                operationName = operationName,
                response = data.responseCode,
                rrn = "",
                code = data.recordId,
                status = data.responseCode,
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID),
                amount = data.amount,
                isIps = data.isIps == true,
                sdkStatus = data.status,
                listName = listName,
                billStatus = null,
                recordId = data.recordId,
                color = color,
                tipAmount = "0.0"
            )

            if (data.source == TransactionSource.IPS) {
                transactionData.rrn = data.creaditTransferIdentificator.toString()
                transactionData.merchantId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID)
                transactionData.terminalId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID)

            }
            intent.putExtra("data", transactionData)
            intent.putExtra("fromAdapter", true)
//            }
            context.startActivity(intent)
        }

        if (data.isIps == true) {
//            holder.btnCancel.visibility = View.GONE
            holder.labelE2E.text = context.getString(R.string.label_transaction_e2e_ips)
        } else {
            holder.labelE2E.text = context.getString(R.string.label_transaction_e2e)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun compareDates(date: String): Boolean {
        try {
            val transactionDate = LocalDate.parse(date)
            val dateNow = LocalDate.now()
            return transactionDate == dateNow
        } catch (ex: Exception) {
            logger.error { ex.stackTrace }
            return false
        }
    }


    class TransactionViewHolder(binding: TransactionLayoutBinding, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(binding.root) {
        val root = binding.root
        val date = binding.date
        val time = binding.time
        val amount = binding.amount
        val labelE2E = binding.labelE2E
        val e2e = binding.e2e
        val labelStatus = binding.labelStatus
        val status = binding.status
        val icon = binding.transactionIcon
        val iconBackground = binding.transactionIconBackground
        val background = binding.background
        val btnCancel = binding.btnCancelTransaction
        val listener = listener
    }
}