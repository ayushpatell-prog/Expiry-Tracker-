package com.example.expirytracker1.api

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}

data class OpenFoodFactsResponse(
    val status: Int,
    val product: ProductData?
)

data class ProductData(
    val product_name: String?,
    val brands: String?,
    val categories: String?,
    val image_url: String?,
    val code: String?
)
