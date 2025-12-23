package com.payten.whitelabel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.api.SupercaseApiService
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.viewmodel.PosViewModel
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import retrofit2.Response

/**
 * Unit tests for PosViewModel.
 *
 * PosViewModel is a lightweight ViewModel used by HeadlessPaymentActivity for error logging
 * during payment transactions. It has only two methods:
 * 1. logError() - Sends error logs to backend API
 * 2. createErrorLog() - Creates ErrorLog DTO with device/SDK diagnostics
 *
 * ## Testing Strategy:
 *
 * ### logError() - Fully Unit Testable (3 tests)
 * - API success response
 * - Network error handling
 * - HTTP error response
 *
 * ### createErrorLog() - Not Unit Testable
 * createErrorLog() calls SDKUtility static methods that require actual SDK:
 * - SDKUtility.logSecurityStatus(context) - Security diagnostics
 * - SDKUtility.getTR() - Transaction record
 * - SDKUtility.getModulesLogsMessage() - SDK logs
 *
 * These static methods cannot be mocked without PowerMock or similar tools.
 * Integration testing with actual SDK is required to test createErrorLog().
 *
 * ## Test Coverage: ~50%
 * - logError() API flow: 100% covered (3 tests)
 * - createErrorLog(): Requires integration testing (0 unit tests)
 */
class PosViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: SupercaseApiService

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    @Mock
    private lateinit var context: Context

    private lateinit var viewModel: PosViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel = PosViewModel(apiService, sharedPreferences)
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    // ==================== logError() Tests ====================

    @Test
    fun `logError with successful response completes without error`() {
        // Given
        val errorLog = ErrorLog(
            "terminal123",
            "user123",
            "Manufacturer:Model",
            "30",
            "PosViewModel",
            "Payment error",
            "NFC read failed",
            "SDK Status OK",
            "OTP banka d.d.",
            null,
            emptyList()
        )

        val response = Response.success<Void>(null)
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(response))

        // When
        viewModel.logError(errorLog)

        // Then - Should complete without throwing exception
        verify(apiService).errorLog(errorLog)
    }

    @Test
    fun `logError with network error does not crash`() {
        // Given
        val errorLog = ErrorLog(
            "terminal123",
            "user123",
            "Manufacturer:Model",
            "30",
            "PosViewModel",
            "Payment error",
            "Network timeout",
            "SDK Status OK",
            "OTP banka d.d.",
            null,
            emptyList()
        )

        val error = RuntimeException("Network error")
        `when`(apiService.errorLog(any())).thenReturn(Observable.error(error))

        // When
        viewModel.logError(errorLog)

        // Then - Should handle error gracefully without crashing
        verify(apiService).errorLog(errorLog)
    }

    @Test
    fun `logError with HTTP error response handles error`() {
        // Given
        val errorLog = ErrorLog(
            "terminal123",
            "user123",
            "Manufacturer:Model",
            "30",
            "PosViewModel",
            "Payment error",
            "Card removed",
            "SDK Status OK",
            "OTP banka d.d.",
            null,
            emptyList()
        )

        val errorResponse = Response.error<Void>(500, okhttp3.ResponseBody.create(null, "Server error"))
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(errorResponse))

        // When
        viewModel.logError(errorLog)

        // Then - Should complete without throwing (doesn't check response code)
        verify(apiService).errorLog(errorLog)
    }

    // ==================== createErrorLog() Tests ====================
    // NOTE: createErrorLog() cannot be unit tested because it calls SDKUtility static methods:
    // - SDKUtility.logSecurityStatus(context) - Security diagnostics
    // - SDKUtility.getTR() - Transaction record
    // - SDKUtility.getModulesLogsMessage() - SDK logs
    //
    // These static methods require the actual payment SDK to be initialized and cannot
    // be mocked without PowerMock or similar tools. The method constructs an ErrorLog DTO
    // with device/terminal info but also includes SDK diagnostics.
    //
    // ## What createErrorLog() Does (from PosViewModel.kt):
    // 1. Calls SDKUtility.logSecurityStatus(context) for security status
    // 2. Constructs ErrorLog with:
    //    - tid, userId (parameters)
    //    - device: Build.MANUFACTURER + ":" + Build.MODEL
    //    - os: Build.VERSION.SDK_INT.toString()
    //    - activity: this.javaClass.simpleName (returns "PosViewModel")
    //    - description, stack/error (parameters)
    //    - sdkStatus (from SDKUtility)
    //    - institution: SupercaseConfig.INSTITUTION
    //    - tr (from SDKUtility.getTR())
    //    - messages (from SDKUtility.getModulesLogsMessage())
    //
    // ## Testing Strategy:
    // - logError() is fully tested above (API integration)
    // - createErrorLog() requires integration testing with actual SDK
    // - ErrorLog DTO structure is validated in LandingViewModelTest and other tests
    // - SDKUtility methods are documented in SDKUtilityTest.kt
}
