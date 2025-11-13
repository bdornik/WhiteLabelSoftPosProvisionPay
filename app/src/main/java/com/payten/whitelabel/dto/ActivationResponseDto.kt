package com.payten.whitelabel.dto

data class ActivationResponseDto(
    val tid: String,
    val statusCode: String,
    val tenant : String
)