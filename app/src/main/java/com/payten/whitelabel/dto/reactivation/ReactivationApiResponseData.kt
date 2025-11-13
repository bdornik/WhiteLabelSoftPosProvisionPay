package com.payten.whitelabel.dto.reactivation

data class ReactivationApiResponseData(
    var terminalId: String,
    var terminalUserId: String,
    var terminalActivationCode :String,
    var phoneNumber: String,
    var services: String
)
