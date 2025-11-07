package com.payten.nkbm.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.payten.nkbm.persistance.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single
import mu.KotlinLogging

@HiltWorker
class ReloadWorker @AssistedInject constructor(@Assisted context: Context,
                                               @Assisted workerParameters: WorkerParameters,
                                               var appDatabase: AppDatabase
) : RxWorker(context, workerParameters) {

    private val logger = KotlinLogging.logger {}

    override fun createWork(): Single<Result> {
        return Single.create { emitter ->
            run {
                logger.info { "Done" }
                emitter.onSuccess(Result.success())
            }
        }
//        return apiService
//                .getAllWorkOrders()
//                .observeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .map { response -> response.data }
//                .map { dtoList -> {
//                    var entityList = mutableListOf<WorkOrder>()
//                    dtoList.forEach { dto ->
//                        entityList.add(
//                                WorkOrder(
//                                        id = dto.id,
//                                        assetId = dto.assetId,
//                                        name = dto.name,
//                                        priority = dto.priority,
//                                        status = dto.status,
//                                        ticketCount = dto.ticketCount
//                                )
//                        )
//                    }
//
//                    logger.info { "Received ${entityList.size} work orders from API" }
//                    entityList
//                } }
//                .map { it.invoke() }
//                .map {
//                    val ids = mutableListOf<Int>()
//                    it.forEach {
//                        ids.add(it.id)
//                    }
//                    appDatabase.workOrdersDao().deleteEntitiesNotInListSync(ids)
//                    it
//                }
//                .map { appDatabase.workOrdersDao().insertAllWorkOrders(it) }
//                .map { Result.success() }
    }
}