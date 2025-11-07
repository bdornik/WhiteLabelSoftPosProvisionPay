package rs.digitalworx.takt.api

import com.payten.nkbm.dto.*
import com.payten.nkbm.dto.ipsTransactions.GetIpsTransactionRequest
import com.payten.nkbm.dto.ipsTransactions.GetIpsTransactionResponse
import com.payten.nkbm.dto.keys.GetKeysRequestDto
import com.payten.nkbm.dto.keys.GetKeysResponse
import com.payten.nkbm.dto.reactivation.ReactivationApiResponse
import com.payten.nkbm.dto.status.GetTerminalStatusApiResponse
import com.payten.nkbm.dto.status.GetTerminalStatusRequest
import com.payten.nkbm.dto.transactionDetails.GetTransactionDetailsRequest
import com.payten.nkbm.dto.transactionDetails.GetTransactionDetailsResponse
import com.payten.nkbm.dto.transactions.GetTransactionResponse
import com.payten.nkbm.dto.transactions.GetTransactionsRequest
import io.reactivex.rxjava3.core.Observable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

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