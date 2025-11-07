package com.payten.nkbm.dto

data class SendEmailReportDto(
    val terminalIdentification: String,
    val dateFrom: String,
    val dateTo: String,
    val email: String,
    val fileFormat: String,
)