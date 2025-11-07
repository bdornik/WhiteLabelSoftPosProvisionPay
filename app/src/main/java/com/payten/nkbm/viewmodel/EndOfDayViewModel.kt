package com.payten.nkbm.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cioccarellia.ksprefs.KsPrefs
import com.icmp10.mtms.codes.opGetTransactions.GetTransactionsInputData
import com.icmp10.mtms.codes.opGetTransactions.GetTransactionsResult
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.dto.ErrorLog
import com.payten.nkbm.utils.SDKUtility
import com.simant.softpos.api.TransactionApi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject


@HiltViewModel
class EndOfDayViewModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    private val compositeDisposable = CompositeDisposable()
    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun logError(errorLog: ErrorLog) {
        logger.info { "logError $errorLog" }
        val disposable = apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _ ->
                logger.info("Error Send!")
            }, { error ->
                logger.throwing(error)
                logger.info("Error Not Send!")
            })

        compositeDisposable.add(disposable)

        
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