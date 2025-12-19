package com.payten.whitelabel.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ActivationDto
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.dto.GenerateTokenDto
import com.payten.whitelabel.dto.LogSend
import com.payten.whitelabel.dto.keys.GetKeysRequestDto
import com.payten.whitelabel.enums.ErrorDescription
import com.payten.whitelabel.event.SingleLiveEvent
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.DebugSdk
import com.payten.whitelabel.utils.SDKUtility
import com.sacbpp.api.SACBTPApplication
import com.sacbpp.codes.SACBPPError
import com.sacbpp.core.bytes.ByteArrayFactory
import com.sacbpp.ui.CMSSecureActivationListener
import com.sacbpp.ui.InitializationListener
import com.simant.MainApplication
import com.simant.sample.SimantApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.digitalworx.takt.api.ApiService
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject
import androidx.core.content.edit

/**
 * ViewModel for merchant registration and terminal activation workflows.
 *
 * This ViewModel orchestrates the complex registration process that involves both backend
 * API calls and payment SDK initialization. It handles:
 * - Merchant/terminal activation with Payten backend
 * - Host cryptographic key retrieval (ECDSA public keys)
 * - Session token generation
 * - Payment SDK (SoftPOS) wallet initialization and activation
 * - Terminal reactivation when required
 * - Error logging and health checks
 *
 * ## Registration Flow (New Merchant):
 * 1. **healthCheck()** - Verify backend API connectivity
 * 2. **activate(userId, activationCode, appId)** - Register terminal with backend
 *    - Receives TID (Terminal ID) and stores credentials
 *    - Verifies SDK status is ready (status == 0)
 * 3. **getHostKeys(request)** - Retrieve ECDSA public key components (hostX, hostY)
 *    - Keys used for secure SDK wallet activation
 * 4. **generateToken(userId, tid)** - Get session token for authenticated API calls
 * 5. **initializeMTA(application, userId, userCode)** - Initialize payment SDK
 *    - Triggers `onRegistrationNeeded()` callback
 * 6. **registerOnSDK(userId, activationCode, pin)** - Complete SDK wallet activation
 *    - Uses host keys for cryptographic initialization
 *    - Wallet activation enables NFC payment processing
 *
 * ## Reactivation Flow (Existing Terminal):
 * 1. **reactivation(tid)** - When backend requires terminal reactivation
 *    - Calls `localWipeWallet()` to clear existing SDK wallet
 *    - Re-registers terminal with backend (receives new activation code)
 *    - Stores new credentials in SharedPreferences
 *    - Returns to step 5-6 of registration flow to reinitialize SDK
 *
 * ## SDK Integration:
 * This ViewModel implements `InitializationListener` to receive callbacks from the
 * payment SDK during initialization:
 * - `onRegistrationNeeded()` - SDK requires wallet activation, calls `registerOnSDK()`
 * - `onMPAReady()` - Multi-Payment Application ready
 * - `onError(error)` - SDK initialization errors
 *
 * The SDK activation uses `CMSSecureActivationListener` for wallet activation callbacks:
 * - `onWalletActivated()` - Wallet successfully initialized, ready for payments
 * - `onActivationError(message)` - Wallet activation failed
 * - `onActivationStarted()` - Wallet activation process started
 * - `onNetWorkError()` / `onCryptoError()` - Specific activation failures
 *
 * ## LiveData Observers:
 *
 * ### Registration/Activation:
 * - `paytenActivationSuccessfull` - Backend terminal activation success
 * - `paytenActivationFailed` - Backend terminal activation failure
 * - `paytenGetHostKeys` - Host keys retrieval success/failure
 * - `paytenGenerateTokenSuccessfull` - Session token generation success
 * - `paytenGenerateTokenFailed` - Session token generation failure
 * - `sdkRegisterSuccess` - SDK wallet activation success
 * - `sdkRegisterFailed` - SDK wallet activation failure (with error message)
 * - `reactivationSuccess` - Terminal reactivation success/failure
 *
 * ### Monitoring:
 * - `healthCheck` - Backend API health status
 * - `logsSendSuccess` / `logsSendFailed` - Error log transmission status
 *
 * ## Stored Credentials (SharedPreferences):
 * - `USER_ID` - Merchant user identifier
 * - `USER_TID` - Terminal ID (assigned by backend)
 * - `USER_ACTIVATION_CODE` - Terminal activation code
 * - `APP_ID` - Application identifier
 * - `TOKEN` - Session authentication token
 * - `HOST_X` / `HOST_Y` - ECDSA public key components for SDK initialization
 * - `REGISTRATION_USER_ID` - Terminal user ID for reactivation
 *
 * ## Error Handling:
 * - Validates SDK status before activation (must be 0)
 * - Logs errors to backend via `errorLog()` API
 * - Creates detailed error logs including device info, SDK status, transaction records
 * - Uses `DebugSdk.getSDKDetailedInfo()` for comprehensive SDK diagnostics
 *
 * @property apiService SupercaseApiService for backend API calls (Hilt injected)
 * @property sharedPreferences KsPrefs for encrypted credential storage (Hilt injected)
 * @property userId Merchant user ID (set during initialization)
 * @property userCode Terminal activation code (set during initialization)
 *
 * @see RegistrationPage for UI that uses this ViewModel
 * @see ReactivationScreen for reactivation flow UI
 * @see MainApplication for SDK initialization entry point
 * @see SupercaseApiService for backend API interface
 */
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel(), InitializationListener {

    private val TAG = "RegistrationViewModel"
    private val logger = KotlinLogging.logger {}


    val paytenActivationSuccessfull = SingleLiveEvent<Boolean>()
    val paytenActivationFailed = SingleLiveEvent<Boolean>()
    val paytenGenerateTokenSuccessfull = MutableLiveData<Boolean>()
    val paytenGenerateTokenFailed = MutableLiveData<Boolean>()
    val paytenGetHostKeys = MutableLiveData<Boolean>()
    val sdkRegisterSuccess = MutableLiveData<Boolean>()
    val sdkRegisterFailed = MutableLiveData<String>()
    val reactivationSuccess = SingleLiveEvent<Boolean>()

    val logsSendSuccess = SingleLiveEvent<LogSend>()
    val logsSendFailed = SingleLiveEvent<LogSend>()

    val healthCheck = SingleLiveEvent<Boolean>()

    var userId = ""
    var userCode = ""

    @SuppressLint("CheckResult")
    fun reactivation(tid: String){
        val app: SACBTPApplication = MainApplication.getSACBTPApplication()
        app.localWipeWallet()



        val s = DebugSdk.getSDKDetailedInfo(sharedPreferences)
        logger.info { "PORUKA: $s" }

        apiService
            .reactivation(tid)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                logger.info { "Reactivation response: $response" }
                if (response.statusCode.equals(ApiService.SUCCESS, true)) {
                    sharedPreferences.push(SharedPreferencesKeys.USER_TID, response.data.terminalId)
                    sharedPreferences.push(
                        SharedPreferencesKeys.USER_ACTIVATION_CODE,
                        response.data.terminalActivationCode
                    )
                    sharedPreferences.push(SharedPreferencesKeys.REGISTRATION_USER_ID, response.data.terminalUserId)

                    reactivationSuccess.postValue(true)
                } else {
                    reactivationSuccess.postValue(false)
                }
                logger.info("Reactivation successfull")
            }, { error ->
                logger.throwing(error)
                reactivationSuccess.postValue(false)
            })
    }

    fun healthCheck(){
        logger.info { "health check" }
        val call: Call<Void> = apiService.healthCheck()

        logger.info { "health check: $call" }
        call.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    // Log success message if HTTP status code is in the range of 200-299
                    logger.info { "$TAG/healthCheck successful" }
                    healthCheck.postValue(true)
                } else {
                    // Log failure message with HTTP status code
                    logger.info { TAG + "/healthCheck failed with code: " + response.code()}
                    healthCheck.postValue(false)
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                // Log error message
                logger.info { TAG +"Failed to make /healthCheck API call" + t}
                healthCheck.postValue(false)
            }
        })
    }

    @SuppressLint("CheckResult")
    fun activate(userId: String, activationCode: String, appId: String, context: Context, sharedPreferencesSOFT: SharedPreferences) {
        try {
            val sdkStatus = SimantApplication.getSDKStatus()

            logger.error { "SDK Status: $sdkStatus" }


            if (sdkStatus == 0) {
                apiService
                    .activate(ActivationDto(userId, activationCode, appId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        logger.info { "Activation response: $response" }
                        if (response.statusCode.equals(ApiService.SUCCESS, true)) {
                            sharedPreferences.push(SharedPreferencesKeys.USER_TID, response.tid)
                            sharedPreferences.push(
                                SharedPreferencesKeys.USER_ACTIVATION_CODE,
                                activationCode
                            )

                            context.getSharedPreferences(
                                "SOFTPOS_PARAMETERS_MDI",
                                Context.MODE_PRIVATE
                            ).edit { putString("TENANT", response.tenant) }

                            sharedPreferences.push(SharedPreferencesKeys.USER_ID, userId)
                            sharedPreferences.push(SharedPreferencesKeys.APP_ID, appId)

                            paytenActivationSuccessfull.postValue(true)
                        } else {
                            paytenActivationFailed.postValue(true)
                        }
                        logger.info("Activation successfull")
                    }, { error ->
                        logger.throwing(error)
                        paytenActivationFailed.postValue(true)
                    })

            } else {
                logError(
                    createErrorLog(
                        userId,
                        "SdkStatus: $sdkStatus",
                        ErrorDescription.sdkStatus.name,
                        context
                    ),false
                )
                paytenActivationFailed.postValue(true)
            }

        } catch (ex: Exception) {
            logError(
                createErrorLog(
                    userId,
                    ex.message.toString(),
                    ErrorDescription.sdkStatus.name,
                    context
                )
                ,false
            )
            paytenActivationFailed.postValue(true)
        }

    }

    @SuppressLint("CheckResult")
    fun getHostKeys(request : GetKeysRequestDto){
        logger.info { "getHostKeys request: $request" }
        apiService.getKeys(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (response.statusCode == "00"){
                    logger.info{ "response: $response" }
                    logger.info { "Keys: " + response.data.keyX + " : " + response.data.keyY  }
                    sharedPreferences.push(SharedPreferencesKeys.HOST_X, response.data.keyX)
                    sharedPreferences.push(SharedPreferencesKeys.HOST_Y, response.data.keyY)

                    paytenGetHostKeys.postValue(true)
                }else{
                    paytenGetHostKeys.postValue(false)
                }

            }, { error ->
                logger.throwing(error)
                paytenGetHostKeys.postValue(false)
            })
    }

    @SuppressLint("CheckResult")
    fun generateToken(userId: String, tid: String) {
        logger.info { "Generating token..." }
        apiService
            .refreshToken(GenerateTokenDto(userId, tid))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                sharedPreferences.push(SharedPreferencesKeys.TOKEN, response.sessionToken)
                logger.info("Generate token successfull")
                paytenGenerateTokenSuccessfull.postValue(true)
            }, { error ->
                logger.throwing(error)
                paytenGenerateTokenSuccessfull.postValue(false)
            })
    }

    fun registerOnSDK(usrId: String, cmsMPAActv: String) {
        val cmsSDKVersion = SACBTPApplication.getCMSSDKVersion()

        MainApplication.getSAMTAApplication().initCMSSecureEC(
            usrId,
            cmsMPAActv,
            cmsSDKVersion.trim { it <= ' ' },
            object : CMSSecureActivationListener {
                override fun onWalletActivated() {
                    logger.info { "onWalletActivated" }
                    val s = DebugSdk.getSDKDetailedInfo(sharedPreferences)
                    logger.info { "PORUKA: $s" }
                    sdkRegisterSuccess.postValue(true)
                }

                override fun onActivationError(message: String) {
                    logger.error { "onActivationError $message" }
                    sdkRegisterFailed.postValue(message)
                }

                override fun onActivationStarted() {
                    logger.info { "onActivationStarted" }
                }

                override fun onNetWorkError() {
                    logger.error { "onNetworkError" }
                }

                override fun onCryptoError() {
                    logger.error { "onCrypteError" }
                }
            },
            ByteArrayFactory.getInstance().fromHexString(sharedPreferences.pull(SharedPreferencesKeys.HOST_X)),
            ByteArrayFactory.getInstance().fromHexString(sharedPreferences.pull(SharedPreferencesKeys.HOST_Y))
//            SupercaseConfig.hostX,
//            SupercaseConfig.hostY
        )
    }

    fun initializeMta(
        application: MainApplication,
        userId: String,
        userCode: String,
        context: Context
    ) {

        this.userId = userId
        this.userCode = userCode
        logger.info { "initializeMTA: $userId | $userCode" }
        try {
            application.initializeMTA(this)
        } catch (ex: Exception) {
            logError(
                createErrorLog(
                    userId,
                    ex.message.toString(),
                    ErrorDescription.initializeMta.name,
                    context
                ),false
            )
            sdkRegisterFailed.postValue(ex.message)
        }
    }

    override fun onRegistrationNeeded() {
        logger.info { "onRegistrationNeeded" }
        registerOnSDK(userId, userCode)
    }

    override fun onError(p0: SACBPPError?) {
        logger.info { "onError" }
        logger.error { p0 }
    }

    override fun onMPAReady() {
        logger.info { "onMPAReady" }
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
                logsSendSuccess.postValue(LogSend(dialog,"", true))
            }, { error ->
                logger.throwing(error)
                logger.info("Error Not Send!")
                logsSendFailed.postValue(LogSend(dialog,error.message.toString(), false))
            })
    }

    fun createErrorLog(
        userId: String,
        error: String,
        description: String,
        context: Context
    ): ErrorLog {
        val status = SDKUtility.logSecurityStatus(context)
        return ErrorLog(
            "",
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
