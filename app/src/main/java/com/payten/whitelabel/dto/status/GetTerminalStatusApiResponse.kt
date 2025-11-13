package com.payten.whitelabel.dto.status

data class GetTerminalStatusApiResponse(
    var statusCode: String,
    var message: String,
    var data : GetTerminalStatusData
)
