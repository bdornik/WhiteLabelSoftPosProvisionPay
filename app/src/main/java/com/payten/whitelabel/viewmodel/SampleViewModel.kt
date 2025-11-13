package com.payten.whitelabel.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.payten.whitelabel.background.SchedulerService
import com.payten.whitelabel.persistance.user.User
import com.payten.whitelabel.persistance.user.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import mu.KotlinLogging
import rs.digitalworx.takt.api.SupercaseApiService
import javax.inject.Inject

@HiltViewModel
class SampleViewModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val userDao: UserDao,
    private val schedulerService: SchedulerService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val logger = KotlinLogging.logger {}

    fun getValues() : LiveData<List<User>> {
        loadValues();
        return userDao.getAllUsers()
    }

    private fun loadValues() {
//        apiService
//            .getProducts()
//            .subscribeOn(Schedulers.io())
//            .observeOn(Schedulers.io())
//            .subscribe ({ response ->
//                userDao.insertUser(User(userName = response.statusCode.toString())).subscribe()
//                schedulerService.reloadData()
//                logger.info("User successfully saved")
//            }, { error ->
//                logger.throwing(error)
//            })

    }
}
