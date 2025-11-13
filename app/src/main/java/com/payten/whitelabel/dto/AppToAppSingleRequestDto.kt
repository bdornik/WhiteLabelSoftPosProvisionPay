package com.payten.whitelabel.dto

data class AppToAppSingleRequestDto(
    val pin: String?,
    val amount: String?,
    val transactionType: String?,
    val merchantUniqueID: String,
    val transactionClass: String?,
    val packageName: String?,
    val authorizationCode: String?
)