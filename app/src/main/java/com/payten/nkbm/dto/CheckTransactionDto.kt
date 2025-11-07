package com.payten.nkbm.dto

data class CheckTransactionDto(
    val creditTransferIdentificator: String,
    val terminalIdentificator: String,
    val creditTransferAmount: String,
)