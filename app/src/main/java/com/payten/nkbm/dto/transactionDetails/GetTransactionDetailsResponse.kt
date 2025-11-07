package com.payten.nkbm.dto.transactionDetails

data class GetTransactionDetailsResponse(
    val statusCode:String,
    val message:String,
    val data: GetTransactionDetailsResponseDataList
)
