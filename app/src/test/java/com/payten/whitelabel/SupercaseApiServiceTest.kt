package com.payten.whitelabel

import com.payten.whitelabel.api.SupercaseApiService
import com.payten.whitelabel.dto.ActivationDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import com.google.common.truth.Truth.assertThat
import com.payten.whitelabel.dto.ipsTransactions.GetIpsTransactionRequest
import java.util.concurrent.TimeUnit

class SupercaseApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: SupercaseApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()

        // Build Retrofit using the MockWebServer's URL
        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
            .create(SupercaseApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `activate sends correct POST request and parses response`() {
        // Mock successful response
        val successJson = """
            {
                "statusCode": "00",
                "message": "Success",
                "data": { "terminalId": "12345678" } 
            }
        """
        mockWebServer.enqueue(MockResponse().setBody(successJson).setResponseCode(200))

        // Note: Update arguments to match your actual ActivationDto constructor
        val requestDto = ActivationDto("user1", "1234", "A0000000041010")

        val testObserver = apiService.activate(requestDto).test()

        testObserver.awaitDone(1, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValue { response ->
            response.statusCode == "00"
        }

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/res/v2/activate")
        assertThat(recordedRequest.method).isEqualTo("POST")

        val requestBody = recordedRequest.body.readUtf8()
        assertThat(requestBody).contains("user1")
        assertThat(requestBody).contains("A0000000041010")
    }

    @Test
    fun `sendEmailReport sends correct Query params and Headers`() {
        // Mock empty void response (Retrofit Response<Void>)
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val dateFrom = "2023-01-01"
        val dateTo = "2023-01-31"
        val email = "merchant@test.com"
        val format = "PDF"
        val tid = "12345678"

        val testObserver = apiService.sendEmailReport(dateFrom, dateTo, email, format, tid).test()

        testObserver.awaitDone(1, TimeUnit.SECONDS)
        testObserver.assertNoErrors()

        val recordedRequest = mockWebServer.takeRequest()

        // Verify Path and Query Parameters
        // Expected: /ips/v2/sendTransactionReportUsingMail?dateFrom=...&dateTo=...&email=...&fileFormat=...
        val path = recordedRequest.path
        assertThat(path).startsWith("/ips/v2/sendTransactionReportUsingMail")
        assertThat(path).contains("dateFrom=$dateFrom")
        assertThat(path).contains("dateTo=$dateTo")
        assertThat(path).contains("email=merchant%40test.com")
        assertThat(path).contains("fileFormat=$format")

        // Verify Header
        assertThat(recordedRequest.getHeader("Terminal-Identification")).isEqualTo(tid)

        // Verify Method
        assertThat(recordedRequest.method).isEqualTo("POST")
    }

    @Test
    fun `reactivation substitutes Path variable correctly`() {
        val successJson = """
            {
                "statusCode": "00",
                "message": "Reactivation successful"
            }
        """
        mockWebServer.enqueue(MockResponse().setBody(successJson).setResponseCode(200))

        val tid = "999999"
        val testObserver = apiService.reactivation(tid).test()

        testObserver.awaitDone(1, TimeUnit.SECONDS)
        testObserver.assertNoErrors()

        val recordedRequest = mockWebServer.takeRequest()

        // Verify path substitution: /ips/v2/terminal/{tid}/reactivate
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/terminal/$tid/reactivate")
        assertThat(recordedRequest.method).isEqualTo("GET")
    }

    @Test
    fun `getDetails makes correct GET request`() {
        val jsonResponse = """
            {
                "statusCode": "00",
                "merchantName": "Test Merchant"
            }
        """
        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val testObserver = apiService.getDetails().test()

        testObserver.awaitDone(1, TimeUnit.SECONDS)
        testObserver.assertNoErrors()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/softpos/login/details")
        assertThat(recordedRequest.method).isEqualTo("GET")
    }

    @Test
    fun `getIpsTransactions sends correct POST request and parses list response`() {
        // 1. Prepare JSON response matching GetIpsTransactionResponse and TransactionData structure
        val jsonResponse = """
            {
                "statusCode": "00",
                "message": "Success",
                "data": [
                    {
                        "endToEndIdentificator": "txn-12345",
                        "tid": "12345678",
                        "statusCode": "00",
                        "date": "2023-10-01T12:00:00",
                        "amount": "1500.00"
                    }
                ]
            }
        """
        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        // 2. Prepare Request using the correct flat DTO structure
        val request = GetIpsTransactionRequest(
            userId = "user_test",
            dateFrom = "2023-10-01",
            dateTo = "2023-10-31",
            tid = "12345678"
        )

        // 3. Call the API
        val testObserver = apiService.getIpsTransactions(request).test()

        // 4. Verify Response Logic
        testObserver.awaitDone(1, TimeUnit.SECONDS)
        testObserver.assertNoErrors()
        testObserver.assertValue { response ->
            // Verify top-level response fields
            val isStatusCorrect = response.statusCode == "00"

            // Verify the list contains our mocked transaction
            val isDataCorrect = response.data.isNotEmpty() &&
                    response.data[0].amount == "1500.00" &&
                    response.data[0].endToEndIdentificator == "txn-12345"

            isStatusCorrect && isDataCorrect
        }

        // 5. Verify the HTTP Request
        val recordedRequest = mockWebServer.takeRequest()

        // Assert Path
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/getIpsTransactions")

        // Assert Method
        assertThat(recordedRequest.method).isEqualTo("POST")

        // Assert Body contains the flat JSON fields we sent
        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("user_test")
        assertThat(body).contains("2023-10-01")
        assertThat(body).contains("12345678")
    }
    // ==========================================
    // PAYMENT & TRANSACTIONS (CRITICAL PATH)
    // ==========================================

    @Test
    fun `payTransaction sends correct POST request for IPS payments`() {
        val jsonResponse = """{"statusCode": "00", "message": "Payment Initiated"}"""
        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        // Updated to use the correct PayTransactionDto fields
        val request = com.payten.whitelabel.dto.PayTransactionDto(
            creditTransferIdentificator = "IPS-ID-12345",
            terminalIdentificator = "12345678",
            creditTransferAmount = "1500.00",
            debtorAccountNumber = "999-000000-11",
            oneTimeCode = "123456",
            debtorReference = "REF-2023",
            debtorName = "John Doe",
            debtorAddress = "Main St 1"
        )

        val testObserver = apiService.payTransaction(request).test()

        testObserver.awaitDone(1, TimeUnit.SECONDS)
        testObserver.assertNoErrors()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/requestToPay")
        assertThat(recordedRequest.method).isEqualTo("POST")

        // Verify specific field mapping
        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("creditTransferAmount") //
        assertThat(body).contains("1500.00")
        assertThat(body).contains("debtorAccountNumber")
    }

    @Test
    fun `getTransactionDetails sends correct POST request`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00"}""").setResponseCode(200))

        // Updated to use correct GetTransactionDetailsRequest fields
        val request = com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsRequest(
            recordId = "REC-999",
            tid = "12345678"
        )

        apiService.getTransactionDetails(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/getTransactionDetail")
        assertThat(recordedRequest.method).isEqualTo("POST")

        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("REC-999")
        assertThat(body).contains("tid")
    }

    // ==========================================
    // AUTHENTICATION & SECURITY
    // ==========================================

    @Test
    fun `refreshToken sends correct POST request`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00", "token": "new-jwt"}""").setResponseCode(200))

        val request = com.payten.whitelabel.dto.GenerateTokenDto("userId", "tid")

        apiService.refreshToken(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/res/v2/generateToken")
        assertThat(recordedRequest.method).isEqualTo("POST")
    }

    @Test
    fun `getKeys retrieves security keys correctly`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00", "keys": "..."}""").setResponseCode(200))

        val request = com.payten.whitelabel.dto.keys.GetKeysRequestDto("tid-123")

        apiService.getKeys(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/terminal/getKeys")
        assertThat(recordedRequest.method).isEqualTo("POST")
    }

    @Test
    fun `otpCreate sends Header correctly via GET`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val tid = "12345678"
        apiService.otpCreate(tid).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/otpCreate")
        assertThat(recordedRequest.method).isEqualTo("GET")
        // Verify the @Header annotation worked
        assertThat(recordedRequest.getHeader("Terminal-Identification")).isEqualTo(tid)
    }

    // ==========================================
    // SYSTEM & DIAGNOSTICS
    // ==========================================

    @Test
    fun `healthCheck executes correct GET request`() {
        // NOTE: healthCheck returns Call<Void>, not Observable, so we test it differently
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val call = apiService.healthCheck()
        val response = call.execute() // Synchronous execution for testing

        assertThat(response.isSuccessful).isTrue()

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/healthCheck")
        assertThat(recordedRequest.method).isEqualTo("GET")
    }

    @Test
    fun `errorLog sends diagnostics via POST`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // Updated to use correct ErrorLog fields
        // Note: passing emptyList() for 'messages'. Ensure your project has the 'Message' class available.
        val request = com.payten.whitelabel.dto.ErrorLog(
            tid = "12345678",
            userId = "user_01",
            device = "Samsung S21",
            os = "Android 13",
            activity = "PaymentActivity",
            description = "NullPointerException",
            stack = "Stacktrace...",
            sdkStatus = "ACTIVE",
            institution = "BankXYZ",
            tr = "TR-CODE",
            messages = emptyList()
        )

        apiService.errorLog(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/res/v2/logErrorEx")
        assertThat(recordedRequest.method).isEqualTo("POST")

        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("Samsung S21")
        assertThat(body).contains("sdkStatus")
    }

    @Test
    fun `cancelIpsTransactions sends correct POST request`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00"}""").setResponseCode(200))

        // Uses CancelIpsTransactionDto
        val request = com.payten.whitelabel.dto.CancelIpsTransactionDto(
            creditTransferIdentificator = "IPS-REF-123",
            creditTransferAmount = "1500.00",
            terminalIdentificator = "12345678"
        )

        apiService.cancelIpsTransactions(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/paymentReturn")
        assertThat(recordedRequest.method).isEqualTo("POST")

        // Check body content
        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("IPS-REF-123")
        assertThat(body).contains("1500.00")
    }

    @Test
    fun `checkCTSStatus polls status via POST`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00", "status": "COMPLETED"}""").setResponseCode(200))

        // Uses CheckTransferRequest
        val request = com.payten.whitelabel.dto.CheckTransferRequest(
            endToEndReference = "E2E-REF-999",
            terminalIdentificator = "12345678",
            amount = "200.00",
            qrCodeString = "RAW_QR_DATA_STRING",
            transactionId = "TX-ID-555"
        )

        apiService.checkCTSStatus(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/checkCTStatusProxy")
        assertThat(recordedRequest.method).isEqualTo("POST")

        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("E2E-REF-999")
        assertThat(body).contains("RAW_QR_DATA_STRING")
    }

    @Test
    fun `otpCheck verifies code via POST with Header`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00"}""").setResponseCode(200))

        // Uses OtpCheckDto
        val request = com.payten.whitelabel.dto.OtpCheckDto(
            userId = "user_test_01",
            activationCode = "123456"
        )
        val tidHeader = "TID-HEADER-123"

        apiService.otpCheck(tidHeader, request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/otpCheck")
        assertThat(recordedRequest.method).isEqualTo("POST")

        // Verify Header
        assertThat(recordedRequest.getHeader("Terminal-Identification")).isEqualTo(tidHeader)

        // Verify Body
        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("user_test_01")
        assertThat(body).contains("123456")
    }

    // ==========================================
    // REMAINING SYSTEM & CARD OPERATIONS
    // ==========================================

    @Test
    fun `getTransaction retrieves history via POST`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00", "transactions": []}""").setResponseCode(200))

        // Uses GetTransactionsRequest
        val request = com.payten.whitelabel.dto.transactions.GetTransactionsRequest(
            userId = "user_test",
            dateFrom = "2023-01-01",
            dateTo = "2023-01-31",
            tid = "12345678"
        )

        apiService.getTransaction(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/getTransaction")
        assertThat(recordedRequest.method).isEqualTo("POST")

        val body = recordedRequest.body.readUtf8()
        assertThat(body).contains("user_test")
        assertThat(body).contains("2023-01-01")
    }

    @Test
    fun `getTerminalStatus checks device health via POST`() {
        mockWebServer.enqueue(MockResponse().setBody("""{"statusCode": "00", "status": "ACTIVE"}""").setResponseCode(200))

        // Uses GetTerminalStatusRequest
        val request = com.payten.whitelabel.dto.status.GetTerminalStatusRequest(
            userId = "user_status_check"
        )

        apiService.getTerminalStatus(request).test().awaitDone(1, TimeUnit.SECONDS)

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).isEqualTo("/ips/v2/terminal/status")
        assertThat(recordedRequest.method).isEqualTo("POST")

        assertThat(recordedRequest.body.readUtf8()).contains("user_status_check")
    }
}