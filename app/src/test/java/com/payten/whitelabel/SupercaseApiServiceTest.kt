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
}