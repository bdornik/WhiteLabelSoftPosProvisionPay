package com.payten.whitelabel.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.utils.SDKUtility
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject


@HiltViewModel
class VoidViewModel @Inject constructor (
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    val sdkProccessSuccess = MutableLiveData<Boolean>()
    val sdkProccessFailed = MutableLiveData<String>()


    fun logError(errorLog: ErrorLog) {
        logger.info { "logError $errorLog" }
        apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
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
