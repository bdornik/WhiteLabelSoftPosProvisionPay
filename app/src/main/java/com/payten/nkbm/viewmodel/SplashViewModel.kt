package com.payten.nkbm.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.facebook.stetho.server.http.HttpStatus
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.dto.ErrorLog
import com.payten.nkbm.dto.GenerateTokenDto
import com.payten.nkbm.event.SingleLiveEvent
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.SDKUtility
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

@HiltViewModel
class SplashViewModel @Inject constructor(


    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,

) : ViewModel() {
    private val TAG = "PinViewModel"
    private val logger = KotlinLogging.logger {}

    val getTokenSuccessfull =  MutableLiveData<Boolean>()

    val logsSendSuccess = SingleLiveEvent<Boolean>()
    val logsSendFailed = SingleLiveEvent<String>()

    val healthCheck = SingleLiveEvent<Boolean>()

    fun refreshData(isDummy: Boolean){

        if (isDummy) {
            getTokenSuccessfull.postValue(true)
            return
        }
        apiService
            .refreshToken(GenerateTokenDto(sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""), sharedPreferences.pull(SharedPreferencesKeys.USER_TID)))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                sharedPreferences.push(SharedPreferencesKeys.TOKEN, response.sessionToken)
                if (response.status != null && response.status == 0){
                    logger.info("Generate token successfull")
                    getTokenSuccessfull.postValue(true)
                }else
                    getTokenSuccessfull.postValue(false)
            }, { error ->
                logger.throwing(error)
                getTokenSuccessfull.postValue(false)
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
                    logger.info { TAG + "/healthCheck successful"}
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
    fun logError(errorLog: ErrorLog) {
        logger.info { "logError $errorLog" }
        apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                logger.info("Error Send!")
                logsSendSuccess.postValue(true)
            }, { error ->
                logger.throwing(error)
                logsSendFailed.postValue(error.message)
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
