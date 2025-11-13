package rs.digitalworx.takt.api

import androidx.annotation.NonNull
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.payten.whitelabel.config.SupercaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Field
import java.net.HttpURLConnection


@Module
@InstallIn(SingletonComponent::class)
class SyncApiService {

    lateinit var apiService : TaktSyncApiService

    private val logger = KotlinLogging.logger {}

    @Provides
    fun provideApiService(sharedPreferences: KsPrefs): TaktSyncApiService {
        val gson = Converters.registerLocalDateTime(GsonBuilder()).create()

        apiService = Retrofit
            .Builder()
            .baseUrl(SupercaseConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(provideHttpClient(sharedPreferences))
            .build()
            .create(TaktSyncApiService::class.java)
        return apiService
    }

    fun provideHttpClient(sharedPreferences: KsPrefs) : OkHttpClient{
        return OkHttpClient()
            .newBuilder()
            .addNetworkInterceptor(UnauthorizedCaseParserInterceptor())
            .build()
    }

    class UnauthorizedCaseParserInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(@NonNull chain: Interceptor.Chain): Response {
            val request: Request = chain.request()
            val response: Response = chain.proceed(request)
            if (isUnauthorizedResponse(response)) {
                try {
                    val codeField: Field = response.javaClass.getDeclaredField("code")
                    codeField.setAccessible(true)
                    codeField.set(response, HttpURLConnection.HTTP_UNAUTHORIZED)
                } catch (e: Exception) {
                    return response
                }
            }
            return response
        }

        private fun isUnauthorizedResponse(response: Response): Boolean {
            if(response.code.equals(419)){
                return true
            }
            return false
        }
    }

}