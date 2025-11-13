package com.payten.whitelabel.dto

data class CardTrafficPrint(
    var type:String?="",

    var purchaseNumber: String? = "",
    var purchase: String? = "",

    var cancelPurchaseNumber: String? = "",
    var cancelPurchase: String? = "",

    var returnNumber: String? = "",
    var returnPurchase: String? = "",

    var cancelReturnNumber: String? = "",
    var cancelReturnPurchase: String? = "",

    var tipNumber: String? = "",
    var tipAmount: String? = "",

    var totalNumber: String? = "",
    var total: String? = ""
)