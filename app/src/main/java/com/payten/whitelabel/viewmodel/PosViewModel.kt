package com.payten.whitelabel.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.utils.SDKUtility
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import com.payten.whitelabel.api.SupercaseApiService
import javax.inject.Inject

/**
 * ViewModel for payment transaction processing and error logging.
 *
 * This lightweight ViewModel is used by HeadlessPaymentActivity to handle payment SDK
 * errors and diagnostic logging. It provides utilities for creating comprehensive error
 * reports and transmitting them to the backend for debugging and monitoring.
 *
 * ## Primary Responsibilities:
 * - Creating detailed error logs during payment transactions
 * - Transmitting error logs to backend API
 * - Capturing SDK diagnostics and security status
 * - Including transaction records in error reports
 *
 * ## Error Log Contents:
 *
 * When creating an error log via `createErrorLog()`, the following information is captured:
 *
 * ### Device Information:
 * - Manufacturer and model
 * - Android SDK version
 *
 * ### Terminal Information:
 * - Terminal ID (TID)
 * - User ID
 * - Institution name
 *
 * ### Error Details:
 * - Error message/description
 * - Error category (description parameter)
 * - Source class name (ViewModel)
 *
 * ### SDK Diagnostics:
 * - Security status (via `SDKUtility.logSecurityStatus()`)
 * - Transaction records (via `SDKUtility.getTR()`)
 * - Module logs (via `SDKUtility.getModulesLogsMessage()`)
 *
 * ## LiveData Observers:
 * - `sdkProccessSuccess: MutableLiveData<Boolean>` - SDK processing success (currently unused)
 * - `sdkProccessFailed: MutableLiveData<String>` - SDK processing failure with error message (currently unused)
 *
 * These LiveData observers are declared but not actively used in current implementation.
 * Error logging happens asynchronously without UI feedback.
 *
 * ## Backend Integration:
 * Error logs are transmitted to the backend via `apiService.errorLog()` API endpoint.
 * The backend stores these logs for:
 * - Debugging payment failures
 * - Monitoring SDK health
 * - Analyzing security status trends
 * - Terminal diagnostics
 *
 * @property apiService SupercaseApiService for backend API calls (Hilt injected)
 * @property sharedPreferences KsPrefs for credential access (Hilt injected)
 *
 * @see HeadlessPaymentActivity for primary usage
 * @see SDKUtility for SDK diagnostics utilities
 * @see ErrorLog for error log DTO structure
 */
@HiltViewModel
class PosViewModel @Inject constructor (
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    private val logger = KotlinLogging.logger {}


    @SuppressLint("CheckResult")
    fun logError(errorLog: ErrorLog) {
        logger.info { "logError $errorLog" }
        apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _ ->
                logger.info("Error Send!")
            }, { error ->
                logger.throwing(error)
                logger.info("Error Not Send!")
            })
    }

    fun createErrorLog(
        tid:String,
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
