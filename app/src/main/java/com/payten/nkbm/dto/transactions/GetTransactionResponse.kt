package com.payten.nkbm.dto.transactions

data class GetTransactionResponse(
    val statusCode:String,
    val message:String,
    val data: GetTransactionResponseDataList

)
