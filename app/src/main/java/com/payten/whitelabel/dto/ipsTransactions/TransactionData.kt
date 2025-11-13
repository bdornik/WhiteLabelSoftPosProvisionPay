package com.payten.whitelabel.dto.ipsTransactions


data class TransactionData(
    val endToEndIdentificator: String?,
    val tid: String,
    val statusCode: String,
    val date: String,
    var amount: String
)

