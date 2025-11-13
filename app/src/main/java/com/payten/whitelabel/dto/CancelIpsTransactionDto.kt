package com.payten.whitelabel.dto

data class CancelIpsTransactionDto(
    val creditTransferIdentificator: String,
    val creditTransferAmount: String,
    val terminalIdentificator: String,
)