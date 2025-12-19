package com.payten.whitelabel.api

import android.text.TextUtils
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * OkHttp interceptor for adding authentication headers to all outgoing API requests.
 *
 * This interceptor is the first layer of authentication in the app's networking stack,
 * proactively adding authentication credentials to every HTTP request before it's sent
 * to the backend. It works in conjunction with the Authenticator in ApiService to provide
 * comprehensive authentication handling.
 *
 * ## Authentication Headers Added:
 *
 * ### 1. Authorization Header (Bearer Token):
 * - Header: `Authorization: Bearer <token>`
 * - Token retrieved from SharedPreferences via `SharedPreferencesKeys.TOKEN`
 * - Token obtained during registration/login via `refreshToken()` API
 * - Token stored encrypted in KsPrefs
 * - Required for all authenticated API endpoints
 *
 * ### 2. Terminal-Identification Header:
 * - Header: `Terminal-Identification: <tid>`
 * - TID (Terminal ID) retrieved from SharedPreferences via `SharedPreferencesKeys.USER_TID`
 * - TID assigned by backend during terminal activation
 * - Used by backend to identify which terminal is making the request
 * - Critical for multi-terminal merchant accounts
 *
 * ### 3. Content-Type and Accept Headers:
 * - `Content-Type: application/json; charset=utf-8`
 * - `Accept: application/json; charset=utf-8`
 * - Ensures all requests/responses use JSON format with UTF-8 encoding
 * - Added to every request regardless of authentication status
 *
 * ## Conditional Authentication:
 *
 * The interceptor conditionally adds authentication headers based on the request URL:
 *
 * **Authentication SKIPPED for:**
 * - `/res/v1/activate` - Terminal activation endpoint (not yet authenticated)
 * - `/res/v2/logError` - Error logging endpoint (may run before authentication)
 *
 * **Authentication ADDED for:**
 * - All other endpoints (majority of API calls)
 * - Only if token is not empty (user is logged in)
 *
 * **Note:** The logic uses `||` (OR) instead of `&&` (AND), which means auth headers are
 * added in most cases. This appears to be intentional to ensure authentication is added
 * unless explicitly skipped.
 *
 * ## Integration with ApiService:
 *
 * This interceptor is registered in ApiService.provideHttpClient():
 * ```kotlin
 * OkHttpClient()
 *     .newBuilder()
 *     .addInterceptor(TokenInterceptor(sharedPreferences))  // Layer 1: Proactive auth
 *     .authenticator(...)                                    // Layer 2: Token refresh on 401
 *     .build()
 * ```
 *
 * ## Two-Layer Authentication System:
 *
 * ### Layer 1: TokenInterceptor (This Class)
 * - **Timing**: Before request is sent
 * - **Purpose**: Proactively add authentication headers to all requests
 * - **Advantage**: Fast path, no round-trip needed
 * - **Limitation**: Cannot handle token expiration
 *
 * ### Layer 2: Authenticator (ApiService)
 * - **Timing**: After receiving 401 Unauthorized response
 * - **Purpose**: Refresh expired token and retry request
 * - **Advantage**: Handles token expiration gracefully
 * - **Limitation**: Requires extra round-trip
 *
 * Together, these layers provide robust authentication:
 * 1. TokenInterceptor adds auth to every request (fast path)
 * 2. If token expires, backend returns 401
 * 3. Authenticator catches 401, refreshes token, retries request
 * 4. User experiences seamless authentication without app-level logic
 *
 * ## Error Handling:
 *
 * **TID Retrieval Failure:**
 * - If `sharedPreferences.pull(USER_TID)` throws exception, caught silently
 * - TID left as empty string ("")
 * - Request proceeds with empty Terminal-Identification header
 * - Backend may reject request if TID is required
 *
 * This graceful degradation allows error logging to work even if TID is not available.
 *
 * ## Thread Safety:
 *
 * - OkHttp interceptors are called on background threads (IO dispatcher)
 * - SharedPreferences (KsPrefs) is thread-safe for reads
 * - No state is mutated in this interceptor (thread-safe)
 * - Multiple concurrent requests can safely use the same interceptor instance
 *
 * ## Token Lifecycle:
 *
 * 1. **Registration/Login**: Token obtained via `refreshToken()` API, stored in SharedPreferences
 * 2. **Every Request**: TokenInterceptor reads token from SharedPreferences, adds to headers
 * 3. **Token Expiration**: Backend returns 401, Authenticator refreshes token
 * 4. **Token Refresh**: New token stored in SharedPreferences, automatically used by next request
 * 5. **Logout**: Token cleared from SharedPreferences (not handled by this class)
 *
 * ## Testing Considerations:
 *
 * To test API calls with authentication:
 * - Mock KsPrefs to return test token and TID
 * - Or use OkHttp MockWebServer to verify headers are sent
 * - Or inject test OkHttpClient without TokenInterceptor
 *
 * @property sharedPreferences KsPrefs instance for reading token and TID
 *
 * @see ApiService for Authenticator implementation (Layer 2)
 * @see SupercaseApiService for API endpoint definitions
 * @see SharedPreferencesKeys for token and TID storage keys
 */
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
        } catch (_: Exception) {

        }
        if (!request.url.toString().endsWith("/res/v1/activate") || !request.url.toString().endsWith("/res/v2/logError") || !TextUtils.isEmpty(token)) {
            builder.header(
                "Authorization",
                "Bearer $token"
            )
            builder.header("Terminal-Identification", tid)
        }

        return chain.proceed(builder.build())
    }

}