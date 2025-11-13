package com.payten.whitelabel.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.dto.LogSend
import com.payten.whitelabel.event.SingleLiveEvent
import com.payten.whitelabel.utils.SDKUtility
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mu.KotlinLogging
import okhttp3.*
import rs.digitalworx.takt.api.SupercaseApiService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject


@HiltViewModel
class TermsConditionsViewModel @Inject constructor(
    private val apiService: SupercaseApiService,
    private val application: Application,
) : AndroidViewModel(application){

    private val TAG = "RegistrationViewModel"
    private val logger = KotlinLogging.logger {}

    private val client = OkHttpClient()



    val pdfFileLiveData = MutableLiveData<File?>()

    val logsSendSuccess = SingleLiveEvent<LogSend>()
    val logsSendFailed = SingleLiveEvent<LogSend>()

    fun downloadPdf(url: String) {
        val request = Request.Builder().url(url).build()
        logger.info("Get Terms")

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                logger.error {e.printStackTrace()}
                pdfFileLiveData.postValue(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    pdfFileLiveData.postValue(null)

                    throw IOException("Unexpected code $response")
                }

                val inputStream: InputStream = response.body?.byteStream() ?: return

                // Create a temporary file in the cache directory
                val file = File(getApplication<Application>().cacheDir, "downloaded_pdf.pdf")

                // Write the input stream to the file
                val fos = FileOutputStream(file)
                val buffer = ByteArray(1024)
                var length: Int

                try {
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        fos.write(buffer, 0, length)
                    }
                } finally {
                    fos.close()
                    inputStream.close()
                }

                // Post the file to LiveData
                pdfFileLiveData.postValue(file)
            }
        })
    }


    fun logError(errorLog: ErrorLog, dialog: Boolean) {
        logger.info { "logError $errorLog" }
        apiService
            .errorLog(errorLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                logger.info("Error Send!")
                logsSendSuccess.postValue(LogSend(dialog,"", true))
            }, { error ->
                logger.throwing(error)
                logger.info("Error Not Send!")
                logsSendFailed.postValue(LogSend(dialog,error.message.toString(), false))
            })
    }

    fun createErrorLog(
        tid: String,
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
