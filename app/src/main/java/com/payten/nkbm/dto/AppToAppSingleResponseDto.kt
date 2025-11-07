package com.payten.nkbm.dto

data class AppToAppSingleResponseDto(
    val status: AppToAppSingleResponseStatusDto,
    val paymentIdentificator: String
)