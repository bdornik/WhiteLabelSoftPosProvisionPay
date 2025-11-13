package com.payten.whitelabel.dto

data class CheckTransactionResponseDto(
    val creditTransferIdentificator: String,
    val terminalIdentificator: String,
    val approvalCode: String,
    val statusCode: String
)