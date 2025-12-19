package com.payten.whitelabel.api

import com.payten.whitelabel.dto.*
import com.payten.whitelabel.dto.ipsTransactions.GetIpsTransactionRequest
import com.payten.whitelabel.dto.ipsTransactions.GetIpsTransactionResponse
import com.payten.whitelabel.dto.keys.GetKeysRequestDto
import com.payten.whitelabel.dto.keys.GetKeysResponse
import com.payten.whitelabel.dto.reactivation.ReactivationApiResponse
import com.payten.whitelabel.dto.status.GetTerminalStatusApiResponse
import com.payten.whitelabel.dto.status.GetTerminalStatusRequest
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsRequest
import com.payten.whitelabel.dto.transactionDetails.GetTransactionDetailsResponse
import com.payten.whitelabel.dto.transactions.GetTransactionResponse
import com.payten.whitelabel.dto.transactions.GetTransactionsRequest
import io.reactivex.rxjava3.core.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Main Retrofit API service interface for all backend communication.
 *
 * This interface defines all API endpoints for terminal management, payment transactions,
 * merchant configuration, and system monitoring. All requests are automatically authenticated
 * via TokenInterceptor which adds Bearer token and Terminal-Identification headers.
 *
 * ## API Categories:
 *
 * ### Terminal Management & Registration:
 * - `activate()` - Register new terminal with backend
 * - `reactivation()` - Re-register existing terminal (wallet wipe + new credentials)
 * - `refreshToken()` - Refresh session authentication token
 * - `getTerminalStatus()` - Check terminal health and reactivation requirements
 * - `getDetails()` - Fetch merchant configuration (name, services, limits, receipt settings)
 * - `getKeys()` - Retrieve ECDSA public key components (hostX, hostY) for SDK wallet activation
 * - `healthCheck()` - Verify backend API connectivity
 *
 * ### Card Payment Transactions (via SDK):
 * - `getTransaction()` - Retrieve transaction history (card payments processed via SDK)
 * - `getTransactionDetails()` - Get detailed transaction info by ID
 *
 * ### IPS Payment Transactions (QR Code Payments):
 * - `payTransaction()` - Initiate IPS QR code payment request
 * - `checkCTSStatus()` - Poll IPS payment status (used for QR payment completion)
 * - `getIpsTransactions()` - Retrieve IPS transaction history
 * - `cancelIpsTransactions()` - Cancel/refund IPS transaction
 *
 * ### OTP & Security:
 * - `otpCreate()` - Generate OTP for sensitive operations
 * - `otpCheck()` - Verify OTP code
 *
 * ### Reporting & Diagnostics:
 * - `sendEmailReport()` - Email transaction report to merchant (PDF/CSV)
 * - `errorLog()` - Log errors with SDK diagnostics to backend for debugging
 *
 * ## Authentication:
 * Most endpoints require authentication via `TokenInterceptor`:
 * - Adds `Authorization: Bearer <token>` header
 * - Adds `Terminal-Identification: <tid>` header
 * - Token obtained from `refreshToken()` API during registration/login
 * - Token stored in SharedPreferences and automatically injected
 *
 * ## RxJava3 Integration:
 * All endpoints return `Observable<T>` for reactive programming:
 * - Subscribed on `Schedulers.io()` for background execution
 * - Observed on `AndroidSchedulers.mainThread()` for UI updates
 * - ViewModels use `CompositeDisposable` to manage subscriptions
 *
 * ## Base URL Configuration:
 * Configured in `ApiService.kt` Retrofit builder:
 * - **Test Environment:** `http://91.239.151.43:9090`
 * - **Production:** `https://spap.payten.rs/`
 *
 * ## Response Formats:
 * All API responses follow standard format:
 * ```kotlin
 * {
 *   "statusCode": "00",  // "00" = success, other codes = error
 *   "message": "Success",
 *   "data": { ... }      // Response payload
 * }
 * ```
 *
 * ## Error Logging:
 * When operations fail, ViewModels create error logs via `errorLog()` including:
 * - Device information (manufacturer, model, Android version)
 * - Terminal credentials (TID, user ID)
 * - SDK diagnostics (security status, transaction records, module logs)
 * - Error message and category
 *
 * @see TokenInterceptor for authentication header injection
 * @see RegistrationViewModel for terminal activation and key retrieval usage
 * @see LandingViewModel for merchant configuration and terminal status usage
 * @see PosViewModel for error logging usage
 * @see ApiService for Retrofit configuration and base URL setup
 */
interface SupercaseApiService {

//    @GET("ips/merchants/v2/details")
//    fun getDetails():
//            Observable<DetailsResponseDto>

    @GET("softpos/login/details")
    fun getDetails():
            Observable<DetailsResponseDto>

    @POST("ips/v2/getTransactionDetail")
    fun getTransactionDetails(@Body request: GetTransactionDetailsRequest): Observable<GetTransactionDetailsResponse>



    @GET("healthCheck")
    fun healthCheck(): Call<Void>

    @GET("ips/v2/terminal/{tid}/reactivate")
    fun reactivation(@Path("tid") tid: String):  Observable<ReactivationApiResponse>

    @POST("ips/v2/getTransaction")
    fun getTransaction(@Body request: GetTransactionsRequest): Observable<GetTransactionResponse>

    @POST("ips/v2/terminal/status")
    fun getTerminalStatus(@Body request: GetTerminalStatusRequest): Observable<GetTerminalStatusApiResponse>
    @POST("ips/v2/terminal/getKeys")
    fun getKeys(@Body request: GetKeysRequestDto): Observable<GetKeysResponse>

    @POST("ips/v2/getIpsTransactions")
    fun getIpsTransactions(@Body request: GetIpsTransactionRequest):
            Observable<GetIpsTransactionResponse>


    @POST("ips/v2/paymentReturn")
    fun cancelIpsTransactions(@Body request: CancelIpsTransactionDto):
            Observable<CancelIpsTransactionResponseDto>

    @POST("ips/v2/sendTransactionReportUsingMail")
    fun sendEmailReport(@Query("dateFrom") fromDate: String, @Query("dateTo") dateFrom: String, @Query("email") email: String, @Query("fileFormat") fileFormat: String, @Header("Terminal-Identification")terminalIdentification: String ):
            Observable<Response<Void>>

    @POST("res/v2/activate")
    fun activate(@Body request: ActivationDto):
            Observable<ActivationResponseDto>

    @POST("res/v2/generateToken")
    fun refreshToken(@Body request: GenerateTokenDto):
            Observable<ApiResponse>

    @POST("ips/checkCTStatusProxy")
    fun checkCTSStatus(@Body request: CheckTransferRequest): Observable<CheckTransactionResponseDto>

    @POST("ips/v2/requestToPay")
    fun payTransaction(@Body request: PayTransactionDto): Observable<CheckTransactionResponseDto>

    @GET("ips/v2/otpCreate")
    fun otpCreate(@Header("Terminal-Identification")terminalIdentification: String):
            Observable<Response<Void>>

    @POST("ips/v2/otpCheck")
    fun otpCheck(@Header("Terminal-Identification")terminalIdentification: String, @Body request: OtpCheckDto): Observable<ApiResponse>

    @POST("res/v2/logErrorEx")
    fun errorLog(@Body request: ErrorLog): Observable<Response<Void>>


}