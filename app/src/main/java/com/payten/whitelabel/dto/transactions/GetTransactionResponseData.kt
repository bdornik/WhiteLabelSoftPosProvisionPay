package com.payten.whitelabel.dto.transactions

import java.util.Date

data class GetTransactionResponseData(
    var amount: Double,
    var authorizationCode: String,
    var batchNo: Int,
    var batchSeqNo: Int,
    var currencyCode: String,
    var maskedPAN: String,
    var printerMessage: String,
    var recordId: Int,
    var responseCode: String,
    var screenMessage: String,
    var statusCode: String,
    var transactionCode: String,
    var transactionDate: String,
    var mainRecordId: Int,
    var terminalId: String,
    var merchantId: String,
    var internalMpaId: Int,
    var internalTerminalId: Int,
    var operationName: String,
    var wspId: Int,
    var clearingDate: Date,
    var isVoidable: String,
    var isRefundable: String,
    var financialMode: String,
    var applicationLabel: String,
    var aid: String,
    var bankResponseCode: String,
    var originalAmount: Double,
    var otherAmount: Double,
    var RRN: String,
    var STAN: String,
    var tipAmount: Double

)
