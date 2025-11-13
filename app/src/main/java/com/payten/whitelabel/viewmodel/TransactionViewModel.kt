package com.payten.whitelabel.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailResponseData
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsRequest
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsResponseDataList
import com.payten.whitelabel.event.SingleLiveEvent
import com.payten.whitelabel.utils.SDKUtility
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject


@HiltViewModel
class TransactionViewModel @Inject constructor (
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    val transactionResultsSuccess =  SingleLiveEvent<GetTransactionDetailResponseData?>()


    private val compositeDisposable = CompositeDisposable()



    fun getTransactionDetailFromServer(request: GetTransactionDetailsRequest){
        logger.info("Transaction request: ${request}")
        apiService
            .getTransactionDetails(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Transaction successfull: ${response}")
                if(response.statusCode.equals("00", true)){
                    transactionResultsSuccess.postValue(prepackTransactions(response.data))
                } else {
                    transactionResultsSuccess.postValue(null)
                }
            }, { error ->
                logger.info("Transaction Unsuccessfull: ${error}")
                transactionResultsSuccess.postValue(null)
            })
    }

    private fun prepackTransactions(data: GetTransactionDetailsResponseDataList): GetTransactionDetailResponseData? {
        logger.info { "TransactionDetails response: ${data.transaction}" }
        if (data != null && data.transaction.isNotEmpty()){
            for (transaction in data.transaction){
                logger.info { "Transaction data: " + transaction }
                return transaction
            }
        }

        return null
    }

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
