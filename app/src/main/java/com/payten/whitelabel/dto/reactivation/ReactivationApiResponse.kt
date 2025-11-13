package com.payten.whitelabel.dto.reactivation

data class ReactivationApiResponse(
    var statusCode: String,
    var message : String,
    var data: ReactivationApiResponseData
)
