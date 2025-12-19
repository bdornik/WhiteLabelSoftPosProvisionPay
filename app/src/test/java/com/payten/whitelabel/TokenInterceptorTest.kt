package com.payten.whitelabel

import android.text.TextUtils
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.api.TokenInterceptor
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

/**
 * Unit tests for TokenInterceptor.
 *
 * Tests authentication header injection for all API requests.
 */
class TokenInterceptorTest {

    @Mock
    private lateinit var mockPrefs: KsPrefs

    @Mock
    private lateinit var mockChain: Interceptor.Chain

    private lateinit var interceptor: TokenInterceptor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        interceptor = TokenInterceptor(mockPrefs)
    }

    // ==================== Authentication Header Tests ====================

    @Test
    fun `intercept adds Authorization header with Bearer token`() {
        // Given
        val testToken = "test-token-123"
        val testTid = "TID-001"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn(testTid)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify Authorization header
            assert(modifiedRequest.header("Authorization") == "Bearer $testToken") {
                "Authorization header should be 'Bearer $testToken', got: ${modifiedRequest.header("Authorization")}"
            }

            // Create mock response
            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    @Test
    fun `intercept adds Terminal-Identification header`() {
        // Given
        val testToken = "test-token-123"
        val testTid = "TID-001"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn(testTid)

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify Terminal-Identification header
            assert(modifiedRequest.header("Terminal-Identification") == testTid) {
                "Terminal-Identification header should be '$testTid', got: ${modifiedRequest.header("Terminal-Identification")}"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    @Test
    fun `intercept adds Content-Type and Accept headers`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify Content-Type header
            assert(modifiedRequest.header("Content-Type") == "application/json; charset=utf-8") {
                "Content-Type should be 'application/json; charset=utf-8'"
            }

            // Verify Accept header
            assert(modifiedRequest.header("Accept") == "application/json; charset=utf-8") {
                "Accept should be 'application/json; charset=utf-8'"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    // ==================== Conditional Authentication Tests ====================

    @Test
    fun `intercept adds auth headers for regular endpoints`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val request = Request.Builder()
            .url("https://api.test.com/ips/v2/getTransaction")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Should have auth header
            assert(modifiedRequest.header("Authorization")?.startsWith("Bearer ") == true) {
                "Should have Authorization header for regular endpoint"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    @Test
    fun `intercept adds headers even with empty token for most endpoints`() {
        // Mock TextUtils.isEmpty() to work in unit tests
        mockStatic(TextUtils::class.java).use { mockedTextUtils ->
            mockedTextUtils.`when`<Boolean> { TextUtils.isEmpty(any()) }.thenAnswer { invocation ->
                val charSequence = invocation.getArgument<CharSequence?>(0)
                charSequence == null || charSequence.isEmpty()
            }

            // Given - Note: The current logic uses OR (||) so auth is added in most cases
            `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn("")
            `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

            val request = Request.Builder()
                .url("https://api.test.com/ips/v2/getTransaction")
                .build()

            `when`(mockChain.request()).thenReturn(request)
            `when`(mockChain.proceed(any())).thenAnswer { invocation ->
                val modifiedRequest = invocation.getArgument<Request>(0)

                // Due to OR logic, empty token still adds header
                // OkHttp trims trailing whitespace, so "Bearer " becomes "Bearer"
                val authHeader = modifiedRequest.header("Authorization")
                assert(authHeader == "Bearer" || authHeader == "Bearer ") {
                    "Authorization header should be present (even if empty token), got: $authHeader"
                }

                Response.Builder()
                    .request(modifiedRequest)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            // When
            interceptor.intercept(mockChain)

            // Then - verified in answer block above
        }
    }

    // ==================== TID Retrieval Tests ====================

    @Test
    fun `intercept handles TID retrieval exception gracefully`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenThrow(RuntimeException("TID not found"))

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Should have empty TID but not crash
            assert(modifiedRequest.header("Terminal-Identification") == "") {
                "Terminal-Identification should be empty string when TID retrieval fails"
            }

            // Should still have other headers
            assert(modifiedRequest.header("Authorization") == "Bearer $testToken") {
                "Should still have Authorization header"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When - Should not throw exception
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    @Test
    fun `intercept with empty TID still adds empty Terminal-Identification header`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("")

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Should have empty TID header
            assert(modifiedRequest.header("Terminal-Identification") == "") {
                "Terminal-Identification should be empty string"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    // ==================== HTTP Method Preservation Tests ====================

    @Test
    fun `intercept preserves POST method and body`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val requestBody = """{"key":"value"}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .post(requestBody)
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify method preserved
            assert(modifiedRequest.method == "POST") {
                "HTTP method should be POST"
            }

            // Verify body preserved
            assert(modifiedRequest.body != null) {
                "Request body should be preserved"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    @Test
    fun `intercept preserves GET method`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .get()
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify method preserved
            assert(modifiedRequest.method == "GET") {
                "HTTP method should be GET"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    // ==================== Multiple Request Tests ====================

    @Test
    fun `intercept can handle multiple sequential requests`() {
        // Given
        val testToken = "test-token-123"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(testToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val request1 = Request.Builder()
            .url("https://api.test.com/endpoint1")
            .build()

        val request2 = Request.Builder()
            .url("https://api.test.com/endpoint2")
            .build()

        `when`(mockChain.request()).thenReturn(request1, request2)
        `when`(mockChain.proceed(any())).thenReturn(
            Response.Builder()
                .request(request1)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build(),
            Response.Builder()
                .request(request2)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        )

        // When - Make two requests
        val response1 = interceptor.intercept(mockChain)
        val response2 = interceptor.intercept(mockChain)

        // Then - Both should succeed
        assert(response1.code == 200) { "First request should succeed" }
        assert(response2.code == 200) { "Second request should succeed" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `intercept with special characters in token`() {
        // Given
        val specialToken = "token-with-special-chars_123!@#"
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(specialToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify special chars preserved
            assert(modifiedRequest.header("Authorization") == "Bearer $specialToken") {
                "Token with special characters should be preserved"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }

    @Test
    fun `intercept with long token string`() {
        // Given
        val longToken = "a".repeat(1000) // 1000 character token
        `when`(mockPrefs.pull(SharedPreferencesKeys.TOKEN, "")).thenReturn(longToken)
        `when`(mockPrefs.pull(SharedPreferencesKeys.USER_TID, "")).thenReturn("TID-001")

        val request = Request.Builder()
            .url("https://api.test.com/endpoint")
            .build()

        `when`(mockChain.request()).thenReturn(request)
        `when`(mockChain.proceed(any())).thenAnswer { invocation ->
            val modifiedRequest = invocation.getArgument<Request>(0)

            // Verify long token preserved
            assert(modifiedRequest.header("Authorization") == "Bearer $longToken") {
                "Long token should be preserved"
            }

            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        // When
        interceptor.intercept(mockChain)

        // Then - verified in answer block above
    }
}
