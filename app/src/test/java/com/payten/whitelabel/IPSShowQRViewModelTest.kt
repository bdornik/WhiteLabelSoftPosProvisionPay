package com.payten.whitelabel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.api.SupercaseApiService
import com.payten.whitelabel.dto.*
import com.payten.whitelabel.viewmodel.IPSShowQRViewModel
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

/**
 * Unit tests for IPSShowQRViewModel.
 *
 * Tests IPS QR code payment flow including:
 * - QR code string parsing and validation
 * - QR object creation from parsed data
 * - Payment transaction API calls
 * - Transaction status checking with polling
 *
 * ## Test Coverage:
 * - QR code parsing (createQrCodeKeyValueMapFromString)
 * - Tag validation (containsRequiredTags)
 * - QR object mapping (createQRcodeObjectFromValues)
 * - Field validation (containsRequiredFieldValues)
 * - Payment API (pay, check)
 * - Disposable management
 */
class IPSShowQRViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: SupercaseApiService

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    private lateinit var viewModel: IPSShowQRViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        viewModel = IPSShowQRViewModel(apiService, sharedPreferences)
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    // ==================== QR Code Parsing Tests (4 tests) ====================

    @Test
    fun `createQrCodeKeyValueMapFromString with valid QR string parses correctly`() {
        // Given
        val qrString = "K:PK|V:01|C:1|O:123456789012345678|R:987654321098765432|I:RSD100.00"

        // When
        val result = viewModel.createQrCodeKeyValueMapFromString(qrString)

        // Then
        assert(result != null)
        assert(result!!["K"] == "PK")
        assert(result["V"] == "01")
        assert(result["C"] == "1")
        assert(result["O"] == "123456789012345678")
        assert(result["R"] == "987654321098765432")
        assert(result["I"] == "RSD100.00")
    }

    @Test
    fun `createQrCodeKeyValueMapFromString with empty string returns empty map`() {
        // Given
        val qrString = ""

        // When
        val result = viewModel.createQrCodeKeyValueMapFromString(qrString)

        // Then
        assert(result != null)
        assert(result!!.isEmpty())
    }

    @Test
    fun `createQrCodeKeyValueMapFromString ignores malformed entries`() {
        // Given - Contains entry without delimiter
        val qrString = "K:PK|INVALID|V:01"

        // When
        val result = viewModel.createQrCodeKeyValueMapFromString(qrString)

        // Then - Should only parse valid entries
        assert(result != null)
        assert(result!!["K"] == "PK")
        assert(result["V"] == "01")
        assert(result.size == 2)
    }

    @Test
    fun `createQrCodeKeyValueMapFromString handles colon in value`() {
        // Given - Value contains multiple colons
        val qrString = "K:PK|S:Payment:with:colons"

        // When
        val result = viewModel.createQrCodeKeyValueMapFromString(qrString)

        // Then - Should take everything after LAST colon as value
        assert(result != null)
        assert(result!!["K"] == "PK")
        assert(result["S"] == "colons") // Takes substring after last colon
    }

    // ==================== Tag Validation Tests ====================

    @Test
    fun `containsRequiredTags returns true when all required tags present`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "C" to "1",
            "O" to "123456789012345678"
        )

        // When
        val result = viewModel.containsRequiredTags(qrValues)

        // Then
        assert(result)
    }

    @Test
    fun `containsRequiredTags returns false when K tag missing`() {
        // Given
        val qrValues = mapOf(
            "V" to "01",
            "C" to "1",
            "O" to "123456789012345678"
        )

        // When
        val result = viewModel.containsRequiredTags(qrValues)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredTags returns false when V tag missing`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "C" to "1",
            "O" to "123456789012345678"
        )

        // When
        val result = viewModel.containsRequiredTags(qrValues)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredTags returns false when C tag missing`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "O" to "123456789012345678"
        )

        // When
        val result = viewModel.containsRequiredTags(qrValues)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredTags returns false when O tag missing`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "C" to "1"
        )

        // When
        val result = viewModel.containsRequiredTags(qrValues)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredTags returns true with extra optional tags`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "C" to "1",
            "O" to "123456789012345678",
            "R" to "987654321098765432",
            "I" to "RSD100.00",
            "N" to "Merchant Name",
            "P" to "Payer Name"
        )

        // When
        val result = viewModel.containsRequiredTags(qrValues)

        // Then
        assert(result)
    }

    // ==================== QR Object Creation Tests ====================

    @Test
    fun `createQRcodeObjectFromValues creates QR with all fields`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "C" to "1",
            "R" to "987654321098765432",
            "N" to "Recipient Name",
            "I" to "RSD100.00",
            "O" to "123456789012345678",
            "P" to "Payer Name",
            "SF" to "221",
            "S" to "Payment purpose",
            "M" to "5411",
            "JS" to "password123",
            "RK" to "REF001",
            "RO" to "CALL001",
            "RL" to "987654",
            "RP" to "TXN001"
        )

        // When
        val result = viewModel.createQRcodeObjectFromValues(qrValues)

        // Then
        assert(result != null)
        assert(result!!.identificationCode == "PK")
        assert(result.version == "01")
        assert(result.charSet == "1")
        assert(result.recepientAccNumber == "987654321098765432")
        assert(result.recepientNameAndPlace == "Recipient Name")
        assert(result.amountAndCurrency == "RSD100.00")
        assert(result.accNumber == "123456789012345678")
        assert(result.nameAndPlace == "Payer Name")
        assert(result.paymentCode == "221")
        assert(result.paymentPurpose == "Payment purpose")
        assert(result.mcc == "5411")
        assert(result.oneTimePaymentPassword == "password123")
        assert(result.payerReference == "REF001")
        assert(result.recepientCallNumberReference == "CALL001")
        assert(result.recepientReferenceNumber == "987654")
        assert(result.transactionReference == "TXN001")
    }

    @Test
    fun `createQRcodeObjectFromValues with minimal fields creates valid QR`() {
        // Given - Only required fields
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "C" to "1",
            "O" to "123456789012345678"
        )

        // When
        val result = viewModel.createQRcodeObjectFromValues(qrValues)

        // Then
        assert(result != null)
        assert(result!!.identificationCode == "PK")
        assert(result.version == "01")
        assert(result.charSet == "1")
        assert(result.accNumber == "123456789012345678")
    }

    @Test
    fun `createQRcodeObjectFromValues ignores unknown tags`() {
        // Given
        val qrValues = mapOf(
            "K" to "PK",
            "V" to "01",
            "UNKNOWN" to "value",
            "ANOTHER_UNKNOWN" to "value2"
        )

        // When
        val result = viewModel.createQRcodeObjectFromValues(qrValues)

        // Then - Should not crash, creates QR with known fields only
        assert(result != null)
        assert(result!!.identificationCode == "PK")
        assert(result.version == "01")
    }

    // ==================== Field Validation Tests ====================

    @Test
    fun `containsRequiredFieldValues returns true for valid QR`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = "123456789012345678"

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when identificationCode is null`() {
        // Given
        val qr = QR()
        qr.identificationCode = null
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = "123456789012345678"

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when identificationCode is not PK`() {
        // Given
        val qr = QR()
        qr.identificationCode = "INVALID"
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = "123456789012345678"

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when version is not 01`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "02"
        qr.charSet = "1"
        qr.accNumber = "123456789012345678"

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when charSet is not 1`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "01"
        qr.charSet = "2"
        qr.accNumber = "123456789012345678"

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when accNumber is null`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = null

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when accNumber exceeds 18 characters`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = "1234567890123456789" // 19 characters

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    @Test
    fun `containsRequiredFieldValues returns true when accNumber is exactly 18 characters`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = "123456789012345678" // Exactly 18

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(result)
    }

    @Test
    fun `containsRequiredFieldValues returns false when accNumber is empty`() {
        // Given
        val qr = QR()
        qr.identificationCode = "PK"
        qr.version = "01"
        qr.charSet = "1"
        qr.accNumber = ""

        // When
        val result = viewModel.containsRequiredFieldValues(qr)

        // Then
        assert(!result)
    }

    // ==================== API Tests - pay() ====================

    @Test
    fun `pay with successful response posts payTransaction LiveData`() {
        // Given
        val payDto = PayTransactionDto(
            creditTransferIdentificator = "CTI123",
            terminalIdentificator = "TERM123",
            creditTransferAmount = "100.00",
            debtorAccountNumber = "123456789012345678",
            oneTimeCode = "OTC123",
            debtorReference = "REF001",
            debtorName = "Test Merchant",
            debtorAddress = "Test Address"
        )

        val responseDto = CheckTransactionResponseDto(
            creditTransferIdentificator = "CTI123",
            terminalIdentificator = "TERM123",
            approvalCode = "APP123",
            statusCode = "00"
        )

        `when`(apiService.payTransaction(any())).thenReturn(Observable.just(responseDto))

        // When
        viewModel.pay(payDto)

        // Then
        assert(viewModel.payTransaction.value == responseDto)
    }

    @Test
    fun `pay with network error posts payTransactionFailed LiveData`() {
        // Given
        val payDto = PayTransactionDto(
            creditTransferIdentificator = "CTI123",
            terminalIdentificator = "TERM123",
            creditTransferAmount = "100.00",
            debtorAccountNumber = "123456789012345678",
            oneTimeCode = "OTC123",
            debtorReference = null,
            debtorName = null,
            debtorAddress = null
        )

        val error = RuntimeException("Network error")
        `when`(apiService.payTransaction(any())).thenReturn(Observable.error(error))

        // When
        viewModel.pay(payDto)

        // Then
        assert(viewModel.payTransactionFailed.value != null)
        assert(viewModel.payTransactionFailed.value!!.contains("Network error"))
    }

    // ==================== API Tests - check() ====================

    @Test
    fun `check with status code 00 posts payTransaction LiveData`() {
        // Given
        val responseDto = CheckTransactionResponseDto(
            creditTransferIdentificator = "CTI123",
            terminalIdentificator = "TERM123",
            approvalCode = "APP123",
            statusCode = "00"
        )

        `when`(apiService.checkCTSStatus(any())).thenReturn(Observable.just(responseDto))

        // When
        viewModel.check("CTI123", "TERM123", "100.00", "K:PK|V:01")

        // Then
        assert(viewModel.payTransaction.value == responseDto)
    }

    @Test
    fun `check with status code 85 does not post LiveData`() {
        // Given - Status 85 means pending/timeout
        val responseDto = CheckTransactionResponseDto(
            creditTransferIdentificator = "CTI123",
            terminalIdentificator = "TERM123",
            approvalCode = "",
            statusCode = "85"
        )

        `when`(apiService.checkCTSStatus(any())).thenReturn(Observable.just(responseDto))

        // When
        viewModel.check("CTI123", "TERM123", "100.00", "K:PK|V:01")

        // Then - Should not post to payTransaction (stays null)
        assert(viewModel.payTransaction.value == null)
    }

    @Test
    fun `check with non-00 and non-85 status posts payTransaction LiveData`() {
        // Given
        val responseDto = CheckTransactionResponseDto(
            creditTransferIdentificator = "CTI123",
            terminalIdentificator = "TERM123",
            approvalCode = "",
            statusCode = "06"
        )

        `when`(apiService.checkCTSStatus(any())).thenReturn(Observable.just(responseDto))

        // When
        viewModel.check("CTI123", "TERM123", "100.00", "K:PK|V:01")

        // Then
        assert(viewModel.payTransaction.value == responseDto)
    }

    @Test
    fun `check with network error posts checkTransactionFailed LiveData`() {
        // Given
        val error = RuntimeException("Network timeout")
        `when`(apiService.checkCTSStatus(any())).thenReturn(Observable.error(error))

        // When
        viewModel.check("CTI123", "TERM123", "100.00", "K:PK|V:01")

        // Then
        assert(viewModel.checkTransactionFailed.value != null)
        assert(viewModel.checkTransactionFailed.value!!.contains("Network timeout"))
    }
}
