package com.payten.nkbm.dto.transactions

data class GetTransactionsRequest(
    var userId: String,
    var dateFrom: String,
    var dateTo: String,
    var tid: String
)
