package com.payten.nkbm.dto

data class QRDto(
    val identificationCode: String,
    val version: String,
    val charSet: String,
    val payerAccNumber: String,
    val merchantName: String,
    val payerNameAndPlace: String,
    val merchantAddress: String,
    val amountAndCurrency: String,
    val mcc: String,
    val creditTransferIdentificator: String,
    val paymentCode: String,
    val terminalIdentification: String,
    val merchantIdentification: String,
    val stan: String,
    val date: String
)