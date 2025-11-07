package com.payten.nkbm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.dto.*
import com.payten.nkbm.dto.ipsTransactions.GetIpsTransactionRequest
import com.payten.nkbm.dto.ipsTransactions.GetIpsTransactionResponse
import com.payten.nkbm.dto.transactions.GetTransactionResponseData
import com.payten.nkbm.dto.transactions.GetTransactionsRequest
import com.payten.nkbm.enums.TransactionSource
import com.payten.nkbm.utils.AmountUtil
import com.payten.nkbm.utils.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import org.threeten.bp.LocalDateTime
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject

@HiltViewModel
class TrafficViewModel @Inject constructor(private val apiService: SupercaseApiService, private val sharedPreferences: KsPrefs,) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    //val transactionResultsSuccess =  MutableLiveData<GetTransactionsResult>()

    val transactionResultsSuccess =  MutableLiveData<List<TransactionDto>?>()

    val ipsTransactionResultsSuccess =  MutableLiveData<GetIpsTransactionResponse>()

    val cancelIpsTransactionSuccess =  MutableLiveData<Boolean>()
    val cancelIpsTransactionFailed =  MutableLiveData<Boolean>()
    val sendEmailSuccess =  MutableLiveData<Boolean>()
    val sendEmailFailed =  MutableLiveData<Boolean>()

//    fun getTransactions(inputData: GetTransactionsInputData) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val gtr = TransactionApi.doGetTransactions(inputData)
//            transactionResultsSuccess.postValue(gtr)
//        }
//    }


    private val compositeDisposable = CompositeDisposable()
    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun getTransactionsFromServer(request: GetTransactionsRequest){
        logger.info("Transaction request: ${request}")
        apiService
            .getTransaction(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Transaction successfull: ${response}")
                if(response.statusCode.equals("00", true)){
                    transactionResultsSuccess.postValue(prepackTransactions(response.data.transaction))
                } else {
                    transactionResultsSuccess.postValue(null)
                }
            }, { error ->
                logger.info("Transaction Unsuccessfull: ${error}")
                transactionResultsSuccess.postValue(null)
            })
    }

    private fun prepackTransactions(data: List<GetTransactionResponseData>): List<TransactionDto> {

        val resposne :ArrayList<TransactionDto> = arrayListOf()
        for (transaction in data){
            logger.info { "Transaction data: " + transaction }
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
                null,
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
            logger.info { "Transaction: " + trnx }
            resposne.add(trnx)
        }
        logger.info { "Transaction response: " + resposne }
        return resposne
    }

    fun cancelIpsTransaction(request: CancelIpsTransactionDto){
        apiService
            .cancelIpsTransactions(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Cancel ips transaction successfull: ${response}")
                if(response.statusCode.equals("00", true)){
                    cancelIpsTransactionSuccess.postValue(true)
                } else {
                    cancelIpsTransactionFailed.postValue(true)
                }
            }, { error ->
                cancelIpsTransactionFailed.postValue(true)
            })
    }

    fun sendEmailReport(request: SendEmailReportDto){
        apiService
            .sendEmailReport(request.dateFrom, request.dateFrom, request.email, request.fileFormat, request.terminalIdentification)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Send email successfull: ${response}")
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
