package com.payten.whitelabel.dto

data class Data(
    val merchantName: String?,
    val merchantPlaceName: String?,
    val merchantAddress: String?,
    val returnEnabled: String?,
    val amountLimit: String?,
    val receiptAllowed: String?,
    val services: Array<Service>,
    val mcc: String?,
    val paymentCode: String?,
    val tips: String?
)

data class Service(
    val type: String,
    val status: String,
    val serviceAccountNumber: String?,
    val defaultPaymentMethod: String?,
    val serviceMerchantId: String?,
    val serviceTerminalId: String?
)

data class DetailsResponseDto(val statusCode: String, val data: Data)