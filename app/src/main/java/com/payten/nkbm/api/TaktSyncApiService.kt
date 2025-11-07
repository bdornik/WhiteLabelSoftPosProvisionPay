package rs.digitalworx.takt.api

import com.payten.nkbm.dto.ApiResponse
import retrofit2.Call
import retrofit2.http.Headers
import retrofit2.http.POST

interface TaktSyncApiService {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("res/v2/generateToken")
    fun refreshToken() :
            Call<ApiResponse>
//
//    @Multipart
//    @POST("file")
//    fun uploadFile(@Part part: MultipartBody.Part): Call<ApiResponse<UploadFileResponseDto>>
}