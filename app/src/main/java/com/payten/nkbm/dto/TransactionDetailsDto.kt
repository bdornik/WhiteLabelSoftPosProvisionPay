package com.payten.nkbm.dto

import com.payten.nkbm.enums.TransactionStatus
import java.io.Serializable

data class TransactionDetailsDto(
    val bankName: String,
    val dateTime: String,
    var merchantId: String,
    var terminalId: String,
    val merchantName: String,
    val cardNumber: String?,
    val authorizationCode: String?,
    var operationName: String,
    var status: String?,
    var response: String?,
    var message: String?,
    var rrn: String,
    var code: String,
    val applicationLabel: String?,
    val aid: String?,
    val amount: String,
    val isIps: Boolean,
    var sdkStatus: TransactionStatus?,
    var billStatus: String?,
    var recordId: String?,
    var listName: String?,
    var color : Int,
    var tipAmount: String
) : Serializable