package com.payten.whitelabel.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.payten.whitelabel.dto.AppToAppResponseDto
import com.payten.whitelabel.dto.AppToAppSingleResponseDto
import com.payten.whitelabel.dto.AppToAppSingleResponseStatusDto
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.TransactionStatus
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.IpsQRScreen
import com.payten.whitelabel.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

/**
 * HeadlessIpsActivity
 *
 * Handles IPS payment transactions for app-to-app flow.
 * Shows IpsQRScreen (Compose) and returns results in app-to-app format.
 */
@AndroidEntryPoint
class HeadlessIpsActivity : ComponentActivity() {

    private val logger = KotlinLogging.logger {}
    private val TAG = "HeadlessIpsActivity"

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private var providedPackageName = ""
    private var amount = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is app-to-app flow
        if (intent.hasExtra("providedPackageName")) {
            providedPackageName = intent.getStringExtra("providedPackageName") ?: ""
            logger.info { "HeadlessIpsActivity started via app-to-app from: $providedPackageName" }
        }

        amount = intent.getStringExtra("Amount") ?: ""
        if (amount.isEmpty()) {
            Log.e(TAG, "Amount not provided")
            returnError("Amount not provided")
            return
        }

        logger.info { "HeadlessIpsActivity started - Amount: $amount" }

        setContent {
            AppTheme {
                IpsQRScreen(
                    amount = amount,
                    sharedPreferences = sharedPreferences,
                    onNavigateBack = {
                        logger.info { "IPS transaction cancelled by user" }
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onTransactionComplete = { isSuccess, statusCode, message, e2eRef ->
                        logger.info { "IPS transaction complete - Success: $isSuccess, StatusCode: $statusCode" }
                        handleTransactionComplete(isSuccess, statusCode, message, e2eRef)
                    }
                )
            }
        }
    }

    private fun handleTransactionComplete(
        isSuccess: Boolean,
        statusCode: String,
        message: String,
        e2eRef: String
    ) {
        val transactionData = TransactionDetailsDto(
            aid = "",
            applicationLabel = "",
            authorizationCode = e2eRef,
            bankName = "",
            cardNumber = "",
            dateTime = LocalDateTime.now().toString(),
            merchantId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID, ""),
            merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
            message = message,
            operationName = "IPS Payment",
            response = statusCode,
            rrn = e2eRef,
            code = e2eRef,
            status = if (isSuccess) "A" else "R",
            terminalId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, ""),
            amount = amount,
            isIps = true,
            sdkStatus = if (isSuccess) TransactionStatus.Accepted else TransactionStatus.Rejected,
            billStatus = null,
            color = if (isSuccess) 0 else -1,
            recordId = e2eRef,
            listName = "",
            tipAmount = "0.0"
        )

        if (providedPackageName.isNotEmpty()) {
            // App-to-app flow - return response in app-to-app format
            returnAppToAppResponse(transactionData)
        } else {
            // Internal flow - return normal result
            val resultIntent = Intent().apply {
                putExtra("transaction_data", transactionData)
                putExtra("success", isSuccess)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun returnAppToAppResponse(transactionData: TransactionDetailsDto) {
        val gson = Converters.registerLocalDateTime(GsonBuilder()).create()

        val status = transactionData.response
        val responseMessage = transactionData.message
        val dataResponse = gson.toJson(transactionData)

        val obj = AppToAppResponseDto(
            AppToAppSingleResponseDto(
                AppToAppSingleResponseStatusDto(
                    status!!,
                    responseMessage!!,
                    dataResponse
                ),
                transactionData.recordId ?: ""
            )
        )

        logger.info { "Sending app-to-app IPS response to $providedPackageName" }

        val intent = packageManager.getLaunchIntentForPackage(providedPackageName)
        if (intent != null) {
            intent.putExtra("RESPONSE_JSON_STRING", gson.toJson(obj))
            setResult(RESULT_OK, intent)
        } else {
            logger.error { "Package $providedPackageName not found" }
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    private fun returnError(errorMessage: String) {
        if (providedPackageName.isNotEmpty()) {
            // App-to-app flow
            val transactionData = TransactionDetailsDto(
                aid = "",
                applicationLabel = "",
                authorizationCode = "",
                bankName = "",
                cardNumber = "",
                dateTime = LocalDateTime.now().toString(),
                merchantId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID, ""),
                merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
                message = errorMessage,
                operationName = "IPS Payment",
                response = "05",
                rrn = "",
                code = "",
                status = "R",
                terminalId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, ""),
                amount = amount,
                isIps = true,
                sdkStatus = TransactionStatus.Rejected,
                billStatus = null,
                color = -1,
                recordId = "",
                listName = "",
                tipAmount = "0.0"
            )
            returnAppToAppResponse(transactionData)
        } else {
            // Internal flow
            val resultIntent = Intent().apply {
                putExtra("error_message", errorMessage)
                putExtra("success", false)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
