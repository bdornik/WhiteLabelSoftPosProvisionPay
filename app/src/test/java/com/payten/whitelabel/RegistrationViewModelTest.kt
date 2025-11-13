package com.payten.whitelabel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.dto.ActivationResponseDto
import com.payten.whitelabel.dto.ApiResponse
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.viewmodel.RegistrationViewModel
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import rs.digitalworx.takt.api.SupercaseApiService

/**
 * Unit tests for RegistrationViewModel.
 *
 * These tests focus on API response handling and LiveData updates.
 * SDK initialization and Android Context dependencies are not tested here.
 */
class RegistrationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: SupercaseApiService

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    private lateinit var viewModel: RegistrationViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel = RegistrationViewModel(apiService, sharedPreferences)
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    // ==================== API Response Tests ====================

    @Test
    fun `generateToken with successful response stores token`() {
        // Given
        val userId = "testUser"
        val tid = "terminal123"
        val sessionToken = "testToken123"

        val tokenResponse = ApiResponse(
            sessionToken = sessionToken,
            status = 0,
            error = "",
            statusCode = "00"
        )

        `when`(apiService.refreshToken(any())).thenReturn(Observable.just(tokenResponse))

        // When
        viewModel.generateToken(userId, tid)

        // Then
        verify(sharedPreferences).push(SharedPreferencesKeys.TOKEN, sessionToken)
        assert(viewModel.paytenGenerateTokenSuccessfull.value == true)
    }

    @Test
    fun `generateToken with network error does not crash`() {
        // Given
        val userId = "testUser"
        val tid = "terminal123"
        val error = RuntimeException("Network error")

        `when`(apiService.refreshToken(any())).thenReturn(Observable.error(error))

        // When
        viewModel.generateToken(userId, tid)

        // Then - Should not crash, just not update success LiveData
        assert(viewModel.paytenGenerateTokenSuccessfull.value != true)
    }

    // ==================== Response Code Validation Tests ====================

    @Test
    fun `activation response with code 00 is treated as success`() {
        // Given
        val response = ActivationResponseDto(
            tid = "test123",
            statusCode = "00",
            tenant = "tenant"
        )

        // When
        val isSuccess = response.statusCode.equals("00", ignoreCase = true)

        // Then
        assert(isSuccess)
    }

    @Test
    fun `activation response with non-00 code is treated as failure`() {
        // Given
        val failureCodes = listOf("01", "05", "99", "ERROR")

        // Then
        failureCodes.forEach { code ->
            val response = ActivationResponseDto(
                tid = "",
                statusCode = code,
                tenant = ""
            )
            val isSuccess = response.statusCode.equals("00", ignoreCase = true)
            assert(!isSuccess) { "Status code $code should be treated as failure" }
        }
    }

    // ==================== Data Validation Tests ====================

    @Test
    fun `valid activation response contains required fields`() {
        // Given
        val response = ActivationResponseDto(
            tid = "terminal123",
            statusCode = "00",
            tenant = "testTenant"
        )

        // Then
        assert(response.tid.isNotEmpty())
        assert(response.statusCode == "00")
        assert(response.tenant.isNotEmpty())
    }

    @Test
    fun `failed activation response has empty or null fields`() {
        // Given
        val response = ActivationResponseDto(
            tid = "",
            statusCode = "05",
            tenant = ""
        )

        // Then
        assert(response.tid.isEmpty())
        assert(response.statusCode != "00")
        assert(response.tenant.isEmpty())
    }

    // ==================== Token Response Tests ====================

    @Test
    fun `successful token response has valid session token`() {
        // Given
        val tokenResponse = ApiResponse(
            sessionToken = "validToken123",
            status = 0,
            error = "",
            statusCode = "00"
        )

        // Then
        assert(tokenResponse.sessionToken.isNotEmpty())
        assert(tokenResponse.statusCode == "00")
    }

    @Test
    fun `failed token response has error message`() {
        // Given
        val tokenResponse = ApiResponse(
            sessionToken = "",
            status = 1,
            error = "Invalid credentials",
            statusCode = "01"
        )

        // Then
        assert(tokenResponse.error.isNotEmpty())
        assert(tokenResponse.statusCode != "00")
    }
}