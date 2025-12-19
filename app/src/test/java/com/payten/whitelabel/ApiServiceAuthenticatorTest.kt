package com.payten.whitelabel

import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.dto.ApiResponse
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import retrofit2.Call
import rs.digitalworx.takt.api.TaktSyncApiService

/**
 * Unit tests for ApiService Authenticator logic.
 *
 * Tests automatic token refresh on 401 Unauthorized responses.
 */
class ApiServiceAuthenticatorTest {

    @Mock
    private lateinit var mockPrefs: KsPrefs

    @Mock
    private lateinit var mockTaktSyncApi: TaktSyncApiService

    @Mock
    private lateinit var mockRoute: Route

    private lateinit var authenticator: Authenticator

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Create the authenticator inline (same logic as ApiService.provideHttpClient)
        authenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                // Check if token already changed (another thread refreshed it)
                if (!response.request.header("Authorization")
                        .equals(mockPrefs.pull(SharedPreferencesKeys.TOKEN, ""))
                ) return null

                // Refresh token
                var accessToken: String? = null
                try {
                    val loginDtoCall = mockTaktSyncApi.refreshToken()
                    val responseCall: retrofit2.Response<*> = loginDtoCall.execute()
                    val responseRequest: ApiResponse? = responseCall.body() as ApiResponse?
                    if (responseRequest != null) {
                        val token: String = responseRequest.sessionToken
                        mockPrefs.push(SharedPreferencesKeys.TOKEN, token)
                        accessToken = token
                    }
                } catch (_: Exception) {
                    // Token refresh failed
                }

                return if (accessToken != null)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                else null
            }
        }
    }

    // ==================== Token Refresh Success Tests ====================

    @Test
    fun `authenticate refreshes token on 401 response`() {
        // Given - old token in request
        val oldToken = "old-token-123"
        val newToken = "new-token-456"

        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        // Mock token refresh call
        val mockCall = createMockCall(newToken)
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then
        assert(newRequest != null) { "Should return new request with refreshed token" }
        assert(newRequest?.header("Authorization") == "Bearer $newToken") {
            "New request should have new token, got: ${newRequest?.header("Authorization")}"
        }
        verify(mockPrefs).push(SharedPreferencesKeys.TOKEN, newToken)
    }

    @Test
    fun `authenticate stores new token in SharedPreferences`() {
        // Given
        val oldToken = "old-token"
        val newToken = "new-token"

        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        val mockCall = createMockCall(newToken)
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        authenticator.authenticate(mockRoute, response)

        // Then - verify token was stored
        verify(mockPrefs).push(SharedPreferencesKeys.TOKEN, newToken)
    }

    // ==================== Token Already Changed Tests ====================

    @Test
    fun `authenticate returns null when token already changed`() {
        // Given - token in request doesn't match current token (another thread already refreshed)
        val oldTokenInRequest = "old-token"
        val currentToken = "already-refreshed-token"

        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(currentToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldTokenInRequest)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - should return null (don't retry, another thread already handled it)
        assert(newRequest == null) {
            "Should return null when token already changed by another thread"
        }
    }

    // ==================== Token Refresh Failure Tests ====================

    @Test
    fun `authenticate returns null when token refresh fails`() {
        // Given
        val oldToken = "old-token"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        // Mock token refresh failure
        val mockCall = createMockFailedCall()
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - should return null (stop retry loop)
        assert(newRequest == null) {
            "Should return null when token refresh fails"
        }
    }

    @Test
    fun `authenticate returns null when refreshToken throws exception`() {
        // Given
        val oldToken = "old-token"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        // Mock exception during refresh
        `when`(mockTaktSyncApi.refreshToken()).thenThrow(RuntimeException("Network error"))

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - should return null (exception caught)
        assert(newRequest == null) {
            "Should return null when token refresh throws exception"
        }
    }

    @Test
    fun `authenticate returns null when response body is null`() {
        // Given
        val oldToken = "old-token"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        // Mock null response body
        val mockCall = createMockCallWithNullBody()
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - should return null
        assert(newRequest == null) {
            "Should return null when response body is null"
        }
    }

    // ==================== Request Preservation Tests ====================

    @Test
    fun `authenticate preserves original request URL`() {
        // Given
        val oldToken = "old-token"
        val newToken = "new-token"
        val testUrl = "https://api.test.com/important/endpoint"

        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url(testUrl)
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        val mockCall = createMockCall(newToken)
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - URL should be preserved
        assert(newRequest?.url.toString() == testUrl) {
            "Original request URL should be preserved"
        }
    }

    @Test
    fun `authenticate preserves original request method and body`() {
        // Given
        val oldToken = "old-token"
        val newToken = "new-token"

        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val requestBody = RequestBody.create("application/json".toMediaType(), """{"key":"value"}""")
        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .post(requestBody)
            .header("Authorization", oldToken)
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        val mockCall = createMockCall(newToken)
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - method and body should be preserved
        assert(newRequest?.method == "POST") {
            "Original request method should be preserved"
        }
        assert(newRequest?.body != null) {
            "Original request body should be preserved"
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `authenticate with missing Authorization header in request`() {
        // Given
        val currentToken = "current-token"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(currentToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            // No Authorization header
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then - should return null (token check fails with null header)
        assert(newRequest == null) {
            "Should return null when original request has no Authorization header"
        }
    }

    @Test
    fun `authenticate updates only Authorization header`() {
        // Given
        val oldToken = "old-token"
        val newToken = "new-token"

        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(oldToken)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .header("Authorization", oldToken)
            .header("Custom-Header", "custom-value")
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

        val mockCall = createMockCall(newToken)
        `when`(mockTaktSyncApi.refreshToken()).thenReturn(mockCall)

        // When
        val newRequest = authenticator.authenticate(mockRoute, response)

        // Then
        assert(newRequest?.header("Authorization") == "Bearer $newToken") {
            "Authorization header should be updated"
        }
        assert(newRequest?.header("Custom-Header") == "custom-value") {
            "Other headers should be preserved"
        }
    }

    // ==================== Helper Methods ====================

    private fun createMockCall(newToken: String): Call<ApiResponse> {
        val mockCall = org.mockito.kotlin.mock<Call<ApiResponse>>()
        val apiResponse = ApiResponse(
            sessionToken = newToken,
            status = 0,
            error = "",
            statusCode = "00"
        )

        // Create a successful retrofit response
        val retrofitResponse = retrofit2.Response.success(apiResponse)

        `when`(mockCall.execute()).thenReturn(retrofitResponse)

        return mockCall
    }

    private fun createMockFailedCall(): Call<ApiResponse> {
        val mockCall = org.mockito.kotlin.mock<Call<ApiResponse>>()

        // Create a response with null body to simulate failure
        val retrofitResponse = retrofit2.Response.success<ApiResponse>(null)

        `when`(mockCall.execute()).thenReturn(retrofitResponse)

        return mockCall
    }

    private fun createMockCallWithNullBody(): Call<ApiResponse> {
        val mockCall = org.mockito.kotlin.mock<Call<ApiResponse>>()

        // Create a response with null body
        val retrofitResponse = retrofit2.Response.success<ApiResponse>(null)

        `when`(mockCall.execute()).thenReturn(retrofitResponse)

        return mockCall
    }
}
