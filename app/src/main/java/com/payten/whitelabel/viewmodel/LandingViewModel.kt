package com.payten.whitelabel.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.dto.GenerateTokenDto
import com.payten.whitelabel.dto.status.GetTerminalStatusRequest
import com.payten.whitelabel.enums.Advice
import com.payten.whitelabel.event.SingleLiveEvent
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.SDKUtility
import com.simant.MainApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import com.payten.whitelabel.api.ApiService
import com.payten.whitelabel.api.SupercaseApiService
import javax.inject.Inject

/**
 * ViewModel for the LandingScreen, managing terminal health monitoring and merchant configuration.
 *
 * This ViewModel is responsible for:
 * - Checking terminal status and detecting reactivation requirements
 * - Refreshing session tokens
 * - Fetching and caching merchant configuration (details, services, limits)
 * - Managing payment service availability (Card/POS and IPS)
 * - Error logging and diagnostics
 *
 * ## Terminal Status Monitoring:
 *
 * On every LandingScreen entry, `getTerminalStatus()` checks:
 * 1. **Backend Terminal Status** - Server-side terminal health check
 * 2. **SDK Readiness** - Local SDK wallet initialization status
 * 3. **Reactivation Advice** - Backend directive for terminal reactivation
 *
 * ### Reactivation Triggers:
 * Terminal reactivation is required when:
 * - `sdkTerminalStatus == "A"` AND SDK wallet is NOT ready (`!isReady()`)
 *   - This indicates SDK wallet was wiped or corrupted
 * - `advice == "FORCE_REACTIVATION"`
 *   - Backend mandates reactivation (security update, configuration change, etc.)
 *
 * When reactivation is detected:
 * - Sets `reactivation` LiveData to `true`
 * - LandingScreen shows reactivation dialog
 * - User must complete reactivation flow before transactions allowed
 *
 * ## Merchant Configuration Management:
 *
 * ### refreshData() / getDetails():
 * Fetches merchant configuration from backend and caches in SharedPreferences:
 *
 * #### Merchant Information:
 * - Merchant name, place name, address
 * - MCC (Merchant Category Code)
 * - Amount limits
 * - Receipt configuration
 * - Return/refund enabled flag
 * - Tips enabled/disabled
 *
 * #### Service Configuration:
 * Parses `services` array from backend to configure payment methods:
 *
 * **CARD Service (NFC Card Payments via SDK):**
 * - Status: "100" = provisioned and active
 * - Service account number
 * - Merchant ID and Terminal ID
 * - Default payment method
 * - Stored with `POS_*` prefixed keys
 *
 * **IPS Service (QR Code Mobile Payments):**
 * - Status: "100" = provisioned and active
 * - IPS account number (IBAN for QR payments)
 * - Merchant ID and Terminal ID
 * - Default payment method
 * - Stored with `IPS_*` prefixed keys
 *
 * Service availability controls which payment methods appear in PaymentMethodScreen.
 *
 * ## Session Token Management:
 *
 * `refreshData()` refreshes the session token before fetching merchant details:
 * - Calls `refreshToken()` API with userId + terminalId
 * - Updates stored `TOKEN` in SharedPreferences
 * - Token used in `TokenInterceptor` for authenticated API calls
 * - Ensures fresh token for subsequent API operations
 *
 * ## LiveData Observers:
 *
 * ### Terminal Status:
 * - `reactivation: SingleLiveEvent<Boolean>` - Terminal requires reactivation (true/false)
 *
 * ### Configuration:
 * - `getDetailsSuccessfull: MutableLiveData<Boolean>` - Merchant config fetch success
 * - `getDetailsProvision: MutableLiveData<Boolean>` - CARD service provisioned
 *
 * ### Error Logging:
 * - `logsSendSuccess: SingleLiveEvent<Boolean>` - Error log transmission success
 * - `logsSendFailed: SingleLiveEvent<String>` - Error log transmission failure with message
 *
 * ## Stored Configuration (SharedPreferences):
 *
 * ### Merchant Details:
 * - `MERCHANT_NAME` - Business name
 * - `MERCHANT_PLACE_NAME` - Location name
 * - `MERCHANT_ADDRESS` - Physical address
 * - `MCC` - Merchant category code
 * - `PAYMENT_CODE` - Payment purpose code
 * - `TIPS` - Tips enabled flag
 * - `MERCHANT_AMOUNT_LIMIT` - Maximum transaction amount
 * - `MERCHANT_RETURN_ENABLED` - Refunds allowed
 * - `MERCHANT_RECEIPT_ALLOWED` - Receipt printing allowed
 *
 * ### Service Availability:
 * - `POS_EXISTS` - Card payment service active (status == "100")
 * - `IPS_EXISTS` - IPS QR payment service active (status == "100")
 * - `POS_STATUS` / `IPS_STATUS` - Service status codes
 *
 * ### Service Credentials:
 * - `POS_SERVICE_ACCOUNT_NUMBER` / `IPS_SERVICE_ACCOUNT_NUMBER`
 * - `POS_SERVICE_MERCHANT_ID` / `IPS_SERVICE_MERCHANT_ID`
 * - `POS_SERVICE_TERMINAL_ID` / `IPS_SERVICE_TERMINAL_ID`
 * - `POS_DEFAULT_PAYMENT_METHOD` / `IPS_DEFAULT_PAYMENT_METHOD`
 *
 * ## Error Handling:
 * - Creates comprehensive error logs with device info, SDK status, transaction records
 * - Logs terminal health check failures
 * - Logs configuration fetch errors
 *
 * @property apiService SupercaseApiService for backend API calls (Hilt injected)
 * @property sharedPreferences KsPrefs for encrypted configuration storage (Hilt injected)
 *
 * @see LandingScreen for UI that uses this ViewModel
 * @see ReactivationScreen for reactivation flow triggered by this ViewModel
 * @see PaymentMethodScreen for service availability checks
 */
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    val getDetailsSuccessfull = MutableLiveData<Boolean>()
    val getDetailsProvision = MutableLiveData<Boolean>()

    val reactivation = SingleLiveEvent<Boolean>()


    val logsSendSuccess = SingleLiveEvent<Boolean>()
    val logsSendFailed = SingleLiveEvent<String>()

    @SuppressLint("CheckResult")
    fun getTerminalStatus() {
        val userId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
        apiService
            .getTerminalStatus(GetTerminalStatusRequest(userId))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                logger.info("GetTerminalStatus response: $response")
                if (response.statusCode == "00") {
                    val isReady =
                        MainApplication.getInstance().configurationInterface.isReady
                    logger.info("isReady: $isReady")
                    if (response.data.sdkTerminalStatus == "A" && !isReady) {
                        logger.info("SDK REACTIVATION")
                        sharedPreferences.push(SharedPreferencesKeys.SDKReactivate,true)
                        reactivation.postValue(true)
                    } else if (response.data.advice == Advice.FORCE_REACTIVATION.toString()) {
                        logger.info("ADVICE REACTIVATION")
                        reactivation.postValue(true)
                    } else {
                        reactivation.postValue(false)
                    }
                }
            }, { error ->
                logger.throwing(error)
                reactivation.postValue(false)

            })
    }

    @SuppressLint("CheckResult")
    fun refreshData(isDummy: Boolean) {
        if (isDummy) {
            getDetailsSuccessfull.postValue(true)
            dummyDetails()
            return
        }
        apiService
            .refreshToken(
                GenerateTokenDto(
                    sharedPreferences.pull(
                        SharedPreferencesKeys.USER_ID,
                        ""
                    ), sharedPreferences.pull(SharedPreferencesKeys.USER_TID)
                )
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                sharedPreferences.push(SharedPreferencesKeys.TOKEN, response.sessionToken)
                logger.info("Generate token successfull")
                getDetails()
            }, { error ->
                logger.throwing(error)

            })
    }

    fun dummyDetails() {
        sharedPreferences.push(
            SharedPreferencesKeys.MERCHANT_NAME,
            "Google"
        )
        sharedPreferences.push(
            SharedPreferencesKeys.MERCHANT_PLACE_NAME,
            "Google"
        )
        sharedPreferences.push(
            SharedPreferencesKeys.MERCHANT_ADDRESS,
            "Test"
        )

    }

    @SuppressLint("CheckResult")
    fun getDetails() {

        apiService
            .getDetails()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (response.statusCode == ApiService.SUCCESS) {

                    logger.info { "RESPONSE: $response" }
                    sharedPreferences.push(SharedPreferencesKeys.POS_EXISTS, false)
                    sharedPreferences.push(SharedPreferencesKeys.IPS_EXISTS, false)


                    if (response.data.tips != null){
                        if (response.data.tips == "1")
                            sharedPreferences.push(SharedPreferencesKeys.TIPS,true) //ispraviti na true kada se steknu uslovi
                        else
                            sharedPreferences.push(SharedPreferencesKeys.TIPS,false)
                    }
                    if (response.data.merchantName != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.MERCHANT_NAME,
                            response.data.merchantName
                        )
                    }
                    if (response.data.merchantPlaceName != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.MERCHANT_PLACE_NAME,
                            response.data.merchantPlaceName
                        )
                    }
                    if (response.data.merchantAddress != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.MERCHANT_ADDRESS,
                            response.data.merchantAddress
                        )
                    }
                    if (response.data.returnEnabled != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.MERCHANT_RETURN_ENABLED,
                            response.data.returnEnabled
                        )
                    }
                    if (response.data.amountLimit != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.MERCHANT_AMOUNT_LIMIT,
                            response.data.amountLimit
                        )
                    }
                    if (response.data.receiptAllowed != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.MERCHANT_RECEIPT_ALLOWED,
                            response.data.receiptAllowed
                        )
                    }
                    if (response.data.mcc != null) {
                        sharedPreferences.push(SharedPreferencesKeys.MCC, response.data.mcc)
                    }
                    if (response.data.paymentCode != null) {
                        sharedPreferences.push(
                            SharedPreferencesKeys.PAYMENT_CODE,
                            response.data.paymentCode
                        )
                    }
                    for (service in response.data.services) {
                        if (service.type.equals("CARD", true)) {
                            if (service.status.equals("100", true)) {
                                sharedPreferences.push(SharedPreferencesKeys.POS_EXISTS, true)
                                getDetailsProvision.postValue(true)
                            } else {
                                getDetailsProvision.postValue(false)
                            }
                            sharedPreferences.push(SharedPreferencesKeys.POS_STATUS, service.status)
                            if (service.serviceAccountNumber != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.POS_SERVICE_ACCOUNT_NUMBER,
                                    service.serviceAccountNumber
                                )
                            }
                            if (service.defaultPaymentMethod != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.POS_DEFAULT_PAYMENT_METHOD,
                                    service.defaultPaymentMethod
                                )
                            }
                            if (service.serviceMerchantId != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID,
                                    service.serviceMerchantId
                                )
                            }
                            if (service.serviceTerminalId != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID,
                                    service.serviceTerminalId
                                )
                            }
                        } else {
                            if (service.status.equals("100", true)) {
                                sharedPreferences.push(SharedPreferencesKeys.IPS_EXISTS, true) //ispraviti na true kada se steknu uslovi
                            }
                            sharedPreferences.push(SharedPreferencesKeys.IPS_STATUS, service.status)
                            if (service.serviceAccountNumber != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.IPS_SERVICE_ACCOUNT_NUMBER,
                                    service.serviceAccountNumber
                                )
                            }
                            if (service.defaultPaymentMethod != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.IPS_DEFAULT_PAYMENT_METHOD,
                                    service.defaultPaymentMethod
                                )
                            }
                            if (service.serviceMerchantId != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID,
                                    service.serviceMerchantId
                                )
                            }
                            if (service.serviceTerminalId != null) {
                                sharedPreferences.push(
                                    SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID,
                                    service.serviceTerminalId
                                )
                            }
                        }
                    }

                    getDetailsSuccessfull.postValue(true)
                } else {
                    getDetailsSuccessfull.postValue(false)
                }
                logger.info("Get details successfull")
            }, { error ->
                logger.throwing(error)
                getDetailsSuccessfull.postValue(false)
            })

    }

    @SuppressLint("CheckResult")
    fun logError(errorLog: ErrorLog, dialog: Boolean) {
        logger.info { "logError $errorLog" }
        apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _ ->
                logger.info("Error Send!")
                if (dialog)
                    logsSendSuccess.postValue(true)
            }, { error ->
                logger.throwing(error)
                logsSendFailed.postValue(error.message)
                logger.info("Error Not Send!")
            })
    }

    fun createErrorLog(
        tid: String,
        userId: String,
        error: String,
        description: String,
        context: Context
    ): ErrorLog {
        val status = SDKUtility.logSecurityStatus(context)
        return ErrorLog(
            tid,
            userId,
            Build.MANUFACTURER + ":" + Build.MODEL,
            Build.VERSION.SDK_INT.toString(),
            this.javaClass.simpleName,
            description,
            error,
            status,
            SupercaseConfig.INSTITUTION,
            SDKUtility.getTR(),
            SDKUtility.getModulesLogsMessage()
        )
    }


}