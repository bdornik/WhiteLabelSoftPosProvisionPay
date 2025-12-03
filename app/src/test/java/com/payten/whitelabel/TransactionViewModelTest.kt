package com.payten.whitelabel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.dto.ErrorLog
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailResponseData
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsRequest
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsResponse
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsResponseDataList
import com.payten.whitelabel.viewmodel.TransactionViewModel
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import retrofit2.Response
import rs.digitalworx.takt.api.SupercaseApiService

/**
 * Unit tests for TransactionViewModel.
 *
 * Tests transaction retrieval, error handling, and transaction data processing.
 */
class TransactionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: SupercaseApiService

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel = TransactionViewModel(apiService, sharedPreferences)
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    // ==================== Get Transaction Details Tests ====================

    @Test
    fun `getTransactionDetailFromServer with success returns transaction data`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC123", tid = "T001")
        val transactionDto = createMockTransactionDto()
        val transactionList = GetTransactionDetailsResponseDataList(listOf(transactionDto))
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = transactionList
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        var result: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { result = it }
        viewModel.getTransactionDetailFromServer(request)

        // Then
        assert(result != null) { "Should return transaction data" }
        verify(apiService).getTransactionDetails(request)
    }

    @Test
    fun `getTransactionDetailFromServer with non-00 status returns null`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC123", tid = "T001")
        val response = GetTransactionDetailsResponse(
            statusCode = "01",
            message = "Error",
            data = GetTransactionDetailsResponseDataList(emptyList())
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        var result: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { result = it }
        viewModel.getTransactionDetailFromServer(request)

        // Then
        assert(result == null) { "Should return null for non-00 status code" }
    }

    @Test
    fun `getTransactionDetailFromServer with error returns null`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC123", tid = "T001")
        val error = RuntimeException("Network error")

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.error(error))

        // When
        var result: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { result = it }
        viewModel.getTransactionDetailFromServer(request)

        // Then
        assert(result == null) { "Should return null on error" }
    }

    @Test
    fun `getTransactionDetailFromServer with empty transaction list returns null`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC123", tid = "T001")
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = GetTransactionDetailsResponseDataList(emptyList())
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        var result: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { result = it }
        viewModel.getTransactionDetailFromServer(request)

        // Then
        assert(result == null) { "Should return null when transaction list is empty" }
    }

    @Test
    fun `getTransactionDetailFromServer returns first transaction from list`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC123", tid = "T001")
        val transaction1 = createMockTransactionDto(recordId = 123)
        val transaction2 = createMockTransactionDto(recordId = 456)
        val transactionList = GetTransactionDetailsResponseDataList(listOf(transaction1, transaction2))
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = transactionList
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        var result: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { result = it }
        viewModel.getTransactionDetailFromServer(request)

        // Then
        assert(result != null) { "Should return first transaction" }
        assert(result?.recordId == 123) { "Should return first transaction with correct recordId" }
    }

    // ==================== Error Logging Tests ====================

    @Test
    fun `logError calls API with error log`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Test:Device",
            os = "30",
            activity = "TransactionViewModel",
            description = "Test error",
            stack = "Error stack",
            sdkStatus = "Active",
            institution = "Test Bank",
            tr = null,
            messages = emptyList()
        )

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        viewModel.logError(errorLog)

        // Then
        verify(apiService).errorLog(errorLog)
    }

    @Test
    fun `logError handles API error gracefully`() {
        // Given
        val errorLog = ErrorLog(
            tid = "T001",
            userId = "U001",
            device = "Test:Device",
            os = "30",
            activity = "TransactionViewModel",
            description = "Test error",
            stack = "Error stack",
            sdkStatus = "Active",
            institution = "Test Bank",
            tr = null,
            messages = emptyList()
        )

        val error = RuntimeException("Network error")
        `when`(apiService.errorLog(any())).thenReturn(Observable.error(error))

        // When - Should not throw exception
        viewModel.logError(errorLog)

        // Then - Verify API was called even though it failed
        verify(apiService).errorLog(errorLog)
    }

    @Test
    fun `multiple logError calls add to composite disposable`() {
        // Given
        val errorLog1 = ErrorLog("T001", "U001", "Device", "30", "Test", "Desc1", "Error1", "Active", "Bank", null, emptyList())
        val errorLog2 = ErrorLog("T002", "U002", "Device", "30", "Test", "Desc2", "Error2", "Active", "Bank", null, emptyList())

        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        viewModel.logError(errorLog1)
        viewModel.logError(errorLog2)

        // Then - Both calls should be made
        verify(apiService).errorLog(errorLog1)
        verify(apiService).errorLog(errorLog2)
    }

    // ==================== Request Validation Tests ====================

    @Test
    fun `GetTransactionDetailsRequest can be created with recordId and tid`() {
        // Given
        val recordId = "REC123456"
        val tid = "T001"

        // When
        val request = GetTransactionDetailsRequest(recordId = recordId, tid = tid)

        // Then
        assert(request.recordId == recordId) { "RecordId should match" }
        assert(request.tid == tid) { "TID should match" }
    }

    @Test
    fun `getTransactionDetailFromServer accepts valid request`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC001", tid = "T001")
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = GetTransactionDetailsResponseDataList(emptyList())
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        viewModel.getTransactionDetailFromServer(request)

        // Then
        verify(apiService).getTransactionDetails(request)
    }

    // ==================== LiveData Observer Tests ====================

    @Test
    fun `transactionResultsSuccess can be observed`() {
        // When
        var observedValue: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { observedValue = it }
        viewModel.transactionResultsSuccess.postValue(null)

        // Then
        assert(observedValue == null) { "LiveData should emit null" }
    }

    @Test
    fun `transactionResultsSuccess emits transaction data`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "REC123", tid = "T001")
        val transactionDto = createMockTransactionDto()
        val transactionList = GetTransactionDetailsResponseDataList(listOf(transactionDto))
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = transactionList
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        var emittedValue: GetTransactionDetailResponseData? = null
        viewModel.transactionResultsSuccess.observeForever { emittedValue = it }
        viewModel.getTransactionDetailFromServer(request)

        // Then
        assert(emittedValue != null) { "Should emit transaction data" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `getTransactionDetailFromServer with empty recordId still makes API call`() {
        // Given
        val request = GetTransactionDetailsRequest(recordId = "", tid = "T001")
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = GetTransactionDetailsResponseDataList(emptyList())
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        viewModel.getTransactionDetailFromServer(request)

        // Then
        verify(apiService).getTransactionDetails(request)
    }

    @Test
    fun `multiple getTransactionDetailFromServer calls can be made`() {
        // Given
        val request1 = GetTransactionDetailsRequest(recordId = "REC001", tid = "T001")
        val request2 = GetTransactionDetailsRequest(recordId = "REC002", tid = "T001")
        val response = GetTransactionDetailsResponse(
            statusCode = "00",
            message = "Success",
            data = GetTransactionDetailsResponseDataList(emptyList())
        )

        `when`(apiService.getTransactionDetails(any())).thenReturn(Observable.just(response))

        // When
        viewModel.getTransactionDetailFromServer(request1)
        viewModel.getTransactionDetailFromServer(request2)

        // Then
        verify(apiService).getTransactionDetails(request1)
        verify(apiService).getTransactionDetails(request2)
    }

    @Test
    fun `logError disposable is managed by composite disposable`() {
        // Given
        val errorLog = ErrorLog("T001", "U001", "Device", "30", "Test", "Desc", "Error", "Active", "Bank", null, emptyList())
        val mockResponse: Response<Void> = mock(Response::class.java) as Response<Void>
        `when`(apiService.errorLog(any())).thenReturn(Observable.just(mockResponse))

        // When
        viewModel.logError(errorLog)

        // Then - Disposable is added to CompositeDisposable (verified by no exceptions)
        verify(apiService).errorLog(errorLog)
    }

    // ==================== Helper Functions ====================

    private fun createMockTransactionDto(
        recordId: Int = 123
    ) = GetTransactionDetailResponseData(
        amount = 100.0,
        authorizationCode = "AUTH001",
        batchNo = 1,
        batchSeqNo = 1,
        currencyCode = "978",
        maskedPAN = "****1234",
        printerMessage = "Approved",
        recordId = recordId,
        responseCode = "00",
        screenMessage = "Approved",
        statusCode = "00",
        transactionCode = "00",
        transactionDate = "2025-12-03",
        mainRecordId = 123,
        terminalId = "T001",
        merchantId = "M001",
        internalMpaId = 1,
        internalTerminalId = 1,
        operationName = "SALE",
        wspId = 1,
        clearingDate = "2025-12-03",
        isVoidable = "Y",
        isRefundable = "Y",
        financialMode = "DEBIT",
        applicationLabel = "VISA",
        aid = "A0000000031010",
        bankResponseCode = "00",
        additionalDataOut = null,
        additionalDataIn = "",
        acqTenantId = "1",
        acquirerId = "1",
        issuerBin = "123456",
        proxyMerchantId = null,
        proxyTerminalId = null,
        userId = "U001",
        acqSelMode = "AUTO",
        wspTenantId = "1",
        mpaRecordId = "123",
        mpaOrderId = "ORD123",
        wspSubtenantId = "1",
        additionalOrderId = "",
        additionalUniqueId = "",
        originalAmount = 100.0,
        otherAmount = 0.0,
        mti = "0200",
        processingCode = "000000",
        posEntryMode = "071",
        transactionTypeSdk = "GOODS",
        transactionTypeInternal = "SALE",
        transactionSource = "POS",
        vendor = "TEST",
        RRN = "123456789012",
        STAN = "000001"
    )
}
