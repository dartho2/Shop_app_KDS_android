package com.itsorderkds.data.network

import com.itsorderkds.data.model.Addon
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.model.ProductsCategoriesResponse
import com.itsorderkds.data.model.ProductsResponse
import com.itsorderkds.data.model.UpdateAddonStatusRequest
import com.itsorderkds.data.model.UpdateProductStatusRequest
import com.itsorderkds.data.model.UpdateProductRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {
    @GET("client/v3/api/admin/product")
    suspend fun getProducts(
        @Query("limit") limit: Int?,
        @Query("search") search: String?,
        @Query("status") status: Boolean?
    ): Response<ProductsResponse>

    @GET("client/v3/api/admin/categories/product")
    suspend fun getProductsByCategories(): Response<ProductsCategoriesResponse>

    @GET("client/v3/api/admin/product/{productId}")
    suspend fun getProductDetails(@Path("productId") productId: String, @Header("Accept-Language") lang: String? = null): Response<Product>

    @PUT("client/v3/api/admin/product/{productId}")
    suspend fun updateProduct(
        @Path("productId") productId: String,
        @Body productData: UpdateProductRequest,
    ): Response<Product>

    @PUT("client/v3/api/admin/addon/{addonId}/status") // Sprawdź endpoint w dokumentacji swojego backendu!
    suspend fun updateAddonStatus(
        @Path("addonId") id: String,
        @Body request: UpdateAddonStatusRequest
    ): Response<Addon>

    @PUT("client/v3/api/admin/product/{productId}/status")
    suspend fun updateStockStatus(
        @Path("productId") productId: String,
        @Body productData: UpdateProductStatusRequest,
    ): Response<Product>
}

