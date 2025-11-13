package com.payten.whitelabel.dto

data class CheckTransferRequest(
    //CheckTransferRequest
    val endToEndReference: String,
    val terminalIdentificator: String,
    val amount: String,
    val qrCodeString: String,
    val transactionId: String,
)