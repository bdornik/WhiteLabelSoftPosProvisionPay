package com.payten.whitelabel.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.dto.*
import com.payten.whitelabel.utils.IpsUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import rs.digitalworx.takt.api.SupercaseApiService
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class IPSShowQRViewModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    enum class Tags {
        K, V, C, R, N, I, O, P, SF, S, M, JS, RK, RO, RL, RP
    }
    private val logger = KotlinLogging.logger {}

    val checkTransactionSuccessfull =  MutableLiveData<Boolean>()
    val checkTransactionFailed =  MutableLiveData<String>()
    val payTransactionSuccess =  MutableLiveData<Boolean>()
    val payTransaction =  MutableLiveData<CheckTransactionResponseDto>()
    val payTransactionFailed =  MutableLiveData<String>()

    val VALUE_DELIMITER = ":"
    val LINE_DELIMITER = "|"
    val MAX_ACC_CAR_NO = 18

    private val disposables = CompositeDisposable()


    fun createQrCodeString(qr: QRDto): String {


        return IpsUtil.generateQrCodeContent(qr)
    }

    fun disposeAllChecks(){
        disposables.dispose()
    }

    fun check(creditTransferIndicator: String, terminalIdentificator: String, creditTransferAmount: String, qrCodeString: String) {
//        disposables.dispose()

        var request = CheckTransferRequest(creditTransferIndicator, terminalIdentificator, creditTransferAmount,qrCodeString,"")
        logger.info { "Request: $request" }


        val disp = apiService
            .checkCTSStatus(request)
            .timeout(210, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Check response: $response")
                if(response.statusCode.equals("00", true)){
                    payTransaction.postValue(response)
                } else if(response.statusCode.equals("85", true)){
//                    checkTransactionFailed.postValue(response.statusCode)
                } else {
                    payTransaction.postValue(response)
                }
            }, { error ->
                logger.throwing(error)
                checkTransactionFailed.postValue(error.localizedMessage)
            })

        disposables.add(disp)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

    fun pay(dto: PayTransactionDto) {
        logger.info { "Invoking pay $dto" }
        val disposable = apiService
            .payTransaction(dto)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info { "Response Pay api: ${response}" }
                payTransaction.postValue(response)
            }, { error ->
                logger.throwing(error)
                payTransactionFailed.postValue(error.localizedMessage)
            })
    }


    fun createQrCodeKeyValueMapFromString(qrDecoded: String?): Map<String, String>? {
        val stringTokenizer = StringTokenizer(qrDecoded, LINE_DELIMITER)
        val qrKeyValueMap: MutableMap<String, String> = HashMap()
        while (stringTokenizer.hasMoreElements()) {
            val tokenValue = stringTokenizer.nextToken()
            if (tokenValue.contains(VALUE_DELIMITER)) {
                val qrCodeValue: String =
                    tokenValue.substring(tokenValue.lastIndexOf(VALUE_DELIMITER) + 1)
                val qrCodeKey =
                    tokenValue.substring(0, tokenValue.indexOf(VALUE_DELIMITER))
                qrKeyValueMap[qrCodeKey] = qrCodeValue
            }
        }
        return qrKeyValueMap
    }

    fun containsRequiredTags(qrValues: Map<String, String?>): Boolean {
        val tagSet = qrValues.keys
        val requiredTags: MutableList<String> = ArrayList()
        requiredTags.add(Tags.K.toString())
        requiredTags.add(Tags.V.toString())
        requiredTags.add(Tags.C.toString())
        requiredTags.add(Tags.O.toString())
        return if (tagSet.containsAll(requiredTags)) {
            true
        } else false
    }

    fun createQRcodeObjectFromValues(qrValues: Map<String, String?>): QR? {
        val qr = QR()
        for (key in qrValues.keys) {
            when (key.trim { it <= ' ' }) {
                "K" -> {
                    qr.setIdentificationCode(qrValues[key])
                }
                "V" -> {
                    qr.setVersion(qrValues[key])
                }
                "C" -> {
                    qr.setCharSet(qrValues[key])
                }
                "R" -> {
                    qr.setRecepientAccNumber(qrValues[key])
                }
                "N" -> {
                    qr.setRecepientNameAndPlace(qrValues[key])
                }
                "I" -> {
                    qr.setAmountAndCurrency(qrValues[key])
                }
                "O" -> {
                    qr.setAccNumber(qrValues[key])
                }
                "P" -> {
                    qr.setNameAndPlace(qrValues[key])
                }
                "SF" -> {
                    qr.setPaymentCode(qrValues[key])
                }
                "S" -> {
                    qr.setPaymentPurpose(qrValues[key])
                }
                "M" -> {
                    qr.setMcc(qrValues[key])
                }
                "JS" -> {
                    qr.setOneTimePaymentPassword(qrValues[key])
                }
                "RK" -> {
                    qr.setPayerReference(qrValues[key])
                }
                "RO" -> {
                    qr.setRecepientCallNumberReference(qrValues[key])
                }
                "RL" -> {
                    qr.setRecepientReferenceNumber(qrValues[key])
                }
                "RP" -> {
                    qr.setTransactionReference(qrValues[key])
                }
                else -> {}
            }
        }
        return qr
    }

    fun containsRequiredFieldValues(qr: QR): Boolean {
        if (qr.identificationCode == null || qr.identificationCode.equals("", true) || !qr.identificationCode.equals(
                "PK", true
            )
        ) {
            return false
        }
        if (qr.version == null || qr.version.equals("", true) || !qr.version.equals("01", true)) {
            return false
        }
        if (qr.charSet == null || qr.charSet.equals("", true) || !qr.charSet.equals("1", true)) {
            return false
        }
        return if (qr.accNumber == null || qr.accNumber.trim()
                .equals("", true) || qr.accNumber.trim().length > MAX_ACC_CAR_NO
        ) {
            false
        } else true
    }

}
