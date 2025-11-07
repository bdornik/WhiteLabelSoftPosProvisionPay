package com.payten.nkbm.dto

import com.payten.nkbm.enums.TransactionSource
import com.payten.nkbm.enums.TransactionStatus
import org.threeten.bp.LocalDateTime

data class TransactionDto(
    var amount: String,
    var amountDouble: Double,
    val recordId: String,
    val transactionId: String?,
    val statusCode: String?,
    val transactionDate: LocalDateTime?,
    val responseCode: String?,
    var source: TransactionSource?,
    var screenMessage: String?,
    var status : TransactionStatus?,
    var authorizationCode: String?,
    var maskedPAN: String?,
    var merchantId: String?,
    var isIps: Boolean?,
    var creaditTransferIdentificator: String?,
    var applicationLabel: String?,
    var operationName: String?,
    var applicationId: String,
    var tipAmount: String,
    var newest: Boolean = false
    )