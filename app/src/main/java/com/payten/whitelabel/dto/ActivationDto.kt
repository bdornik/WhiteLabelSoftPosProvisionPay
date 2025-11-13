package com.payten.whitelabel.dto

data class ActivationDto(
    val userId: String,
    val activationCode: String,
    val appId: String,
)