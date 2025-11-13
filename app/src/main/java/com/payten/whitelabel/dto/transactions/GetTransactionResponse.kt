package com.payten.whitelabel.dto.transactions

data class GetTransactionResponse(
    val statusCode:String,
    val message:String,
    val data: GetTransactionResponseDataList

)
