package com.payten.whitelabel.dto

data class EndOfDay (
    var merchantName: String? = "",
    var TID: String?  = "",
    var MID: String?  = "",
    var lastEndOfDay: String?  = "",

    var data: List<CardTraffic>? = null
)