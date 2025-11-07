package com.payten.nkbm.dto

data class CardTraffic(
    var type:String?="",

    var purchaseNumber: Int? = 0,
    var purchase: Double? = 0.00,

    var cancelPurchaseNumber: Int? = 0,
    var cancelPurchase: Double? = 0.00,

    var tipNumber: Int? = 0,
    var tipAmount: Double? = 0.00,

    var returnNumber: Int? = 0,
    var returnPurchase: Double? = 0.00,

    var cancelReturnNumber: Int? = 0,
    var cancelReturnPurchase: Double? = 0.00,

    var totalNumber: Int? = 0,
    var total: Double? = 0.00
)