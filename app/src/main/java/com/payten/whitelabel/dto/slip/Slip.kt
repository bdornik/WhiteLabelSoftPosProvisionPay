package com.payten.whitelabel.dto.slip

import android.content.res.Resources
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig

data class Slip(
    val status: String,
    val amount: String,
    val tipLessAmount: String,
    val tip : String,
    val date : String,
    val mid: String,
    val tid : String,
    val merchantName: String,
    val cardNumber: String,
    val authorizationCode: String,
    val operationName: String,
    val response : String,
    val message: String,
    val installmentNumber: String,
    val uniqueId: String,
    val cardType:String,
    private val resources: Resources

) {
    override fun toString(): String {
        return (    getAmountPrint() +
                    "${resources.getString(R.string.share_date)}: ${date}\n" +
                    "${resources.getString(R.string.label_transaction_status)} ${status}\n" +
                    "${resources.getString(R.string.label_transaction_merchant_id)} ${mid}\n" +
                    "${resources.getString(R.string.label_transaction_terminal_id)} ${tid}\n" +
                    "${resources.getString(R.string.label_transaction_merchant_name)} ${merchantName}\n" +
                    "${resources.getString(R.string.label_transaction_card_number)} ${cardNumber}\n" +
                    "${resources.getString(R.string.label_transaction_authorization_code)} ${authorizationCode}\n" +
                    "${resources.getString(R.string.label_transaction_operation_name)} ${operationName}\n" +
                    "${resources.getString(R.string.label_transaction_response)} ${response}\n" +
                    "${resources.getString(R.string.label_transaction_message)} ${message}\n" +
                    //getInstallmentPrint()+
                   // getUniqueIdPrint()+
                    "${cardType}\n")

    }

    fun toStringPrint(): String {

        return ("[L]\n" +
                "[C]================================\n" +
                "[L]\n"+
                "[C]"+getAmountForPrinter()+
                "[L]${resources.getString(R.string.share_date)}: ${date}\n" +
                "[L]${resources.getString(R.string.label_transaction_status)} ${status}\n" +
                "[L]${resources.getString(R.string.label_transaction_merchant_id)} ${mid}\n" +
                "[L]${resources.getString(R.string.label_transaction_terminal_id)} ${tid}\n" +
                "[L]${resources.getString(R.string.label_transaction_merchant_name)} ${merchantName}\n" +
                "[L]${resources.getString(R.string.label_transaction_card_number)} ${cardNumber}\n" +
                "[L]${resources.getString(R.string.label_transaction_authorization_code)} ${authorizationCode}\n" +
                "[L]${resources.getString(R.string.label_transaction_operation_name)} ${operationName}\n" +
                "[L]${resources.getString(R.string.label_transaction_response)} ${response}\n" +
                "[L]${resources.getString(R.string.label_transaction_message)} ${message}\n" +
                //"[L]"+getInstallmentPrint()+
                // getUniqueIdPrint()+
                "[L]\n" +
                "[L]\n" +
                "[C]${cardType}\n"+
                "[L]\n" +
                "[C]================================\n")

    }


     private fun getAmountPrint(): String{
         if (tip.isNotEmpty()){
             return "${resources.getString(R.string.label_full_amount)} ${amount} ${SupercaseConfig.CURRENCY_STRING}\n"+
                     "${resources.getString(R.string.hint_amount)}: $tipLessAmount \n"+
                     "${resources.getString(R.string.label_tip)} $tip \n"
         }else{
             return "${resources.getString(R.string.hint_amount)}: ${amount} ${SupercaseConfig.CURRENCY_STRING}\n"
         }
     }
//    fun getInstallmentPrint(): String{
//        if (installmentNumber.isNotEmpty()){
//            return "${resources.getString(R.string.installment_number)} ${installmentNumber} \n"
//        }else{
//            return ""
//        }
//    }

    private fun getAmountForPrinter(): String{
        if (tip.isNotEmpty()){
            return "${resources.getString(R.string.label_full_amount)} ${amount} \n"+
                    "[L]${resources.getString(R.string.hint_amount)}: $tipLessAmount \n"+
                    "[L]${resources.getString(R.string.label_tip)} $tip \n"
        }else{
            return "${resources.getString(R.string.hint_amount)}: ${amount} \n"
        }
    }


//    fun getUniqueIdPrint(): String{
//        if (uniqueId.isNotEmpty()){
//            return "${resources.getString(R.string.label_unique_id)} ${uniqueId}\n "
//        }else{
//            return ""
//        }
//    }
}
