package com.payten.nkbm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.dto.GenerateTokenDto
import com.payten.nkbm.dto.OtpCheckDto
import com.payten.nkbm.persistance.SharedPreferencesKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject

@HiltViewModel
class ForgotPinModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val sharedPreferences: KsPrefs,
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    val otpResponse =  MutableLiveData<Boolean>()
    val otpCheckResponse =  MutableLiveData<String>()

    fun sendSms() {
        apiService
            .otpCreate(sharedPreferences.pull(SharedPreferencesKeys.USER_TID))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Otp created")
                otpResponse.postValue(true)
            }, { error ->
                logger.throwing(error)
                otpResponse.postValue(false)
            })
    }

    fun otpCheck(otpCheckDto: OtpCheckDto) {

        logger.info { "OtpCheck request: $otpCheckDto" }
        apiService
            .otpCheck(sharedPreferences.pull(SharedPreferencesKeys.USER_TID), otpCheckDto)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                logger.info("Otp check passed")
                otpCheckResponse.postValue(response.statusCode)
            }, { error ->
                logger.throwing(error)
                otpCheckResponse.postValue("05")
            })
    }

    fun refreshData() {
        apiService
            .refreshToken(GenerateTokenDto(sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""), sharedPreferences.pull(SharedPreferencesKeys.USER_TID)))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                sharedPreferences.push(SharedPreferencesKeys.TOKEN, response.sessionToken)
                logger.info("Generate token successfull ${response.sessionToken}")
            }, { error ->
                logger.throwing(error)

            })
    }
}
