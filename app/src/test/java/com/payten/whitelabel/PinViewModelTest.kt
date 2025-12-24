package com.payten.whitelabel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.api.SupercaseApiService
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.viewmodel.PinViewModel
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Unit tests for PinViewModel.
 *
 * Tests PIN login flow including:
 * - Token refresh (dummy mode only)
 * - Health check API calls
 * - Error logging
 *
 * ## Test Coverage:
 * - refreshData() with dummy mode (1 test)
 * - healthCheck() with success/failure scenarios (4 tests)
 * - logError() API calls (3 tests)
 *
 * ## Not Unit Testable:
 * - refreshData() with isDummy=false - requires integration test (KsPrefs dispatcher issue)
 * - createErrorLog() - requires SDK integration (uses SDKUtility static methods)
 */
class PinViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: SupercaseApiService

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    @Mock
    private lateinit var healthCheckCall: Call<Void>

    private lateinit var viewModel: PinViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel = PinViewModel(apiService, sharedPreferences)
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    // ==================== refreshData() Tests ====================

    @Test
    fun `refreshData with isDummy true posts success immediately`() {
        // When
        viewModel.refreshData(isDummy = true)

        // Then
        assert(viewModel.getTokenSuccessfull.value == true)
        verify(apiService, never()).refreshToken(any())
    }

    // ==================== refreshData() Real Flow Tests ====================
    // NOTE: refreshData() with isDummy=false cannot be unit tested because:
    // - It calls sharedPreferences.pull(USER_ID, "") and pull<String>(USER_TID)
    // - The generic pull<T>(key) method without default parameter accesses KsPrefs internal dispatcher
    // - Mocking pull<T>(key) causes NullPointerException from getDispatcher()
    // - This requires integration testing with actual KsPrefs instance
    //
    // Testable scenario: isDummy=true (already tested above)

    // ==================== healthCheck() Tests ====================

    @Test
    fun `healthCheck with successful response posts true`() {
        // Given
        val successResponse = Response.success<Void>(null)
        whenever(apiService.healthCheck()).thenReturn(healthCheckCall)

        // When
        viewModel.healthCheck()

        // Then - Capture the callback and invoke onResponse
        val callbackCaptor = ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<Void?>>
        verify(healthCheckCall).enqueue(callbackCaptor.capture())
        callbackCaptor.value.onResponse(healthCheckCall as Call<Void?>, successResponse)

        assert(viewModel.healthCheck.value == true)
    }

    @Test
    fun `healthCheck with HTTP 500 error posts false`() {
        // Given
        val errorResponse = Response.error<Void>(500, okhttp3.ResponseBody.create(null, ""))
        whenever(apiService.healthCheck()).thenReturn(healthCheckCall)

        // When
        viewModel.healthCheck()

        // Then - Capture the callback and invoke onResponse
        val callbackCaptor = ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<Void?>>
        verify(healthCheckCall).enqueue(callbackCaptor.capture())
        callbackCaptor.value.onResponse(healthCheckCall as Call<Void?>, errorResponse)

        assert(viewModel.healthCheck.value == false)
    }

    @Test
    fun `healthCheck with HTTP 404 error posts false`() {
        // Given
        val errorResponse = Response.error<Void>(404, okhttp3.ResponseBody.create(null, ""))
        whenever(apiService.healthCheck()).thenReturn(healthCheckCall)

        // When
        viewModel.healthCheck()

        // Then - Capture the callback and invoke onResponse
        val callbackCaptor = ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<Void?>>
        verify(healthCheckCall).enqueue(callbackCaptor.capture())
        callbackCaptor.value.onResponse(healthCheckCall as Call<Void?>, errorResponse)

        assert(viewModel.healthCheck.value == false)
    }

    @Test
    fun `healthCheck with network failure posts false`() {
        // Given
        val throwable = Throwable("Network timeout")
        whenever(apiService.healthCheck()).thenReturn(healthCheckCall)

        // When
        viewModel.healthCheck()

        // Then - Capture the callback and invoke onFailure
        val callbackCaptor = ArgumentCaptor.forClass(Callback::class.java) as ArgumentCaptor<Callback<Void?>>
        verify(healthCheckCall).enqueue(callbackCaptor.capture())
        callbackCaptor.value.onFailure(healthCheckCall as Call<Void?>, throwable)

        assert(viewModel.healthCheck.value == false)
    }

    // ==================== logError() Tests ====================

    @Test
    fun `logError with successful response posts logsSendSuccess`() {
        // Given
        val errorLog = ErrorLog(
            tid = "TID123",
            userId = "user123",
            device = "Samsung:Galaxy",
            os = "33",
            activity = "PinViewModel",
            description = "Test error",
            stack = "Error message",
            sdkStatus = "OK",
            institution = "TestBank",
            tr = "TR123",
            messages = emptyList()
        )

        val successResponse = Response.success<Void>(null)
        whenever(apiService.errorLog(any())).thenReturn(Observable.just(successResponse))

        // When
        viewModel.logError(errorLog)

        // Then
        assert(viewModel.logsSendSuccess.value == true)
    }

    @Test
    fun `logError with network error posts logsSendFailed`() {
        // Given
        val errorLog = ErrorLog(
            tid = "TID123",
            userId = "user123",
            device = "Samsung:Galaxy",
            os = "33",
            activity = "PinViewModel",
            description = "Test error",
            stack = "Error message",
            sdkStatus = "OK",
            institution = "TestBank",
            tr = "TR123",
            messages = emptyList()
        )

        val error = RuntimeException("Network error")
        whenever(apiService.errorLog(any())).thenReturn(Observable.error(error))

        // When
        viewModel.logError(errorLog)

        // Then
        assert(viewModel.logsSendFailed.value != null)
        assert(viewModel.logsSendFailed.value!!.contains("Network error"))
    }

    @Test
    fun `logError sends correct ErrorLog DTO`() {
        // Given
        val errorLog = ErrorLog(
            tid = "TID123",
            userId = "user456",
            device = "Google:Pixel",
            os = "34",
            activity = "PinViewModel",
            description = "Login failed",
            stack = "Invalid PIN",
            sdkStatus = "SECURE",
            institution = "TestInstitution",
            tr = "TR456",
            messages = emptyList()
        )

        val successResponse = Response.success<Void>(null)
        val captor = argumentCaptor<ErrorLog>()
        whenever(apiService.errorLog(captor.capture())).thenReturn(Observable.just(successResponse))

        // When
        viewModel.logError(errorLog)

        // Then
        val sentLog = captor.firstValue
        assert(sentLog.tid == "TID123")
        assert(sentLog.userId == "user456")
        assert(sentLog.description == "Login failed")
    }
}
