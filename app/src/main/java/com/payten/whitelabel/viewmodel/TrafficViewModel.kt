package com.payten.whitelabel.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.payten.whitelabel.dto.*
import com.payten.whitelabel.dto.ipsTransactions.GetIpsTransactionRequest
import com.payten.whitelabel.dto.ipsTransactions.GetIpsTransactionResponse
import com.payten.whitelabel.dto.transactions.GetTransactionResponseData
import com.payten.whitelabel.dto.transactions.GetTransactionsRequest
import com.payten.whitelabel.enums.TransactionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import org.threeten.bp.LocalDateTime
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(private val apiService: SupercaseApiService) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    init {
        logger.info { "TrafficViewModel instance created: ${this.hashCode()}" }
        android.util.Log.d("TrafficViewModel", "TrafficViewModel instance created: ${this.hashCode()}")
    }

    //val transactionResultsSuccess =  MutableLiveData<GetTransactionsResult>()

    val transactionResultsSuccess =  MutableLiveData<List<TransactionDto>?>()
    val isLoading = MutableLiveData(false)

    val ipsTransactionResultsSuccess =  MutableLiveData<GetIpsTransactionResponse>()

    val cancelIpsTransactionSuccess =  MutableLiveData<Boolean>()
    val cancelIpsTransactionFailed =  MutableLiveData<Boolean>()
    val sendEmailSuccess =  MutableLiveData<Boolean>()
    val sendEmailFailed =  MutableLiveData<Boolean>()

    // Track whether transactions have been loaded to avoid reloading on navigation
    private var hasLoadedTransactions = false

    fun shouldLoadTransactions(): Boolean {
        android.util.Log.d("TrafficViewModel", "shouldLoadTransactions: $hasLoadedTransactions (returning ${!hasLoadedTransactions})")
        return !hasLoadedTransactions
    }

    fun markTransactionsAsLoaded() {
        android.util.Log.d("TrafficViewModel", "markTransactionsAsLoaded: true")
        hasLoadedTransactions = true
    }

