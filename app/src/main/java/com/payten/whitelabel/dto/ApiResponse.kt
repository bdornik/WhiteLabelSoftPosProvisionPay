package com.payten.whitelabel.dto

data class ApiResponse(val status: Int,
                          val error: String,
                          val sessionToken: String,
                          val statusCode: String)
