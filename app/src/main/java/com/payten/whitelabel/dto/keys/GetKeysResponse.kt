package com.payten.whitelabel.dto.keys

data class GetKeysResponse(
    val statusCode : String,
    val message : String,
    val data : GetKeysResponseData
)
