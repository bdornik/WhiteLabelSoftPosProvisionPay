package com.payten.whitelabel.viewmodel

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
import rs.digitalworx.takt.api.ApiService
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject

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

    fun getTerminalStatus() {
        var userId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
        apiService
            .getTerminalStatus(GetTerminalStatusRequest(userId))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                logger.info("GetTerminalStatus response: " + response)
                if (response.statusCode.equals("00")) {
                    var isReady =
                        MainApplication.getInstance().getConfigurationInterface().isReady()
                    logger.info("isReady: " + isReady)
                    if (response.data.sdkTerminalStatus.equals("A") && !isReady) {
                        logger.info("SDK REACTIVATION")
                        sharedPreferences.push(SharedPreferencesKeys.SDKReactivate,true)
                        reactivation.postValue(true)
                    } else if (response.data.advice.equals(Advice.FORCE_REACTIVATION.toString())) {
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

    fun getDetails() {

        apiService
            .getDetails()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (response.statusCode.equals(ApiService.SUCCESS)) {

                    logger.info { "RESPONSE: ${response}" }
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

    fun logError(errorLog: ErrorLog, dialog: Boolean) {
        logger.info { "logError $errorLog" }
        apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
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