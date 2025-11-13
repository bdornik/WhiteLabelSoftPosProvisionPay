package com.payten.whitelabel.persistance

class SharedPreferencesKeys {
    companion object{
        var TIPS = "tips"
        var END_OF_DAY_DATE = "EndOfDayDate"
        var SDKReactivate = "SDKReactivate"
        var TENANT = "tenant"
        var HOST_X = "hostX"
        var HOST_Y = "hostY"
        var DUMMY = "dummy"
        var PIN = "Pin"
        var FILTER_TYPE = "TransactionType"
        var FILTER_STATUS = "TransactionStatus"
        var FILTER_SORT = "TransactionSorted"
        var DATE_FROM = "DateFrom"
        var DATE_TO = "DateTo"
        var FIREBASE_TOKEN = "firebaseToken"
        var REGISTERED = "Registered"
        var REGISTRATION_USER_ID = "RegistrationUserId"
        var HASHED_ACT_ID = "HashedActId"
        var IS_REGISTERED = "IsRegistered"
        var IS_LOGGED_IN = "IsLoggedIn"
        var PIN_COUNT = "PinCount"
        var IS_BLACKLISTED = "IsBlacklisted"
        var TOKEN = "Token"
        val APP_ID = "AppId"
        val USER_ID = "UserId"
        val USER_ACTIVATION_CODE = "UserActivationCode"
        val USER_TID = "UserTid"
        val MERCHANT_NAME = "MerchantName"
        val MCC = "Mcc"
        val MERCHANT_PLACE_NAME = "MerchantPlaceName"
        val MERCHANT_ADDRESS = "MerchantAddress"
        val MERCHANT_RETURN_ENABLED = "MerchantReturnEnabled"
        val MERCHANT_AMOUNT_LIMIT = "MerchantAmountLimit"
        val MERCHANT_RECEIPT_ALLOWED = "MerchantReceiptAllowed"
        val PAYMENT_CODE = "PaymentCode"

        val POS_EXISTS = "PosExists"
        val IPS_EXISTS = "IpsExists"

        val POS_STATUS = "PosStatus"
        val POS_SERVICE_ACCOUNT_NUMBER = "PosServiceAccountNumber"
        val POS_DEFAULT_PAYMENT_METHOD = "PosDefaultPaymentMethod"
        val POS_SERVICE_MERCHANT_ID = "PosServiceMerchantId"
        val POS_SERVICE_TERMINAL_ID = "PosServiceTerminalId"

        val IPS_STATUS = "IPSStatus"
        val IPS_SERVICE_ACCOUNT_NUMBER = "IPSServiceAccountNumber"
        val IPS_DEFAULT_PAYMENT_METHOD = "IPSDefaultPaymentMethod"
        val IPS_SERVICE_MERCHANT_ID = "IPSServiceMerchantId"
        val IPS_SERVICE_TERMINAL_ID = "IPSServiceTerminalId"

        val LAST_TRANSACTION_AUTH_CODE = "lastTransactionAuthCode"

        val IS_DARK_MODE = "IsDarkMode"

        val COUNTER = "Counter"
        val LANGUAGE = "Language"

        val APP_BLOCKED = "App_Blocked"

        val ATTEMPT_NUMBER = "Number_Attempts"

    }
}