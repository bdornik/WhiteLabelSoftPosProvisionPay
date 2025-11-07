package com.payten.nkbm.dto.keys

data class GetKeysResponse(
    val statusCode : String,
    val message : String,
    val data : GetKeysResponseData
)
