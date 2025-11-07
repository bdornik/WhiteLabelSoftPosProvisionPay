package com.payten.nkbm.dto.status

data class GetTerminalStatusApiResponse(
    var statusCode: String,
    var message: String,
    var data : GetTerminalStatusData
)