//    fun getTransactions(inputData: GetTransactionsInputData) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val gtr = TransactionApi.doGetTransactions(inputData)
//            transactionResultsSuccess.postValue(gtr)
//        }
//    }


    private val compositeDisposable = CompositeDisposable()
    override fun onCleared() {
        super.onCleared()
        logger.info { "TrafficViewModel cleared: ${this.hashCode()}" }
        android.util.Log.d("TrafficViewModel", "TrafficViewModel cleared: ${this.hashCode()}")
        compositeDisposable.clear()
    }

    fun getTransactionsFromServer(request: GetTransactionsRequest){
        logger.info("Transaction request: $request")
        isLoading.postValue(true)
        val disposable = apiService
            .getTransaction(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Transaction successfull: $response")
                if(response.statusCode.equals("00", true)){
                    transactionResultsSuccess.postValue(prepackTransactions(response.data.transaction))
                    markTransactionsAsLoaded()
                } else {
                    transactionResultsSuccess.postValue(null)
                }
                isLoading.postValue(false)
            }, { error ->
                logger.info("Transaction Unsuccessfull: $error")
                transactionResultsSuccess.postValue(null)
                isLoading.postValue(false)
            })

        compositeDisposable.add(disposable)
    }

    private fun prepackTransactions(data: List<GetTransactionResponseData>): List<TransactionDto> {

        val resposne :ArrayList<TransactionDto> = arrayListOf()

        // Build a set of recordIds that are referenced as mainRecordId by void transactions
        // These are the original transactions that have been voided and should be filtered out
        val voidedRecordIds = data
            .filter { it.statusCode.equals("v", true) && it.mainRecordId != 0 }
            .map { it.mainRecordId }
            .toSet()

        for (transaction in data){
            logger.info { "Transaction data: $transaction" }

            // Skip this transaction if it's been voided (its recordId appears in the voidedRecordIds set)
            if (transaction.recordId in voidedRecordIds) {
                logger.info { "Skipping original transaction ${transaction.recordId} - it has been voided" }
                continue
            }

            // Map statusCode and responseCode to TransactionStatus
            val status = when {
                transaction.statusCode.equals("a", true) -> {
                    when {
                        transaction.responseCode.equals("00", true) -> com.payten.whitelabel.enums.TransactionStatus.Accepted
                        transaction.responseCode.equals("06", true) -> com.payten.whitelabel.enums.TransactionStatus.Rejected
                        transaction.responseCode.equals("17", true) -> com.payten.whitelabel.enums.TransactionStatus.WrongPin
                        else -> com.payten.whitelabel.enums.TransactionStatus.Rejected
                    }
                }
                transaction.statusCode.equals("f", true) || transaction.statusCode.equals("p", true) -> {
                    com.payten.whitelabel.enums.TransactionStatus.Rejected
                }
                transaction.statusCode.equals("v", true) -> {
                    com.payten.whitelabel.enums.TransactionStatus.Voided
                }
                transaction.statusCode.equals("s", true) -> {
                    com.payten.whitelabel.enums.TransactionStatus.PinNotEntered
                }
                transaction.statusCode.equals("d", true) -> {
                    com.payten.whitelabel.enums.TransactionStatus.Reversed
                }
                else -> com.payten.whitelabel.enums.TransactionStatus.Rejected
            }

            val trnx = TransactionDto(
                transaction.amount.toString(),
                transaction.amount,
                transaction.recordId.toString(),
                transaction.aid,
                transaction.statusCode,
                LocalDateTime.parse(transaction.transactionDate),
                transaction.responseCode,
                TransactionSource.POS,
                transaction.screenMessage,
                status,
                transaction.authorizationCode,
                transaction.maskedPAN,
                transaction.merchantId,
                false,
                transaction.RRN,
                transaction.applicationLabel,
                transaction.operationName,
                transaction.aid,
                transaction.tipAmount.toString()
            )
            logger.info { "Transaction: $trnx" }
            resposne.add(trnx)
        }
        logger.info { "Transaction response: $resposne" }
        return resposne
    }

    @SuppressLint("CheckResult")
    fun cancelIpsTransaction(request: CancelIpsTransactionDto){
        apiService
            .cancelIpsTransactions(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Cancel ips transaction successfull: $response")
                if(response.statusCode.equals("00", true)){
                    cancelIpsTransactionSuccess.postValue(true)
                } else {
                    cancelIpsTransactionFailed.postValue(true)
                }
            }, {
                cancelIpsTransactionFailed.postValue(true)
            })
    }

    @SuppressLint("CheckResult")
    fun sendEmailReport(request: SendEmailReportDto){
        apiService
            .sendEmailReport(request.dateFrom, request.dateFrom, request.email, request.fileFormat, request.terminalIdentification)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Send email successfull: $response")
                sendEmailSuccess.postValue(true)
            }, { error ->
                logger.throwing(error)
                sendEmailFailed.postValue(true)
            })
    }

    fun getIpsTransactions(userID: String, fromDate: String, toDate: String, tid: String) {
        logger.info { "Get ips transactions from: $fromDate to: $toDate" }
        val disposable = apiService
            .getIpsTransactions(GetIpsTransactionRequest(userID,fromDate,toDate,tid))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Get ips transactions successfully: $response")
                prepackIpsTransactions(response)
                ipsTransactionResultsSuccess.postValue(response)
            }, { error ->
                logger.throwing(error)

            })

        compositeDisposable.add(disposable)


    }


    private fun prepackIpsTransactions(data: GetIpsTransactionResponse){

        val resposne :ArrayList<TransactionDto> = arrayListOf()
        for (transaction in data.data){
            transaction.amount = transaction.amount.replace(",",".")
        }
        logger.info { "Transaction response: $resposne" }
    }
}
