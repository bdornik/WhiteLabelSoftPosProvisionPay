package com.payten.nkbm.dto.ipsTransactions

data class GetIpsTransactionResponse(
    val statusCode: String,
    val message: String,
    val data: List<TransactionData>

)
