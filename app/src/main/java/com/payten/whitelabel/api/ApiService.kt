package rs.digitalworx.takt.api

import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.payten.whitelabel.api.TokenInterceptor
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ApiResponse
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import mu.KotlinLogging
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(ViewModelComponent::class)
class ApiService {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val SUCCESS = "00"
    }

    @Provides
    @ViewModelScoped
    fun provideApiService(sharedPreferences: KsPrefs, client: OkHttpClient): SupercaseApiService {
        val gson = Converters.registerAll(GsonBuilder()).create()

        return Retrofit
            .Builder()
            .baseUrl(SupercaseConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(client)
            .build()
            .create(SupercaseApiService::class.java)
    }

    @Provides
    fun provideHttpClient(sharedPreferences: KsPrefs, apiService: TaktSyncApiService) : OkHttpClient{
        return OkHttpClient()
            .newBuilder()
            .authenticator(object : Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    if (!response.request.header("Authorization")
                            .equals(
                                "${
                                    sharedPreferences.pull(
                                        SharedPreferencesKeys.TOKEN,
                                        ""
                                    )
                                }"
                            )
                    ) return null

                    logger.info { "Refreshing token" }
                    var accessToken: String? = null
                    try {
                        val loginDtoCall = apiService?.refreshToken()
                        val responseCall: retrofit2.Response<*> = loginDtoCall?.execute()!!
                        val responseRequest: ApiResponse? =
                            responseCall.body() as ApiResponse?
                        if (responseRequest != null) {
                            val token: String = responseRequest.sessionToken
                            sharedPreferences.push(SharedPreferencesKeys.TOKEN, token)
                            accessToken = token
                        }
                    } catch (ex: Exception) {
                        logger.error("Authentication error", ex)
                    }

                    return if (accessToken != null)
                        response.request.newBuilder()
                            .header(
                                "Authorization",
                                "Bearer $accessToken"
                            ) // use the new access token
                            .build() else null
                }

            })
            .addInterceptor(TokenInterceptor(sharedPreferences))
            .addNetworkInterceptor(SyncApiService.UnauthorizedCaseParserInterceptor())
            .readTimeout(62, TimeUnit.SECONDS)
            .writeTimeout(62, TimeUnit.SECONDS)
            .build()
    }
}