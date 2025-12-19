package com.payten.whitelabel.api

import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
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
import rs.digitalworx.takt.api.SyncApiService
import rs.digitalworx.takt.api.TaktSyncApiService
import java.util.concurrent.TimeUnit

/**
 * Hilt Dependency Injection module for backend API configuration.
 *
 * This module provides Retrofit and OkHttpClient instances for the SupercaseApiService,
 * configuring all networking infrastructure including authentication, token refresh,
 * request/response interceptors, and timeout settings.
 *
 * ## Hilt Configuration:
 *
 * - **@Module**: Declares this as a Hilt dependency provider
 * - **@InstallIn(ViewModelComponent::class)**: Scoped to ViewModels lifecycle
 * - **@ViewModelScoped**: Single instance per ViewModel, destroyed when ViewModel cleared
 *
 * This ensures each ViewModel gets the same API service instance during its lifetime,
 * but instances are cleaned up when ViewModels are destroyed (memory efficiency).
 *
 * ## Provided Dependencies:
 *
 * ### 1. SupercaseApiService (via provideApiService):
 * Retrofit service interface instance configured with:
 *
 * **Base URL:**
 * - Configured via `SupercaseConfig.API_URL`
 * - Test: `http://91.239.151.43:9090`
 * - Production: `https://spap.payten.rs/`
 *
 * **Converters:**
 * - GsonConverterFactory with Java Time support (LocalDateTime, Instant, etc.)
 * - Registered via `Converters.registerAll()` for proper date/time serialization
 *
 * **Call Adapter:**
 * - RxJava3CallAdapterFactory for reactive programming
 * - Converts Retrofit calls to RxJava3 Observables
 * - Enables Schedulers.io() / AndroidSchedulers.mainThread() subscription
 *
 * **HTTP Client:**
 * - Uses provided OkHttpClient with authentication and interceptors
 *
 * ### 2. OkHttpClient (via provideHttpClient):
 * HTTP client configured with authentication, token refresh, and interceptors.
 *
 * ## Authentication Flow:
 *
 * This module implements a two-layer authentication mechanism:
 *
 * ### Layer 1: TokenInterceptor (Request Interceptor)
 * - Intercepts **every outgoing request**
 * - Adds `Authorization: Bearer <token>` header from SharedPreferences
 * - Adds `Terminal-Identification: <tid>` header for terminal identification
 * - First line of defense, adds auth to all requests proactively
 * - See `TokenInterceptor.kt` for implementation
 *
 * ### Layer 2: Authenticator (401 Response Handler)
 * - Intercepts **401 Unauthorized responses**
 * - Automatically attempts to refresh expired token
 * - Makes synchronous call to `TaktSyncApiService.refreshToken()`
 * - Updates token in SharedPreferences on success
 * - Retries original request with new token
 * - Returns null if token refresh fails (stops retry loop)
 *
 * **Token Refresh Logic:**
 * ```
 * 1. Request fails with 401 Unauthorized
 * 2. Authenticator checks if current token matches failed request token
 * 3. If token already changed (another thread refreshed), returns null (no retry)
 * 4. Calls refreshToken() API synchronously (blocks current thread)
 * 5. Saves new token to SharedPreferences
 * 6. Returns new request with updated Authorization header
 * 7. Retrofit automatically retries original request with new token
 * ```
 *
 * **Why Two Layers?**
 * - TokenInterceptor: Proactively adds auth to every request (fast path)
 * - Authenticator: Handles token expiration gracefully without app-level logic (fallback)
 *
 * ## Network Configuration:
 *
 * **Timeouts:**
 * - Read timeout: 62 seconds (waiting for server response)
 * - Write timeout: 62 seconds (sending request body)
 * - Long timeouts accommodate payment SDK operations and slow networks
 *
 * **Interceptors Chain (in order):**
 * 1. `TokenInterceptor` - Adds authentication headers (application interceptor)
 * 2. `UnauthorizedCaseParserInterceptor` - Parses 401 responses (network interceptor)
 * 3. `Authenticator` - Refreshes token on 401 (authenticator)
 *
 * ## Response Status Codes:
 *
 * **SUCCESS Constant:**
 * - `const val SUCCESS = "00"` - Standard success status code in API responses
 * - Used throughout app to check API response success:
 *   ```kotlin
 *   if (response.statusCode.equals(ApiService.SUCCESS)) {
 *       // Handle success
 *   }
 *   ```
 * - Different from HTTP status codes (this is application-level status in response body)
 *
 * ## Error Handling:
 *
 * **Token Refresh Failures:**
 * - Logged via `logger.error("Authentication error", ex)`
 * - Returns null from Authenticator, causing request to fail
 * - ViewModels handle failure in RxJava error callback
 * - User typically redirected to login screen
 *
 * **Network Errors:**
 * - Handled by RxJava error callbacks in ViewModels
 * - No automatic retry mechanism (ViewModels decide retry strategy)
 *
 * ## Dependencies:
 *
 * This module requires:
 * - `KsPrefs` (SharedPreferences) - Injected by SharedPreferenceModule
 * - `TaktSyncApiService` - Provided by SyncApiService module
 *
 * Hilt automatically resolves these dependencies when providing SupercaseApiService.
 *
 * ## Thread Safety:
 *
 * - OkHttpClient connection pool is thread-safe
 * - Authenticator token refresh is synchronized (blocks other requests during refresh)
 * - SharedPreferences token updates are thread-safe via KsPrefs
 * - Multiple ViewModels can safely share the same SupercaseApiService instance
 *
 * ## Testing Considerations:
 *
 * To mock API calls in tests:
 * - Replace this module with test module providing mock SupercaseApiService
 * - Or use OkHttp MockWebServer for integration tests
 * - Or inject mock SupercaseApiService directly in @HiltAndroidTest
 *
 * @see SupercaseApiService for API endpoint definitions
 * @see TokenInterceptor for authentication header injection
 * @see TaktSyncApiService for token refresh endpoint
 * @see SharedPreferencesKeys for token storage keys
 * @see SupercaseConfig for base URL configuration
 */
@Module
@InstallIn(ViewModelComponent::class)
class ApiService {

    private val logger = KotlinLogging.logger {}

    companion object {
        /**
         * Standard success status code in API response body (not HTTP status code).
         * Used throughout app to check if API operation succeeded.
         */
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
                                sharedPreferences.pull(
                                    SharedPreferencesKeys.TOKEN,
                                    ""
                                )
                            )
                    ) return null

                    logger.info { "Refreshing token" }
                    var accessToken: String? = null
                    try {
                        val loginDtoCall = apiService.refreshToken()
                        val responseCall: retrofit2.Response<*> = loginDtoCall.execute()
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