package com.payten.whitelabel.dto

data class IpsTransactionDto(
    val transactionId: String,
    val transactionDate: String,
    val transactionTime: String,
    val transactionStatus: String,
    val creditTransferIdentificator: String,
    val creditTransferAmount: String,
    val terminalIdentificator: String,
    val transactionStatusCode: String,
    val operationName : String,
)