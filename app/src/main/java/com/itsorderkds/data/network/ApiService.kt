package com.itsorderkds.data.network


import com.itsorderkds.data.model.CategoryResponse
import com.itsorderkds.data.model.LanguagesResponse
import com.itsorderkds.data.model.VehicleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @GET("client/v3/api/admin/category")
    suspend fun getCategories(@Header("Accept-Language") lang: String? = null): Response<CategoryResponse>

    @GET("client/v3/api/admin/language")
    suspend fun getLanguages(): Response<LanguagesResponse>
}