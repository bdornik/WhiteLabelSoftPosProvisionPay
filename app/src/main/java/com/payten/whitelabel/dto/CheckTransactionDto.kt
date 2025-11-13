package com.payten.whitelabel.dto

data class CheckTransactionDto(
    val creditTransferIdentificator: String,
    val terminalIdentificator: String,
    val creditTransferAmount: String,
)