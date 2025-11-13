package com.payten.whitelabel.background

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SchedulerService {

    lateinit var sharedPreferences: KsPrefs

    lateinit var appContext: Context

    lateinit var appDatabase: AppDatabase

    @Provides
    @Singleton
    fun provideSchedulerService(@ApplicationContext appContext: Context, sharedPreferences: KsPrefs, appDatabase: AppDatabase): SchedulerService {
        this.sharedPreferences = sharedPreferences;
        this.appContext = appContext
        this.appDatabase = appDatabase
        return this;
    }

    fun reloadData(){
        val workManager = WorkManager.getInstance(appContext)
//        val data = workDataOf(DataKeys.TICKET_ID to ticketId, DataKeys.WORK_ORDER_ID to workOrderId)
        val ticketFiles = OneTimeWorkRequest.Builder(ReloadWorker::class.java)
//            .setInputData(data)
            .build()

        workManager
            .beginWith(ticketFiles)
            .enqueue()
    }
}