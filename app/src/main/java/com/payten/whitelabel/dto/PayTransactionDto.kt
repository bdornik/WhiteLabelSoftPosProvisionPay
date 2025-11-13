package com.payten.whitelabel.dto

data class PayTransactionDto(
    var creditTransferIdentificator: String,
    var terminalIdentificator: String,
    var creditTransferAmount: String,
    var debtorAccountNumber: String,
    var oneTimeCode: String,
    var debtorReference: String?,
    var debtorName: String?,
    var debtorAddress: String?,
)