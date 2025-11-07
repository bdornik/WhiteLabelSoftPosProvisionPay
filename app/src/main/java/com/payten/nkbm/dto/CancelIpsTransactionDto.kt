package com.payten.nkbm.dto

data class CancelIpsTransactionDto(
    val creditTransferIdentificator: String,
    val creditTransferAmount: String,
    val terminalIdentificator: String,
)