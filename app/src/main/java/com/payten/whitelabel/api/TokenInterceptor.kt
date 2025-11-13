package com.payten.whitelabel.api

import android.text.TextUtils
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response


class TokenInterceptor(var sharedPreferences: KsPrefs) : Interceptor {
    private val logger = KotlinLogging.logger {}

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()

        val builder: Request.Builder = request.newBuilder()
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json; charset=utf-8")
            .method(request.method, request.body)

        val token = sharedPreferences.pull(SharedPreferencesKeys.TOKEN, "")
        var tid = ""
        try {
            tid = sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "")
        } catch (e: Exception) {

        }
        if (!request.url.toString().endsWith("/res/v1/activate") || !request.url.toString().endsWith("/res/v2/logError") || !TextUtils.isEmpty(token)) {
            builder.header(
                "Authorization",
                "Bearer $token"
            )
            builder.header("Terminal-Identification", "${tid}")
        }

        return chain.proceed(builder.build())
    }

}