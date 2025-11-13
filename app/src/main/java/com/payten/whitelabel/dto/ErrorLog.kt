package com.payten.whitelabel.dto

data class ErrorLog(
    var tid : String,
    var userId: String,
    var device: String,
    var os: String,
    var activity: String,
    var description: String,
    var stack: String,
    var sdkStatus: String,
    var institution: String,
    var tr: String?,
    var messages: List<Message>
)

