package com.payten.whitelabel.dto

data class AppToAppSingleResponseDto(
    val status: AppToAppSingleResponseStatusDto,
    val paymentIdentificator: String
)