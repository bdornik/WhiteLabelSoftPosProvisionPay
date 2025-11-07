package com.payten.nkbm.dto

data class CheckTransactionResponseDto(
    val creditTransferIdentificator: String,
    val terminalIdentificator: String,
    val approvalCode: String,
    val statusCode: String
)