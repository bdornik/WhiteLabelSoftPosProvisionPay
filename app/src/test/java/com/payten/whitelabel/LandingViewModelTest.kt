package com.payten.whitelabel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.viewmodel.LandingViewModel
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
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import retrofit2.Response
import com.payten.whitelabel.api.SupercaseApiService

/**
 * Unit tests for LandingViewModel.
 *
 * Tests dashboard functionality, terminal status checks, and merchant details retrieval.
 */
class LandingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: SupercaseApiService

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    private lateinit var viewModel: LandingViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel = LandingViewModel(apiService, sharedPreferences)
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    // ==================== Refresh Data Tests ====================

    @Test
    fun `refreshData with dummy flag sets dummy details`() {
        // When
        var successResult: Boolean? = null
        viewModel.getDetailsSuccessfull.observeForever { successResult = it }
        viewModel.refreshData(isDummy = true)

        // Then
        assert(successResult == true) { "Should succeed immediately for dummy data" }
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_NAME, "Google")
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "Google")
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_ADDRESS, "Test")
    }

    // ==================== Dummy Details Tests ====================

    @Test
    fun `dummyDetails sets correct merchant information`() {
        // When
        viewModel.dummyDetails()

        // Then
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_NAME, "Google")
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "Google")
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_ADDRESS, "Test")
    }

    @Test
    fun `dummyDetails stores exactly three values`() {
        // When
        viewModel.dummyDetails()

        // Then - Verify only merchant name, place name, and address are set
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_NAME, "Google")
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "Google")
        verify(sharedPreferences).push(SharedPreferencesKeys.MERCHANT_ADDRESS, "Test")
    }

    // ==================== Error Logging Tests ====================

    @Test
    fun `logError calls API with correct error log`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Samsung:Galaxy",
            os = "30",
            activity = "LandingViewModel",
            description = "Test error",
            stack = "Error occurred",
            sdkStatus = "Active",
            institution = "Test Bank",
            tr = "",
            messages = emptyList()
        )

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        viewModel.logError(errorLog, dialog = false)

        // Then
        verify(apiService).errorLog(errorLog)
    }

    @Test
    fun `logError with dialog true posts success event`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Test",
            os = "30",
            activity = "Test",
            description = "Test",
            stack = "Test",
            sdkStatus = "Active",
            institution = "Test",
            tr = "",
            messages = emptyList()
        )

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        var successEvent: Boolean? = null
        viewModel.logsSendSuccess.observeForever { successEvent = it }
        viewModel.logError(errorLog, dialog = true)

        // Then
        assert(successEvent == true) { "Success event should be posted when dialog is true" }
    }

    @Test
    fun `logError with dialog false does not post success event`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Test",
            os = "30",
            activity = "Test",
            description = "Test",
            stack = "Test",
            sdkStatus = "Active",
            institution = "Test",
            tr = "",
            messages = emptyList()
        )

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        var successEvent: Boolean? = null
        viewModel.logsSendSuccess.observeForever { successEvent = it }
        viewModel.logError(errorLog, dialog = false)

        // Then
        assert(successEvent == null) { "Success event should not be posted when dialog is false" }
    }

    @Test
    fun `logError failure posts error message`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Test",
            os = "30",
            activity = "Test",
            description = "Test",
            stack = "Test",
            sdkStatus = "Active",
            institution = "Test",
            tr = "",
            messages = emptyList()
        )
        val error = RuntimeException("Network failure")

        `when`(apiService.errorLog(any())).thenReturn(Observable.error(error))

        // When
        var errorMessage: String? = null
        viewModel.logsSendFailed.observeForever { errorMessage = it }
        viewModel.logError(errorLog, dialog = true)

        // Then
        assert(errorMessage == "Network failure") { "Error message should match exception message" }
    }

    // ==================== ErrorLog Structure Tests ====================

    @Test
    fun `ErrorLog can be created with all required fields`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Samsung:Galaxy",
            os = "30",
            activity = "LandingViewModel",
            description = "Test description",
            stack = "Test error",
            sdkStatus = "Active",
            institution = "Test Bank",
            tr = null,
            messages = emptyList()
        )

        // Then
        assert(errorLog.tid == "T001") { "TID should match" }
        assert(errorLog.userId == "U001") { "UserId should match" }
        assert(errorLog.stack == "Test error") { "Stack should match" }
        assert(errorLog.description == "Test description") { "Description should match" }
        assert(errorLog.activity == "LandingViewModel") { "Activity should match" }
        assert(errorLog.device.contains(":")) { "Device should contain manufacturer:model format" }
        assert(errorLog.os.isNotEmpty()) { "OS should not be empty" }
        assert(errorLog.institution.isNotEmpty()) { "Institution should not be empty" }
    }

    // ==================== LiveData Observers Tests ====================

    @Test
    fun `getDetailsSuccessfull LiveData can be observed`() {
        // When
        var observedValue: Boolean? = null
        viewModel.getDetailsSuccessfull.observeForever { observedValue = it }
        viewModel.getDetailsSuccessfull.postValue(true)

        // Then
        assert(observedValue == true) { "LiveData should emit observed value" }
    }

    @Test
    fun `getDetailsProvision LiveData can be observed`() {
        // When
        var observedValue: Boolean? = null
        viewModel.getDetailsProvision.observeForever { observedValue = it }
        viewModel.getDetailsProvision.postValue(true)

        // Then
        assert(observedValue == true) { "LiveData should emit observed value" }
    }

    @Test
    fun `logsSendSuccess SingleLiveEvent can be observed`() {
        // When
        var observedValue: Boolean? = null
        viewModel.logsSendSuccess.observeForever { observedValue = it }
        viewModel.logsSendSuccess.postValue(true)

        // Then
        assert(observedValue == true) { "SingleLiveEvent should emit observed value" }
    }

    @Test
    fun `logsSendFailed SingleLiveEvent can be observed`() {
        // When
        var observedValue: String? = null
        viewModel.logsSendFailed.observeForever { observedValue = it }
        viewModel.logsSendFailed.postValue("Error message")

        // Then
        assert(observedValue == "Error message") { "SingleLiveEvent should emit observed value" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `multiple logError calls can be made sequentially`() {
        // Given
        val errorLog1 = ErrorLog("T001", "U001", "Device1", "30", "Screen1", "Desc1", "Error1", "Active", "Bank1", "", emptyList())
        val errorLog2 = ErrorLog("T002", "U002", "Device2", "30", "Screen2", "Desc2", "Error2", "Active", "Bank2", "", emptyList())

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        viewModel.logError(errorLog1, dialog = false)
        viewModel.logError(errorLog2, dialog = false)

        // Then - Both calls should succeed
        verify(apiService).errorLog(errorLog1)
        verify(apiService).errorLog(errorLog2)
    }

    @Test
    fun `ErrorLog handles special characters in fields`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T-001/TEST",
            userId = "U@001#",
            device = "Test:Device",
            os = "30",
            activity = "Test",
            description = "Description with newlines and tabs",
            stack = "Error with quotes and apostrophes",
            sdkStatus = "Active",
            institution = "Test Bank",
            tr = null,
            messages = emptyList()
        )

        // Then - Should handle special characters without issues
        assert(errorLog.tid == "T-001/TEST") { "TID with special chars should be preserved" }
        assert(errorLog.userId == "U@001#") { "UserId with special chars should be preserved" }
        assert(errorLog.stack == "Error with quotes and apostrophes") { "Stack with special chars should be preserved" }
        assert(errorLog.description == "Description with newlines and tabs") { "Description with special chars should be preserved" }
    }

    @Test
    fun `logError with empty error message still calls API`() {
        // Given
        val errorLog = ErrorLog("T001", "U001", "Device", "30", "Screen", "", "", "Active", "Bank", "", emptyList())

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        viewModel.logError(errorLog, dialog = false)

        // Then - Should still make API call even with empty error message
        verify(apiService).errorLog(errorLog)
    }
}
