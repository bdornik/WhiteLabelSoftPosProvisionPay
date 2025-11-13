package com.payten.whitelabel.dto.ipsTransactions

data class GetIpsTransactionRequest(
    val userId: String,
    val dateFrom: String,
    val dateTo: String,
    val tid: String
)
