package com.simant.utils

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


object AppEventBus {
    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }


    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}