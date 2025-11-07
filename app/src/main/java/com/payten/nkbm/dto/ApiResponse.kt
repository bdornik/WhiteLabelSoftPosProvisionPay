package com.payten.nkbm.dto

data class ApiResponse(val status: Int,
                          val error: String,
                          val sessionToken: String,
                          val statusCode: String)
