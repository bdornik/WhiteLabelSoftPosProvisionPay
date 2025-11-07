package com.payten.nkbm.dto

data class ActivationDto(
    val userId: String,
    val activationCode: String,
    val appId: String,
)